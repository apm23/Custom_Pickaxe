package com.apm23.custompickaxe;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CustomPickaxeMod implements ModInitializer {
    public static final String MOD_ID = "custom_pickaxe";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Custom Pickaxe server-side mod initialized");
    }
}
