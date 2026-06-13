package dev.neu1.blazeprogress;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entrypoint. The interesting work happens in the client entrypoint. */
public final class BlazeProgress implements ModInitializer {

    public static final String MOD_ID = "blazeprogress";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[BlazeProgress] loaded — Blaze Slayer T4 setup tracker");
    }
}
