package com.example.bmicalculator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final float CALORIES_PER_STEP = 0.04f;

    private TextView textViewResult;
    private TextView tvHomeSteps;
    private TextView tvHomeEnergy;
    private TextInputEditText editTextWeight;
    private TextInputEditText editTextHeightFt;
    private TextInputEditText editTextHeightIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(true);
            insetsController.setAppearanceLightNavigationBars(true);
        }

        MaterialButton switchToNext = findViewById(R.id.switchToNext);
        MaterialButton openStepTracker = findViewById(R.id.openStepTracker);
        MaterialButton openWorkout = findViewById(R.id.openWorkout);
        MaterialButton calculateButton = findViewById(R.id.calculateButton);

        textViewResult = findViewById(R.id.textViewResult);
        tvHomeSteps = findViewById(R.id.tvHomeSteps);
        tvHomeEnergy = findViewById(R.id.tvHomeEnergy);
        editTextWeight = findViewById(R.id.editTextweight);
        editTextHeightFt = findViewById(R.id.ediTextheight);
        editTextHeightIn = findViewById(R.id.edittexthightIn);
        switchToNext.setOnClickListener(view -> openActivity(MainActivity2.class));
        openStepTracker.setOnClickListener(view -> openActivity(StepTrackerActivity.class));
        openWorkout.setOnClickListener(view -> openActivity(WorkoutActivity.class));
        calculateButton.setOnClickListener(view -> calculateAndShowBmi());

        findViewById(R.id.openTrackerCard).setOnClickListener(view -> openActivity(StepTrackerActivity.class));
        findViewById(R.id.openWorkoutCard).setOnClickListener(view -> openActivity(WorkoutActivity.class));
        findViewById(R.id.openFitnessPlusCard).setOnClickListener(view -> openActivity(FitnessPlusActivity.class));
        findViewById(R.id.openSharingCard).setOnClickListener(view -> openActivity(SharingActivity.class));

        updateHomeStats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHomeStats();
    }

    private void openActivity(Class<?> destination) {
        startActivity(new Intent(this, destination));
    }

    private void updateHomeStats() {
        SharedPreferences preferences = getSharedPreferences(StepTrackingService.PREFS_NAME, MODE_PRIVATE);
        int steps = preferences.getInt(StepTrackingService.KEY_STEPS, 0);
        int activeCalories = Math.round(steps * CALORIES_PER_STEP);

        tvHomeSteps.setText(String.format(Locale.getDefault(), "%,d", steps));
        tvHomeEnergy.setText(getString(R.string.energy_value, activeCalories));
    }

    private void calculateAndShowBmi() {
        String wtInput = readInput(editTextWeight);
        String ftInput = readInput(editTextHeightFt);
        String inInput = readInput(editTextHeightIn);

        if (wtInput.isEmpty() || ftInput.isEmpty() || inInput.isEmpty()) {
            Toast.makeText(this, "Please enter weight, feet, and inches.", Toast.LENGTH_SHORT).show();
            return;
        }

        double weightKg;
        int feet;
        int inches;
        try {
            weightKg = Double.parseDouble(wtInput);
            feet = Integer.parseInt(ftInput);
            inches = Integer.parseInt(inInput);
        } catch (NumberFormatException exception) {
            Toast.makeText(this, "Please enter valid numeric values.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (weightKg <= 0 || feet < 0 || inches < 0 || inches > 11 || (feet == 0 && inches == 0)) {
            Toast.makeText(this, "Check the values. Inches must be between 0 and 11.", Toast.LENGTH_SHORT).show();
            return;
        }

        int totalInches = (feet * 12) + inches;
        double totalMeters = (totalInches * 2.54) / 100.0;
        double bmi = weightKg / (totalMeters * totalMeters);

        if (bmi < 18.5d) {
            applyResult(bmi, getString(R.string.bmi_underweight), getString(R.string.bmi_under_message, bmi),
                    R.color.coloreow, R.color.bmi_under_text);
        } else if (bmi < 25d) {
            applyResult(bmi, getString(R.string.bmi_healthy), getString(R.string.bmi_healthy_message, bmi),
                    R.color.coloreh, R.color.bmi_healthy_text);
        } else if (bmi < 30d) {
            applyResult(bmi, getString(R.string.bmi_overweight), getString(R.string.bmi_over_message, bmi),
                    R.color.coloreuw, R.color.bmi_over_text);
        } else {
            applyResult(bmi, getString(R.string.bmi_obese), getString(R.string.bmi_obese_message, bmi),
                    R.color.coloreuw, R.color.bmi_over_text);

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setIcon(R.drawable.ic_fitness_logo)
                    .setTitle("Health Tip")
                    .setMessage(getString(R.string.bmi_obese_message, bmi));
            builder.create().show();
        }
    }

    private String readInput(TextInputEditText input) {
        if (input.getText() == null) {
            return "";
        }
        return input.getText().toString().trim();
    }

    private void applyResult(double bmi, String category, String message, int backgroundColor, int textColor) {
        textViewResult.setText(getString(R.string.bmi_result_short, bmi, category));
        textViewResult.setBackgroundTintList(ColorStateList.valueOf(getColor(backgroundColor)));
        textViewResult.setTextColor(getColor(textColor));
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
