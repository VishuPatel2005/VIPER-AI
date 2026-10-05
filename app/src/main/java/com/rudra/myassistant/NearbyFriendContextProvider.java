package com.rudra.myassistant;

import android.util.Log;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.rudra.myassistant.network.FirestoreRepository;

/**
 * Single source of truth for friend name and location during hangout planning.
 * Reads from Firestore users/{friendUid} and users/{friendUid}/location.
 * Caches result in memory for the current session.
 */
public class NearbyFriendContextProvider {

    private static final String TAG = "NearbyFriendContext";
    private static final long STALE_THRESHOLD_MS = 10 * 60 * 1000; // 10 minutes

    // Singleton instance
    private static NearbyFriendContextProvider instance;

    // Cached fields
    private String friendUid;
    private String friendName;
    private double friendLat;
    private double friendLng;
    private boolean isLocationFresh;
    private long fetchedAtMillis;

    private final FirestoreRepository firestoreRepo;

    public interface Callback {
        void onContextLoaded(NearbyFriendContextProvider provider);
    }

    private NearbyFriendContextProvider() {
        this.firestoreRepo = new FirestoreRepository();
    }

    /**
     * Singleton per session. Resets when a new friendUid is loaded.
     */
    public static synchronized NearbyFriendContextProvider getInstance() {
        if (instance == null) {
            instance = new NearbyFriendContextProvider();
        }
        return instance;
    }

    /**
     * Reset the singleton (e.g., when user navigates away from hangout flow).
     */
    public static synchronized void reset() {
        instance = null;
    }

    /**
     * Load friend context from Firestore.
     * Step 1: Read users/{friendUid} → name (try displayName, name, fullName, then email prefix)
     * Step 2: Read users/{friendUid}/location → lat, lng, last_updated
     * Step 3: Validate freshness of location
     * Step 4: Cache and deliver via callback
     */
    public void loadFriendContext(String friendUid, Callback callback) {
        // If we already have fresh cached data for this friend, return immediately
        if (this.friendUid != null && this.friendUid.equals(friendUid) && fetchedAtMillis > 0) {
            callback.onContextLoaded(this);
            return;
        }

        this.friendUid = friendUid;
        this.friendName = "your friend"; // Safe fallback
        this.friendLat = 0;
        this.friendLng = 0;
        this.isLocationFresh = false;

        // Step 1: Fetch user document for name
        firestoreRepo.getUser(friendUid).addOnSuccessListener(userDoc -> {
            if (userDoc != null && userDoc.exists()) {
                // Try field names in priority order: displayName → name → fullName → email prefix
                friendName = resolveNameFromDoc(userDoc);
            }

            // Step 2: Fetch location sub-document
            firestoreRepo.getUserLocation(friendUid).addOnSuccessListener(locDoc -> {
                if (locDoc != null && locDoc.exists()) {
                    Double lat = locDoc.getDouble("latitude");
                    Double lng = locDoc.getDouble("longitude");
                    Timestamp ts = locDoc.getTimestamp("last_updated");

                    if (lat != null && lng != null) {
                        friendLat = lat;
                        friendLng = lng;

                        // Step 3: Validate freshness
                        if (ts != null) {
                            long age = System.currentTimeMillis() - ts.toDate().getTime();
                            isLocationFresh = age < STALE_THRESHOLD_MS;
                        } else {
                            isLocationFresh = false;
                        }
                    }
                }

                fetchedAtMillis = System.currentTimeMillis();
                callback.onContextLoaded(NearbyFriendContextProvider.this);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to load friend location: " + e.getMessage());
                fetchedAtMillis = System.currentTimeMillis();
                callback.onContextLoaded(NearbyFriendContextProvider.this);
            });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load friend user doc: " + e.getMessage());
            fetchedAtMillis = System.currentTimeMillis();
            callback.onContextLoaded(NearbyFriendContextProvider.this);
        });
    }

    /**
     * Resolve friend name from Firestore document.
     * Priority: displayName → name → fullName → email prefix → "your friend"
     */
    private String resolveNameFromDoc(DocumentSnapshot doc) {
        String name = doc.getString("displayName");
        if (name != null && !name.trim().isEmpty()) return name.trim();

        name = doc.getString("name");
        if (name != null && !name.trim().isEmpty()) return name.trim();

        name = doc.getString("fullName");
        if (name != null && !name.trim().isEmpty()) return name.trim();

        // Last resort: email prefix
        String email = doc.getString("email");
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf("@"));
        }

        return "your friend";
    }

    // --- Getters ---

    /**
     * Returns friendName if non-null and non-empty, "your friend" as fallback.
     */
    public String getFriendDisplayName() {
        return (friendName != null && !friendName.isEmpty()) ? friendName : "your friend";
    }

    public String getFriendUid() { return friendUid; }
    public double getFriendLat() { return friendLat; }
    public double getFriendLng() { return friendLng; }
    public boolean isLocationFresh() { return isLocationFresh; }

    /**
     * Returns true if friendName is non-null AND location is fresh.
     */
    public boolean isValid() {
        return friendName != null && !friendName.isEmpty() && isLocationFresh;
    }
}
