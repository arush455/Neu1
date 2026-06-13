package dev.neu1.blazeprogress.core;

import java.util.List;

/** Result for a single scored component (one armour piece, weapon, pet, stat). */
public final class ComponentResult {

    public final String name;
    public final double score;     // 0..1
    public final double weight;
    public final boolean measured; // false => excluded from the overall % (couldn't read it)
    public final String status;    // short summary, e.g. "missing", "80%"
    public final List<String> details;

    public ComponentResult(String name, double score, double weight, boolean measured,
                           String status, List<String> details) {
        this.name = name;
        this.score = score;
        this.weight = weight;
        this.measured = measured;
        this.status = status;
        this.details = details;
    }

    public int percent() {
        return (int) Math.round(score * 100);
    }
}
