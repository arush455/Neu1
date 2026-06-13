package dev.neu1.blazeprogress.core;

import java.util.Set;

/** Built-in target setups. Used when no config file exists yet. */
public final class DefaultSetups {

    private DefaultSetups() {}

    /**
     * The standard Mage / Inferno build for Blaze Slayer (Inferno Demonlord) T4:
     * full master-starred Crimson armour (Necrotic), Fire Veil Wand + Pyrochaos
     * Dagger to trigger the Inferno effect, and a level-200 Golden Dragon.
     *
     * Everything here is editable in config/blazeprogress.json — the meta moves,
     * so treat these as sane defaults, not gospel.
     */
    public static TargetSetup mageInfernoT4() {
        TargetSetup t = new TargetSetup();
        t.name = "Mage Inferno — Blaze Slayer T4";
        t.archetype = Archetype.MAGE;

        t.helmet = new ItemSpec("Crimson Helmet", Set.of("CRIMSON_HELMET"),
                "necrotic", 5, KuudraTier.CRIMSON, 1.0);
        t.chestplate = new ItemSpec("Crimson Chestplate", Set.of("CRIMSON_CHESTPLATE"),
                "necrotic", 5, KuudraTier.CRIMSON, 1.0);
        t.leggings = new ItemSpec("Crimson Leggings", Set.of("CRIMSON_LEGGINGS"),
                "necrotic", 5, KuudraTier.CRIMSON, 1.0);
        t.boots = new ItemSpec("Crimson Boots", Set.of("CRIMSON_BOOTS"),
                "necrotic", 5, KuudraTier.CRIMSON, 1.0);

        t.weapons.add(new ItemSpec("Fire Veil Wand", Set.of("FIRE_VEIL_WAND"),
                null, null, null, 1.0));
        t.weapons.add(new ItemSpec("Pyrochaos Dagger", Set.of("PYROCHAOS_DAGGER"),
                null, null, null, 1.0));

        t.pet = new PetSpec("Golden Dragon Pet", "GOLDEN_DRAGON", "LEGENDARY", 200, 2.0);

        t.stats.add(new StatSpec("Intelligence", "intelligence", 1500, 0.5));

        return t;
    }
}
