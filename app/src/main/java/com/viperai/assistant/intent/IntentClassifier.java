package com.viperai.assistant.intent;

import java.util.ArrayList;
import java.util.List;

public class IntentClassifier {

    public IntentResult classify(String message) {
        if (message == null || message.trim().isEmpty()) {
            return new IntentResult(IntentConstants.UNKNOWN, 0.0f, "");
        }

        String query = message.toLowerCase().trim();
        List<IntentResult> matches = new ArrayList<>();

        // FRIENDS_NEARBY
        matches.add(checkFriendsNearby(query));

        // EXPENSE_STATUS
        matches.add(checkExpenseStatus(query));

        // REMINDER_QUERY
        matches.add(checkReminderQuery(query));

        // NOTE_QUERY
        matches.add(checkNoteQuery(query));

        // PLACE_SUGGESTION
        matches.add(checkPlaceSuggestion(query));

        // PLAN_HANGOUT
        matches.add(checkPlanHangout(query));

        // NAVIGATION_REQUEST
        matches.add(checkNavigationRequest(query));

        // CREATE_NOTE
        matches.add(checkCreateNote(query));

        // CREATE_REMINDER
        matches.add(checkCreateReminder(query));

        // CREATE_CALENDAR_EVENT
        matches.add(checkCreateCalendarEvent(query));

        // GENERAL_SUGGESTION
        matches.add(checkGeneralSuggestion(query));

        IntentResult bestMatch = new IntentResult(IntentConstants.UNKNOWN, 0.0f, query);
        for (IntentResult res : matches) {
            if (res.getConfidence() > bestMatch.getConfidence()) {
                bestMatch = res;
            }
        }

        if (bestMatch.getConfidence() < 0.55f) {
            return new IntentResult(IntentConstants.UNKNOWN, 0.0f, query);
        }

        return bestMatch;
    }

    private IntentResult checkFriendsNearby(String query) {
        String[] primary = {"nearby", "near me", "around me", "around here", "close by"};
        String[] secondary = {"friend", "friends", "who is", "who are"};
        
        boolean hasPrimary = containsAny(query, primary);
        boolean hasSecondary = containsAny(query, secondary);
        
        if (hasPrimary && hasSecondary) return new IntentResult(IntentConstants.FRIENDS_NEARBY, 0.95f, query);
        if (hasPrimary) return new IntentResult(IntentConstants.FRIENDS_NEARBY, 0.65f, query);
        return new IntentResult(IntentConstants.UNKNOWN, 0.0f, query);
    }

    private IntentResult checkExpenseStatus(String query) {
        String[] keywords = {"owe", "owes", "expense", "expenses", "split", "pending", "payment", "money", "debt", "due", "balance", "settle"};
        int count = countMatches(query, keywords);
        if (count >= 2) return new IntentResult(IntentConstants.EXPENSE_STATUS, 0.90f, query);
        if (count == 1) return new IntentResult(IntentConstants.EXPENSE_STATUS, 0.70f, query);
        return new IntentResult(IntentConstants.UNKNOWN, 0.0f, query);
    }

    private IntentResult checkReminderQuery(String query) {
        String[] keywords = {"reminder", "reminders", "remind", "due today", "scheduled", "what do i have to do"};
        if (containsAny(query, keywords)) return new IntentResult(IntentConstants.REMINDER_QUERY, 0.90f, query);
        return new IntentResult(IntentConstants.UNKNOWN, 0.0f, query);
    }

    private IntentResult checkNoteQuery(String query) {
        String[] keywords = {"note", "notes", "wrote", "saved", "recent notes", "what did i write"};
        boolean hasKeyword = containsAny(query, keywords);
        boolean isCreate = containsAny(query, new String[]{"create", "add", "make", "new", "write"});
        
        if (hasKeyword && !isCreate) return new IntentResult(IntentConstants.NOTE_QUERY, 0.85f, query);
        return new IntentResult(IntentConstants.UNKNOWN, 0.0f, query);
    }

    private IntentResult checkPlaceSuggestion(String query) {
        String[] keywords = {"suggest", "cafe", "restaurant", "places", "where should i", "nearby place", "food", "eat", "drink", "coffee"};
        int count = countMatches(query, keywords);
        if (count >= 2) return new IntentResult(IntentConstants.PLACE_SUGGESTION, 0.88f, query);
        return new IntentResult(IntentConstants.UNKNOWN, 0.0f, query);
    }

    private IntentResult checkPlanHangout(String query) {
        String[] keywords = {"hangout", "meet", "plan", "where can we", "get together", "chill", "let's go out"};
        int count = countMatches(query, keywords);
        if (count >= 2) return new IntentResult(IntentConstants.PLAN_HANGOUT, 0.88f, query);
        return new IntentResult(IntentConstants.UNKNOWN, 0.0f, query);
    }

    private IntentResult checkNavigationRequest(String query) {
        String[] keywords = {"navigate", "directions", "take me", "show route", "how to get", "go to", "map to"};
        if (containsAny(query, keywords)) return new IntentResult(IntentConstants.NAVIGATION_REQUEST, 0.92f, query);
        return new IntentResult(IntentConstants.UNKNOWN, 0.0f, query);
    }

    private IntentResult checkCreateNote(String query) {
        String[] phrases = {"create note", "add note", "write note", "save note", "make note", "new note"};
        if (containsAny(query, phrases)) return new IntentResult(IntentConstants.CREATE_NOTE, 0.95f, query);
        return new IntentResult(IntentConstants.UNKNOWN, 0.0f, query);
    }

    private IntentResult checkCreateReminder(String query) {
        String[] phrases = {"set reminder", "add reminder", "remind me", "create reminder", "new reminder"};
        if (containsAny(query, phrases)) return new IntentResult(IntentConstants.CREATE_REMINDER, 0.95f, query);
        return new IntentResult(IntentConstants.UNKNOWN, 0.0f, query);
    }

    private IntentResult checkCreateCalendarEvent(String query) {
        String[] phrases = {"set event", "add event", "create event", "new event", "schedule", "calendar", "book a slot", "meeting"};
        if (containsAny(query, phrases)) return new IntentResult(IntentConstants.CREATE_CALENDAR_EVENT, 0.95f, query);
        return new IntentResult(IntentConstants.UNKNOWN, 0.0f, query);
    }

    private IntentResult checkGeneralSuggestion(String query) {
        String[] keywords = {"bored", "what should i do", "suggest something", "anything fun", "any ideas", "what's up"};
        if (containsAny(query, keywords)) return new IntentResult(IntentConstants.GENERAL_SUGGESTION, 0.80f, query);
        return new IntentResult(IntentConstants.UNKNOWN, 0.0f, query);
    }

    private boolean containsAny(String query, String[] keywords) {
        for (String k : keywords) {
            if (query.contains(k)) return true;
        }
        return false;
    }

    private int countMatches(String query, String[] keywords) {
        int count = 0;
        for (String k : keywords) {
            if (query.contains(k)) count++;
        }
        return count;
    }
}
