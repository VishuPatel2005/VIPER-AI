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

public class EditReminderActivity extends BaseEdgeToEdgeActivity {

    private static final String DATE_PICKER_TAG = "DATE_PICKER";
    private static final String TIME_PICKER_TAG = "TIME_PICKER";
    private SwitchCompat dateSwitch;
    private TextView dateText;
    private SwitchCompat timeSwitch;
    private TextView timeText;
    private SwitchCompat urgentSwitch;
    private EditText titleEditText;
    private EditText notesEditText;

    private Long selectedDateTimestamp;
    private Integer selectedHour;
    private Integer selectedMinute;
    private String backendId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_reminder);

        // Update title to "Edit reminder"
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        if (toolbarTitle != null) {
            toolbarTitle.setText("Edit Reminder");
        }

        backendId = getIntent().getStringExtra("reminder_backend_id");
        String title = getIntent().getStringExtra("reminder_title");
        boolean isFlagged = getIntent().getBooleanExtra("reminder_flagged", false);
        boolean isUrgent = getIntent().getBooleanExtra("reminder_urgent", false);
        long dateTimestamp = getIntent().getLongExtra("reminder_date", -1);
        int hour = getIntent().getIntExtra("reminder_hour", -1);
        int minute = getIntent().getIntExtra("reminder_minute", -1);

        ImageView closeButton = findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> finish());

        ImageView doneButton = findViewById(R.id.done_button);
        doneButton.setOnClickListener(v -> updateReminder());

        dateSwitch = findViewById(R.id.date_switch);
        dateText = findViewById(R.id.date_text);
        timeSwitch = findViewById(R.id.time_switch);
        timeText = findViewById(R.id.time_text);
        urgentSwitch = findViewById(R.id.urgent_switch);
        titleEditText = findViewById(R.id.title_edit_text);
        notesEditText = findViewById(R.id.notes_edit_text);
        
        // Pre-fill existing data
        titleEditText.setText(title != null ? title : "");
        urgentSwitch.setChecked(isUrgent);
        
        if (dateTimestamp > 0) {
            selectedDateTimestamp = dateTimestamp;
            dateSwitch.setChecked(true);
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            dateText.setText(sdf.format(new Date(selectedDateTimestamp)));
            dateText.setVisibility(View.VISIBLE);
        } else {
            selectedDateTimestamp = null;
        }
        
        if (hour >= 0 && minute >= 0) {
            selectedHour = hour;
            selectedMinute = minute;
            timeSwitch.setChecked(true);
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
        } else {
            selectedHour = null;
            selectedMinute = null;
        }

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
                    long selectionTimestamp = MaterialDatePicker.todayInUtcMilliseconds();
                    if (selectedDateTimestamp != null) {
                        // Convert local timestamp to UTC for MaterialDatePicker
                        Calendar localCalendar = Calendar.getInstance();
                        localCalendar.setTimeInMillis(selectedDateTimestamp);
                        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                        utcCalendar.set(Calendar.YEAR, localCalendar.get(Calendar.YEAR));
                        utcCalendar.set(Calendar.MONTH, localCalendar.get(Calendar.MONTH));
                        utcCalendar.set(Calendar.DAY_OF_MONTH, localCalendar.get(Calendar.DAY_OF_MONTH));
                        utcCalendar.set(Calendar.HOUR_OF_DAY, 0);
                        utcCalendar.set(Calendar.MINUTE, 0);
                        utcCalendar.set(Calendar.SECOND, 0);
                        utcCalendar.set(Calendar.MILLISECOND, 0);
                        selectionTimestamp = utcCalendar.getTimeInMillis();
                    }
                    MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                            .setTitleText("Select date")
                            .setSelection(selectionTimestamp)
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
                    if (selectedHour != null && selectedMinute != null) {
                        calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                        calendar.set(Calendar.MINUTE, selectedMinute);
                    }
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
    }

    private void updateReminder() {
        if (backendId == null || backendId.isEmpty()) {
            Toast.makeText(this, "Cannot edit this reminder", Toast.LENGTH_SHORT).show();
            return;
        }
        String title = titleEditText.getText().toString().trim();
        if (title.isEmpty()) {
            finish();
            return;
        }
        String datetime = buildDatetimeIso();
        boolean isFlagged = urgentSwitch.isChecked();
        String notes = notesEditText.getText().toString().trim();
        
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("text", title);
        updates.put("datetime", datetime);
        updates.put("notes", notes);
        updates.put("flagged", isFlagged);
        
        FirestoreRepository repository = new FirestoreRepository();
        repository.updateReminder(backendId, updates).addOnSuccessListener(doc -> {
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(EditReminderActivity.this, "Failed to update reminder", Toast.LENGTH_SHORT).show();
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
