package de.fokus.app;

import android.content.Context;

import java.util.LinkedHashSet;
import java.util.Set;

/** Sammelt Ansichts-IDs aus Instagram, um die Reels-Erkennung zu verbessern. Bleibt auf dem Handy. */
final class Diagnose {
    private Diagnose() {}

    private static final Set<String> seen = new LinkedHashSet<>();
    private static long lastSave = 0;

    static boolean on(Context c) { return Prefs.p(c).getBoolean("diag_on", false); }

    static void setOn(Context c, boolean on) { Prefs.p(c).edit().putBoolean("diag_on", on).apply(); }

    static synchronized void record(Context c, String id, int heightPct, boolean selected) {
        String shortId = id.contains(":id/") ? id.substring(id.indexOf(":id/") + 4) : id;
        int bucket = (heightPct / 10) * 10;
        String line = shortId + "  " + bucket + "%" + (selected ? "  [ausgewählt]" : "");
        if (seen.size() < 600 && seen.add(line)) {
            long now = System.currentTimeMillis();
            if (now - lastSave > 2000) {
                lastSave = now;
                save(c);
            }
        }
    }

    private static void save(Context c) {
        StringBuilder sb = new StringBuilder(Prefs.p(c).getString("diag", ""));
        for (String l : seen) {
            if (sb.indexOf(l + "\n") < 0) sb.append(l).append('\n');
        }
        String out = sb.length() > 40_000 ? sb.substring(sb.length() - 40_000) : sb.toString();
        Prefs.p(c).edit().putString("diag", out).apply();
    }

    static String text(Context c) {
        synchronized (Diagnose.class) {
            save(c);
        }
        return Prefs.p(c).getString("diag", "");
    }

    static synchronized void clear(Context c) {
        seen.clear();
        Prefs.p(c).edit().putString("diag", "").apply();
    }
}
