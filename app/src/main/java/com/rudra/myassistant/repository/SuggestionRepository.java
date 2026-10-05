package com.rudra.myassistant.repository;

import com.rudra.myassistant.AssistantSuggestionMapper;
import com.rudra.myassistant.model.SuggestionCardModel;
import com.rudra.myassistant.network.ApiConfig;
import com.rudra.myassistant.network.GeoapifyApi;
import com.rudra.myassistant.network.PlacesSearchResponseDto;
import com.rudra.myassistant.network.RetrofitClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import retrofit2.Response;

/**
 * Fetches REAL, named, currently-relevant places and presents them as
 * suggestion cards with actual place names — not generic labels.
 *
 * Design principles:
 *   1. The TITLE of every card is the ACTUAL PLACE NAME (e.g., "Third Wave Coffee")
 *   2. Categories are filtered by time of day (no parks at 2 AM)
 *   3. We request "named" places via Geoapify conditions so we never get unnamed results
 *   4. Distance is shown in the subtitle so the user knows how far it is
 */
public class SuggestionRepository {
    private final Random random = new Random();

    /** Maps preference key → Geoapify API category */
    private static final Map<String, String> PREF_TO_CATEGORY = new HashMap<>();
    static {
        PREF_TO_CATEGORY.put("cafes",     "catering.cafe");
        PREF_TO_CATEGORY.put("food",      "catering.restaurant");
        PREF_TO_CATEGORY.put("gym",       "leisure.sports_centre");
        PREF_TO_CATEGORY.put("study",     "education.library");
        PREF_TO_CATEGORY.put("shopping",  "commercial.shopping_mall");
        PREF_TO_CATEGORY.put("movies",    "entertainment.cinema");
        PREF_TO_CATEGORY.put("parks",     "leisure.park");
        PREF_TO_CATEGORY.put("desserts",  "catering.confectionery");
        PREF_TO_CATEGORY.put("hangout",   "tourism.attraction");
        PREF_TO_CATEGORY.put("music",     "entertainment.culture");
        PREF_TO_CATEGORY.put("sports",    "leisure.stadium");
        PREF_TO_CATEGORY.put("gaming",    "entertainment.video_arcade");
        PREF_TO_CATEGORY.put("tech",      "commercial.electronics");
    }

    /** Categories that are NEVER appropriate at night (10 PM – 6 AM) */
    private static final Set<String> DAYTIME_ONLY_CATEGORIES = new HashSet<>();
    static {
        DAYTIME_ONLY_CATEGORIES.add("leisure.park");
        DAYTIME_ONLY_CATEGORIES.add("education.library");
        DAYTIME_ONLY_CATEGORIES.add("tourism.attraction");
        DAYTIME_ONLY_CATEGORIES.add("leisure.stadium");
        DAYTIME_ONLY_CATEGORIES.add("entertainment.culture");
        DAYTIME_ONLY_CATEGORIES.add("entertainment.video_arcade");
        DAYTIME_ONLY_CATEGORIES.add("commercial.shopping_mall");
        DAYTIME_ONLY_CATEGORIES.add("commercial.electronics");
    }

    /** Tag chip labels per category */
    private static final Map<String, String> CATEGORY_TAG = new HashMap<>();
    static {
        CATEGORY_TAG.put("catering.cafe",            "Cafe");
        CATEGORY_TAG.put("catering.restaurant",      "Dining");
        CATEGORY_TAG.put("leisure.sports_centre",    "Fitness");
        CATEGORY_TAG.put("education.library",        "Study");
        CATEGORY_TAG.put("commercial.shopping_mall", "Shopping");
        CATEGORY_TAG.put("entertainment.cinema",     "Cinema");
        CATEGORY_TAG.put("leisure.park",             "Nature");
        CATEGORY_TAG.put("catering.confectionery",   "Desserts");
        CATEGORY_TAG.put("tourism.attraction",       "Explore");
        CATEGORY_TAG.put("entertainment.culture",    "Culture");
        CATEGORY_TAG.put("leisure.stadium",          "Sports");
        CATEGORY_TAG.put("entertainment.video_arcade","Gaming");
        CATEGORY_TAG.put("commercial.electronics",   "Tech");
        CATEGORY_TAG.put("catering.fast_food",       "Late Eats");
    }

