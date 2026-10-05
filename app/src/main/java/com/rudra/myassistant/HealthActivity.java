package com.rudra.myassistant;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.UserSessionManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HealthActivity extends BaseEdgeToEdgeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health);

        ImageView backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        View medicationCard = findViewById(R.id.medication_card);
        if (medicationCard != null) {
            medicationCard.setOnClickListener(v -> {
                startActivity(new Intent(this, MedicationDetailActivity.class));
            });
        }

        updateHealthSummary();
        updateGreetingAndDate();
        updatePeriodCountdown();

        View periodCard = findViewById(R.id.period_card);
        if (periodCard != null) {
            periodCard.setOnClickListener(v -> {
                startActivity(new Intent(this, PeriodDetailActivity.class));
            });
        }
        


        // Viper Motion System — entrance animation
        View root = findViewById(android.R.id.content);
        if (root != null) {
            float dp = root.getResources().getDisplayMetrics().density * 24;
            root.setTranslationY(dp);
            root.setAlpha(0.5f);
            root.animate().translationY(0).alpha(1f)
                    .setDuration(350)
                    .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                    .start();
        }
    }

    private void updatePeriodCountdown() {
        TextView countdownText = findViewById(R.id.period_countdown);
        android.widget.Button addDateBtn = findViewById(R.id.btn_add_period_date);
        if (countdownText == null || addDateBtn == null) return;

        UserSessionManager sessionManager = new UserSessionManager(this);
        String userId = sessionManager.getUserId();
        FirestoreRepository repo = new FirestoreRepository();
        repo.getUser(userId).addOnSuccessListener(doc -> {
            if (doc.exists() && doc.contains("last_period_start")) {
                String startDateStr = doc.getString("last_period_start");
                if (startDateStr == null || startDateStr.isEmpty()) {
                    showAddDateButton(countdownText, addDateBtn);
                    return;
                }

                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date startDate = sdf.parse(startDateStr);
                    long lastStart = startDate.getTime();
                    long twentyEightDaysMs = 28L * 24 * 60 * 60 * 1000;
                    long nextStart = lastStart + twentyEightDaysMs;
                    while (nextStart < System.currentTimeMillis()) nextStart += twentyEightDaysMs;

                    long diffMs = nextStart - System.currentTimeMillis();
                    int diffDays = (int) Math.ceil(diffMs / (1000.0 * 60 * 60 * 24));
                    addDateBtn.setVisibility(View.GONE);
                    countdownText.setVisibility(View.VISIBLE);
                    countdownText.setText(diffDays <= 0 ? "Today" : diffDays + (diffDays == 1 ? " Day" : " Days"));
                } catch (Exception e) {
                    showAddDateButton(countdownText, addDateBtn);
                }
            } else {
                showAddDateButton(countdownText, addDateBtn);
            }
        });
    }

    private void showAddDateButton(TextView countdownText, android.widget.Button addDateBtn) {
        countdownText.setVisibility(View.GONE);
        addDateBtn.setVisibility(View.VISIBLE);
        addDateBtn.setOnClickListener(v -> startActivity(new Intent(HealthActivity.this, PeriodDetailActivity.class)));
    }

    private void updateGreetingAndDate() {
        TextView greetingText = findViewById(R.id.greeting_text);
        TextView dateText = findViewById(R.id.date_text);

        if (greetingText != null) {
            java.util.Calendar c = java.util.Calendar.getInstance();
            int hour = c.get(java.util.Calendar.HOUR_OF_DAY);
            if (hour >= 5 && hour < 12) greetingText.setText("Good Morning!");
            else if (hour >= 12 && hour < 17) greetingText.setText("Good Afternoon!");
            else greetingText.setText("Good Evening!");
        }

        if (dateText != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE, MMM d", java.util.Locale.getDefault());
            dateText.setText(sdf.format(new java.util.Date()));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHealthSummary();
        updatePeriodCountdown();
        updateDailyStats();
    }

    private void updateDailyStats() {
        String userId = new UserSessionManager(this).getUserId();
        FirestoreRepository repo = new FirestoreRepository();
        repo.getDailyStats(userId).addOnSuccessListener(doc -> {
            if (doc.exists()) {
                // Steps
                long steps = doc.contains("steps_count") ? doc.getLong("steps_count") : 0;
                TextView stepsTv = findViewById(R.id.activity_value);
                if (stepsTv != null) stepsTv.setText(String.format(Locale.getDefault(), "%,d steps", steps));

                // Sleep
                double sleep = doc.contains("sleep_hours") ? doc.getDouble("sleep_hours") : 0;
                TextView sleepTv = findViewById(R.id.sleep_value);
                if (sleepTv != null) {
                    int h = (int) sleep;
                    int m = (int) ((sleep - h) * 60);
                    sleepTv.setText(String.format(Locale.getDefault(), "%dh %02dm", h, m));
                }


            }
        });
    }



    private void updateHealthSummary() {
        UserSessionManager sessionManager = new UserSessionManager(this);
        String userId = sessionManager.getUserId();
        FirestoreRepository repo = new FirestoreRepository();
        repo.getMedicationsQuery(userId).get().addOnSuccessListener(snapshot -> {
            int total = snapshot.size();
            int taken = 0;
            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                if (Boolean.TRUE.equals(doc.getBoolean("taken"))) taken++;
            }
            updateHealthSummaryUI(total, taken);
        });
    }

    private void updateHealthSummaryUI(int total, int taken) {
        TextView todaysMeds = findViewById(R.id.todays_meds);
        if (todaysMeds != null) {
            todaysMeds.setText("Today's Meds\n(" + taken + " of " + total + " Taken)");
        }

        ProgressBar medProgress = findViewById(R.id.med_progress);
        if (medProgress != null) {
            int progress = (total > 0) ? (taken * 100 / total) : 0;
            medProgress.setProgress(progress);
        }

        TextView progressPercent = findViewById(R.id.progress_percent);
        if (progressPercent != null) {
            int progress = (total > 0) ? (taken * 100 / total) : 0;
            progressPercent.setText(progress + "%");
        }
    }
}
