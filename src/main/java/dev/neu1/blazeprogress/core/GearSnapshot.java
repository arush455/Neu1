package dev.neu1.blazeprogress.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Everything the mod could read about the player's current state. */
public final class GearSnapshot {

    public ItemSnapshot helmet = ItemSnapshot.EMPTY;
    public ItemSnapshot chestplate = ItemSnapshot.EMPTY;
    public ItemSnapshot leggings = ItemSnapshot.EMPTY;
    public ItemSnapshot boots = ItemSnapshot.EMPTY;

    /** Every SkyBlock item found in the inventory (used to locate weapons). */
    public final List<ItemSnapshot> inventory = new ArrayList<>();

    public PetSnapshot pet;          // null = unknown
    public boolean petScanned = false;

    public Integer intelligence;     // null = not measured
    public Integer effectiveHealth;  // null = not measured

    /** First inventory item whose id is in {@code ids}, or null. */
    public ItemSnapshot findItem(Set<String> ids) {
        for (ItemSnapshot it : inventory) {
            if (it.skyblockId != null && ids.contains(it.skyblockId)) {
                return it;
            }
        }
        return null;
    }
}
