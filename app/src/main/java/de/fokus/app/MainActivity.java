package de.fokus.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Einrichtung: Reels-Block aktivieren, Homescreen-Apps wählen, Homescreen festlegen. */
public class MainActivity extends Activity {

    private TextView status;
    private TextView counter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFF111111);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF111111);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 24);
        box.setPadding(pad, Ui.dp(this, 40), pad, pad);
        scroll.addView(box);

        TextView title = Ui.text(this, "Fokus", 34, Ui.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(title);
        box.addView(Ui.text(this, "Reels blocken. Ruhiger Homescreen. Mehr schaffen.", 15, Ui.GRAY));

        section(box, "1. Reels-Block");
        status = Ui.text(this, "", 17, Ui.WHITE);
        box.addView(status);
        counter = Ui.text(this, "", 14, Ui.GRAY);
        box.addView(counter);
        box.addView(Ui.button(this, "Bedienungshilfen öffnen → „Fokus Reels-Block“ einschalten",
                () -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));
        hint(box, "Schalter ist grau oder gesperrt? Android sperrt das bei Apps außerhalb vom "
                + "Play Store. Lösung: „App-Info öffnen“ → oben rechts ⋮ → "
                + "„Eingeschränkte Einstellungen zulassen“. Danach nochmal oben probieren.");
        box.addView(Ui.button(this, "App-Info öffnen", () -> startActivity(
                new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName())))));

        section(box, "2. Ruhiger Homescreen");
        box.addView(Ui.button(this, "Homescreen-Apps auswählen (max. 6)", () -> {
            Intent i = new Intent(this, AppListActivity.class);
            i.putExtra(AppListActivity.EXTRA_PICK, true);
            startActivity(i);
        }));
        box.addView(Ui.button(this, "Fokus als Startbildschirm festlegen",
                () -> startActivity(new Intent(Settings.ACTION_HOME_SETTINGS))));
        hint(box, "Auf dem Fokus-Homescreen lange drücken, um hierher zurückzukommen. "
                + "Alle anderen Apps findest du unten über „Alle Apps“ – bewusst ein Schritt mehr.");

        section(box, "3. Extra-Tipp: Graustufen");
        hint(box, "Ein graues Handy lockt viel weniger. Samsung: Einstellungen → Digital Wellbeing "
                + "→ Schlafenszeitmodus → Graustufen (auch dauerhaft per Zeitplan). "
                + "Andere Handys: Bedienungshilfen → Farbkorrektur/Graustufen.");

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean on = isBlockerOn();
        status.setText(on ? "✅ Aktiv – Reels werden blockiert" : "❌ Noch nicht aktiv");
        counter.setText("Bisher blockiert: " + Prefs.blockCount(this) + "×");
    }

    private boolean isBlockerOn() {
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        ComponentName cn = new ComponentName(this, ReelsBlockerService.class);
        String all = enabled.toLowerCase();
        return all.contains(cn.flattenToString().toLowerCase())
                || all.contains(cn.flattenToShortString().toLowerCase());
    }

    private void section(LinearLayout box, String s) {
        TextView t = Ui.text(this, s, 20, Ui.WHITE);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, Ui.dp(this, 28), 0, Ui.dp(this, 8));
        box.addView(t);
    }

    private void hint(LinearLayout box, String s) {
        TextView t = Ui.text(this, s, 14, Ui.GRAY);
        t.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        box.addView(t);
    }
}
