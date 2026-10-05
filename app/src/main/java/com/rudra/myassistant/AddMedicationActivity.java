package com.rudra.myassistant;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.UserSessionManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Calendar;

public class AddMedicationActivity extends BaseEdgeToEdgeActivity {

    private EditText editName, editDosage, editTime, editDate;
    private LinearLayout daysContainer;
    private String medicationId = null;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medication);

        editName = findViewById(R.id.edit_name);
        editDosage = findViewById(R.id.edit_dosage);
        editTime = findViewById(R.id.edit_time);
        editDate = findViewById(R.id.edit_date);
        daysContainer = findViewById(R.id.days_container);

        if (getIntent().hasExtra("med_id")) {
            medicationId = getIntent().getStringExtra("med_id");
            isEditMode = true;
            loadMedicationDetails();
            ((TextView)findViewById(R.id.title_text)).setText("Edit Medication");
            ((Button)findViewById(R.id.save_button)).setText("Update Medication");
        }
        
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        editTime.setOnClickListener(v -> showTimePicker());
        editDate.setOnClickListener(v -> showDatePicker());

        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> saveMedication());
    }

    private void loadMedicationDetails() {
        if (medicationId != null) {
            new FirestoreRepository().medicationsCol.document(medicationId).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    editName.setText(doc.getString("name"));
                    editDosage.setText(doc.getString("dosage"));
                    editTime.setText(doc.getString("time"));
                    List<String> days = (List<String>) doc.get("days");
                    if (days != null) {
                        for (int i = 0; i < daysContainer.getChildCount(); i++) {
                            CheckBox cb = (CheckBox) daysContainer.getChildAt(i);
                            if (days.contains(cb.getText().toString()) || days.contains("Everyday")) cb.setChecked(true);
                        }
                    }
                    if (doc.contains("specificDate")) {
                        editDate.setText(doc.getString("specificDate"));
                    }
                }
            });
        }
    }

    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minuteOfHour) -> {
                    String amPm = (hourOfDay >= 12) ? "PM" : "AM";
                    int hourDisplay = (hourOfDay > 12) ? hourOfDay - 12 : (hourOfDay == 0 ? 12 : hourOfDay);
                    String time = String.format(Locale.getDefault(), "%02d:%02d %s", hourDisplay, minuteOfHour, amPm);
                    editTime.setText(time);
                }, hour, minute, false);
        timePickerDialog.show();
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year1, monthOfYear + 1, dayOfMonth);
                    editDate.setText(date);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void saveMedication() {
        String name = editName.getText().toString().trim();
        String dosage = editDosage.getText().toString().trim();
        String time = editTime.getText().toString().trim();
        String specificDate = editDate.getText().toString().trim();

        if (name.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Please fill name and time", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> selectedDays = new ArrayList<>();
        for (int i = 0; i < daysContainer.getChildCount(); i++) {
            CheckBox cb = (CheckBox) daysContainer.getChildAt(i);
            if (cb.isChecked()) selectedDays.add(cb.getText().toString());
        }

        UserSessionManager sessionManager = new UserSessionManager(this);
        String userId = sessionManager.getUserId();
        FirestoreRepository repo = new FirestoreRepository();

        Map<String, Object> med = new HashMap<>();
        med.put("userId", userId);
        med.put("name", name);
        med.put("dosage", dosage.isEmpty() ? "Tablet" : dosage);
        med.put("time", time);
        med.put("days", selectedDays);
        if (!specificDate.isEmpty()) {
            med.put("specificDate", specificDate);
        }
        med.put("taken", false);

        if (isEditMode && medicationId != null) {
            repo.medicationsCol.document(medicationId).update(med).addOnSuccessListener(v -> {
                Toast.makeText(AddMedicationActivity.this, "Medication updated", Toast.LENGTH_SHORT).show();
                finish();
            });
        } else {
            repo.medicationsCol.add(med).addOnSuccessListener(v -> {
                Toast.makeText(AddMedicationActivity.this, "Medication added", Toast.LENGTH_SHORT).show();
                finish();
            });
        }
    }
}
