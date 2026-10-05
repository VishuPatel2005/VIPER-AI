package com.rudra.myassistant;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.UserSessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TodayRemindersActivity extends BaseEdgeToEdgeActivity {

    private static final String TAG = "TodayReminders";
    private RecyclerView recyclerView;
    private ReminderAdapter adapter;
    private TextView emptyText;
    private FirestoreRepository firestoreRepository;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_reminder_list);

            TextView titleTextView = findViewById(R.id.title);
            if (titleTextView != null) titleTextView.setText("Today");

            View backButton = findViewById(R.id.back_button);
            if (backButton != null) backButton.setOnClickListener(v -> finish());

            View deleteAllButton = findViewById(R.id.delete_all_button);
            if (deleteAllButton != null) {
                deleteAllButton.setVisibility(View.GONE); // Hide by default for Today view
            }

            recyclerView = findViewById(R.id.reminders_recycler_view);
            emptyText = findViewById(R.id.empty_text);

            if (recyclerView != null) {
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                adapter = new ReminderAdapter(this);
                recyclerView.setAdapter(adapter);

                adapter.setOnReminderChangedListener(new ReminderAdapter.OnReminderChangedListener() {
                    @Override
                    public void onReminderChanged() {
                        loadReminders();
                    }

                    @Override
                    public void onReminderCompleted(Reminder reminder) {
                        if (reminder != null) markAsCompleted(reminder.getBackendId());
                    }
                });
            }

            firestoreRepository = new FirestoreRepository();
            UserSessionManager sessionManager = new UserSessionManager(this);
            userId = sessionManager.getUserId();

            loadReminders();

            // Handle highlighting from intents
            if (getIntent() != null) {
                com.viperai.notifications.NotificationRouter.handleIntent(this, getIntent());
            }
        } catch (Throwable t) {
            Log.e(TAG, "onCreate failed", t);
            finish();
        }
    }

    public void highlightReminder(String id) {
        if (id == null || recyclerView == null) return;
        
        recyclerView.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            
            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                View child = recyclerView.getChildAt(i);
                if (adapter != null) {
                    Reminder r = adapter.getReminderAt(i);
                    if (r != null && id.equals(r.getBackendId())) {
                        animateHighlight(child);
                        break;
                    }
                }
            }
        }, 800);
    }

    private void animateHighlight(View view) {
        if (view == null) return;
        ValueAnimator anim = ValueAnimator.ofArgb(0x00000000, 0x4D71FF00, 0x00000000);
        anim.addUpdateListener(animation -> view.setBackgroundColor((int) animation.getAnimatedValue()));
        anim.setDuration(2000);
        anim.start();
    }

    private void loadReminders() {
        if (userId == null) return;

        if (emptyText != null) emptyText.setText("Loading...");
        if (emptyText != null) emptyText.setVisibility(View.VISIBLE);

        firestoreRepository.getRemindersQuery(userId).get().addOnSuccessListener(snapshot -> {
            if (isFinishing() || isDestroyed()) return;

            List<Reminder> reminders = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                if (!Boolean.TRUE.equals(doc.getBoolean("completed"))) {
                    Reminder r = convertToReminder(doc);
                    if (r != null) reminders.add(r);
                }
            }

            List<Reminder> filtered = filterToday(reminders);
            
            if (adapter != null) {
                adapter.setReminders(filtered);
                if (filtered.isEmpty()) {
                    if (emptyText != null) {
                        emptyText.setText("No reminders for today");
                        emptyText.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (emptyText != null) emptyText.setVisibility(View.GONE);
                }
            }
        }).addOnFailureListener(e -> {
            if (isFinishing() || isDestroyed()) return;
            Log.e(TAG, "Load failed", e);
            if (emptyText != null) emptyText.setText("Failed to load");
        });
    }

    private List<Reminder> filterToday(List<Reminder> reminders) {
        List<Reminder> today = new ArrayList<>();
        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_YEAR);
        int year = now.get(Calendar.YEAR);

        Calendar rCal = Calendar.getInstance();
        for (Reminder r : reminders) {
            if (r.getDateTimestamp() != null) {
                rCal.setTimeInMillis(r.getDateTimestamp());
                if (rCal.get(Calendar.DAY_OF_YEAR) == day && rCal.get(Calendar.YEAR) == year) {
                    today.add(r);
                }
            } else {
                // Reminders without date are treated as "some day" or "today" if intended
                // Here we'll only include those with today's date
            }
        }
        return today;
    }

    private Reminder convertToReminder(DocumentSnapshot doc) {
        try {
            String text = doc.getString("text");
            boolean isFlagged = Boolean.TRUE.equals(doc.getBoolean("flagged"));
            boolean isCompleted = Boolean.TRUE.equals(doc.getBoolean("completed"));
            Long dateTimestamp = doc.getLong("date_timestamp"); // Assume stored as long
            Integer hour = doc.getLong("hour") != null ? doc.getLong("hour").intValue() : null;
            Integer minute = doc.getLong("minute") != null ? doc.getLong("minute").intValue() : null;

            Reminder r = new Reminder(0, text, isFlagged, false, isCompleted, dateTimestamp, hour, minute);
            r.setBackendId(doc.getId());
            r.setDateTime(doc.getString("datetime"));
            return r;
        } catch (Exception e) {
            Log.e(TAG, "Error converting reminder", e);
            return null;
        }
    }

    private void markAsCompleted(String docId) {
        if (docId == null) return;
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("completed", true);
        firestoreRepository.updateReminder(docId, updates)
                .addOnSuccessListener(aVoid -> loadReminders())
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
    }
}
