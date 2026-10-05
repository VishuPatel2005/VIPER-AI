package com.rudra.myassistant;

import com.rudra.myassistant.model.LatLng;
import com.rudra.myassistant.model.PlaceSuggestionModel;
import com.rudra.myassistant.network.GeoapifyService;
import com.rudra.myassistant.network.OpenAiService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Coordinates hangout planning between Geoapify and OpenAI.
 * [BUG 3/4 FIX] Ensures Geoapify is actually called with valid midpoint coordinates.
 */
public class HangoutSuggestionManager {
    private final GeoapifyService geoapifyService;
    private final OpenAiService openAiService;
    private static final Map<String, String> CATEGORY_MAP = new HashMap<>();

    static {
        CATEGORY_MAP.put("coffee", "catering.cafe");
        CATEGORY_MAP.put("cafe", "catering.cafe");
        CATEGORY_MAP.put("restaurant", "catering.restaurant");
        CATEGORY_MAP.put("food", "catering.restaurant");
        CATEGORY_MAP.put("eat", "catering.restaurant");
        CATEGORY_MAP.put("dessert", "catering.ice_cream");
        CATEGORY_MAP.put("sweet", "catering.ice_cream");
        CATEGORY_MAP.put("movie", "entertainment.cinema");
        CATEGORY_MAP.put("cinema", "entertainment.cinema");
        CATEGORY_MAP.put("park", "leisure.park");
        CATEGORY_MAP.put("outdoor", "leisure.park");
        CATEGORY_MAP.put("nature", "leisure.park");
        CATEGORY_MAP.put("shopping", "commercial.shopping_mall");
        CATEGORY_MAP.put("mall", "commercial.shopping_mall");
        CATEGORY_MAP.put("hangout", "leisure");
        CATEGORY_MAP.put("fun", "leisure");
        CATEGORY_MAP.put("activity", "activity");
    }

    private final Map<String, CachedResult> sessionCache = new HashMap<>();

    private static class CachedResult {
        List<PlaceSuggestionModel> places;
        String intent;
        long timestamp;

        CachedResult(List<PlaceSuggestionModel> places, String intent) {
            this.places = places;
            this.intent = intent;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isFresh(String newIntent) {
            return intent.equals(newIntent) && (System.currentTimeMillis() - timestamp < 300000);
        }
    }

    public interface Callback {
        void onSuggestionsReady(String message, List<PlaceSuggestionModel> places);
        void onError(String error);
    }

    public HangoutSuggestionManager() {
        this.geoapifyService = new GeoapifyService();
        this.openAiService = new OpenAiService();
    }

    /**
     * [BUG 3/4 FIX] Fetch initial cafe suggestions using midpoint of user and friend coordinates.
     * This is the entry point for the hangout flow — Geoapify MUST be called here.
     */
    public void fetchInitialSuggestions(String sessionId, LatLng userLatLng, LatLng friendLatLng, String friendName, String userAreaName, Callback callback) {
        // Check cache first
        CachedResult cached = sessionCache.get(sessionId + "_cafe");
        if (cached != null && cached.isFresh("cafe")) {
            callback.onSuggestionsReady("Here are some spots I found earlier:", cached.places);
            return;
        }

        // [BUG 3 FIX] Calculate midpoint and call Geoapify
        double[] midpoint = GeoapifyService.calculateMidpoint(userLatLng.latitude, userLatLng.longitude, friendLatLng.latitude, friendLatLng.longitude);
        
        geoapifyService.fetchNearbyPlaces(midpoint[0], midpoint[1], "catering.cafe", 2000, 3, new GeoapifyService.GeoapifyCallback() {
            @Override
            public void onResult(List<PlaceSuggestionModel> places) {
                if (places.isEmpty()) {
                    callback.onSuggestionsReady("I couldn't find any cafes nearby. Want to try a different category like restaurants or parks?", new ArrayList<>());
                    return;
                }

                // Cache the results
                sessionCache.put(sessionId + "_cafe", new CachedResult(places, "cafe"));

                List<String> placeNames = new ArrayList<>();
                for (PlaceSuggestionModel p : places) placeNames.add(p.getPlaceName());

                openAiService.generateHangoutOpeningMessage(friendName, userAreaName, placeNames, new OpenAiService.OpenAiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        callback.onSuggestionsReady(response, places);
                    }

                    @Override
                    public void onFailure(String error) {
                        // [BUG 4 FIX] Safe fallback message that doesn't crash with <3 results
                        String p1 = placeNames.size() > 0 ? placeNames.get(0) : "a local cafe";
                        String p2 = placeNames.size() > 1 ? placeNames.get(1) : "a nearby spot";
                        String p3 = placeNames.size() > 2 ? placeNames.get(2) : "somewhere fun";
                        String fallback = String.format("Hey! Looks like you and %s are both nearby 📍\nYou're around %s.\n\nHere are some spots nearby:\n1. %s\n2. %s\n3. %s\n\nWant directions to one of these or something different?\nYou can ask for: coffee, dessert, restaurant, movie, park, activity",
                            friendName, userAreaName, p1, p2, p3);
                        callback.onSuggestionsReady(fallback, places);
                    }
                });
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void fetchSuggestionsForIntent(String sessionId, String userMessage, LatLng userLatLng, LatLng friendLatLng, Callback callback) {
        // Simple direct keyword match first
        String simpleIntent = null;
        for (String key : CATEGORY_MAP.keySet()) {
            if (userMessage.toLowerCase().contains(key)) {
                simpleIntent = key;
                break;
            }
        }

        if (simpleIntent != null) {
            processIntent(sessionId, simpleIntent, userLatLng, friendLatLng, callback);
        } else {
            openAiService.extractPlaceIntent(userMessage, new OpenAiService.OpenAiCallback() {
                @Override
                public void onSuccess(String intent) {
                    processIntent(sessionId, intent, userLatLng, friendLatLng, callback);
                }

                @Override
                public void onFailure(String error) {
                    processIntent(sessionId, "coffee", userLatLng, friendLatLng, callback);
                }
            });
        }
    }

    private void processIntent(String sessionId, String intent, LatLng userLatLng, LatLng friendLatLng, Callback callback) {
        // [BUG 5 FIX] Consistent Cache Key: sessionId + intent
        String cacheKey = sessionId + "_" + intent;
        CachedResult cached = sessionCache.get(cacheKey);
        
        if (cached != null && cached.isFresh(intent)) {
            callback.onSuggestionsReady("Here are some more spots for " + intent + ":", cached.places);
            return;
        }

        String geoCategory = CATEGORY_MAP.getOrDefault(intent, "catering.cafe");
        double[] midpoint = GeoapifyService.calculateMidpoint(userLatLng.latitude, userLatLng.longitude, friendLatLng.latitude, friendLatLng.longitude);

        geoapifyService.fetchNearbyPlaces(midpoint[0], midpoint[1], geoCategory, 2000, 3, new GeoapifyService.GeoapifyCallback() {
            @Override
            public void onResult(List<PlaceSuggestionModel> places) {
                if (places.isEmpty()) {
                    callback.onSuggestionsReady("I couldn't find " + intent + " spots nearby. How about something else?", new ArrayList<>());
                } else {
                    // [BUG 5 FIX] Update session cache with fresh result
                    sessionCache.put(cacheKey, new CachedResult(places, intent));
                    callback.onSuggestionsReady("Great choice! Here are some " + intent + " spots nearby:", places);
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
}
