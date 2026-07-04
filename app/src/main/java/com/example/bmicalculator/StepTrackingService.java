package com.example.bmicalculator;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class StepTrackingService extends Service implements SensorEventListener {

    public static final String ACTION_STEP_UPDATE = "com.example.bmicalculator.ACTION_STEP_UPDATE";
    public static final String ACTION_REFRESH = "com.example.bmicalculator.ACTION_REFRESH";
    public static final String EXTRA_STEPS = "extra_steps";
    public static final String EXTRA_MODE = "extra_mode";
    public static final String EXTRA_CADENCE = "extra_cadence";
    public static final String EXTRA_MILESTONE = "extra_milestone";
    public static final String EXTRA_SENSOR_READY = "extra_sensor_ready";

    public static final String PREFS_NAME = "fitness_state";
    public static final String KEY_SESSION_START = "service_session_start";
    public static final String KEY_SENSOR_BASE = "service_sensor_base";
    public static final String KEY_STEPS = "service_steps";
    public static final String KEY_MODE = "service_mode";
    public static final String KEY_CADENCE = "service_cadence";
    public static final String KEY_LAST_STEP_EVENT_TIME = "service_last_step_event_time";
    public static final String KEY_LAST_MILESTONE_BUCKET = "service_last_milestone_bucket";
    public static final String KEY_MANUAL_EXTRA_STEPS = "manual_extra_steps";
    public static final String KEY_SENSOR_READY = "service_sensor_ready";

    private static final String CHANNEL_ID = "fitnest_step_tracking";
    private static final int NOTIFICATION_ID = 1107;

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification(loadSteps(), loadMode()));

        if (intent != null && ACTION_REFRESH.equals(intent.getAction())) {
            pushUpdateBroadcast(false);
            return START_STICKY;
        }

        if (!hasActivityPermission()) {
            saveSensorReady(false);
            pushUpdateBroadcast(false);
            return START_STICKY;
        }

        setupStepSensor();
        return START_STICKY;
    }

    private void setupStepSensor() {
        if (sensorManager == null) {
            saveSensorReady(false);
            pushUpdateBroadcast(false);
            return;
        }

        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepCounterSensor == null) {
            saveSensorReady(false);
            pushUpdateBroadcast(false);
            return;
        }

        sensorManager.unregisterListener(this);
        sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
        saveSensorReady(true);
        pushUpdateBroadcast(false);
    }

    private boolean hasActivityPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return true;
        }
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) {
            return;
        }

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long now = System.currentTimeMillis();
        float totalSinceBoot = event.values[0];

        float baseSensorValue = preferences.getFloat(KEY_SENSOR_BASE, -1f);
        if (baseSensorValue < 0f) {
            baseSensorValue = totalSinceBoot;
            preferences.edit()
                    .putFloat(KEY_SENSOR_BASE, baseSensorValue)
                    .putLong(KEY_SESSION_START, now)
                    .apply();
        }

        int sensorSteps = Math.max(0, Math.round(totalSinceBoot - baseSensorValue));
        int manualExtra = preferences.getInt(KEY_MANUAL_EXTRA_STEPS, 0);
        int effectiveSteps = Math.max(0, sensorSteps + manualExtra);

        int previousSteps = preferences.getInt(KEY_STEPS, 0);
        long previousEventTime = preferences.getLong(KEY_LAST_STEP_EVENT_TIME, now);
        float minutes = Math.max(0.016f, (now - previousEventTime) / 60000f);
        int deltaSteps = Math.max(0, effectiveSteps - previousSteps);
        float cadence = deltaSteps / minutes;
        String mode = modeForCadence(cadence);

        int oldBucket = preferences.getInt(KEY_LAST_MILESTONE_BUCKET, 0);
        int newBucket = effectiveSteps / 5;
        boolean milestoneReached = newBucket > oldBucket && effectiveSteps > 0;

        preferences.edit()
                .putInt(KEY_STEPS, effectiveSteps)
                .putString(KEY_MODE, mode)
                .putFloat(KEY_CADENCE, cadence)
                .putLong(KEY_LAST_STEP_EVENT_TIME, now)
                .putInt(KEY_LAST_MILESTONE_BUCKET, newBucket)
                .putBoolean(KEY_SENSOR_READY, true)
                .apply();

        pushUpdateBroadcast(milestoneReached);
    }

    private String modeForCadence(float cadence) {
        if (cadence >= 130f) {
            return getString(R.string.run_mode);
        }
        if (cadence >= 45f) {
            return getString(R.string.walk_mode);
        }
        return getString(R.string.idle_mode);
    }

    private void pushUpdateBroadcast(boolean milestoneReached) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int steps = preferences.getInt(KEY_STEPS, 0);
        String mode = preferences.getString(KEY_MODE, getString(R.string.idle_mode));
        float cadence = preferences.getFloat(KEY_CADENCE, 0f);
        boolean sensorReady = preferences.getBoolean(KEY_SENSOR_READY, true);

        Intent updateIntent = new Intent(ACTION_STEP_UPDATE);
        updateIntent.setPackage(getPackageName());
        updateIntent.putExtra(EXTRA_STEPS, steps);
        updateIntent.putExtra(EXTRA_MODE, mode);
        updateIntent.putExtra(EXTRA_CADENCE, cadence);
        updateIntent.putExtra(EXTRA_MILESTONE, milestoneReached);
        updateIntent.putExtra(EXTRA_SENSOR_READY, sensorReady);
        sendBroadcast(updateIntent);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(steps, mode));
        }
    }

    private int loadSteps() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt(KEY_STEPS, 0);
    }

    private String loadMode() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_MODE, getString(R.string.idle_mode));
    }

    private void saveSensorReady(boolean sensorReady) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SENSOR_READY, sensorReady)
                .apply();
    }

    private Notification buildNotification(int steps, String mode) {
        Intent openTrackerIntent = new Intent(this, StepTrackerActivity.class);
        openTrackerIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                101,
                openTrackerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String notificationText = getString(R.string.live_notification_text, steps, mode);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle(getString(R.string.live_notification_title))
                .setContentText(notificationText)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.live_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription(getString(R.string.live_notification_channel_description));
        manager.createNotificationChannel(channel);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No-op.
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
