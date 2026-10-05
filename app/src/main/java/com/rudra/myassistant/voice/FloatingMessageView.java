package com.rudra.myassistant.voice;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

public class FloatingMessageView {
    
    private static final String TAG = "FloatingMessageView";
    private static final int DISMISS_DELAY_MS = 3000;
    
    private static CardView currentCardView = null;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static Runnable dismissRunnable;

    public static void show(Activity activity, String message) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.e(TAG, "Cannot show floating message on a null or destroyed activity");
            return;
        }

        mainHandler.post(() -> {
            ViewGroup root = (ViewGroup) activity.getWindow().getDecorView().findViewById(android.R.id.content);
            if (root == null) return;

            // Remove existing if any
            if (currentCardView != null && currentCardView.getParent() != null) {
                ((ViewGroup) currentCardView.getParent()).removeView(currentCardView);
                if (dismissRunnable != null) {
                    mainHandler.removeCallbacks(dismissRunnable);
                }
            }

            // Create CardView container
            CardView cardView = new CardView(activity);
            cardView.setRadius(dpToPx(activity, 16));
            cardView.setCardElevation(dpToPx(activity, 8));
            cardView.setCardBackgroundColor(Color.parseColor("#333333"));
            
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            params.topMargin = dpToPx(activity, 80);
            cardView.setLayoutParams(params);

            // Create TextView
            TextView textView = new TextView(activity);
            textView.setText(message);
            textView.setTextColor(Color.WHITE);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            int padding = dpToPx(activity, 16);
            textView.setPadding(padding, dpToPx(activity, 12), padding, dpToPx(activity, 12));
            
            cardView.addView(textView);

            // Add to root view
            root.addView(cardView);
            currentCardView = cardView;
            
            // Fade in animation
            cardView.setAlpha(0f);
            cardView.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();

            // Schedule dismiss
            dismissRunnable = () -> dismiss(cardView);
            mainHandler.postDelayed(dismissRunnable, DISMISS_DELAY_MS);
        });
    }

    private static void dismiss(CardView cardView) {
        if (cardView != null) {
            cardView.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (cardView.getParent() != null) {
                                ((ViewGroup) cardView.getParent()).removeView(cardView);
                            }
                            if (currentCardView == cardView) {
                                currentCardView = null;
                            }
                        }
                    })
                    .start();
        }
    }

    private static int dpToPx(Activity activity, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                activity.getResources().getDisplayMetrics()
        );
    }
}
