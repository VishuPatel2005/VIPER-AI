package com.rudra.myassistant;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

/**
 * Base activity class that provides Edge-to-Edge support and handles system bar insets
 * to ensure that UI elements are not obscured by status bars, navigation bars, or the keyboard.
 */
public abstract class BaseEdgeToEdgeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Enable edge-to-edge display
        EdgeToEdge.enable(this);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        applyInsetsToRoot();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        applyInsetsToRoot();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        applyInsetsToRoot();
    }

    private void applyInsetsToRoot() {
        ViewGroup content = findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) return;

        View root = content.getChildAt(0);
        if (root == null) return;

        // If root is DrawerLayout, we apply the listener to the content child (index 0)
        // to avoid interfering with DrawerLayout's own inset handling for the drawer.
        final View targetView = (root instanceof DrawerLayout && ((DrawerLayout)root).getChildCount() > 0) 
            ? ((DrawerLayout)root).getChildAt(0) : root;

        // Store original padding values BEFORE any insets are applied
        final int origRootTop = targetView.getPaddingTop();
        final int origRootBottom = targetView.getPaddingBottom();
        final int origRootLeft = targetView.getPaddingLeft();
        final int origRootRight = targetView.getPaddingRight();

        // DrawerLayout should not receive padding directly — it breaks the drawer.
        final boolean isDrawerLayout = root instanceof DrawerLayout;

        ViewCompat.setOnApplyWindowInsetsListener(targetView, (v, insets) -> {
            try {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

                boolean topHandled = false;
                boolean bottomHandled = false;

                // Find and adjust top bar
                View topBar = v.findViewById(R.id.topBar);
                if (topBar == null) topBar = v.findViewById(R.id.top_bar);
                if (topBar == null) topBar = v.findViewById(R.id.toolbar);
                if (topBar == null) topBar = v.findViewById(R.id.chat_top_bar);

                if (topBar != null) {
                    if (topBar.getTag(R.id.home_indicator) == null) {
                        ViewGroup.LayoutParams lp = topBar.getLayoutParams();
                        if (lp != null && lp.height > 0) {
                            lp.height += systemBars.top;
                            topBar.setLayoutParams(lp);
                        }
                        topBar.setPadding(
                            topBar.getPaddingLeft(),
                            topBar.getPaddingTop() + systemBars.top,
                            topBar.getPaddingRight(),
                            topBar.getPaddingBottom()
                        );
                        topBar.setTag(R.id.home_indicator, true);
                    }
                    topHandled = true;
                }

                // Adjust ScrollView padding if present to prevent gap when toolbar hides
                View scrollView = v.findViewById(R.id.scrollView);
                if (scrollView != null && scrollView.getTag(R.id.home_indicator) == null) {
                    scrollView.setPadding(
                        scrollView.getPaddingLeft(),
                        scrollView.getPaddingTop() + systemBars.top,
                        scrollView.getPaddingRight(),
                        scrollView.getPaddingBottom()
                    );
                    scrollView.setTag(R.id.home_indicator, true);
                }

                // Find and adjust bottom bar
                View bottomBar = v.findViewById(R.id.bottomBar);
                if (bottomBar == null) bottomBar = v.findViewById(R.id.bottom_bar);

                if (bottomBar != null) {
                    int bottomInset = Math.max(systemBars.bottom, ime.bottom);
                    Object tagValue = bottomBar.getTag(R.id.navigation_bar);
                    int originalBottomPadding;

                    if (tagValue instanceof Integer) {
                        originalBottomPadding = (Integer) tagValue;
                    } else {
                        originalBottomPadding = bottomBar.getPaddingBottom();
                        bottomBar.setTag(R.id.navigation_bar, originalBottomPadding);
                    }

                    bottomBar.setPadding(
                        bottomBar.getPaddingLeft(),
                        bottomBar.getPaddingTop(),
                        bottomBar.getPaddingRight(),
                        originalBottomPadding + bottomInset
                    );
                    bottomHandled = true;
                }

                // Apply insets to root for anything not handled by specific bars.
                // Skip padding on DrawerLayout roots (it breaks drawer sliding).
                if (!isDrawerLayout) {
                    int newTop = topHandled ? origRootTop : (origRootTop + systemBars.top);
                    int newBottom = bottomHandled ? origRootBottom : (origRootBottom + Math.max(systemBars.bottom, ime.bottom));
                    v.setPadding(origRootLeft, newTop, origRootRight, newBottom);
                }

            } catch (Exception e) {
                android.util.Log.e("BaseEdgeToEdge", "Inset listener failed", e);
            }

            return WindowInsetsCompat.CONSUMED;
        });
    }
}
