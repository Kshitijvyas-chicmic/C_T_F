package com.example.clap_to_find_sandbox;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class AudioService extends Service {
    private static final String TAG = "AudioService";
    private static final String CHANNEL_ID = "ClapDetectionChannel";

    private AudioProcessor processor;
    private ClapDetector detector;
    private final IBinder binder = new LocalBinder();
    private AudioProcessor.OnClapDetectedListener listener;

    public class LocalBinder extends Binder {
        AudioService getService() {
            return AudioService.this;
        }
    }

    private android.media.Ringtone alarmRingtone;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, getNotification("Clap detection is running..."));
    }

    public void setListener(AudioProcessor.OnClapDetectedListener listener) {
        this.listener = listener;
    }

    public void startDetection() {
        if (processor != null) return;

        try {
            detector = new ClapDetector(this);

            // Create a wrapper listener to intercept the clap event
            AudioProcessor.OnClapDetectedListener wrapperListener = new AudioProcessor.OnClapDetectedListener() {
                @Override
                public void onClapDetected(float probability) {
                    playAlarm(); // TRIGGER ALARM
                    if (listener != null) listener.onClapDetected(probability); // Notify UI
                }

                @Override
                public void onConfidenceUpdate(float confidence) {
                    if (listener != null) listener.onConfidenceUpdate(confidence);
                }

                @Override
                public void onStatusUpdate(String message) {
                    if (listener != null) listener.onStatusUpdate(message);
                }
            };

            processor = new AudioProcessor(this, detector, wrapperListener);
            new Thread(processor).start();
            Log.d(TAG, "Detection started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start detection", e);
        }
    }

    public void stopDetection() {
        if (processor != null) {
            processor.stop();
            processor = null;
        }
        if (detector != null) {
            detector.close();
            detector = null;
        }
        stopAlarm(); // Stop alarm when detection stops
        Log.d(TAG, "Detection stopped");
    }

    public void playAlarm() {
        try {
            if (alarmRingtone == null) {
                android.net.Uri notification = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM);
                alarmRingtone = android.media.RingtoneManager.getRingtone(getApplicationContext(), notification);
            }
            if (alarmRingtone != null && !alarmRingtone.isPlaying()) {
                alarmRingtone.play();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing alarm", e);
        }
    }

    public void stopAlarm() {
        try {
            if (alarmRingtone != null && alarmRingtone.isPlaying()) {
                alarmRingtone.stop();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping alarm", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        stopDetection();
        super.onDestroy();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Clap Detection Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification getNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Clap Detector")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .build();
    }
}