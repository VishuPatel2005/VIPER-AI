package com.rudra.myassistant.voice;

import android.content.Context;

public interface VoiceActionHandler {
    /**
     * Tries to handle the prompt or complete a pending action.
     * @param prompt The spoken text from the user.
     * @param stateManager The state manager for tracking multi-turn chats.
     * @param callback Handles communicating back via UI + Voice.
     * @return true if the handler can process this prompt, false otherwise.
     */
    boolean handleAction(String prompt, ConversationStateManager stateManager, VoiceCallback callback);

    interface VoiceCallback {
        void respondWith(String textToSpeak);
        void askFollowUp(String questionToSpeak, ConversationStateManager.PendingAction actionToWaitOn);
        Context getContext();
        void refreshPage();
    }
}
