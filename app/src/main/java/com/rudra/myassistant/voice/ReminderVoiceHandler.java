package com.rudra.myassistant.voice;

import android.util.Log;

import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.UserSessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ReminderVoiceHandler implements VoiceActionHandler {
    private static final String TAG = "ReminderVoiceHandler";
    private final FirestoreRepository firestoreRepository;
    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

    public ReminderVoiceHandler() {
        this.firestoreRepository = new FirestoreRepository();
    }

    @Override
    public boolean handleAction(String prompt, ConversationStateManager stateManager, VoiceCallback callback) {
        String lowerPrompt = prompt.toLowerCase();
        
        // Handle pending state
        if (stateManager.getPendingAction() == ConversationStateManager.PendingAction.ADD_REMINDER_WAITING_FOR_TITLE) {
            String title = prompt;
            stateManager.putSessionData("reminder_title", title);
            
            // Assume we create it for tomorrow by default if no time specified, or ask for time. Let's create it for tomorrow.
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, 1);
            String datetime = df.format(cal.getTime());
            
            createReminder(title, datetime, stateManager, callback);
            return true;
        }

        // Handle initial intent
        if (lowerPrompt.contains("add reminder") || lowerPrompt.contains("set a reminder") || lowerPrompt.contains("remind me")) {
            // Very simple NLP extract
            String title = prompt.replaceAll("(?i)(add reminder|set a reminder|remind me to|remind me)", "").trim();
            if (title.isEmpty() || title.equals("to")) {
                callback.askFollowUp("What should I remind you about?", ConversationStateManager.PendingAction.ADD_REMINDER_WAITING_FOR_TITLE);
            } else {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, 1); // Default to tomorrow
                String datetime = df.format(cal.getTime());
                createReminder(title, datetime, stateManager, callback);
            }
            return true;
        }

        return false;
    }

    private void createReminder(String text, String datetime, ConversationStateManager stateManager, VoiceCallback callback) {
        UserSessionManager sessionManager = new UserSessionManager(callback.getContext());
        String userId = sessionManager.getUserId();
        
        firestoreRepository.addReminder(userId, text, datetime, false, null, null, null, 100.0f, true, false).addOnSuccessListener(docRef -> {
            Log.d(TAG, "Reminder created in Firestore: " + docRef.getId());
            callback.respondWith("Okay, I added a reminder for " + text + ".");
            callback.refreshPage();
            stateManager.clear();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error creating reminder", e);
            callback.respondWith("I'm sorry, I couldn't add the reminder.");
            stateManager.clear();
        });
    }
}
