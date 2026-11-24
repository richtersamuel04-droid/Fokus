package de.fokus.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

final class Prefs {
    private Prefs() {}

    static SharedPreferences p(Context c) {
        return c.getSharedPreferences("fokus", Context.MODE_PRIVATE);
    }

    static List<String> homeApps(Context c) {
        List<String> list = new ArrayList<>();
        for (String s : p(c).getString("home_apps", "").split(",")) {
            if (!s.trim().isEmpty()) list.add(s.trim());
        }
        return list;
    }

    static void setHomeApps(Context c, List<String> apps) {
        p(c).edit().putString("home_apps", String.join(",", apps)).apply();
    }

    static int blockCount(Context c) {
        return p(c).getInt("blocks", 0);
    }

    static void incBlockCount(Context c) {
        p(c).edit().putInt("blocks", blockCount(c) + 1).apply();
    }
}
