package de.fokus.app;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.SearchManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextClock;
import android.widget.TextView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fokus-Homescreen v5: Apple-clean, schwarz, sparsame grüne Akzente.
 * Oben: Uhr, nächster Termin, Bildschirmzeit. Unten am Daumen: schlichte grüne App-Icons.
 */
public class LauncherActivity extends Activity {

    private static final Typeface THIN = Typeface.create("sans-serif-thin", Typeface.NORMAL);
    private static final Typeface LIGHT = Typeface.create("sans-serif-light", Typeface.NORMAL);
    private static final Typeface REGULAR = Typeface.create("sans-serif", Typeface.NORMAL);
    private static final Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final int REQ_CAL = 7;

    private int accent;
    private LinearLayout content, info;
    private FrameLayout rootFrame;
    private boolean onInfo = false;
    private TextView blockWarn;
    private String lastDienst = "";
    private TextView focusPill, importantText, allApps, quietHint;
    private LinearLayout eventBox, screenBox, appsGrid, contactsRow;
    private GestureDetector flingDetector;
    private ObjectAnimator barAnim;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateFocusPill();
            if (Game.focusActive(LauncherActivity.this)) handler.postDelayed(this, 1000);
        }
    };
    private final Runnable minuteRefresh = new Runnable() {
        @Override
        public void run() {
            refreshEvents();
            refreshScreenTime();
            refreshQuiet();
            refreshEvening();
            handler.postDelayed(this, 60_000);
        }
    };

    private static final class Ev {
        String title, location;
        long begin, end;
        boolean allDay;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        rootFrame = new FrameLayout(this);
        rootFrame.setBackgroundColor(Color.BLACK);
        rootFrame.setFocusableInTouchMode(true); // Tastatur nicht automatisch öffnen

        // ===== Seite 1: Uhr, Kontakte, Suche, Apps =====
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(28), dp(40), dp(28), dp(16));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.TOP);
        LinearLayout clockCol = new LinearLayout(this);
        clockCol.setOrientation(LinearLayout.VERTICAL);
        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextSize(76);
        clock.setTextColor(Ui.WHITE);
        clock.setTypeface(THIN);
        clock.setIncludeFontPadding(false);
        clockCol.addView(clock);
        TextClock date = new TextClock(this);
        date.setFormat24Hour("EEEE, d. MMMM");
        date.setFormat12Hour("EEEE, d. MMMM");
        date.setTextSize(16);
        date.setTextColor(Ui.GRAY);
        date.setTypeface(LIGHT);
        date.setPadding(dp(2), dp(4), 0, 0);
        clockCol.addView(date);
        blockWarn = Ui.text(this, "Reels-Block ist aus – antippen zum Einschalten", 13, 0xFFE57373);
        blockWarn.setTypeface(LIGHT);
        blockWarn.setPadding(dp(2), dp(12), 0, 0);
        blockWarn.setVisibility(View.GONE);
        blockWarn.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } catch (Exception ignored) {
            }
        });
        clockCol.addView(blockWarn);
        head.addView(clockCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        focusPill = Ui.text(this, "", 13, Ui.GRAY);
        focusPill.setTypeface(REGULAR);
        focusPill.setPadding(dp(14), dp(7), dp(14), dp(7));
        focusPill.setOnClickListener(v -> onFocusPill());
        LinearLayout.LayoutParams pillLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pillLp.topMargin = dp(14);
        head.addView(focusPill, pillLp);
        content.addView(head);

        content.addView(new View(this), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        contactsRow = new LinearLayout(this);
        contactsRow.setOrientation(LinearLayout.HORIZONTAL);
        contactsRow.setPadding(0, 0, 0, dp(14));
        content.addView(contactsRow);

        content.addView(combinedSearchBar());

        appsGrid = new LinearLayout(this);
        appsGrid.setOrientation(LinearLayout.VERTICAL);
        appsGrid.setPadding(0, dp(14), 0, 0);
        content.addView(appsGrid);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER);
        bottom.setPadding(0, dp(6), 0, 0);
        allApps = Ui.text(this, "Alle Apps", 13, Ui.DARK_GRAY);
        allApps.setTypeface(REGULAR);
        allApps.setPadding(dp(16), dp(6), dp(16), dp(6));
        allApps.setOnClickListener(v -> Ui.open(this, new Intent(this, AppListActivity.class)));
        bottom.addView(allApps);
        quietHint = Ui.text(this, "", 13, 0xFF444444);
        quietHint.setTypeface(LIGHT);
        bottom.addView(quietHint);
        content.addView(bottom);
        content.addView(pageDots(0));

        // ===== Seite 2: Heute (Termine, Wichtig, Bildschirmzeit) =====
        info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setBackgroundColor(Color.BLACK);
        info.setPadding(dp(28), dp(48), dp(28), dp(16));

        TextView infoTitle = Ui.text(this, "Heute", 34, Ui.WHITE);
        infoTitle.setTypeface(THIN);
        info.addView(infoTitle);

        importantText = Ui.text(this, "", 15, Ui.GRAY);
        importantText.setTypeface(LIGHT);
        importantText.setPadding(dp(2), dp(14), 0, 0);
        importantText.setOnClickListener(v -> editImportant());
        info.addView(importantText);

        eventBox = new LinearLayout(this);
        eventBox.setOrientation(LinearLayout.VERTICAL);
        eventBox.setPadding(0, dp(34), 0, 0);
        info.addView(eventBox);

        screenBox = new LinearLayout(this);
        screenBox.setOrientation(LinearLayout.VERTICAL);
        screenBox.setPadding(0, dp(34), 0, 0);
        info.addView(screenBox);

        info.addView(new View(this), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        info.addView(pageDots(1));

        rootFrame.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rootFrame.addView(info, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        info.setVisibility(View.INVISIBLE);

        View.OnLongClickListener openSettings = v -> {
            Ui.open(this, new Intent(this, MainActivity.class));
            return true;
        };
        content.setOnLongClickListener(openSettings);
        info.setOnLongClickListener(openSettings);
        head.setOnLongClickListener(openSettings);

        setupGestures();
        setContentView(rootFrame);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        showInfo(false); // Home-Taste bringt immer zurück zur Hauptseite
    }

    @Override
    protected void onResume() {
        super.onResume();
        Protokoll.log(this, "HOME", "Homescreen geöffnet (Seite: " + (onInfo ? "Heute" : "Haupt") + ")");
        blockWarn.setVisibility(ReelsBlockerService.running() ? View.GONE : View.VISIBLE);
        String ds = Protokoll.dienstStatus(this);
        if (!ds.equals(lastDienst)) {
            lastDienst = ds;
            Protokoll.log(this, "DIENST", ds);
        }
        accent = Game.accent(this);
        updateFocusPill();
        refreshApps();
        // Heute-Seite still vorbereiten, damit beim Wischen nichts nachlädt
        rootFrame.post(() -> {
            refreshImportant();
            refreshEvents();
            refreshScreenTime();
        });
        refreshContacts();
        refreshQuiet();
        refreshEvening();
        handler.removeCallbacks(tick);
        if (Game.focusActive(this)) handler.postDelayed(tick, 1000);
        handler.removeCallbacks(minuteRefresh);
        handler.postDelayed(minuteRefresh, 60_000);
        fadeIn();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
        handler.removeCallbacks(minuteRefresh);
        if (barAnim != null) barAnim.cancel();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_CAL) refreshEvents();
    }

    // ================= Fokus =================

    private void updateFocusPill() {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(20));
        if (Game.focusActive(this)) {
            long left = Math.max(0, Game.focusEnd(this) - System.currentTimeMillis());
            long sec = (left + 999) / 1000;
            focusPill.setText(String.format(Locale.GERMANY, "Fokus  %d:%02d", sec / 60, sec % 60));
            focusPill.setTextColor(accent);
            g.setColor(withAlpha(accent, 0x22));
        } else {
            focusPill.setText("Fokus");
            focusPill.setTextColor(Ui.DARK_GRAY);
            g.setColor(Color.TRANSPARENT);
            g.setStroke(dp(1), 0xFF2A2A2A);
        }
        focusPill.setBackground(g);
    }

    private void onFocusPill() {
        if (Game.focusActive(this)) {
            new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                    .setTitle("Fokus beenden?")
                    .setMessage("Instagram ist während des Fokus komplett gesperrt.")
                    .setPositiveButton("Beenden", (d, w) -> {
                        Game.stopFocus(this);
                        updateFocusPill();
                    })
                    .setNegativeButton("Weiter", null)
                    .show();
            return;
        }
        String[] labels = {"15 Minuten", "25 Minuten", "50 Minuten"};
        int[] minutes = {15, 25, 50};
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Fokus starten")
                .setItems(labels, (d, which) -> {
                    Game.startFocus(this, minutes[which]);
                    updateFocusPill();
                    handler.removeCallbacks(tick);
                    handler.postDelayed(tick, 1000);
                })
                .show();
    }

    // ================= Heute wichtig =================

    private void refreshImportant() {
        String s = Game.important(this);
        if (s.isEmpty()) {
            importantText.setText("+ Heute wichtig");
            importantText.setTextColor(0xFF3A3A3A);
        } else {
            importantText.setText(s);
            importantText.setTextColor(0xFFCCCCCC);
        }
    }

    private void editImportant() {
        EditText input = new EditText(this);
        input.setHint("Was ist heute wichtig?");
        input.setText(Game.important(this));
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setSingleLine(true);
        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(dp(20), dp(8), dp(20), 0);
        wrap.addView(input);
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Heute wichtig")
                .setView(wrap)
                .setPositiveButton("Speichern", (d, w) -> {
                    Game.setImportant(this, input.getText().toString().trim());
                    refreshImportant();
                })
                .setNeutralButton("Leeren", (d, w) -> {
                    Game.setImportant(this, "");
                    refreshImportant();
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    // ================= Termine =================

    private void refreshEvents() {
        eventBox.removeAllViews();
        if (barAnim != null) barAnim.cancel();

        if (checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            TextView t = Ui.text(this, "Termine anzeigen", 15, accent);
            t.setTypeface(REGULAR);
            t.setOnClickListener(v -> requestPermissions(new String[]{Manifest.permission.READ_CALENDAR}, REQ_CAL));
            eventBox.addView(t);
            TextView s = Ui.text(this, "Antippen, um den Kalender zu verbinden", 13, Ui.DARK_GRAY);
            s.setTypeface(LIGHT);
            eventBox.addView(s);
            return;
        }

        List<Ev> events = loadEvents();
        long now = System.currentTimeMillis();
        LocalDate today = LocalDate.now();
        Ev next = null;
        for (Ev e : events) {
            if (!e.allDay) {
                next = e;
                break;
            }
        }

        // Nächster Termin: grüner Strich + Titel + Zeit
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        View bar = new View(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(2));
        bg.setColor(next != null ? accent : 0xFF2A2A2A);
        bar.setBackground(bg);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dp(3), ViewGroup.LayoutParams.MATCH_PARENT);
        barLp.rightMargin = dp(16);
        row.addView(bar, barLp);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView label = Ui.text(this, "NÄCHSTER TERMIN", 11, Ui.DARK_GRAY);
        label.setTypeface(MEDIUM);
        label.setLetterSpacing(0.15f);
        col.addView(label);

        if (next == null) {
            TextView t = Ui.text(this, "Heute nichts mehr geplant", 22, 0xFFBBBBBB);
            t.setTypeface(LIGHT);
            t.setPadding(0, dp(4), 0, 0);
            col.addView(t);
        } else {
            TextView t = Ui.text(this, next.title, 22, Ui.WHITE);
            t.setTypeface(REGULAR);
            t.setMaxLines(2);
            t.setEllipsize(TextUtils.TruncateAt.END);
            t.setPadding(0, dp(4), 0, 0);
            col.addView(t);

            LocalDate day = Instant.ofEpochMilli(next.begin).atZone(ZoneId.systemDefault()).toLocalDate();
            String time = fmt(next.begin) + " – " + fmt(next.end);
            String when;
            if (next.begin <= now) when = "Läuft gerade · bis " + fmt(next.end);
            else if (day.equals(today)) when = time + "  ·  " + relative(next.begin - now);
            else when = "Morgen  ·  " + time;
            if (next.location != null && !next.location.trim().isEmpty()) when += "\n" + next.location.trim();

            TextView w = Ui.text(this, when, 14, next.begin <= now ? accent : Ui.GRAY);
            w.setTypeface(LIGHT);
            w.setPadding(0, dp(4), 0, 0);
            col.addView(w);
        }
        row.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        eventBox.addView(row);

        if (next != null) {
            barAnim = ObjectAnimator.ofFloat(bar, View.ALPHA, 1f, 0.45f);
            barAnim.setDuration(2400);
            barAnim.setRepeatCount(ValueAnimator.INFINITE);
            barAnim.setRepeatMode(ValueAnimator.REVERSE);
            barAnim.start();
        }

        // Weitere Termine heute
        int shown = 0;
        for (Ev e : events) {
            if (e == next || shown >= 3) continue;
            LocalDate d = e.allDay ? today
                    : Instant.ofEpochMilli(e.begin).atZone(ZoneId.systemDefault()).toLocalDate();
            if (!d.equals(today)) continue;
            LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.HORIZONTAL);
            r.setPadding(dp(19), dp(shown == 0 ? 16 : 6), 0, 0);
            TextView tm = Ui.text(this, e.allDay ? "Ganztags" : fmt(e.begin), 14, Ui.DARK_GRAY);
            tm.setTypeface(LIGHT);
            r.addView(tm, new LinearLayout.LayoutParams(dp(72), ViewGroup.LayoutParams.WRAP_CONTENT));
            TextView tt = Ui.text(this, e.title, 14, Ui.GRAY);
            tt.setTypeface(LIGHT);
            tt.setSingleLine(true);
            tt.setEllipsize(TextUtils.TruncateAt.END);
            r.addView(tt);
            eventBox.addView(r);
            shown++;
        }

        View.OnClickListener openCal = v -> {
            Intent i = new Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI.buildUpon()
                    .appendPath("time").build());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(i);
            } catch (Exception ignored) {
            }
        };
        eventBox.setOnClickListener(openCal);
    }

    private List<Ev> loadEvents() {
        List<Ev> out = new ArrayList<>();
        long now = System.currentTimeMillis();
        long end = LocalDate.now().plusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Uri.Builder b = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(b, now - 24 * 3600_000L);
        ContentUris.appendId(b, end);
        String[] proj = {
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.EVENT_LOCATION,
        };
        LocalDate today = LocalDate.now();
        try (Cursor c = getContentResolver().query(b.build(), proj, null, null,
                CalendarContract.Instances.BEGIN + " ASC")) {
            if (c == null) return out;
            while (c.moveToNext()) {
                Ev e = new Ev();
                e.title = c.getString(0);
                e.begin = c.getLong(1);
                e.end = c.getLong(2);
                e.allDay = c.getInt(3) == 1;
                e.location = c.getString(4);
                if (e.title == null || e.title.trim().isEmpty()) e.title = "Termin";
                if (e.allDay) {
                    LocalDate s = Instant.ofEpochMilli(e.begin).atZone(ZoneOffset.UTC).toLocalDate();
                    LocalDate f = Instant.ofEpochMilli(e.end).atZone(ZoneOffset.UTC).toLocalDate();
                    if (today.isBefore(s) || !today.isBefore(f)) continue;
                } else if (e.end <= now) {
                    continue;
                }
                out.add(e);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static String fmt(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime().format(HM);
    }

    private static String relative(long ms) {
        long min = Math.max(1, ms / 60_000);
        if (min < 60) return "in " + min + " min";
        long h = min / 60, m = min % 60;
        return m == 0 ? "in " + h + " h" : "in " + h + " h " + m + " min";
    }

    // ================= Bildschirmzeit =================

    private void refreshScreenTime() {
        screenBox.removeAllViews();
        if (!usageAllowed()) {
            TextView t = Ui.text(this, "Bildschirmzeit anzeigen", 14, Ui.DARK_GRAY);
            t.setTypeface(LIGHT);
            t.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                } catch (Exception ignored) {
                }
            });
            screenBox.addView(t);
            return;
        }
        long now = System.currentTimeMillis();
        long dayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long today = usage(dayStart, now);
        long yesterday = usage(dayStart - 86_400_000L, now - 86_400_000L);
        long avg = weekAverage(dayStart);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView l = Ui.text(this, "Heute am Handy", 14, Ui.DARK_GRAY);
        l.setTypeface(LIGHT);
        row.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView v = Ui.text(this, duration(today), 14, 0xFFCCCCCC);
        v.setTypeface(REGULAR);
        row.addView(v);
        screenBox.addView(row);

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(1000);
        bar.setProgressTintList(ColorStateList.valueOf(accent));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF1C1C1C));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3));
        lp.topMargin = dp(8);
        screenBox.addView(bar, lp);
        int target = (int) Math.min(1000, today * 1000 / (4 * 3600_000L)); // Linie = 4 Stunden
        bar.setProgress(target);

        // Vergleich mit gestern (gleiche Uhrzeit) und Schnitt der letzten 7 Tage
        long diff = today - yesterday;
        String cmp;
        int color;
        if (yesterday == 0 && today > 0) {
            cmp = "";
            color = Ui.DARK_GRAY;
        } else if (Math.abs(diff) < 5 * 60_000L) {
            cmp = "Etwa wie gestern um diese Zeit";
            color = Ui.DARK_GRAY;
        } else if (diff < 0) {
            cmp = "▼ " + duration(-diff) + " weniger als gestern um diese Zeit";
            color = accent;
        } else {
            cmp = "▲ " + duration(diff) + " mehr als gestern um diese Zeit";
            color = 0xFFBBBBBB;
        }
        if (avg > 0) cmp += (cmp.isEmpty() ? "" : "\n") + "Ø letzte 7 Tage: " + duration(avg);
        if (!cmp.isEmpty()) {
            TextView c = Ui.text(this, cmp, 12, color);
            c.setTypeface(LIGHT);
            c.setLineSpacing(0, 1.2f);
            c.setPadding(0, dp(8), 0, 0);
            screenBox.addView(c);
        }
    }

    private static String duration(long ms) {
        long min = ms / 60_000;
        if (min < 60) return min + " min";
        return (min / 60) + " h " + (min % 60) + " min";
    }

    private boolean usageAllowed() {
        AppOpsManager ops = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
        int mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    /** Durchschnitt der letzten 7 vollen Tage (nur Tage mit Daten), einmal pro Tag berechnet. */
    private long weekAverage(long dayStart) {
        String key = LocalDate.now().toString();
        if (key.equals(Prefs.p(this).getString("avg_day", ""))) return Prefs.p(this).getLong("avg_val", 0);
        long sum = 0;
        int days = 0;
        for (int i = 1; i <= 7; i++) {
            long s = dayStart - i * 86_400_000L;
            long u = usage(s, s + 86_400_000L);
            if (u > 0) {
                sum += u;
                days++;
            }
        }
        long avg = days == 0 ? 0 : sum / days;
        Prefs.p(this).edit().putString("avg_day", key).putLong("avg_val", avg).apply();
        return avg;
    }

    /** Vordergrundzeit aller Apps (ohne Fokus selbst) zwischen start und end. */
    private long usage(long start, long end) {
        UsageStatsManager um = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        UsageEvents events = um.queryEvents(start, end);
        if (events == null) return 0;
        Map<String, Long> open = new HashMap<>();
        long total = 0;
        String self = getPackageName();
        UsageEvents.Event e = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(e);
            String pkg = e.getPackageName();
            int type = e.getEventType();
            if (type == 1) { // Vordergrund
                if (!open.containsKey(pkg)) open.put(pkg, e.getTimeStamp());
            } else if (type == 2) { // Hintergrund
                Long s = open.remove(pkg);
                if (s != null && !pkg.equals(self)) total += e.getTimeStamp() - s;
            } else if (type == 16) { // Bildschirm aus
                for (Map.Entry<String, Long> en : open.entrySet()) {
                    if (!en.getKey().equals(self)) total += e.getTimeStamp() - en.getValue();
                }
                open.clear();
            }
        }
        return total;
    }

    // ================= Schnellkontakte =================

    private void refreshContacts() {
        contactsRow.removeAllViews();
        List<String[]> list = QuickContacts.all(this);
        contactsRow.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        for (int i = 0; i < list.size(); i++) {
            final int idx = i;
            final String name = list.get(i)[0], number = list.get(i)[1];

            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);

            TextView circle = Ui.text(this, QuickContacts.initials(name), 16, accent);
            circle.setTypeface(LIGHT);
            circle.setGravity(Gravity.CENTER);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(Color.TRANSPARENT);
            bg.setStroke(dp(1), withAlpha(accent, 0x99));
            circle.setBackground(bg);
            cell.addView(circle, new LinearLayout.LayoutParams(dp(46), dp(46)));

            TextView n = Ui.text(this, QuickContacts.firstName(name), 11, Ui.GRAY);
            n.setTypeface(LIGHT);
            n.setSingleLine(true);
            n.setEllipsize(TextUtils.TruncateAt.END);
            n.setGravity(Gravity.CENTER);
            n.setPadding(0, dp(5), 0, 0);
            cell.addView(n, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            cell.setOnClickListener(v -> new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                    .setTitle(name)
                    .setItems(new String[]{"Anrufen", "WhatsApp"}, (d, which) -> {
                        if (which == 0) call(number);
                        else openUrl("https://wa.me/" + QuickContacts.waNumber(number));
                    })
                    .show());
            cell.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                        .setTitle(name + " entfernen?")
                        .setPositiveButton("Entfernen", (d, w) -> {
                            QuickContacts.remove(this, idx);
                            refreshContacts();
                        })
                        .setNegativeButton("Abbrechen", null)
                        .show();
                return true;
            });
            contactsRow.addView(cell, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        for (int i = list.size(); i < QuickContacts.MAX && !list.isEmpty(); i++) {
            contactsRow.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        }
    }

    private void call(String number) {
        Uri uri = Uri.parse("tel:" + Uri.encode(number));
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            try {
                startActivity(new Intent(Intent.ACTION_CALL, uri));
                return;
            } catch (Exception ignored) {
            }
        } else {
            requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 8);
        }
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, uri));
        } catch (Exception ignored) {
        }
    }

    private void openUrl(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception ignored) {
        }
    }

    // ================= Wischgesten =================

    // ---- Seitenwechsel, der dem Finger folgt ----
    private float downX, downY, dragStartProgress, progress = 0f;
    private boolean dragging = false;
    private VelocityTracker velocity;
    private ValueAnimator settleAnim;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (flingDetector != null && !dragging) flingDetector.onTouchEvent(ev);
        int w = Math.max(1, rootFrame.getWidth());
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getRawX();
                downY = ev.getRawY();
                dragging = false;
                if (velocity != null) velocity.recycle();
                velocity = VelocityTracker.obtain();
                velocity.addMovement(ev);
                break;
            case MotionEvent.ACTION_MOVE: {
                if (velocity != null) velocity.addMovement(ev);
                float dx = ev.getRawX() - downX, dy = ev.getRawY() - downY;
                if (!dragging && Math.abs(dx) > dp(14) && Math.abs(dx) > Math.abs(dy) * 1.3f
                        && ((dx < 0 && !onInfo) || (dx > 0 && onInfo))) {
                    dragging = true;
                    Protokoll.log(this, "SEITE", "Ziehen gestartet (" + (onInfo ? "zurück zur Hauptseite" : "zu Heute") + ")");
                    if (settleAnim != null) settleAnim.cancel();
                    dragStartProgress = progress;
                    downX = ev.getRawX();
                    info.setVisibility(View.VISIBLE);
                    MotionEvent cancel = MotionEvent.obtain(ev);
                    cancel.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(cancel); // Antippen darunter abbrechen
                    cancel.recycle();
                }
                if (dragging) {
                    applyProgress(clamp(dragStartProgress - (ev.getRawX() - downX) / w));
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    dragging = false;
                    float vx = 0;
                    if (velocity != null) {
                        velocity.addMovement(ev);
                        velocity.computeCurrentVelocity(1000);
                        vx = velocity.getXVelocity();
                    }
                    float target;
                    if (vx < -600) target = 1f;
                    else if (vx > 600) target = 0f;
                    else target = progress > 0.5f ? 1f : 0f;
                    Protokoll.log(this, "SEITE", String.format(Locale.GERMANY,
                            "Losgelassen bei %.0f%%, Tempo %.0f → %s", progress * 100, vx, target > 0.5f ? "Heute" : "Hauptseite"));
                    animateTo(target);
                    return true;
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    private static float clamp(float p) {
        return Math.max(0f, Math.min(1f, p));
    }

    /** 0 = Hauptseite, 1 = Heute-Seite. Hauptseite weicht leicht zurück, Heute-Seite gleitet herein. */
    private void applyProgress(float p) {
        progress = p;
        int w = rootFrame.getWidth();
        info.setTranslationX(w * (1f - p));
        content.setTranslationX(-w * 0.25f * p);
        content.setAlpha(1f - 0.85f * p);
        float sc = 1f - 0.04f * p;
        content.setScaleX(sc);
        content.setScaleY(sc);
    }

    private void animateTo(float target) {
        if (settleAnim != null) settleAnim.cancel();
        final boolean toInfo = target > 0.5f;
        if (toInfo) info.setVisibility(View.VISIBLE);
        long dur = (long) (180 + 220 * Math.abs(target - progress));
        settleAnim = ValueAnimator.ofFloat(progress, target);
        settleAnim.setDuration(dur);
        settleAnim.setInterpolator(new DecelerateInterpolator(1.8f));
        settleAnim.addUpdateListener(a -> applyProgress((float) a.getAnimatedValue()));
        settleAnim.addListener(new android.animation.AnimatorListenerAdapter() {
            boolean cancelled = false;

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (cancelled) return;
                boolean wasInfo = onInfo;
                onInfo = toInfo;
                if (!toInfo) info.setVisibility(View.INVISIBLE);

            }
        });
        settleAnim.start();
    }

    /** Inhalte einer Seite gleiten nacheinander leicht von rechts herein. */
    private void staggerIn(LinearLayout page) {
        for (int i = 0; i < page.getChildCount(); i++) {
            View v = page.getChildAt(i);
            v.setAlpha(0f);
            v.setTranslationX(dp(18));
            v.animate().alpha(1f).translationX(0).setStartDelay(i * 55L).setDuration(360)
                    .setInterpolator(new DecelerateInterpolator(1.5f)).start();
        }
    }

    private void setupGestures() {
        flingDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (e1 == null) return false;
                float dy = e2.getY() - e1.getY(), dx = e2.getX() - e1.getX();
                Protokoll.log(LauncherActivity.this, "GESTE", String.format(Locale.GERMANY,
                        "Wisch erkannt: dx=%.0fdp dy=%.0fdp vy=%.0f vx=%.0f", dx / dpf(), dy / dpf(), vy, vx));

                if (Math.abs(dy) < dp(50) || Math.abs(dy) < Math.abs(dx) * 1.2f || Math.abs(vy) < 350) {
                    Protokoll.log(LauncherActivity.this, "GESTE", "→ zu kurz/zu langsam/zu schräg, ignoriert");
                    return false;
                }
                Protokoll.log(LauncherActivity.this, "GESTE", dy < 0 ? "→ hoch: Alle Apps" : "→ runter: Benachrichtigungen");
                if (dy < 0) openAllApps();
                else openNotifications();
                return true;
            }
        });
        GestureDetector tapDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                Protokoll.log(LauncherActivity.this, "GESTE", "Doppeltipp → Bildschirm sperren");
                lockScreen();
                return true;
            }
        });
        // Doppeltipp nur auf leere Fläche (nicht auf Apps, Termine usw.)
        View.OnTouchListener tl = (v, e) -> {
            tapDetector.onTouchEvent(e);
            return false;
        };
        content.setOnTouchListener(tl);
        info.setOnTouchListener(tl);
    }

    private void openAllApps() {
        int until = Game.quietUntil(this);
        if (until >= 0 && LocalTime.now().getHour() < until) { // Ruhiger Start
            Protokoll.log(this, "GESTE", "Alle Apps gesperrt (ruhiger Start bis " + until + ":00)");
            android.widget.Toast.makeText(this, "Ruhiger Start bis " + until + ":00 – Alle Apps danach",
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        Ui.open(this, new Intent(this, AppListActivity.class));
    }

    private void openNotifications() {
        if (ReelsBlockerService.global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)) return;
        Protokoll.log(this, "GESTE", "Benachrichtigungen: Dienst nicht aktiv, versuche Ersatzweg");
        try {
            Object sbm = getSystemService("statusbar");
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(sbm);
        } catch (Exception ignored) {
        }
    }

    private void lockScreen() {
        if (Build.VERSION.SDK_INT >= 28 && ReelsBlockerService.global(
                android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)) return;
        android.widget.Toast.makeText(this, "Für Doppeltipp-Sperre den Reels-Block aktivieren",
                android.widget.Toast.LENGTH_SHORT).show();
    }

    // ================= Abendmodus =================

    private void refreshEvening() {
        rootFrame.animate().alpha(Game.eveningActive(this) ? 0.5f : 1f).setDuration(600).start();
    }

    // ================= Apps =================

    private void refreshApps() {
        appsGrid.removeAllViews();
        List<String> apps = Prefs.homeApps(this);
        if (apps.isEmpty()) {
            TextView hint = Ui.text(this, "Lange drücken, um Apps auszuwählen", 14, Ui.DARK_GRAY);
            hint.setTypeface(LIGHT);
            hint.setGravity(Gravity.CENTER);
            appsGrid.addView(hint, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            return;
        }
        PackageManager pm = getPackageManager();
        LinearLayout row = null;
        int col = 0;
        for (String pkg : apps) {
            String name;
            Drawable icon;
            try {
                name = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
                icon = pm.getApplicationIcon(pkg);
            } catch (PackageManager.NameNotFoundException ex) {
                continue;
            }
            if (row == null || col == 4) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, dp(8), 0, dp(8));
                appsGrid.addView(row);
                col = 0;
            }
            row.addView(appCell(pkg, name, icon),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            col++;
        }
        while (row != null && col < 4) {
            row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
            col++;
        }
    }

    private View appCell(String pkg, String name, Drawable icon) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);

        FrameLayout circle = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0xFF121212);
        bg.setStroke(dp(1), 0xFF1F1F1F);
        circle.setBackground(bg);

        Drawable mono = null;
        if (Build.VERSION.SDK_INT >= 33 && icon instanceof AdaptiveIconDrawable) {
            mono = ((AdaptiveIconDrawable) icon).getMonochrome();
        }
        if (mono != null) {
            ImageView iv = new ImageView(this);
            Drawable d = mono.mutate();
            d.setTint(accent);
            iv.setImageDrawable(d);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            circle.addView(iv, new FrameLayout.LayoutParams(dp(60), dp(60), Gravity.CENTER));
        } else {
            TextView letter = Ui.text(this, name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.GERMANY),
                    20, accent);
            letter.setTypeface(LIGHT);
            letter.setGravity(Gravity.CENTER);
            circle.addView(letter, new FrameLayout.LayoutParams(dp(58), dp(58), Gravity.CENTER));
        }
        cell.addView(circle, new LinearLayout.LayoutParams(dp(58), dp(58)));

        TextView label = Ui.text(this, name, 11, Ui.GRAY);
        label.setTypeface(LIGHT);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER);
        label.setPadding(dp(2), dp(6), dp(2), 0);
        cell.addView(label, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        cell.setOnClickListener(v -> {
            v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80)
                    .withEndAction(() -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                        Ui.launchFrom(v, pkg);
                    }).start();
        });
        return cell;
    }

    // ================= Google- & Claude-Leiste =================

    /** Eine Leiste, zwei Hälften: links Google, rechts Claude. */
    private View combinedSearchBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(24));
        bg.setColor(0xFF111111);
        bg.setStroke(dp(1), 0xFF1F1F1F);
        bar.setBackground(bg);

        bar.addView(barHalf("Google", false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        View div = new View(this);
        div.setBackgroundColor(0xFF242424);
        bar.addView(div, new LinearLayout.LayoutParams(dp(1), dp(22)));
        bar.addView(barHalf("Claude", true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return bar;
    }

    private View barHalf(String label, boolean claude) {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER);
        h.setPadding(dp(12), dp(14), dp(12), dp(14));
        View dot = new View(this);
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(claude ? Game.accent(this) : 0xFF555555);
        dot.setBackground(d);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(6), dp(6));
        dotLp.rightMargin = dp(10);
        h.addView(dot, dotLp);
        TextView t = Ui.text(this, label, 15, 0xFF777777);
        t.setTypeface(LIGHT);
        h.addView(t);
        h.setOnClickListener(v -> {
            Intent i = new Intent(this, QueryActivity.class);
            i.putExtra(QueryActivity.EXTRA_CLAUDE, claude);
            Ui.open(this, i);
        });
        return h;
    }

    private View pageDots(int active) {
        LinearLayout dots = new LinearLayout(this);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        dots.setGravity(Gravity.CENTER);
        dots.setPadding(0, dp(8), 0, dp(2));
        for (int i = 0; i < 2; i++) {
            View d = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(i == active ? 0xFF888888 : 0xFF2E2E2E);
            d.setBackground(g);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(6), dp(6));
            lp.leftMargin = dp(4);
            lp.rightMargin = dp(4);
            dots.addView(d, lp);
        }
        return dots;
    }

    /** Wechsel zwischen Seite 1 (Apps) und Seite 2 (Heute), z. B. per Zurück- oder Home-Taste. */
    private void showInfo(boolean show) {
        if (show == onInfo && Math.abs(progress - (show ? 1f : 0f)) < 0.01f) return;
        animateTo(show ? 1f : 0f);
    }

    // ================= Ruhiger Start =================

    private void refreshQuiet() {
        int until = Game.quietUntil(this);
        boolean quiet = until >= 0 && LocalTime.now().getHour() < until;
        allApps.setVisibility(quiet ? View.GONE : View.VISIBLE);
        quietHint.setVisibility(quiet ? View.VISIBLE : View.GONE);
        quietHint.setText("Ruhiger Start bis " + until + ":00");
    }

    // ================= Animation =================

    private void fadeIn() {
        LinearLayout page = onInfo ? info : content;
        for (int i = 0; i < page.getChildCount(); i++) {
            View v = page.getChildAt(i);
            v.setAlpha(0f);
            v.setTranslationY(dp(10));
            v.animate().alpha(1f).translationY(0).setStartDelay(i * 45L).setDuration(380)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private int dp(int v) {
        return Ui.dp(this, v);
    }

    private float dpf() {
        return getResources().getDisplayMetrics().density;
    }

    @Override
    public void onBackPressed() {
        if (onInfo) showInfo(false);
        // Homescreen bleibt Homescreen
    }
}
