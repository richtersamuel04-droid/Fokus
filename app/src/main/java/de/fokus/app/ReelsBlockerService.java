package de.fokus.app;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;

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
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null) return;
        if (!IG.equals(root.getPackageName().toString())) return;
        int where = detectReels(root);
        if (where != NONE) block(root, where);
    };

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null) return;
        if (!IG.equals(event.getPackageName().toString())) return;
        if (!checkPending) {
            checkPending = true;
            handler.postDelayed(check, 120);
        }
    }

    private int detectReels(AccessibilityNodeInfo root) {
        for (String id : REELS_VIEW_IDS) {
            if (anyVisible(root.findAccessibilityNodeInfosByViewId(id))) return PLAYER;
        }
        List<AccessibilityNodeInfo> tabs = root.findAccessibilityNodeInfosByViewId(REELS_TAB_ID);
        if (tabs != null) {
            for (AccessibilityNodeInfo n : tabs) {
                if (n != null && n.isSelected()) return TAB;
            }
        }
        // Rückfall, falls Instagram seine internen IDs ändert:
        // ein ausgewählter Tab/Knopf mit dem Namen "Reels".
        List<AccessibilityNodeInfo> named = root.findAccessibilityNodeInfosByText("Reels");
        if (named != null) {
            for (AccessibilityNodeInfo n : named) {
                if (n != null && n.isSelected() && n.isVisibleToUser()) return TAB;
            }
        }
        return NONE;
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

    @Override
    public void onInterrupt() {}
}
