package com.rudra.myassistant;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.UserSessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FlaggedRemindersActivity extends BaseEdgeToEdgeActivity {

    private RecyclerView recyclerView;
    private ReminderAdapter adapter;
    private TextView emptyText;
    private FirestoreRepository firestoreRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_list);

        firestoreRepo = new FirestoreRepository();

        TextView titleTextView = findViewById(R.id.title);
        titleTextView.setText("Flagged");

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        View deleteAllButton = findViewById(R.id.delete_all_button);
        deleteAllButton.setOnClickListener(v -> deleteAllReminders());

        recyclerView = findViewById(R.id.reminders_recycler_view);
        emptyText = findViewById(R.id.empty_text);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReminderAdapter(this);
        adapter.setOnReminderChangedListener(new ReminderAdapter.OnReminderChangedListener() {
            @Override
            public void onReminderChanged() {
                loadReminders();
            }

            @Override
            public void onReminderCompleted(Reminder reminder) {
                markAsCompleted(reminder.getBackendId());
            }

            @Override
            public void onReminderFlagged(Reminder reminder) {
                toggleFlag(reminder);
            }
        });

        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReminders();
    }

    private void loadReminders() {
        emptyText.setText("Loading...");
        emptyText.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        UserSessionManager sessionManager = new UserSessionManager(this);
        String userId = sessionManager.getUserId();
        
        firestoreRepo.getRemindersQuery(userId).get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Reminder> reminders = new ArrayList<>();
            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                Boolean isFlagged = doc.getBoolean("flagged");
                Boolean isCompleted = doc.getBoolean("completed");
                
                // Only show flagged and NOT completed
                if (Boolean.TRUE.equals(isFlagged) && !Boolean.TRUE.equals(isCompleted)) {
                    reminders.add(convertToReminder(doc));
                }
            }
            
            // Client-side sort to avoid index requirement
            reminders.sort((r1, r2) -> {
                String d1 = r1.getDateTime() != null ? r1.getDateTime() : "";
                String d2 = r2.getDateTime() != null ? r2.getDateTime() : "";
                return d1.compareTo(d2);
            });
            
            adapter.setReminders(reminders);
            if (reminders.isEmpty()) {
                emptyText.setText("No flagged reminders");
                emptyText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }).addOnFailureListener(e -> {
            emptyText.setText("Error loading reminders");
            Toast.makeText(FlaggedRemindersActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private Reminder convertToReminder(DocumentSnapshot doc) {
        String text = doc.getString("text");
        String datetime = doc.getString("datetime");
        boolean isFlagged = Boolean.TRUE.equals(doc.getBoolean("flagged"));
        boolean isCompleted = Boolean.TRUE.equals(doc.getBoolean("completed"));

        Long dateTimestamp = null;
        Integer hour = null;
        Integer minute = null;

        if (datetime != null && !datetime.isEmpty()) {
            try {
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
                java.util.Date date = fmt.parse(datetime);
                if (date == null) {
                    fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                    date = fmt.parse(datetime);
                }
                if (date != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    Calendar dayStart = Calendar.getInstance();
                    dayStart.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
                    dayStart.set(Calendar.MILLISECOND, 0);
                    dateTimestamp = dayStart.getTimeInMillis();
                    hour = cal.get(Calendar.HOUR_OF_DAY);
                    minute = cal.get(Calendar.MINUTE);
                }
            } catch (Exception ignored) {}
        }

        Reminder r = new Reminder(0, text, isFlagged, false, isCompleted, dateTimestamp, hour, minute);
        r.setBackendId(doc.getId());
        r.setDateTime(datetime);
        return r;
    }

    private void markAsCompleted(String id) {
        if (id == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("completed", true);
        firestoreRepo.updateReminder(id, updates).addOnSuccessListener(v -> loadReminders());
    }

    private void toggleFlag(Reminder reminder) {
        String id = reminder.getBackendId();
        if (id == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("flagged", !reminder.isFlagged());
        firestoreRepo.updateReminder(id, updates).addOnSuccessListener(v -> loadReminders());
    }

    private void deleteAllReminders() {
        if (adapter == null || adapter.getReminders().isEmpty()) return;

        emptyText.setText("Deleting...");
        emptyText.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        List<Reminder> reminders = adapter.getReminders();
        for (int i = 0; i < reminders.size(); i++) {
            String id = reminders.get(i).getBackendId();
            if (id != null) {
                boolean isLast = (i == reminders.size() - 1);
                firestoreRepo.deleteReminder(id).addOnCompleteListener(task -> {
                    if (isLast) loadReminders();
                });
            }
        }
    }
}
