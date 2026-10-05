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

public class ScheduledRemindersActivity extends BaseEdgeToEdgeActivity {

    private RecyclerView recyclerView;
    private ReminderAdapter adapter;
    private TextView emptyText;
    private FirestoreRepository firestoreRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_list);

        firestoreRepository = new FirestoreRepository();

        TextView titleTextView = findViewById(R.id.title);
        titleTextView.setText("Scheduled");

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

        adapter.setOnItemLongClickListener(new ReminderAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick() {}

            @Override
            public void onSelectionChanged(int selectedCount) {}
        });
        recyclerView.setAdapter(adapter);

        loadReminders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReminders();
    }

    @Override
    public void onBackPressed() {
        if (adapter != null && adapter.isSelectionMode()) {
            adapter.setSelectionMode(false);
        } else {
            super.onBackPressed();
        }
    }

    private void loadReminders() {
        emptyText.setText("Loading...");
        emptyText.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        UserSessionManager sessionManager = new UserSessionManager(this);
        String userId = sessionManager.getUserId();
        
        firestoreRepository.getRemindersQuery(userId).get().addOnSuccessListener(snapshot -> {
            List<Reminder> reminders = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                reminders.add(convertToReminder(doc));
            }
            
            // Client-side sort to avoid index requirement
            reminders.sort((r1, r2) -> {
                String d1 = r1.getDateTime() != null ? r1.getDateTime() : "";
                String d2 = r2.getDateTime() != null ? r2.getDateTime() : "";
                return d1.compareTo(d2);
            });
            
            List<Reminder> filtered = filterScheduled(reminders);
            adapter.setReminders(filtered);
            if (filtered.isEmpty()) {
                emptyText.setText("No reminders yet");
                emptyText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }).addOnFailureListener(e -> {
            emptyText.setText("Error loading reminders");
        });
    }

    private List<Reminder> filterScheduled(List<Reminder> reminders) {
        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        long todayStart = now.getTimeInMillis();
        long tomorrowStart = todayStart + 24 * 60 * 60 * 1000;

        List<Reminder> result = new ArrayList<>();
        for (Reminder r : reminders) {
            if (r.getDateTimestamp() != null && !r.isCompleted()) {
                Calendar rem = Calendar.getInstance();
                rem.setTimeInMillis(r.getDateTimestamp());
                rem.set(Calendar.HOUR_OF_DAY, 0);
                rem.set(Calendar.MINUTE, 0);
                rem.set(Calendar.SECOND, 0);
                rem.set(Calendar.MILLISECOND, 0);

                if (rem.getTimeInMillis() >= tomorrowStart) {
                    result.add(r);
                }
            }
        }
        return result;
    }

    private Reminder convertToReminder(DocumentSnapshot doc) {
        String text = doc.getString("text");
        boolean isFlagged = Boolean.TRUE.equals(doc.getBoolean("flagged"));
        boolean isCompleted = Boolean.TRUE.equals(doc.getBoolean("completed"));
        String datetime = doc.getString("datetime");

        Long dateTimestamp = null;
        Integer hour = null;
        Integer minute = null;

        if (datetime != null && !datetime.isEmpty()) {
            try {
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                java.util.Date date = fmt.parse(datetime);
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

    private void markAsCompleted(String docId) {
        if (docId == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("completed", true);
        firestoreRepository.updateReminder(docId, updates).addOnSuccessListener(v -> loadReminders());
    }

    private void toggleFlag(Reminder reminder) {
        if (reminder.getBackendId() == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("flagged", !reminder.isFlagged());
        firestoreRepository.updateReminder(reminder.getBackendId(), updates).addOnSuccessListener(v -> loadReminders());
    }

    private void deleteAllReminders() {
        if (adapter == null || adapter.getItemCount() == 0) return;
        
        List<String> idsToDelete = new ArrayList<>();
        if (adapter.isSelectionMode()) {
            idsToDelete.addAll(adapter.getSelectedIds());
        } else {
            for (Reminder r : adapter.getReminders()) {
                if (r.getBackendId() != null) idsToDelete.add(r.getBackendId());
            }
        }

        if (idsToDelete.isEmpty()) return;

        emptyText.setText("Deleting...");
        emptyText.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        deleteRecursive(idsToDelete, 0);
    }

    private void deleteRecursive(List<String> ids, int index) {
        if (index >= ids.size()) {
            if (adapter.isSelectionMode()) adapter.setSelectionMode(false);
            loadReminders();
            return;
        }

        firestoreRepository.remindersCol.document(ids.get(index)).delete().addOnCompleteListener(task -> {
            deleteRecursive(ids, index + 1);
        });
    }
}
