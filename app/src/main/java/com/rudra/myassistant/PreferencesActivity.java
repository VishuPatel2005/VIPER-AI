package com.rudra.myassistant;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.UserSessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PreferencesActivity extends BaseEdgeToEdgeActivity {

    private UserSessionManager sessionManager;
    private FirestoreRepository firestoreRepo;
    private RecyclerView rvPreferences;
    private PreferenceAdapter adapter;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable syncRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        sessionManager = new UserSessionManager(this);
        firestoreRepo = new FirestoreRepository();

        setupToolbar();
        setupRecyclerView();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvPreferences = findViewById(R.id.rvPreferences);
        rvPreferences.setLayoutManager(new GridLayoutManager(this, 2));

        List<PreferenceAdapter.PreferenceItem> items = new ArrayList<>();
        items.add(new PreferenceAdapter.PreferenceItem("cafes", "Cafes", R.drawable.ic_cafe));
        items.add(new PreferenceAdapter.PreferenceItem("food", "Restaurants", R.drawable.ic_food));
        items.add(new PreferenceAdapter.PreferenceItem("gym", "Gym & Fitness", R.drawable.ic_gym));
        items.add(new PreferenceAdapter.PreferenceItem("study", "Study Spots", R.drawable.ic_notes));
        items.add(new PreferenceAdapter.PreferenceItem("shopping", "Shopping", R.drawable.ic_search));
        items.add(new PreferenceAdapter.PreferenceItem("movies", "Movies", R.drawable.ic_camera_outline));
        items.add(new PreferenceAdapter.PreferenceItem("parks", "Parks & Nature", R.drawable.ic_globe));
        items.add(new PreferenceAdapter.PreferenceItem("desserts", "Desserts", R.drawable.ic_food));
        items.add(new PreferenceAdapter.PreferenceItem("hangout", "Social Hubs", R.drawable.ic_contacts));
        items.add(new PreferenceAdapter.PreferenceItem("music", "Music Events", R.drawable.ic_pulse));
        items.add(new PreferenceAdapter.PreferenceItem("sports", "Sports", R.drawable.ic_globe));
        items.add(new PreferenceAdapter.PreferenceItem("gaming", "Gaming Zone", R.drawable.ic_keyboard));
        items.add(new PreferenceAdapter.PreferenceItem("tech", "Tech & Gadgets", R.drawable.ic_wifi));

        Set<String> selected = sessionManager.getUserPreferences();

        adapter = new PreferenceAdapter(items, selected, currentPrefs -> {
            sessionManager.setUserPreferences(currentPrefs);
            // Invalidate suggestion cache so home page reflects new preferences immediately
            SuggestionsManager.invalidateCache();
            scheduleSync(currentPrefs);
        });

        rvPreferences.setAdapter(adapter);
    }

    private void scheduleSync(Set<String> currentPrefs) {
        if (syncRunnable != null) {
            debounceHandler.removeCallbacks(syncRunnable);
        }

        syncRunnable = () -> {
            String uid = sessionManager.getUserId();
            if (uid != null) {
                List<String> prefList = new ArrayList<>(currentPrefs);
                firestoreRepo.updateUserPreferences(uid, prefList)
                        .addOnSuccessListener(aVoid -> {
                            // Silent success
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to sync preferences", Toast.LENGTH_SHORT).show();
                        });
            }
        };

        debounceHandler.postDelayed(syncRunnable, 2000);
    }
}
