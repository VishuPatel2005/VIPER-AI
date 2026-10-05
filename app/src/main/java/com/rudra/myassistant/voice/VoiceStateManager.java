package com.rudra.myassistant.voice;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the global UI state of the Voice Assistant.
 * Decouples the UI components (like MainActivity and ChatActivity) from the background
 * voice processing logic. It ensures state synchronization across all registered activities.
 */
public class VoiceStateManager {
    private static VoiceStateManager instance;

    public enum VoiceState {
        IDLE,           // Waiting for wake word
        LISTENING,      // Mic is active, waiting for speech
        TRANSCRIBING,   // Capturing live speech
        PROCESSING,     // Speech recognized, waiting for AI response
        RESPONDING,     // AI is speaking
        ERROR           // Something went wrong
    }

    private VoiceState currentState = VoiceState.IDLE;
    private String currentPartialText = "";
    
    // Observers to notify when state changes
    private final List<VoiceStateObserver> observers = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface VoiceStateObserver {
        void onStateChanged(VoiceState state);
        void onPartialTranscriptionUpdated(String text);
        void onFinalTranscription(String text);
        void onAssistantResponse(String text);
    }

    public static synchronized VoiceStateManager getInstance() {
        if (instance == null) {
            instance = new VoiceStateManager();
        }
        return instance;
    }

    public void addObserver(VoiceStateObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
            // Push current state immediately so the new observer syncs up
            observer.onStateChanged(currentState);
            if (!currentPartialText.isEmpty()) {
                observer.onPartialTranscriptionUpdated(currentPartialText);
            }
        }
    }

    public void removeObserver(VoiceStateObserver observer) {
        observers.remove(observer);
    }

    public VoiceState getCurrentState() {
        return currentState;
    }

    public void updateState(VoiceState newState) {
        this.currentState = newState;
        if (newState != VoiceState.LISTENING) {
            this.currentPartialText = ""; // clear when not listening
        }
        mainHandler.post(() -> {
            for (VoiceStateObserver observer : new ArrayList<>(observers)) {
                observer.onStateChanged(newState);
            }
        });
    }

    public void updatePartialTranscription(String text) {
        this.currentPartialText = text;
        mainHandler.post(() -> {
            if (currentState == VoiceState.LISTENING) {
                this.currentState = VoiceState.TRANSCRIBING;
                for (VoiceStateObserver observer : new ArrayList<>(observers)) {
                    observer.onStateChanged(currentState);
                }
            }
            for (VoiceStateObserver observer : new ArrayList<>(observers)) {
                observer.onPartialTranscriptionUpdated(text);
            }
        });
    }

    public void notifyFinalTranscription(String text) {
        this.currentPartialText = "";
        mainHandler.post(() -> {
            for (VoiceStateObserver observer : new ArrayList<>(observers)) {
                observer.onFinalTranscription(text);
            }
        });
    }

    public void notifyAssistantResponse(String text) {
        mainHandler.post(() -> {
            for (VoiceStateObserver observer : new ArrayList<>(observers)) {
                observer.onAssistantResponse(text);
            }
        });
    }
}
