package com.rudra.myassistant;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Pair;
import android.util.Patterns;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;


import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.rudra.myassistant.network.UserSessionManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.messaging.FirebaseMessaging;
import com.rudra.myassistant.network.FirestoreRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class SignupActivity extends BaseEdgeToEdgeActivity {

    private ImageView logoImage;
    private TextView welcomeTitle, welcomeSubtitle, loginLink;
    private MaterialCardView signupCard;
    private TextInputLayout nameLayout, emailLayout, passwordLayout, confirmPasswordLayout;
    private TextInputEditText nameEdit, emailEdit, passwordEdit, confirmPasswordEdit;
    private MaterialButton registerButton, googleButton;
    private ProgressBar loadingBar;
    private UserSessionManager sessionManager;
    private FirestoreRepository firestoreRepo;
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        sessionManager = new UserSessionManager(this);
        firestoreRepo = new FirestoreRepository();
        mAuth = FirebaseAuth.getInstance();
        
        initViews();
        setupAnimations();
        setupGoogleSignIn();
        setupClickListeners();
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    }
                }
        );
    }



    private void initViews() {
        logoImage = findViewById(R.id.logoImage);
        welcomeTitle = findViewById(R.id.welcomeTitle);
        welcomeSubtitle = findViewById(R.id.welcomeSubtitle);
        signupCard = findViewById(R.id.signupCard);
        nameLayout = findViewById(R.id.nameInputLayout);
        emailLayout = findViewById(R.id.emailInputLayout);
        passwordLayout = findViewById(R.id.passwordInputLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordInputLayout);
        nameEdit = findViewById(R.id.nameEditText);
        emailEdit = findViewById(R.id.emailEditText);
        passwordEdit = findViewById(R.id.passwordEditText);
        confirmPasswordEdit = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        googleButton = findViewById(R.id.googleButton);
        loginLink = findViewById(R.id.loginLink);
        loadingBar = findViewById(R.id.loadingProgressBar);
    }

    private void setupAnimations() {
        Animation slideUp = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        slideUp.setDuration(800);
        
        welcomeTitle.startAnimation(slideUp);
        welcomeSubtitle.startAnimation(slideUp);
        signupCard.startAnimation(slideUp);
    }

    private void setupClickListeners() {
        registerButton.setOnClickListener(v -> performRegistration());

        googleButton.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        loginLink.setOnClickListener(v -> {

            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            
            // Reverse shared element transition for the logo
            Pair<View, String> pair = new Pair<>(logoImage, "logo_shared");
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(SignupActivity.this, pair);
            
            startActivity(intent, options.toBundle());
            finish();
        });
    }

    private void performRegistration() {
        String name = nameEdit.getText().toString().trim();
        String email = emailEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString().trim();
        String confirmPassword = confirmPasswordEdit.getText().toString().trim();

        if (!validateInput(name, email, password, confirmPassword)) {
            return;
        }

        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Set display name in Firebase Auth
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            user.updateProfile(profileUpdates);

                            // Provision in Firestore
                            firestoreRepo.createUserIfNotExists(user);
                            
                            // Initialize session
                            sessionManager.createLoginSession(user.getUid(), name, email, null, null);
                            
                            // Always redirect to CreateUsernameActivity for new email signups
                            setLoading(false);
                            Toast.makeText(SignupActivity.this, "Account created!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignupActivity.this, CreateUsernameActivity.class));
                            finish();
                        }
                    } else {
                        setLoading(false);
                        String error = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        Toast.makeText(SignupActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }


    private boolean validateInput(String name, String email, String password, String confirmPassword) {
        boolean isValid = true;

        if (TextUtils.isEmpty(name)) {
            nameLayout.setError("Enter your full name");
            isValid = false;
        } else {
            nameLayout.setError(null);
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Enter a valid email address");
            isValid = false;
        } else {
            emailLayout.setError(null);
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Passwords do not match");
            isValid = false;
        } else {
            confirmPasswordLayout.setError(null);
        }

        return isValid;
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                syncGoogleAccount(account);
            }
        } catch (ApiException e) {
            Toast.makeText(this, "Google sign-in failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
        }

    }

    private void syncGoogleAccount(GoogleSignInAccount acct) {
        setLoading(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            firestoreRepo.createUserIfNotExists(user);
                            fetchUserFromFirestoreAndFinish(user);
                        }
                    } else {
                        setLoading(false);
                        Toast.makeText(this, "Firebase Auth failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchUserFromFirestoreAndFinish(FirebaseUser user) {
        firestoreRepo.getUser(user.getUid()).addOnSuccessListener(documentSnapshot -> {
            setLoading(false);
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                String email = documentSnapshot.getString("email");
                String photoUrl = documentSnapshot.getString("profile_photo_url");
                String username = documentSnapshot.getString("username");
                
                sessionManager.createLoginSession(user.getUid(), name, email, photoUrl, username);
                if (username != null) {
                    startActivity(new Intent(SignupActivity.this, MainActivity.class));
                } else {
                    startActivity(new Intent(SignupActivity.this, CreateUsernameActivity.class));
                }
                finish();
            } else {
                sessionManager.createLoginSession(user.getUid(), user.getDisplayName(), user.getEmail(), 
                    user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null, null);
                startActivity(new Intent(SignupActivity.this, CreateUsernameActivity.class));
                finish();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        loadingBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!isLoading);
        googleButton.setEnabled(!isLoading);
        nameEdit.setEnabled(!isLoading);
        emailEdit.setEnabled(!isLoading);
        passwordEdit.setEnabled(!isLoading);
        confirmPasswordEdit.setEnabled(!isLoading);
        registerButton.setText(isLoading ? "" : "Create Account");
    }

    /**
     * Placeholder structure for backend/firebase auth integration
     */
    private boolean registerUser(String name, String email, String password) {
        // Integrate with Firebase or Backend JWT here
        return true; 
    }
}
