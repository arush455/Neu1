package dev.neu1.blazeprogress.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure comparison engine. No Minecraft dependencies — feed it a
 * {@link GearSnapshot} and a {@link TargetSetup} and it produces a
 * {@link ComparisonResult}. This is the piece that is unit-tested.
 */
public final class SetupComparator {

    public ComparisonResult compare(GearSnapshot snap, TargetSetup target) {
        List<ComponentResult> comps = new ArrayList<>();

        comps.add(scoreItem("Helmet", snap.helmet, target.helmet));
        comps.add(scoreItem("Chestplate", snap.chestplate, target.chestplate));
        comps.add(scoreItem("Leggings", snap.leggings, target.leggings));
        comps.add(scoreItem("Boots", snap.boots, target.boots));

        for (ItemSpec w : target.weapons) {
            ItemSnapshot cur = snap.findItem(w.acceptableIds);
            comps.add(scoreItem(w.label, cur, w));
        }

        if (target.pet != null) {
            if (!snap.petScanned) {
                comps.add(new ComponentResult(target.pet.label, 0, target.pet.weight, false,
                        "open Pets menu to scan",
                        List.of("Open your in-game Pets menu once so the mod can read your active pet.")));
            } else {
                comps.add(scorePet(target.pet, snap.pet));
            }
        }

        for (StatSpec s : target.stats) {
            Integer cur = readStat(snap, s.key);
            if (cur == null) {
                comps.add(new ComponentResult(s.label, 0, s.weight, false, "not measured",
                        List.of("Stat not readable yet (no action-bar data captured).")));
            } else {
                double sc = Math.min(1.0, cur / (double) s.target);
                comps.add(new ComponentResult(s.label, sc, s.weight, true,
                        cur + "/" + s.target, List.of()));
            }
        }

        return new ComparisonResult(target.name, comps);
    }

    private static Integer readStat(GearSnapshot snap, String key) {
        if ("intelligence".equalsIgnoreCase(key)) return snap.intelligence;
        if ("ehp".equalsIgnoreCase(key)) return snap.effectiveHealth;
        return null;
    }

    /** Score a single armour piece or weapon against its spec. */
    ComponentResult scoreItem(String slot, ItemSnapshot cur, ItemSpec spec) {
        if (spec == null) {
            return new ComponentResult(slot, 1, 0, false, "no target", List.of());
        }
        if (cur == null) cur = ItemSnapshot.EMPTY;

        List<String> details = new ArrayList<>();
        String label = spec.label;

        if (cur.isEmpty()) {
            details.add("Missing — need " + label);
            return new ComponentResult(label, 0, spec.weight, true, "missing", details);
        }

        boolean idOk = spec.acceptableIds.contains(cur.skyblockId);
        double totalW = 0;
        double got = 0;

        totalW += spec.idWeight;
        if (idOk) got += spec.idWeight;
        details.add(mark(idOk) + " item: " + cur.displayName + (idOk ? "" : " (need " + label + ")"));

        if (spec.desiredReforge != null) {
            totalW += spec.reforgeWeight;
            boolean ok = idOk && spec.desiredReforge.equalsIgnoreCase(cur.reforge);
            if (ok) got += spec.reforgeWeight;
            details.add(mark(ok) + " reforge: " + (cur.reforge == null ? "none" : cur.reforge)
                    + " (want " + spec.desiredReforge + ")");
        }

        if (spec.desiredStars != null && spec.desiredStars > 0) {
            totalW += spec.starsWeight;
            double r = idOk ? Math.min(1.0, cur.stars / (double) spec.desiredStars) : 0;
            got += spec.starsWeight * r;
            details.add(mark(r >= 1) + " stars: " + cur.stars + "/" + spec.desiredStars);
        }

        if (spec.desiredTier != null) {
            totalW += spec.tierWeight;
            double r = (idOk && cur.kuudraTier != null)
                    ? Math.min(1.0, cur.kuudraTier.rank / (double) spec.desiredTier.rank) : 0;
            got += spec.tierWeight * r;
            details.add(mark(r >= 1) + " tier: " + (cur.kuudraTier == null ? "?" : cur.kuudraTier)
                    + " (want " + spec.desiredTier + ")");
        }

        double score = totalW > 0 ? got / totalW : 0;
        return new ComponentResult(label, score, spec.weight, true,
                (int) Math.round(score * 100) + "%", details);
    }

    ComponentResult scorePet(PetSpec spec, PetSnapshot cur) {
        List<String> details = new ArrayList<>();
        if (cur == null || cur.type == null) {
            details.add("No active pet detected (want " + spec.desiredType + ")");
            return new ComponentResult(spec.label, 0, spec.weight, true, "none", details);
        }

        boolean typeOk = spec.desiredType.equalsIgnoreCase(cur.type);
        double score;
        if (!typeOk) {
            score = 0;
            details.add(mark(false) + " pet: " + cur.displayName + " (want " + spec.desiredType + ")");
        } else {
            double lvl = spec.desiredLevel > 0 ? Math.min(1.0, cur.level / (double) spec.desiredLevel) : 1;
            boolean tierOk = spec.desiredTier == null || spec.desiredTier.equalsIgnoreCase(cur.tier);
            score = 0.2 + 0.6 * lvl + 0.2 * (tierOk ? 1 : 0);
            details.add(mark(true) + " pet: " + spec.desiredType);
            details.add(mark(lvl >= 1) + " level: " + cur.level + "/" + spec.desiredLevel);
            if (spec.desiredTier != null) {
                details.add(mark(tierOk) + " tier: " + cur.tier + " (want " + spec.desiredTier + ")");
            }
        }
        return new ComponentResult(spec.label, score, spec.weight, true,
                (int) Math.round(score * 100) + "%", details);
    }

    private static String mark(boolean ok) {
        return ok ? "✔" : "✘";
    }
}
