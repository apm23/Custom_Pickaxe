package com.apm23.custompickaxe;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
            if (!(level instanceof ServerLevel) || !(player instanceof ServerPlayer serverPlayer)) {
                return;
            }

            ItemStack stack = serverPlayer.getMainHandItem();
            String type = PickaxeIdentity.type(stack);
            if (!PickaxeIdentity.isRemotePickaxe(stack)
                    || !PickaxeIdentity.isEnabled(stack)
                    || !RemoteMiningManager.isSupportedType(type)
                    || isOreLikeResource(state)) {
                return;
            }

            RemoteMiningManager.start(serverPlayer, pos, type);
        });

        ServerTickEvents.END_LEVEL_TICK.register(RemoteMiningManager::tick);

        LOGGER.info("Custom Pickaxe server-side mod initialized");
    }

    private static boolean isOreLikeResource(BlockState state) {
        return state.is(Blocks.COAL_ORE)
                || state.is(Blocks.DEEPSLATE_COAL_ORE)
                || state.is(Blocks.COPPER_ORE)
                || state.is(Blocks.DEEPSLATE_COPPER_ORE)
                || state.is(Blocks.DIAMOND_ORE)
                || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)
                || state.is(Blocks.EMERALD_ORE)
                || state.is(Blocks.DEEPSLATE_EMERALD_ORE)
                || state.is(Blocks.GOLD_ORE)
                || state.is(Blocks.DEEPSLATE_GOLD_ORE)
                || state.is(Blocks.NETHER_GOLD_ORE)
                || state.is(Blocks.IRON_ORE)
                || state.is(Blocks.DEEPSLATE_IRON_ORE)
                || state.is(Blocks.LAPIS_ORE)
                || state.is(Blocks.DEEPSLATE_LAPIS_ORE)
                || state.is(Blocks.REDSTONE_ORE)
                || state.is(Blocks.DEEPSLATE_REDSTONE_ORE)
                || state.is(Blocks.NETHER_QUARTZ_ORE)
                || state.is(Blocks.ANCIENT_DEBRIS);
    }
}
