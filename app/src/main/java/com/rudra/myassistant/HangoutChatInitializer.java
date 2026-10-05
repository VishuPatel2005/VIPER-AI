package com.rudra.myassistant;

import android.content.Context;
import android.content.Intent;
import com.rudra.myassistant.model.LatLng;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles initialization and launching of a Hangout Planning chat session.
 */
public class HangoutChatInitializer {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void startHangoutChat(Context context, String friendUid, String friendName, LatLng friendLatLng, LatLng currentUserLatLng) {
        executor.execute(() -> {
            // Generate a fresh session ID for this hangout plan
            String sessionId = "hangout_" + UUID.randomUUID().toString();

            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("chat_id", sessionId);
            intent.putExtra("EXTRA_CHAT_MODE", "HANGOUT_PLANNING");
            intent.putExtra("EXTRA_FRIEND_NAME", friendName);
            intent.putExtra("EXTRA_FRIEND_UID", friendUid);
            if (friendLatLng != null) {
                intent.putExtra("EXTRA_FRIEND_LAT", friendLatLng.latitude);
                intent.putExtra("EXTRA_FRIEND_LNG", friendLatLng.longitude);
            }
            if (currentUserLatLng != null) {
                intent.putExtra("EXTRA_USER_LAT", currentUserLatLng.latitude);
                intent.putExtra("EXTRA_USER_LNG", currentUserLatLng.longitude);
            }
            
            // Flags to clear stack if needed or just start new
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }
}