    /** Emoji per category */
    private static final Map<String, String> CATEGORY_EMOJI = new HashMap<>();
    static {
        CATEGORY_EMOJI.put("catering.cafe",            "☕");
        CATEGORY_EMOJI.put("catering.restaurant",      "🍴");
        CATEGORY_EMOJI.put("leisure.sports_centre",    "💪");
        CATEGORY_EMOJI.put("education.library",        "📚");
        CATEGORY_EMOJI.put("commercial.shopping_mall", "🛍️");
        CATEGORY_EMOJI.put("entertainment.cinema",     "🎬");
        CATEGORY_EMOJI.put("leisure.park",             "🌳");
        CATEGORY_EMOJI.put("catering.confectionery",   "🍰");
        CATEGORY_EMOJI.put("tourism.attraction",       "🤝");
        CATEGORY_EMOJI.put("entertainment.culture",    "🎵");
        CATEGORY_EMOJI.put("leisure.stadium",          "🏟️");
        CATEGORY_EMOJI.put("entertainment.video_arcade","🎮");
        CATEGORY_EMOJI.put("commercial.electronics",   "⚙️");
        CATEGORY_EMOJI.put("catering.fast_food",       "🍔");
    }

    public SuggestionRepository() {}

    /**
     * Returns suggestion cards with ACTUAL PLACE NAMES as titles.
     * Filters categories by time of day so parks aren't suggested at 2 AM.
     */
    public List<SuggestionCardModel> getLocationSuggestions(double lat, double lon, List<String> userPrefs) {
        if (lat == 0 && lon == 0) return new ArrayList<>(); // No location = no location cards
        
        List<String> categories = resolveCategories(userPrefs);
        if (categories.isEmpty()) return new ArrayList<>();
        
        // Pick up to 2 random categories for variety
        Collections.shuffle(categories, random);
        int pick = Math.min(2, categories.size());
        List<String> selected = new ArrayList<>(categories.subList(0, pick));
        
        String categoryQuery = String.join(",", selected);
        return fetchRealPlaces(lat, lon, categoryQuery, selected);
    }

    /** Backward-compatible overload */
    public List<SuggestionCardModel> getLocationSuggestions(double lat, double lon) {
        return getLocationSuggestions(lat, lon, null);
    }

    /**
     * Converts user preferences → Geoapify categories, then filters out
     * categories that make no sense at the current time of day.
     */
    private List<String> resolveCategories(List<String> prefs) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        boolean isNight = (hour >= 22 || hour < 6);
        
        List<String> cats = new ArrayList<>();
        
        if (prefs != null && !prefs.isEmpty()) {
            for (String p : prefs) {
                String cat = PREF_TO_CATEGORY.get(p);
                if (cat != null) {
                    // Filter out daytime-only categories at night
                    if (isNight && DAYTIME_ONLY_CATEGORIES.contains(cat)) continue;
                    cats.add(cat);
                }
            }
        }
        
        // Fallback: time-aware defaults
        if (cats.isEmpty()) {
            if (hour >= 6 && hour < 11) {
                cats.add("catering.cafe");
            } else if (hour >= 11 && hour < 17) {
                cats.add("catering.restaurant");
                cats.add("catering.cafe");
            } else if (hour >= 17 && hour < 22) {
                cats.add("catering.restaurant");
                cats.add("entertainment.cinema");
            } else {
                // Late night: only food that's likely open
                cats.add("catering.restaurant");
                cats.add("catering.fast_food");
            }
        }
        
