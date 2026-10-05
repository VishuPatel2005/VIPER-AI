package com.rudra.myassistant;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.net.Uri;
import android.provider.OpenableColumns;
import android.database.Cursor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.RetrofitClient;
import com.rudra.myassistant.network.UserSessionManager;

public class SettingsActivity extends BaseEdgeToEdgeActivity {

    private UserSessionManager sessionManager;
    private FirestoreRepository firestoreRepo;
    private FirebaseAuth mAuth;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<Intent> uCropLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sessionManager = new UserSessionManager(this);
        firestoreRepo = new FirestoreRepository();
        mAuth = FirebaseAuth.getInstance();
        
        setupPicker();

        setupToolbar();
        setupProfile();
        setupAppearance();
        setupPreferences();
        setupChat();
        setupConnect();
        setupNotifications();
        setupAccount();
        setupLogout();
        setupServerUrl();
        
        // Initialize hardening labels immediately so they don't show "Title/Value"
        preInitializeHardeningLabels();
        fetchAndSetupHardeningSettings();
    }

    private void setupLogout() {
        View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> performLogout());
        }
    }

    private void setupToolbar() {
        ImageView closeBtn = findViewById(R.id.btnClose);
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> finish());
        }
    }

    private void setupProfile() {
        TextView nameText = findViewById(R.id.userName);
        TextView usernameText = findViewById(R.id.userUsername);
        ImageView profileImg = findViewById(R.id.profileImage);
        TextView initialsText = findViewById(R.id.profileInitials);
        View btnEditProfile = findViewById(R.id.btnEditProfile);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName() != null ? user.getDisplayName() : sessionManager.getUserName();
            nameText.setText(name);
            
            String username = sessionManager.getUsername();
            if (usernameText != null) {
                usernameText.setText(username != null ? username : "user");
                usernameText.setVisibility(View.VISIBLE);
            }
            
            if (initialsText != null && name != null && !name.isEmpty()) {
                String initials = "";
                String[] parts = name.split(" ");
                if (parts.length > 0 && parts[0].length() > 0) initials += parts[0].charAt(0);
                if (parts.length > 1 && parts[1].length() > 0) initials += parts[1].charAt(0);
                initialsText.setText(initials.toUpperCase());
            }

            String photoUrl = sessionManager.getUserPhotoUrl();
            if (photoUrl != null) {
                if (initialsText != null) initialsText.setVisibility(View.GONE);
                Glide.with(this)
                        .load(photoUrl)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                        .into(profileImg);
            } else {
                profileImg.setImageResource(R.drawable.ic_person);
                if (initialsText != null) initialsText.setVisibility(View.VISIBLE);
            }
        }
        
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        }
        profileImg.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
    }

    private void setupPicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        startCrop(uri);
                    }
                }
        );

        uCropLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        final Uri resultUri = UCrop.getOutput(result.getData());
                        if (resultUri != null) {
                            uploadImage(resultUri);
                        }
                    } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                        final Throwable cropError = UCrop.getError(result.getData());
                        Toast.makeText(this, "Crop error: " + (cropError != null ? cropError.getMessage() : "Unknown"), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void startCrop(Uri sourceUri) {
        String destinationFileName = "cropped_profile_" + System.currentTimeMillis() + ".jpg";
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), destinationFileName));

        UCrop.Options options = new UCrop.Options();
        options.setCircleDimmedLayer(true);
        options.setShowCropFrame(false);
        options.setToolbarColor(ContextCompat.getColor(this, R.color.black));
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        options.setToolbarWidgetColor(ContextCompat.getColor(this, R.color.white));
        options.setActiveControlsWidgetColor(ContextCompat.getColor(this, R.color.purple_accent));

        Intent cropIntent = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(1000, 1000)
                .withOptions(options)
                .getIntent(this);

        uCropLauncher.launch(cropIntent);
    }

    private void uploadImage(Uri uri) {
        try {
            File file;
            if ("file".equals(uri.getScheme())) {
                file = new File(uri.getPath());
            } else {
                file = getFileFromUri(uri);
            }

            String uid = mAuth.getUid();
            if (uid == null) {
                Toast.makeText(this, "User ID not found. Please log in again.", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show();

            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                    .child("profile_images/" + uid + ".jpg");

            storageRef.putFile(uri)
                    .addOnSuccessListener(taskSnapshot -> {
                        storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            String newPhotoUrl = downloadUri.toString();
                            
                            // Update Firestore
                            firestoreRepo.updateProfilePhoto(uid, newPhotoUrl).addOnSuccessListener(aVoid -> {
                                sessionManager.setPhotoUrl(newPhotoUrl);
                                setupProfile(); // Reload UI
                                Toast.makeText(SettingsActivity.this, "Profile photo updated", Toast.LENGTH_SHORT).show();
                            });
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(SettingsActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private File getFileFromUri(Uri uri) {
        try {
            String fileName = getFileName(uri);
            File file = new File(getCacheDir(), fileName);
            InputStream is = getContentResolver().openInputStream(uri);
            OutputStream os = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
            os.close();
            is.close();
            return file;
        } catch (Exception e) {
            return null;
        }
    }

    private String getFileName(Uri uri) {
        if (uri == null) return "upload_" + System.currentTimeMillis() + ".jpg";
        String result = null;
        String scheme = uri.getScheme();
        if ("content".equals(scheme)) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) result = result.substring(cut + 1);
            }
        }
        return result != null ? result : "temp_image.jpg";
    }

    private void setupAppearance() {
        View layoutTheme = findViewById(R.id.layoutTheme);
        ImageView icon = layoutTheme.findViewById(R.id.itemIcon);
        TextView title = layoutTheme.findViewById(R.id.itemTitle);
        TextView subtitle = layoutTheme.findViewById(R.id.itemSubtitle);

        icon.setImageResource(R.drawable.ic_moon);
        title.setText("Appearance");
        
        int currentTheme = sessionManager.getAppTheme();
        subtitle.setText(options[currentTheme]);
        subtitle.setVisibility(View.VISIBLE);

        layoutTheme.setOnClickListener(v -> showThemeDialog(subtitle));
    }

    private final String[] options = {"Light", "Dark", "System Default"};

    private void showThemeDialog(TextView subtitle) {
        int checkedItem = sessionManager.getAppTheme();

        new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialogTheme)
                .setTitle("Select Theme")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    subtitle.setText(options[which]);
                    sessionManager.setAppTheme(which);
                    applyTheme(which);
                    dialog.dismiss();
                })
                .show();
    }

    private void applyTheme(int theme) {
        switch (theme) {
            case 0: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
            case 1: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
            case 2: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
        }
        recreate();
    }

    private void setupPreferences() {
        View layoutPrefs = findViewById(R.id.layoutPreferences);
        ImageView icon = layoutPrefs.findViewById(R.id.itemIcon);
        TextView title = layoutPrefs.findViewById(R.id.itemTitle);
        TextView subtitle = layoutPrefs.findViewById(R.id.itemSubtitle);

        icon.setImageResource(R.drawable.ic_urgent); // Using ic_urgent since ic_star_outline is missing
        title.setText("Live Suggestions");
        subtitle.setVisibility(View.GONE); // Removed explanation per request

        layoutPrefs.setOnClickListener(v -> {
            Intent intent = new Intent(this, PreferencesActivity.class);
            startActivity(intent);
        });
    }

    private void setupChat() {
        View layoutClearChat = findViewById(R.id.layoutClearChat);
        ImageView icon = layoutClearChat.findViewById(R.id.itemIcon);
        TextView title = layoutClearChat.findViewById(R.id.itemTitle);
        TextView subtitle = layoutClearChat.findViewById(R.id.itemSubtitle);

        icon.setImageResource(R.drawable.ic_delete);
        title.setText("Clear Chat History");
        subtitle.setVisibility(View.GONE); // Removed explanation per request

        layoutClearChat.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialogTheme)
                    .setTitle("Clear History?")
                    .setMessage("This will permanently delete all your chat history from the cloud and this device. This action cannot be undone.")
                    .setPositiveButton("Clear", (dialog, which) -> {
                        String uid = mAuth.getUid();
                        if (uid != null) {
                            firestoreRepo.clearChatHistory(uid).addOnSuccessListener(aVoid -> {
                                new ChatDbHelper(this).clearAllChats();
                                Toast.makeText(this, "Chat history cleared", Toast.LENGTH_SHORT).show();
                            }).addOnFailureListener(e -> {
                                Toast.makeText(this, "Error clearing history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void setupConnect() {
        View layoutShareLocation = findViewById(R.id.layoutShareLocation);
        ImageView icon = layoutShareLocation.findViewById(R.id.itemIcon);
        TextView title = layoutShareLocation.findViewById(R.id.itemTitle);
        TextView subtitle = layoutShareLocation.findViewById(R.id.itemSubtitle);
        SwitchMaterial toggle = layoutShareLocation.findViewById(R.id.itemToggle);

        icon.setImageResource(R.drawable.ic_navigation_arrow);
        title.setText("Share Location");
        subtitle.setText("Allow friends to see you on the map");
        toggle.setChecked(sessionManager.isLocationSharingEnabled());

        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sessionManager.setLocationSharingEnabled(isChecked);
            String status = isChecked ? "enabled" : "disabled";
            Toast.makeText(this, "Location sharing " + status, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupNotifications() {
        setupToggle(R.id.layoutRemindersNotify, R.drawable.ic_reminders, "Reminders", "Get notified for upcoming tasks", com.viperai.notifications.NotificationConstants.PREF_NOTIF_REMINDERS);
        setupToggle(R.id.layoutEventsNotify, R.drawable.ic_notes, "Events", "Alerts for calendar events", com.viperai.notifications.NotificationConstants.PREF_NOTIF_EVENTS);
        setupToggle(R.id.layoutSuggestionsNotify, R.drawable.ic_search, "Suggestions", "Time-aware live suggestions", com.viperai.notifications.NotificationConstants.PREF_NOTIF_SUGGESTIONS);
        setupToggle(R.id.layoutFriendsNotify, R.drawable.ic_contacts, "Friends Nearby", "Alert when friends are close", com.viperai.notifications.NotificationConstants.PREF_NOTIF_FRIENDS);
        setupToggle(R.id.layoutExpensesNotify, R.drawable.ic_urgent, "Expenses", "Reminders for pending payments", com.viperai.notifications.NotificationConstants.PREF_NOTIF_EXPENSES);
    }

    private void setupToggle(int layoutId, int iconRes, String titleText, String subtitleText, String prefKey) {
        View layout = findViewById(layoutId);
        if (layout == null) return;
        
        ImageView icon = layout.findViewById(R.id.itemIcon);
        TextView title = layout.findViewById(R.id.itemTitle);
        TextView subtitle = layout.findViewById(R.id.itemSubtitle);
        SwitchMaterial toggle = layout.findViewById(R.id.itemToggle);

        if (icon != null) icon.setImageResource(iconRes);
        if (title != null) title.setText(titleText);
        if (subtitle != null) {
            subtitle.setText(subtitleText);
            subtitle.setVisibility(View.VISIBLE);
        }

        android.content.SharedPreferences prefs = getSharedPreferences(com.viperai.notifications.NotificationConstants.PREFS_NAME, MODE_PRIVATE);
        if (toggle != null) {
            toggle.setChecked(prefs.getBoolean(prefKey, true));
            toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(prefKey, isChecked).apply();
                if (isChecked && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    checkNotificationPermission();
                }
            });
        }
    }

    private void checkNotificationPermission() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    private void setupAccount() {
        // Setup Email Item
        View layoutEmail = findViewById(R.id.layoutUserEmail);
        if (layoutEmail != null) {
            ImageView icon = layoutEmail.findViewById(R.id.itemIcon);
            TextView title = layoutEmail.findViewById(R.id.itemTitle);
            TextView subtitle = layoutEmail.findViewById(R.id.itemSubtitle);
            ImageView chevron = layoutEmail.findViewById(R.id.itemChevron);

            if (icon != null) icon.setImageResource(R.drawable.ic_mail);
            if (title != null) title.setText("Email");
            if (subtitle != null) {
                FirebaseUser user = mAuth.getCurrentUser();
                subtitle.setText(user != null ? user.getEmail() : "");
            }
            if (chevron != null) chevron.setVisibility(View.GONE);
        }
    }

    private void fetchAndSetupHardeningSettings() {
        String userId = mAuth.getUid();
        if (userId == null) return;

        com.rudra.myassistant.network.ViperApiService api = RetrofitClient.getViperInstance().create(com.rudra.myassistant.network.ViperApiService.class);
        api.getSettings(userId).enqueue(new retrofit2.Callback<com.rudra.myassistant.network.ViperApiService.UserSettings>() {
            @Override
            public void onResponse(retrofit2.Call<com.rudra.myassistant.network.ViperApiService.UserSettings> call, retrofit2.Response<com.rudra.myassistant.network.ViperApiService.UserSettings> response) {
                if (response.isSuccessful() && response.body() != null) {
                    setupHardeningUI(response.body());
                }
            }
            @Override public void onFailure(retrofit2.Call<com.rudra.myassistant.network.ViperApiService.UserSettings> call, Throwable t) {}
        });
    }

    private void preInitializeHardeningLabels() {
        // Timezone
        View layoutTz = findViewById(R.id.layoutTimezone);
        if (layoutTz != null) {
            ((ImageView)layoutTz.findViewById(R.id.itemIcon)).setImageResource(R.drawable.ic_navigation_arrow);
            ((TextView)layoutTz.findViewById(R.id.itemTitle)).setText("Timezone");
            ((TextView)layoutTz.findViewById(R.id.itemSubtitle)).setText("Loading...");
        }

        // DND
        View layoutDND = findViewById(R.id.layoutDND);
        if (layoutDND != null) {
            ((ImageView)layoutDND.findViewById(R.id.itemIcon)).setImageResource(R.drawable.ic_moon);
            ((TextView)layoutDND.findViewById(R.id.itemTitle)).setText("DND Window");
            ((TextView)layoutDND.findViewById(R.id.itemSubtitle)).setText("Loading...");
        }

        // Proximity
        View layoutProx = findViewById(R.id.layoutProximitySharing);
        if (layoutProx != null) {
            ((ImageView)layoutProx.findViewById(R.id.itemIcon)).setImageResource(R.drawable.ic_contacts);
            ((TextView)layoutProx.findViewById(R.id.itemTitle)).setText("Mutual Proximity");
            ((TextView)layoutProx.findViewById(R.id.itemSubtitle)).setText("Only share when both friends opt-in");
        }
    }

    private void setupServerUrl() {
        View layoutServer = findViewById(R.id.layoutServerUrl);
        if (layoutServer == null) return;

        ImageView icon = layoutServer.findViewById(R.id.itemIcon);
        TextView title = layoutServer.findViewById(R.id.itemTitle);
        TextView subtitle = layoutServer.findViewById(R.id.itemSubtitle);

        icon.setImageResource(R.drawable.ic_search); // Using search icon for server
        title.setText("Server URL");
        
        String currentUrl = sessionManager.getServerUrl();
        subtitle.setText(currentUrl);
        subtitle.setVisibility(View.VISIBLE);

        layoutServer.setOnClickListener(v -> {
            final android.widget.EditText input = new android.widget.EditText(this);
            input.setText(currentUrl);
            new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialogTheme)
                .setTitle("Update Server URL")
                .setView(input)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newUrl = input.getText().toString().trim();
                    if (!newUrl.isEmpty()) {
                        sessionManager.setServerUrl(newUrl);
                        Toast.makeText(this, "Server updated (requires restart)", Toast.LENGTH_SHORT).show();
                        subtitle.setText(newUrl);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    private void setupHardeningUI(com.rudra.myassistant.network.ViperApiService.UserSettings settings) {
        // Proximity Toggle
        View layoutProx = findViewById(R.id.layoutProximitySharing);
        ImageView proxIcon = layoutProx.findViewById(R.id.itemIcon);
        TextView proxTitle = layoutProx.findViewById(R.id.itemTitle);
        TextView proxSub = layoutProx.findViewById(R.id.itemSubtitle);
        SwitchMaterial proxToggle = layoutProx.findViewById(R.id.itemToggle);

        proxIcon.setImageResource(R.drawable.ic_contacts);
        proxTitle.setText("Mutual Proximity");
        proxSub.setText("Only share when both friends opt-in");
        proxToggle.setChecked(settings.proximity_sharing_enabled != null && settings.proximity_sharing_enabled);
        proxToggle.setOnCheckedChangeListener((b, isChecked) -> updateBackendSettings("proximity_sharing_enabled", isChecked));

        // Timezone
        View layoutTz = findViewById(R.id.layoutTimezone);
        ImageView tzIcon = layoutTz.findViewById(R.id.itemIcon);
        TextView tzTitle = layoutTz.findViewById(R.id.itemTitle);
        TextView tzSub = layoutTz.findViewById(R.id.itemSubtitle);
        
        tzIcon.setImageResource(R.drawable.ic_navigation_arrow);
        tzTitle.setText("Timezone");
        tzSub.setText(settings.timezone != null ? settings.timezone : "UTC");
        tzSub.setVisibility(View.VISIBLE);
        layoutTz.setOnClickListener(v -> {
            // Simple selection for demo, usually we'd have a searchable list
            String[] tzs = {"UTC", "America/New_York", "Europe/London", "Asia/Kolkata", "Asia/Tokyo"};
            new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialogTheme)
                .setTitle("Select Timezone")
                .setItems(tzs, (dialog, which) -> {
                    updateBackendSettings("timezone", tzs[which]);
                    tzSub.setText(tzs[which]);
                }).show();
        });

        // DND
        View layoutDND = findViewById(R.id.layoutDND);
        ImageView dndIcon = layoutDND.findViewById(R.id.itemIcon);
        TextView dndTitle = layoutDND.findViewById(R.id.itemTitle);
        TextView dndSub = layoutDND.findViewById(R.id.itemSubtitle);
        
        dndIcon.setImageResource(R.drawable.ic_moon);
        dndTitle.setText("DND Window");
        dndSub.setText((settings.dnd_start != null ? settings.dnd_start : "22:00") + " - " + (settings.dnd_end != null ? settings.dnd_end : "07:00"));
        dndSub.setVisibility(View.VISIBLE);
        layoutDND.setOnClickListener(v -> showDNDPickers(settings, dndSub));
    }

    private void showDNDPickers(com.rudra.myassistant.network.ViperApiService.UserSettings settings, TextView sub) {
        com.google.android.material.timepicker.MaterialTimePicker startPicker = new com.google.android.material.timepicker.MaterialTimePicker.Builder()
            .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_24H)
            .setHour(22).setMinute(0).setTitleText("DND Start").build();
            
        startPicker.addOnPositiveButtonClickListener(v -> {
            String startTime = String.format("%02d:%02d", startPicker.getHour(), startPicker.getMinute());
            updateBackendSettings("dnd_start", startTime);
            
            com.google.android.material.timepicker.MaterialTimePicker endPicker = new com.google.android.material.timepicker.MaterialTimePicker.Builder()
                .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_24H)
                .setHour(7).setMinute(0).setTitleText("DND End").build();
                
            endPicker.addOnPositiveButtonClickListener(v2 -> {
                String endTime = String.format("%02d:%02d", endPicker.getHour(), endPicker.getMinute());
                updateBackendSettings("dnd_end", endTime);
                sub.setText(startTime + " - " + endTime);
            });
            endPicker.show(getSupportFragmentManager(), "DND_END");
        });
        startPicker.show(getSupportFragmentManager(), "DND_START");
    }

    private void updateBackendSettings(String key, Object value) {
        String userId = mAuth.getUid();
        if (userId == null) return;
        
        com.rudra.myassistant.network.ViperApiService api = RetrofitClient.getViperInstance().create(com.rudra.myassistant.network.ViperApiService.class);
        com.rudra.myassistant.network.ViperApiService.UserSettings update = new com.rudra.myassistant.network.ViperApiService.UserSettings();
        if (key.equals("proximity_sharing_enabled")) update.proximity_sharing_enabled = (Boolean) value;
        if (key.equals("timezone")) update.timezone = (String) value;
        if (key.equals("dnd_start")) update.dnd_start = (String) value;
        if (key.equals("dnd_end")) update.dnd_end = (String) value;

        api.updateSettings(userId, update).enqueue(new retrofit2.Callback<Void>() {
            @Override public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                if (response.isSuccessful()) Toast.makeText(SettingsActivity.this, "Settings updated", Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(retrofit2.Call<Void> call, Throwable t) {}
        });
    }

    private void performLogout() {
        new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialogTheme)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    mAuth.signOut();
                    sessionManager.logoutUser();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
