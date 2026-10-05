package com.rudra.myassistant;

import android.content.Context;
import android.content.Intent;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import com.rudra.myassistant.network.AIService;
import com.rudra.myassistant.network.PromptManager;
import com.rudra.myassistant.network.ApiConfig;
import com.rudra.myassistant.network.GeoapifyApi;
import com.rudra.myassistant.network.PlacesSearchResponseDto;
import com.rudra.myassistant.network.RetrofitClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Modern Intelligence Engine for dashboard suggestions.
 * Centralizes rules, data context, and AI-driven insights.
 */
public class SuggestionsManager {

    public static class Suggestion {
        public String title;
        public String titleEmoji;
        public String subtitle;
        public List<String> bulletActions = new ArrayList<>();
        public String type; // e.g., "time", "data", "ai", "location"
        public Class<?> actionActivity;
        
        // New interactive/location fields
        public String placeName;
        public Double latitude;
        public Double longitude;
        public String actionType; // e.g., "OPEN_LOCATION", "OPEN_CHAT", "CREATE_REMINDER"
        public String description;
        
        // Redesign fields
        public String tagChip; // e.g., "Recommended for you", "Nearby"
        public int layoutType; // 1-5
        public String mediaUrl; // For image previews
        public String categoryIcon; // For layout V1
        public String categoryBackground; // For layout V4
        
        // Hangout specific
        public String friendUid;
        public String friendName;
        public com.rudra.myassistant.model.LatLng friendLatLng;
        public boolean isPlanHangoutEnabled = false;

        public Suggestion(String title, String titleEmoji, String subtitle, String type) {
            this.title = title;
            this.titleEmoji = titleEmoji;
            this.subtitle = subtitle;
            this.type = type;
            this.layoutType = 5; // Default emoji style
        }

        public Suggestion(String title, String titleEmoji, String placeName, String description, double lat, double lon) {
            this.title = title;
            this.titleEmoji = titleEmoji;
            this.placeName = placeName;
            this.subtitle = description; // Show contextual description as subtitle
            this.description = description;
            this.latitude = lat;
            this.longitude = lon;
            this.type = "location";
            this.actionType = "OPEN_LOCATION";
            this.layoutType = (int) (Math.random() * 4) + 1; // Randomly select layouts 1-4
        }

    }

    public interface SuggestionListener {
        void onSuggestionReady(Suggestion suggestion);
        void onError(String error);
    }

    private static Suggestion cachedSuggestion;
    private static long lastFetchTime = 0;
    private static final long CACHE_DURATION = 10 * 60 * 1000; // 10 minutes — shorter for more variety

    private static List<String> nearbyFriends = new ArrayList<>();

    // [BUG 1 FIX] Map of friendUid → display name, populated by ConnectActivity
    private static java.util.Map<String, String> nearbyFriendNames = new java.util.concurrent.ConcurrentHashMap<>();
    // [BUG 1 FIX] Map of friendUid → LatLng, populated by ConnectActivity
    private static java.util.Map<String, com.rudra.myassistant.model.LatLng> nearbyFriendLocations = new java.util.concurrent.ConcurrentHashMap<>();

    public static List<String> getNearbyFriends() {
        return nearbyFriends != null ? nearbyFriends : new ArrayList<>();
    }

    public static void setNearbyFriends(List<String> friendIds) {
        nearbyFriends = friendIds != null ? friendIds : new ArrayList<>();
        invalidateCache(); // Allow fresh generation with new context
    }

    /**
     * [BUG 1 FIX] Called by ConnectActivity to populate friend name + location for suggestion cards.
     */
    public static String getNearbyFriendName(String friendUid) {
        if (friendUid == null) return "your friend";
        return nearbyFriendNames.containsKey(friendUid) ? nearbyFriendNames.get(friendUid) : "your friend";
    }

    public static com.rudra.myassistant.model.LatLng getNearbyFriendLocation(String friendUid) {
        if (friendUid == null) return null;
        return nearbyFriendLocations.get(friendUid);
    }

    public static void setNearbyFriendContext(String friendUid, String friendName, double lat, double lng) {
        if (friendUid != null) {
            nearbyFriendNames.put(friendUid, friendName != null ? friendName : "your friend");
            if (lat != 0 && lng != 0) {
                nearbyFriendLocations.put(friendUid, new com.rudra.myassistant.model.LatLng(lat, lng));
            }
        }
    }

    public static void invalidateCache() {
        lastFetchTime = 0;
        cachedSuggestion = null;
    }

