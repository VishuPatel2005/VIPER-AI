package com.rudra.myassistant.voice;

import android.content.Context;
import android.util.Log;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.SpeechService;
import org.vosk.android.RecognitionListener;
import org.vosk.android.StorageService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;


public class WakeWordManager implements RecognitionListener {
    private static final String TAG = "WakeWordManager";
    private final Context context;
    private final WakeWordCallback wakeWordCallback;
    
    private Model model;
    private SpeechService speechService;
    private boolean isModelUnpacking = false;
    private boolean isModelReady = false;
    private boolean shouldBeListening = false;

    // Detection Phrases
    private static final List<String> WAKE_WORDS = Arrays.asList("viper", "hey viper");
    private long lastDetectionTime = 0;
    private static final long DETECTION_COOLDOWN_MS = 2000; // 2 seconds between detections

    public interface WakeWordCallback {
        void onWakeWordDetected();
        void onError(String error);
    }

    public WakeWordManager(Context context, WakeWordCallback callback) {
        this.context = context;
        this.wakeWordCallback = callback;
        initModel(); 
    }

    private void initModel() {
        if (isModelUnpacking || isModelReady) return;
        isModelUnpacking = true;
        Log.d(TAG, "Unpacking Vosk model...");
        StorageService.unpack(context, "model", "model",
                (m) -> {
                    this.model = m;
                    this.isModelReady = true;
                    this.isModelUnpacking = false;
                    Log.d(TAG, "Vosk model unpacked successfully.");
                    if (shouldBeListening) {
                        startListening();
                    }
                },
                (e) -> {
                    Log.e(TAG, "Failed to unpack the Vosk model: \n" + e.getMessage());
                    this.isModelUnpacking = false;
                    if (wakeWordCallback != null) wakeWordCallback.onError(e.getMessage());
                });
    }

    public void startListening() {
        shouldBeListening = true;
        
        if (!isModelReady) {
            if (!isModelUnpacking) {
                initModel();
            }
            Log.d(TAG, "Vosk model not ready yet. Queued to start listening once unpacked...");
            return; 
        }

        if (speechService != null) {
            // Already listening
            return;
        }

        try {
            // Grammar-Based Recognition: Restrict the search space to only our wake words.
            // This significantly improves sensitivity and reduces false negatives.
            StringBuilder grammar = new StringBuilder("[");
            for (int i = 0; i < WAKE_WORDS.size(); i++) {
                grammar.append("\"").append(WAKE_WORDS.get(i)).append("\"");
                if (i < WAKE_WORDS.size() - 1) grammar.append(",");
            }
            grammar.append(", \"[unk]\"]"); // [unk] allows the model to ignore non-matching noise

            Log.d(TAG, "Initializing Vosk Recognizer with grammar: " + grammar.toString());
            Recognizer rec = new Recognizer(model, 16000.0f, grammar.toString());
            
            speechService = new SpeechService(rec, 16000.0f);
            speechService.startListening(this);
            Log.d(TAG, "Vosk Wake Word (Grammar Mode) started successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error starting Vosk with grammar, falling back to general model", e);
            try {
                Recognizer rec = new Recognizer(model, 16000.0f);
                speechService = new SpeechService(rec, 16000.0f);
                speechService.startListening(this);
            } catch (IOException ex) {
                Log.e(TAG, "General model fallback also failed", ex);
                if (wakeWordCallback != null) wakeWordCallback.onError(ex.getMessage());
            }
        }
    }

    public void stopListening() {
        shouldBeListening = false;
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
            speechService = null;
            Log.d(TAG, "Vosk Wake Word stopped.");
        }
    }

    public void delete() {
        stopListening();
        if (model != null) {
            model.close();
            model = null;
            isModelReady = false;
            Log.d(TAG, "Vosk model closed.");
        }
    }

    // --- RecognitionListener Callbacks ---

    @Override
    public void onPartialResult(String hypothesis) {
        // Disabled partial triggering to reduce false positives.
        // We only trigger on high-confidence final results in onResult().
    }

    @Override
    public void onResult(String hypothesis) {
        if (checkWakeWord(hypothesis)) {
            Log.d(TAG, "Wake word detected (Result): " + hypothesis);
            stopListening();
            if (wakeWordCallback != null) wakeWordCallback.onWakeWordDetected();
        }
    }

    private boolean checkWakeWord(String hypothesis) {
        if (hypothesis == null || hypothesis.isEmpty()) return false;
        
        // Cooldown check
        if (System.currentTimeMillis() - lastDetectionTime < DETECTION_COOLDOWN_MS) {
            return false;
        }

        try {
            JSONObject json = new JSONObject(hypothesis);
            String text = "";
            if (json.has("text")) {
                text = json.getString("text").toLowerCase().trim();
            } else if (json.has("partial")) {
                text = json.getString("partial").toLowerCase().trim();
            }

            if (text.isEmpty() || text.equals("[unk]")) return false;

            for (String word : WAKE_WORDS) {
                // [BUG FIX] Use strict equality instead of 'find' with regex. 
                // Since we use a grammar-based recognizer, we should only trust exact matches 
                // to avoid background noise triggering the assistant.
                if (text.equals(word.toLowerCase().trim())) {
                    lastDetectionTime = System.currentTimeMillis();
                    return true;
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing Vosk JSON: " + hypothesis, e);
            String lower = hypothesis.toLowerCase();
            for (String word : WAKE_WORDS) {
                if (Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(lower).find()) {
                    lastDetectionTime = System.currentTimeMillis();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onFinalResult(String hypothesis) {
        // Ignored for continuous listening
    }

    @Override
    public void onError(Exception e) {
        Log.e(TAG, "Vosk SpeechService Error", e);
        stopListening();
        if (wakeWordCallback != null) wakeWordCallback.onError(e.getMessage());
    }

    @Override
    public void onTimeout() {
        Log.d(TAG, "Vosk SpeechService Timeout");
    }
}
