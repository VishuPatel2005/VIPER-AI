package com.rudra.myassistant;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.PopupMenu;
import android.widget.Toast;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.MedicationDto;
import com.rudra.myassistant.network.UserSessionManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MedicationDetailActivity extends BaseEdgeToEdgeActivity {

    private RecyclerView recyclerView;
    private MedicationAdapter adapter;
    private List<MedicationDto> medicationList = new ArrayList<>();

    private ProgressBar progressBar;
    private TextView progressPercent, takenText, remainingText, totalCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication_detail);

        initViews();
        setupRecyclerView();
        
        loadData();

        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        findViewById(R.id.add_medication_button).setOnClickListener(v -> {
            startActivity(new Intent(this, AddMedicationActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void initViews() {
        progressBar = findViewById(R.id.med_progress);
        progressPercent = findViewById(R.id.progress_percent);
        takenText = findViewById(R.id.taken_text);
        remainingText = findViewById(R.id.remaining_text);
        totalCount = findViewById(R.id.total_meds_count);
        
        TextView todayLabel = findViewById(R.id.today_label);
        if (todayLabel != null) {
            todayLabel.setVisibility(View.GONE);
        }
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.medications_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MedicationAdapter(new ArrayList<>(), new MedicationAdapter.OnMedicationClickListener() {
            @Override
            public void onMarkTakenClick(MedicationDto medication) {
                onMarkTaken(medication);
            }

            @Override
            public void onInfoClick(View view, MedicationDto medication) {
                showMedicationOptionsMenu(view, medication);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadData() {
        UserSessionManager sessionManager = new UserSessionManager(this);
        String userId = sessionManager.getUserId();
        FirestoreRepository repo = new FirestoreRepository();
        repo.getMedicationsQuery(userId).get().addOnSuccessListener(snapshot -> {
            medicationList = new ArrayList<>();
            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                MedicationDto m = new MedicationDto();
                m.setId(doc.getId());
                m.setName(doc.getString("name"));
                m.setDosage(doc.getString("dosage"));
                m.setTime(doc.getString("time"));
                m.setDays((List<String>) doc.get("days"));
                m.setSpecificDate(doc.getString("specific_date"));
                m.setTaken(Boolean.TRUE.equals(doc.getBoolean("taken")));
                medicationList.add(m);
            }
            
            List<Object> groupedList = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String todayStr = sdf.format(new java.util.Date());

            java.util.Map<String, List<MedicationDto>> dateMap = new java.util.TreeMap<>();
            for (MedicationDto m : medicationList) {
                if (m.getSpecificDate() != null) {
                    String d = m.getSpecificDate();
                    if (!dateMap.containsKey(d)) dateMap.put(d, new ArrayList<>());
                    dateMap.get(d).add(m);
                }
            }

            for (String dateStr : dateMap.keySet()) {
                String header = dateStr;
                if (dateStr.equals(todayStr)) header = "Today - " + new SimpleDateFormat("MMM d", Locale.getDefault()).format(new Date());
                else {
                    try { header = new SimpleDateFormat("EEEE - MMM d", Locale.getDefault()).format(sdf.parse(dateStr)); } catch (Exception e) {}
                }
                groupedList.add(header);
                groupedList.addAll(dateMap.get(dateStr));
            }

            String[] daysOrder = {"Everyday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
            for (String day : daysOrder) {
                List<MedicationDto> medsForDay = new ArrayList<>();
                for (MedicationDto m : medicationList) {
                    if (m.getSpecificDate() == null && m.getDays() != null && m.getDays().contains(day)) medsForDay.add(m);
                }
                if (!medsForDay.isEmpty()) {
                    groupedList.add(resolveDayHeader(day));
                    groupedList.addAll(medsForDay);
                }
            }
            adapter.setItems(groupedList);
            updateSummary();
        });
    }

    private void updateSummary() {
        Calendar now = Calendar.getInstance();
        String dayOfWeek = new SimpleDateFormat("EEEE", Locale.getDefault()).format(now.getTime());
        String dateToday = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.getTime());

        int totalToday = 0;
        int takenToday = 0;

        for (MedicationDto m : medicationList) {
            boolean isDueToday = false;
            
            if (m.getSpecificDate() != null) {
                if (m.getSpecificDate().equals(dateToday)) {
                    isDueToday = true;
                }
            } else if (m.getDays() != null) {
                if (m.getDays().contains("Everyday") || m.getDays().contains(dayOfWeek)) {
                    isDueToday = true;
                }
            }

            if (isDueToday) {
                totalToday++;
                if (m.isTaken()) takenToday++;
            }
        }

        int remaining = totalToday - takenToday;
        int progress = (totalToday > 0) ? (takenToday * 100 / totalToday) : 0;

        progressBar.setProgress(progress);
        progressPercent.setText(progress + "%");
        takenText.setText(takenToday + " Taken");
        remainingText.setText(remaining + " Remaining");
        totalCount.setText(String.valueOf(totalToday));
        
        TextView summaryTitle = findViewById(R.id.summary_title);
        if (summaryTitle != null) {
            summaryTitle.setText("Today's Progress");
        }
    }

    private void onMarkTaken(MedicationDto medication) {
        FirestoreRepository repo = new FirestoreRepository();
        Map<String, Object> updates = new HashMap<>();
        updates.put("taken", true);
        repo.updateMedication(medication.getId(), updates).addOnSuccessListener(v -> loadData());
    }

    private void showMedicationOptionsMenu(View view, MedicationDto medication) {
        PopupMenu popup = new PopupMenu(this, view);
        if (medication.isTaken()) {
            popup.getMenu().add("Unmark");
        }
        popup.getMenu().add("Edit");
        popup.getMenu().add("Delete");

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            FirestoreRepository repo = new FirestoreRepository();
            if ("Unmark".equals(title)) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("taken", false);
                repo.updateMedication(medication.getId(), updates).addOnSuccessListener(v -> loadData());
                return true;
            } else if ("Edit".equals(title)) {
                Intent intent = new Intent(this, AddMedicationActivity.class);
                intent.putExtra("med_id", medication.getId());
                startActivity(intent);
                return true;
            } else if ("Delete".equals(title)) {
                repo.medicationsCol.document(medication.getId()).delete().addOnSuccessListener(v -> {
                    loadData();
                    Toast.makeText(MedicationDetailActivity.this, "Medication deleted", Toast.LENGTH_SHORT).show();
                });
                return true;
            }
            return false;
        });
        popup.show();
    }

    private String resolveDayHeader(String dayName) {
        if ("Everyday".equalsIgnoreCase(dayName)) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
            return "Everyday - " + sdf.format(new Date());
        }

        Calendar now = Calendar.getInstance();
        SimpleDateFormat dayOfWeekFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        String todayName = dayOfWeekFormat.format(now.getTime());

        if (dayName.equalsIgnoreCase(todayName)) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
            return "Today - " + sdf.format(now.getTime());
        }

        // Find the date for this day name in the current week
        Calendar cal = (Calendar) now.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Scan the current week to find the date for dayName
        cal.add(Calendar.DAY_OF_YEAR, -3);
        for (int i = 0; i < 7; i++) {
            if (dayOfWeekFormat.format(cal.getTime()).equalsIgnoreCase(dayName)) {
                SimpleDateFormat sdf = new SimpleDateFormat("EEEE - MMM d", Locale.getDefault());
                return sdf.format(cal.getTime());
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        return dayName;
    }
}
