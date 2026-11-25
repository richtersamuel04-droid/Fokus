package de.fokus.app;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Beobachtet nur Instagram. Sobald Reels sichtbar sind (Reels-Tab oder
 * Vollbild-Reels aus Feed/DMs), wird man sofort zurückgeschickt.
 */
public class ReelsBlockerService extends AccessibilityService {

    private static final String IG = "com.instagram.android";

    /** Ansichten, die nur im Reels-Player vorkommen. */
    private static final String[] REELS_VIEW_IDS = {
            IG + ":id/clips_viewer_view_pager",
            IG + ":id/clips_viewer_container",
            IG + ":id/clips_video_container",
            IG + ":id/root_clips_layout",
            IG + ":id/clips_swipe_refresh_container",
    };
    private static final String REELS_TAB_ID = IG + ":id/clips_tab";
    private static final String FEED_TAB_ID = IG + ":id/feed_tab";

    private static final int NONE = 0, PLAYER = 1, TAB = 2;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean checkPending = false;
    private long lastBlock = 0;
    private long lastToast = 0;

    private final Runnable check = () -> {
        checkPending = false;
        List<AccessibilityNodeInfo> roots = instagramRoots();
        if (roots.isEmpty()) return;
        if (Game.focusActive(this)) {
            blockAll();
            return;
        }
        boolean diag = Diagnose.on(this);
        for (AccessibilityNodeInfo root : roots) {
            int where = detectReels(root, diag);
            if (where != NONE) {
                block(root, where);
                return;
            }
        }
    };

    /** Alle sichtbaren Instagram-Fenster (nicht nur das aktive). */
    private List<AccessibilityNodeInfo> instagramRoots() {
        List<AccessibilityNodeInfo> out = new ArrayList<>();
        try {
            for (AccessibilityWindowInfo w : getWindows()) {
                AccessibilityNodeInfo r = w.getRoot();
                if (r != null && r.getPackageName() != null && IG.equals(r.getPackageName().toString())) out.add(r);
            }
        } catch (Exception ignored) {
        }
        if (out.isEmpty()) {
            AccessibilityNodeInfo r = getRootInActiveWindow();
            if (r != null && r.getPackageName() != null && IG.equals(r.getPackageName().toString())) out.add(r);
        }
        return out;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null) return;
        if (!IG.equals(event.getPackageName().toString())) return;
        if (!checkPending) {
            checkPending = true;
            handler.postDelayed(check, 100);
        }
    }

    /**
     * Erkennung in drei Stufen:
     * 1. bekannte IDs des Reels-Players,
     * 2. jede sichtbare Ansicht mit "clips" in der ID, die fast den ganzen Bildschirm füllt
     *    (Vollbild-Reel aus Feed, DMs, Suche, Profil),
     * 3. ausgewählter Reels-Tab (per ID oder Name).
     */
    private int detectReels(AccessibilityNodeInfo root, boolean diag) {
        for (String id : REELS_VIEW_IDS) {
            if (anyVisible(root.findAccessibilityNodeInfosByViewId(id))) return PLAYER;
        }
        int screenH = getResources().getDisplayMetrics().heightPixels;
        Rect r = new Rect();
        ArrayDeque<AccessibilityNodeInfo> stack = new ArrayDeque<>();
        stack.push(root);
        int visited = 0;
        int result = NONE;
        while (!stack.isEmpty() && visited < 4000) {
            AccessibilityNodeInfo n = stack.pop();
            visited++;
            if (n == null) continue;
            String id = n.getViewIdResourceName();
            boolean visible = n.isVisibleToUser();
            if (visible) n.getBoundsInScreen(r);
            if (diag && id != null && visible) Diagnose.record(this, id, r.height() * 100 / Math.max(1, screenH), n.isSelected());

            if (visible && id != null) {
                String low = id.toLowerCase(Locale.ROOT);
                if (low.contains("clips") && !low.contains("clips_tab") && r.height() >= screenH * 0.6) {
                    result = PLAYER;
                    if (!diag) return result;
                }
                if (low.endsWith(":id/clips_tab") && n.isSelected()) {
                    if (result == NONE) result = TAB;
                    if (!diag) return result;
                }
            }
            if (visible && n.isSelected()) {
                CharSequence cd = n.getContentDescription(), tx = n.getText();
                if ((cd != null && cd.toString().trim().equalsIgnoreCase("Reels"))
                        || (tx != null && tx.toString().trim().equalsIgnoreCase("Reels"))) {
                    if (result == NONE) result = TAB;
                    if (!diag) return result;
                }
            }
            for (int i = n.getChildCount() - 1; i >= 0; i--) stack.push(n.getChild(i));
        }
        return result;
    }

    private static boolean anyVisible(List<AccessibilityNodeInfo> nodes) {
        if (nodes == null) return false;
        for (AccessibilityNodeInfo n : nodes) {
            if (n != null && n.isVisibleToUser()) return true;
        }
        return false;
    }

    private void block(AccessibilityNodeInfo root, int where) {
        long now = SystemClock.uptimeMillis();
        if (now - lastBlock < 700) return;
        lastBlock = now;

        boolean done = false;
        if (where == TAB) {
            // Im Reels-Tab: zurück auf den normalen Feed
            List<AccessibilityNodeInfo> feed = root.findAccessibilityNodeInfosByViewId(FEED_TAB_ID);
            if (feed != null) {
                for (AccessibilityNodeInfo n : feed) {
                    if (n != null && n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        done = true;
                        break;
                    }
                }
            }
        }
        if (!done) performGlobalAction(GLOBAL_ACTION_BACK);

        Prefs.incBlockCount(this);
        if (now - lastToast > 4000) {
            lastToast = now;
            Toast.makeText(this, "Reels sind blockiert", Toast.LENGTH_SHORT).show();
        }
        // Nachprüfen: Falls man noch in Reels ist, wird erneut zurückgeschickt.
        handler.postDelayed(check, 800);
    }

    /** Während eines Fokusblocks ist Instagram komplett gesperrt. */
    private void blockAll() {
        long now = SystemClock.uptimeMillis();
        if (now - lastBlock < 700) return;
        lastBlock = now;
        performGlobalAction(GLOBAL_ACTION_HOME);
        long min = Math.max(1, (Game.focusEnd(this) - System.currentTimeMillis() + 59_999) / 60_000);
        if (now - lastToast > 4000) {
            lastToast = now;
            Toast.makeText(this, "Fokusblock läuft noch " + min + " Min – Instagram ist gesperrt",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ---------- Für Wischgesten auf dem Homescreen ----------
    private static ReelsBlockerService instance;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        instance = null;
        return super.onUnbind(intent);
    }

    /** Führt eine System-Aktion aus (z. B. Bildschirm sperren). false, wenn der Dienst nicht läuft. */
    static boolean global(int action) {
        return instance != null && instance.performGlobalAction(action);
    }

    @Override
    public void onInterrupt() {}
}
