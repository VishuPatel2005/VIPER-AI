package com.rudra.myassistant;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.UserSessionManager;

import java.util.Calendar;

public class RemindersActivity extends BaseEdgeToEdgeActivity {

    private static final String TAG = "RemindersActivity";
    private TextView todayCount;
    private TextView scheduledCount;
    private TextView allCount;
    private TextView flaggedCount;
    private boolean isInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_reminders);

            View backBtn = findViewById(R.id.back_button);
            if (backBtn != null) backBtn.setOnClickListener(v -> finish());

            todayCount = findViewById(R.id.today_count);
            scheduledCount = findViewById(R.id.scheduled_count);
            allCount = findViewById(R.id.all_count);
            flaggedCount = findViewById(R.id.flagged_count);

            // Set up click listeners for reminder categories
            View todayCard = findViewById(R.id.today_card);
            if (todayCard != null) todayCard.setOnClickListener(v -> {
                startActivity(new Intent(RemindersActivity.this, TodayRemindersActivity.class));
            });

            View scheduledCard = findViewById(R.id.scheduled_card);
            if (scheduledCard != null) scheduledCard.setOnClickListener(v -> {
                startActivity(new Intent(RemindersActivity.this, ScheduledRemindersActivity.class));
            });

            View allCard = findViewById(R.id.all_card);
            if (allCard != null) allCard.setOnClickListener(v -> {
                startActivity(new Intent(RemindersActivity.this, AllRemindersActivity.class));
            });

            View flaggedCard = findViewById(R.id.flagged_card);
            if (flaggedCard != null) flaggedCard.setOnClickListener(v -> {
                startActivity(new Intent(RemindersActivity.this, FlaggedRemindersActivity.class));
            });

            View completedCard = findViewById(R.id.completed_card);
            if (completedCard != null) completedCard.setOnClickListener(v -> {
                startActivity(new Intent(RemindersActivity.this, CompletedRemindersActivity.class));
            });

            FloatingActionButton fab = findViewById(R.id.add_reminder_fab);
            if (fab != null) {
                fab.setOnClickListener(view -> {
                    startActivity(new Intent(RemindersActivity.this, NewReminderActivity.class));
                });
            }

            // Simple entry animation for the dashboard content
            View contentView = findViewById(R.id.reminders_grid);
            if (contentView != null) {
                float dp = contentView.getResources().getDisplayMetrics().density * 24;
                contentView.setTranslationY(dp);
                contentView.setAlpha(0f);
                contentView.animate().translationY(0).alpha(1f)
                        .setDuration(450)
                        .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                        .start();
            }

            // Handle incoming intents for highlighting
            if (getIntent() != null) {
                com.viperai.notifications.NotificationRouter.handleIntent(this, getIntent());
            }

            isInitialized = true;
            updateReminderCounts();

        } catch (Throwable t) {
            Log.e(TAG, "CRITICAL: onCreate failed", t);
            Toast.makeText(this, "Failed to load Reminders", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isInitialized) {
            updateReminderCounts();
            
            // Sync geofences in background to avoid UI block
            new Thread(() -> {
                try {
                    new ReminderRepository(this).syncGeofences();
                } catch (Exception e) {
                    Log.e(TAG, "Geofence sync failed", e);
                }
            }).start();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null) {
            com.viperai.notifications.NotificationRouter.handleIntent(this, intent);
        }
    }

    public void highlightReminder(String id) {
        if (id == null) return;
        Intent intent = new Intent(this, TodayRemindersActivity.class);
        intent.putExtra(com.viperai.notifications.NotificationConstants.EXTRA_REMINDER_ID, id);
        startActivity(intent);
    }

    private void updateReminderCounts() {
        UserSessionManager sessionManager = new UserSessionManager(this);
        String userId = sessionManager.getUserId();

        if (userId == null) {
            Log.w(TAG, "No user ID found, skipping count update");
            return;
        }

        FirestoreRepository firestoreRepo = new FirestoreRepository();
        firestoreRepo.getRemindersQuery(userId).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (isFinishing() || isDestroyed()) return;

            try {
                int[] counts = computeCounts(queryDocumentSnapshots);
                
                if (todayCount != null) todayCount.setText(String.valueOf(counts[0]));
                if (scheduledCount != null) scheduledCount.setText(String.valueOf(counts[1]));
                if (allCount != null) allCount.setText(String.valueOf(counts[2]));
                if (flaggedCount != null) flaggedCount.setText(String.valueOf(counts[3]));
            } catch (Exception e) {
                Log.e(TAG, "Error computing counts", e);
            }

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Firestore fetch failed", e);
            setCountsToZero();
        });
    }

    private void setCountsToZero() {
        if (todayCount != null) todayCount.setText("0");
        if (scheduledCount != null) scheduledCount.setText("0");
        if (allCount != null) allCount.setText("0");
        if (flaggedCount != null) flaggedCount.setText("0");
    }

    private int[] computeCounts(com.google.firebase.firestore.QuerySnapshot queryDocumentSnapshots) {
        int today = 0, scheduled = 0, all = 0, flagged = 0;

        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        long todayStart = now.getTimeInMillis();
        long tomorrowStart = todayStart + 24 * 60 * 60 * 1000;

        Calendar rCal = Calendar.getInstance();

        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
            boolean completed = Boolean.TRUE.equals(doc.getBoolean("completed"));
            if (completed) continue;

            all++;

            if (Boolean.TRUE.equals(doc.getBoolean("flagged")) || Boolean.TRUE.equals(doc.getBoolean("urgent"))) {
                flagged++;
            }

            String dt = doc.getString("datetime");
            if (dt != null && !dt.isEmpty()) {
                scheduled++;
                try {
                    // Try to determine if it's for today
                    if (dt.contains("T")) {
                         String datePart = dt.split("T")[0];
                         java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                         String todayStr = sdf.format(new java.util.Date());
                         if (datePart.equals(todayStr)) {
                             today++;
                         }
                    }
                } catch (Exception ignored) {}
            }
        }

        return new int[]{today, scheduled, all, flagged};
    }
}
