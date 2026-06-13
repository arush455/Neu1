package dev.neu1.blazeprogress.client;

import dev.neu1.blazeprogress.config.ConfigManager;
import dev.neu1.blazeprogress.config.ModConfig;
import dev.neu1.blazeprogress.core.ComparisonResult;
import dev.neu1.blazeprogress.core.ComponentResult;
import dev.neu1.blazeprogress.core.GearSnapshot;
import dev.neu1.blazeprogress.core.SetupComparator;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.List;

/** Renders the compact progress overlay each frame. */
public final class ProgressHud {

    private static final int COL_TITLE = 0xFFFFAA00;
    private static final int COL_GREEN = 0xFF55FF55;
    private static final int COL_YELLOW = 0xFFFFFF55;
    private static final int COL_GOLD = 0xFFFFAA00;
    private static final int COL_RED = 0xFFFF5555;
    private static final int COL_GREY = 0xFFAAAAAA;
    private static final int COL_BG = 0x90000000;

    private static final long SCAN_INTERVAL_MS = 1000;

    private static GearSnapshot cached;
    private static long lastScan;

    private ProgressHud() {}

    public static void register() {
        HudRenderCallback.EVENT.register(ProgressHud::render);
    }

    /** Builds a fresh comparison from the latest scan (used by the chat command too). */
    public static ComparisonResult currentComparison(MinecraftClient mc) {
        refresh(mc, true);
        return new SetupComparator().compare(cached, ConfigManager.get().target);
    }

    private static void refresh(MinecraftClient mc, boolean force) {
        long now = System.currentTimeMillis();
        if (cached == null || force || now - lastScan > SCAN_INTERVAL_MS) {
            cached = GearScanner.scan(mc);
            cached.pet = PetTracker.current();
            cached.petScanned = PetTracker.hasScanned();
            cached.intelligence = StatTracker.intelligence();
            lastScan = now;
        }
    }

    private static void render(DrawContext ctx, RenderTickCounter counter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ModConfig cfg = ConfigManager.get();
        if (!cfg.overlayEnabled || mc.player == null || mc.world == null || mc.options.hudHidden) {
            return;
        }
        refresh(mc, false);

        ComparisonResult res = new SetupComparator().compare(cached, cfg.target);
        List<Line> lines = buildLines(res, cfg.showDetails);
        draw(ctx, mc.textRenderer, cfg, lines);
    }

    private static List<Line> buildLines(ComparisonResult res, boolean details) {
        List<Line> lines = new ArrayList<>();
        lines.add(new Line("BlazeProgress — " + res.setupName, COL_TITLE));
        double overall = res.overallScore();
        lines.add(new Line("Overall " + res.overallPercent() + "%  "
                + ComparisonResult.bar(overall, 10), colourFor(overall)));

        for (ComponentResult c : res.components) {
            String value = c.measured ? c.percent() + "%" : "n/a";
            lines.add(new Line(" - " + c.name + ": " + value
                    + (c.measured ? "" : " (" + c.status + ")"),
                    c.measured ? colourFor(c.score) : COL_GREY));
            if (details && c.measured) {
                for (String d : c.details) {
                    lines.add(new Line("     " + d, COL_GREY));
                }
            }
        }
        return lines;
    }

    private static void draw(DrawContext ctx, TextRenderer font, ModConfig cfg, List<Line> lines) {
        int pad = 3;
        int lineH = font.fontHeight + 1;
        int width = 0;
        for (Line l : lines) width = Math.max(width, font.getWidth(l.text));

        MatrixStack matrices = ctx.getMatrices();
        matrices.push();
        matrices.translate(cfg.x, cfg.y, 0);
        if (cfg.scale != 1.0) matrices.scale((float) cfg.scale, (float) cfg.scale, 1f);

        int boxW = width + pad * 2;
        int boxH = lines.size() * lineH + pad * 2 - 1;
        ctx.fill(0, 0, boxW, boxH, COL_BG);

        int y = pad;
        for (Line l : lines) {
            ctx.drawText(font, l.text, pad, y, l.color, true);
            y += lineH;
        }
        matrices.pop();
    }

    private static int colourFor(double score) {
        if (score >= 0.999) return COL_GREEN;
        if (score >= 0.6) return COL_YELLOW;
        if (score >= 0.3) return COL_GOLD;
        return COL_RED;
    }

    private record Line(String text, int color) {}
}
