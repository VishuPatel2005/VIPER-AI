package com.rudra.myassistant.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

public class SpeechRecognitionHandler {
    private static final String TAG = "SpeechRecognition";
    private final Context context;
    private SpeechRecognizer speechRecognizer;
    private final SpeechCallback callback;
    private boolean isListening = false;
    private Handler mainHandler;

    public interface SpeechCallback {
        void onSpeechRecognized(String text);
        void onSpeechPartialResult(String text);
        void onSpeechError(String error);
        void onSpeechTimeout();
    }

    public SpeechRecognitionHandler(Context context, SpeechCallback callback) {
        this.context = context;
        this.callback = callback;
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        mainHandler.post(this::initSpeechRecognizer);
    }

    private void initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "Ready for speech");
                }
                
                @Override
                public void onBeginningOfSpeech() {
                    Log.d(TAG, "Beginning of speech");
                }
                
                @Override
                public void onRmsChanged(float rmsdB) {
                }
                
                @Override
                public void onBufferReceived(byte[] buffer) {
                }
                
                @Override
                public void onEndOfSpeech() {
                    Log.d(TAG, "End of speech");
                    isListening = false;
                    broadcastListeningState(false);
                }
                
                @Override
                public void onError(int error) {
                    isListening = false;
                    broadcastListeningState(false);
                    String errorMessage = getErrorText(error);
                    Log.e(TAG, "Speech Error: " + errorMessage);
                    
                    if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH) {
                        if (callback != null) callback.onSpeechTimeout();
                    } else {
                        if (callback != null) callback.onSpeechError(errorMessage);
                    }
                }
                
                @Override
                public void onResults(Bundle results) {
                    isListening = false;
                    broadcastListeningState(false);
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        Log.d(TAG, "Recognized text: " + text);
                        if (callback != null) {
                            callback.onSpeechRecognized(text);
                        }
                    } else {
                        if (callback != null) callback.onSpeechTimeout();
                    }
                }
                
                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        if (callback != null) {
                            callback.onSpeechPartialResult(text);
                        }
                    }
                }
                
                @Override
                public void onEvent(int eventType, Bundle params) {
                }
            });
        } else {
            Log.e(TAG, "Speech recognition not available on this device");
        }
    }

    public static final String ACTION_LISTENING_START = "com.rudra.myassistant.ACTION_LISTENING_START";
    public static final String ACTION_LISTENING_STOP = "com.rudra.myassistant.ACTION_LISTENING_STOP";

    private void broadcastListeningState(boolean isListeningActive) {
        Intent intent = new Intent(isListeningActive ? ACTION_LISTENING_START : ACTION_LISTENING_STOP);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    public void startListening() {
        mainHandler.post(() -> {
            if (speechRecognizer != null && !isListening) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening...");
                intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                isListening = true;
                broadcastListeningState(true);
                speechRecognizer.startListening(intent);
            } else if (speechRecognizer == null) {
                Log.e(TAG, "SpeechRecognizer is null. Attempting to re-init.");
                initSpeechRecognizer();
            }
        });
    }

    public void stopListening() {
        mainHandler.post(() -> {
            if (speechRecognizer != null && isListening) {
                speechRecognizer.stopListening();
                isListening = false;
                broadcastListeningState(false);
            }
        });
    }

    public void destroy() {
        mainHandler.post(() -> {
            if (speechRecognizer != null) {
                speechRecognizer.destroy();
                speechRecognizer = null;
                broadcastListeningState(false);
            }
        });
    }

    private String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "Error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }
}