        return cats;
    }

    /**
     * Fetches real places from Geoapify with:
     *   - conditions=named (only places that have an actual name)
     *   - Title = the actual place name
     *   - Subtitle = distance + category context
     */
    private List<SuggestionCardModel> fetchRealPlaces(double lat, double lon, String categories, List<String> selectedCats) {
        List<SuggestionCardModel> result = new ArrayList<>();
        String filter = "circle:" + lon + "," + lat + ",5000";
        GeoapifyApi api = RetrofitClient.getGeoapifyInstance().create(GeoapifyApi.class);
        
        try {
            // Use conditions=named to only get places that have an actual name
            Response<PlacesSearchResponseDto> response = api.searchNearbyWithConditions(
                    categories, filter, "named", 10, ApiConfig.GEOAPIFY_API_KEY
            ).execute();
            
            if (response.isSuccessful() && response.body() != null 
                    && response.body().getFeatures() != null 
                    && !response.body().getFeatures().isEmpty()) {
                
                List<PlacesSearchResponseDto.Feature> features = response.body().getFeatures();
                
                // Shuffle for variety
                Collections.shuffle(features, random);
                
                int added = 0;
                for (PlacesSearchResponseDto.Feature f : features) {
                    if (added >= 2) break; // Cap at 2 location cards
                    
                    String name = f.getProperties().getName();
                    if (name == null || name.trim().isEmpty()) continue;
                    
                    // Get distance
                    float distMeters = f.getProperties().getDistance();
                    String distText = formatDistance(distMeters);
                    
                    // Determine category for emoji/tag
                    String matchedCat = detectCategory(f, selectedCats);
                    String emoji = CATEGORY_EMOJI.getOrDefault(matchedCat, "📍");
                    String tag = CATEGORY_TAG.getOrDefault(matchedCat, "Nearby");
                    
                    // Build the card with the REAL PLACE NAME as the title
                    String title = name;  // "Third Wave Coffee", not "Cafe Nearby"
                    String subtitle = emoji + " " + tag + "  •  " + distText;
                    String description = getContextualDescription(matchedCat);
                    
                    SuggestionCardModel card = AssistantSuggestionMapper.mapToModel(
                            "LOCATION_BASED", title, subtitle, name);
                    card.setDescription(description);
                    result.add(card);
                    added++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // NO generic fallback — if we can't find real places, return empty.
        // The engine will still show time/friend/assistant cards.
        return result;
    }

    /** Formats distance in human-readable form */
    private String formatDistance(float meters) {
        if (meters <= 0) return "Nearby";
        if (meters < 100) return "Just around the corner";
        if (meters < 1000) return (int) meters + "m away";
        return String.format("%.1f km away", meters / 1000.0);
    }

    /** Detects which selected category best matches a feature */
    private String detectCategory(PlacesSearchResponseDto.Feature f, List<String> selectedCats) {
        if (f.getProperties().getCategories() != null) {
            for (String fCat : f.getProperties().getCategories()) {
                if (selectedCats.contains(fCat)) return fCat;
                // Also check parent category match
                for (String sel : selectedCats) {
                    if (fCat.startsWith(sel) || sel.startsWith(fCat)) return sel;
                }
            }
        }
        return selectedCats.isEmpty() ? "catering.restaurant" : selectedCats.get(0);
    }

    /** Returns a contextual description based on category and time */
    private String getContextualDescription(String category) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        
        if (category.contains("cafe")) {
            if (hour < 11) return "Perfect spot for your morning coffee";
            if (hour < 17) return "Take a break with a good brew";
            return "Wind down with a warm cup";
        }
        if (category.contains("restaurant") || category.contains("fast_food")) {
            if (hour < 11) return "Start the day with a great meal";
            if (hour < 17) return "Highly rated dining option nearby";
            if (hour < 22) return "Great dinner option right around the corner";
            return "Open late — grab a bite now";
        }
        if (category.contains("cinema")) return "Check what's currently playing";
        if (category.contains("sports_centre")) return "Stay consistent with your fitness goals";
        if (category.contains("library")) return "Quiet environment for focused work";
        if (category.contains("shopping")) return "Browse what's trending nearby";
        if (category.contains("park")) return "Fresh air and green spaces nearby";
        if (category.contains("confectionery")) return "Treat yourself — you've earned it";
        
        return "Worth checking out";
    }
}
