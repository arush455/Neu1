package dev.neu1.blazeprogress.core;

import java.util.ArrayList;
import java.util.List;

/** Outcome of comparing a {@link GearSnapshot} against a {@link TargetSetup}. */
public final class ComparisonResult {

    public final String setupName;
    public final List<ComponentResult> components;

    public ComparisonResult(String setupName, List<ComponentResult> components) {
        this.setupName = setupName;
        this.components = components;
    }

    /** Weighted average over the components we could actually measure (0..1). */
    public double overallScore() {
        double totalWeight = 0;
        double got = 0;
        for (ComponentResult c : components) {
            if (!c.measured) continue;
            totalWeight += c.weight;
            got += c.weight * c.score;
        }
        return totalWeight > 0 ? got / totalWeight : 0;
    }

    public int overallPercent() {
        return (int) Math.round(overallScore() * 100);
    }

    /** A simple unicode bar of fixed width for the given 0..1 fraction. */
    public static String bar(double fraction, int width) {
        int filled = (int) Math.round(Math.max(0, Math.min(1, fraction)) * width);
        StringBuilder sb = new StringBuilder(width);
        for (int i = 0; i < width; i++) sb.append(i < filled ? '▰' : '▱');
        return sb.toString();
    }

    /** Minecraft-formatted lines for printing to chat (uses section colour codes). */
    public List<String> toChatLines() {
        List<String> out = new ArrayList<>();
        out.add("§6§lBlazeProgress §r§7— §f" + setupName);
        out.add("§eOverall: " + colour(overallScore()) + overallPercent()
                + "%  §7" + bar(overallScore(), 10));
        for (ComponentResult c : components) {
            String value = c.measured ? colour(c.score) + c.percent() + "%" : "§8n/a";
            out.add(" §7- §f" + c.name + ": " + value + " §8(" + c.status + ")");
        }
        return out;
    }

    private static String colour(double score) {
        if (score >= 0.999) return "§a"; // green
        if (score >= 0.6) return "§e";   // yellow
        if (score >= 0.3) return "§6";   // gold
        return "§c";                       // red
    }
}
