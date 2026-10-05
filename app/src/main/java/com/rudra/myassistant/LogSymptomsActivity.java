package com.rudra.myassistant;

import android.os.Bundle;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.UserSessionManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LogSymptomsActivity extends BaseEdgeToEdgeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_symptoms);

        TextView dateText = findViewById(R.id.current_date_text);
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault());
        dateText.setText("Today, " + sdf.format(new Date()));

        RecyclerView recyclerView = findViewById(R.id.symptoms_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<String> symptoms = Arrays.asList(
            "Abdominal Cramps", "Acne", "Appetite Changes", "Bladder Incontinence", 
            "Bloating", "Breast Pain", "Chills", "Constipation", "Diarrhea", 
            "Dizziness", "Fatigue", "Headache", "Hot Flashes", "Insomnia", 
            "Irritability", "Mood Swings", "Nausea", "Ovulation Pain", "Lower Back Pain"
        );

        SymptomsAdapter adapter = new SymptomsAdapter(symptoms);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.save_button).setOnClickListener(v -> logSymptoms(adapter.getSelectedSymptoms()));
    }

    private void logSymptoms(List<String> symptoms) {
        UserSessionManager sessionManager = new UserSessionManager(this);
        String userId = sessionManager.getUserId();
        FirestoreRepository repo = new FirestoreRepository();
        
        Map<String, Object> data = new HashMap<>();
        data.put("symptoms", symptoms);
        data.put("date", new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date()));
        
        repo.logPeriodData(userId, data).addOnSuccessListener(v -> {
            android.widget.Toast.makeText(this, "Symptoms logged", android.widget.Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
