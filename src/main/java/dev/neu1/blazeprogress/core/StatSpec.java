package dev.neu1.blazeprogress.core;

/** Target requirement for a derived stat (intelligence, effective health, ...). */
public final class StatSpec {

    public final String label; // e.g. "Intelligence"
    public final String key;   // "intelligence" | "ehp"
    public final int target;   // value considered 100%
    public final double weight;

    public StatSpec(String label, String key, int target, double weight) {
        this.label = label;
        this.key = key;
        this.target = target;
        this.weight = weight;
    }
}
