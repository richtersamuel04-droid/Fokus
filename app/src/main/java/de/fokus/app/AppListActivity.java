package de.fokus.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Alle Apps als schlichte Textliste mit Suche – oder Auswahl der Homescreen-Apps. */
public class AppListActivity extends Activity {

    static final String EXTRA_PICK = "pick";
    private static final int MAX_HOME_APPS = 8;

    private final List<String[]> apps = new ArrayList<>();   // {Name, Paket}
    private final List<String[]> shown = new ArrayList<>();
    private final Set<String> selected = new LinkedHashSet<>();
    private ListView list;
    private boolean pick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.BLACK);
        pick = getIntent().getBooleanExtra(EXTRA_PICK, false);
        selected.addAll(Prefs.homeApps(this));
        loadApps();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        int pad = Ui.dp(this, 16);
        root.setPadding(pad, Ui.dp(this, 32), pad, pad);

        if (pick) {
            root.addView(Ui.text(this, "Wähle bis zu " + MAX_HOME_APPS
                    + " Apps für deinen Homescreen.\nNur was du wirklich brauchst.", 16, Ui.GRAY));
        }

        EditText search = new EditText(this);
        search.setHint("Suchen …");
        search.setTextColor(Ui.WHITE);
        search.setHintTextColor(Ui.DARK_GRAY);
        search.setSingleLine(true);
        root.addView(search);

        list = new ListView(this);
        root.addView(list, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        if (pick) {
            list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            list.setOnItemClickListener((parent, view, pos, id) -> {
                String pkg = shown.get(pos)[1];
                if (list.isItemChecked(pos)) {
                    if (selected.size() >= MAX_HOME_APPS) {
                        list.setItemChecked(pos, false);
                        Toast.makeText(this, "Maximal " + MAX_HOME_APPS + " Apps – weniger ist mehr.",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        selected.add(pkg);
                    }
                } else {
                    selected.remove(pkg);
                }
            });
            root.addView(Ui.button(this, "Speichern", () -> {
                Prefs.setHomeApps(this, new ArrayList<>(selected));
                finish();
            }));
        } else {
            list.setOnItemClickListener((parent, view, pos, id) -> {
                Ui.launch(this, shown.get(pos)[1]);
                finish();
            });
        }

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { filter(s.toString()); }
        });

        setContentView(root);
        filter("");
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        Set<String> seen = new HashSet<>();
        for (ResolveInfo r : pm.queryIntentActivities(i, 0)) {
            String pkg = r.activityInfo.packageName;
            if (pick && pkg.equals(getPackageName())) continue;
            if (!seen.add(pkg)) continue;
            apps.add(new String[]{r.loadLabel(pm).toString(), pkg});
        }
        apps.sort((a, b) -> a[0].compareToIgnoreCase(b[0]));
    }

    private void filter(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        shown.clear();
        List<String> labels = new ArrayList<>();
        for (String[] a : apps) {
            if (q.isEmpty() || a[0].toLowerCase(Locale.ROOT).contains(q)) {
                shown.add(a);
                labels.add(a[0]);
            }
        }
        int layout = pick ? android.R.layout.simple_list_item_multiple_choice
                : android.R.layout.simple_list_item_1;
        list.setAdapter(new ArrayAdapter<>(this, layout, labels));
        if (pick) {
            for (int i = 0; i < shown.size(); i++) {
                list.setItemChecked(i, selected.contains(shown.get(i)[1]));
            }
        }
    }
}
