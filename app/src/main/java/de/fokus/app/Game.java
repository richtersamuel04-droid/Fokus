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
