package com.rudra.myassistant.voice;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;
import java.util.UUID;

public class TTSHandler implements TextToSpeech.OnInitListener {
    private static final String TAG = "TTSHandler";
    private TextToSpeech tts;
    private final Context context;
    private boolean isInitialized = false;
    private final TTSCallback callback;
    
    public interface TTSCallback {
        void onTTSCompleted();
    }

    public TTSHandler(Context context, TTSCallback callback) {
        this.context = context;
        this.callback = callback;
        tts = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported or missing data");
            } else {
                isInitialized = true;
                
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        Log.d(TAG, "TTS onStart");
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        Log.d(TAG, "TTS onDone");
                        if (callback != null) {
                            callback.onTTSCompleted();
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.e(TAG, "TTS onError");
                        if (callback != null) {
                            callback.onTTSCompleted(); // Resume even on error
                        }
                    }
                });
            }
        } else {
            Log.e(TAG, "TTS Initialization failed");
        }
    }

    public void speak(String text) {
        if (isInitialized && text != null && !text.isEmpty()) {
            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UUID.randomUUID().toString());
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, params.getString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID));
        } else if (callback != null) {
            callback.onTTSCompleted(); // If TTS can't play, resume WakeWord immediately
        }
    }

    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    public void destroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
