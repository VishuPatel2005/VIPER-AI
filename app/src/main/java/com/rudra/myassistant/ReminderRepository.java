package com.rudra.myassistant;

import android.content.Context;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.ReminderDto;
import com.rudra.myassistant.network.UserSessionManager;
import com.rudra.myassistant.location.GeofenceManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReminderRepository {

    private final FirestoreRepository firestoreRepo;
    private final UserSessionManager sessionManager;
    private final GeofenceManager geofenceManager;

    public ReminderRepository(Context context) {
        this.firestoreRepo = new FirestoreRepository();
        this.sessionManager = new UserSessionManager(context);
        this.geofenceManager = new GeofenceManager(context);
    }

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }

    public void getAllReminders(DataCallback<List<ReminderDto>> callback) {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            callback.onError("User not logged in");
            return;
        }

        firestoreRepo.getRemindersQuery(userId).get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<ReminderDto> reminders = new ArrayList<>();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                ReminderDto r = new ReminderDto(
                    doc.getId(),
                    doc.getString("text"),
                    doc.getString("datetime"),
                    Boolean.TRUE.equals(doc.getBoolean("completed")),
                    Boolean.TRUE.equals(doc.getBoolean("flagged")),
                    doc.getString("created_at"),
                    doc.getString("updated_at")
                );
                r.setLocationName(doc.getString("location_name"));
                r.setLatitude(doc.getDouble("latitude"));
                r.setLongitude(doc.getDouble("longitude"));
                Double radius = doc.getDouble("radius");
                if (radius != null) r.setRadius(radius.floatValue());
                r.setRemindOnArrival(Boolean.TRUE.equals(doc.getBoolean("remind_on_arrival")));
                r.setRemindOnDeparture(Boolean.TRUE.equals(doc.getBoolean("remind_on_departure")));
                reminders.add(r);
            }
            callback.onSuccess(reminders);
        }).addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void addReminder(String text, String datetime, boolean flagged, String locationName, Double lat, Double lng, float radius, boolean arrive, boolean depart, DataCallback<ReminderDto> callback) {
        String userId = sessionManager.getUserId();
        firestoreRepo.addReminder(userId, text, datetime, flagged, locationName, lat, lng, radius, arrive, depart)
            .addOnSuccessListener(docRef -> {
                ReminderDto r = new ReminderDto(docRef.getId(), text, datetime, false, flagged, null, null);
                r.setLocationName(locationName);
                r.setLatitude(lat);
                r.setLongitude(lng);
                r.setRadius(radius);
                r.setRemindOnArrival(arrive);
                r.setRemindOnDeparture(depart);
                callback.onSuccess(r);
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateReminder(String backendId, String text, String datetime, Boolean completed, Boolean flagged, DataCallback<ReminderDto> callback) {
        Map<String, Object> updates = new HashMap<>();
        if (text != null) updates.put("text", text);
        if (datetime != null) updates.put("datetime", datetime);
        if (completed != null) updates.put("completed", completed);
        if (flagged != null) updates.put("flagged", flagged);

        FirebaseFirestore.getInstance().collection("reminders").document(backendId)
            .update(updates)
            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void deleteReminder(String backendId, DataCallback<Void> callback) {
        FirebaseFirestore.getInstance().collection("reminders").document(backendId)
            .delete()
            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void syncGeofences() {
        getAllReminders(new DataCallback<List<ReminderDto>>() {
            @Override
            public void onSuccess(List<ReminderDto> data) {
                for (ReminderDto dto : data) {
                    if (dto.getLatitude() != null && dto.getLongitude() != null && !dto.isCompleted()) {
                        Reminder r = new Reminder(dto.getText(), dto.isFlagged());
                        r.setId(dto.getId().hashCode()); // Simple mapping for request ID
                        r.setLatitude(dto.getLatitude());
                        r.setLongitude(dto.getLongitude());
                        r.setRadius(dto.getRadius());
                        r.setRemindOnArrival(dto.isRemindOnArrival());
                        r.setRemindOnDeparture(dto.isRemindOnDeparture());
                        geofenceManager.addGeofence(r);
                    }
                }
            }
            @Override
            public void onError(String error) {}
        });
    }

    public List<String> getTodayReminderSummary() {
        List<String> summary = new ArrayList<>();
        String userId = sessionManager.getUserId();
        if (userId == null) return summary;

        try {
            // Ensure this doesn't run on main thread
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                android.util.Log.e("ReminderRepo", "Error: getTodayReminderSummary called on main thread!");
                return summary;
            }
            com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task = firestoreRepo.getRemindersQuery(userId).get();
            com.google.firebase.firestore.QuerySnapshot snapshot = com.google.android.gms.tasks.Tasks.await(task, 10, java.util.concurrent.TimeUnit.SECONDS);
            
            if (snapshot != null) {
                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
                    if (Boolean.TRUE.equals(doc.getBoolean("completed"))) continue;
                    String text = doc.getString("text");
                    String date = doc.getString("datetime");
                    if (text != null) {
                        summary.add(text + (date != null ? " (Due: " + date + ")" : ""));
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ReminderRepo", "Error fetching summary", e);
        }
        return summary;
    }

    public List<ReminderDto> getRemindersForNotification() {
        List<ReminderDto> list = new ArrayList<>();
        String userId = sessionManager.getUserId();
        if (userId == null) return list;

        try {
            // Ensure this doesn't run on main thread
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                android.util.Log.e("ReminderRepo", "Error: getRemindersForNotification called on main thread!");
                return list;
            }
            com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task = firestoreRepo.getRemindersQuery(userId).get();
            com.google.firebase.firestore.QuerySnapshot snapshot = com.google.android.gms.tasks.Tasks.await(task, 10, java.util.concurrent.TimeUnit.SECONDS);
            
            if (snapshot != null) {
                long now = System.currentTimeMillis();
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);

                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
                    if (Boolean.TRUE.equals(doc.getBoolean("notified"))) continue;
                    if (Boolean.TRUE.equals(doc.getBoolean("completed"))) continue;

                    String datetime = doc.getString("datetime");
                    if (datetime == null) continue;

                    try {
                        java.util.Date date = sdf.parse(datetime);
                        if (date != null) {
                            long reminderTime = date.getTime();
                            // Window: ±60 seconds
                            if (reminderTime > now - 60_000 && reminderTime <= now + 60_000) {
                                ReminderDto r = new ReminderDto(
                                    doc.getId(),
                                    doc.getString("text"),
                                    datetime,
                                    false,
                                    Boolean.TRUE.equals(doc.getBoolean("flagged")),
                                    doc.getString("created_at"),
                                    doc.getString("updated_at")
                                );
                                list.add(r);
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("ReminderRepo", "Error parsing date: " + datetime, e);
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ReminderRepo", "Error fetching reminders for notification", e);
        }
        return list;
    }

    public void markReminderNotified(String reminderId) {
        FirebaseFirestore.getInstance().collection("reminders").document(reminderId)
            .update("notified", true);
    }
}
