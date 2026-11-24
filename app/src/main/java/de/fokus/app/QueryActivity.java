package de.fokus.app;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Eigenes ruhiges Eingabefenster für Google-Suche oder Claude-Frage. */
public class QueryActivity extends Activity {

    static final String EXTRA_CLAUDE = "claude";
    private static final String CLAUDE_PKG = "com.anthropic.claude";
    private boolean claude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        claude = getIntent().getBooleanExtra(EXTRA_CLAUDE, false);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        int accent = Game.accent(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(Ui.dp(this, 24), Ui.dp(this, 48), Ui.dp(this, 24), Ui.dp(this, 24));

        TextView title = Ui.text(this, claude ? "CLAUDE FRAGEN" : "GOOGLE", 11, claude ? accent : Ui.GRAY);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setLetterSpacing(0.2f);
        title.setPadding(Ui.dp(this, 4), 0, 0, Ui.dp(this, 12));
        root.addView(title);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 8), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(Ui.dp(this, 26));
        bg.setColor(0xFF111111);
        bg.setStroke(Ui.dp(this, 1), claude ? accent : 0xFF333333);
        bar.setBackground(bg);

        EditText input = new EditText(this);
        input.setHint(claude ? "Was willst du wissen?" : "Suchen");
        input.setHintTextColor(0xFF555555);
        input.setTextColor(Ui.WHITE);
        input.setTextSize(17);
        input.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        input.setBackground(null);
        input.setPadding(0, Ui.dp(this, 15), 0, Ui.dp(this, 15));
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setSingleLine(true);
        input.setImeOptions(claude ? EditorInfo.IME_ACTION_SEND : EditorInfo.IME_ACTION_SEARCH);
        input.setOnEditorActionListener((v, id, e) -> {
            submit(input.getText().toString().trim());
            return true;
        });
        bar.addView(input, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView go = Ui.text(this, "›", 26, claude ? accent : Ui.GRAY);
        go.setPadding(Ui.dp(this, 14), Ui.dp(this, 2), Ui.dp(this, 12), Ui.dp(this, 6));
        go.setOnClickListener(v -> submit(input.getText().toString().trim()));
        bar.addView(go);
        root.addView(bar);

        if (claude) {
            TextView open = Ui.text(this, "Claude-App öffnen", 14, Ui.DARK_GRAY);
            open.setPadding(Ui.dp(this, 4), Ui.dp(this, 18), 0, 0);
            open.setOnClickListener(v -> submit(""));
            root.addView(open);
        }

        // Tipp auf leere Fläche schließt das Fenster
        root.setOnClickListener(v -> finish());
        setContentView(root);
        input.requestFocus();
    }

    private void submit(String q) {
        if (claude) askClaude(q);
        else searchGoogle(q);
        finish();
    }

    private void searchGoogle(String q) {
        if (q.isEmpty()) {
            openUrl("https://www.google.com");
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_WEB_SEARCH);
            i.putExtra(SearchManager.QUERY, q);
            startActivity(i);
        } catch (Exception e) {
            openUrl("https://www.google.com/search?q=" + Uri.encode(q));
        }
    }

    private void askClaude(String q) {
        if (q.isEmpty()) {
            Intent launch = getPackageManager().getLaunchIntentForPackage(CLAUDE_PKG);
            if (launch != null) startActivity(launch);
            else openUrl("https://claude.ai/new");
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, q);
            i.setPackage(CLAUDE_PKG);
            startActivity(i);
        } catch (Exception e) {
            openUrl("https://claude.ai/new?q=" + Uri.encode(q));
        }
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {
        }
    }
}
