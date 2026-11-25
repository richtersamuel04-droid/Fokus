package de.fokus.app;

import android.app.Application;

/** Fängt Abstürze ab und schreibt sie ins Protokoll, bevor die App beendet wird. */
public class FokusApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Thread.UncaughtExceptionHandler standard = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            Protokoll.fehler(this, "ABSTURZ in " + thread.getName(), ex);
            if (standard != null) standard.uncaughtException(thread, ex);
        });
    }
}
