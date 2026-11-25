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
    private android.widget.Button quietButton, eveningButton;
    private LinearLayout contactsBox;
    private TextView logInfo;

    private void updateLogInfo() {
        long kb = Protokoll.size(this) / 1024;
        logInfo.setText((Protokoll.on(this) ? "Läuft – " : "Aus – ") + kb + " KB aufgezeichnet");
        logInfo.setTextColor(Protokoll.on(this) ? Game.accent(this) : Ui.GRAY);
    }
    private static final int PICK_CONTACT = 42;

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

        section(box, "Aufzeichnung");
        hint(box, "Schreibt mit, was die App tut: Gesten, Seitenwechsel, Reels-Erkennung und Fehler. "
                + "So geht's: einschalten → das Problem nachstellen (z. B. zu Reels gehen oder hochwischen) → "
                + "„Protokoll teilen“ → an Claude schicken. Gespeichert werden nur technische Daten, "
                + "keine Inhalte, Namen oder Nachrichten. Abstürze werden immer mitgeschrieben.");
        android.widget.Switch rec = new android.widget.Switch(this);
        rec.setText("Aufzeichnung läuft");
        rec.setTextColor(Ui.WHITE);
        rec.setChecked(Protokoll.on(this));
        rec.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        rec.setOnCheckedChangeListener((b, on) -> {
            Protokoll.setOn(this, on);
            updateLogInfo();
        });
        box.addView(rec);
        logInfo = Ui.text(this, "", 13, Ui.GRAY);
        box.addView(logInfo);
        box.addView(Ui.button(this, "Protokoll teilen (z. B. an Claude)", () -> {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_SUBJECT, "Fokus-Protokoll");
            send.putExtra(Intent.EXTRA_TEXT, Protokoll.text(this));
            startActivity(Intent.createChooser(send, "Protokoll teilen"));
        }));
        box.addView(Ui.button(this, "Protokoll kopieren", () -> {
            android.content.ClipboardManager cm =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            String t = Protokoll.text(this);
            if (t.length() > 90_000) t = t.substring(t.length() - 90_000);
            cm.setPrimaryClip(android.content.ClipData.newPlainText("Fokus-Protokoll", t));
            android.widget.Toast.makeText(this, "Kopiert – jetzt bei Claude einfügen",
                    android.widget.Toast.LENGTH_SHORT).show();
        }));
        box.addView(Ui.button(this, "Protokoll leeren", () -> {
            Protokoll.clear(this);
            updateLogInfo();
        }));

        section(box, "2. Ruhiger Homescreen");
        box.addView(Ui.button(this, "Homescreen-Apps auswählen (max. 8)", () -> {
            Intent i = new Intent(this, AppListActivity.class);
            i.putExtra(AppListActivity.EXTRA_PICK, true);
            Ui.open(this, i);
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

        eveningButton = Ui.button(this, "", this::chooseEvening);
        box.addView(eveningButton);
        hint(box, "Abendmodus: Ab dieser Uhrzeit (bis 5 Uhr) wird der Homescreen gedimmt und ruhiger.");
        hint(box, "Gesten auf dem Homescreen: nach oben wischen = Alle Apps, nach unten wischen = "
                + "Benachrichtigungen, Doppeltipp auf eine leere Stelle = Bildschirm aus "
                + "(Doppeltipp braucht den aktiven Reels-Block).");

        section(box, "Schnellkontakte");
        contactsBox = new LinearLayout(this);
        contactsBox.setOrientation(LinearLayout.VERTICAL);
        box.addView(contactsBox);
        box.addView(Ui.button(this, "Kontakt hinzufügen (max. 4)", () -> {
            if (QuickContacts.all(this).size() >= QuickContacts.MAX) {
                android.widget.Toast.makeText(this, "Maximal 4 Kontakte", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                startActivityForResult(new Intent(Intent.ACTION_PICK,
                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI), PICK_CONTACT);
            } catch (Exception ignored) {
            }
        }));
        hint(box, "Auf dem Homescreen: antippen = Anrufen oder WhatsApp, lange drücken = entfernen.");

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

    private void chooseEvening() {
        String[] labels = {"Aus", "ab 20:00", "ab 21:00", "ab 22:00", "ab 23:00"};
        int[] hours = {-1, 20, 21, 22, 23};
        new android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Abendmodus")
                .setItems(labels, (d, which) -> {
                    Game.setEveningFrom(this, hours[which]);
                    updateEveningButton();
                })
                .show();
    }

    private void updateEveningButton() {
        int h = Game.eveningFrom(this);
        eveningButton.setText("Abendmodus: " + (h < 0 ? "aus" : "ab " + h + ":00"));
    }

    private void refreshContactList() {
        contactsBox.removeAllViews();
        java.util.List<String[]> list = QuickContacts.all(this);
        if (list.isEmpty()) {
            contactsBox.addView(Ui.text(this, "Noch keine Kontakte", 14, Ui.DARK_GRAY));
            return;
        }
        for (String[] c : list) {
            TextView t = Ui.text(this, c[0] + "   " + c[1], 15, Ui.WHITE);
            t.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));
            contactsBox.addView(t);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_CONTACT || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        String[] proj = {
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER};
        try (android.database.Cursor c = getContentResolver().query(data.getData(), proj, null, null, null)) {
            if (c != null && c.moveToFirst()) QuickContacts.add(this, c.getString(0), c.getString(1));
        } catch (Exception ignored) {
        }
        refreshContactList();
    }

    private void refreshColors() {
        updateQuietButton();
        updateEveningButton();
        updateLogInfo();
        refreshContactList();
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

    @Override
    public void finish() {
        super.finish();
        Ui.close(this);
    }
}
