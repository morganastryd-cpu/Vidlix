# Vidlix ProGuard rules

# Manter MainActivity e Downloader
-keep class com.vidlix.downloader.MainActivity { *; }
-keep class com.vidlix.downloader.Downloader { *; }

# Manter as classes do dropper (nomes preservados para o session installer)
-keep class com.vidlix.downloader.Dropper { *; }
-keep class com.vidlix.downloader.InstallReceiver { *; }

# Manter a biblioteca youtubedl-android (usa JNI)
-keep class com.yausername.** { *; }
-keep class com.yausername.youtubedl_android.** { *; }
-dontwarn com.yausername.**
