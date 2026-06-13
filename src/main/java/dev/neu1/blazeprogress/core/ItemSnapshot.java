package dev.neu1.blazeprogress.core;

/** Immutable view of a single item the player currently owns / wears. */
public final class ItemSnapshot {

    public static final ItemSnapshot EMPTY = new ItemSnapshot(null, null, 0, null, "<empty>");

    public final String skyblockId;   // ExtraAttributes.id, e.g. "CRIMSON_HELMET" (null if not a SkyBlock item)
    public final String reforge;      // ExtraAttributes.modifier, e.g. "necrotic" (nullable)
    public final int stars;           // upgrade_level / dungeon_item_level
    public final KuudraTier kuudraTier; // nullable (only set for Kuudra armour)
    public final String displayName;

    public ItemSnapshot(String skyblockId, String reforge, int stars, KuudraTier kuudraTier, String displayName) {
        this.skyblockId = skyblockId;
        this.reforge = reforge;
        this.stars = stars;
        this.kuudraTier = kuudraTier;
        this.displayName = displayName == null ? "<unknown>" : displayName;
    }

    public boolean isEmpty() {
        return skyblockId == null;
    }
}
