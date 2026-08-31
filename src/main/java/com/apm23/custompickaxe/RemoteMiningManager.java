package com.apm23.custompickaxe;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class RemoteMiningManager {
    private static final int HALF_RANGE = 32;
    private static final int SIDE = 64;
    private static final int POSITIONS_PER_TICK = 8192;
    private static final int BLOCKS_PER_TICK = 256;

    private static final Map<String, Block> TARGETS = Map.of(
            "iron", Blocks.IRON_BLOCK,
            "copper", Blocks.COPPER_BLOCK.asList().get(0),
            "gold", Blocks.GOLD_BLOCK,
            "diamond", Blocks.DIAMOND_BLOCK,
            "emerald", Blocks.EMERALD_BLOCK,
            "coal", Blocks.COAL_BLOCK,
            "lapis", Blocks.LAPIS_BLOCK,
            "redstone", Blocks.REDSTONE_BLOCK,
            "debris", Blocks.ANCIENT_DEBRIS
    );

    private static final Map<UUID, ScanTask> TASKS = new HashMap<>();

    private RemoteMiningManager() {
    }

    public static boolean isSupportedType(String type) {
        return TARGETS.containsKey(type);
    }

    public static void start(ServerPlayer player, BlockPos origin, String type) {
        Block target = TARGETS.get(type);
        if (target == null) {
            return;
        }

        if (player.level() instanceof ServerLevel level) {
            breakNaturalMask(level, player, origin);
        }

        TASKS.put(player.getUUID(), new ScanTask(player, origin.immutable(), player.level().dimension(), target));
    }

    public static void tick(ServerLevel level) {
        TASKS.values().removeIf(task -> task.tick(level));
    }

    private static void breakNaturalMask(ServerLevel level, ServerPlayer player, BlockPos origin) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int wanted = random.nextInt(2, 6);
        int broken = 0;
        int attempts = 0;

        Direction forward = player.getDirection();
        Direction side = forward.getClockWise();

        while (broken < wanted && attempts++ < 24) {
            int forwardOffset = random.nextInt(0, 3);
            int sideOffset = random.nextInt(-1, 2);
            int yOffset = random.nextInt(-1, 2);

            BlockPos candidate = origin
                    .relative(forward, forwardOffset)
                    .relative(side, sideOffset)
                    .above(yOffset);

            if (candidate.equals(origin)
                    || !level.isInWorldBounds(candidate)
                    || !level.hasChunkAt(candidate)
                    || level.getBlockEntity(candidate) != null) {
                continue;
            }

            BlockState state = level.getBlockState(candidate);
            if (!isNaturalMaskTerrain(state)) {
                continue;
            }

            if (level.destroyBlock(candidate, true, player)) {
                broken++;
            }
        }
    }

    private static boolean isNaturalMaskTerrain(BlockState state) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.NETHERRACK)
                || state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.SMOOTH_BASALT);
    }

    private static final class ScanTask {
        private final ServerPlayer player;
        private final BlockPos origin;
        private final ResourceKey<Level> dimension;
        private final Block targetBlock;
        private final Deque<BlockPos> targets = new ArrayDeque<>();
        private int cursor;
        private int collected;
        private boolean scanComplete;

        private ScanTask(ServerPlayer player, BlockPos origin, ResourceKey<Level> dimension, Block targetBlock) {
            this.player = player;
            this.origin = origin;
            this.dimension = dimension;
            this.targetBlock = targetBlock;
        }

        private boolean tick(ServerLevel level) {
            if (player.isRemoved()) {
                return true;
            }
            if (level.dimension() != dimension) {
                return false;
            }
            if (player.level() != level) {
                return true;
            }

            if (!scanComplete) {
                scan(level);
            }
            breakTargets(level);

            if (scanComplete && targets.isEmpty()) {
                dropCollected(level);
                return true;
            }
            return false;
        }

        private void scan(ServerLevel level) {
            int end = Math.min(cursor + POSITIONS_PER_TICK, SIDE * SIDE * SIDE);
            while (cursor < end) {
                int index = cursor++;
                int x = index & 63;
                int z = (index >> 6) & 63;
                int y = (index >> 12) & 63;
                BlockPos pos = origin.offset(x - HALF_RANGE, y - HALF_RANGE, z - HALF_RANGE);
                if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)) {
                    continue;
                }
                if (level.getBlockState(pos).is(targetBlock)) {
                    targets.addLast(pos.immutable());
                }
            }
            scanComplete = cursor >= SIDE * SIDE * SIDE;
        }

        private void breakTargets(ServerLevel level) {
            int broken = 0;
            while (broken < BLOCKS_PER_TICK && !targets.isEmpty()) {
                BlockPos pos = targets.removeFirst();
                if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(targetBlock)) {
                    continue;
                }
                if (level.destroyBlock(pos, false, player)) {
                    collected++;
                    broken++;
                }
            }
        }

        private void dropCollected(ServerLevel level) {
            if (collected <= 0) {
                return;
            }

            var look = player.getLookAngle();
            double x = player.getX() + look.x;
            double y = player.getY() + 0.5;
            double z = player.getZ() + look.z;
            int remaining = collected;
            int maxStack = targetBlock.asItem().getDefaultMaxStackSize();

            while (remaining > 0) {
                int amount = Math.min(remaining, maxStack);
                ItemStack stack = new ItemStack(targetBlock.asItem(), amount);
                ItemEntity entity = new ItemEntity(level, x, y, z, stack);
                entity.setDefaultPickUpDelay();
                level.addFreshEntity(entity);
                remaining -= amount;
            }
        }
    }
}
