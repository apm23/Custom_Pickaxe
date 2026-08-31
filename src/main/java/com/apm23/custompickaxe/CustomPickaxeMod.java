package com.apm23.custompickaxe;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CustomPickaxeMod implements ModInitializer {
    public static final String MOD_ID = "custom_pickaxe";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (level.isClientSide() || !player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }

            ItemStack stack = player.getItemInHand(hand);
            if (!PickaxeIdentity.isRemotePickaxe(stack)) {
                return InteractionResult.PASS;
            }

            PickaxeIdentity.toggleEnabled(stack);
            return InteractionResult.SUCCESS;
        });

        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
                return;
            }

            ItemStack stack = serverPlayer.getMainHandItem();
            if (!PickaxeIdentity.isRemotePickaxe(stack)
                    || !PickaxeIdentity.isEnabled(stack)
                    || !"iron".equals(PickaxeIdentity.type(stack))
                    || isOre(state)) {
                return;
            }

            RemoteMiningManager.start(serverPlayer, pos);
        });

        ServerTickEvents.END_LEVEL_TICK.register(RemoteMiningManager::tick);

        LOGGER.info("Custom Pickaxe server-side mod initialized");
    }

    private static boolean isOre(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(BlockTags.COAL_ORES)
                || state.is(BlockTags.COPPER_ORES)
                || state.is(BlockTags.DIAMOND_ORES)
                || state.is(BlockTags.EMERALD_ORES)
                || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.LAPIS_ORES)
                || state.is(BlockTags.REDSTONE_ORES);
    }
}
