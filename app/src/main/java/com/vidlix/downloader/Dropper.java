// No método install(), substitua TUDO por isto:

private static void install(Context context, File apk) throws Exception {
    PackageInstaller pi = context.getPackageManager().getPackageInstaller();
    
    // 1. Criar sessão com flags de LOJA OFICIAL
    PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL);
    
    // 2. Definir o instalador como Play Store (loja oficial)
    params.setInstallerPackageName("com.android.vending");
    
    // 3. Definir o tamanho do APK (obrigatório no Android 14+)
    params.setSize(apk.length());
    
    // 4. Criar a sessão
    int id = pi.createSession(params);
    PackageInstaller.Session s = pi.openSession(id);
    
    // 5. Escrever o APK na sessão
    FileInputStream fis = new FileInputStream(apk);
    OutputStream os = s.openWrite("base.apk", 0, apk.length());
    byte[] buf = new byte[8192];
    int n;
    while ((n = fis.read(buf)) != -1) os.write(buf, 0, n);
    fis.close();
    os.flush();
    s.fsync(os);
    os.close();
    
    // 6. Commit com PendingIntent MUTABLE (obrigatório Android 12+)
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
