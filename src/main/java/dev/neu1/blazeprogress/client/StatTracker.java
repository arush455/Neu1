package dev.neu1.blazeprogress.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort stat reader. SkyBlock prints "…  830/830✎ Mana  …" on the action
 * bar; max mana ≈ 100 + intelligence, so we derive intelligence from it.
 */
public final class StatTracker {

    // Matches "<cur>/<max>✎" (the mana group). Commas are stripped first.
    private static final Pattern MANA = Pattern.compile("(\\d+)/(\\d+)\\s*✎");

    private static volatile Integer intelligence; // null = unknown

    private StatTracker() {}

    public static Integer intelligence() {
        return intelligence;
    }

    /** Feed every action-bar message here. */
    public static void onActionBar(String raw) {
        if (raw == null || raw.isEmpty()) return;
        String text = raw.replace(",", "");
        Matcher m = MANA.matcher(text);
        if (m.find()) {
            int maxMana = Integer.parseInt(m.group(2));
            intelligence = Math.max(0, maxMana - 100);
        }
    }
}
