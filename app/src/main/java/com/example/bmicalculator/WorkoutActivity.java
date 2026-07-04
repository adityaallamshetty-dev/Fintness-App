package com.example.bmicalculator;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class WorkoutActivity extends AppCompatActivity {

    private TextView tvWorkoutStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_workout);

        ImageButton btnBack = findViewById(R.id.btnBackWorkout);
        MaterialButton btnStartWalk = findViewById(R.id.btnStartWalk);
        MaterialButton btnStartRun = findViewById(R.id.btnStartRun);
        MaterialButton btnNearbyPark = findViewById(R.id.btnNearbyPark);
        tvWorkoutStatus = findViewById(R.id.tvWorkoutStatus);

        btnBack.setOnClickListener(view -> finish());
        btnStartWalk.setOnClickListener(view -> startWorkoutSession(getString(R.string.walk_mode), 30));
        btnStartRun.setOnClickListener(view -> startWorkoutSession(getString(R.string.run_mode), 70));
        btnNearbyPark.setOnClickListener(view -> openNearbyParks());
    }

    private void startWorkoutSession(String mode, int calories) {
        int addedSteps = FitnessState.logWorkout(this, mode, calories);
        tvWorkoutStatus.setText(getString(R.string.workout_status_started, mode, calories, addedSteps));
        Toast.makeText(this, getString(R.string.workout_logged_to_tracker), Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, StepTrackerActivity.class));
    }

    private void openNearbyParks() {
        Intent mapsIntent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("geo:0,0?q=parks near me")
        );
        try {
            startActivity(mapsIntent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, getString(R.string.maps_unavailable), Toast.LENGTH_SHORT).show();
        }
    }
}
