package com.dudifidof.longwalk;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LongWalkMod implements ModInitializer {

    public static final String MOD_ID = "longwalk";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("A Long Walk to Water initialized");
    }
}
