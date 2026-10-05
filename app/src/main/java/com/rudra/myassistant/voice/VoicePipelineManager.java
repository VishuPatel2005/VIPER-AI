package com.rudra.myassistant.voice;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import com.rudra.myassistant.ChatActivity;
import com.rudra.myassistant.MyAssistantApplication;

/**
 * VoicePipelineManager synchronizes WakeWord activation directly into SpeechRecognition.
 * Upon successful capture, it delegates payload routing directly to ChatActivity 
 * to utilize the established OpenAI processing endpoint securely.
 */
public class VoicePipelineManager implements
        WakeWordManager.WakeWordCallback,
        SpeechRecognitionHandler.SpeechCallback,
        TTSHandler.TTSCallback {

    private static final String TAG = "VoicePipeline";

    // Singleton Instance
    private static VoicePipelineManager instance;

    private final Context context;
    private final Handler mainHandler;

    private WakeWordManager wakeWordManager;
    private SpeechRecognitionHandler speechRecognitionHandler;
    private TTSHandler ttsHandler;
    private final VoiceStateManager uiStateManager;
    
    private boolean isWaitingForGreetingCompletion = false;
    private boolean hasGreeted = false;
    private boolean isContinuousMode = false;

    public static synchronized VoicePipelineManager getInstance(Context context) {
        if (instance == null) {
            instance = new VoicePipelineManager(context.getApplicationContext());
        }
        return instance;
    }

    private VoicePipelineManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.uiStateManager = VoiceStateManager.getInstance();
        
        this.speechRecognitionHandler = new SpeechRecognitionHandler(context, this);
        
        try {
            this.wakeWordManager = new WakeWordManager(context, this);
        } catch (Throwable t) {
            Log.e(TAG, "CRITICAL ERROR: Failed to initialize WakeWordManager (Vosk). Voice activation will be disabled.", t);
            this.wakeWordManager = null;
        }
        
        // Pass 'this' as the callback to handle the greeting sequence
        this.ttsHandler = new TTSHandler(context, this);
    }

    public void start() {
        uiStateManager.updateState(VoiceStateManager.VoiceState.IDLE);
        if (wakeWordManager != null) {
            wakeWordManager.startListening();
        }
        Log.d(TAG, "VoicePipelineManager started.");
    }

    public void stop() {
        if (wakeWordManager != null) wakeWordManager.stopListening();
        if (speechRecognitionHandler != null) speechRecognitionHandler.stopListening();
        if (ttsHandler != null) ttsHandler.stop();
        hasGreeted = false; // Reset greeting for next session
        uiStateManager.updateState(VoiceStateManager.VoiceState.IDLE);
    }

    public void destroy() {
        if (wakeWordManager != null) wakeWordManager.delete();
        if (speechRecognitionHandler != null) speechRecognitionHandler.destroy();
        if (ttsHandler != null) ttsHandler.destroy();
        instance = null;
    }

    // Explicitly triggers STT regardless of current context
    public void startSpeechRecognition() {
        mainHandler.post(() -> {
            if (speechRecognitionHandler != null) {
                speechRecognitionHandler.startListening();
            }
        });
    }

    // Handles speaking text aloud
    public void speak(String text) {
        if (ttsHandler != null) ttsHandler.speak(text);
    }

    @Override
    public void onWakeWordDetected() {
        Log.d(TAG, "Wake word triggered.");
        
        // Ensure standard speech recognizer is NOT active during setup
        if (speechRecognitionHandler != null) {
            speechRecognitionHandler.stopListening();
        }
        if (wakeWordManager != null) {
            wakeWordManager.stopListening();
        }
        
        // Mark that we are in a voice conversation for seamless follow-ups
        isContinuousMode = true;
        
        // 1. Enter RESPONDING state for the greeting if it's the first time
        uiStateManager.updateState(VoiceStateManager.VoiceState.RESPONDING);
        
        CurrentPageTracker tracker = ((MyAssistantApplication) context).getCurrentPageTracker();
        Activity currentActivity = tracker.getCurrentActivity();

        // 2. Launch ChatActivity if not already in foreground
        Intent intent = new Intent(context, ChatActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("start_voice_listening", true);
        
        if (currentActivity != null) {
            Log.d(TAG, "Launching ChatActivity from " + currentActivity.getLocalClassName());
            currentActivity.startActivity(intent);
        } else {
            Log.d(TAG, "Launching ChatActivity from application context");
            context.startActivity(intent);
        }
        
        // 3. Trigger the spoken greeting ONLY if it's the first time this session
        if (!hasGreeted) {
            isWaitingForGreetingCompletion = true;
            hasGreeted = true;
            mainHandler.postDelayed(() -> {
                speak("Hey, What can I do for you?");
            }, 500); // Small delay to let UI settle
        } else {
            // If already greeted, just start listening immediately
            Log.d(TAG, "Already greeted. Skipping to listening.");
            mainHandler.postDelayed(() -> {
                uiStateManager.updateState(VoiceStateManager.VoiceState.LISTENING);
                startSpeechRecognition();
            }, 500);
        }
    }
    
    // --- TTS Callback ---
    @Override
    public void onTTSCompleted() {
        if (isWaitingForGreetingCompletion) {
            isWaitingForGreetingCompletion = false;
            Log.d(TAG, "Greeting completed. Starting listening mode.");
            
            uiStateManager.updateState(VoiceStateManager.VoiceState.LISTENING);
            startSpeechRecognition();
        } else if (isContinuousMode) {
            // Seamless flow: after the assistant finishes speaking its reply, 
            // automatically go back to listening for the next user command.
            Log.d(TAG, "Response completed. Continuous mode active, re-listening.");
            uiStateManager.updateState(VoiceStateManager.VoiceState.LISTENING);
            startSpeechRecognition();
        } else {
            // Normal response finished (e.g. from manual text input), go back to idle
            uiStateManager.updateState(VoiceStateManager.VoiceState.IDLE);
            // Interaction finished, safely resume wake word monitoring
            if (wakeWordManager != null) {
                mainHandler.postDelayed(() -> wakeWordManager.startListening(), 500);
            }
        }
    }

    @Override
    public void onError(String error) {
        Log.e(TAG, "WakeWord Error: " + error);
        if (uiStateManager.getCurrentState() == VoiceStateManager.VoiceState.IDLE) {
            if (wakeWordManager != null) {
                mainHandler.postDelayed(() -> wakeWordManager.startListening(), 2000);
            }
        }
    }

    // --- Speech Recognition Callbacks ---
    @Override
    public void onSpeechPartialResult(String text) {
        uiStateManager.updatePartialTranscription(text);
    }

    @Override
    public void onSpeechRecognized(String text) {
        Log.d(TAG, "onSpeechRecognized: " + text);
        uiStateManager.updateState(VoiceStateManager.VoiceState.PROCESSING);
        
        // This broadcasts the recognized speech directly to ChatActivity's UI Controller!
        uiStateManager.notifyFinalTranscription(text);
        
        // DO NOT start wake word here. Wait for TTS to complete or timeout 
        // to avoid microphone hardware conflicts between Vosk and SpeechRecognizer.
    }

    @Override
    public void onSpeechError(String error) {
        Log.e(TAG, "onSpeechError: " + error);
        uiStateManager.updateState(VoiceStateManager.VoiceState.ERROR);

        mainHandler.postDelayed(() -> {
            uiStateManager.updateState(VoiceStateManager.VoiceState.IDLE);
            if (wakeWordManager != null) {
                wakeWordManager.startListening();
            }
        }, 2000);
    }

    @Override
    public void onSpeechTimeout() {
        Log.d(TAG, "onSpeechTimeout: Flow aborted.");
        isContinuousMode = false; // Stop auto-re-listening on timeout
        uiStateManager.notifyFinalTranscription("");
        uiStateManager.updateState(VoiceStateManager.VoiceState.IDLE);
        if (wakeWordManager != null) {
            mainHandler.postDelayed(() -> wakeWordManager.startListening(), 500);
        }
    }
}
