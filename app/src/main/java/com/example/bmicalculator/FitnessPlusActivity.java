package com.example.bmicalculator;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class FitnessPlusActivity extends AppCompatActivity {

    private TextView tvPlanStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fitness_plus);

        ImageButton btnBack = findViewById(R.id.btnBackFitness);
        MaterialButton btnStartTrial = findViewById(R.id.btnStartTrial);
        MaterialButton btnRestorePlan = findViewById(R.id.btnRestorePlan);
        tvPlanStatus = findViewById(R.id.tvPlanStatus);

        btnBack.setOnClickListener(view -> finish());
        btnStartTrial.setOnClickListener(view -> activatePlan(getString(R.string.plan_trial_name), R.string.trial_started));
        btnRestorePlan.setOnClickListener(view -> activatePlan(getString(R.string.plan_restored_name), R.string.plan_restored));

        refreshPlanStatus();
    }

    private void activatePlan(String planName, int toastMessage) {
        FitnessState.activatePlusPlan(this, planName);
        refreshPlanStatus();
        Toast.makeText(this, getString(toastMessage), Toast.LENGTH_SHORT).show();
    }

    private void refreshPlanStatus() {
        SharedPreferences preferences = FitnessState.preferences(this);
        boolean active = preferences.getBoolean(FitnessState.KEY_FITNESS_PLUS_ACTIVE, false);
        String planName = preferences.getString(FitnessState.KEY_FITNESS_PLUS_PLAN, getString(R.string.plan_trial_name));
        if (active) {
            tvPlanStatus.setText(getString(R.string.plan_status_active, planName));
        } else {
            tvPlanStatus.setText(getString(R.string.plan_status_free));
        }
    }
}
