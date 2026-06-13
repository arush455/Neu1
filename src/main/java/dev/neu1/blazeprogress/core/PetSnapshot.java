package dev.neu1.blazeprogress.core;

/** Immutable view of the player's currently active pet. */
public final class PetSnapshot {

    public final String type;        // e.g. "GOLDEN_DRAGON" (nullable if unparsed)
    public final String tier;        // e.g. "LEGENDARY" (nullable)
    public final int level;          // 0..200
    public final String displayName;

    public PetSnapshot(String type, String tier, int level, String displayName) {
        this.type = type;
        this.tier = tier;
        this.level = level;
        this.displayName = displayName == null ? "<unknown>" : displayName;
    }
}
