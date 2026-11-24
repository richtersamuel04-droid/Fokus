package de.fokus.app;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/** Bis zu 4 Schnellkontakte: Name + Nummer. */
final class QuickContacts {
    private QuickContacts() {}

    static final int MAX = 4;

    static List<String[]> all(Context c) {
        List<String[]> out = new ArrayList<>();
        for (String line : Prefs.p(c).getString("contacts", "").split("\n")) {
            String[] p = line.split("\t");
            if (p.length == 2 && !p[1].trim().isEmpty()) out.add(p);
        }
        return out;
    }

    private static void save(Context c, List<String[]> list) {
        StringBuilder sb = new StringBuilder();
        for (String[] p : list) sb.append(p[0].replace("\t", " ").replace("\n", " "))
                .append('\t').append(p[1].replace("\t", "").replace("\n", "")).append('\n');
        Prefs.p(c).edit().putString("contacts", sb.toString()).apply();
    }

    static boolean add(Context c, String name, String number) {
        List<String[]> list = all(c);
        if (list.size() >= MAX) return false;
        list.add(new String[]{name == null || name.trim().isEmpty() ? number : name.trim(), number});
        save(c, list);
        return true;
    }

    static void remove(Context c, int index) {
        List<String[]> list = all(c);
        if (index >= 0 && index < list.size()) list.remove(index);
        save(c, list);
    }

    /** Nummer für wa.me: nur Ziffern, mit Ländervorwahl (Standard Deutschland). */
    static String waNumber(String n) {
        String d = n.replaceAll("[^0-9+]", "");
        if (d.startsWith("+")) d = d.substring(1);
        else if (d.startsWith("00")) d = d.substring(2);
        else if (d.startsWith("0")) d = "49" + d.substring(1);
        return d;
    }

    static String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        String s = parts[0].isEmpty() ? "?" : parts[0].substring(0, 1);
        if (parts.length > 1 && !parts[parts.length - 1].isEmpty()) s += parts[parts.length - 1].substring(0, 1);
        return s.toUpperCase();
    }

    static String firstName(String name) {
        return name.trim().split("\\s+")[0];
    }
}
