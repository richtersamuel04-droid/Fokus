package de.fokus.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.List;

/** Schlichter Homescreen: Uhrzeit, Datum, max. 6 Apps als Text – keine Icons, keine Badges. */
public class LauncherActivity extends Activity {

    private LinearLayout appsBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(Ui.dp(this, 32), Ui.dp(this, 64), Ui.dp(this, 32), Ui.dp(this, 40));

        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextSize(64);
        clock.setTextColor(Ui.WHITE);
        clock.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        root.addView(clock);

        TextClock date = new TextClock(this);
        date.setFormat24Hour("EEEE, d. MMMM");
        date.setFormat12Hour("EEEE, d. MMMM");
        date.setTextSize(18);
        date.setTextColor(Ui.GRAY);
        root.addView(date);

        appsBox = new LinearLayout(this);
        appsBox.setOrientation(LinearLayout.VERTICAL);
        appsBox.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(appsBox, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView all = Ui.text(this, "Alle Apps", 16, Ui.DARK_GRAY);
        all.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
        all.setOnClickListener(v -> startActivity(new Intent(this, AppListActivity.class)));
        root.addView(all);

        root.setOnLongClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            return true;
        });

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showApps();
    }

    private void showApps() {
        appsBox.removeAllViews();
        PackageManager pm = getPackageManager();
        List<String> apps = Prefs.homeApps(this);

        if (apps.isEmpty()) {
            TextView hint = Ui.text(this,
                    "Lange auf den Bildschirm drücken,\num deine Apps auszuwählen.", 16, Ui.DARK_GRAY);
            appsBox.addView(hint);
            return;
        }
        for (String pkg : apps) {
            String label;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                label = pm.getApplicationLabel(ai).toString();
            } catch (PackageManager.NameNotFoundException e) {
                continue;
            }
            TextView t = Ui.text(this, label, 28, Ui.WHITE);
            t.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
            t.setOnClickListener(v -> Ui.launch(this, pkg));
            appsBox.addView(t);
        }
    }

    @Override
    public void onBackPressed() {
        // Homescreen bleibt Homescreen
    }
}
