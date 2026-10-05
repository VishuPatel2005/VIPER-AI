package com.rudra.myassistant.voice;

import android.util.Log;
import com.rudra.myassistant.network.AIService;
import com.google.gson.JsonObject;

public class ChatVoiceHandler implements VoiceActionHandler {
    private static final String TAG = "ChatVoiceHandler";
    private final AIService aiService;

    public ChatVoiceHandler() {
        this.aiService = new AIService();
    }

    @Override
    public boolean handleAction(String prompt, ConversationStateManager stateManager, VoiceCallback callback) {
        Log.d(TAG, "Routing to Chat Handler: " + prompt);
        
        aiService.askAssistant(prompt, null, new AIService.AIResponseListener() {
            @Override
            public void onSuccess(String response) {
                callback.respondWith(response);
                callback.refreshPage();
            }

            @Override
            public void onToolCall(String functionName, JsonObject arguments) {
                // Global voice mode just acknowledges the intent for now
                callback.respondWith("I can handle that for you in the chat. Please open the app for complex tasks.");
            }

            @Override
            public void onFailure(String error) {
                callback.respondWith("Sorry, I'm having trouble reaching the AI right now.");
            }
        });
        
        return true;
    }
}
