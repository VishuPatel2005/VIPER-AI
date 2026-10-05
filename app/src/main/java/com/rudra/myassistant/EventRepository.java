package com.rudra.myassistant;

import android.content.Context;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.UserSessionManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EventRepository {
    private final FirestoreRepository firestoreRepository;
    private final UserSessionManager sessionManager;

    public EventRepository(Context context) {
        this.firestoreRepository = new FirestoreRepository();
        this.sessionManager = new UserSessionManager(context);
    }

    public List<CalendarEvent> getUpcomingEvents() {
        List<CalendarEvent> eventsList = new ArrayList<>();
        String userId = sessionManager.getUserId();
        if (userId == null) return eventsList;

        try {
            QuerySnapshot snapshot = Tasks.await(firestoreRepository.getEventsQuery(userId).get(), 10, TimeUnit.SECONDS);
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                eventsList.add(new CalendarEvent(
                    doc.getId(),
                    doc.getString("title"),
                    doc.getString("startTime"),
                    doc.getString("endTime"),
                    doc.getString("description"),
                    doc.getString("location")
                ));
            }
        } catch (Exception e) {
            android.util.Log.e("EventRepository", "Error fetching upcoming events", e);
        }
        return eventsList;
    }
}
