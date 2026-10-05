package com.rudra.myassistant;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.google.firebase.Timestamp;
import com.rudra.myassistant.network.ApiConfig;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.GeoapifyApi;
import com.rudra.myassistant.network.PlacesSearchResponseDto;
import com.rudra.myassistant.network.RetrofitClient;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Manages proactive, context-aware suggestions for the user dashboard.
 * Designed to respect Geoapify free tier limits (throttling).
 */
public class SmartContextManager {

    private static SmartContextManager instance;
    private final FirestoreRepository firestoreRepo;
    private final GeoapifyApi geoapifyApi;
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    private static final long LOCATION_CHECK_COOLDOWN = 15 * 60 * 1000; // 15 mins
    private long lastLocationCheck = 0;
    
    public interface SuggestionListener {
        void onSuggestionReady(SuggestionsManager.Suggestion suggestion);
        void onClearSuggestion();
    }

    public static synchronized SmartContextManager getInstance() {
        if (instance == null) instance = new SmartContextManager();
        return instance;
    }

    private SmartContextManager() {
        this.firestoreRepo = new FirestoreRepository();
        this.geoapifyApi = RetrofitClient.getGeoapifyInstance().create(GeoapifyApi.class);
    }

    public void runContextChecks(String userId, double lat, double lng, java.util.Set<String> preferences, SuggestionListener listener) {
        long now = System.currentTimeMillis();
        
        // 1. Keep local purely offline/Firebase checks
        checkWellness(userId, listener);
        checkPeriodPrediction(userId, listener);

        // 2. Call hardened backend for location & time-aware intelligence
        if (lat != 0 && (now - lastLocationCheck > 10 * 60 * 1000)) { // Throttled to 10 mins
            fetchBackendSuggestions(userId, lat, lng, listener);
            lastLocationCheck = now;
        }
    }

