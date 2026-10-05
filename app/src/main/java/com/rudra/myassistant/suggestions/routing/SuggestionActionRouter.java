package com.rudra.myassistant.suggestions.routing;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import com.rudra.myassistant.ConnectActivity;
import com.rudra.myassistant.HealthActivity;
import com.rudra.myassistant.NotesActivity;
import com.rudra.myassistant.RemindersActivity;
import com.rudra.myassistant.model.SuggestionCardModel;

public class SuggestionActionRouter {

    public static final String ACTION_OPEN_MAP_NAVIGATION = "ACTION_OPEN_MAP_NAVIGATION";
    public static final String ACTION_SEARCH_MAP = "ACTION_SEARCH_MAP";
    public static final String ACTION_OPEN_HANGOUT_PLANNER = "ACTION_OPEN_HANGOUT_PLANNER";
    public static final String ACTION_OPEN_REMINDERS = "ACTION_OPEN_REMINDERS";
    public static final String ACTION_OPEN_NOTES = "ACTION_OPEN_NOTES";
    public static final String ACTION_OPEN_HEALTH = "ACTION_OPEN_HEALTH";
    public static final String ACTION_NONE = "ACTION_NONE";

    public static void route(Context context, SuggestionCardModel card) {
        if (card == null || card.getActionKey() == null) return;

        String key = card.getActionKey();
        String payload = card.getActionPayload();

        switch (key) {
            case ACTION_OPEN_MAP_NAVIGATION:
                openMap(context, "google.navigation:q=" + Uri.encode(payload != null ? payload : card.getTitle()));
                break;
            case ACTION_SEARCH_MAP:
                openMap(context, "geo:0,0?q=" + Uri.encode(payload != null ? payload : card.getTitle()));
                break;
            case ACTION_OPEN_HANGOUT_PLANNER: {
                Intent intent = new Intent(context, com.rudra.myassistant.ChatActivity.class);
                intent.putExtra("EXTRA_CHAT_MODE", "HANGOUT_PLANNING");
                
                String friendUid = payload;
                if (friendUid != null) {
                    intent.putExtra("EXTRA_FRIEND_UID", friendUid);
                    intent.putExtra("EXTRA_FRIEND_NAME", com.rudra.myassistant.SuggestionsManager.getNearbyFriendName(friendUid));
                    
                    com.rudra.myassistant.model.LatLng fLoc = com.rudra.myassistant.SuggestionsManager.getNearbyFriendLocation(friendUid);
                    if (fLoc != null) {
                        intent.putExtra("EXTRA_FRIEND_LAT", fLoc.latitude);
                        intent.putExtra("EXTRA_FRIEND_LNG", fLoc.longitude);
                    }
                }
                
                com.rudra.myassistant.location.LocationTracker tracker = com.rudra.myassistant.location.LocationTracker.getInstance();
                android.location.Location uLoc = (tracker != null) ? tracker.getLastKnownLocation() : null;
                if (uLoc != null) {
                    intent.putExtra("EXTRA_USER_LAT", uLoc.getLatitude());
                    intent.putExtra("EXTRA_USER_LNG", uLoc.getLongitude());
                }
                
                context.startActivity(intent);
                break;
            }
            case ACTION_OPEN_REMINDERS:
                context.startActivity(new Intent(context, RemindersActivity.class));
                break;
            case ACTION_OPEN_NOTES:
                context.startActivity(new Intent(context, NotesActivity.class));
                break;
            case ACTION_OPEN_HEALTH:
                // Rule: ONLY for cards explicitly of type HEALTH (or if the engine allowed it)
                context.startActivity(new Intent(context, HealthActivity.class));
                break;
            case ACTION_NONE:
                if (card.getCardType().equals("TIP")) {
                    Toast.makeText(context, card.getDescription() != null ? card.getDescription() : "Keep it up!", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                Log.w("SuggestionRouter", "Unknown action key: " + key);
                break;
        }
    }

    private static void openMap(Context context, String uriString) {
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(mapIntent);
        } else {
            // Fallback to browser/generic view if Google Maps not installed
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uriString)));
        }
    }
}
