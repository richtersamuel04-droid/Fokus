package de.fokus.app;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Fokus-Homescreen v2: dunkel mit Akzentfarbe, Level & Serie,
 * Tagesziele, Challenge des Tages, Spruch des Tages, max. 6 Apps.
 */
public class LauncherActivity extends Activity {

    private int accent;

    private TextView levelText, streakText, dateText, quoteText;
    private ProgressBar xpBar;
    private View glow;
    private TextClock clock;
    private LinearLayout goalsBox, challengeCard, appsBox;
    private TextView goalsLabel, appsLabel;
    private ObjectAnimator glowAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.BLACK);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(28), dp(40), dp(28), dp(28));
        scroll.addView(root);

        // --- Level & Serie ---
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        levelText = Ui.text(this, "", 14, Ui.WHITE);
        levelText.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(levelText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        streakText = Ui.text(this, "", 14, Ui.WHITE);
        streakText.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(streakText);
        root.addView(top);

        xpBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        xpBar.setMax(100);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        barLp.topMargin = dp(8);
        root.addView(xpBar, barLp);

        // --- Uhr mit Leuchten ---
        FrameLayout clockFrame = new FrameLayout(this);
        glow = new View(this);
        FrameLayout.LayoutParams glowLp = new FrameLayout.LayoutParams(dp(220), dp(220));
        glowLp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        glowLp.leftMargin = -dp(40);
        clockFrame.addView(glow, glowLp);

        clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextSize(72);
        clock.setTextColor(Ui.WHITE);
        clock.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        FrameLayout.LayoutParams clockLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clockLp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        clockFrame.addView(clock, clockLp);
        LinearLayout.LayoutParams frameLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(170));
        root.addView(clockFrame, frameLp);

        TextClock date = new TextClock(this);
        date.setFormat24Hour("EEEE, d. MMMM");
        date.setFormat12Hour("EEEE, d. MMMM");
        date.setTextSize(17);
        date.setTextColor(Ui.GRAY);
        dateText = date;
        root.addView(date);

        quoteText = Ui.text(this, "", 14, Ui.GRAY);
        quoteText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        quoteText.setPadding(0, dp(14), 0, dp(6));
        root.addView(quoteText);

        // --- Tagesziele ---
        goalsLabel = label("HEUTE");
        root.addView(goalsLabel);
        goalsBox = new LinearLayout(this);
        goalsBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(goalsBox);

        // --- Challenge ---
        challengeCard = new LinearLayout(this);
        challengeCard.setOrientation(LinearLayout.VERTICAL);
        challengeCard.setPadding(dp(16), dp(12), dp(16), dp(12));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = dp(16);
        root.addView(challengeCard, cardLp);

        // --- Apps ---
        appsLabel = label("APPS");
        root.addView(appsLabel);
        appsBox = new LinearLayout(this);
        appsBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(appsBox);

        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView all = Ui.text(this, "Alle Apps  ›", 15, Ui.DARK_GRAY);
        all.setPadding(0, dp(18), 0, dp(6));
        all.setOnClickListener(v -> startActivity(new Intent(this, AppListActivity.class)));
        root.addView(all);

        View.OnLongClickListener openSettings = v -> {
            startActivity(new Intent(this, MainActivity.class));
            return true;
        };
        root.setOnLongClickListener(openSettings);
        clockFrame.setOnLongClickListener(openSettings);

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (glowAnim != null) glowAnim.cancel();
    }

    // ================= Aufbau =================

    private void refresh() {
        accent = Game.accent(this);
        refreshStats();
        refreshGlow();
        quoteText.setText("„" + Content.quote() + "“");
        goalsLabel.setTextColor(accent);
        appsLabel.setTextColor(accent);
        refreshGoals();
        refreshChallenge();
        refreshApps();
    }

    private void refreshStats() {
        int xp = Game.xp(this);
        int lvl = Game.level(xp);
        int from = Game.xpForLevel(lvl), to = Game.xpForLevel(lvl + 1);
        int pct = (int) (100f * (xp - from) / Math.max(1, to - from));
        levelText.setText("LEVEL " + lvl + "   ·   " + (xp - from) + "/" + (to - from) + " XP");
        int s = Game.streak(this);
        streakText.setText(s > 0 ? "🔥 " + s + (s == 1 ? " Tag" : " Tage") : "🔥 0");
        streakText.setTextColor(s > 0 ? Ui.WHITE : Ui.DARK_GRAY);
        xpBar.setProgress(pct);
        xpBar.setProgressTintList(ColorStateList.valueOf(accent));
        xpBar.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF2A2A2A));
    }

    private void refreshGlow() {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setGradientRadius(dp(110));
        g.setColors(new int[]{withAlpha(accent, 0x66), withAlpha(accent, 0x00)});
        glow.setBackground(g);

        if (glowAnim != null) glowAnim.cancel();
        glowAnim = ObjectAnimator.ofPropertyValuesHolder(glow,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.85f, 1.15f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.85f, 1.15f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.55f, 1f));
        glowAnim.setDuration(4000);
        glowAnim.setRepeatCount(ValueAnimator.INFINITE);
        glowAnim.setRepeatMode(ValueAnimator.REVERSE);
        glowAnim.start();
    }

    private void refreshGoals() {
        goalsBox.removeAllViews();
        List<Game.Goal> goals = Game.goals(this);

        for (int i = 0; i < goals.size(); i++) {
            final int index = i;
            Game.Goal g = goals.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));

            TextView dot = Ui.text(this, g.done ? "●" : "○", 20, accent);
            dot.setPadding(0, 0, dp(14), 0);
            row.addView(dot);

            TextView t = Ui.text(this, g.text, 19, g.done ? Ui.DARK_GRAY : Ui.WHITE);
            if (g.done) t.setPaintFlags(t.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            row.setOnClickListener(v -> toggleGoal(index, row));
            row.setOnLongClickListener(v -> {
                confirmDelete(index);
                return true;
            });
            goalsBox.addView(row);
        }

        if (goals.size() < Game.MAX_GOALS) {
            String s = goals.isEmpty() ? "+  Was willst du heute schaffen?" : "+  Ziel hinzufügen";
            TextView add = Ui.text(this, s, 17, goals.isEmpty() ? accent : Ui.DARK_GRAY);
            add.setPadding(0, dp(8), 0, dp(8));
            add.setOnClickListener(v -> addGoalDialog());
            goalsBox.addView(add);
        }
    }

    private void refreshChallenge() {
        challengeCard.removeAllViews();
        boolean done = Game.challengeDone(this);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(0xFF111111);
        bg.setStroke(dp(1), done ? 0xFF2A2A2A : accent);
        challengeCard.setBackground(bg);

        TextView head = Ui.text(this, done ? "✓  CHALLENGE GESCHAFFT" : "⚡  CHALLENGE DES TAGES  ·  +" + Game.XP_CHALLENGE + " XP",
                12, done ? Ui.DARK_GRAY : accent);
        head.setTypeface(Typeface.DEFAULT_BOLD);
        head.setLetterSpacing(0.08f);
        challengeCard.addView(head);

        TextView body = Ui.text(this, Content.challenge(), 16, done ? Ui.DARK_GRAY : Ui.WHITE);
        body.setPadding(0, dp(6), 0, 0);
        challengeCard.addView(body);

        if (!done) {
            TextView hint = Ui.text(this, "Antippen, wenn erledigt", 12, Ui.DARK_GRAY);
            hint.setPadding(0, dp(6), 0, 0);
            challengeCard.addView(hint);
            challengeCard.setOnClickListener(v -> {
                Game.setChallengeDone(this);
                reward(Game.XP_CHALLENGE, "Challenge geschafft! ⚡");
                pulse(challengeCard);
                challengeCard.postDelayed(this::refresh, 300);
            });
        } else {
            challengeCard.setOnClickListener(null);
            challengeCard.setClickable(false);
        }
    }

    private void refreshApps() {
        appsBox.removeAllViews();
        PackageManager pm = getPackageManager();
        List<String> apps = Prefs.homeApps(this);

        if (apps.isEmpty()) {
            appsBox.addView(Ui.text(this,
                    "Lange auf den Bildschirm drücken, um deine Apps auszuwählen.", 14, Ui.DARK_GRAY));
            return;
        }
        for (String pkg : apps) {
            String name;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                name = pm.getApplicationLabel(ai).toString();
            } catch (PackageManager.NameNotFoundException e) {
                continue;
            }
            TextView t = Ui.text(this, name, 22, Ui.WHITE);
            t.setPadding(0, dp(8), 0, dp(8));
            t.setOnClickListener(v -> Ui.launch(this, pkg));
            appsBox.addView(t);
        }
    }

    // ================= Aktionen =================

    private void toggleGoal(int index, View row) {
        List<Game.Goal> goals = Game.goals(this);
        if (index >= goals.size()) return;
        Game.Goal g = goals.get(index);
        g.done = !g.done;

        int xp = 0;
        String msg = null;
        if (g.done && !g.rewarded) {
            g.rewarded = true;
            xp += Game.XP_GOAL;
            msg = "Ziel geschafft! 🎯";
            boolean allDone = goals.size() >= 2;
            for (Game.Goal x : goals) allDone &= x.done;
            if (allDone) {
                xp += Game.XP_ALL_GOALS;
                msg = "Alle Ziele geschafft! 🏆";
            }
        }
        Game.saveGoals(this, goals);
        if (xp > 0) {
            pulse(row);
            reward(xp, msg);
            row.postDelayed(this::refresh, 300);
        } else {
            refresh();
        }
    }

    /** Vergibt XP, zählt die Serie und zeigt Level-Ups. */
    private void reward(int xp, String msg) {
        int streakBonus = Game.markActiveToday(this);
        int levelUp = Game.addXp(this, xp + streakBonus);
        String extra = streakBonus > 0 ? "  ·  🔥 Serie +" + streakBonus : "";
        Toast.makeText(this, msg + "  +" + xp + " XP" + extra, Toast.LENGTH_SHORT).show();
        if (levelUp > 0) {
            new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                    .setTitle("🎉 Level " + levelUp + " erreicht!")
                    .setMessage(levelMessage(levelUp))
                    .setPositiveButton("Weiter so", null)
                    .show();
        }
    }

    private static String levelMessage(int lvl) {
        if (lvl <= 2) return "Der Anfang ist gemacht. Bleib dran!";
        if (lvl <= 4) return "Du baust gerade echte Gewohnheiten auf.";
        if (lvl <= 7) return "Stark. Die meisten hören viel früher auf.";
        if (lvl <= 10) return "Fokus-Profi. Das merkt man.";
        return "Legendär. Dein Handy arbeitet für dich, nicht umgekehrt.";
    }

    private void addGoalDialog() {
        EditText input = new EditText(this);
        input.setHint("z. B. Angebot fertig schreiben");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setSingleLine(true);
        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(dp(20), dp(8), dp(20), 0);
        wrap.addView(input);

        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Ziel für heute")
                .setView(wrap)
                .setPositiveButton("Hinzufügen", (d, w) -> {
                    String text = input.getText().toString().trim();
                    if (text.isEmpty()) return;
                    List<Game.Goal> goals = Game.goals(this);
                    if (goals.size() >= Game.MAX_GOALS) return;
                    goals.add(new Game.Goal(text, false, false));
                    Game.saveGoals(this, goals);
                    refresh();
                })
                .setNegativeButton("Abbrechen", null)
                .show();
        input.requestFocus();
    }

    private void confirmDelete(int index) {
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Ziel löschen?")
                .setPositiveButton("Löschen", (d, w) -> {
                    List<Game.Goal> goals = Game.goals(this);
                    if (index < goals.size()) goals.remove(index);
                    Game.saveGoals(this, goals);
                    refresh();
                })
                .setNegativeButton("Behalten", null)
                .show();
    }

    // ================= Helfer =================

    private TextView label(String s) {
        TextView t = Ui.text(this, s, 12, Ui.GRAY);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLetterSpacing(0.15f);
        t.setPadding(0, dp(22), 0, dp(4));
        return t;
    }

    private void pulse(View v) {
        v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(120)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(160).start())
                .start();
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
