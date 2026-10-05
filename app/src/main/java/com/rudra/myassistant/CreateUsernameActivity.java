package com.rudra.myassistant;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.UserSessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateUsernameActivity extends BaseEdgeToEdgeActivity {

    private TextInputLayout usernameInputLayout;
    private TextInputEditText usernameEditText;
    private TextView tvStatus;
    private MaterialButton btnContinue;
    private ProgressBar progressBar;

    private UserSessionManager sessionManager;
    private FirestoreRepository firestoreRepo;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable checkRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_username);

        sessionManager = new UserSessionManager(this);
        firestoreRepo = new FirestoreRepository();

        initViews();
        setupListeners();
    }

    private void initViews() {
        usernameInputLayout = findViewById(R.id.usernameInputLayout);
        usernameEditText = findViewById(R.id.usernameEditText);
        tvStatus = findViewById(R.id.tvStatus);
        btnContinue = findViewById(R.id.btnContinue);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (checkRunnable != null) handler.removeCallbacks(checkRunnable);
                validateLocal(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnContinue.setOnClickListener(v -> saveUsername());
    }

    private void validateLocal(String username) {
        btnContinue.setEnabled(false);
        tvStatus.setVisibility(View.GONE);
        
        if (username.isEmpty()) {
            usernameInputLayout.setHelperText("Username is required");
            return;
        }

        if (username.length() < 3) {
            usernameInputLayout.setError("Minimum 3 characters");
            return;
        }

        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            usernameInputLayout.setError("Only letters, numbers, and underscores allowed");
            return;
        }

        usernameInputLayout.setError(null);
        usernameInputLayout.setHelperText("Checking availability...");
        
        checkRunnable = () -> checkAvailability(username);
        handler.postDelayed(checkRunnable, 500);
    }

    private void checkAvailability(String username) {
        progressBar.setVisibility(View.VISIBLE);
        firestoreRepo.isUsernameAvailable(username).addOnSuccessListener(queryDocumentSnapshots -> {
            progressBar.setVisibility(View.GONE);
            tvStatus.setVisibility(View.VISIBLE);
            if (queryDocumentSnapshots.isEmpty()) {
                tvStatus.setText("Username is available!");
                tvStatus.setTextColor(Color.GREEN);
                btnContinue.setEnabled(true);
                usernameInputLayout.setHelperText("Available!");
                usernameInputLayout.setError(null);
            } else {
                tvStatus.setText("Username is already taken.");
                tvStatus.setTextColor(Color.RED);
                btnContinue.setEnabled(false);
                usernameInputLayout.setError("Already taken");
            }
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(CreateUsernameActivity.this, "Error checking availability.", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveUsername() {
        String username = usernameEditText.getText().toString().trim();
        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) {
            Toast.makeText(this, "Session error. Log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnContinue.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        firestoreRepo.setUsername(uid, username).addOnSuccessListener(aVoid -> {
            progressBar.setVisibility(View.GONE);
            sessionManager.setUsername(username);
            Toast.makeText(this, "Username set successfully!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(CreateUsernameActivity.this, MainActivity.class));
            finish();
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            btnContinue.setEnabled(true);
            Toast.makeText(this, "Failed to set username: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
