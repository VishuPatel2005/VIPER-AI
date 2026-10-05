package com.rudra.myassistant;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.UserSessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewReminderActivity extends BaseEdgeToEdgeActivity {

    private static final String DATE_PICKER_TAG = "DATE_PICKER";
    private static final String TIME_PICKER_TAG = "TIME_PICKER";
    private SwitchCompat dateSwitch;
    private TextView dateText;
    private SwitchCompat timeSwitch;
    private TextView timeText;
    private SwitchCompat urgentSwitch;
    private SwitchCompat locationSwitch;
    private EditText titleEditText;
    private EditText notesEditText;
    private EditText locationEditText;

    // Store selected date and time
    private Long selectedDateTimestamp;
    private Integer selectedHour;
    private Integer selectedMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_reminder);

        ImageView closeButton = findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> finish());

        ImageView doneButton = findViewById(R.id.done_button);
        doneButton.setOnClickListener(v -> createReminder());

        dateSwitch = findViewById(R.id.date_switch);
        dateText = findViewById(R.id.date_text);
        timeSwitch = findViewById(R.id.time_switch);
        timeText = findViewById(R.id.time_text);
        urgentSwitch = findViewById(R.id.urgent_switch);
        locationSwitch = findViewById(R.id.location_switch);
        titleEditText = findViewById(R.id.title_edit_text);
        notesEditText = findViewById(R.id.notes_edit_text);
        locationEditText = findViewById(R.id.location_edit_text);
        
        selectedDateTimestamp = null;
        selectedHour = null;
        selectedMinute = null;

        // If the date picker is already showing (e.g., after rotation), re-attach the listeners
        if (getSupportFragmentManager().findFragmentByTag(DATE_PICKER_TAG) != null) {
            MaterialDatePicker<Long> datePicker = (MaterialDatePicker<Long>) getSupportFragmentManager().findFragmentByTag(DATE_PICKER_TAG);
            addDatePickerListeners(datePicker);
        }

        // If the time picker is already showing (e.g., after rotation), re-attach the listeners
        if (getSupportFragmentManager().findFragmentByTag(TIME_PICKER_TAG) != null) {
            MaterialTimePicker timePicker = (MaterialTimePicker) getSupportFragmentManager().findFragmentByTag(TIME_PICKER_TAG);
            addTimePickerListeners(timePicker);
        }

        dateSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // To prevent crash on toggling multiple times
                if (getSupportFragmentManager().findFragmentByTag(DATE_PICKER_TAG) == null) {
                    MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                            .setTitleText("Select date")
                            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                            .build();

                    addDatePickerListeners(datePicker);
                    datePicker.show(getSupportFragmentManager(), DATE_PICKER_TAG);
                }
            } else {
                dateText.setVisibility(View.GONE);
                selectedDateTimestamp = null;
                // Dismiss the picker if it's showing
                MaterialDatePicker<?> picker = (MaterialDatePicker<?>) getSupportFragmentManager().findFragmentByTag(DATE_PICKER_TAG);
                if (picker != null) {
                    // This is safe to call even if the picker is not showing.
                    picker.dismiss();
                }
            }
        });

        timeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (getSupportFragmentManager().findFragmentByTag(TIME_PICKER_TAG) == null) {
                    Calendar calendar = Calendar.getInstance();
                    MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                            .setTimeFormat(TimeFormat.CLOCK_12H)
                            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                            .setMinute(calendar.get(Calendar.MINUTE))
                            .setTitleText("Select time")
                            .build();

                    addTimePickerListeners(timePicker);
                    timePicker.show(getSupportFragmentManager(), TIME_PICKER_TAG);
                }
            } else {
                timeText.setVisibility(View.GONE);
                selectedHour = null;
                selectedMinute = null;
                MaterialTimePicker picker = (MaterialTimePicker) getSupportFragmentManager().findFragmentByTag(TIME_PICKER_TAG);
                if (picker != null) {
                    picker.dismiss();
                }
            }
        });

        locationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            locationEditText.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) {
                locationEditText.requestFocus();
            } else {
                locationEditText.setText("");
            }
        });
    }

    private void createReminder() {
        String title = titleEditText.getText().toString().trim();
        if (title.isEmpty()) {
            finish();
            return;
        }

        String datetime = buildDatetimeIso();
        UserSessionManager sessionManager = new UserSessionManager(this);
        String userId = sessionManager.getUserId();
        boolean isUrgent = urgentSwitch.isChecked();
        String notes = notesEditText.getText().toString().trim();
        String locationName = locationSwitch.isChecked() ? locationEditText.getText().toString().trim() : null;
        if (locationName != null && locationName.isEmpty()) locationName = null;
        
        FirestoreRepository repository = new FirestoreRepository();
        repository.addReminder(userId, title, datetime, isUrgent, locationName, null, null, 100.0f, true, false).addOnSuccessListener(doc -> {
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(NewReminderActivity.this, "Failed to create reminder", Toast.LENGTH_SHORT).show();
        });
    }

    private String buildDatetimeIso() {
        Calendar cal = Calendar.getInstance();
        if (selectedDateTimestamp != null) {
            cal.setTimeInMillis(selectedDateTimestamp);
        }
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = (selectedHour != null) ? selectedHour : 0;
        int minute = (selectedMinute != null) ? selectedMinute : 0;
        return String.format(Locale.US, "%04d-%02d-%02dT%02d:%02d:00", year, month + 1, day, hour, minute);
    }

    private void addDatePickerListeners(@NonNull MaterialDatePicker<Long> datePicker) {
        // Clear listeners to prevent duplicates after configuration changes
        datePicker.clearOnPositiveButtonClickListeners();
        datePicker.clearOnNegativeButtonClickListeners();
        datePicker.clearOnCancelListeners();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // MaterialDatePicker returns midnight UTC for the selected date
            // Convert to local time and get date components
            Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utcCalendar.setTimeInMillis(selection);
            
            int year = utcCalendar.get(Calendar.YEAR);
            int month = utcCalendar.get(Calendar.MONTH);
            int day = utcCalendar.get(Calendar.DAY_OF_MONTH);
            
            // Create calendar in local timezone with selected date at midnight
            Calendar localCalendar = Calendar.getInstance();
            localCalendar.set(year, month, day, 0, 0, 0);
            localCalendar.set(Calendar.MILLISECOND, 0);
            
            selectedDateTimestamp = localCalendar.getTimeInMillis();
            
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            dateText.setText(sdf.format(new Date(selectedDateTimestamp)));
            dateText.setVisibility(View.VISIBLE);
        });

        datePicker.addOnNegativeButtonClickListener(v -> {
            dateSwitch.setChecked(false);
            selectedDateTimestamp = null;
        });

        datePicker.addOnCancelListener(dialog -> {
            dateSwitch.setChecked(false);
            selectedDateTimestamp = null;
        });
    }

    private void addTimePickerListeners(@NonNull MaterialTimePicker timePicker) {
        timePicker.clearOnPositiveButtonClickListeners();
        timePicker.clearOnNegativeButtonClickListeners();
        timePicker.clearOnCancelListeners();

        timePicker.addOnPositiveButtonClickListener(v -> {
            selectedHour = timePicker.getHour();
            selectedMinute = timePicker.getMinute();
            
            // Display time in 12-hour format
            int displayHour = selectedHour;
            String amPm = displayHour < 12 ? "AM" : "PM";
            if (displayHour > 12) {
                displayHour -= 12;
            } else if (displayHour == 0) {
                displayHour = 12;
            }
            timeText.setText(String.format(Locale.getDefault(), "%d:%02d %s", displayHour, selectedMinute, amPm));
            timeText.setVisibility(View.VISIBLE);
        });

        timePicker.addOnNegativeButtonClickListener(v -> {
            timeSwitch.setChecked(false);
            selectedHour = null;
            selectedMinute = null;
        });

        timePicker.addOnCancelListener(dialog -> {
            timeSwitch.setChecked(false);
            selectedHour = null;
            selectedMinute = null;
        });
    }
}
