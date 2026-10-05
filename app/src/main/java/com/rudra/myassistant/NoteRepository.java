package com.rudra.myassistant;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.UserSessionManager;
import java.util.ArrayList;
import java.util.List;

public class NoteRepository {

    private final FirestoreRepository firestoreRepo;
    private final UserSessionManager sessionManager;

    public NoteRepository(android.content.Context context) {
        firestoreRepo = new FirestoreRepository();
        sessionManager = new UserSessionManager(context);
    }
    
    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }
    
    public interface DefaultCallback {
        void onSuccess();
        void onError(String error);
    }

    public void getNotes(DataCallback<List<Note>> callback) {
        String userId = sessionManager.getUserId();
        firestoreRepo.getNotesQuery(userId).get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Note> notesList = new ArrayList<>();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                Note n = doc.toObject(Note.class);
                if (n != null) {
                    n.setId(doc.getId());
                    notesList.add(n);
                }
            }
            
            // Client-side sort (Descending by createdAt)
            notesList.sort((n1, n2) -> {
                com.google.firebase.Timestamp t1 = n1.getCreatedAt();
                com.google.firebase.Timestamp t2 = n2.getCreatedAt();
                if (t1 == null && t2 == null) return 0;
                if (t1 == null) return 1;
                if (t2 == null) return -1;
                return t2.compareTo(t1);
            });
            
            callback.onSuccess(notesList);
        }).addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void createNote(String title, String content, boolean pinned, DataCallback<Note> callback) {
        String userId = sessionManager.getUserId();
        // Since Firestore add returns a Task<DocumentReference>, we'll wait for it
        firestoreRepo.addNote(userId, title, content)
            .addOnSuccessListener(docRef -> {
                Note n = new Note(docRef.getId(), title, content, pinned, false, null, null);
                callback.onSuccess(n);
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateNote(String id, String title, String content, Boolean pinned, Boolean locked, DataCallback<Note> callback) {
        // Direct Firestore update
        com.google.firebase.firestore.DocumentReference ref = FirebaseFirestore.getInstance().collection("notes").document(id);
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        if (title != null) updates.put("title", title);
        if (content != null) updates.put("content", content);
        if (pinned != null) updates.put("pinned", pinned);
        if (locked != null) updates.put("locked", locked);

        ref.update(updates).addOnSuccessListener(aVoid -> callback.onSuccess(null))
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void deleteNote(String id, DefaultCallback callback) {
        firestoreRepo.deleteNote(id)
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public List<String> getRecentNotesSummary() {
        List<String> summary = new ArrayList<>();
        String userId = sessionManager.getUserId();
        if (userId == null) return summary;

        try {
            com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task = firestoreRepo.getNotesQuery(userId)
                    .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(5)
                    .get();
            com.google.firebase.firestore.QuerySnapshot snapshot = com.google.android.gms.tasks.Tasks.await(task, 10, java.util.concurrent.TimeUnit.SECONDS);
            
            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
                String title = doc.getString("title");
                if (title != null && !title.isEmpty()) {
                    summary.add(title);
                } else {
                    String content = doc.getString("content");
                    if (content != null) {
                        summary.add(content.length() > 30 ? content.substring(0, 27) + "..." : content);
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("NoteRepo", "Error fetching summary", e);
        }
        return summary;
    }
}
