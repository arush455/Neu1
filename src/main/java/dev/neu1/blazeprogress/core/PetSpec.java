package dev.neu1.blazeprogress.core;

/** Target requirement for the active pet. */
public final class PetSpec {

    public final String label;        // e.g. "Golden Dragon Pet"
    public final String desiredType;  // e.g. "GOLDEN_DRAGON"
    public final String desiredTier;  // e.g. "LEGENDARY" (nullable)
    public final int desiredLevel;    // e.g. 200
    public final double weight;

    public PetSpec(String label, String desiredType, String desiredTier, int desiredLevel, double weight) {
        this.label = label;
        this.desiredType = desiredType;
        this.desiredTier = desiredTier;
        this.desiredLevel = desiredLevel;
        this.weight = weight;
    }
}
