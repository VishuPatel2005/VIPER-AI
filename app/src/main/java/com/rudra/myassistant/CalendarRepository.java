package com.rudra.myassistant;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.UserSessionManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class CalendarRepository {

    private final FirestoreRepository firestoreRepository;
    private final UserSessionManager sessionManager;

    public CalendarRepository(android.content.Context context) {
        firestoreRepository = new FirestoreRepository();
        sessionManager = new UserSessionManager(context);
    }

    public void getEvents(DataCallback<List<CalendarEvent>> callback) {
        String userId = sessionManager.getUserId();
        firestoreRepository.getEventsQuery(userId).get().addOnSuccessListener(snapshot -> {
            List<CalendarEvent> eventsList = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                eventsList.add(new CalendarEvent(
                    (String) doc.getId(),
                    (String) doc.getString("title"),
                    (String) doc.getString("startTime"),
                    (String) doc.getString("endTime"),
                    (String) doc.getString("description"),
                    "" // location
                ));
            }
            
            // Client-side sort (Ascending by startTime)
            eventsList.sort((e1, e2) -> {
                String s1 = e1.getStartTime() != null ? e1.getStartTime() : "";
                String s2 = e2.getStartTime() != null ? e2.getStartTime() : "";
                return s1.compareTo(s2);
            });
            
            callback.onSuccess(eventsList);
        }).addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void createEvent(String title, String startTime, String endTime, String description, String location) {
        String userId = sessionManager.getUserId();
        firestoreRepository.addEvent(userId, title, startTime, endTime, description);
    }

    public void deleteEvent(String id) {
        firestoreRepository.deleteEvent(id);
    }
}
