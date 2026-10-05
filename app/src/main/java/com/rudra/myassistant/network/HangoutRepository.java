package com.rudra.myassistant.network;

import com.rudra.myassistant.model.LatLng;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository to manage Hangout-related data from Firestore.
 */
public class HangoutRepository {
    private final FirebaseFirestore db;
    private final Map<String, FriendContext> cache = new HashMap<>();

    public interface Callback {
        void onResult(FriendContext context);
        void onError(String error);
    }

    public static class FriendContext {
        public String friendName;
        public LatLng latLng;
        public boolean isLocationFresh;

        public FriendContext(String name, LatLng latLng, boolean isFresh) {
            this.friendName = name;
            this.latLng = latLng;
            this.isLocationFresh = isFresh;
        }
    }

    public HangoutRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void fetchFriendContext(String friendUid, Callback callback) {
        if (cache.containsKey(friendUid)) {
            callback.onResult(cache.get(friendUid));
            return;
        }

        // 1. Fetch Name from users/{uid}
        db.collection("users").document(friendUid).get().addOnSuccessListener(userDoc -> {
            if (!userDoc.exists()) {
                callback.onError("User not found");
                return;
            }

            String name = userDoc.getString("displayName");
            if (name == null) name = userDoc.getString("name");
            if (name == null) name = "Your Friend";

            final String finalName = name;

            // 2. Fetch Location from users/{uid}/location/current
            db.collection("users").document(friendUid).collection("location").document("current").get()
                .addOnSuccessListener(locDoc -> {
                    if (!locDoc.exists()) {
                        // Fallback to main doc for legacy
                        Double lat = userDoc.getDouble("latitude");
                        Double lng = userDoc.getDouble("longitude");
                        if (lat != null && lng != null) {
                            FriendContext context = new FriendContext(finalName, new LatLng(lat, lng), true);
                            cache.put(friendUid, context);
                            callback.onResult(context);
                        } else {
                            callback.onResult(new FriendContext(finalName, null, false));
                        }
                        return;
                    }

                    Double lat = locDoc.getDouble("latitude");
                    Double lng = locDoc.getDouble("longitude");
                    Timestamp ts = locDoc.getTimestamp("last_updated");

                    boolean isFresh = false;
                    if (ts != null) {
                        long diff = new Date().getTime() - ts.toDate().getTime();
                        isFresh = diff < 600000; // 10 minutes
                    }

                    if (lat != null && lng != null) {
                        FriendContext context = new FriendContext(finalName, new LatLng(lat, lng), isFresh);
                        cache.put(friendUid, context);
                        callback.onResult(context);
                    } else {
                        callback.onResult(new FriendContext(finalName, null, false));
                    }
                }).addOnFailureListener(e -> callback.onError(e.getMessage()));
        }).addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
