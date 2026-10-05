package com.rudra.myassistant;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Viper Motion System — Static animation utilities for Viper AI.
 * All methods are null-safe and designed for mid-range Android devices.
 * No Lottie. No Compose. No Kotlin. Pure native Android animation APIs.
 */
public final class AnimationHelper {

    private static final FastOutSlowInInterpolator EASE = new FastOutSlowInInterpolator();

    private AnimationHelper() { /* Utility class */ }

    // ─── PART 4: Button Press Micro-Interactions ──────────────────────────

    /**
     * Applies a subtle press-scale animation on any tappable view.
     * ACTION_DOWN → shrink to 0.96, ACTION_UP/CANCEL → restore to 1.0.
     */
    public static void applyPressAnimation(@NonNull final View view) {
        applyPressAnimation(view, 0.96f);
    }

    /**
     * Overload that accepts a custom target scale (e.g. 1.08 for mic button).
     */
    public static void applyPressAnimation(@NonNull final View view, final float targetScale) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(targetScale).scaleY(targetScale)
                            .setDuration(100).setInterpolator(EASE).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f)
                            .setDuration(140).setInterpolator(EASE).start();
                    // Ensure click still fires on UP
                    if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                        v.performClick();
                    }
                    break;
            }
            return true; // Consume touch to prevent double-firing
        });
    }

    // ─── PART 3: Carousel Scroll Effect ──────────────────────────────────

    /**
     * Applies center-focus scaling to a horizontal RecyclerView / ViewPager2 child.
     * Center-most card: scale 1.0, alpha 1.0
     * Side cards: scale 0.92, alpha 0.85
     */
    public static void applyCarouselEffect(@NonNull final RecyclerView rv) {
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int centerX = recyclerView.getWidth() / 2;
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    View child = recyclerView.getChildAt(i);
                    if (child == null) continue;

                    int childCenterX = (child.getLeft() + child.getRight()) / 2;
                    float distFromCenter = Math.abs(centerX - childCenterX);
                    float maxDist = recyclerView.getWidth() / 2f;

                    // Normalize: 0 = center, 1 = edge
                    float ratio = Math.min(distFromCenter / maxDist, 1f);

                    float scale = 1f - (0.08f * ratio);   // 1.0 → 0.92
                    float alpha = 1f - (0.15f * ratio);    // 1.0 → 0.85

                    child.setScaleX(scale);
                    child.setScaleY(scale);
                    child.setAlpha(alpha);
                }
            }
        });
    }

    // ─── PART 8: Glow Pulse on Active Card ───────────────────────────────

    /**
     * Pulses a glow overlay view's alpha between 0.15 and 0.22 every 5 seconds.
     * Returns the animator so caller can cancel it in onPause/onDestroy.
     */
    public static ObjectAnimator applyGlowPulse(@NonNull final View glowOverlay) {
        ObjectAnimator pulse = ObjectAnimator.ofFloat(glowOverlay, "alpha", 0.15f, 0.22f, 0.15f);
        pulse.setDuration(5000);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setInterpolator(EASE);
        pulse.start();
        return pulse;
    }

    // ─── PART 9: Health Icon Breathing Animation ─────────────────────────

    /**
     * Applies a subtle infinite breathing scale loop (1.0 → 1.04 → 1.0) to any icon.
     * Returns the AnimatorSet so callers can cancel() in onPause to prevent leaks.
     */
    public static AnimatorSet applyBreathingAnimation(@NonNull final View iconView) {
        ObjectAnimator breatheX = ObjectAnimator.ofFloat(iconView, "scaleX", 1f, 1.04f, 1f);
        ObjectAnimator breatheY = ObjectAnimator.ofFloat(iconView, "scaleY", 1f, 1.04f, 1f);
        AnimatorSet breathe = new AnimatorSet();
        breathe.playTogether(breatheX, breatheY);
        breathe.setDuration(2800);
        breathe.setInterpolator(EASE);
        breathe.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (!breathe.isStarted()) return; // Guard against post-cancel calls
                breathe.start();
            }
        });
        breathe.start();
        return breathe;
    }

    // ─── PART 5: Container Transform (Press Confirm + Navigate) ──────────

    /**
     * Animates a source view with a press-confirm effect, then fires runnable.
     * scaleX/Y 1.0 → 0.96 over 80ms → 1.0 over 80ms → runs action.
     */
    public static void animateContainerTransform(@NonNull final View source, @NonNull final Runnable action) {
        source.animate().scaleX(0.96f).scaleY(0.96f)
                .setDuration(80).setInterpolator(EASE)
                .withEndAction(() ->
                    source.animate().scaleX(1f).scaleY(1f)
                            .setDuration(80).setInterpolator(EASE)
                            .withEndAction(action)
                            .start()
                ).start();
    }

    /**
     * Animates a view sliding up from 20dp below with a fade-in effect.
     * Use with index * 60ms delay for a staggered entrance.
     */
    public static void applySlideUpFade(@NonNull final View view, int index) {
        view.setAlpha(0f);
        view.setTranslationY(60f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(index * 50L)
                .setInterpolator(EASE)
                .start();
    }

    /**
     * Applies a gentle floating motion (up and down 4dp).
     */
    public static void applyFloatingAnimation(@NonNull final View view) {
        ObjectAnimator floatAnim = ObjectAnimator.ofFloat(view, "translationY", 0f, -12f, 0f);
        floatAnim.setDuration(3000 + (long)(Math.random() * 1000)); 
        floatAnim.setRepeatCount(ValueAnimator.INFINITE);
        floatAnim.setInterpolator(EASE);
        floatAnim.start();
    }

    /**
     * Pulses alpha between 0.3 and 0.6 for loading states.
     */
    public static void applyShimmerPulse(@NonNull final View view) {
        ObjectAnimator pulse = ObjectAnimator.ofFloat(view, "alpha", 0.3f, 0.6f, 0.3f);
        pulse.setDuration(1200);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setInterpolator(EASE);
        pulse.start();
    }

    // ─── Utility ─────────────────────────────────────────────────────────

    /**
     * Converts dp to pixels using the view's context.
     */
    public static int dpToPx(@NonNull Context context, float dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
