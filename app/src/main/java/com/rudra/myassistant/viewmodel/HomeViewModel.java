package com.rudra.myassistant.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.rudra.myassistant.AssistantSuggestionMapper;
import com.rudra.myassistant.LiveSuggestionEngine;
import com.rudra.myassistant.model.SuggestionCardModel;
import com.rudra.myassistant.repository.PreferenceRepository;
import com.rudra.myassistant.repository.SuggestionRepository;
import com.rudra.myassistant.SuggestionsManager;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * ViewModel for the Home dashboard. Orchestrates suggestion fetching from
 * multiple sources (location, time, preferences, friends) and merges them
 * through the LiveSuggestionEngine.
 * 
 * Key improvement: user preferences are now passed through to the
 * SuggestionRepository so that Geoapify queries respect selected interests.
 */
public class HomeViewModel extends AndroidViewModel {
    private final MutableLiveData<List<SuggestionCardModel>> suggestions = new MutableLiveData<>();
    private final LiveSuggestionEngine engine;
    private final PreferenceRepository prefRepo;
    private final SuggestionRepository suggestionRepo;
    private final com.rudra.myassistant.network.FirestoreRepository firestoreRepoLegacy;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        this.engine = new LiveSuggestionEngine();
        this.prefRepo = new PreferenceRepository();
        this.suggestionRepo = new SuggestionRepository();
        this.firestoreRepoLegacy = new com.rudra.myassistant.network.FirestoreRepository();
    }

    public LiveData<List<SuggestionCardModel>> getSuggestions() {
        return suggestions;
    }

    public void refreshSuggestions(String uid, double lat, double lon, List<String> nearbyFriends) {
        if (uid == null) return;
        
        prefRepo.getUserPreferences(uid).addOnCompleteListener(task -> {
            List<String> prefs = task.isSuccessful() ? task.getResult() : new ArrayList<>();
            
            new Thread(() -> {
                double finalLat = lat;
                double finalLon = lon;
                
                if (finalLat == 0 && finalLon == 0) {
                    android.location.Location syncLoc = com.rudra.myassistant.location.LocationTracker.getInstance().getCurrentLocationSync();
                    if (syncLoc != null) {
                        finalLat = syncLoc.getLatitude();
                        finalLon = syncLoc.getLongitude();
                    }
                }

                // Fetch Location Cards — now preference-aware!
                List<SuggestionCardModel> locationCards = (suggestionRepo != null) 
                        ? suggestionRepo.getLocationSuggestions(finalLat, finalLon, prefs) 
                        : new ArrayList<>();
                
                // Fetch Time Cards with rotation
                List<SuggestionCardModel> timeCards = new java.util.ArrayList<>();
                SuggestionCardModel timeCard = getRotatingTimeCard();
                if (timeCard != null) {
                    timeCards.add(timeCard);
                }

                // AI Insight or Status Cards
                List<SuggestionCardModel> assistantCards = new java.util.ArrayList<>();
                
                // Merge everything and update UI via postValue
                if (firestoreRepoLegacy != null) {
                    firestoreRepoLegacy.getRemindersQuery(uid).get().addOnCompleteListener(remindersTask -> {
                        if (remindersTask.isSuccessful() && remindersTask.getResult() != null && !remindersTask.getResult().isEmpty()) {
                            int count = remindersTask.getResult().size();
                            assistantCards.add(AssistantSuggestionMapper.mapToModel(
                                "REMINDER", 
                                count + " Upcoming Tasks", 
                                "Stay on top of your reminders", 
                                "reminders"
                            ));
                        }
                        
                        List<SuggestionCardModel> unified = engine.buildLiveSuggestions(
                                getApplication().getApplicationContext(), 
                                locationCards, 
                                timeCards, 
                                prefs, 
                                nearbyFriends, 
                                assistantCards);
                                
                        suggestions.postValue(unified);
                    });
                } else {
                    // Fallback if firestore is null (shouldn't happen)
                    List<SuggestionCardModel> unified = engine.buildLiveSuggestions(
                            getApplication().getApplicationContext(), 
                            locationCards, 
                            timeCards, 
                            prefs, 
                            nearbyFriends, 
                            assistantCards);
                    suggestions.postValue(unified);
                }
            }).start();
        });
    }

    /**
     * Generates time-based cards with multiple variants per time slot
     * so the user doesn't see the exact same "Morning Routine" text every day.
     */
    private SuggestionCardModel getRotatingTimeCard() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        
        // Use day-of-week to rotate variants — a different variant each day
        String title, subtitle;
        
        if (hour >= 5 && hour < 11) {
            String[][] variants = {
                {"Morning Routine", "Kickstart your day — check your schedule"},
                {"Rise & Shine", "A productive morning starts now"},
                {"Good Morning", "Here's what your day looks like"},
                {"Fresh Start", "Plan your morning for success"},
                {"AM Gameplan", "Set your priorities for today"},
                {"Dawn Patrol", "Early bird gets the worm 🐛"},
                {"Morning Mindset", "What matters most today?"}
            };
            int idx = dayOfWeek % variants.length;
            title = variants[idx][0];
            subtitle = variants[idx][1];
        } else if (hour >= 11 && hour < 17) {
            String[][] variants = {
                {"Afternoon Focus", "Keep the momentum going"},
                {"Midday Check-in", "How's your progress today?"},
                {"Power Hour", "Deep focus for maximum output"},
                {"Stay Sharp", "Your afternoon productivity hub"},
                {"Halfway There", "Review what's done, plan what's next"},
                {"PM Sprint", "Crush the remaining tasks"},
                {"Focus Time", "Minimize distractions, maximize output"}
            };
            int idx = dayOfWeek % variants.length;
            title = variants[idx][0];
            subtitle = variants[idx][1];
        } else if (hour >= 17 && hour < 21) {
            String[][] variants = {
                {"Evening Glow", "Reflect on today's wins"},
                {"Golden Hour", "Wind down and review your progress"},
                {"Evening Recap", "What did you accomplish today?"},
                {"Sunset Check-in", "Time to wrap up and relax"},
                {"Day's End", "Review, reflect, and reset"},
                {"Evening Mode", "Transition from work to rest"},
                {"Twilight Brief", "Your evening at a glance"}
            };
            int idx = dayOfWeek % variants.length;
            title = variants[idx][0];
            subtitle = variants[idx][1];
        } else {
            String[][] variants = {
                {"Late Night Prep", "Sleep well, plan ahead"},
                {"Nightowl Mode", "Wrapping up? Plan tomorrow now"},
                {"Wind Down", "Tomorrow is a fresh start"},
                {"Sleep Timer", "Rest is part of the plan"},
                {"Night Recap", "Jot down final thoughts before bed"},
                {"Lights Out Soon", "Set your alarms and rest"},
                {"Dream Plan", "Let your brain process today's wins"}
            };
            int idx = dayOfWeek % variants.length;
            title = variants[idx][0];
            subtitle = variants[idx][1];
        }
        
        return AssistantSuggestionMapper.mapToModel("TIME_BASED", title, subtitle, "time");
    }

    // Missing Flow/State logic for build fix if needed
    private final kotlinx.coroutines.flow.MutableStateFlow<HomeSuggestionState> suggestionState = 
        kotlinx.coroutines.flow.StateFlowKt.MutableStateFlow(new HomeSuggestionState(false, "", 0.0));

    public kotlinx.coroutines.flow.StateFlow<HomeSuggestionState> getSuggestionState() {
        return suggestionState;
    }

    public void setCurrentUid(String uid) {
        // Compatibility
    }
}
