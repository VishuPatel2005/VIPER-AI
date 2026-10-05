package com.rudra.myassistant.voice;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.rudra.myassistant.ChatActivity;
import com.rudra.myassistant.R;

/**
 * Handles the Gemini-like bottom bar voice interaction architecture.
 * Responsible for managing UI state transitions, triggering animations, 
 * updating the chat input text live, and automatically routing the payload.
 */
public class VoiceUIController implements VoiceStateManager.VoiceStateObserver {

    private final Activity activity;
    private final ViewGroup bottomBar;
    private final EditText inputField;
    private final ImageView micButton;
    private final ImageView sendButton;
    private final View uploadButton;

    private AnimatorSet micGlowAnimator;
    private ValueAnimator waveformAnimator;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    


    public VoiceUIController(Activity activity, ViewGroup bottomBar, EditText inputField, ImageView micButton, ImageView sendButton, View uploadButton) {
        this.activity = activity;
        this.bottomBar = bottomBar;
        this.inputField = inputField;
        this.micButton = micButton;
        this.sendButton = sendButton;
        this.uploadButton = uploadButton;

        // Bind observer
        VoiceStateManager.getInstance().addObserver(this);
    }

    public void destroy() {
        VoiceStateManager.getInstance().removeObserver(this);
        stopAnimations();
    }

    // -- Voice State Observer Implementation --

    @Override
    public void onStateChanged(VoiceStateManager.VoiceState state) {
        activity.runOnUiThread(() -> {
            switch (state) {
                case IDLE:
                    transitionToIdle();
                    break;
                case LISTENING:
                    transitionToListening();
                    break;
                case TRANSCRIBING:
                    transitionToTranscribing();
                    break;
                case PROCESSING:
                    transitionToProcessing();
                    break;
                case RESPONDING:
                    transitionToResponding();
                    break;
                case ERROR:
                    transitionToIdle();
                    break;
            }
        });
    }

    @Override
    public void onPartialTranscriptionUpdated(String text) {
        activity.runOnUiThread(() -> {
            if (inputField != null) {
                inputField.setText(text);
                inputField.setSelection(text.length());
            }
        });
    }

    @Override
    public void onFinalTranscription(String text) {
        activity.runOnUiThread(() -> {
            if (inputField != null) {
                inputField.setText(text);
            }
            // Once transcription completes, automatically dispatch the message
            sendMessageToChatAndTriggerResponse(text);
        });
    }

    @Override
    public void onAssistantResponse(String text) {
        // UI responds to state changes; specific text logic is handled by ChatActivity
    }

    // -- Gemini-like State Transitions --

    private void transitionToIdle() {
        stopAnimations();
        if (inputField != null) {
            inputField.setText("");
            inputField.setHint("Ask your assistant...");
            inputField.setEnabled(true);
        }
        if (uploadButton != null) uploadButton.setVisibility(View.VISIBLE);
        
        if (micButton != null) {
            micButton.setImageResource(R.drawable.ic_mic); // Default mic icon
            micButton.setColorFilter(ContextCompat.getColor(activity, R.color.text_secondary));
            micButton.setScaleX(1f);
            micButton.setScaleY(1f);
        }
        
        if (sendButton != null) sendButton.setVisibility(View.VISIBLE);
    }

    private void transitionToListening() {
        if (inputField != null) {
            inputField.setText("");
            inputField.setHint("Listening...");
            inputField.setEnabled(false); // Lock manual entry while voice is taking over
        }
        
        if (uploadButton != null) uploadButton.setVisibility(View.GONE);
        if (sendButton != null) sendButton.setVisibility(View.GONE);
 
        // Highlight mic
        if (micButton != null) {
            micButton.setColorFilter(ContextCompat.getColor(activity, R.color.purple_accent));
        }
        startMicGlowAnimation();
    }

    private void transitionToTranscribing() {
        // Same visual as listening, but maybe start a heavier waveform parsing effect
        if (waveformAnimator == null || !waveformAnimator.isRunning()) {
            startWaveformAnimation();
        }
    }

