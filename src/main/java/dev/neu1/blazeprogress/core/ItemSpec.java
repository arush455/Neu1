package dev.neu1.blazeprogress.core;

import java.util.Set;

/**
 * A target requirement for one gear slot / weapon. Only the sub-criteria that
 * are non-null are scored; their relative importance is controlled by the
 * sub-weights.
 */
public final class ItemSpec {

    public final String label;            // human readable, e.g. "Crimson Helmet"
    public final Set<String> acceptableIds; // SkyBlock ids that satisfy this slot
    public final String desiredReforge;   // nullable
    public final Integer desiredStars;    // nullable
    public final KuudraTier desiredTier;  // nullable
    public final double weight;           // importance of this slot in the overall score

    // Relative weight of each sub-criterion (item presence always counts).
    public double idWeight = 0.5;
    public double reforgeWeight = 0.2;
    public double starsWeight = 0.2;
    public double tierWeight = 0.1;

    public ItemSpec(String label, Set<String> acceptableIds, String desiredReforge,
                    Integer desiredStars, KuudraTier desiredTier, double weight) {
        this.label = label;
        this.acceptableIds = acceptableIds;
        this.desiredReforge = desiredReforge;
        this.desiredStars = desiredStars;
        this.desiredTier = desiredTier;
        this.weight = weight;
    }
}
