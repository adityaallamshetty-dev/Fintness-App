package com.example.bmicalculator;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class MainActivity2 extends AppCompatActivity {

    private static final String DOCTOR_PHONE = "9686729490";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);

        MaterialButton btnCall = findViewById(R.id.btncall);
        btnCall.setOnClickListener(view -> {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:" + DOCTOR_PHONE));

            try {
                startActivity(dialIntent);
            } catch (ActivityNotFoundException exception) {
                Toast.makeText(this, "No phone app found on this device.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