    public static List<Suggestion> generateSuggestions(Context context, List<com.google.firebase.firestore.QueryDocumentSnapshot> reminders, List<com.google.firebase.firestore.QueryDocumentSnapshot> notes, List<ChatHistory> chats, Set<String> preferences) {
        List<Suggestion> results = new ArrayList<>();
        
        // 1. Priority Context Card (Time)
        results.add(getTimeBasedSuggestion());

        // 2. Preference-Based Logic (Reminders/Notes)
        // Only show if user has relevant preferences or general productivity interests
        boolean showData = preferences == null || preferences.isEmpty() || preferences.contains("productivity") || preferences.contains("study");

        if (showData) {
            if (reminders != null && !reminders.isEmpty()) {
                Suggestion s = new Suggestion("Upcoming Tasks", "🕒", "Focus on what matters:", "data");
                s.bulletActions.add("Review " + reminders.size() + " active reminders");
                s.bulletActions.add("Plan your schedule");
                s.actionActivity = RemindersActivity.class;
                s.tagChip = "Recommended";
                results.add(s);
            }

            if (notes != null && !notes.isEmpty()) {
                Suggestion s = new Suggestion("Recent Ideas", "💡", "Your recent thoughts:", "data");
                s.bulletActions.add(notes.get(0).getString("title"));
                s.actionActivity = NotesActivity.class;
                s.tagChip = "Notes";
                results.add(s);
            }
        }

        // 3. Chat Keyword Scanning (Localized Insight)
        Suggestion chatSuggestion = scanChatsForKeywords(chats);
        if (chatSuggestion != null && showData) {
            results.add(chatSuggestion);
        }

        // 4. Friend Nearby Context Signal — [BUG 1 FIX] Use real friend name + location
        if (!nearbyFriends.isEmpty()) {
            String friendUid = nearbyFriends.get(0);
            // [BUG 1 FIX] Resolve actual friend name from the context map
            String friendName = nearbyFriendNames.containsKey(friendUid)
                    ? nearbyFriendNames.get(friendUid) : "your friend";
            
            // [BUG 1 FIX] Build subtitle with real name: "Rahul is nearby!" instead of "Your friend is nearby!"
            String subtitle = friendName + " is nearby!";
            
            Suggestion meetup = new Suggestion("Meetup Opportunity", "🤝", subtitle, "FRIEND_NEARBY");
            meetup.bulletActions.add("Suggest a hangout spot");
            meetup.tagChip = "FRIEND_NEARBY";
            meetup.layoutType = 1; // High importance
            meetup.isPlanHangoutEnabled = true;
            meetup.friendUid = friendUid;
            meetup.friendName = friendName;
            
            // [BUG 1 FIX] Populate friendLatLng so HangoutChatInitializer gets real coordinates
            if (nearbyFriendLocations.containsKey(friendUid)) {
                meetup.friendLatLng = nearbyFriendLocations.get(friendUid);
            }
            
            results.add(0, meetup); // Inject at the top
        }

        return results;
    }

    public static Suggestion getHistorySuggestion(String summary) {
        return new Suggestion("Insight", "🧠", summary, "ai");
    }

    public static Suggestion getExpenseSuggestion(String friendName, double amount) {
        Suggestion s = new Suggestion("Debt Reminder", "💸", "You owe " + friendName + " ₹" + (int)amount, "expense");
        s.bulletActions.add("Settle now");
        return s;
    }

    public static Suggestion getEmptyStateSuggestion() {
        Suggestion setup = new Suggestion("Personalize your Feed", "✨", "Choose your interests to receive smarter suggestions.", "setup");
        setup.tagChip = "Get Started";
        setup.actionType = "OPEN_PREFERENCES";
        setup.layoutType = 5; // Emoji focused
        return setup;
    }

    public static Suggestion getNearbyFriendSuggestion(String friendUid) {
        if (friendUid == null) return null;
        String friendName = nearbyFriendNames.containsKey(friendUid)
                ? nearbyFriendNames.get(friendUid) : "your friend";
        
        String subtitle = friendName + " is nearby!";
        Suggestion meetup = new Suggestion("Meetup Opportunity", "🤝", subtitle, "FRIEND_NEARBY");
        meetup.bulletActions.add("Suggest a hangout spot");
        meetup.tagChip = "FRIEND_NEARBY";
        meetup.layoutType = 1;
        meetup.isPlanHangoutEnabled = true;
        meetup.friendUid = friendUid;
        meetup.friendName = friendName;
        
        if (nearbyFriendLocations.containsKey(friendUid)) {
            meetup.friendLatLng = nearbyFriendLocations.get(friendUid);
        }
        return meetup;
    }

