package com.rudra.myassistant;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.datepicker.MaterialDatePicker;
import androidx.core.util.Pair;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.UserSessionManager;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PeriodDetailActivity extends BaseEdgeToEdgeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_period_detail);

        ImageView backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        TextView dateText = findViewById(R.id.current_date_text);
        if (dateText != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault());
            dateText.setText("Today, " + sdf.format(new Date()));
        }

        ImageView calButton = findViewById(R.id.calendar_button);
        if (calButton != null) {
            calButton.setOnClickListener(v -> showDateRangePicker());
        }

        findViewById(R.id.btn_log_period).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, LogPeriodActivity.class));
        });

        findViewById(R.id.btn_log_symptoms).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, LogSymptomsActivity.class));
        });

        findViewById(R.id.btn_log_spotting).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, LogSpottingActivity.class));
        });

        findViewById(R.id.btn_open_history).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, PeriodHistoryActivity.class));
        });

        loadPeriodData();
    }

    private void loadPeriodData() {
        UserSessionManager sessionManager = new UserSessionManager(this);
        String userId = sessionManager.getUserId();
        FirestoreRepository repo = new FirestoreRepository();
        repo.getUser(userId).addOnSuccessListener(doc -> {
            if (doc.exists() && doc.contains("last_period_start")) {
                String start = doc.getString("last_period_start");
                String end = doc.getString("last_period_end");
                if (start != null && end != null) {
                    TextView statusText = findViewById(R.id.period_status_text);
                    if (statusText != null) {
                        statusText.setText("Last Period: " + start + " - " + end);
                    }
                }
            }
        });
    }



    private void showDateRangePicker() {
        if (isFinishing() || isDestroyed()) return;

        MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Last Period Dates")
                .setTheme(R.style.MaterialDatePickerTheme)
                .build();

        picker.show(getSupportFragmentManager(), "PERIOD_RANGE_PICKER");

        picker.addOnPositiveButtonClickListener(selection -> {
            Long startDate = selection.first;
            Long endDate = selection.second;
            
            if (startDate != null && endDate != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String startStr = sdf.format(new Date(startDate));
                String endStr = sdf.format(new Date(endDate));
                
                UserSessionManager sessionManager = new UserSessionManager(PeriodDetailActivity.this);
                String userId = sessionManager.getUserId();
                FirestoreRepository repo = new FirestoreRepository();
                Map<String, Object> updates = new HashMap<>();
                updates.put("last_period_start", startStr);
                updates.put("last_period_end", endStr);
                repo.usersCol.document(userId).update(updates).addOnSuccessListener(v -> {
                    loadPeriodData();
                    android.widget.Toast.makeText(PeriodDetailActivity.this, "Period dates updated!", android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
