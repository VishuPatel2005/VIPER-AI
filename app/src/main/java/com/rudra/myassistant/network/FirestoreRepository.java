package com.rudra.myassistant.network;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.auth.FirebaseUser;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirestoreRepository {

    public final FirebaseFirestore db;
    public final CollectionReference usersCol;
    public final CollectionReference friendsCol;
    public final CollectionReference requestsCol;
    public final CollectionReference healthStatsCol;
    public final CollectionReference chatsCol;
    public final CollectionReference eventsCol;
    public final CollectionReference medicationsCol;
    public final CollectionReference remindersCol;
    public final CollectionReference notesCol;

    public final CollectionReference p2pConversationsCol;

    public FirestoreRepository() {
        db = FirebaseFirestore.getInstance();
        usersCol = db.collection("users");
        friendsCol = db.collection("friends");
        requestsCol = db.collection("friend_requests");
        remindersCol = db.collection("reminders");
        notesCol = db.collection("notes");
        healthStatsCol = db.collection("health_stats");
        chatsCol = db.collection("chat_messages");
        eventsCol = db.collection("calendar_events");
        medicationsCol = db.collection("medications");
        p2pConversationsCol = db.collection("p2p_conversations");
    }

    public CollectionReference getChatsCol() {
        return chatsCol;
    }

    public CollectionReference getRemindersCol() {
        return remindersCol;
    }

    // --- USER PROVISIONING ---

    public void createUserIfNotExists(FirebaseUser user) {
        if (user == null) return;
        
        DocumentReference userRef = usersCol.document(user.getUid());
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            Map<String, Object> userData = new HashMap<>();
            userData.put("uid", user.getUid());
            userData.put("name", user.getDisplayName());
            userData.put("email", user.getEmail());
            userData.put("lastActive", FieldValue.serverTimestamp());
            userData.put("status", "online");

            if (!documentSnapshot.exists()) {
                userData.put("username", null); // Forced to create later
                userData.put("profile_photo_url", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
                userData.put("created_at", FieldValue.serverTimestamp());
                userData.put("searchName", user.getDisplayName() != null ? user.getDisplayName().toLowerCase() : "");
                userRef.set(userData);
            } else {
                userRef.update("lastActive", FieldValue.serverTimestamp(), "status", "online");
                if (documentSnapshot.getString("profile_photo_url") == null && user.getPhotoUrl() != null) {
                    userRef.update("profile_photo_url", user.getPhotoUrl().toString());
                }
            }
        });
    }

    public Task<Void> setUsername(String uid, String username) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username);
        updates.put("searchUsername", username.toLowerCase());
        return usersCol.document(uid).update(updates);
    }

    public Task<Void> updateProfilePhoto(String uid, String url) {
        return usersCol.document(uid).update("profile_photo_url", url);
    }

    public Task<QuerySnapshot> isUsernameAvailable(String username) {
        return usersCol.whereEqualTo("searchUsername", username.toLowerCase()).get();
    }

    public Task<Void> saveUserToFirestore(String uid, String name, String email, String photoUrl) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("profile_photo_url", photoUrl);
        userData.put("lastActive", FieldValue.serverTimestamp());
        userData.put("status", "online");

        return usersCol.document(uid).set(userData, com.google.firebase.firestore.SetOptions.merge());
    }

    public Task<DocumentSnapshot> getUser(String uid) {
        return usersCol.document(uid).get();
    }

    // --- SEARCH ---

    public Task<QuerySnapshot> searchUsers(String query) {
        String normalizedQuery = query.toLowerCase().trim();
        return usersCol
                .orderBy("searchUsername")
                .startAt(normalizedQuery)
                .endAt(normalizedQuery + "\uf8ff")
                .limit(20)
                .get();
    }

    public Task<QuerySnapshot> searchUsersByName(String query) {
        String normalizedQuery = query.toLowerCase().trim();
        return usersCol
                .orderBy("searchName")
                .startAt(normalizedQuery)
                .endAt(normalizedQuery + "\uf8ff")
                .limit(20)
                .get();
    }

    // --- FRIEND REQUESTS ---

    public Task<Void> sendFriendRequest(String senderUid, String receiverUid) {
        String requestId = senderUid + "_" + receiverUid;
        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("senderUid", senderUid);
        request.put("receiverUid", receiverUid);
        request.put("status", "pending");
        request.put("timestamp", FieldValue.serverTimestamp());
        
        // Add expiration (7 days)
        long sevenDaysMillis = 7L * 24 * 60 * 60 * 1000;
        Date expiresAt = new Date(System.currentTimeMillis() + sevenDaysMillis);
        request.put("expiresAt", expiresAt);

        return requestsCol.document(requestId).set(request);
    }

    public Task<Void> respondToRequest(String requestId, String status, String user_id) {
        if ("block".equals(status)) {
            // Get the request first to find the sender
            return requestsCol.document(requestId).get().continueWithTask(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String senderUid = task.getResult().getString("senderUid");
                    if (senderUid != null) {
                        blockUser(user_id, senderUid);
                    }
                }
                return requestsCol.document(requestId).delete();
            });
        }
        
        if ("declined".equals(status)) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "declined");
            // Add 30-day cooldown
            long thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000;
            Date cooldownUntil = new Date(System.currentTimeMillis() + thirtyDaysMillis);
            updates.put("cooldownUntil", cooldownUntil);
            return requestsCol.document(requestId).update(updates);
        }

        return requestsCol.document(requestId).update("status", status);
    }

    public Task<Void> blockUser(String blockerUid, String blockedUid) {
        Map<String, Object> blockData = new HashMap<>();
        blockData.put("blockerUid", blockerUid);
        blockData.put("blockedUid", blockedUid);
        blockData.put("timestamp", FieldValue.serverTimestamp());
        
        // Also remove friendship if exists
        removeFriendship(blockerUid, blockedUid);
        
        return db.collection("blocked_users").document(blockerUid + "_" + blockedUid).set(blockData);
    }

    public com.google.firebase.firestore.Query getPendingRequests(String uid) {
        return requestsCol.whereEqualTo("receiverUid", uid)
                .whereEqualTo("status", "pending");
    }

    // --- FRIENDS ---

    public Task<Void> addFriendship(String uid1, String uid2) {
        Map<String, Object> friendship = new HashMap<>();
        friendship.put("userUid", uid1);
        friendship.put("friendUid", uid2);
        friendship.put("iAmSharingWithThem", true);
        friendship.put("theyAreSharingWithMe", true);
        return friendsCol.document(uid1 + "_" + uid2).set(friendship);
    }

    public Task<Void> toggleLocationSharing(String myUid, String friendUid, boolean isSharing) {
        // Update both docs to keep state consistent across both users' lists
        Task<Void> task1 = friendsCol.document(myUid + "_" + friendUid).update("iAmSharingWithThem", isSharing);
        Task<Void> task2 = friendsCol.document(friendUid + "_" + myUid).update("theyAreSharingWithMe", isSharing);
        return Tasks.whenAll(task1, task2);
    }

    public Task<Void> removeFriendship(String uid1, String uid2) {
        Task<Void> t1 = friendsCol.document(uid1 + "_" + uid2).delete();
        Task<Void> t2 = friendsCol.document(uid2 + "_" + uid1).delete();
        return Tasks.whenAll(t1, t2);
    }

    public Query getFriendsQuery(String uid) {
        return friendsCol.whereEqualTo("userUid", uid);
    }

    // --- REMINDERS (DECENTRALIZED) ---
    public Task<DocumentReference> addReminder(String uid, String text, String datetime, boolean flagged, 
                                              String locationName, Double lat, Double lng, float radius, 
                                              boolean arrive, boolean depart) {
        Map<String, Object> reminder = new HashMap<>();
        reminder.put("user_id", uid);
        reminder.put("text", text);
        reminder.put("datetime", datetime);
        reminder.put("completed", false);
        reminder.put("flagged", flagged);
        reminder.put("location_name", locationName);
        reminder.put("latitude", lat);
        reminder.put("longitude", lng);
        reminder.put("radius", radius);
        reminder.put("remind_on_arrival", arrive);
        reminder.put("remind_on_departure", depart);
        reminder.put("created_at", FieldValue.serverTimestamp());
        reminder.put("updated_at", FieldValue.serverTimestamp());
        return db.collection("reminders").add(reminder);
    }

    public Query getRemindersQuery(String uid) {
        return remindersCol.whereEqualTo("user_id", uid);
    }

    public Task<Void> updateReminder(String id, Map<String, Object> updates) {
        updates.put("updated_at", FieldValue.serverTimestamp());
        return remindersCol.document(id).update(updates);
    }

    public Task<Void> deleteReminder(String id) {
        return remindersCol.document(id).delete();
    }

    // --- NOTES (DECENTRALIZED) ---

    public Task<DocumentReference> addNote(String uid, String title, String content) {
        Map<String, Object> note = new HashMap<>();
        note.put("userId", uid);
        note.put("title", title);
        note.put("content", content);
        note.put("created_at", FieldValue.serverTimestamp());
        return notesCol.add(note);
    }

    public Query getNotesQuery(String uid) {
        return notesCol.whereEqualTo("userId", uid);
    }

    public Task<Void> updateNote(String id, Map<String, Object> updates) {
        updates.put("updated_at", FieldValue.serverTimestamp());
        return notesCol.document(id).update(updates);
    }

    public Task<Void> deleteNote(String id) {
        return notesCol.document(id).delete();
    }

    // --- MEDICATIONS (DECENTRALIZED) ---
 
    public Task<DocumentReference> addMedication(String uid, String name, String dosage, String time, List<String> days, String specificDate) {
        Map<String, Object> med = new HashMap<>();
        med.put("userId", uid);
        med.put("name", name);
        med.put("dosage", dosage);
        med.put("time", time);
        med.put("days", days);
        med.put("specific_date", specificDate);
        med.put("created_at", FieldValue.serverTimestamp());
        return medicationsCol.add(med);
    }
 
    public Query getMedicationsQuery(String uid) {
        return medicationsCol.whereEqualTo("userId", uid).orderBy("created_at", Query.Direction.DESCENDING);
    }
 
    public Task<Void> updateMedication(String id, Map<String, Object> updates) {
        return medicationsCol.document(id).update(updates);
    }
 
    public Task<Void> deleteMedication(String id) {
        return medicationsCol.document(id).delete();
    }
 
    public Task<QuerySnapshot> findMedicationByName(String uid, String name) {
        return medicationsCol.whereEqualTo("userId", uid).whereEqualTo("name", name).limit(1).get();
    }
 
    public Task<Void> addMemory(String uid, String content, String type) {
        Map<String, Object> memory = new HashMap<>();
        memory.put("userId", uid);
        memory.put("content", content);
        memory.put("type", type != null ? type : "preference");
        memory.put("timestamp", FieldValue.serverTimestamp());
        return db.collection("assistant_memory").add(memory).continueWith(task -> null);
    }

    // --- CALENDAR (DECENTRALIZED) ---

    public Task<DocumentReference> addEvent(String uid, String title, String start, String end, String desc) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", uid);
        event.put("title", title);
        event.put("startTime", start);
        event.put("endTime", end);
        event.put("description", desc);
        event.put("created_at", FieldValue.serverTimestamp());
        return eventsCol.add(event);
    }

    public Query getEventsQuery(String uid) {
        return eventsCol.whereEqualTo("userId", uid);
    }

    public Task<Void> deleteEvent(String id) {
        return eventsCol.document(id).delete();
    }

    // --- CHAT PERSISTENCE ---

    public Task<DocumentReference> saveChatMessage(String sessionId, String userId, String message, String role) {
        Map<String, Object> chat = new HashMap<>();
        chat.put("sessionId", sessionId);
        chat.put("userId", userId);
        chat.put("message", message);
        chat.put("role", role);
        chat.put("timestamp", FieldValue.serverTimestamp());
        return chatsCol.add(chat);
    }

    // --- HEALTH TRACKING ---

    public Task<DocumentReference> logPeriodData(String uid, Map<String, Object> data) {
        data.put("userId", uid);
        data.put("timestamp", FieldValue.serverTimestamp());
        return healthStatsCol.add(data);
    }

    public Task<Void> updateHealthStats(String uid, Integer water, Integer steps, Double sleep) {
        Map<String, Object> stats = new HashMap<>();
        if (water != null) stats.put("water_intake", water);
        if (steps != null) stats.put("steps_count", steps);
        if (sleep != null) stats.put("sleep_hours", sleep);
        stats.put("last_updated", FieldValue.serverTimestamp());
        return db.collection("daily_stats").document(uid).set(stats, com.google.firebase.firestore.SetOptions.merge());
    }

    public Task<DocumentSnapshot> getDailyStats(String uid) {
        return db.collection("daily_stats").document(uid).get();
    }

    // --- LOCATION UPDATES ---

    public Task<Void> updateUserLocation(String uid, double lat, double lon) {
        Map<String, Object> location = new HashMap<>();
        location.put("latitude", lat);
        location.put("longitude", lon);
        location.put("last_updated", FieldValue.serverTimestamp());
        return usersCol.document(uid).collection("location").document("current").set(location);
    }

    public DocumentReference getFriendLocationRef(String uid) {
        return usersCol.document(uid).collection("location").document("current");
    }

    public Task<DocumentSnapshot> getUserLocation(String uid) {
        return usersCol.document(uid).collection("location").document("current").get();
    }

    public Task<Void> updateUserPreferences(String uid, List<String> preferences) {
        return usersCol.document(uid).update("preferences", preferences);
    }

    // --- P2P MESSAGING ---

    public Query getConversationsQuery(String uid) {
        return p2pConversationsCol.whereArrayContains("participants", uid);
    }

    public Task<DocumentReference> sendP2PMessage(String conversationId, String senderId, String receiverId, String text) {
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", senderId);
        message.put("receiverId", receiverId);
        message.put("text", text);
        message.put("timestamp", FieldValue.serverTimestamp());

        // Update the conversation's last message snippet
        Map<String, Object> convUpdate = new HashMap<>();
        convUpdate.put("lastMessage", text);
        convUpdate.put("lastTimestamp", FieldValue.serverTimestamp());
        convUpdate.put("lastSenderId", senderId);
        convUpdate.put("read", false);
        convUpdate.put("participants", java.util.Arrays.asList(senderId, receiverId));

        p2pConversationsCol.document(conversationId).set(convUpdate, com.google.firebase.firestore.SetOptions.merge());
        
        return p2pConversationsCol.document(conversationId).collection("messages").add(message);
    }

    public Query getMessagesQuery(String conversationId) {
        return p2pConversationsCol.document(conversationId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING);
    }

    public Task<Void> markConversationAsRead(String conversationId) {
        return p2pConversationsCol.document(conversationId).update("read", true);
    }

    public Task<Void> clearChatHistory(String userId) {
        return chatsCol.whereEqualTo("userId", userId).get().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) return Tasks.forResult(null);
            List<Task<Void>> deleteTasks = new java.util.ArrayList<>();
            for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult()) {
                deleteTasks.add(doc.getReference().delete());
            }
            return com.google.android.gms.tasks.Tasks.whenAll(deleteTasks);
        });
    }
}
