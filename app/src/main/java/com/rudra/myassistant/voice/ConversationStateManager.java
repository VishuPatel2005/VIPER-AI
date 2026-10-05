package com.rudra.myassistant.voice;

import java.util.HashMap;
import java.util.Map;

public class ConversationStateManager {

    public enum PendingAction {
        NONE,
        ADD_REMINDER_WAITING_FOR_TITLE,
        ADD_REMINDER_WAITING_FOR_TIME,
        ADD_NOTE_WAITING_FOR_CONTENT,
        ADD_NOTE_WAITING_FOR_TITLE,
        ADD_MEDICATION_WAITING_FOR_NAME,
        LOG_PERIOD_WAITING_FOR_DATE,
        // Add more states as needed
    }

    private PendingAction currentPendingAction = PendingAction.NONE;
    private final Map<String, Object> sessionData = new HashMap<>();

    public PendingAction getPendingAction() {
        return currentPendingAction;
    }

    public void setPendingAction(PendingAction action) {
        this.currentPendingAction = action;
    }

    public void putSessionData(String key, Object value) {
        sessionData.put(key, value);
    }

    public Object getSessionData(String key) {
        return sessionData.get(key);
    }

    public void clear() {
        currentPendingAction = PendingAction.NONE;
        sessionData.clear();
    }
}
