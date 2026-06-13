package dev.neu1.blazeprogress.core;

/**
 * Standalone sanity check for the pure comparison engine. Compiled and run with
 * plain javac (no Minecraft, no Gradle) to verify scoring behaviour.
 *
 *   javac -d /tmp/bp $(find src/main/java/dev/neu1/blazeprogress/core -name '*.java') \
 *         selftest/dev/neu1/blazeprogress/core/CoreSelfTest.java
 *   java -cp /tmp/bp dev.neu1.blazeprogress.core.CoreSelfTest
 */
public final class CoreSelfTest {

    private static int failures = 0;

    public static void main(String[] args) {
        TargetSetup target = DefaultSetups.mageInfernoT4();
        SetupComparator cmp = new SetupComparator();

        // 1) Empty player -> 0% over the measurable components.
        GearSnapshot empty = new GearSnapshot();
        ComparisonResult emptyRes = cmp.compare(empty, target);
        printReport("EMPTY", emptyRes);
        assertEquals("empty overall", 0, emptyRes.overallPercent());

        // 2) Perfect player -> exactly 100%.
        ComparisonResult perfectRes = cmp.compare(perfect(), target);
        printReport("PERFECT", perfectRes);
        assertEquals("perfect overall", 100, perfectRes.overallPercent());

        // 3) Partial player -> strictly between 0 and 100.
        ComparisonResult partialRes = cmp.compare(partial(), target);
        printReport("PARTIAL", partialRes);
        assertTrue("partial > 0", partialRes.overallPercent() > 0);
        assertTrue("partial < 100", partialRes.overallPercent() < 100);

        if (failures == 0) {
            System.out.println("\nALL TESTS PASSED");
        } else {
            System.out.println("\n" + failures + " TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static GearSnapshot perfect() {
        GearSnapshot s = new GearSnapshot();
        s.helmet = new ItemSnapshot("CRIMSON_HELMET", "necrotic", 5, KuudraTier.CRIMSON, "Crimson Helmet");
        s.chestplate = new ItemSnapshot("CRIMSON_CHESTPLATE", "necrotic", 5, KuudraTier.CRIMSON, "Crimson Chestplate");
        s.leggings = new ItemSnapshot("CRIMSON_LEGGINGS", "necrotic", 5, KuudraTier.CRIMSON, "Crimson Leggings");
        s.boots = new ItemSnapshot("CRIMSON_BOOTS", "necrotic", 5, KuudraTier.CRIMSON, "Crimson Boots");
        s.inventory.add(new ItemSnapshot("FIRE_VEIL_WAND", null, 0, null, "Fire Veil Wand"));
        s.inventory.add(new ItemSnapshot("PYROCHAOS_DAGGER", null, 0, null, "Pyrochaos Dagger"));
        s.pet = new PetSnapshot("GOLDEN_DRAGON", "LEGENDARY", 200, "[Lvl 200] Golden Dragon");
        s.petScanned = true;
        s.intelligence = 1500;
        return s;
    }

    private static GearSnapshot partial() {
        GearSnapshot s = new GearSnapshot();
        // Full helmet, a half-starred wrong-reforge chestplate, nothing else.
        s.helmet = new ItemSnapshot("CRIMSON_HELMET", "necrotic", 5, KuudraTier.CRIMSON, "Crimson Helmet");
        s.chestplate = new ItemSnapshot("CRIMSON_CHESTPLATE", "wise", 2, KuudraTier.FIERY, "Fiery Crimson Chestplate");
        s.inventory.add(new ItemSnapshot("PYROCHAOS_DAGGER", null, 0, null, "Pyrochaos Dagger"));
        s.pet = new PetSnapshot("ENDER_DRAGON", "EPIC", 100, "[Lvl 100] Ender Dragon");
        s.petScanned = true;
        s.intelligence = 600;
        return s;
    }

    private static void printReport(String tag, ComparisonResult res) {
        System.out.println("=== " + tag + " === overall " + res.overallPercent() + "%");
        for (ComponentResult c : res.components) {
            System.out.printf("   %-18s %s  (w=%.1f, measured=%s)%n",
                    c.name, c.measured ? c.percent() + "%" : "n/a", c.weight, c.measured);
        }
    }

    private static void assertEquals(String what, int expected, int actual) {
        if (expected != actual) {
            failures++;
            System.out.println("FAIL: " + what + " expected " + expected + " got " + actual);
        }
    }

    private static void assertTrue(String what, boolean cond) {
        if (!cond) {
            failures++;
            System.out.println("FAIL: " + what);
        }
    }
}
