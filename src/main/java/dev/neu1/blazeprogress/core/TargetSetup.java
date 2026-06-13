package dev.neu1.blazeprogress.core;

import java.util.ArrayList;
import java.util.List;

/** The "perfect" setup to compare against. Fully serialisable to JSON config. */
public final class TargetSetup {

    public String name;
    public Archetype archetype;

    public ItemSpec helmet;
    public ItemSpec chestplate;
    public ItemSpec leggings;
    public ItemSpec boots;

    public List<ItemSpec> weapons = new ArrayList<>();
    public PetSpec pet;
    public List<StatSpec> stats = new ArrayList<>();
}
