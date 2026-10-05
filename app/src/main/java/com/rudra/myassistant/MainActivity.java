package com.rudra.myassistant;

import android.Manifest;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.PopupMenu;
import android.widget.FrameLayout;
import com.google.gson.JsonObject;
import com.rudra.myassistant.network.AIService;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.view.animation.LinearInterpolator;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import com.rudra.myassistant.helper.HomeSuggestionObserver;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;

import com.rudra.myassistant.network.FirestoreRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import com.rudra.myassistant.model.BalanceSummary;
import com.rudra.myassistant.viewmodel.ExpenseViewModel;
import com.rudra.myassistant.model.Expense;
import com.rudra.myassistant.network.UserSessionManager;
import com.google.gson.JsonObject;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import android.location.Location;



import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import android.location.Location;
import java.util.List;
import java.util.Locale;

import android.animation.AnimatorSet;
import com.rudra.myassistant.location.LocationTracker;
import androidx.core.content.ContextCompat;
import android.widget.Toast;

import com.rudra.myassistant.voice.VoiceUIController;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.rudra.myassistant.model.SuggestionCardModel;
import com.rudra.myassistant.AssistantSuggestionMapper;
import android.net.Uri;

public class MainActivity extends BaseEdgeToEdgeActivity implements LocationTracker.LocationChangeListener {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int REQUEST_IMAGE_CAPTURE = 1002;
    private static final int REQUEST_PICK_IMAGE = 1003;
    private static final int REQUEST_PICK_FILE = 1004;
    
    // UI components
    private TextView greetingHeader, userName;
    private TextView reminderChipText, meetingChipText;
    private LinearLayout reminderListContainer;
    private TextView noteTitle, notePreview, noteTime, tvNoNotes;
    private TextView eventDate, eventTime, eventTitle, eventDesc;
    private TextView dosageStatusText, periodStatusText, viewAllHealth;
    private ProgressBar dosageProgressBar, periodProgressBar;
    private LinearLayout medicationSection, periodSection;
    private EditText textInputBar;
    private ImageView sendButton, micButton, uploadButton;
    private DrawerLayout drawerLayout;
    private com.google.android.material.navigation.NavigationView navigationView;
    private RecyclerView rvChatHistory;
    private ChatHistoryAdapter chatHistoryAdapter;
    private ChatDbHelper chatDbHelper;
    private long lastErrorTime = 0;
    private static final long ERROR_COOLDOWN = 10000; // 10 seconds between error toasts
    
    private SmartContextManager smartContextManager;
    private VoiceUIController voiceUIController;
    private UserSessionManager sessionManager;
    private FirestoreRepository firestoreRepository;
    private List<SuggestionsManager.Suggestion> activeSuggestions = new java.util.ArrayList<>();
    private boolean isSuggestionsRefreshing = false;

