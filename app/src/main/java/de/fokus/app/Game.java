package de.fokus.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** XP, Level, Serie, Tagesziele und Challenge. */
final class Game {
    private Game() {}

    static final int XP_GOAL = 25;
    static final int XP_ALL_GOALS = 25;
    static final int XP_CHALLENGE = 20;
    static final int MAX_GOALS = 3;

    static final class Goal {
        String text;
        boolean done;
        boolean rewarded;

        Goal(String text, boolean done, boolean rewarded) {
            this.text = text;
            this.done = done;
            this.rewarded = rewarded;
        }
    }

    static String today() { return LocalDate.now().toString(); }

    static String yesterday() { return LocalDate.now().minusDays(1).toString(); }

    // ---------- XP & Level ----------
    static int xp(Context c) { return Prefs.p(c).getInt("xp", 0); }

    static int level(int xp) { return (int) Math.floor(Math.sqrt(xp / 50.0)) + 1; }

    static int xpForLevel(int level) { return (level - 1) * (level - 1) * 50; }

    /** Gibt das neue Level zurück, falls ein Level-Up passiert ist, sonst 0. */
    static int addXp(Context c, int amount) {
        int before = xp(c);
        int after = before + amount;
        Prefs.p(c).edit().putInt("xp", after).apply();
        int lb = level(before), la = level(after);
        return la > lb ? la : 0;
    }

    // ---------- Serie ----------
    static int streak(Context c) {
        SharedPreferences p = Prefs.p(c);
        String last = p.getString("streak_day", "");
        if (last.equals(today()) || last.equals(yesterday())) return p.getInt("streak", 0);
        return 0;
    }

    static int bestStreak(Context c) { return Prefs.p(c).getInt("best_streak", 0); }

    /** Zählt den heutigen Tag für die Serie. Gibt Bonus-XP zurück (0, wenn heute schon gezählt). */
    static int markActiveToday(Context c) {
        SharedPreferences p = Prefs.p(c);
        String last = p.getString("streak_day", "");
        if (last.equals(today())) return 0;
        int s = last.equals(yesterday()) ? p.getInt("streak", 0) + 1 : 1;
        int best = Math.max(s, p.getInt("best_streak", 0));
        p.edit().putString("streak_day", today()).putInt("streak", s).putInt("best_streak", best).apply();
        return Math.min(s * 5, 50);
    }

    // ---------- Tagesziele ----------
    static List<Goal> goals(Context c) {
        List<Goal> list = new ArrayList<>();
        SharedPreferences p = Prefs.p(c);
        if (!today().equals(p.getString("goals_day", ""))) return list;
        for (String line : p.getString("goals", "").split("\n")) {
            if (line.length() < 5) continue;
            String[] parts = line.split("\\|", 3);
            if (parts.length < 3) continue;
            list.add(new Goal(parts[2], "1".equals(parts[0]), "1".equals(parts[1])));
        }
        return list;
    }

    static void saveGoals(Context c, List<Goal> goals) {
        StringBuilder sb = new StringBuilder();
        for (Goal g : goals) {
            sb.append(g.done ? "1" : "0").append('|')
              .append(g.rewarded ? "1" : "0").append('|')
              .append(g.text.replace("\n", " ")).append('\n');
        }
        Prefs.p(c).edit().putString("goals_day", today()).putString("goals", sb.toString()).apply();
        if (goals.isEmpty()) Prefs.p(c).edit().putString("goals", "").apply();
    }

    // ---------- Challenge ----------
    static boolean challengeDone(Context c) {
        return today().equals(Prefs.p(c).getString("challenge_day", ""));
    }

    static void setChallengeDone(Context c) {
        Prefs.p(c).edit().putString("challenge_day", today()).apply();
    }

    // ---------- Akzentfarbe ----------
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
