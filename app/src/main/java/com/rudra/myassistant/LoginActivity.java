package com.rudra.myassistant;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.Patterns;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.messaging.FirebaseMessaging;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.UserSessionManager;

public class LoginActivity extends BaseEdgeToEdgeActivity {

    private static final String TAG = "LoginActivity";
    private ImageView logoImage;
    private TextView welcomeTitle, welcomeSubtitle, signupLink;
    private MaterialCardView loginCard;
    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailEdit, passwordEdit;
    private MaterialButton loginButton, googleButton;
    private ProgressBar loadingBar;
    
    private UserSessionManager sessionManager;
    private FirestoreRepository firestoreRepo;

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new UserSessionManager(this);
        try {
            firestoreRepo = new FirestoreRepository();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firestore: " + e.getMessage());
        }
        mAuth = FirebaseAuth.getInstance();
        
        // Redirect if already logged in (Firebase session or SessionManager)
        if (mAuth.getCurrentUser() != null || sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

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
                    } else if (result.getResultCode() == RESULT_CANCELED) {
                        Toast.makeText(this, "Sign-in cancelled", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Google Sign-in failed", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void initViews() {
        logoImage = findViewById(R.id.logoImage);
        welcomeTitle = findViewById(R.id.welcomeTitle);
        welcomeSubtitle = findViewById(R.id.welcomeSubtitle);
        loginCard = findViewById(R.id.loginCard);
        emailLayout = findViewById(R.id.emailInputLayout);
        passwordLayout = findViewById(R.id.passwordInputLayout);
        emailEdit = findViewById(R.id.emailEditText);
        passwordEdit = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        googleButton = findViewById(R.id.googleButton);
        signupLink = findViewById(R.id.signupLink);
        loadingBar = findViewById(R.id.loadingProgressBar);
    }

    private void setupAnimations() {
        Animation slideUp = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        slideUp.setDuration(800);
        welcomeTitle.startAnimation(slideUp);
        welcomeSubtitle.startAnimation(slideUp);
        loginCard.startAnimation(slideUp);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> performLogin());

        googleButton.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        signupLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            Pair<View, String> pair = new Pair<>(logoImage, "logo_shared");
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(LoginActivity.this, pair);
            startActivity(intent, options.toBundle());
        });
    }

    private void performLogin() {
        String email = emailEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString().trim();

        if (!validateInput(email, password)) return;

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (firestoreRepo != null) firestoreRepo.createUserIfNotExists(user);
                            fetchUserFromFirestoreAndFinish(user);
                        }
                    } else {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"), 
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void fetchUserFromFirestoreAndFinish(FirebaseUser user) {
        if (firestoreRepo == null) {
            saveSessionAndFinish(user.getUid(), user.getDisplayName(), user.getEmail(), 
                    user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null, null, false);
            return;
        }
        firestoreRepo.getUser(user.getUid()).addOnSuccessListener(documentSnapshot -> {
            setLoading(false);
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                String email = documentSnapshot.getString("email");
                String photoUrl = documentSnapshot.getString("profile_photo_url");
                String username = documentSnapshot.getString("username");
                saveSessionAndFinish(user.getUid(), name, email, photoUrl, username, username != null);
            } else {
                saveSessionAndFinish(user.getUid(), user.getDisplayName(), user.getEmail(), 
                    user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null, null, false);
            }
        }).addOnFailureListener(e -> {
            setLoading(false);
            Toast.makeText(this, "Failed to fetch profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account);
            }
        } catch (ApiException e) {
            Log.e(TAG, "Google sign-in error: " + e.getStatusCode());
            Toast.makeText(this, "Google sign-in error (code: " + e.getStatusCode() + ")", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
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
                        Toast.makeText(this, "Firebase Auth failed: " + 
                                (task.getException() != null ? task.getException().getMessage() : ""), 
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveSessionAndFinish(String userId, String name, String email, String photoUrl, String username, boolean usernameSet) {
        sessionManager.createLoginSession(userId, name, email, photoUrl, username);
        
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && firestoreRepo != null) {
            firestoreRepo.createUserIfNotExists(user);
        }

        Toast.makeText(this, "Logged in as " + email, Toast.LENGTH_SHORT).show();
        
        if (!usernameSet) {
            startActivity(new Intent(this, CreateUsernameActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }

    private boolean validateInput(String email, String password) {
        boolean isValid = true;
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
        return isValid;
    }

    private void setLoading(boolean isLoading) {
        loadingBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!isLoading);
        googleButton.setEnabled(!isLoading);
        emailEdit.setEnabled(!isLoading);
        passwordEdit.setEnabled(!isLoading);
        loginButton.setText(isLoading ? "" : "Login");
    }
}