    private void transitionToProcessing() {
        stopAnimations();
        
        // Keep transcription visible, but show loading status
        micButton.setImageResource(R.drawable.ic_search); // A placeholder since progressbar takes work dynamically
        micButton.setColorFilter(ContextCompat.getColor(activity, R.color.water_blue));
        
        // Spin the mic icon subtly to represent processing
        ObjectAnimator spin = ObjectAnimator.ofFloat(micButton, View.ROTATION, 0f, 360f);
        spin.setDuration(1500);
        spin.setRepeatCount(ObjectAnimator.INFINITE);
        spin.setInterpolator(new android.view.animation.LinearInterpolator());
        spin.start();
        micButton.setTag(spin);
    }

    private void transitionToResponding() {
        ObjectAnimator spin = (ObjectAnimator) micButton.getTag();
        if (spin != null) {
            spin.cancel();
            micButton.setRotation(0f);
        }
        
        // The Assistant holds output in Chat UI
        // Bottom bar returns to idle shortly after response triggers if not in continuous mode
        mainHandler.postDelayed(() -> {
            if (VoiceStateManager.getInstance().getCurrentState() == VoiceStateManager.VoiceState.RESPONDING) {
                transitionToIdle();
            }
        }, 2000);
    }

    // -- Core Pipeline Logic --

    /**
     * Completes the pipeline: WakeWordDetected -> LISTENING -> TRANSCRIBING -> PROCESSING -> 
     * -> sendMessageToChat -> triggerLLMResponse -> RESPONDING
     */
    private void sendMessageToChatAndTriggerResponse(String text) {
        if (TextUtils.isEmpty(text)) return;
        
        // Launch Chat UI automatically with the recognized payload
        // This transfers standard Home Screen interaction seamlessly into Voice Mode Chat history!
        Intent chatIntent = new Intent(activity, ChatActivity.class);
        chatIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        chatIntent.putExtra(ChatActivity.EXTRA_INITIAL_MESSAGE, text);
        // Generate a fresh session ID for a new chat thread
        chatIntent.putExtra("chat_id", java.util.UUID.randomUUID().toString());
        // Add a flag to tell chat activity to avoid double-processing if VoiceManager is handling it
        chatIntent.putExtra("from_voice_controller", true);
        activity.startActivity(chatIntent);
        
        // VoicePipelineManager's intent routing engine catches final transcription logic 
        // to process offline capabilities (e.g. Add Reminder) OR triggers ChatVoiceHandler.
    }

    // -- Animation Utilities --

    private void startMicGlowAnimation() {
        if (micGlowAnimator != null && micGlowAnimator.isRunning()) return;

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(micButton, View.SCALE_X, 1.0f, 1.35f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(micButton, View.SCALE_Y, 1.0f, 1.35f, 1.0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(micButton, View.ALPHA, 1.0f, 0.7f, 1.0f);
        
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        alpha.setRepeatCount(ObjectAnimator.INFINITE);

        micGlowAnimator = new AnimatorSet();
        micGlowAnimator.playTogether(scaleX, scaleY, alpha);
        micGlowAnimator.setDuration(1200);
        micGlowAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        micGlowAnimator.start();
    }

    private void startWaveformAnimation() {
        if (waveformAnimator != null && waveformAnimator.isRunning()) return;

        waveformAnimator = ValueAnimator.ofFloat(0.9f, 1.1f);
        waveformAnimator.setDuration(400);
        waveformAnimator.setRepeatCount(ValueAnimator.INFINITE);
        waveformAnimator.setRepeatMode(ValueAnimator.REVERSE);
        waveformAnimator.addUpdateListener(anim -> {
            float val = (float) anim.getAnimatedValue();
            bottomBar.setScaleY(val);
        });
        // waveformAnimator.start(); // Opting out of resizing the entire bar to avoid jank. Keeping mic glow focused.
    }

    private void stopAnimations() {
        if (micGlowAnimator != null) {
            micGlowAnimator.cancel();
        }
        if (waveformAnimator != null) {
            waveformAnimator.cancel();
            bottomBar.setScaleY(1.0f);
        }
    }
}
