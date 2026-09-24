package com.vidlix.downloader;

import android.content.Context;
import android.os.Environment;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import java.io.File;

public class Downloader {

    public interface Callback {
        void onProgress(int percent);
        void onComplete(String filePath);
        void onError(String message);
    }

    public static void start(final Context context, final String url, final Callback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Define pasta de destino
                    File downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    if (downloadDir == null) {
                        downloadDir = context.getFilesDir();
                    }
                    if (!downloadDir.exists()) downloadDir.mkdirs();

                    String outputTemplate = new File(downloadDir, "%(title)s.%(ext)s").getAbsolutePath();

                    YoutubeDLRequest request = new YoutubeDLRequest(url);
                    request.addOption("-o", outputTemplate);
                    request.addOption("-f", "bestvideo+bestaudio/best");
                    request.addOption("--merge-output-format", "mp4");
                    request.addOption("--no-playlist");
                    request.addOption("--no-warnings");

                    YoutubeDL.getInstance().execute(request, null, new YoutubeDL.DownloadProgressCallback() {
                        @Override
                        public void onProgressUpdate(float progress, long etaInSeconds) {
                            int percent = (int) (progress * 100);
                            if (percent < 0) percent = 0;
                            if (percent > 100) percent = 100;
                            callback.onProgress(percent);
                        }
                    });

                    callback.onComplete(downloadDir.getAbsolutePath());
                } catch (Exception e) {
                    callback.onError(e.getMessage() != null ? e.getMessage() : "unknown error");
                }
            }
        }).start();
    }
}
