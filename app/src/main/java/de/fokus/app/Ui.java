package de.fokus.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

final class Ui {
    private Ui() {}

    static int dp(Context c, int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics());
    }

    static TextView text(Context c, String s, int sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    static Button button(Context c, String s, Runnable action) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    /** Öffnet eine eigene Fokus-Seite mit weichem Übergang von unten. */
    static void open(android.app.Activity a, Intent i) {
        a.startActivity(i);
        a.overridePendingTransition(R.anim.fokus_enter_up, R.anim.fokus_recede);
    }

    /** Schließt eine eigene Fokus-Seite weich nach unten. */
    static void close(android.app.Activity a) {
        a.overridePendingTransition(R.anim.fokus_return, R.anim.fokus_exit_down);
    }

    /** Startet eine App so, dass sie aus dem angetippten Icon heraus wächst. */
    static void launchFrom(android.view.View v, String pkg) {
        Context c = v.getContext();
        Intent i = c.getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) {
            Toast.makeText(c, "App nicht gefunden", Toast.LENGTH_SHORT).show();
            return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Protokoll.log(c, "APP", "Starte " + pkg);
        android.app.ActivityOptions o = android.app.ActivityOptions.makeScaleUpAnimation(
                v, 0, 0, v.getWidth(), v.getHeight());
        c.startActivity(i, o.toBundle());
    }

    static void launch(Context c, String pkg) {
        Intent i = c.getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) {
            Toast.makeText(c, "App nicht gefunden", Toast.LENGTH_SHORT).show();
            return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
    }

    static final int WHITE = Color.WHITE;
    static final int GRAY = 0xFF8A8A8A;
    static final int DARK_GRAY = 0xFF5A5A5A;
}
