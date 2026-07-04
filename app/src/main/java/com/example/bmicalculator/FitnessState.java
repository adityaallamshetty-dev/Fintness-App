package com.example.bmicalculator;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class FitnessState {

    public static final float CALORIES_PER_STEP = 0.04f;
    public static final float KM_PER_STEP = 0.00078f;
    public static final int DEFAULT_CALORIE_GOAL = 410;

    public static final String KEY_CALORIE_GOAL = "calorie_goal";
    public static final String KEY_HYDRATION_ML = "hydration_ml";
    public static final String KEY_SELECTED_MOOD = "selected_mood";
    public static final String KEY_FITNESS_PLUS_ACTIVE = "fitness_plus_active";
    public static final String KEY_FITNESS_PLUS_PLAN = "fitness_plus_plan";
    public static final String KEY_LAST_BMI = "last_bmi";
    public static final String KEY_LAST_BMI_CATEGORY = "last_bmi_category";
    public static final String KEY_LAST_WORKOUT = "last_workout";
    public static final String KEY_LAST_WORKOUT_CALORIES = "last_workout_calories";
    public static final String KEY_LAST_ACTIVE_DATE = "last_active_date";
    public static final String KEY_STREAK_DAYS = "streak_days";

    private FitnessState() {
    }

    public static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(StepTrackingService.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static int activeCalories(int steps) {
        return Math.round(steps * CALORIES_PER_STEP);
    }

    public static float distanceKm(int steps) {
        return steps * KM_PER_STEP;
    }

    public static int goalProgress(int activeCalories, int calorieGoal) {
        return Math.min(100, Math.round((activeCalories * 100f) / Math.max(100, calorieGoal)));
    }

    public static int addHydration(Context context, int amountMl) {
        SharedPreferences preferences = preferences(context);
        int updated = Math.max(0, preferences.getInt(KEY_HYDRATION_ML, 0) + amountMl);
        preferences.edit()
                .putInt(KEY_HYDRATION_ML, updated)
                .apply();
        markActiveToday(context);
        return updated;
    }

    public static int logWorkout(Context context, String mode, int calories) {
        SharedPreferences preferences = preferences(context);
        int addedSteps = Math.round(calories / CALORIES_PER_STEP);
        int manualSteps = preferences.getInt(StepTrackingService.KEY_MANUAL_EXTRA_STEPS, 0) + addedSteps;
        int totalSteps = preferences.getInt(StepTrackingService.KEY_STEPS, 0) + addedSteps;

        preferences.edit()
                .putInt(StepTrackingService.KEY_MANUAL_EXTRA_STEPS, manualSteps)
                .putInt(StepTrackingService.KEY_STEPS, totalSteps)
                .putString(StepTrackingService.KEY_MODE, mode)
                .putFloat(StepTrackingService.KEY_CADENCE, cadenceForMode(context, mode))
                .putLong(StepTrackingService.KEY_SESSION_START, System.currentTimeMillis())
                .putBoolean(StepTrackingService.KEY_SENSOR_READY, true)
                .putString(KEY_LAST_WORKOUT, mode)
                .putInt(KEY_LAST_WORKOUT_CALORIES, calories)
                .apply();
        markActiveToday(context);
        return addedSteps;
    }

    public static void saveBmi(Context context, double bmi, String category) {
        preferences(context).edit()
                .putFloat(KEY_LAST_BMI, (float) bmi)
                .putString(KEY_LAST_BMI_CATEGORY, category)
                .apply();
        markActiveToday(context);
    }

    public static void activatePlusPlan(Context context, String planName) {
        preferences(context).edit()
                .putBoolean(KEY_FITNESS_PLUS_ACTIVE, true)
                .putString(KEY_FITNESS_PLUS_PLAN, planName)
                .putInt(KEY_CALORIE_GOAL, 520)
                .apply();
        markActiveToday(context);
    }

    public static void markActiveToday(Context context) {
        SharedPreferences preferences = preferences(context);
        String today = todayKey();
        String lastActiveDate = preferences.getString(KEY_LAST_ACTIVE_DATE, "");
        if (today.equals(lastActiveDate)) {
            return;
        }

        int currentStreak = preferences.getInt(KEY_STREAK_DAYS, 0);
        int nextStreak = isYesterday(lastActiveDate) ? currentStreak + 1 : 1;
        preferences.edit()
                .putString(KEY_LAST_ACTIVE_DATE, today)
                .putInt(KEY_STREAK_DAYS, nextStreak)
                .apply();
    }

    private static String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private static boolean isYesterday(String dateKey) {
        if (dateKey == null || dateKey.isEmpty()) {
            return false;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            Date lastDate = format.parse(dateKey);
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DAY_OF_YEAR, -1);
            return lastDate != null && format.format(yesterday.getTime()).equals(format.format(lastDate));
        } catch (ParseException exception) {
            return false;
        }
    }

    private static float cadenceForMode(Context context, String mode) {
        if (context.getString(R.string.run_mode).equals(mode)) {
            return 145f;
        }
        if (context.getString(R.string.walk_mode).equals(mode)) {
            return 95f;
        }
        return 0f;
    }
}
