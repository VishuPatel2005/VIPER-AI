package com.rudra.myassistant.voice;

import android.util.Log;

import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.UserSessionManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HealthVoiceHandler implements VoiceActionHandler {
    private static final String TAG = "HealthVoiceHandler";
    private final FirestoreRepository firestoreRepository;
    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public HealthVoiceHandler() {
        this.firestoreRepository = new FirestoreRepository();
    }

    @Override
    public boolean handleAction(String prompt, ConversationStateManager stateManager, VoiceCallback callback) {
        String lowerPrompt = prompt.toLowerCase();

        // Very basic health handling for logging periods
        if (lowerPrompt.contains("log period") || lowerPrompt.contains("started my period") || lowerPrompt.contains("record period")) {
            UserSessionManager sessionManager = new UserSessionManager(callback.getContext());
            String userId = sessionManager.getUserId();
            String dateStr = df.format(new Date());
            Map<String, Object> log = new HashMap<>();
            log.put("flow", "Medium"); // Default to Medium for voice log
            log.put("date", dateStr);
            
            firestoreRepository.logPeriodData(userId, log).addOnSuccessListener(v -> {
                callback.respondWith("I've logged your period for today.");
                callback.refreshPage();
                stateManager.clear();
            }).addOnFailureListener(e -> {
                callback.respondWith("I'm sorry, but there was an issue logging your period.");
                stateManager.clear();
            });
            return true;
        }

        return false;
    }
}
