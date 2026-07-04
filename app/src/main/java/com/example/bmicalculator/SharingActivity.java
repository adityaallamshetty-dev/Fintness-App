package com.example.bmicalculator;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class SharingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sharing);

        ImageButton btnBack = findViewById(R.id.btnBackSharing);
        MaterialButton btnShareProgress = findViewById(R.id.btnShareProgress);
        MaterialButton btnInviteSms = findViewById(R.id.btnInviteSms);
        MaterialButton btnCopyStats = findViewById(R.id.btnCopyStats);

        btnBack.setOnClickListener(view -> finish());
        btnShareProgress.setOnClickListener(view -> shareSummary());
        btnInviteSms.setOnClickListener(view -> inviteBySms());
        btnCopyStats.setOnClickListener(view -> copyStats());
    }

    private void shareSummary() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, buildSummaryText());
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_action)));
    }

    private void inviteBySms() {
        Intent smsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:"));
        smsIntent.putExtra("sms_body", getString(R.string.sms_invite_body));
        try {
            startActivity(smsIntent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, getString(R.string.no_compatible_app), Toast.LENGTH_SHORT).show();
        }
    }

    private void copyStats() {
        ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager != null) {
            manager.setPrimaryClip(ClipData.newPlainText("fitness_stats", buildSummaryText()));
            Toast.makeText(this, getString(R.string.stats_copied), Toast.LENGTH_SHORT).show();
        }
    }

    private String buildSummaryText() {
        SharedPreferences preferences = FitnessState.preferences(this);
        int steps = preferences.getInt(StepTrackingService.KEY_STEPS, 0);
        int hydrationMl = preferences.getInt(FitnessState.KEY_HYDRATION_ML, 0);
        int activeCalories = FitnessState.activeCalories(steps);
        float distanceKm = FitnessState.distanceKm(steps);
        String mode = preferences.getString(StepTrackingService.KEY_MODE, getString(R.string.idle_mode));
        if (mode == null || mode.trim().isEmpty()) {
            mode = getString(R.string.idle_mode);
        }

        return getString(
                R.string.share_summary_template,
                steps,
                distanceKm,
                activeCalories,
                hydrationMl,
                mode
        );
    }
}