    public static Suggestion getTimeBasedSuggestion() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK); // 1-7, different each day
        
        if (hour >= 5 && hour < 11) {
            String[][] variants = {
                {"Morning Routine", "☀️", "Kickstart your day:"},
                {"Rise & Shine", "🌅", "A productive morning starts now:"},
                {"Good Morning", "☀️", "Here's what your day looks like:"},
                {"Fresh Start", "🌤️", "Plan your morning for success:"},
                {"AM Gameplan", "🗓️", "Set your priorities for today:"},
                {"Dawn Patrol", "🌄", "Early bird gets the worm:"},
                {"Morning Mindset", "☀️", "What matters most today?"}
            };
            int idx = day % variants.length;
            Suggestion s = new Suggestion(variants[idx][0], variants[idx][1], variants[idx][2], "time");
            s.bulletActions.add("Check today's schedule");
            s.actionActivity = CalendarActivity.class;
            s.tagChip = "Daily Digest";
            s.layoutType = 5;
            return s;
        } else if (hour >= 11 && hour < 17) {
            String[][] variants = {
                {"Afternoon Focus", "☕", "Keep the momentum:"},
                {"Midday Check-in", "📋", "How's your progress today?"},
                {"Power Hour", "⚡", "Deep focus for maximum output:"},
                {"Stay Sharp", "🎯", "Your afternoon productivity hub:"},
                {"Halfway There", "🔄", "Review what's done, plan what's next:"},
                {"PM Sprint", "🏃", "Crush the remaining tasks:"},
                {"Focus Time", "🧠", "Minimize distractions, maximize output:"}
            };
            int idx = day % variants.length;
            Suggestion s = new Suggestion(variants[idx][0], variants[idx][1], variants[idx][2], "time");
            s.bulletActions.add("Follow up on tasks");
            s.actionActivity = RemindersActivity.class;
            s.tagChip = "Productivity";
            s.layoutType = 5;
            return s;
        } else if (hour >= 17 && hour < 21) {
            String[][] variants = {
                {"Evening Glow", "🌆", "Reflect on today:"},
                {"Golden Hour", "🌇", "Wind down and review your progress:"},
                {"Evening Recap", "📊", "What did you accomplish today?"},
                {"Sunset Check-in", "🌅", "Time to wrap up and relax:"},
                {"Day's End", "✅", "Review, reflect, and reset:"},
                {"Evening Mode", "🎶", "Transition from work to rest:"},
                {"Twilight Brief", "🌙", "Your evening at a glance:"}
            };
            int idx = day % variants.length;
            Suggestion s = new Suggestion(variants[idx][0], variants[idx][1], variants[idx][2], "time");
            s.bulletActions.add("Review your progress");
            s.actionActivity = CalendarActivity.class;
            s.tagChip = "Reflection";
            s.layoutType = 5;
            return s;
        } else {
            String[][] variants = {
                {"Late Night Prep", "🌙", "Sleep well, plan ahead:"},
                {"Nightowl Mode", "🦉", "Wrapping up? Plan tomorrow now:"},
                {"Wind Down", "😴", "Tomorrow is a fresh start:"},
                {"Sleep Timer", "💤", "Rest is part of the plan:"},
                {"Night Recap", "📝", "Jot down final thoughts before bed:"},
                {"Lights Out Soon", "🌑", "Set your alarms and rest:"},
                {"Dream Plan", "✨", "Let your brain process today's wins:"}
            };
            int idx = day % variants.length;
            Suggestion s = new Suggestion(variants[idx][0], variants[idx][1], variants[idx][2], "time");
            s.bulletActions.add("Set tomorrow's wake up");
            s.actionActivity = HealthActivity.class;
            s.layoutType = 5;
            return s;
        }
    }


    private static Suggestion scanChatsForKeywords(List<ChatHistory> chats) {
        if (chats == null || chats.isEmpty()) return null;
        
        // Scan last 5 chats
        for (int i = 0; i < Math.min(5, chats.size()); i++) {
            String lastText = chats.get(i).getLastMessage();
            String titleText = chats.get(i).getTitle();
            
            String last = lastText != null ? lastText.toLowerCase() : "";
            String title = titleText != null ? titleText.toLowerCase() : "";
            
            if (last.contains("travel") || title.contains("travel")) {
                Suggestion s = new Suggestion("Travel Planning?", "✈️", "Based on your chat:", "chat");
                s.bulletActions.add("Save flight/hotel details");
                s.bulletActions.add("Create trip reminder");
                s.actionActivity = NotesActivity.class;
                return s;
            }
            if (last.contains("study") || title.contains("study") || last.contains("exam")) {
                Suggestion s = new Suggestion("Focus Session?", "📚", "Preparing for something?", "chat");
                s.bulletActions.add("Create a study note");
                s.bulletActions.add("Set focus timer");
                s.actionActivity = NotesActivity.class;
                return s;
            }
            if (last.contains("meeting") || title.contains("meeting")) {
                Suggestion s = new Suggestion("Upcoming Event?", "📅", "Don't miss a beat:", "chat");
                s.bulletActions.add("Add to your calendar");
                s.bulletActions.add("Set prep reminder");
                s.actionActivity = CalendarActivity.class;
                return s;
            }
        }
        return null;
    }

    public static void fetchAiInsight(Context context, List<com.google.firebase.firestore.QueryDocumentSnapshot> reminders, List<com.google.firebase.firestore.QueryDocumentSnapshot> notes, List<ChatHistory> chats, AIService.AIResponseListener listener) {
        StringBuilder sb = new StringBuilder();
        sb.append("Current context: ");
        if (reminders != null) sb.append(reminders.size()).append(" reminders. ");
        if (notes != null) sb.append(notes.size()).append(" notes. ");
        if (chats != null && !chats.isEmpty()) {
            sb.append("Last chat topic: ").append(chats.get(0).getTitle());
        }

        String prompt = "Based on the user context provided, generate ONE short, actionable personal insight or tip for their dashboard. " +
                       "Format: Title|Subtitle|Bullet1|Bullet2. Keep it under 10 words total. " +
                       "Context: " + sb.toString();

        new AIService().askAssistant(prompt, null, new AIService.AIResponseListener() {
            @Override
            public void onSuccess(String response) {
                try {
                    String[] parts = response.split("\\|");
                    if (parts.length >= 2) {
                        Suggestion s = new Suggestion(parts[0].trim(), "🧠", parts[1].trim(), "ai");
                        for (int i = 2; i < parts.length; i++) {
                            s.bulletActions.add(parts[i].trim());
                        }
                        listener.onSuccess(response); 
                    }
                } catch (Exception e) {
                    listener.onFailure(e.getMessage());
                }
            }
            @Override public void onToolCall(String fn, JsonObject args) {}
            @Override public void onFailure(String err) { listener.onFailure(err); }
        });
    }

    public static void fetchNearbySuggestion(Context context, double lat, double lon, Set<String> preferences, SuggestionListener listener) {
        if (System.currentTimeMillis() - lastFetchTime < CACHE_DURATION && cachedSuggestion != null) {
            listener.onSuggestionReady(cachedSuggestion);
            return;
        }

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        boolean isNight = (hour >= 22 || hour < 6);
        String category, title, emoji, description, tag;

        // Step 1: Filter categories based on preferences
        List<String> pool = new ArrayList<>();
        if (preferences != null && !preferences.isEmpty()) {
            for (String p : preferences) {
                switch (p) {
                    case "cafes": pool.add("catering.cafe"); break;
                    case "food": pool.add("catering.restaurant"); break;
                    case "gym": if (!isNight) pool.add("leisure.sports_centre"); break;
                    case "study": if (!isNight) pool.add("education.library"); break;
                    case "shopping": if (!isNight) pool.add("commercial.shopping_mall"); break;
                    case "movies": pool.add("entertainment.cinema"); break;
                    case "parks": if (!isNight) pool.add("leisure.park"); break;
                    case "desserts": pool.add("catering.confectionery"); break;
                    case "hangout": if (!isNight) pool.add("tourism.attraction"); break;
                    case "music": if (!isNight) pool.add("entertainment.culture"); break;
                    case "sports": if (!isNight) pool.add("leisure.stadium"); break;
                    case "gaming": if (!isNight) pool.add("entertainment.video_arcade"); break;
                    case "tech": if (!isNight) pool.add("commercial.electronics"); break;
                }
            }
        }

        // Default or fallback logic — time-aware
        if (pool.isEmpty()) {
            if (hour >= 6 && hour < 11) {
                pool.add("catering.cafe");
            } else if (hour >= 11 && hour < 17) {
                pool.add("catering.restaurant");
                pool.add("catering.cafe");
            } else if (hour >= 17 && hour < 22) {
                pool.add("catering.restaurant");
                pool.add("entertainment.cinema");
                pool.add("leisure.park");
            } else {
                // Late night: only food that's actually open
                pool.add("catering.restaurant");
                pool.add("catering.fast_food");
            }
        }

        // Randomly select for variety
        category = pool.get((int)(Math.random() * pool.size()));


        // Special Case: Mock Live Matches if Sports selected
        if (category.equals("leisure.stadium") && Math.random() > 0.4) {
            String[] matches = {
                "Warriors vs Lakers|🏀|NBA Season|LIVE 102-98",
                "Cricket Match|🏏|IPL Season|LIVE 145/4 (16.2)",
                "Football Match|⚽|Premier League|LIVE 2-1 (65')",
                "US Open|🎾|City Stadium|Grand Slam Finals"
            };
            String[] parts = matches[(int)(Math.random() * matches.length)].split("\\|");
            cachedSuggestion = new Suggestion(parts[0], parts[1], parts[2], parts[3], lat, lon);
            cachedSuggestion.tagChip = "LIVE SCORES";
            cachedSuggestion.layoutType = 6; // Special Match Layout
            cachedSuggestion.actionActivity = ChatActivity.class;
            cachedSuggestion.actionType = "OPEN_CHAT";
            lastFetchTime = System.currentTimeMillis();
            listener.onSuggestionReady(cachedSuggestion);
            return;
        }

        // Step 2: Set category-aware attributes for visual variety
        if (category.equals("catering.cafe")) {
            title = "Morning Coffee";
            emoji = "☕";
            description = "Start your day with a perfect brew.";
            tag = "Early Birds";
        } else if (category.equals("catering.restaurant")) {
            title = hour < 15 ? "Lunch Spot" : "Dinner Time";
            emoji = "🍴";
            description = "Discover the best local table.";
            tag = "Foodie Alert";
        } else if (category.equals("leisure.sports_centre")) {
            title = "Workout Time";
            emoji = "💪";
            description = "Stay on track. Keep the momentum.";
            tag = "Fitness Mode";
        } else if (category.equals("education.library")) {
            title = "Deep Focus";
            emoji = "📚";
            description = "Quiet spots for your deep work.";
            tag = "Study Session";
        } else if (category.equals("commercial.shopping_mall")) {
            title = "Shopping Time";
            emoji = "🛍️";
            description = "Best shopping locations nearby.";
            tag = "Retail Therapy";
        } else if (category.equals("entertainment.cinema")) {
            title = "Movie Night?";
            emoji = "🎬";
            description = "Catch the latest releases tonight.";
            tag = "Cinema";
        } else if (category.equals("leisure.park")) {
            title = "Evening Walk";
            emoji = "🌳";
            description = "Relax and recharge in nature.";
            tag = "Nature";
        } else if (category.equals("catering.confectionery")) {
            title = "Sweet Treat";
            emoji = "🍰";
            description = "Grab a quick dessert nearby.";
            tag = "Sweet Tooth";
        } else if (category.equals("tourism.attraction")) {
            title = "Hangout Spot";
            emoji = "🤝";
            description = "Great places for social vibes.";
            tag = "Social Hub";
        } else if (category.equals("entertainment.video_arcade")) {
            title = "Nearby Arcade";
            emoji = "🎮";
            description = "Ignite your competitive spirit.";
            tag = "Fun Area";
        } else if (category.equals("commercial.electronics")) {
            title = "Nearby Tech Store";
            emoji = "⚙️";
            description = "Explore latest gadgets and gear.";
            tag = "Tech Discovery";
        } else if (hour >= 17 && hour < 22) {
            title = "Evening Spotlight";
            emoji = "🌆";
            tag = "Night Out";
            description = "Perfect spot for your evening.";
        } else {
            title = "Late Night Vibe";
            emoji = "🌙";
            tag = "Open Now";
            description = "Found a spot for your late night cravings.";
        }

        String filter = "circle:" + lon + "," + lat + ",5000";
        GeoapifyApi api = RetrofitClient.getGeoapifyInstance().create(GeoapifyApi.class);
        
        final String finalCategory = category;
        final String finalTitle = title;
        final String finalEmoji = emoji;
        final String finalDescription = description;
        final String finalTag = tag;

        api.searchNearbyWithConditions(finalCategory, filter, "named", 8, ApiConfig.GEOAPIFY_API_KEY).enqueue(new retrofit2.Callback<PlacesSearchResponseDto>() {
            @Override
            public void onResponse(retrofit2.Call<PlacesSearchResponseDto> call, retrofit2.Response<PlacesSearchResponseDto> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().getFeatures().isEmpty()) {
                    // Filter to only named results and pick randomly
                    List<PlacesSearchResponseDto.Feature> namedFeatures = new ArrayList<>();
                    for (PlacesSearchResponseDto.Feature f : response.body().getFeatures()) {
                        if (f.getProperties().getName() != null && !f.getProperties().getName().trim().isEmpty()) {
                            namedFeatures.add(f);
                        }
                    }
                    
                    if (namedFeatures.isEmpty()) {
                        listener.onSuggestionReady(null);
                        return;
                    }
                    
                    int randomIndex = (int)(Math.random() * namedFeatures.size());
                    PlacesSearchResponseDto.Feature feature = namedFeatures.get(randomIndex);
                    String name = feature.getProperties().getName();
                    
                    double pLat = feature.getGeometry().getLat();
                    double pLon = feature.getGeometry().getLon();
                    
                    // Use the ACTUAL PLACE NAME as the title, category info as subtitle
                    cachedSuggestion = new Suggestion(name, finalEmoji, finalDescription, finalTag, pLat, pLon);
                    cachedSuggestion.tagChip = finalTag;
                    cachedSuggestion.placeName = name;
                    
                    // Assign Layout specific assets
                    cachedSuggestion.categoryIcon = getIconForCategory(finalCategory);
                    cachedSuggestion.layoutType = (int)(Math.random() * 4) + 1; // Layouts 1-4 for variety
                    
                    lastFetchTime = System.currentTimeMillis();
                    listener.onSuggestionReady(cachedSuggestion);
                } else {
                    // No nearby places found for selected categories
                    listener.onSuggestionReady(null);
                }
            }

            @Override public void onFailure(retrofit2.Call<PlacesSearchResponseDto> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    public static Suggestion fromFeature(PlacesSearchResponseDto.Feature feature, String source) {
        if (feature == null) return null;
        String name = feature.getProperties().getName();
        if (name == null) return null;

        double lat = feature.getGeometry().getLat();
        double lon = feature.getGeometry().getLon();
        String cat = feature.getProperties().getCategories().get(0);

        String title = "Nearby Spot";
        String emoji = "📍";
        String desc = "Interesting place currently open.";
        String tag = "Explore";

        if (cat.contains("cafe")) {
            title = "Morning Coffee"; emoji = "☕"; desc = "Start your day with a perfect brew."; tag = "Early Birds";
        } else if (cat.contains("restaurant")) {
            title = "Dining Spot"; emoji = "🍴"; desc = "Discover the best local table."; tag = "Foodie Alert";
        } else if (cat.contains("sports_centre")) {
            title = "Workout Time"; emoji = "💪"; desc = "Stay on track. Keep the momentum."; tag = "Fitness Mode";
        } else if (cat.contains("library")) {
            title = "Deep Focus"; emoji = "📚"; desc = "Quiet spots for your deep work."; tag = "Study Session";
        } else if (cat.contains("shopping")) {
            title = "Shopping Time"; emoji = "🛍️"; desc = "Best shopping locations nearby."; tag = "Retail Therapy";
        } else if (cat.contains("cinema")) {
            title = "Movie Night?"; emoji = "🎬"; desc = "Catch the latest releases tonight."; tag = "Cinema";
        } else if (cat.contains("park")) {
            title = "Evening Walk"; emoji = "🌳"; desc = "Relax and recharge in nature."; tag = "Nature";
        } else if (cat.contains("confectionery")) {
            title = "Sweet Treat"; emoji = "🍰"; desc = "Grab a quick dessert nearby."; tag = "Sweet Tooth";
        } else if (cat.contains("attraction")) {
            title = "Hangout Spot"; emoji = "🤝"; desc = "Great places for social vibes."; tag = "Social Hub";
        }

        Suggestion s = new Suggestion(title, emoji, name, desc, lat, lon);
        s.tagChip = tag;
        s.categoryIcon = getIconForCategory(cat);
        return s;
    }

    private static String getIconForCategory(String cat) {
        if (cat.contains("cafe")) return "ic_cafe";
        if (cat.contains("restaurant")) return "ic_food";
        if (cat.contains("park")) return "ic_globe";
        if (cat.contains("cinema")) return "ic_more";
        return "ic_search";
    }

}
