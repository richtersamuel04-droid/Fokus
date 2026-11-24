package de.fokus.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Tagesziele und Akzentfarbe. */
final class Game {
    private Game() {}

    static final int MAX_GOALS = 3;

    static final class Goal {
        String text;
        boolean done;

        Goal(String text, boolean done) {
            this.text = text;
            this.done = done;
        }
    }

    static String today() { return LocalDate.now().toString(); }

    static List<Goal> goals(Context c) {
        List<Goal> list = new ArrayList<>();
        SharedPreferences p = Prefs.p(c);
        if (!today().equals(p.getString("goals_day", ""))) return list;
        for (String line : p.getString("goals", "").split("\n")) {
            String[] parts = line.split("\\|");
            if (parts.length < 2) continue;
            // altes Format "d|r|text" und neues Format "d|text"
            String text = parts[parts.length - 1];
            if (text.trim().isEmpty()) continue;
            list.add(new Goal(text, "1".equals(parts[0])));
        }
        return list;
    }

    static void saveGoals(Context c, List<Goal> goals) {
        StringBuilder sb = new StringBuilder();
        for (Goal g : goals) {
            sb.append(g.done ? "1" : "0").append('|')
              .append(g.text.replace("\n", " ").replace("|", "/")).append('\n');
        }
        Prefs.p(c).edit().putString("goals_day", today()).putString("goals", sb.toString()).apply();
        boolean any = false;
        for (Goal g : goals) any |= g.done;
        setDoneOn(c, today(), any);
    }

    // ---------- Heute wichtig ----------
    static String important(Context c) {
        SharedPreferences p = Prefs.p(c);
        return today().equals(p.getString("important_day", "")) ? p.getString("important", "") : "";
    }

    static void setImportant(Context c, String text) {
        Prefs.p(c).edit().putString("important_day", today()).putString("important", text).apply();
    }

    // ---------- Ruhiger Start ----------
    /** Stunde, bis zu der der ruhige Start gilt; -1 = aus. */
    static int quietUntil(Context c) { return Prefs.p(c).getInt("quiet_until", 8); }

    static void setQuietUntil(Context c, int hour) { Prefs.p(c).edit().putInt("quiet_until", hour).apply(); }

    // ---------- Abendmodus ----------
    /** Stunde, ab der der Abendmodus gilt (bis 5 Uhr); -1 = aus. */
    static int eveningFrom(Context c) { return Prefs.p(c).getInt("evening_from", 22); }

    static void setEveningFrom(Context c, int hour) { Prefs.p(c).edit().putInt("evening_from", hour).apply(); }

    static boolean eveningActive(Context c) {
        int from = eveningFrom(c);
        if (from < 0) return false;
        int h = java.time.LocalTime.now().getHour();
        return h >= from || h < 5;
    }

    // ---------- Wochenleiste ----------
    static boolean doneOn(Context c, String day) {
        return ("," + Prefs.p(c).getString("done_days", "") + ",").contains("," + day + ",");
    }

    private static void setDoneOn(Context c, String day, boolean done) {
        List<String> days = new ArrayList<>();
        for (String d : Prefs.p(c).getString("done_days", "").split(",")) {
            if (!d.isEmpty() && !d.equals(day)) days.add(d);
        }
        if (done) days.add(day);
        while (days.size() > 60) days.remove(0);
        Prefs.p(c).edit().putString("done_days", String.join(",", days)).apply();
    }

    // ---------- Fokus-Timer ----------
    static long focusEnd(Context c) { return Prefs.p(c).getLong("focus_end", 0); }

    static long focusLength(Context c) { return Prefs.p(c).getLong("focus_len", 1); }

    static boolean focusActive(Context c) { return focusEnd(c) > System.currentTimeMillis(); }

    static void startFocus(Context c, int minutes) {
        long len = minutes * 60_000L;
        Prefs.p(c).edit().putLong("focus_end", System.currentTimeMillis() + len)
                .putLong("focus_len", len).putBoolean("focus_review", true).apply();
    }

    static void stopFocus(Context c) {
        Prefs.p(c).edit().putLong("focus_end", 0).putBoolean("focus_review", false).apply();
    }

    /** Fokusblock ist abgelaufen und wurde noch nicht ausgewertet. */
    static boolean focusReview(Context c) {
        return Prefs.p(c).getBoolean("focus_review", false) && !focusActive(c);
    }

    static void clearReview(Context c) {
        Prefs.p(c).edit().putBoolean("focus_review", false).putLong("focus_end", 0).apply();
    }

    static boolean autoColor(Context c) { return Prefs.p(c).getBoolean("auto_color", false); }

    static void setAutoColor(Context c, boolean on) { Prefs.p(c).edit().putBoolean("auto_color", on).apply(); }

    static void setColorIndex(Context c, int i) { Prefs.p(c).edit().putInt("color", i).apply(); }

    static int colorIndex(Context c) {
        if (autoColor(c)) return Content.autoColorIndex();
        int i = Prefs.p(c).getInt("color", 0);
        return (i >= 0 && i < Content.COLORS.length) ? i : 0;
    }

    static int accent(Context c) { return Content.COLORS[colorIndex(c)]; }
}
