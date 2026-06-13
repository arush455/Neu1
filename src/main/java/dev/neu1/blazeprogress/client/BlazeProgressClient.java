package dev.neu1.blazeprogress.client;

import dev.neu1.blazeprogress.config.ConfigManager;
import dev.neu1.blazeprogress.config.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Client entrypoint: wires up the HUD, command, keybind and data trackers. */
public final class BlazeProgressClient implements ClientModInitializer {

    public static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        ConfigManager.load();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.blazeprogress.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "key.categories.blazeprogress"));

        ProgressHud.register();
        Commands.register();

        // Capture intelligence from the SkyBlock action bar.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) StatTracker.onActionBar(message.getString());
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PetTracker.tick(client);
            while (toggleKey.wasPressed()) {
                ModConfig cfg = ConfigManager.get();
                cfg.overlayEnabled = !cfg.overlayEnabled;
                ConfigManager.save();
            }
        });
    }
}
