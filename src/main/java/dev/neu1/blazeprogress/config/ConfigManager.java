package dev.neu1.blazeprogress.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.neu1.blazeprogress.core.DefaultSetups;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Loads/saves the JSON config and exposes the active {@link ModConfig}. */
public final class ConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("blazeprogress");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ModConfig config;

    private ConfigManager() {}

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("blazeprogress.json");
    }

    public static ModConfig get() {
        if (config == null) load();
        return config;
    }

    public static void load() {
        Path p = path();
        ModConfig loaded = null;
        try {
            if (Files.exists(p)) {
                loaded = GSON.fromJson(Files.readString(p), ModConfig.class);
            }
        } catch (Exception e) {
            LOGGER.warn("[BlazeProgress] Failed to read config, using defaults", e);
        }
        if (loaded == null) loaded = ModConfig.defaults();
        if (loaded.target == null) loaded.target = DefaultSetups.mageInfernoT4();
        config = loaded;
        save();
    }

    public static void save() {
        try {
            Path p = path();
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(config));
        } catch (IOException e) {
            LOGGER.warn("[BlazeProgress] Failed to write config", e);
        }
    }
}
