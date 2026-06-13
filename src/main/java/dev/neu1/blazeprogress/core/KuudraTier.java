package dev.neu1.blazeprogress.core;

/**
 * Upgrade tier of Kuudra (Crimson-family) armour. The lower tiers carry a
 * prefix word in the item's display name; a fully upgraded piece has none.
 */
public enum KuudraTier {
    HOT(1),
    BURNING(2),
    FIERY(3),
    INFERNAL(4),
    CRIMSON(5);

    public final int rank;

    KuudraTier(int rank) {
        this.rank = rank;
    }

    /**
     * Infer the tier from a (display) item name. Only meaningful for
     * Kuudra-family armour pieces.
     */
    public static KuudraTier fromDisplayName(String name) {
        if (name == null) return null;
        String n = name.toLowerCase();
        if (n.contains("infernal")) return INFERNAL;
        if (n.contains("fiery")) return FIERY;
        if (n.contains("burning")) return BURNING;
        if (n.contains("hot ")) return HOT; // trailing space avoids "shot", "ghost", etc.
        return CRIMSON; // no lower-tier prefix => maxed
    }
}
