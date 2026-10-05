package com.rudra.myassistant.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.rudra.myassistant.MyAssistantApplication;
import com.rudra.myassistant.R;
import com.rudra.myassistant.network.ApiConfig;
import com.rudra.myassistant.network.ContextSuggestionDto;
import com.rudra.myassistant.network.GeoapifyApi;
import com.rudra.myassistant.network.PlacesSearchResponseDto;
import com.rudra.myassistant.network.RetrofitClient;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ContextUIManager {
    private static ContextUIManager instance;
    private final Handler mainHandler;
    private GeoapifyApi geoapifyApi;
    
    // Cache to prevent re-showing the same context suggestion
    // Key: context_type, Value: timestamp
    private final HashMap<String, Long> shownSuggestionsCache = new HashMap<>();
    private static final long COOLDOWN_MS = 600000; // 10 minutes

    // Single active view across activities so it doesn't pile up
    private View activeSuggestionView;

    private ContextUIManager() {
        geoapifyApi = RetrofitClient.getGeoapifyInstance().create(GeoapifyApi.class);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized ContextUIManager getInstance() {
        if (instance == null) {
            instance = new ContextUIManager();
        }
        return instance;
    }

    public void checkAndShowContext(double latitude, double longitude, MyAssistantApplication app) {
        // Decentralized logic: Ported from backend generate_smart_suggestions
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String category;
        String message;
        String type;

        if (hour >= 6 && hour <= 10) {
            category = "catering.cafe";
            message = "Morning stop?";
            type = "cafe";
        } else if (hour >= 11 && hour <= 14) {
            category = "catering.restaurant";
            message = "Lunch time?";
            type = "restaurant";
        } else {
            category = "entertainment";
            message = "Nearby hangout?";
            type = "entertainment";
        }

        String filter = "circle:" + longitude + "," + latitude + ",2000";
        String apiKey = ApiConfig.GEOAPIFY_API_KEY;

        geoapifyApi.searchNearby(category, filter, 5, apiKey).enqueue(new Callback<PlacesSearchResponseDto>() {
            @Override
            public void onResponse(Call<PlacesSearchResponseDto> call, Response<PlacesSearchResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PlacesSearchResponseDto body = response.body();
                    if (body.getFeatures() != null && !body.getFeatures().isEmpty()) {
                        // Pick the first result as the primary spot
                        PlacesSearchResponseDto.Feature feature = body.getFeatures().get(0);
                        String placeName = feature.getProperties().getName();
                        if (placeName == null) placeName = feature.getProperties().getStreet();
                        
                        // Create a list of suggestions (optional, using category/name)
                        List<String> suggestions = new ArrayList<>();
                        suggestions.add("Visit " + placeName);
                        suggestions.add("Check details");
                        
                        ContextSuggestionDto dto = new ContextSuggestionDto();
                        dto.setContextType(type);
                        dto.setMessage(message);
                        dto.setPlaceName(placeName);
                        dto.setSuggestions(suggestions);
                        
                        handleSuggestion(dto, app);
                    }
                }
            }

            @Override
            public void onFailure(Call<PlacesSearchResponseDto> call, Throwable t) {
                Log.e("ContextUIManager", "Failed to fetch context: " + t.getMessage());
            }
        });
    }

    private void handleSuggestion(ContextSuggestionDto dto, MyAssistantApplication app) {
        String type = dto.getContextType();
        long now = System.currentTimeMillis();
        
        if (shownSuggestionsCache.containsKey(type)) {
            Long lastShown = shownSuggestionsCache.get(type);
            if (lastShown != null && now - lastShown < COOLDOWN_MS) {
                return; // Suppress showing again within 10 minutes
            }
        }
        
        // Mark as shown immediately
        shownSuggestionsCache.put(type, now);

        mainHandler.post(() -> {
            Activity currentActivity = app.getCurrentPageTracker().getCurrentActivity();
            if (currentActivity == null || currentActivity.isFinishing()) return;

            showFloatingCard(currentActivity, dto);
        });
    }

    private void showFloatingCard(Activity activity, ContextSuggestionDto dto) {
        ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null) return;

        // Remove old if exists
        if (activeSuggestionView != null && activeSuggestionView.getParent() != null) {
            ((ViewGroup)activeSuggestionView.getParent()).removeView(activeSuggestionView);
        }

        // Build UI programmatically
        CardView card = new CardView(activity);
        
        // CardView properties
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.setMargins(40, 150, 40, 0); 
        card.setLayoutParams(params);
        card.setCardBackgroundColor(Color.parseColor("#1E1E24"));
        card.setRadius(24f);
        card.setCardElevation(12f);
        
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(40, 40, 40, 40);

        // Header
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        
        final String emoji;
        if ("supermarket".equals(dto.getContextType())) emoji = "🛒";
        else if ("shopping_mall".equals(dto.getContextType())) emoji = "🛍️";
        else if ("cafe".equals(dto.getContextType())) emoji = "☕";
        else if ("restaurant".equals(dto.getContextType())) emoji = "🍔";
        else if ("park".equals(dto.getContextType())) emoji = "🌳";
        else if ("cinema".equals(dto.getContextType())) emoji = "🎬";
        else if ("entertainment".equals(dto.getContextType())) emoji = "🎡";
        else emoji = "📍";
        
        TextView title = new TextView(activity);
        String nameStr = dto.getPlaceName() != null ? " (" + dto.getPlaceName() + ")" : "";
        title.setText(emoji + " " + dto.getMessage() + nameStr);
        title.setTextColor(Color.WHITE);
        title.setTextSize(16f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams tParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        title.setLayoutParams(tParams);
        
        ImageView closeBtn = new ImageView(activity);
        closeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        closeBtn.setColorFilter(Color.parseColor("#808080"));
        closeBtn.setOnClickListener(v -> {
            if (card.getParent() != null) {
                ((ViewGroup)card.getParent()).removeView(card);
                activeSuggestionView = null;
            }
        });

        header.addView(title);
        header.addView(closeBtn);
        container.addView(header);

        // Subtitle / Options
        TextView subtitle = new TextView(activity);
        subtitle.setText("Location Suggestions:");
        subtitle.setTextColor(Color.parseColor("#A0A0A0"));
        subtitle.setTextSize(14f);
        subtitle.setPadding(0, 10, 0, 10);
        container.addView(subtitle);

        List<String> items = dto.getSuggestions();
        if (items != null) {
            for (String itemStr : items) {
                TextView item = new TextView(activity);
                item.setText("• " + itemStr);
                item.setTextColor(Color.WHITE);
                item.setTextSize(14f);
                item.setPadding(0, 4, 0, 4);
                container.addView(item);
            }
        }

        card.addView(container);
        root.addView(card);
        
        activeSuggestionView = card;
        
        mainHandler.postDelayed(() -> {
            if (card.getParent() != null) {
                ((ViewGroup)card.getParent()).removeView(card);
                if (activeSuggestionView == card) activeSuggestionView = null;
            }
        }, 15000);
    }
}
