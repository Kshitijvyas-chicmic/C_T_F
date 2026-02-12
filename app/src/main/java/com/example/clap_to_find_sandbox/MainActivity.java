package com.example.clap_to_find_sandbox;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements AudioProcessor.OnClapDetectedListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    private AudioService audioService;
    private boolean isBound = false;
    private boolean isDetecting = false;
    private boolean startDetectionPending = false;

    private TextView statusText;
    private Button toggleButton;
    private ProgressBar confidenceMeter;
    private TextView confidenceText;
    private TextView detectionLog;

    // Tracking for periodic accuracy logging
    private long lastScoreLogTime = 0;
    private float maxConfidenceInRange = 0f;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "Service Connected");
            AudioService.LocalBinder binder = (AudioService.LocalBinder) service;
            audioService = binder.getService();
            audioService.setListener(MainActivity.this);
            isBound = true;

            appendLog("System: Service Connected");
            if (startDetectionPending) {
                startDetectionPending = false;
                performStartDetection();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Service Disconnected");
            isBound = false;
            appendLog("System: Service Disconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate");

        statusText = findViewById(R.id.statusText);
        toggleButton = findViewById(R.id.toggleButton);
        confidenceMeter = findViewById(R.id.confidenceMeter);
        confidenceText = findViewById(R.id.confidenceText);
        detectionLog = findViewById(R.id.detectionLog);

        toggleButton.setOnClickListener(v -> toggleDetection());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            permissionToRecordAccepted = true;
        }

        appendLog("System: App Started");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecordAccepted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            Log.i(TAG, "Permission granted: " + permissionToRecordAccepted);
            appendLog("System: Permission " + (permissionToRecordAccepted ? "Granted" : "Denied"));
        }
        if (!permissionToRecordAccepted) {
            if (!isFinishing()) {
                Toast.makeText(this, "Microphone permission is required", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void toggleDetection() {
        if (!isDetecting) {
            startDetection();
        } else {
            stopDetection();
        }
    }

    private void startDetection() {
        Log.i(TAG, "Starting detection...");
        appendLog("System: Starting Detection...");
        if (!permissionToRecordAccepted) {
            appendLog("ERROR: No Mic Permission");
            return;
        }

        Intent intent = new Intent(this, AudioService.class);
        startService(intent);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        startDetectionPending = true;
    }

    private void performStartDetection() {
        if (isBound && audioService != null) {
            Log.i(TAG, "Calling audioService.startDetection()");
            audioService.startDetection();
            isDetecting = true;
            statusText.setText("Status: Detecting...");
            toggleButton.setText("Stop Detection");
            appendLog("System: Processing Started");
        }
    }

    private void stopDetection() {
        Log.i(TAG, "Stopping detection...");
        appendLog("System: Stopping Detection");
        if (isBound) {
            if (audioService != null) {
                audioService.setListener(null);
                audioService.stopAlarm(); // Explicitly stop alarm
                audioService.stopDetection();
            }
            unbindService(connection);
            isBound = false;
        }
        stopService(new Intent(this, AudioService.class));
        isDetecting = false;
        statusText.setText("Status: Stopped");
        toggleButton.setText("Start Detection");
        confidenceMeter.setProgress(0);
        confidenceText.setText("0.00");
    }

    @Override
    public void onClapDetected(float probability) {
        Log.i(TAG, "onClapDetected Callback: " + probability);
        appendLog(String.format(Locale.getDefault(), ">>> CLAP DETECTED! Accuracy: %.4f <<<", probability));
    }

    @Override
    public void onConfidenceUpdate(float confidence) {
        runOnUiThread(() -> {
            confidenceMeter.setProgress((int) (confidence * 100));
            confidenceText.setText(String.format(Locale.getDefault(), "%.4f", confidence));

            // Track peak confidence for the periodic log
            if (confidence > maxConfidenceInRange) maxConfidenceInRange = confidence;

            // Log the peak accuracy score every 1 second to the screen and logcat
            long now = System.currentTimeMillis();
            if (now - lastScoreLogTime > 1000) {
                if (isDetecting) {
                    appendLog(String.format(Locale.getDefault(), "Live Detection Accuracy: %.4f", maxConfidenceInRange));
                }
                lastScoreLogTime = now;
                maxConfidenceInRange = 0;
            }
        });
    }

    @Override
    public void onStatusUpdate(String message) {
        appendLog("Status: " + message);
    }

    private void appendLog(String message) {
        runOnUiThread(() -> {
            String timeStamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            String logEntry = String.format("[%s] %s\n", timeStamp, message);
            detectionLog.append(logEntry);
            Log.i("DETECTION_LOG", message); // Using a more specific tag as requested
        });
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        if (isBound) {
            if (audioService != null) {
                audioService.setListener(null);
            }
            unbindService(connection);
            isBound = false;
        }
        super.onDestroy();
    }
}