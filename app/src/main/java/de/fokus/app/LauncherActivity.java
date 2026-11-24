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
import android.view.Gravity;
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
    private LinearLayout content;
    private TextView focusPill, importantText, allApps, quietHint;
    private LinearLayout eventBox, screenBox, appsGrid;
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

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundColor(Color.BLACK);
        content.setFocusableInTouchMode(true); // Tastatur nicht automatisch öffnen
        content.setPadding(dp(28), dp(40), dp(28), dp(20));

        // --- Uhr + Fokus ---
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

        importantText = Ui.text(this, "", 15, Ui.GRAY);
        importantText.setTypeface(LIGHT);
        importantText.setPadding(dp(2), dp(14), 0, 0);
        importantText.setOnClickListener(v -> editImportant());
        content.addView(importantText);

        // --- Termine ---
        eventBox = new LinearLayout(this);
        eventBox.setOrientation(LinearLayout.VERTICAL);
        eventBox.setPadding(0, dp(34), 0, 0);
        content.addView(eventBox);

        // --- Bildschirmzeit ---
        screenBox = new LinearLayout(this);
        screenBox.setOrientation(LinearLayout.VERTICAL);
        screenBox.setPadding(0, dp(30), 0, 0);
        content.addView(screenBox);

        // --- Abstand: Apps sitzen unten am Daumen ---
        content.addView(new View(this), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        content.addView(searchBar("Google durchsuchen", false));
        content.addView(searchBar("Claude fragen", true));

        appsGrid = new LinearLayout(this);
        appsGrid.setOrientation(LinearLayout.VERTICAL);
        appsGrid.setPadding(0, dp(10), 0, 0);
        content.addView(appsGrid);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER);
        bottom.setPadding(0, dp(14), 0, dp(4));
        allApps = Ui.text(this, "Alle Apps", 13, Ui.DARK_GRAY);
        allApps.setTypeface(REGULAR);
        allApps.setPadding(dp(16), dp(8), dp(16), dp(8));
        allApps.setOnClickListener(v -> startActivity(new Intent(this, AppListActivity.class)));
        bottom.addView(allApps);
        quietHint = Ui.text(this, "", 13, 0xFF444444);
        quietHint.setTypeface(LIGHT);
        bottom.addView(quietHint);
        content.addView(bottom);

        View.OnLongClickListener openSettings = v -> {
            startActivity(new Intent(this, MainActivity.class));
            return true;
        };
        content.setOnLongClickListener(openSettings);
        head.setOnLongClickListener(openSettings);

        setContentView(content);
    }

    @Override
    protected void onResume() {
        super.onResume();
        accent = Game.accent(this);
        updateFocusPill();
        refreshImportant();
        refreshEvents();
        refreshScreenTime();
        refreshApps();
        refreshQuiet();
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
        long ms = screenTimeToday();
        if (ms < 0) {
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
        long min = ms / 60_000;
        String time = min < 60 ? min + " min" : (min / 60) + " h " + (min % 60) + " min";

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView l = Ui.text(this, "Heute am Handy", 14, Ui.DARK_GRAY);
        l.setTypeface(LIGHT);
        row.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView v = Ui.text(this, time, 14, 0xFFCCCCCC);
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
        int target = (int) Math.min(1000, ms * 1000 / (4 * 3600_000L)); // Linie = 4 Stunden
        ObjectAnimator a = ObjectAnimator.ofInt(bar, "progress", 0, target);
        a.setDuration(900);
        a.setInterpolator(new DecelerateInterpolator());
        a.start();
    }

    /** Bildschirmzeit seit Mitternacht in ms (ohne Fokus selbst), -1 ohne Berechtigung. */
    private long screenTimeToday() {
        AppOpsManager ops = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
        int mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName());
        if (mode != AppOpsManager.MODE_ALLOWED) return -1;

        UsageStatsManager um = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        long start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long now = System.currentTimeMillis();
        UsageEvents events = um.queryEvents(start, now);
        Map<String, Long> open = new HashMap<>();
        long total = 0;
        UsageEvents.Event e = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(e);
            String pkg = e.getPackageName();
            int type = e.getEventType();
            if (type == 1) { // Vordergrund
                if (!open.containsKey(pkg)) open.put(pkg, e.getTimeStamp());
            } else if (type == 2) { // Hintergrund
                Long s = open.remove(pkg);
                if (s != null && !pkg.equals(getPackageName())) total += e.getTimeStamp() - s;
            } else if (type == 16) { // Bildschirm aus
                for (Map.Entry<String, Long> en : open.entrySet()) {
                    if (!en.getKey().equals(getPackageName())) total += e.getTimeStamp() - en.getValue();
                }
                open.clear();
            }
        }
        return total;
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
                        Ui.launch(this, pkg);
                    }).start();
        });
        return cell;
    }

    // ================= Google- & Claude-Leiste =================

    private static final String CLAUDE_PKG = "com.anthropic.claude";

    private View searchBar(String hint, boolean claude) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(18), 0, dp(10), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(24));
        bg.setColor(0xFF111111);
        bg.setStroke(dp(1), 0xFF1F1F1F);
        bar.setBackground(bg);

        View dot = new View(this);
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(claude ? Game.accent(this) : 0xFF555555);
        dot.setBackground(d);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(7), dp(7));
        dotLp.rightMargin = dp(14);
        bar.addView(dot, dotLp);

        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(0xFF555555);
        input.setTextColor(Ui.WHITE);
        input.setTextSize(15);
        input.setTypeface(LIGHT);
        input.setBackground(null);
        input.setSingleLine(true);
        input.setPadding(0, dp(13), 0, dp(13));
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setImeOptions(claude ? EditorInfo.IME_ACTION_SEND : EditorInfo.IME_ACTION_SEARCH);
        input.setOnEditorActionListener((v, actionId, event) -> {
            submit(input, claude);
            return true;
        });
        bar.addView(input, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView go = Ui.text(this, "›", 22, claude ? Game.accent(this) : Ui.GRAY);
        go.setPadding(dp(12), dp(4), dp(8), dp(6));
        go.setOnClickListener(v -> submit(input, claude));
        bar.addView(go);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        bar.setLayoutParams(lp);
        return bar;
    }

    private void submit(EditText input, boolean claude) {
        String q = input.getText().toString().trim();
        input.setText("");
        input.clearFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        if (claude) askClaude(q);
        else searchGoogle(q);
    }

    private void searchGoogle(String q) {
        if (q.isEmpty()) {
            openUrl("https://www.google.com");
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_WEB_SEARCH);
            i.putExtra(SearchManager.QUERY, q);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            openUrl("https://www.google.com/search?q=" + Uri.encode(q));
        }
    }

    private void askClaude(String q) {
        if (q.isEmpty()) {
            Intent launch = getPackageManager().getLaunchIntentForPackage(CLAUDE_PKG);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launch);
            } else {
                openUrl("https://claude.ai/new");
            }
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, q);
            i.setPackage(CLAUDE_PKG);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            openUrl("https://claude.ai/new?q=" + Uri.encode(q));
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
        for (int i = 0; i < content.getChildCount(); i++) {
            View v = content.getChildAt(i);
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

    @Override
    public void onBackPressed() {
        // Homescreen bleibt Homescreen
    }
}
