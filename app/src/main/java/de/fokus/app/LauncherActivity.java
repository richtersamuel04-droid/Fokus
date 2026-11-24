package de.fokus.app;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.List;

/** Ruhiger Homescreen: Uhr, Datum, Tagesziele, max. 6 Apps. Dunkel mit dezenter Akzentfarbe. */
public class LauncherActivity extends Activity {

    private static final Typeface THIN = Typeface.create("sans-serif-thin", Typeface.NORMAL);
    private static final Typeface LIGHT = Typeface.create("sans-serif-light", Typeface.NORMAL);
    private static final Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);

    private int accent;
    private View glow, divider;
    private LinearLayout goalsBox, appsBox;
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
        root.setPadding(dp(32), dp(48), dp(32), dp(28));
        scroll.addView(root);

        // Uhr mit dezentem Leuchten
        FrameLayout clockFrame = new FrameLayout(this);
        glow = new View(this);
        FrameLayout.LayoutParams glowLp = new FrameLayout.LayoutParams(dp(260), dp(260));
        glowLp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        glowLp.leftMargin = -dp(70);
        clockFrame.addView(glow, glowLp);

        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextSize(84);
        clock.setTextColor(Ui.WHITE);
        clock.setTypeface(THIN);
        clock.setIncludeFontPadding(false);
        FrameLayout.LayoutParams clockLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clockLp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        clockFrame.addView(clock, clockLp);
        root.addView(clockFrame, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(150)));

        TextClock date = new TextClock(this);
        date.setFormat24Hour("EEEE, d. MMMM");
        date.setFormat12Hour("EEEE, d. MMMM");
        date.setTextSize(17);
        date.setTextColor(Ui.GRAY);
        date.setTypeface(LIGHT);
        root.addView(date);

        divider = new View(this);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(dp(48), dp(2));
        divLp.topMargin = dp(28);
        root.addView(divider, divLp);

        root.addView(label("HEUTE"));
        goalsBox = new LinearLayout(this);
        goalsBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(goalsBox);

        root.addView(label("APPS"));
        appsBox = new LinearLayout(this);
        appsBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(appsBox);

        root.addView(new View(this), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView all = Ui.text(this, "Alle Apps", 14, Ui.DARK_GRAY);
        all.setTypeface(LIGHT);
        all.setLetterSpacing(0.05f);
        all.setPadding(0, dp(20), 0, dp(6));
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

    private void refresh() {
        accent = Game.accent(this);
        divider.setBackgroundColor(accent);
        refreshGlow();
        refreshGoals();
        refreshApps();
    }

    private void refreshGlow() {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setGradientRadius(dp(130));
        g.setColors(new int[]{withAlpha(accent, 0x38), withAlpha(accent, 0x00)});
        glow.setBackground(g);

        if (glowAnim != null) glowAnim.cancel();
        glowAnim = ObjectAnimator.ofPropertyValuesHolder(glow,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.92f, 1.08f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.92f, 1.08f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.6f, 1f));
        glowAnim.setDuration(6000);
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
            row.setPadding(0, dp(9), 0, dp(9));

            View box = new View(this);
            GradientDrawable b = new GradientDrawable();
            b.setCornerRadius(dp(3));
            b.setStroke(dp(1), g.done ? accent : Ui.GRAY);
            b.setColor(g.done ? accent : Color.TRANSPARENT);
            box.setBackground(b);
            LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(dp(16), dp(16));
            boxLp.rightMargin = dp(16);
            row.addView(box, boxLp);

            TextView t = Ui.text(this, g.text, 18, g.done ? Ui.DARK_GRAY : Ui.WHITE);
            t.setTypeface(LIGHT);
            if (g.done) t.setPaintFlags(t.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            row.setOnClickListener(v -> {
                List<Game.Goal> list = Game.goals(this);
                if (index < list.size()) {
                    list.get(index).done = !list.get(index).done;
                    Game.saveGoals(this, list);
                }
                refreshGoals();
            });
            row.setOnLongClickListener(v -> {
                confirmDelete(index);
                return true;
            });
            goalsBox.addView(row);
        }

        if (goals.size() < Game.MAX_GOALS) {
            TextView add = Ui.text(this, goals.isEmpty() ? "Ziel für heute festlegen" : "Weiteres Ziel",
                    15, Ui.DARK_GRAY);
            add.setTypeface(LIGHT);
            add.setPadding(0, dp(9), 0, dp(9));
            add.setOnClickListener(v -> addGoalDialog());
            goalsBox.addView(add);
        }
    }

    private void refreshApps() {
        appsBox.removeAllViews();
        PackageManager pm = getPackageManager();
        List<String> apps = Prefs.homeApps(this);

        if (apps.isEmpty()) {
            TextView hint = Ui.text(this, "Lange drücken, um Apps auszuwählen", 15, Ui.DARK_GRAY);
            hint.setTypeface(LIGHT);
            appsBox.addView(hint);
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
            TextView t = Ui.text(this, name, 24, Ui.WHITE);
            t.setTypeface(LIGHT);
            t.setPadding(0, dp(7), 0, dp(7));
            t.setOnClickListener(v -> Ui.launch(this, pkg));
            appsBox.addView(t);
        }
    }

    private void addGoalDialog() {
        EditText input = new EditText(this);
        input.setHint("Was ist heute wichtig?");
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
                    goals.add(new Game.Goal(text, false));
                    Game.saveGoals(this, goals);
                    refreshGoals();
                })
                .setNegativeButton("Abbrechen", null)
                .show();
        input.requestFocus();
    }

    private void confirmDelete(int index) {
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Ziel entfernen?")
                .setPositiveButton("Entfernen", (d, w) -> {
                    List<Game.Goal> goals = Game.goals(this);
                    if (index < goals.size()) goals.remove(index);
                    Game.saveGoals(this, goals);
                    refreshGoals();
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private TextView label(String s) {
        TextView t = Ui.text(this, s, 11, Ui.DARK_GRAY);
        t.setTypeface(MEDIUM);
        t.setLetterSpacing(0.2f);
        t.setPadding(0, dp(26), 0, dp(6));
        return t;
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
