package com.rudra.myassistant;

import android.content.Context;
import android.content.SharedPreferences;
import com.rudra.myassistant.model.SuggestionCardModel;
import com.rudra.myassistant.suggestions.routing.SuggestionActionRouter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Merges, deduplicates, and prioritizes suggestion cards from all sources.
 *
 * Key design decisions:
 *   - NO generic preference cards (e.g., "Cafe near you" with no actual place).
 *     Those are useless. Instead, the SuggestionRepository now returns cards
 *     with real place names based on the user's preferences.
 *   - Anti-repetition tracking via SharedPreferences
 *   - Friend cards always have highest priority
 */
public class LiveSuggestionEngine {

    private static final String PREFS_NAME = "viper_suggestion_history";
    private static final String KEY_LAST_SHOWN = "last_shown_suggestions";
    private static final String KEY_LAST_ROTATE_INDEX = "rotate_index";
    private static final int MAX_HISTORY = 20;
    private final Random random = new Random();

    public List<SuggestionCardModel> buildLiveSuggestions(
            Context context, 
            List<SuggestionCardModel> locationCards,
            List<SuggestionCardModel> timeCards,
            List<String> userPreferences,
            List<String> nearbyFriends,
            List<SuggestionCardModel> assistantCards) {
            
        // No more generic preference cards — location cards already reflect preferences
        List<SuggestionCardModel> friendCards = generateFriendCards(nearbyFriends);

        return mergeSuggestions(context, locationCards, timeCards, friendCards, assistantCards);
    }

    public List<SuggestionCardModel> mergeSuggestions(
            Context context,
            List<SuggestionCardModel> locationCards,
            List<SuggestionCardModel> timeCards,
            List<SuggestionCardModel> friendCards,
            List<SuggestionCardModel> assistantCards) {

        List<SuggestionCardModel> combined = new ArrayList<>();
        
        // Priority order: friends > location (real places) > assistant > time
        if (friendCards != null) combined.addAll(friendCards);
        if (locationCards != null) combined.addAll(locationCards);
        if (assistantCards != null) combined.addAll(assistantCards);
        if (timeCards != null) combined.addAll(timeCards);

        // Deduplicate by title
        List<SuggestionCardModel> uniqueList = deduplicate(combined);

        // Filter out recently shown location cards for variety
        if (context != null) {
            uniqueList = filterRecentlyShown(context, uniqueList);
        }

        // Sort by Priority
        List<SuggestionCardModel> sortedList = buildSortedSuggestionList(uniqueList);

        // Cap at 5
        if (sortedList.size() > 5) {
            sortedList = new ArrayList<>(sortedList.subList(0, 5));
        }
        
        // If we have zero cards (unlikely), add time card as absolute fallback
        if (sortedList.isEmpty()) {
            SuggestionsManager.Suggestion ts = SuggestionsManager.getTimeBasedSuggestion();
            if (ts != null) {
                sortedList.add(AssistantSuggestionMapper.mapToModel(
                        "TIME_BASED", ts.title, ts.subtitle, "time"));
            }
        }

        // Record what we showed for anti-repetition
        if (context != null) {
            recordShownSuggestions(context, sortedList);
        }

        return sortedList;
    }

    public List<SuggestionCardModel> buildSortedSuggestionList(List<SuggestionCardModel> rawCards) {
        if (rawCards == null) return new ArrayList<>();

        Collections.sort(rawCards, new Comparator<SuggestionCardModel>() {
            @Override
            public int compare(SuggestionCardModel c1, SuggestionCardModel c2) {
                int p1 = getPriority(c1.getCardType());
                int p2 = getPriority(c2.getCardType());
                return Integer.compare(p1, p2);
            }

            private int getPriority(String type) {
                if (type == null) return 6;
                switch (type) {
                    case "FRIEND_NEARBY": return 1;
                    case "LOCATION_BASED": return 2;
                    case "REMINDER": return 3;
                    case "TIME_BASED": return 4;
                    case "FALLBACK": return 5;
                    default: return 6;
                }
            }
        });

        return rawCards;
    }

    private List<SuggestionCardModel> deduplicate(List<SuggestionCardModel> list) {
        List<SuggestionCardModel> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (SuggestionCardModel card : list) {
            if (card == null) continue;
            String title = card.getTitle() != null ? card.getTitle() : "Suggestion";
            String id = title.toLowerCase().trim();
            if (!seen.contains(id)) {
                result.add(card);
                seen.add(id);
            }
        }
        return result;
    }

    private List<SuggestionCardModel> generateFriendCards(List<String> friendIds) {
        List<SuggestionCardModel> cards = new ArrayList<>();
        if (friendIds == null) return cards;
        for (String uid : friendIds) {
            String name = SuggestionsManager.getNearbyFriendName(uid);
            String subtitle = name + " is nearby!";
            cards.add(AssistantSuggestionMapper.mapToModel("FRIEND_NEARBY", "Meetup Opportunity", subtitle, uid));
        }
        return cards;
    }

    // ─── ANTI-REPETITION SYSTEM ──────────────────────────────────────────

    /**
     * Filters out LOCATION_BASED suggestions whose exact title was shown recently.
     * Friend cards and time cards are never filtered.
     */
    private List<SuggestionCardModel> filterRecentlyShown(Context context, List<SuggestionCardModel> cards) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> recentlyShown = prefs.getStringSet(KEY_LAST_SHOWN, new HashSet<>());
        
        List<SuggestionCardModel> filtered = new ArrayList<>();
        int skippedCount = 0;
        
        for (SuggestionCardModel card : cards) {
            String type = card.getCardType();
            boolean isLocationCard = "LOCATION_BASED".equals(type);
            
            if (isLocationCard && card.getTitle() != null) {
                String key = card.getTitle().toLowerCase().trim();
                if (recentlyShown.contains(key) && skippedCount < 1) {
                    // Skip — we showed this exact place recently
                    skippedCount++;
                    continue;
                }
            }
            filtered.add(card);
        }
        
        // Safety: if we filtered everything, return originals
        if (filtered.isEmpty()) return cards;
        
        return filtered;
    }

    /** Records the current set of shown suggestions for anti-repetition tracking. */
    private void recordShownSuggestions(Context context, List<SuggestionCardModel> shown) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> keys = new HashSet<>();
        for (SuggestionCardModel card : shown) {
            if ("LOCATION_BASED".equals(card.getCardType()) && card.getTitle() != null) {
                keys.add(card.getTitle().toLowerCase().trim());
            }
        }
        
        // Keep history bounded
        if (keys.size() > MAX_HISTORY) {
            List<String> list = new ArrayList<>(keys);
            keys = new HashSet<>(list.subList(list.size() - MAX_HISTORY, list.size()));
        }
        
        prefs.edit().putStringSet(KEY_LAST_SHOWN, keys).apply();
    }
}
