package de.fokus.app;

import android.content.Context;
import android.os.Build;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Aufzeichnungsmodus: schreibt mit, was die App tut (Gesten, Seitenwechsel, Reels-Erkennung, Fehler).
 * Bleibt auf dem Handy, bis der Nutzer es selbst kopiert oder teilt.
 */
final class Protokoll {
    private Protokoll() {}

    private static final long MAX_BYTES = 300_000;
    private static final SimpleDateFormat TIME = new SimpleDateFormat("dd.MM. HH:mm:ss.SSS", Locale.GERMANY);
    private static Boolean cachedOn;

    static boolean on(Context c) {
        if (cachedOn == null) cachedOn = Prefs.p(c).getBoolean("log_on", false);
        return cachedOn;
    }

    static void setOn(Context c, boolean on) {
        cachedOn = on;
        Prefs.p(c).edit().putBoolean("log_on", on).apply();
        Diagnose.setOn(c, on);
        write(c, "INFO", on ? "Aufzeichnung gestartet – " + geraet(c) : "Aufzeichnung beendet");
    }

    /** Normaler Eintrag, nur wenn die Aufzeichnung läuft. */
    static void log(Context c, String bereich, String text) {
        if (c == null || !on(c)) return;
        write(c, bereich, text);
    }

    /** Fehler werden immer geschrieben. */
    static void fehler(Context c, String bereich, Throwable t) {
        if (c == null) return;
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        String st = sw.toString();
        if (st.length() > 4000) st = st.substring(0, 4000);
        write(c, "FEHLER " + bereich, st);
    }

    private static synchronized void write(Context c, String bereich, String text) {
        try {
            File f = file(c);
            if (f.length() > MAX_BYTES) {
                String all = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                Files.write(f.toPath(), all.substring(all.length() / 2).getBytes(StandardCharsets.UTF_8));
            }
            try (FileWriter w = new FileWriter(f, true)) {
                w.write(TIME.format(new Date()) + "  [" + bereich + "]  " + text + "\n");
            }
        } catch (Exception ignored) {
        }
    }

    static String text(Context c) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Fokus-Protokoll ===\n").append(geraet(c)).append("\n\n");
        try {
            File f = file(c);
            if (f.exists()) {
                String all = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                if (all.length() > 120_000) all = "…(gekürzt)…\n" + all.substring(all.length() - 120_000);
                sb.append(all);
            }
        } catch (Exception e) {
            sb.append("(Protokoll nicht lesbar: ").append(e).append(")\n");
        }
        String diag = Diagnose.text(c);
        if (!diag.isEmpty()) sb.append("\n=== Instagram-Ansichten (ID, Höhe in %) ===\n").append(diag);
        return sb.toString();
    }

    static long size(Context c) {
        File f = file(c);
        return f.exists() ? f.length() : 0;
    }

    static void clear(Context c) {
        file(c).delete();
        Diagnose.clear(c);
    }

    private static File file(Context c) {
        return new File(c.getApplicationContext().getFilesDir(), "fokus_protokoll.txt");
    }

    static String geraet(Context c) {
        String version = "?";
        try {
            version = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Exception ignored) {
        }
        String ig = "nicht installiert";
        try {
            ig = c.getPackageManager().getPackageInfo("com.instagram.android", 0).versionName;
        } catch (Exception ignored) {
        }
        return "Fokus " + version + " | " + Build.MANUFACTURER + " " + Build.MODEL
                + " | Android " + Build.VERSION.RELEASE + " | Instagram " + ig;
    }
}
