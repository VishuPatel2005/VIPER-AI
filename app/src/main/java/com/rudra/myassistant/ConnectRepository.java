package com.rudra.myassistant;

import android.content.Context;
import com.google.android.gms.tasks.Tasks;
import com.rudra.myassistant.network.FirestoreRepository;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.rudra.myassistant.network.Friend;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ConnectRepository {
    private final FirestoreRepository firestoreRepo;
    private final String currentUserId;

    private final Context context;

    public ConnectRepository(Context context) {
        this.context = context;
        this.firestoreRepo = new FirestoreRepository();
        this.currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
    }

    public List<String> getNearbyFriendsSummary() {
        List<String> summary = new ArrayList<>();
        if (currentUserId == null) return summary;

        try {
            // Synchronous fetch using Tasks.await (must be on background thread)
            QuerySnapshot friendDocs = Tasks.await(firestoreRepo.getFriendsQuery(currentUserId).get(), 10, TimeUnit.SECONDS);
            
            for (DocumentSnapshot doc : friendDocs) {
                String friendUid = doc.getString("friendUid");
                if (friendUid == null) continue;

                // Fetch friend name and location
                DocumentSnapshot userDoc = Tasks.await(firestoreRepo.getUser(friendUid), 5, TimeUnit.SECONDS);
                DocumentSnapshot locDoc = Tasks.await(firestoreRepo.getUserLocation(friendUid), 5, TimeUnit.SECONDS);

                if (userDoc.exists() && locDoc.exists()) {
                    String name = userDoc.getString("name");
                    Double lat = locDoc.getDouble("latitude");
                    Double lng = locDoc.getDouble("longitude");
                    com.google.firebase.Timestamp ts = locDoc.getTimestamp("last_updated");
                    
                    if (name != null && lat != null && lng != null && ts != null) {
                        // 2-hour stale check (relaxed for assistant summaries)
                        long diffMs = System.currentTimeMillis() - ts.toDate().getTime();
                        if (diffMs < 2 * 60 * 60 * 1000) {
                            String area = getAreaName(lat, lng);
                            summary.add(name + " is nearby, currently active near " + area);
                        } else {
                            android.util.Log.d("ConnectRepo", "Skipping " + name + " (location stale: " + (diffMs / 3600000) + "h ago)");
                        }
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ConnectRepo", "Error fetching friends summary", e);
        }
        return summary;
    }

    private String getAreaName(double lat, double lng) {
        try {
            android.location.Geocoder geocoder = new android.location.Geocoder(context, java.util.Locale.getDefault());
            List<android.location.Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                android.location.Address address = addresses.get(0);
                String subLocality = address.getSubLocality();
                String locality = address.getLocality();
                if (subLocality != null && locality != null) return subLocality + ", " + locality;
                if (subLocality != null) return subLocality;
                if (locality != null) return locality;
                return address.getFeatureName();
            }
        } catch (Exception e) {
            android.util.Log.e("ConnectRepo", "Geocoding failed", e);
        }
        return "your current location";
    }

    public interface FriendLocationCallback {
        void onSuccess(List<FriendLocation> friends);
        void onError(String error);
    }

    public static class FriendLocation {
        public String uid, name;
        public double lat, lng;
        public FriendLocation(String uid, String name, double lat, double lng) {
            this.uid = uid; this.name = name; this.lat = lat; this.lng = lng;
        }
    }

    public void getFriendLocations(FriendLocationCallback callback) {
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }

        new Thread(() -> {
            try {
                List<FriendLocation> friends = new ArrayList<>();
                QuerySnapshot friendDocs = Tasks.await(firestoreRepo.getFriendsQuery(currentUserId).get(), 10, TimeUnit.SECONDS);
                
                for (DocumentSnapshot doc : friendDocs) {
                    String friendUid = doc.getString("friendUid");
                    if (friendUid == null) continue;

                    DocumentSnapshot userDoc = Tasks.await(firestoreRepo.getUser(friendUid), 5, TimeUnit.SECONDS);
                    DocumentSnapshot locDoc = Tasks.await(firestoreRepo.getUserLocation(friendUid), 5, TimeUnit.SECONDS);

                    if (userDoc.exists() && locDoc.exists()) {
                        String name = userDoc.getString("name");
                        Double lat = locDoc.getDouble("latitude");
                        Double lng = locDoc.getDouble("longitude");
                        com.google.firebase.Timestamp ts = locDoc.getTimestamp("last_updated");

                        if (name != null && lat != null && lng != null && ts != null) {
                            long diffMs = System.currentTimeMillis() - ts.toDate().getTime();
                            if (diffMs < 30 * 60 * 1000) {
                                friends.add(new FriendLocation(friendUid, name, lat, lng));
                            }
                        }
                    }
                }
                callback.onSuccess(friends);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }
}
