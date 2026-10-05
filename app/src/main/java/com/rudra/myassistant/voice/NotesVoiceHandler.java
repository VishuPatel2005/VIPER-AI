package com.rudra.myassistant.voice;

import android.util.Log;

import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.UserSessionManager;

public class NotesVoiceHandler implements VoiceActionHandler {
    private static final String TAG = "NotesVoiceHandler";
    private final FirestoreRepository firestoreRepository;

    public NotesVoiceHandler() {
        this.firestoreRepository = new FirestoreRepository();
    }

    @Override
    public boolean handleAction(String prompt, ConversationStateManager stateManager, VoiceCallback callback) {
        String lowerPrompt = prompt.toLowerCase();

        // Handle Pending Title
        if (stateManager.getPendingAction() == ConversationStateManager.PendingAction.ADD_NOTE_WAITING_FOR_TITLE) {
            stateManager.putSessionData("note_title", prompt);
            callback.askFollowUp("What should the note say?", ConversationStateManager.PendingAction.ADD_NOTE_WAITING_FOR_CONTENT);
            return true;
        }

        // Handle Pending Content
        if (stateManager.getPendingAction() == ConversationStateManager.PendingAction.ADD_NOTE_WAITING_FOR_CONTENT) {
            String title = (String) stateManager.getSessionData("note_title");
            createNote(title, prompt, stateManager, callback);
            return true;
        }

        // Handle initial intent
        if (lowerPrompt.contains("add note") || lowerPrompt.contains("create a note") || lowerPrompt.contains("take a note")) {
            callback.askFollowUp("What is the title of the note?", ConversationStateManager.PendingAction.ADD_NOTE_WAITING_FOR_TITLE);
            return true;
        }

        return false;
    }

    private void createNote(String title, String content, ConversationStateManager stateManager, VoiceCallback callback) {
        UserSessionManager sessionManager = new UserSessionManager(callback.getContext());
        String userId = sessionManager.getUserId();
        
        firestoreRepository.addNote(userId, title, content).addOnSuccessListener(v -> {
            callback.respondWith("Your note has been saved.");
            callback.refreshPage();
            stateManager.clear();
        }).addOnFailureListener(e -> {
            callback.respondWith("I'm sorry, I couldn't save the note.");
            stateManager.clear();
        });
    }
}
