package com.rudra.myassistant.voice;

import android.util.Log;

public class VoiceIntentRouter {
    private static final String TAG = "VoiceIntentRouter";

    private final ReminderVoiceHandler reminderHandler;
    private final NotesVoiceHandler notesHandler;
    private final HealthVoiceHandler healthHandler;
    private final ChatVoiceHandler chatHandler;

    public VoiceIntentRouter() {
        this.reminderHandler = new ReminderVoiceHandler();
        this.notesHandler = new NotesVoiceHandler();
        this.healthHandler = new HealthVoiceHandler();
        this.chatHandler = new ChatVoiceHandler();
    }

    public void routeIntent(String prompt, CurrentPageTracker.PageType currentPage, ConversationStateManager stateManager, VoiceActionHandler.VoiceCallback callback) {
        String lowerPrompt = prompt.toLowerCase();
        
        // 1. Check pending states first
        if (stateManager.getPendingAction() != ConversationStateManager.PendingAction.NONE) {
            String stateStr = stateManager.getPendingAction().name();
            if (stateStr.startsWith("ADD_REMINDER")) {
                reminderHandler.handleAction(prompt, stateManager, callback);
                return;
            } else if (stateStr.startsWith("ADD_NOTE")) {
                notesHandler.handleAction(prompt, stateManager, callback);
                return;
            } else if (stateStr.startsWith("ADD_MEDICATION") || stateStr.startsWith("LOG_PERIOD")) {
                healthHandler.handleAction(prompt, stateManager, callback);
                return;
            }
        }

        // 2. Check explicit keywords (override context)
        if (lowerPrompt.contains("remind") || lowerPrompt.contains("reminder")) {
            if (reminderHandler.handleAction(prompt, stateManager, callback)) return;
        } else if (lowerPrompt.contains("note")) {
            if (notesHandler.handleAction(prompt, stateManager, callback)) return;
        } else if (lowerPrompt.contains("period") || lowerPrompt.contains("medication") || lowerPrompt.contains("health")) {
            if (healthHandler.handleAction(prompt, stateManager, callback)) return;
        }

        // 3. Check Context (Current Page)
        switch (currentPage) {
            case REMINDER:
                if (reminderHandler.handleAction(prompt, stateManager, callback)) return;
                break;
            case NOTES:
                if (notesHandler.handleAction(prompt, stateManager, callback)) return;
                break;
            case HEALTH:
                if (healthHandler.handleAction(prompt, stateManager, callback)) return;
                break;
            default:
                break;
        }

        // 4. Fallback to general Chat
        Log.d(TAG, "No specific intent found. Using Chat fallback.");
        chatHandler.handleAction(prompt, stateManager, callback);
    }
}