    // Viper Motion System
    private HomeMotionController motionController;
    private ScrollAnimationManager scrollAnimManager;
    private AnimatorSet healthBreathAnimator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);
            
            sessionManager = new UserSessionManager(this);
            initViews(); // Initialize views first!
            
            try {
                firestoreRepository = new FirestoreRepository();
                homeViewModel = new androidx.lifecycle.ViewModelProvider(this).get(com.rudra.myassistant.viewmodel.HomeViewModel.class);
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Logic dependency init failed", e);
            }

            if (!sessionManager.isLoggedIn()) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }

            setupClickListeners();
            
            // Initialize required singletons
            com.rudra.myassistant.location.LocationTracker.init(this);
            smartContextManager = com.rudra.myassistant.SmartContextManager.getInstance();
            com.rudra.myassistant.location.LocationTracker.getInstance().setLocationChangeListener(this);
            
            checkAndRequestPermissions();
            
            setupSmartSuggestions();
            loadDashboardData();

            View bottomBar = findViewById(R.id.bottomBar);
            if (bottomBar != null) {
                voiceUIController = new VoiceUIController(this, (ViewGroup) bottomBar, textInputBar, micButton, sendButton, uploadButton);
            }
            
            // Handle incoming notification deep-links
            com.viperai.notifications.NotificationRouter.handleIntent(this, getIntent());

            // Schedule background workers
            scheduleNotificationWorkers();
            
            // Activate Viper Motion System
            initMotionSystem();
        } catch (Throwable t) {
            android.util.Log.e("MainActivity", "CRITICAL ERROR during onCreate", t);
            Toast.makeText(this, "A critical error occurred. Some features may be unavailable.", Toast.LENGTH_LONG).show();
        }
    }

    private com.rudra.myassistant.viewmodel.HomeViewModel homeViewModel;

    private void setupSmartSuggestions() {
        if (suggestionsAdapter == null) return;
        
        homeViewModel = new androidx.lifecycle.ViewModelProvider(this).get(com.rudra.myassistant.viewmodel.HomeViewModel.class);
        
        // Listen for suggestion updates
        if (homeViewModel != null) {
            homeViewModel.getSuggestions().observe(this, newSuggestions -> {
                if (newSuggestions != null) {
                    runOnUiThread(() -> {
                        suggestionsAdapter.setSuggestions(newSuggestions);
                        
                        // Also proactively fetch AI insights for the new suggestion context
                        fetchAiInsightForDashboard();
                    });
                }
            });
        }

        refreshDashboardSuggestions();
    }

    private void refreshDashboardSuggestions() {
        android.location.Location lastLoc = com.rudra.myassistant.location.LocationTracker.getInstance().getLastKnownLocation();
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        
        if (uid != null && homeViewModel != null) {
            double lat = lastLoc != null ? lastLoc.getLatitude() : 0;
            double lon = lastLoc != null ? lastLoc.getLongitude() : 0;
            
            java.util.List<String> friends = SuggestionsManager.getNearbyFriends();
            android.util.Log.d("MainActivity", "Refreshing suggestions. Friends nearby: " + (friends != null ? friends.size() : 0));
            homeViewModel.refreshSuggestions(uid, lat, lon, friends);
        }
    }

    private void fetchAiInsightForDashboard() {
        SuggestionsManager.fetchAiInsight(this, null, null, chatHistoryAdapter != null ? chatHistoryAdapter.getChats() : null, new com.rudra.myassistant.network.AIService.AIResponseListener() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    String[] parts = response.split("\\|");
                    if (parts.length >= 2) {
                        SuggestionCardModel card = AssistantSuggestionMapper.mapToModel("TIP", parts[0].trim(), "Personal Insight", null);
                        if (parts.length > 2) card.setSubtitle(parts[2].trim());
                        suggestionsAdapter.addSuggestion(card);
                    }
                });
            }
            @Override public void onToolCall(String fn, com.google.gson.JsonObject args) {}
            @Override public void onFailure(String err) {}
        });
    }

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private SuggestionsPagerAdapter suggestionsAdapter;
    private androidx.viewpager2.widget.ViewPager2 suggestionsViewPager;
    private android.location.Location lastProcessedLocation;
    private static final float LOCATION_REFRESH_THRESHOLD = 50.0f; // Meters



    private void initViews() {
        suggestionsViewPager = findViewById(R.id.suggestionsViewPager);
        if (suggestionsViewPager != null) {
            suggestionsAdapter = new SuggestionsPagerAdapter(this);
            suggestionsViewPager.setAdapter(suggestionsAdapter);
            
            // Setup Page Transformer for Stack Feel
            androidx.viewpager2.widget.CompositePageTransformer transformer = new androidx.viewpager2.widget.CompositePageTransformer();
            transformer.addTransformer(new androidx.viewpager2.widget.MarginPageTransformer(30));
            suggestionsViewPager.setPageTransformer(transformer);
            suggestionsViewPager.setOffscreenPageLimit(3);
            
            // Anchor logic: Removed to ensure new priority items at index 0/1 are visible immediately
            // without being shifted into the background by the current item maintainer.
            
            // Apply Dynamic Carousel Effect
            View internalRv = suggestionsViewPager.getChildAt(0);
            if (internalRv instanceof RecyclerView) {
                AnimationHelper.applyCarouselEffect((RecyclerView) internalRv);
            }
        }
        
        greetingHeader = findViewById(R.id.greetingHeader);
        userName = findViewById(R.id.userName);
        reminderChipText = findViewById(R.id.reminderChipText);
        meetingChipText = findViewById(R.id.meetingChipText);
        reminderListContainer = findViewById(R.id.reminderListContainer);
        noteTitle = findViewById(R.id.noteTitle);
        notePreview = findViewById(R.id.notePreview);
        noteTime = findViewById(R.id.noteTime);
        tvNoNotes = findViewById(R.id.tvNoNotes);
        eventDate = findViewById(R.id.eventDate);
        eventTime = findViewById(R.id.eventTime);
        eventTitle = findViewById(R.id.eventTitle);
        eventDesc = findViewById(R.id.eventDesc);
        dosageStatusText = findViewById(R.id.dosageStatusText);
        periodStatusText = findViewById(R.id.periodStatusText);
        viewAllHealth = findViewById(R.id.viewAllHealth);
        dosageProgressBar = findViewById(R.id.dosageProgressBar);
        periodProgressBar = findViewById(R.id.periodProgressBar);
        medicationSection = findViewById(R.id.medicationSection);
        periodSection = findViewById(R.id.periodSection);
        textInputBar = findViewById(R.id.textInputBar);
        sendButton = findViewById(R.id.sendButton);
        micButton = findViewById(R.id.micButton);
        uploadButton = findViewById(R.id.uploadButton);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        com.viperai.notifications.NotificationRouter.handleIntent(this, intent);
    }

    private void scheduleNotificationWorkers() {
        androidx.work.WorkManager wm = androidx.work.WorkManager.getInstance(this);
        
        // SuggestionWorker (3 hours)
        wm.enqueueUniquePeriodicWork(com.viperai.notifications.NotificationConstants.TAG_SUGGESTION_WORKER,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                new androidx.work.PeriodicWorkRequest.Builder(com.viperai.workers.SuggestionWorker.class, 3, java.util.concurrent.TimeUnit.HOURS)
                        .addTag(com.viperai.notifications.NotificationConstants.TAG_SUGGESTION_WORKER)
                        .build());

        // FriendProximityWorker (15 minutes)
        wm.enqueueUniquePeriodicWork(com.viperai.notifications.NotificationConstants.TAG_FRIEND_WORKER,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                new androidx.work.PeriodicWorkRequest.Builder(com.viperai.workers.FriendProximityWorker.class, 15, java.util.concurrent.TimeUnit.MINUTES)
                        .addTag(com.viperai.notifications.NotificationConstants.TAG_FRIEND_WORKER)
                        .build());

        // ExpenseWorker (24 hours)
        wm.enqueueUniquePeriodicWork(com.viperai.notifications.NotificationConstants.TAG_EXPENSE_WORKER,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                new androidx.work.PeriodicWorkRequest.Builder(com.viperai.workers.ExpenseWorker.class, 24, java.util.concurrent.TimeUnit.HOURS)
                        .addTag(com.viperai.notifications.NotificationConstants.TAG_EXPENSE_WORKER)
                        .build());

        // EventWorker (5 minutes)
        wm.enqueueUniquePeriodicWork(com.viperai.notifications.NotificationConstants.TAG_EVENT_WORKER,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                new androidx.work.PeriodicWorkRequest.Builder(com.viperai.workers.EventWorker.class, 5, java.util.concurrent.TimeUnit.MINUTES)
                        .addTag(com.viperai.notifications.NotificationConstants.TAG_EVENT_WORKER)
                        .build());

        // Channel Setup
        com.viperai.notifications.NotificationHelper.createAllChannels(this);
    }

    public void scrollToSuggestionsSection() {
        androidx.core.widget.NestedScrollView scrollView = findViewById(R.id.scrollView);
        if (scrollView != null && suggestionsViewPager != null) {
            scrollView.smoothScrollTo(0, suggestionsViewPager.getTop());
            
            suggestionsViewPager.postDelayed(() -> {
                if (suggestionsViewPager.getChildCount() > 0) {
                    View firstChild = suggestionsViewPager.getChildAt(0);
                    if (firstChild instanceof RecyclerView) {
                        RecyclerView rv = (RecyclerView) firstChild;
                        View firstCard = rv.getChildAt(0);
                        if (firstCard != null) {
                            android.animation.ValueAnimator pulse = android.animation.ValueAnimator.ofFloat(1.0f, 0.5f, 1.0f);
                            pulse.setDuration(800);
                            pulse.addUpdateListener(animation -> firstCard.setAlpha((float) animation.getAnimatedValue()));
                            pulse.start();
                        }
                    } else if (firstChild != null) {
                        // If it's not a RecyclerView, animate the child itself
                        android.animation.ValueAnimator pulse = android.animation.ValueAnimator.ofFloat(1.0f, 0.5f, 1.0f);
                        pulse.setDuration(800);
                        pulse.addUpdateListener(animation -> firstChild.setAlpha((float) animation.getAnimatedValue()));
                        pulse.start();
                    }
                }
            }, 800);
        }
    }

    public void scrollToExpense(String id) {
        Intent intent = new Intent(this, ConnectActivity.class);
        intent.putExtra("ACTION", "SHOW_EXPENSES");
        intent.putExtra("EXTRA_EXPENSE_ID", id);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateProfileImages();
        setupSidebar();
        loadDashboardData();

        LocationTracker tracker = LocationTracker.getInstance();
        if (tracker != null) {
            tracker.setLocationChangeListener(this);
        }

        // Proactively refresh suggestions on resume to pick up new nearby friends/context
        refreshDashboardSuggestions();
        
        // Ensure background voice assistant is listening for wake word
        com.rudra.myassistant.voice.VoicePipelineManager.getInstance(this).start();
    }

    private void updateProfileImages() {
        ImageView mainProfile = findViewById(R.id.profileImage);
        String photoUrl = sessionManager.getUserPhotoUrl();

        if (photoUrl != null && mainProfile != null) {
            Glide.with(this)
                    .load(photoUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(mainProfile);
        }
    }

    private void setupSidebar() {
        navigationView = findViewById(R.id.navigationView);
        if (navigationView == null) return;
        
        chatDbHelper = new ChatDbHelper(this);
        // Clean up old chats (10 days)
        long tenDaysAgo = System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000);
        chatDbHelper.deleteOldChats(tenDaysAgo);

        rvChatHistory = navigationView.findViewById(R.id.rvChatHistory);
        if (rvChatHistory == null) return; 

        rvChatHistory.setLayoutManager(new LinearLayoutManager(this));
        chatHistoryAdapter = new ChatHistoryAdapter(chat -> {
            // Load selected chat
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("chat_id", chat.getChatId());
            startActivity(intent);
            drawerLayout.closeDrawers();
        });
        chatHistoryAdapter.setOnChatLongClickListener((chat, anchor) -> {
            PopupMenu popup = new PopupMenu(this, anchor);
            popup.getMenu().add("Delete");
            popup.setOnMenuItemClickListener(item -> {
                if ("Delete".equals(item.getTitle())) {
                    chatDbHelper.deleteChat(chat.getChatId());
                    loadChatHistory();
                    return true;
                }
                return false;
            });
            popup.show();
        });
        rvChatHistory.setAdapter(chatHistoryAdapter);

        View btnSettings = navigationView.findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                startActivity(new Intent(this, SettingsActivity.class));
                drawerLayout.closeDrawers();
            });
        }

        View btnLogout = navigationView.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                performLogout();
            });
        }

        // Update sidebar profile section
        TextView sidebarName = navigationView.findViewById(R.id.sidebarUserName);
        TextView sidebarUsername = navigationView.findViewById(R.id.sidebarUserUsername);
        TextView sidebarEmail = navigationView.findViewById(R.id.sidebarUserEmail);
        ImageView sidebarProfile = navigationView.findViewById(R.id.sidebarProfileImage);
        
        String name = sessionManager.getUserName();
        String username = sessionManager.getUsername();
        
        if (sidebarName != null && name != null) sidebarName.setText(name);
        
        if (sidebarUsername != null && username != null) {
            sidebarUsername.setText("@" + username);
            sidebarUsername.setVisibility(View.VISIBLE);
        } else if (sidebarUsername != null) {
            sidebarUsername.setVisibility(View.GONE);
        }
        
        if (sidebarEmail != null) {
            com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                sidebarEmail.setText(user.getEmail());
            }
        }

        if (sidebarProfile != null) {
            String photoUrl = sessionManager.getUserPhotoUrl();
            if (photoUrl != null) {
                Glide.with(this)
                        .load(photoUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_person)
                        .circleCrop()
                        .into(sidebarProfile);
            }
        }

        loadChatHistory();
        setupSidebarShortcuts();
    }

    private void setupSidebarShortcuts() {
        if (drawerLayout == null || navigationView == null) return;
        
        // Find views once to avoid redundant traversals
        View eventsBtn = navigationView.findViewById(R.id.btnNavEvents);
        View remindersBtn = navigationView.findViewById(R.id.btnNavReminders);
        View healthBtn = navigationView.findViewById(R.id.btnNavHealth);
        View notesBtn = navigationView.findViewById(R.id.btnNavNotes);

        // Set listeners safely
        setSafeNavListener(eventsBtn, CalendarActivity.class);
        setSafeNavListener(remindersBtn, RemindersActivity.class);
        setSafeNavListener(healthBtn, HealthActivity.class);
        setSafeNavListener(notesBtn, NotesActivity.class);

        // Apply press animations for high-quality Feel
        if (eventsBtn != null) AnimationHelper.applyPressAnimation(eventsBtn);
        if (remindersBtn != null) AnimationHelper.applyPressAnimation(remindersBtn);
        if (healthBtn != null) AnimationHelper.applyPressAnimation(healthBtn);
        if (notesBtn != null) AnimationHelper.applyPressAnimation(notesBtn);
    }

    private void setSafeNavListener(View view, Class<?> activityClass) {
        if (view == null) return;
        view.setOnClickListener(v -> {
            try {
                if (drawerLayout != null) {
                    drawerLayout.closeDrawers();
                }
                
                // Use a handler to avoid issues with detached views and race conditions
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        if (isFinishing() || isDestroyed()) return;
                        Intent intent = new Intent(MainActivity.this, activityClass);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_fade_in, R.anim.hold);
                    } catch (Exception e) {
                        android.util.Log.e("SidebarNav", "Failed to navigate to " + activityClass.getSimpleName(), e);
                    }
                }, 280); 
            } catch (Exception e) {
                android.util.Log.e("SidebarNav", "Failed to setup navigation", e);
            }
        });
    }

    private void loadChatHistory() {
        if (chatHistoryAdapter != null && chatDbHelper != null) {
            chatHistoryAdapter.setChats(chatDbHelper.getAllChats());
        }
    }

    private void setupClickListeners() {
        // Viper: container transform transitions
        View viewAllReminders = findViewById(R.id.viewAllReminders);
        AnimationHelper.applyPressAnimation(viewAllReminders);
        viewAllReminders.setOnClickListener(v -> 
            AnimationHelper.animateContainerTransform(v, () -> {
                startActivity(new Intent(this, RemindersActivity.class));
                overridePendingTransition(R.anim.slide_fade_in, R.anim.hold);
            }));

        findViewById(R.id.notesLayout).setOnClickListener(v -> {
            startActivity(new Intent(this, NotesActivity.class));
            overridePendingTransition(R.anim.slide_fade_in, R.anim.hold);
        });

        View latestNoteCard = findViewById(R.id.latestNoteCard);
        latestNoteCard.setOnClickListener(v -> 
            AnimationHelper.animateContainerTransform(v, () -> {
                startActivity(new Intent(this, NotesActivity.class));
                overridePendingTransition(R.anim.slide_fade_in, R.anim.hold);
            }));

        findViewById(R.id.calendarLayout).setOnClickListener(v -> {
            startActivity(new Intent(this, CalendarActivity.class));
            overridePendingTransition(R.anim.slide_fade_in, R.anim.hold);
        });

        View nextEventCard = findViewById(R.id.nextEventCard);
        nextEventCard.setOnClickListener(v -> 
            AnimationHelper.animateContainerTransform(v, () -> {
                startActivity(new Intent(this, CalendarActivity.class));
                overridePendingTransition(R.anim.slide_fade_in, R.anim.hold);
            }));

        View healthLayout = findViewById(R.id.healthLayout);
        View.OnClickListener healthAction = v -> 
            AnimationHelper.animateContainerTransform(v, () -> {
                startActivity(new Intent(this, HealthActivity.class));
                overridePendingTransition(R.anim.slide_fade_in, R.anim.hold);
            });
        
        if (healthLayout != null) healthLayout.setOnClickListener(healthAction);
        if (viewAllHealth != null) {
            AnimationHelper.applyPressAnimation(viewAllHealth);
            viewAllHealth.setOnClickListener(healthAction);
        }

        View globeButton = findViewById(R.id.globeButton);
        AnimationHelper.applyPressAnimation(globeButton);
        globeButton.setOnClickListener(v -> 
            AnimationHelper.animateContainerTransform(v, () -> {
                startActivity(new Intent(this, ConnectActivity.class));
                overridePendingTransition(R.anim.slide_fade_in, R.anim.hold);
            }));

        sendButton.setOnClickListener(v -> sendMessage());

        // Viper: button press micro-interactions
        AnimationHelper.applyPressAnimation(sendButton);
        AnimationHelper.applyPressAnimation(micButton);
        AnimationHelper.applyPressAnimation(uploadButton);

        textInputBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        micButton.setOnClickListener(v -> {
            com.rudra.myassistant.voice.VoicePipelineManager.getInstance(this).onWakeWordDetected();
        });

        uploadButton.setOnClickListener(this::showUploadMenu);
        
        findViewById(R.id.profileImage).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        findViewById(R.id.menuButton).setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(Gravity.LEFT);
            }
        });

        if (medicationSection != null) {
            medicationSection.setOnClickListener(v -> 
                startActivity(new Intent(this, MedicationDetailActivity.class)));
        }

        if (periodSection != null) {
            periodSection.setOnClickListener(v -> 
                startActivity(new Intent(this, PeriodDetailActivity.class)));
        }
    }

    private void performLogout() {
        // 1. Sign out from Firebase
        FirebaseAuth.getInstance().signOut();

        // 2. Clear local session
        sessionManager.logoutUser();

        // 3. Stop background services
        stopService(new Intent(this, com.rudra.myassistant.voice.VoiceAssistantService.class));
        LocationTracker instance = LocationTracker.getInstance();
        if (instance != null) instance.stopTracking();

        // 4. Sign out from Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignInClient.signOut();

        // 5. Return to login screen
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    private void showUploadMenu(View v) {
        ContextThemeWrapper wrapper = new ContextThemeWrapper(this, R.style.ThemeOverlay_App_DarkPopupMenu);
        PopupMenu popup = new PopupMenu(wrapper, v);
        popup.getMenuInflater().inflate(R.menu.upload_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_camera) {
                Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                } else {
                    Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (id == R.id.action_photos) {
                Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhotoIntent, REQUEST_PICK_IMAGE);
                return true;
            } else if (id == R.id.action_files) {
                Intent pickFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
                pickFileIntent.setType("*/*");
                startActivityForResult(pickFileIntent, REQUEST_PICK_FILE);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void sendMessage() {
        String message = textInputBar.getText().toString().trim();
        if (!message.isEmpty()) {
            textInputBar.setText("");
            
            // Generate a fresh session ID for a new chat thread
            String newChatId = java.util.UUID.randomUUID().toString();
            
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_INITIAL_MESSAGE, message);
            intent.putExtra("chat_id", newChatId);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_fade_in, R.anim.hold);
        }
    }

    // ── Viper Motion System ──────────────────────────────────────────

    private void initMotionSystem() {
        try {
            motionController = new HomeMotionController(this);

            // Part 1: Toolbar hide on scroll
            View topBar = findViewById(R.id.topBar);
            androidx.core.widget.NestedScrollView scrollView = findViewById(R.id.scrollView);
            if (topBar != null && scrollView != null) {
                scrollAnimManager = new ScrollAnimationManager(topBar);
                scrollAnimManager.attachToNestedScrollView(scrollView);
            }

            // Part 2: Staggered card entrance animation
            List<View> dashboardCards = new ArrayList<>();
            View suggestionsVP = findViewById(R.id.suggestionsViewPager);
            View nextEventCardView = findViewById(R.id.nextEventCard);
            View reminderContainer = findViewById(R.id.reminderListContainer);
            View latestNoteCardView = findViewById(R.id.latestNoteCard);
            View healthSummaryCard = findViewById(R.id.healthSummaryCard);
            if (suggestionsVP != null) dashboardCards.add(suggestionsVP);
            if (nextEventCardView != null) dashboardCards.add(nextEventCardView);
            if (reminderContainer != null) dashboardCards.add(reminderContainer);
            if (latestNoteCardView != null) dashboardCards.add(latestNoteCardView);
            if (healthSummaryCard != null) dashboardCards.add(healthSummaryCard);
            // Use post() to defer until after first layout — otherwise animation plays before screen is drawn
            View mainRoot = findViewById(R.id.main_content_root);
            final List<View> finalCards = dashboardCards;
            if (mainRoot != null) {
                mainRoot.post(() -> motionController.animateCardsEntrance(finalCards));
            } else {
                motionController.animateCardsEntrance(finalCards);
            }

            // Part 9: Health icon breathing animation
            if (healthSummaryCard != null) {
                View pillIcon = healthSummaryCard.findViewById(R.id.dosageProgressBar);
                if (pillIcon != null) {
                    healthBreathAnimator = AnimationHelper.applyBreathingAnimation(pillIcon);
                }
            }

            // Part 11: Alive Floating Icons
            View globeButton = findViewById(R.id.globeButton);
            if (globeButton != null) AnimationHelper.applyFloatingAnimation(globeButton);
            
            View micButton = findViewById(R.id.micButton);
            if (micButton != null) AnimationHelper.applyFloatingAnimation(micButton);

            View viperLogo = findViewById(R.id.tvToolbarTitle);
            if (viperLogo != null) AnimationHelper.applyBreathingAnimation(viperLogo);

            // Part 10: Input bar focus animation
            if (textInputBar != null) {
                View inputContainer = textInputBar.getParent() instanceof View ? (View) textInputBar.getParent() : null;
                if (inputContainer != null) {
                    textInputBar.setOnFocusChangeListener((v, hasFocus) -> {
                        int from = AnimationHelper.dpToPx(this, hasFocus ? 48 : 54);
                        int to = AnimationHelper.dpToPx(this, hasFocus ? 54 : 48);
                        android.animation.ValueAnimator heightAnim = android.animation.ValueAnimator.ofInt(from, to);
                        heightAnim.addUpdateListener(anim -> {
                            inputContainer.getLayoutParams().height = (int) anim.getAnimatedValue();
                            inputContainer.requestLayout();
                        });
                        heightAnim.setDuration(120);
                        heightAnim.setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator());
                        heightAnim.start();
                        if (sendButton != null) {
                            sendButton.animate().alpha(hasFocus ? 1f : 0.7f).setDuration(120).start();
                        }
                    });
                }
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Motion system init failed (non-fatal)", e);
        }
    }




    @Override
    protected void onPause() {
        super.onPause();
        LocationTracker tracker = LocationTracker.getInstance();
        if (tracker != null) {
            tracker.setLocationChangeListener(null);
        }
        // Viper: cancel breathing animation to prevent leaks
        if (healthBreathAnimator != null) {
            healthBreathAnimator.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceUIController != null) {
            voiceUIController.destroy();
        }
        if (healthBreathAnimator != null) {
            healthBreathAnimator.cancel();
            healthBreathAnimator = null;
        }
        com.rudra.myassistant.location.LocationTracker.getInstance().setLocationChangeListener(null);
    }

    private void loadDashboardData() {
        updateTimeBasedGreeting();
        fetchReminders();
        fetchNotes();
        fetchCalendarEvents();
        fetchMedicationStatus();
        fetchPeriodStatus();
    }



    private void updateTimeBasedGreeting() {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour >= 4 && hour < 12) greeting = "Good Morning";
        else if (hour >= 12 && hour < 17) greeting = "Good Afternoon";
        else greeting = "Good Evening";
        
        if (greetingHeader != null) greetingHeader.setText(greeting);
        
        String name = sessionManager.getUserName();
        if (userName != null) {
            userName.setText(name != null ? name : "User");
        }
    }

    private void fetchReminders() {
        String userId = sessionManager.getUserId();
        firestoreRepository.getRemindersQuery(userId).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (isFinishing() || isDestroyed()) return;
            if (reminderListContainer == null) return;
            int activeCount = 0;
            reminderListContainer.removeAllViews();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                if (Boolean.FALSE.equals(doc.getBoolean("completed"))) {
                    activeCount++;
                    if (activeCount <= 2) {
                        addReminderPreview(doc);
                    }
                }
            }
            if (reminderChipText != null) reminderChipText.setText(activeCount + " reminders");
            if (activeCount == 0) {
                TextView empty = new TextView(this);
                empty.setText("No pending reminders");
                empty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                empty.setPadding(20, 20, 20, 20);
                reminderListContainer.addView(empty);
            }
        });
    }

    private void addReminderPreview(QueryDocumentSnapshot doc) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_reminder_preview, reminderListContainer, false);
        TextView text = view.findViewById(R.id.reminderText);
        text.setText(doc.getString("text"));
        
        ImageView checkBtn = view.findViewById(R.id.reminderCheckBtn);
        View mainContent = view.findViewById(R.id.reminderMainContent);
        View completionContent = view.findViewById(R.id.reminderCompletionContent);
        View undoBtn = view.findViewById(R.id.undoReminderBtn);

        Handler completionHandler = new Handler(Looper.getMainLooper());
        Runnable completionRunnable = () -> {
            firestoreRepository.getRemindersCol().document(doc.getId()).update("completed", true)
                .addOnSuccessListener(aVoid -> {
                    reminderListContainer.removeView(view);
                    loadDashboardData();
                });
        };

        checkBtn.setOnClickListener(v -> {
            mainContent.setVisibility(View.GONE);
            completionContent.setVisibility(View.VISIBLE);
            completionHandler.postDelayed(completionRunnable, 2000);
        });

        undoBtn.setOnClickListener(v -> {
            completionHandler.removeCallbacks(completionRunnable);
            completionContent.setVisibility(View.GONE);
            mainContent.setVisibility(View.VISIBLE);
        });

        reminderListContainer.addView(view);
    }

    private void fetchNotes() {
        String userId = sessionManager.getUserId();
        firestoreRepository.getNotesQuery(userId).limit(1).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (isFinishing() || isDestroyed()) return;
            if (!queryDocumentSnapshots.isEmpty()) {
                QueryDocumentSnapshot latest = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                noteTitle.setText(latest.getString("title"));
                notePreview.setText(latest.getString("content"));
                noteTime.setText("Latest Note");
                
                noteTitle.setVisibility(View.VISIBLE);
                notePreview.setVisibility(View.VISIBLE);
                noteTime.setVisibility(View.VISIBLE);
                tvNoNotes.setVisibility(View.GONE);
            } else {
                noteTitle.setVisibility(View.GONE);
                notePreview.setVisibility(View.GONE);
                noteTime.setVisibility(View.GONE);
                tvNoNotes.setVisibility(View.VISIBLE);
            }
        });
    }

    private void fetchCalendarEvents() {
        String userId = sessionManager.getUserId();
        firestoreRepository.getEventsQuery(userId).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (isFinishing() || isDestroyed()) return;
            
            int todayMeetings = 0;
            Calendar today = Calendar.getInstance();
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

            androidx.cardview.widget.CardView card = findViewById(R.id.nextEventCard);
            if (card == null) return;

            if (queryDocumentSnapshots.isEmpty()) {
                // Show Empty State
                card.removeAllViews();
                View emptyView = getLayoutInflater().inflate(R.layout.item_empty_calendar, card, false);
                TextView dayNum = emptyView.findViewById(R.id.emptyDayNumber);
                TextView monText = emptyView.findViewById(R.id.emptyMonthText);
                
                if (dayNum != null) dayNum.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(new Date()));
                if (monText != null) monText.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(new Date()).toUpperCase());
                
                card.addView(emptyView);
                if (meetingChipText != null) meetingChipText.setText("No meetings");
                return;
            }

            // Restore original layout for actual events
            boolean first = true;
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                try {
                    String startStr = doc.getString("startTime");
                    if (startStr == null) continue;
                    Date start = iso.parse(startStr);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(start);
                    
                    if (cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                        todayMeetings++;
                    }

                    if (first) {
                        // We need to re-inflate or ensure the standard views exist
                        if (card.getChildCount() > 0 && card.findViewById(R.id.eventTitle) == null) {
                            card.removeAllViews();
                            getLayoutInflater().inflate(R.layout.layout_event_content, card, true);
                            // Re-binding logic
                            eventTitle = card.findViewById(R.id.eventTitle);
                            eventDesc = card.findViewById(R.id.eventDesc);
                            eventDate = card.findViewById(R.id.eventDate);
                            eventTime = card.findViewById(R.id.eventTime);
                        }
                        
                        if (eventTitle != null) eventTitle.setText(doc.getString("title"));
                        if (eventDesc != null) eventDesc.setText(doc.getString("description"));
                        if (eventDate != null) eventDate.setText(new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(start));
                        if (eventTime != null) eventTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(start));
                        first = false;
                    }
                } catch (Exception ignored) {}
            }
            if (meetingChipText != null) meetingChipText.setText(todayMeetings + (todayMeetings == 1 ? " meeting" : " meetings"));
        });
    }

    private void fetchMedicationStatus() {
        String userId = sessionManager.getUserId();
        firestoreRepository.getMedicationsQuery(userId).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (isFinishing() || isDestroyed()) return;
            
            int total = queryDocumentSnapshots.size();
            int taken = 0;
            QueryDocumentSnapshot nextMed = null;

            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                boolean isTaken = Boolean.TRUE.equals(doc.getBoolean("taken"));
                if (isTaken) taken++;
                else if (nextMed == null) nextMed = doc;
            }

            dosageStatusText.setText(taken + " / " + total);
            if (total > 0) {
                dosageProgressBar.setProgress(taken * 100 / total);
            } else {
                dosageProgressBar.setProgress(0);
                dosageStatusText.setText("--");
            }
        });
    }

    private void fetchPeriodStatus() {
        if (periodStatusText == null || periodProgressBar == null) return;

        String userId = sessionManager.getUserId();
        firestoreRepository.getUser(userId).addOnSuccessListener(doc -> {
            if (isFinishing() || isDestroyed()) return;
            
            if (doc.exists() && doc.contains("last_period_start")) {
                String startDateStr = doc.getString("last_period_start");
                if (startDateStr == null || startDateStr.isEmpty()) {
                    periodStatusText.setText("Setup");
                    periodProgressBar.setProgress(0);
                    return;
                }

                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date startDate = sdf.parse(startDateStr);
                    long lastStart = startDate.getTime();
                    long cycleMs = 28L * 24 * 60 * 60 * 1000;
                    
                    long timeSinceStart = System.currentTimeMillis() - lastStart;
                    long dayInCycleMs = timeSinceStart % cycleMs;
                    int dayInCycle = (int) (dayInCycleMs / (1000.0 * 60 * 60 * 24)) + 1; // 1-indexed

                    periodStatusText.setText("Day " + dayInCycle);
                    periodProgressBar.setProgress(dayInCycle * 100 / 28);
                    
                } catch (Exception e) {
                    periodStatusText.setText("Err");
                    periodProgressBar.setProgress(0);
                }
            } else {
                periodStatusText.setText("Setup");
                periodProgressBar.setProgress(0);
            }
        });
    }

    private void checkAndRequestPermissions() {
    boolean needAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED;
    boolean needNotification = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
                               ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;
    boolean needLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;

    if (needAudio || needNotification || needLocation) {
        java.util.List<String> perms = new java.util.ArrayList<>();
        if (needAudio) perms.add(Manifest.permission.RECORD_AUDIO);
        if (needNotification) perms.add(Manifest.permission.POST_NOTIFICATIONS);
        if (needLocation) {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
            perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), PERMISSION_REQUEST_CODE);
    } else {
        startVoiceAssistantService();
        LocationTracker instance = LocationTracker.getInstance();
        if (instance != null) {
            instance.startTracking();
        }
    }
}

    @Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == PERMISSION_REQUEST_CODE) {
        boolean audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        boolean locationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        
        if (audioGranted) startVoiceAssistantService();
        if (locationGranted) {
            LocationTracker instance = LocationTracker.getInstance();
            if (instance != null) {
                instance.startTracking();
            }
        } else {
            Toast.makeText(this, "Location permission denied. Tracking disabled.", Toast.LENGTH_SHORT).show();
        }
    }
}

    private void handleNetworkError() {
        long now = System.currentTimeMillis();
        if (now - lastErrorTime > ERROR_COOLDOWN) {
            lastErrorTime = now;
            runOnUiThread(() -> {
                Toast.makeText(this, "Something went wrong. Please check your internet connection.", Toast.LENGTH_LONG).show();
            });
        }
    }

    private void startVoiceAssistantService() {
        Intent serviceIntent = new Intent(this, com.rudra.myassistant.voice.VoiceAssistantService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
    @Override
    public void onLocationChanged(android.location.Location location) {
        if (location == null) return;

        // Prevent shifting/refreshing for minor GPS jitter (only refresh if moved > 50m)
        if (lastProcessedLocation != null && location.distanceTo(lastProcessedLocation) < LOCATION_REFRESH_THRESHOLD) {
            return;
        }
        lastProcessedLocation = location;

        if (smartContextManager != null && sessionManager != null) {
            smartContextManager.runContextChecks(
                    sessionManager.getUserId(),
                    location.getLatitude(),
                    location.getLongitude(),
                    sessionManager.getUserPreferences(),
                    new SmartContextManager.SuggestionListener() {
                        @Override
                        public void onSuggestionReady(SuggestionsManager.Suggestion suggestion) {
                            runOnUiThread(() -> {
                                if (suggestion != null) {
                                    SuggestionCardModel model = AssistantSuggestionMapper.mapToModel(
                                        suggestion.type, suggestion.title, suggestion.subtitle, suggestion.placeName);
                                    suggestionsAdapter.addSuggestion(model);
                                }
                            });
                        }
                        @Override public void onClearSuggestion() {}
                    }
            );
        }
        // Trigger refined suggestion engine on location move
        if (homeViewModel != null) {
            refreshDashboardSuggestions();
        }
    }
}
