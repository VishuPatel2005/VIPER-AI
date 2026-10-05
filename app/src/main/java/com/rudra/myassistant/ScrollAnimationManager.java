package com.rudra.myassistant;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Viper Motion System — Top bar hide/show on scroll.
 * Tracks scroll direction and smoothly translates the toolbar off-screen.
 * Supports both NestedScrollView and RecyclerView as scroll sources.
 * Java only. No Kotlin. No Compose.
 */
public final class ScrollAnimationManager {

    private static final FastOutSlowInInterpolator EASE = new FastOutSlowInInterpolator();
    private static final int DURATION_MS = 220;
    private static final int SCROLL_THRESHOLD = 12; // minimum dy to trigger

    private final View toolbar;
    private boolean isHidden = false;

    public ScrollAnimationManager(@NonNull View toolbar) {
        this.toolbar = toolbar;
    }

    // ─── Attach to NestedScrollView ──────────────────────────────────────

    /**
     * Attaches scroll tracking to a NestedScrollView.
     */
    public void attachToNestedScrollView(@NonNull NestedScrollView scrollView) {
        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    int dy = scrollY - oldScrollY;
                    handleScroll(dy);
                });
    }

    // ─── Attach to RecyclerView ──────────────────────────────────────────

    /**
     * Attaches scroll tracking to a RecyclerView.
     */
    public void attachToRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                handleScroll(dy);
            }
        });
    }

    // ─── Core Logic ──────────────────────────────────────────────────────

    private void handleScroll(int dy) {
        if (toolbar == null) return;

        if (dy > SCROLL_THRESHOLD && !isHidden) {
            // Scrolling DOWN → hide toolbar
            isHidden = true;
            toolbar.animate()
                    .translationY(-toolbar.getHeight())
                    .setDuration(DURATION_MS)
                    .setInterpolator(EASE)
                    .start();
        } else if (dy < -SCROLL_THRESHOLD && isHidden) {
            // Scrolling UP → show toolbar
            isHidden = false;
            toolbar.animate()
                    .translationY(0)
                    .setDuration(DURATION_MS)
                    .setInterpolator(EASE)
                    .start();
        }
    }

    /**
     * Force-shows the toolbar (e.g. when returning to screen).
     */
    public void reset() {
        isHidden = false;
        if (toolbar != null) {
            toolbar.setTranslationY(0);
        }
    }
}
