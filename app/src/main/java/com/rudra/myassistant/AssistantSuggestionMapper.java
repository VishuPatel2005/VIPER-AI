package com.rudra.myassistant;

import com.rudra.myassistant.model.SuggestionCardModel;
import com.rudra.myassistant.suggestions.routing.SuggestionActionRouter;

public class AssistantSuggestionMapper {

    public static SuggestionCardModel mapToModel(String type, String title, String subtitle, String payload) {
        String finalType = type != null ? type : "TIP";
        SuggestionCardModel card = new SuggestionCardModel();
        card.setCardType(finalType);
        card.setTitle(title);
        card.setSubtitle(subtitle);
        card.setActionPayload(payload);
        card.setPrimaryButtonLabel("Explore"); // Default

        // Assign Icon and Action Keys
        switch (finalType) {
            case "FRIEND_NEARBY":
                card.setIconResName("ic_contacts");
                card.setTagLabel("Friend Nearby");
                card.setPrimaryButtonLabel("Plan Hangout");
                card.setActionKey(SuggestionActionRouter.ACTION_OPEN_HANGOUT_PLANNER);
                break;
            case "location":
            case "LOCATION_BASED":
                card.setIconResName("ic_globe");
                card.setPrimaryButtonLabel("Get Directions");
                card.setActionKey(SuggestionActionRouter.ACTION_OPEN_MAP_NAVIGATION);
                break;
            case "TIME_BASED":
            case "time":
                card.setIconResName("ic_pulse");
                card.setTagLabel("Daily Context");
                card.setActionKey(SuggestionActionRouter.ACTION_NONE);
                break;
            case "PREFERENCE_BASED":
                card.setIconResName("ic_search");
                card.setTagLabel("For You");
                card.setActionKey(SuggestionActionRouter.ACTION_SEARCH_MAP);
                break;
            case "productivity":
            case "REMINDER":
                card.setIconResName("ic_notes");
                card.setTagLabel("Tasks");
                card.setActionKey(SuggestionActionRouter.ACTION_OPEN_REMINDERS);
                break;
            case "NOTE":
                card.setIconResName("ic_notes");
                card.setTagLabel("Notes");
                card.setActionKey(SuggestionActionRouter.ACTION_OPEN_NOTES);
                break;
            case "health":
                card.setIconResName("ic_pulse");
                card.setTagLabel("Health");
                card.setActionKey(SuggestionActionRouter.ACTION_OPEN_HEALTH);
                break;
            case "social":
                card.setIconResName("ic_contacts");
                card.setTagLabel("Social");
                card.setActionKey(SuggestionActionRouter.ACTION_NONE);
                break;
            case "TIP":
            case "ai":
                card.setIconResName("ic_globe");
                card.setActionKey(SuggestionActionRouter.ACTION_NONE);
                break;
            case "FALLBACK":
                card.setIconResName("ic_globe");
                card.setActionKey(SuggestionActionRouter.ACTION_OPEN_MAP_NAVIGATION);
                break;
            default:
                card.setIconResName("ic_globe");
                card.setActionKey(SuggestionActionRouter.ACTION_NONE);
                break;
        }

        return card;
    }
}
