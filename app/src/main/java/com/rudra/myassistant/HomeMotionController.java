package com.rudra.myassistant;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import java.util.List;

/**
 * Viper Motion System — Home screen card entrance orchestrator.
 * Triggers a staggered fade+slide animation for each dashboard section.
 * Java only. No Kotlin. No Compose.
 */
public final class HomeMotionController {

    private static final FastOutSlowInInterpolator EASE = new FastOutSlowInInterpolator();
    private static final int DURATION_MS = 320;
    private static final int STAGGER_MS = 70;
    private static final float TRANSLATE_DP = 30f;

    private final Context context;

    public HomeMotionController(@NonNull Context context) {
        this.context = context;
    }

    /**
     * Animates a list of card views with a staggered entrance.
     * Each card slides up from +30dp and fades in from alpha 0.
     * Cards are staggered by 70ms * index.
     *
     * @param cardViews ordered list of dashboard section views (top to bottom)
     */
    public void animateCardsEntrance(@NonNull List<View> cardViews) {
        float translatePx = AnimationHelper.dpToPx(context, TRANSLATE_DP);

        for (int i = 0; i < cardViews.size(); i++) {
            View card = cardViews.get(i);
            if (card == null) continue;

            // Set initial state
            card.setAlpha(0f);
            card.setTranslationY(translatePx);

            // Animate with stagger
            card.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(DURATION_MS)
                    .setStartDelay((long) STAGGER_MS * i)
                    .setInterpolator(EASE)
                    .start();
        }
    }
}
