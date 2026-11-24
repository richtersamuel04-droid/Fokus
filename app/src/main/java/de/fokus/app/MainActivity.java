package de.fokus.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.ScrollView;
import android.widget.TextView;

/** Einrichtung: Reels-Block aktivieren, Homescreen-Apps wählen, Homescreen festlegen. */
public class MainActivity extends Activity {

    private TextView status;
    private TextView counter;
    private LinearLayout colorRow;
    private android.widget.Button quietButton;

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
        box.addView(Ui.button(this, "Homescreen-Apps auswählen (max. 8)", () -> {
            Intent i = new Intent(this, AppListActivity.class);
            i.putExtra(AppListActivity.EXTRA_PICK, true);
            startActivity(i);
        }));
        box.addView(Ui.button(this, "Fokus als Startbildschirm festlegen",
                () -> startActivity(new Intent(Settings.ACTION_HOME_SETTINGS))));
        hint(box, "Auf dem Fokus-Homescreen lange drücken, um hierher zurückzukommen. "
                + "Alle anderen Apps findest du unten über „Alle Apps“ – bewusst ein Schritt mehr.");

        box.addView(Ui.button(this, "Kalender für Termine erlauben",
                () -> requestPermissions(new String[]{android.Manifest.permission.READ_CALENDAR}, 1)));
        box.addView(Ui.button(this, "Bildschirmzeit erlauben (Nutzungsdatenzugriff → Fokus)",
                () -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))));
        quietButton = Ui.button(this, "", this::chooseQuiet);
        box.addView(quietButton);
        hint(box, "Ruhiger Start: Bis zu dieser Uhrzeit ist „Alle Apps“ ausgeblendet – "
                + "nur Uhr, Termine und deine Homescreen-Apps.");

        section(box, "3. Akzentfarbe");
        colorRow = new LinearLayout(this);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        box.addView(colorRow);
        Switch auto = new Switch(this);
        auto.setText("Jeden Tag eine neue Farbe");
        auto.setTextColor(Ui.WHITE);
        auto.setChecked(Game.autoColor(this));
        auto.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 4));
        auto.setOnCheckedChangeListener((b, on) -> {
            Game.setAutoColor(this, on);
            refreshColors();
        });
        box.addView(auto);

        section(box, "4. Extra-Tipp: Graustufen");
        hint(box, "Ein graues Handy lockt viel weniger. Samsung: Einstellungen → Digital Wellbeing "
                + "→ Schlafenszeitmodus → Graustufen (auch dauerhaft per Zeitplan). "
                + "Andere Handys: Bedienungshilfen → Farbkorrektur/Graustufen.");

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean on = isBlockerOn();
        status.setText(on ? "Aktiv – Reels werden blockiert" : "Noch nicht aktiv");
        status.setTextColor(on ? Game.accent(this) : 0xFFE57373);
        counter.setText("Bisher blockiert: " + Prefs.blockCount(this) + "×");
        refreshColors();
    }

    private void chooseQuiet() {
        String[] labels = {"Aus", "bis 6:00", "bis 7:00", "bis 8:00", "bis 9:00", "bis 10:00"};
        int[] hours = {-1, 6, 7, 8, 9, 10};
        new android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Ruhiger Start")
                .setItems(labels, (d, which) -> {
                    Game.setQuietUntil(this, hours[which]);
                    updateQuietButton();
                })
                .show();
    }

    private void updateQuietButton() {
        int h = Game.quietUntil(this);
        quietButton.setText("Ruhiger Start: " + (h < 0 ? "aus" : "bis " + h + ":00"));
    }

    private void refreshColors() {
        updateQuietButton();
        colorRow.removeAllViews();
        int current = Game.colorIndex(this);
        boolean auto = Game.autoColor(this);
        for (int i = 0; i < Content.COLORS.length; i++) {
            final int idx = i;
            View dot = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(Content.COLORS[i]);
            if (i == current) g.setStroke(Ui.dp(this, 3), Ui.WHITE);
            dot.setBackground(g);
            dot.setAlpha(auto && i != current ? 0.35f : 1f);
            dot.setContentDescription(Content.COLOR_NAMES[i]);
            dot.setOnClickListener(v -> {
                Game.setAutoColor(this, false);
                Game.setColorIndex(this, idx);
                recreate();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40));
            lp.rightMargin = Ui.dp(this, 12);
            colorRow.addView(dot, lp);
        }
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