    private void fetchBackendSuggestions(String userId, double lat, double lng, SuggestionListener listener) {
        com.rudra.myassistant.network.ViperApiService api = RetrofitClient.getViperInstance().create(com.rudra.myassistant.network.ViperApiService.class);
        api.getContextSuggestions(userId, lat, lng).enqueue(new Callback<List<com.rudra.myassistant.network.ViperApiService.ContextSuggestion>>() {
            @Override
            public void onResponse(Call<List<com.rudra.myassistant.network.ViperApiService.ContextSuggestion>> call, Response<List<com.rudra.myassistant.network.ViperApiService.ContextSuggestion>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (com.rudra.myassistant.network.ViperApiService.ContextSuggestion cs : response.body()) {
                        String emoji = "✨"; // Default
                        if (cs.title.contains("Boost")) emoji = "☕";
                        else if (cs.title.contains("Focus")) emoji = "📖";
                        else if (cs.title.contains("Evening")) emoji = "🍽️";
                        
                        SuggestionsManager.Suggestion s = new SuggestionsManager.Suggestion(
                            cs.title, emoji, cs.title, cs.message, 
                            cs.lat != null ? cs.lat : 0, 
                            cs.lng != null ? cs.lng : 0
                        );
                        s.tagChip = "AI Assistant";
                        if (listener != null) listener.onSuggestionReady(s);
                    }
                }
            }
            @Override public void onFailure(Call<List<com.rudra.myassistant.network.ViperApiService.ContextSuggestion>> call, Throwable t) {
                Log.e("SmartContext", "Backend suggestion failure: " + t.getMessage());
            }
        });
    }

    private String getTimeBasedCategories() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 11) return "catering.cafe,catering.restaurant.breakfast";
        if (hour >= 11 && hour < 17) return "catering.cafe,education.library";
        if (hour >= 17 && hour < 22) return "catering.restaurant,leisure.park";
        return "catering.fast_food,catering.restaurant";
    }

    private String getPreferenceCategories(java.util.Set<String> preferences) {
        if (preferences == null || preferences.isEmpty()) return null;
        java.util.List<String> cats = new java.util.ArrayList<>();
        for (String p : preferences) {
            switch (p) {
                case "cafes": cats.add("catering.cafe"); break;
                case "food": cats.add("catering.restaurant"); break;
                case "gym": cats.add("leisure.sports_centre"); break;
                case "study": cats.add("education.library"); break;
                case "shopping": cats.add("commercial.shopping_mall"); break;
                case "movies": cats.add("entertainment.cinema"); break;
                case "parks": cats.add("leisure.park"); break;
            }
        }
        return String.join(",", cats);
    }

    private void checkActionableLocationSuggestions(double lat, double lng, java.util.Set<String> preferences, SuggestionListener listener) {
        String categories = getPreferenceCategories(preferences);
        if (categories == null) categories = getTimeBasedCategories();
        
        String filter = "circle:" + lng + "," + lat + ",2000"; // Within 2km
        
        geoapifyApi.searchNearby(categories, filter, 2, ApiConfig.GEOAPIFY_API_KEY).enqueue(new Callback<PlacesSearchResponseDto>() {
            @Override
            public void onResponse(Call<PlacesSearchResponseDto> call, Response<PlacesSearchResponseDto> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().getFeatures().isEmpty()) {
                    for (PlacesSearchResponseDto.Feature feature : response.body().getFeatures()) {
                        String name = feature.getProperties().getName();
                        if (name == null) continue;
                        
                        double pLat = feature.getGeometry().getLat();
                        double pLon = feature.getGeometry().getLon();
                        
                        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                        String title, message, emoji;
                        
                        if (hour < 11) {
                            title = "Morning Boost";
                            emoji = "☕";
                            message = "Start your day right at " + name;
                        } else if (hour < 17) {
                            title = "Afternoon Focus";
                            emoji = "📖";
                            message = "Perfect place nearby to continue your work: " + name;
                        } else if (hour < 22) {
                            title = "Evening Relaxation";
                            emoji = "🍽️";
                            message = "Time for a break? " + name + " is highly recommended.";
                        } else {
                            title = "Late Night Bite";
                            emoji = "🍕";
                            message = "Getting hungry? " + name + " is open nearby.";
                        }

                        SuggestionsManager.Suggestion s = new SuggestionsManager.Suggestion(title, emoji, name, message, pLat, pLon);
                        s.tagChip = "Nearby";
                        if (listener != null) listener.onSuggestionReady(s);
                    }
                }
            }
            @Override public void onFailure(Call<PlacesSearchResponseDto> call, Throwable t) {}
        });
    }

    private void checkWellness(String userId, SuggestionListener listener) {
        // Hydration reminders removed per user request
    }

    private void checkSocialGhosting(String userId, SuggestionListener listener) {
        // Redundant - removed per user feedback to reduce clutter
    }

    private void checkGroceryAssistant(String userId, double lat, double lng, SuggestionListener listener) {
        String filter = "circle:" + lng + "," + lat + ",150"; // Within 150m
        geoapifyApi.searchNearby("commercial.supermarket", filter, 1, ApiConfig.GEOAPIFY_API_KEY).enqueue(new Callback<PlacesSearchResponseDto>() {
            @Override
            public void onResponse(Call<PlacesSearchResponseDto> call, Response<PlacesSearchResponseDto> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().getFeatures().isEmpty()) {
                    // User is near a supermarket, check notes
                    firestoreRepo.getNotesQuery(userId).get().addOnSuccessListener(notes -> {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : notes.getDocuments()) {
                            String title = doc.getString("title");
                            String content = doc.getString("content");
                            if ((title != null && title.toLowerCase().contains("grocery")) || 
                                (content != null && content.toLowerCase().contains("buy"))) {
                                SuggestionsManager.Suggestion s = new SuggestionsManager.Suggestion(
                                    "Grocery Note Found", "🛒", "You're near a store! Check your list?", "grocery"
                                );
                                s.tagChip = "Shopping";
                                listener.onSuggestionReady(s);
                                break;
                            }
                        }
                    });
                }
            }
            @Override public void onFailure(Call<PlacesSearchResponseDto> call, Throwable t) {}
        });
    }

    private void checkWorkingContext(String userId, double lat, double lng, SuggestionListener listener) {
        String filter = "circle:" + lng + "," + lat + ",100";
        geoapifyApi.searchNearby("education,commercial.office", filter, 1, ApiConfig.GEOAPIFY_API_KEY).enqueue(new Callback<PlacesSearchResponseDto>() {
            @Override
            public void onResponse(Call<PlacesSearchResponseDto> call, Response<PlacesSearchResponseDto> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().getFeatures().isEmpty()) {
                    SuggestionsManager.Suggestion s = new SuggestionsManager.Suggestion(
                        "Focus Mode?", "🎓", "Should I silence your notifications?", "focus"
                    );
                    s.tagChip = "Context Aware";
                    listener.onSuggestionReady(s);
                }
            }
            @Override public void onFailure(Call<PlacesSearchResponseDto> call, Throwable t) {}
        });
        
        checkTrafficAlerts(userId, lat, lng, listener);
    }

    private void checkNearbyExploration(double lat, double lng, java.util.Set<String> preferences, SuggestionListener listener) {
        String filter = "circle:" + lng + "," + lat + ",1500"; 
        String categories = getPreferenceCategories(preferences);
        if (categories == null) categories = "amenity.cafe,tourism.attraction,leisure.park,commercial.shopping_mall";
        
        geoapifyApi.searchNearby(categories, filter, 3, ApiConfig.GEOAPIFY_API_KEY).enqueue(new Callback<PlacesSearchResponseDto>() {
            @Override
            public void onResponse(Call<PlacesSearchResponseDto> call, Response<PlacesSearchResponseDto> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().getFeatures().isEmpty()) {
                    for (int i = 0; i < Math.min(2, response.body().getFeatures().size()); i++) {
                        String name = response.body().getFeatures().get(i).getProperties().getName();
                        if (name != null) {
                            SuggestionsManager.Suggestion s = new SuggestionsManager.Suggestion(
                                "Explore Neighborhood", "🌎", "You're not far from " + name, "social"
                            );
                            s.tagChip = "Nearby";
                            listener.onSuggestionReady(s);
                        }
                    }
                }
            }
            @Override public void onFailure(Call<PlacesSearchResponseDto> call, Throwable t) {}
        });
    }

    private void checkWelcome(String userId, SuggestionListener listener) {
        SuggestionsManager.Suggestion s = new SuggestionsManager.Suggestion(
            "Welcome to Viper AI", "✨", "I'm monitoring your context in the background.", "intro"
        );
        s.tagChip = "Intro";
        listener.onSuggestionReady(s);
    }

    private void checkNightOwl(String userId, SuggestionListener listener) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 23 || hour < 5) {
            SuggestionsManager.Suggestion s = new SuggestionsManager.Suggestion(
                "Night Owl Wellness", "🌙", "Quality sleep is key to your goals.", "wellness"
            );
            s.tagChip = "Health Tip";
            listener.onSuggestionReady(s);
        }
    }

    private void checkLunchBreak(String userId, SuggestionListener listener) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 12 && hour <= 14) {
            SuggestionsManager.Suggestion s = new SuggestionsManager.Suggestion(
                "Lunch Break", "🥗", "Nourish yourself during this break.", "wellness"
            );
            s.tagChip = "Routine";
            listener.onSuggestionReady(s);
        }
    }

    private void checkMorningCommute(String userId, SuggestionListener listener) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 8 && hour < 10) {
            SuggestionsManager.Suggestion s = new SuggestionsManager.Suggestion(
                "Morning Commute", "🚗", "I can check your traffic today.", "traffic"
            );
            s.tagChip = "Travel";
            listener.onSuggestionReady(s);
        }
    }

    private void checkTrafficAlerts(String userId, double lat, double lng, SuggestionListener listener) {
        firestoreRepo.getEventsQuery(userId).get().addOnSuccessListener(events -> {
            long now = System.currentTimeMillis();
            for (com.google.firebase.firestore.DocumentSnapshot doc : events.getDocuments()) {
                String startStr = doc.getString("startTime");
                if (startStr != null) {
                    try {
                        java.text.SimpleDateFormat iso = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
                        java.util.Date start = iso.parse(startStr);
                        long diff = start.getTime() - now;
                        if (diff > 0 && diff < 60 * 60 * 1000) {
                            SuggestionsManager.Suggestion s = new SuggestionsManager.Suggestion(
                                "Leave Soon?", "🚗", "Your event starts soon.", "traffic"
                            );
                            s.tagChip = "Event Alert";
                            listener.onSuggestionReady(s);
                            break;
                        }
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    private void checkPeriodPrediction(String userId, SuggestionListener listener) {
        firestoreRepo.healthStatsCol.whereEqualTo("userId", userId).orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(1).get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot != null && !snapshot.isEmpty()) {
                    com.google.firebase.firestore.DocumentSnapshot latest = snapshot.getDocuments().get(0);
                    Timestamp ts = latest.getTimestamp("timestamp");
                    if (ts != null) {
                        long daysSince = (System.currentTimeMillis() - ts.toDate().getTime()) / (24 * 60 * 60 * 1000);
                        if (daysSince >= 25 && daysSince <= 28) {
                            SuggestionsManager.Suggestion s = new SuggestionsManager.Suggestion(
                                "Cycle Insights", "📅", "Your next cycle is predicted to start soon.", "health"
                            );
                            s.tagChip = "Health Insights";
                            listener.onSuggestionReady(s);
                        }
                    }
                }
            });
    }

}

