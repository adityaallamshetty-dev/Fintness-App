package com.example.bmicalculator;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StepTrackerActivity extends AppCompatActivity {

    private static final int REQUEST_ACTIVITY_RECOGNITION = 3001;
    private static final int REQUEST_NOTIFICATIONS = 3002;
    private static final int DEFAULT_CALORIE_GOAL = FitnessState.DEFAULT_CALORIE_GOAL;
    private static final int STEP_RING_GOAL = 10000;
    private static final int BASE_TOTAL_CALORIES = 1180;
    private static final float KM_PER_STEP = FitnessState.KM_PER_STEP;
    private static final float CALORIES_PER_STEP = FitnessState.CALORIES_PER_STEP;
    private static final String PREFS_NAME = StepTrackingService.PREFS_NAME;
    private static final String KEY_CALORIE_GOAL = FitnessState.KEY_CALORIE_GOAL;
    private static final String KEY_HYDRATION_ML = FitnessState.KEY_HYDRATION_ML;
    private static final String KEY_SELECTED_MOOD = FitnessState.KEY_SELECTED_MOOD;

    private ImageButton btnBack;
    private ImageButton btnCalendar;
    private ImageButton btnShare;
    private MaterialButton btnQuickAdd;
    private MaterialButton btnQuickRemove;
    private MaterialButton btnStartWalk;
    private MaterialButton btnStartRun;
    private MaterialButton btnFindRoute;
    private MaterialButton btnOpenFitness;
    private MaterialButton btnOpenSharing;
    private MaterialButton btnMoodCalm;
    private MaterialButton btnMoodFocus;
    private MaterialButton btnMoodStress;
    private MaterialButton btnMoodLow;
    private MaterialButton btnAddWater;
    private MaterialButton btnRemoveWater;
    private MaterialButton btnCalculateBmiTracker;

    private TextView tvDateTitle;
    private TextView tvMoveValue;
    private TextView tvGoalLabel;
    private TextView tvGoalProgress;
    private TextView tvStepCount;
    private TextView tvDistance;
    private TextView tvActiveCalories;
    private TextView tvMode;
    private TextView tvChartTopValue;
    private TextView tvTotalCalories;
    private TextView tvStatus;
    private TextView tvAdaptiveCoach;
    private TextView tvRecoveryScore;
    private TextView tvEcoImpact;
    private TextView tvHydrationValue;
    private TextView tvBmiResult;
    private TextView tvRingSteps;
    private TextView tvRingMode;
    private TextView[] dayLabels;

    private TextInputEditText bmiWeightInput;
    private TextInputEditText bmiFeetInput;
    private TextInputEditText bmiInchesInput;

    private LinearProgressIndicator progressDailyGoal;
    private CircularProgressIndicator progressStepRing;
    private LinearLayout chartBars;

    private final List<View> barViews = new ArrayList<>();
    private final float[] hourlyCalories = new float[24];
    private final int[] baseWeekProgress = new int[]{62, 55, 71, 47, 68, 39, 52};
    private final String[] dayNames = new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private final List<Float> cadenceSamples = new ArrayList<>();

    private int sensorSteps = 0;
    private int sessionSteps = 0;
    private int lastSessionSteps = -1;
    private int manualExtraSteps = 0;
    private int currentGoalProgress = 0;
    private int calorieGoal = DEFAULT_CALORIE_GOAL;
    private int hydrationMl = 0;
    private String selectedMood = "Calm";
    private String liveMode = "";
    private float liveCadence = 0f;
    private long sessionStartTimeMillis = System.currentTimeMillis();
    private long selectedDateMillis = System.currentTimeMillis();
    private boolean stepReceiverRegistered = false;

    private final BroadcastReceiver liveStepReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!StepTrackingService.ACTION_STEP_UPDATE.equals(intent.getAction())) {
                return;
            }

            boolean sensorReady = intent.getBooleanExtra(StepTrackingService.EXTRA_SENSOR_READY, true);
            if (!sensorReady) {
                tvStatus.setText(getString(R.string.step_sensor_missing));
                return;
            }

            int updatedSteps = intent.getIntExtra(StepTrackingService.EXTRA_STEPS, sessionSteps);
            String updatedMode = intent.getStringExtra(StepTrackingService.EXTRA_MODE);
            liveCadence = intent.getFloatExtra(StepTrackingService.EXTRA_CADENCE, liveCadence);
            boolean milestoneReached = intent.getBooleanExtra(StepTrackingService.EXTRA_MILESTONE, false);

            if (updatedMode != null) {
                liveMode = updatedMode;
            }

            applyLiveStepUpdate(updatedSteps);
            updateMetrics();

            if (milestoneReached) {
                tvStatus.setText(getString(R.string.live_status_milestone, sessionSteps, liveMode));
            } else {
                tvStatus.setText(getString(R.string.live_status_active, sessionSteps, liveMode));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_step_tracker);

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(true);
            insetsController.setAppearanceLightNavigationBars(true);
        }

        bindViews();
        loadUserState();
        loadLiveTrackingSnapshot();
        setupTopBarActions();
        setupActionButtons();
        setupMoodButtons();
        setupDaySelectors();
        setupDateTitle();
        buildChartBars();
        refreshMoodSelection();
        updateMetrics();
        ensurePermissionThenStartTracking();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerLiveStepReceiver();
        loadLiveTrackingSnapshot();
        updateMetrics();
        requestTrackingRefresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterLiveStepReceiver();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        btnCalendar = findViewById(R.id.btnCalendar);
        btnShare = findViewById(R.id.btnShare);
        btnQuickAdd = findViewById(R.id.btnQuickAdd);
        btnQuickRemove = findViewById(R.id.btnQuickRemove);
        btnStartWalk = findViewById(R.id.btnStartWalk);
        btnStartRun = findViewById(R.id.btnStartRun);
        btnFindRoute = findViewById(R.id.btnFindRoute);
        btnOpenFitness = findViewById(R.id.btnOpenFitness);
        btnOpenSharing = findViewById(R.id.btnOpenSharing);
        btnMoodCalm = findViewById(R.id.btnMoodCalm);
        btnMoodFocus = findViewById(R.id.btnMoodFocus);
        btnMoodStress = findViewById(R.id.btnMoodStress);
        btnMoodLow = findViewById(R.id.btnMoodLow);
        btnAddWater = findViewById(R.id.btnAddWater);
        btnRemoveWater = findViewById(R.id.btnRemoveWater);
        btnCalculateBmiTracker = findViewById(R.id.btnCalculateBmiTracker);

        tvDateTitle = findViewById(R.id.tvDateTitle);
        tvMoveValue = findViewById(R.id.tvMoveValue);
        tvGoalLabel = findViewById(R.id.tvGoalLabel);
        tvGoalProgress = findViewById(R.id.tvGoalProgress);
        tvStepCount = findViewById(R.id.tvStepCount);
        tvDistance = findViewById(R.id.tvDistance);
        tvActiveCalories = findViewById(R.id.tvActiveCalories);
        tvMode = findViewById(R.id.tvMode);
        tvChartTopValue = findViewById(R.id.tvChartTopValue);
        tvTotalCalories = findViewById(R.id.tvTotalCalories);
        tvStatus = findViewById(R.id.tvStatus);
        tvAdaptiveCoach = findViewById(R.id.tvAdaptiveCoach);
        tvRecoveryScore = findViewById(R.id.tvRecoveryScore);
        tvEcoImpact = findViewById(R.id.tvEcoImpact);
        tvHydrationValue = findViewById(R.id.tvHydrationValue);
        tvBmiResult = findViewById(R.id.tvBmiResult);
        tvRingSteps = findViewById(R.id.tvRingSteps);
        tvRingMode = findViewById(R.id.tvRingMode);

        bmiWeightInput = findViewById(R.id.bmiWeightInput);
        bmiFeetInput = findViewById(R.id.bmiFeetInput);
        bmiInchesInput = findViewById(R.id.bmiInchesInput);

        progressDailyGoal = findViewById(R.id.progressDailyGoal);
        progressStepRing = findViewById(R.id.progressStepRing);
        chartBars = findViewById(R.id.chartBars);

        dayLabels = new TextView[]{
                findViewById(R.id.tvDayMon),
                findViewById(R.id.tvDayTue),
                findViewById(R.id.tvDayWed),
                findViewById(R.id.tvDayThu),
                findViewById(R.id.tvDayFri),
                findViewById(R.id.tvDaySat),
                findViewById(R.id.tvDaySun)
        };
    }

    private void loadUserState() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        calorieGoal = preferences.getInt(KEY_CALORIE_GOAL, DEFAULT_CALORIE_GOAL);
        hydrationMl = preferences.getInt(KEY_HYDRATION_ML, 0);
        selectedMood = preferences.getString(KEY_SELECTED_MOOD, "Calm");
        if (selectedMood == null) {
            selectedMood = "Calm";
        }
    }

    private void saveUserState() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        preferences.edit()
                .putInt(KEY_CALORIE_GOAL, calorieGoal)
                .putInt(KEY_HYDRATION_ML, hydrationMl)
                .putString(KEY_SELECTED_MOOD, selectedMood)
                .apply();
    }

    private void setupTopBarActions() {
        btnBack.setOnClickListener(view -> finish());
        btnCalendar.setOnClickListener(view -> openDatePicker());
        btnShare.setOnClickListener(view -> shareSummary());

        // Tapping goal title/progress lets user set a custom daily calorie goal.
        tvGoalLabel.setOnClickListener(view -> showGoalEditorDialog());
        tvGoalProgress.setOnClickListener(view -> showGoalEditorDialog());
        progressDailyGoal.setOnClickListener(view -> showGoalEditorDialog());
    }

    private void setupActionButtons() {
        btnQuickAdd.setOnClickListener(view -> quickAddActivity());
        btnQuickRemove.setOnClickListener(view -> quickRemoveActivity());
        btnStartWalk.setOnClickListener(view -> logWorkoutFromTracker(getString(R.string.walk_mode), 30));
        btnStartRun.setOnClickListener(view -> logWorkoutFromTracker(getString(R.string.run_mode), 70));
        btnFindRoute.setOnClickListener(view -> openRouteSearch());
        btnOpenFitness.setOnClickListener(view -> startActivity(new Intent(this, FitnessPlusActivity.class)));
        btnOpenSharing.setOnClickListener(view -> startActivity(new Intent(this, SharingActivity.class)));
        btnAddWater.setOnClickListener(view -> adjustHydration(250));
        btnRemoveWater.setOnClickListener(view -> adjustHydration(-250));
        btnCalculateBmiTracker.setOnClickListener(view -> calculateBmiFromInputs());

        findViewById(R.id.cardSteps).setOnClickListener(view ->
                Toast.makeText(this, "Step details are up to date.", Toast.LENGTH_SHORT).show()
        );
        findViewById(R.id.cardDistance).setOnClickListener(view -> openRouteSearch());
        findViewById(R.id.cardActive).setOnClickListener(view ->
                Toast.makeText(this, "Great active calorie burn so far.", Toast.LENGTH_SHORT).show()
        );
        findViewById(R.id.cardMode).setOnClickListener(view ->
                Toast.makeText(this, "Mode is auto-detected from live cadence.", Toast.LENGTH_SHORT).show()
        );
    }

    private void setupMoodButtons() {
        btnMoodCalm.setOnClickListener(view -> setMood("Calm"));
        btnMoodFocus.setOnClickListener(view -> setMood("Focused"));
        btnMoodStress.setOnClickListener(view -> setMood("Stressed"));
        btnMoodLow.setOnClickListener(view -> setMood("Low Energy"));
    }

    private void setMood(String mood) {
        selectedMood = mood;
        saveUserState();
        refreshMoodSelection();
        updateAdaptiveCoach(lastCadence());
    }

    private void refreshMoodSelection() {
        resetMoodButtonStyle(btnMoodCalm);
        resetMoodButtonStyle(btnMoodFocus);
        resetMoodButtonStyle(btnMoodStress);
        resetMoodButtonStyle(btnMoodLow);

        if ("Calm".equals(selectedMood)) {
            highlightMoodButton(btnMoodCalm);
        } else if ("Focused".equals(selectedMood)) {
            highlightMoodButton(btnMoodFocus);
        } else if ("Stressed".equals(selectedMood)) {
            highlightMoodButton(btnMoodStress);
        } else if ("Low Energy".equals(selectedMood)) {
            highlightMoodButton(btnMoodLow);
        }
    }

    private void resetMoodButtonStyle(MaterialButton button) {
        button.setAlpha(0.85f);
        button.setStrokeWidth(0);
    }

    private void highlightMoodButton(MaterialButton button) {
        button.setAlpha(1f);
        button.setStrokeWidth(dp(1));
        button.setStrokeColor(ColorStateList.valueOf(getColor(R.color.tracker_accent_alt)));
    }

    private void setupDaySelectors() {
        for (int i = 0; i < dayLabels.length; i++) {
            final int dayIndex = i;
            dayLabels[i].setOnClickListener(view -> {
                selectedDateMillis = dateForDayIndexThisWeek(dayIndex);
                setupDateTitle();
                refreshDaySelection();
                int progress = progressForDay(dayIndex);
                String msg = getString(R.string.day_progress_toast, dayNames[dayIndex], progress);
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            });
        }
        refreshDaySelection();
    }

    private long dateForDayIndexThisWeek(int targetDayIndex) {
        Calendar calendar = Calendar.getInstance();
        int currentDayIndex = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        calendar.add(Calendar.DAY_OF_YEAR, targetDayIndex - currentDayIndex);
        return calendar.getTimeInMillis();
    }

    private void refreshDaySelection() {
        int selectedDay = dayIndexFromMillis(selectedDateMillis);
        for (int i = 0; i < dayLabels.length; i++) {
            if (i == selectedDay) {
                dayLabels[i].setBackgroundResource(R.drawable.bg_tracker_day_selected);
                dayLabels[i].setTextColor(getColor(android.R.color.white));
            } else {
                dayLabels[i].setBackgroundResource(R.drawable.bg_tracker_day_unselected);
                dayLabels[i].setTextColor(getColor(R.color.tracker_secondary_text));
            }
        }
    }

    private void setupDateTitle() {
        Date selectedDate = new Date(selectedDateMillis);
        String datePart = new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(selectedDate);
        if (isSameDay(selectedDateMillis, System.currentTimeMillis())) {
            tvDateTitle.setText(getString(R.string.today_date) + ", " + datePart);
        } else {
            String fullDate = new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(selectedDate);
            tvDateTitle.setText(fullDate);
        }
    }

    private void openDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDateMillis);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth, 12, 0, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    selectedDateMillis = selected.getTimeInMillis();
                    setupDateTitle();
                    refreshDaySelection();

                    String shortDate = new SimpleDateFormat("d MMM", Locale.getDefault()).format(selected.getTime());
                    Toast.makeText(this, getString(R.string.date_selected_message, shortDate), Toast.LENGTH_SHORT).show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void showGoalEditorDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint(getString(R.string.set_goal_hint));
        input.setText(String.valueOf(calorieGoal));

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.set_goal_title))
                .setView(input)
                .setPositiveButton("Save", (dialogInterface, i) -> {
                    String value = input.getText().toString().trim();
                    try {
                        int newGoal = Integer.parseInt(value);
                        if (newGoal < 100 || newGoal > 3000) {
                            Toast.makeText(this, "Goal should be between 100 and 3000.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        calorieGoal = newGoal;
                        saveUserState();
                        Toast.makeText(this, getString(R.string.goal_updated), Toast.LENGTH_SHORT).show();
                        updateMetrics();
                    } catch (NumberFormatException ignored) {
                        Toast.makeText(this, "Please enter a valid number.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void buildChartBars() {
        chartBars.removeAllViews();
        barViews.clear();

        for (int hour = 0; hour < 24; hour++) {
            View bar = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(5), 1f);
            params.setMargins(dp(1), 0, dp(1), 0);
            bar.setLayoutParams(params);
            bar.setBackgroundColor(getColor(R.color.tracker_accent_alt));
            bar.setAlpha(0.35f);
            chartBars.addView(bar);
            barViews.add(bar);
        }
    }

    private void updateChartBars() {
        float maxValue = 1f;
        for (float value : hourlyCalories) {
            if (value > maxValue) {
                maxValue = value;
            }
        }

        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        for (int i = 0; i < barViews.size(); i++) {
            float normalized = hourlyCalories[i] / maxValue;
            int targetHeight = dp(5) + Math.round(normalized * dp(70));

            View bar = barViews.get(i);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) bar.getLayoutParams();
            params.height = targetHeight;
            bar.setLayoutParams(params);
            bar.setBackgroundColor(getColor(R.color.tracker_accent_alt));
            bar.setAlpha(i == currentHour ? 1f : 0.35f);
        }

        int roundedMax = Math.max(1, Math.round(maxValue));
        tvChartTopValue.setText(getString(R.string.energy_peak_fmt, roundedMax));
    }

    private void ensurePermissionThenStartTracking() {
        if (!hasActivityPermission()) {
            tvStatus.setText(getString(R.string.step_permission_required));
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                    REQUEST_ACTIVITY_RECOGNITION
            );
            return;
        }

        startTrackingService();
        requestTrackingRefresh();
        requestNotificationPermissionIfNeeded();
        tvStatus.setText(getString(R.string.tracking_active));
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

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQUEST_NOTIFICATIONS
        );
    }

    private void startTrackingService() {
        Intent serviceIntent = new Intent(this, StepTrackingService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void requestTrackingRefresh() {
        if (!hasActivityPermission()) {
            return;
        }
        Intent refreshIntent = new Intent(this, StepTrackingService.class);
        refreshIntent.setAction(StepTrackingService.ACTION_REFRESH);
        ContextCompat.startForegroundService(this, refreshIntent);
    }

    private void registerLiveStepReceiver() {
        if (stepReceiverRegistered) {
            return;
        }
        IntentFilter intentFilter = new IntentFilter(StepTrackingService.ACTION_STEP_UPDATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(liveStepReceiver, intentFilter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(liveStepReceiver, intentFilter);
        }
        stepReceiverRegistered = true;
    }

    private void unregisterLiveStepReceiver() {
        if (!stepReceiverRegistered) {
            return;
        }
        unregisterReceiver(liveStepReceiver);
        stepReceiverRegistered = false;
    }

    private void loadLiveTrackingSnapshot() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        manualExtraSteps = preferences.getInt(StepTrackingService.KEY_MANUAL_EXTRA_STEPS, manualExtraSteps);
        sessionSteps = preferences.getInt(StepTrackingService.KEY_STEPS, sessionSteps);
        sensorSteps = Math.max(0, sessionSteps - manualExtraSteps);
        lastSessionSteps = sessionSteps;

        String storedMode = preferences.getString(StepTrackingService.KEY_MODE, getString(R.string.idle_mode));
        if (storedMode != null) {
            liveMode = storedMode;
        }
        liveCadence = preferences.getFloat(StepTrackingService.KEY_CADENCE, 0f);
        sessionStartTimeMillis = preferences.getLong(
                StepTrackingService.KEY_SESSION_START,
                sessionStartTimeMillis
        );

        boolean sensorReady = preferences.getBoolean(StepTrackingService.KEY_SENSOR_READY, true);
        if (!sensorReady) {
            tvStatus.setText(getString(R.string.step_sensor_missing));
        }
    }

    private void applyLiveStepUpdate(int updatedSteps) {
        int safeSteps = Math.max(0, updatedSteps);
        if (lastSessionSteps >= 0 && safeSteps > lastSessionSteps) {
            int deltaSteps = safeSteps - lastSessionSteps;
            int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            hourlyCalories[currentHour] += deltaSteps * CALORIES_PER_STEP;
        }
        sessionSteps = safeSteps;
        lastSessionSteps = safeSteps;
        sensorSteps = Math.max(0, sessionSteps - manualExtraSteps);
    }

    private void logWorkoutFromTracker(String mode, int calories) {
        int addedSteps = FitnessState.logWorkout(this, mode, calories);
        loadLiveTrackingSnapshot();
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        hourlyCalories[currentHour] += calories;
        Toast.makeText(this, getString(R.string.workout_status_started, mode, calories, addedSteps), Toast.LENGTH_SHORT).show();
        updateMetrics();
        requestTrackingRefresh();
    }

    private void quickAddActivity() {
        loadLiveTrackingSnapshot();
        int addedCalories = 30;
        int addedSteps = Math.round(addedCalories / CALORIES_PER_STEP);
        manualExtraSteps += addedSteps;
        sessionSteps = sensorSteps + manualExtraSteps;
        lastSessionSteps = sessionSteps;
        saveManualStepAdjustments();
        FitnessState.markActiveToday(this);

        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        hourlyCalories[currentHour] += addedCalories;
        Toast.makeText(this, getString(R.string.quick_add_logged), Toast.LENGTH_SHORT).show();
        updateMetrics();
        requestTrackingRefresh();
    }

    private void quickRemoveActivity() {
        loadLiveTrackingSnapshot();
        int removeCalories = 30;
        int removeSteps = Math.round(removeCalories / CALORIES_PER_STEP);

        if (manualExtraSteps <= 0) {
            Toast.makeText(this, getString(R.string.quick_remove_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        int removedSteps = Math.min(removeSteps, manualExtraSteps);
        manualExtraSteps -= removedSteps;
        sessionSteps = sensorSteps + manualExtraSteps;
        lastSessionSteps = sessionSteps;
        saveManualStepAdjustments();
        FitnessState.markActiveToday(this);

        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        float removedCalories = removedSteps * CALORIES_PER_STEP;
        hourlyCalories[currentHour] = Math.max(0f, hourlyCalories[currentHour] - removedCalories);
        Toast.makeText(
                this,
                getString(R.string.quick_remove_logged, Math.round(removedCalories)),
                Toast.LENGTH_SHORT
        ).show();
        updateMetrics();
        requestTrackingRefresh();
    }

    private void saveManualStepAdjustments() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putInt(StepTrackingService.KEY_MANUAL_EXTRA_STEPS, manualExtraSteps)
                .putInt(StepTrackingService.KEY_STEPS, sessionSteps)
                .apply();
    }

    private void adjustHydration(int deltaMl) {
        if (deltaMl < 0 && hydrationMl <= 0) {
            Toast.makeText(this, getString(R.string.hydration_minimum), Toast.LENGTH_SHORT).show();
            return;
        }

        int previousHydration = hydrationMl;
        hydrationMl = FitnessState.addHydration(this, deltaMl);
        if (hydrationMl == previousHydration) {
            Toast.makeText(this, getString(R.string.hydration_minimum), Toast.LENGTH_SHORT).show();
            return;
        }

        if (deltaMl > 0) {
            Toast.makeText(this, getString(R.string.hydration_added), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.hydration_removed), Toast.LENGTH_SHORT).show();
        }
        updateMetrics();
    }

    private void updateMetrics() {
        float distanceKm = sessionSteps * KM_PER_STEP;
        int activeCalories = Math.round(sessionSteps * CALORIES_PER_STEP);
        int totalCalories = BASE_TOTAL_CALORIES + activeCalories;

        float elapsedMinutes = Math.max(1f, (System.currentTimeMillis() - sessionStartTimeMillis) / 60000f);
        float cadence = liveCadence > 0f ? liveCadence : (sessionSteps / elapsedMinutes);
        rememberCadenceSample(cadence);

        String modeText = liveMode;
        if (modeText == null || modeText.trim().isEmpty()) {
            if (cadence >= 130f) {
                modeText = getString(R.string.run_mode);
            } else if (cadence >= 45f) {
                modeText = getString(R.string.walk_mode);
            } else {
                modeText = getString(R.string.idle_mode);
            }
        }

        currentGoalProgress = Math.min(100, Math.round((activeCalories * 100f) / calorieGoal));
        int recoveryScore = calculateRecoveryFlux(cadence);
        float ecoImpact = distanceKm * 120f;

        tvMoveValue.setText(getString(R.string.energy_value, activeCalories));
        tvGoalProgress.setText(getString(R.string.goal_progress_value, currentGoalProgress));
        progressDailyGoal.setProgress(currentGoalProgress);
        tvStepCount.setText(String.format(Locale.getDefault(), "%,d", sessionSteps));
        tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", distanceKm));
        tvActiveCalories.setText(String.valueOf(activeCalories));
        tvMode.setText(modeText);
        tvRingSteps.setText(String.format(Locale.getDefault(), "%,d", sessionSteps));
        tvRingMode.setText(modeText);
        progressStepRing.setMax(STEP_RING_GOAL);
        progressStepRing.setProgress(Math.min(STEP_RING_GOAL, sessionSteps));
        tvTotalCalories.setText(getString(R.string.today_total_calories, totalCalories));
        tvRecoveryScore.setText(getString(R.string.recovery_score_fmt, recoveryScore));
        tvEcoImpact.setText(getString(R.string.eco_impact_fmt, ecoImpact));
        tvHydrationValue.setText(getString(R.string.hydration_fmt, hydrationMl));

        updateAdaptiveCoach(cadence);
        updateChartBars();
    }

    private void rememberCadenceSample(float cadence) {
        cadenceSamples.add(cadence);
        if (cadenceSamples.size() > 20) {
            cadenceSamples.remove(0);
        }
    }

    private void calculateBmiFromInputs() {
        String weightText = textFromInput(bmiWeightInput);
        String feetText = textFromInput(bmiFeetInput);
        String inchesText = textFromInput(bmiInchesInput);

        if (weightText.isEmpty() || feetText.isEmpty() || inchesText.isEmpty()) {
            Toast.makeText(this, getString(R.string.tracker_bmi_missing_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        double weightKg;
        int feet;
        int inches;
        try {
            weightKg = Double.parseDouble(weightText);
            feet = Integer.parseInt(feetText);
            inches = Integer.parseInt(inchesText);
        } catch (NumberFormatException exception) {
            Toast.makeText(this, getString(R.string.tracker_bmi_invalid_number), Toast.LENGTH_SHORT).show();
            return;
        }

        if (weightKg <= 0) {
            Toast.makeText(this, getString(R.string.tracker_bmi_invalid_weight), Toast.LENGTH_SHORT).show();
            return;
        }

        if (feet < 0 || inches < 0 || inches > 11) {
            Toast.makeText(this, getString(R.string.tracker_bmi_invalid_height), Toast.LENGTH_SHORT).show();
            return;
        }

        int totalInches = (feet * 12) + inches;
        if (totalInches <= 0) {
            Toast.makeText(this, getString(R.string.tracker_bmi_invalid_height), Toast.LENGTH_SHORT).show();
            return;
        }

        double heightMeters = totalInches * 0.0254d;
        double bmi = weightKg / (heightMeters * heightMeters);

        String category;
        if (bmi < 18.5d) {
            category = getString(R.string.tracker_bmi_underweight);
        } else if (bmi < 25d) {
            category = getString(R.string.tracker_bmi_healthy);
        } else if (bmi < 30d) {
            category = getString(R.string.tracker_bmi_overweight);
        } else {
            category = getString(R.string.tracker_bmi_obese);
        }

        tvBmiResult.setText(getString(R.string.tracker_bmi_result_value, bmi, category));
        FitnessState.saveBmi(this, bmi, category);
    }

    private String textFromInput(TextInputEditText inputEditText) {
        if (inputEditText == null || inputEditText.getText() == null) {
            return "";
        }
        return inputEditText.getText().toString().trim();
    }

    private int calculateRecoveryFlux(float currentCadence) {
        if (cadenceSamples.isEmpty()) {
            return 70;
        }
        float mean = 0f;
        for (float value : cadenceSamples) {
            mean += value;
        }
        mean /= cadenceSamples.size();

        float variance = 0f;
        for (float value : cadenceSamples) {
            float diff = value - mean;
            variance += diff * diff;
        }
        variance /= cadenceSamples.size();
        float stdDev = (float) Math.sqrt(variance);

        float score = 90f - (stdDev * 0.9f);
        if (currentCadence > 145f) {
            score -= (currentCadence - 145f) * 0.2f;
        }
        score += Math.min(20f, hydrationMl / 100f);

        int rounded = Math.round(score);
        if (rounded < 20) {
            return 20;
        }
        return Math.min(99, rounded);
    }

    private void updateAdaptiveCoach(float cadence) {
        String suggestion;
        if ("Stressed".equals(selectedMood)) {
            suggestion = "Try 8-min recovery walk at 90-100 steps/min with nasal breathing.";
        } else if ("Low Energy".equals(selectedMood)) {
            suggestion = "Start with 5-min brisk walk, then 2 x 60 sec light jog.";
        } else if ("Focused".equals(selectedMood)) {
            if (cadence > 120f) {
                suggestion = "Perfect focus window. Do a 12-min tempo run now.";
            } else {
                suggestion = "Build momentum: 3 rounds of 2-min fast walk + 1-min jog.";
            }
        } else {
            suggestion = "Balanced state. Aim for a smooth 20-min walk-run mix today.";
        }
        tvAdaptiveCoach.setText(suggestion);
    }

    private float lastCadence() {
        if (cadenceSamples.isEmpty()) {
            return 0f;
        }
        return cadenceSamples.get(cadenceSamples.size() - 1);
    }

    private int progressForDay(int dayIndex) {
        int selectedDay = dayIndexFromMillis(selectedDateMillis);
        if (dayIndex == selectedDay) {
            return currentGoalProgress;
        }
        return baseWeekProgress[dayIndex];
    }

    private void shareSummary() {
        String text = "FitNest Summary\n"
                + "Date: " + tvDateTitle.getText() + "\n"
                + "Energy: " + tvMoveValue.getText() + "\n"
                + "Steps: " + tvStepCount.getText() + "\n"
                + "Distance: " + tvDistance.getText() + "\n"
                + "Recovery Flux: " + tvRecoveryScore.getText() + "\n"
                + "Eco Impact: " + tvEcoImpact.getText() + "\n"
                + "Coach: " + tvAdaptiveCoach.getText();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_action)));
    }

    private void openRouteSearch() {
        Intent mapsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=running track near me"));
        try {
            startActivity(mapsIntent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, getString(R.string.no_compatible_app), Toast.LENGTH_SHORT).show();
        }
    }

    private int dayIndexFromMillis(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return (dayOfWeek + 5) % 7;
    }

    private boolean isSameDay(long first, long second) {
        Calendar firstCal = Calendar.getInstance();
        firstCal.setTimeInMillis(first);
        Calendar secondCal = Calendar.getInstance();
        secondCal.setTimeInMillis(second);
        return firstCal.get(Calendar.YEAR) == secondCal.get(Calendar.YEAR)
                && firstCal.get(Calendar.DAY_OF_YEAR) == secondCal.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ACTIVITY_RECOGNITION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTrackingService();
                requestTrackingRefresh();
                tvStatus.setText(getString(R.string.tracking_active));
            } else {
                tvStatus.setText(getString(R.string.step_permission_required));
            }
            return;
        }

        if (requestCode == REQUEST_NOTIFICATIONS) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.notification_permission_note), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
