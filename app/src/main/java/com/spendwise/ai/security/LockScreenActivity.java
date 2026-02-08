package com.spendwise.ai.security;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.spendwise.ai.MainActivity;
import com.spendwise.ai.R;

/**
 * Lock screen activity shown when biometric authentication is required.
 */
public class LockScreenActivity extends AppCompatActivity implements BiometricHelper.AuthCallback {

    private Button unlockButton;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_screen);

        unlockButton = findViewById(R.id.btn_unlock);
        statusText = findViewById(R.id.text_status);

        unlockButton.setOnClickListener(v -> authenticate());

        // Auto-trigger authentication on start
        if (BiometricHelper.isBiometricAvailable(this)) {
            authenticate();
        } else {
            statusText.setText("Biometric authentication not available");
            unlockButton.setVisibility(View.GONE);
        }
    }

    private void authenticate() {
        statusText.setText("");
        BiometricHelper.authenticate(this, this);
    }

    @Override
    public void onSuccess() {
        // Go to main activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onError(String message) {
        statusText.setText(message);
        // Check if it's a cancel - user can retry
        unlockButton.setText("Try Again");
    }

    @Override
    public void onFailure() {
        statusText.setText("Authentication failed. Try again.");
    }

    @Override
    public void onBackPressed() {
        // Prevent back navigation - must authenticate
        finishAffinity();
    }
}
