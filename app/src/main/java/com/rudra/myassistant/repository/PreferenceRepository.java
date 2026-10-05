package com.rudra.myassistant.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.rudra.myassistant.network.FirestoreRepository;
import java.util.ArrayList;
import java.util.List;

public class PreferenceRepository {
    private final FirestoreRepository firestoreRepo;

    public PreferenceRepository() {
        this.firestoreRepo = new FirestoreRepository();
    }

    public Task<List<String>> getUserPreferences(String uid) {
        return firestoreRepo.getUser(uid).continueWith(task -> {
            DocumentSnapshot doc = task.getResult();
            if (doc != null && doc.exists()) {
                List<String> prefs = (List<String>) doc.get("preferences");
                return prefs != null ? prefs : new ArrayList<String>();
            }
            return new ArrayList<String>();
        });
    }
}
