package com.apm23.custompickaxe;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
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

        LOGGER.info("Custom Pickaxe server-side mod initialized");
    }
}
