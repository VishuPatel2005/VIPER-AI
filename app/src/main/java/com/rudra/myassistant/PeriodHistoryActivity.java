package com.rudra.myassistant;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.UserSessionManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PeriodHistoryActivity extends BaseEdgeToEdgeActivity {

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_period_history);

        ImageView backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        recyclerView = findViewById(R.id.history_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadHistory();
    }

    private void loadHistory() {
        UserSessionManager sessionManager = new UserSessionManager(this);
        String userId = sessionManager.getUserId();
        FirestoreRepository repo = new FirestoreRepository();
        
        // Fetch from user doc for cycles AND logs for symptoms/spotting
        repo.getUser(userId).addOnSuccessListener(doc -> {
            if (doc.exists() && doc.contains("cycles")) {
                List<Map<String, String>> cycles = (List<Map<String, String>>) doc.get("cycles");
                repo.db.collection("period_logs")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        Map<String, Map<String, Object>> logs = new HashMap<>();
                        for (com.google.firebase.firestore.DocumentSnapshot logDoc : snapshot.getDocuments()) {
                            logs.put(logDoc.getString("date"), logDoc.getData());
                        }
                        
                        if (cycles != null) {
                            PeriodHistoryAdapter adapter = new PeriodHistoryAdapter(cycles, logs);
                            recyclerView.setAdapter(adapter);
                        }
                    });
            }
        });
    }
}
