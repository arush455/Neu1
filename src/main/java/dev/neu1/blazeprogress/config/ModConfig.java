package dev.neu1.blazeprogress.config;

import dev.neu1.blazeprogress.core.DefaultSetups;
import dev.neu1.blazeprogress.core.TargetSetup;

/** Serialised to {@code config/blazeprogress.json}. */
public final class ModConfig {

    public boolean overlayEnabled = true;
    public boolean showDetails = false; // false = compact HUD, true = per-piece breakdown
    public int x = 4;
    public int y = 4;
    public double scale = 1.0;

    public TargetSetup target;

    public static ModConfig defaults() {
        ModConfig c = new ModConfig();
        c.target = DefaultSetups.mageInfernoT4();
        return c;
    }
}
