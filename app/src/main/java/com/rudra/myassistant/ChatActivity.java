package com.rudra.myassistant;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;
import android.content.Intent;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import com.google.firebase.auth.FirebaseAuth;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rudra.myassistant.network.AIService;
import com.rudra.myassistant.network.ApiConfig;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.PromptManager;
import com.rudra.myassistant.network.ReminderInterpreter;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.rudra.myassistant.network.SessionIdProvider;
import com.rudra.myassistant.network.UserSessionManager;
import com.rudra.myassistant.network.RetrofitClient;
import com.rudra.myassistant.voice.VoiceStateManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import com.rudra.myassistant.model.LatLng;
import com.viperai.assistant.AssistantOrchestrator;
import com.viperai.assistant.bridge.AssistantFeatureBridge;
import com.rudra.myassistant.network.OpenAiService;
import android.location.Geocoder;
import android.location.Address;
import java.io.IOException;

/**
 * Provides a conversational UI that listens to the global VoiceStateManager for hands-free 
 * interaction (Google Assistant / ChatGPT style) while still allowing manual keyboard input.
 */
public class ChatActivity extends BaseEdgeToEdgeActivity implements VoiceStateManager.VoiceStateObserver {

    public static final String EXTRA_INITIAL_MESSAGE = "extra_initial_message";
    private static final String TAG = "ChatActivity";

    private AIService aiService;
    private FirestoreRepository firestoreRepository;
    private ChatAdapter chatAdapter;
    private EditText inputField;
    private ImageView sendButton;
    private ImageView micButton;
    private SessionIdProvider sessionIdProvider;
    private UserSessionManager sessionManager;
    private DrawerLayout drawerLayout;
    private com.google.android.material.navigation.NavigationView navigationView;
    private RecyclerView rvChatHistory;
    private ChatHistoryAdapter chatHistoryAdapter;
    private ChatDbHelper chatDbHelper;
    private FirebaseAuth mAuth;
    
    // Conversation State (Migrated from backend)
    private String pendingAction = null;
    private JsonObject pendingArgs = null;
    private String lastUserMessage = "";

    // Hangout Mode State
    private String chatMode = "NORMAL";
    private String hangoutFriendName;
    private String hangoutFriendUid;
    private LatLng hangoutFriendLatLng;
    private LatLng hangoutUserLatLng;
    private HangoutSuggestionManager hangoutManager;

    
    private ObjectAnimator micAnimator;

    private AssistantOrchestrator orchestrator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        aiService = new AIService();
        firestoreRepository = new FirestoreRepository();
        chatDbHelper = new ChatDbHelper(this);
        // Clean up old chats (10 days)
        long tenDaysAgo = System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000);
        chatDbHelper.deleteOldChats(tenDaysAgo);
        
        sessionIdProvider = new SessionIdProvider(this);
        sessionManager = new UserSessionManager(this);
        mAuth = FirebaseAuth.getInstance();
        hangoutManager = new HangoutSuggestionManager();

        // Initialize Intent Detection Layer
        ConnectRepository connectRepo = new ConnectRepository(this);
        ExpenseRepository expenseRepo = new ExpenseRepository(this);
        ReminderRepository reminderRepo = new ReminderRepository(this);
        NoteRepository noteRepo = new NoteRepository(this);
        AssistantFeatureBridge bridge = new AssistantFeatureBridge(connectRepo, expenseRepo, reminderRepo, noteRepo);
        orchestrator = new AssistantOrchestrator(bridge, new OpenAiService());

        drawerLayout = findViewById(R.id.chatDrawerLayout);
        setupSidebar();

        findViewById(R.id.chat_title).setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(androidx.core.view.GravityCompat.START);
            }
        });


        RecyclerView recyclerView = findViewById(R.id.chat_recycler_view);
        inputField = findViewById(R.id.chat_input_field);
        sendButton = findViewById(R.id.chat_send_button);
        ImageView backButton = findViewById(R.id.chat_back_button);

        chatAdapter = new ChatAdapter();
        recyclerView.setAdapter(chatAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        backButton.setOnClickListener(v -> finish());
        AnimationHelper.applyPressAnimation(backButton);
        
        micButton = findViewById(R.id.chat_mic_button);
        AnimationHelper.applyPressAnimation(micButton);
        micButton.setOnClickListener(v -> {
            com.rudra.myassistant.voice.VoicePipelineManager.getInstance(this).onWakeWordDetected();
        });

        AnimationHelper.applyPressAnimation(sendButton);
        sendButton.setOnClickListener(v -> handleManualSend());

        // Auto-scroll and Lift Input Bar when keyboard opens
        View rootLayout = findViewById(R.id.chatDrawerLayout);
        View inputContainer = findViewById(R.id.chat_input_container);
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            // Lift the input container by the keyboard height
            if (inputContainer != null) {
                inputContainer.setTranslationY(-imeInsets.bottom + systemBars.bottom);
            }
            
            // Scroll to bottom if keyboard becomes visible
            if (imeInsets.bottom > 0) {
                recyclerView.postDelayed(this::scrollToBottom, 50);
            }
            
            return insets;
        });

        // Viper Motion System — Part 6: Chat entrance animation
        RecyclerView chatRecycler = recyclerView;
        float dp20 = AnimationHelper.dpToPx(this, 20);
        float dp16 = AnimationHelper.dpToPx(this, 16);

        // Defer until after first layout so the animation is actually visible
        View contentRoot = findViewById(android.R.id.content);
        if (contentRoot != null) {
            contentRoot.post(() -> {
                if (chatRecycler != null) {
                    chatRecycler.setAlpha(0f);
                    chatRecycler.setTranslationY(dp20);
                    chatRecycler.animate().alpha(1f).translationY(0)
                            .setStartDelay(60).setDuration(300)
                            .setInterpolator(new FastOutSlowInInterpolator()).start();
                }
                if (inputContainer != null) {
                    inputContainer.setAlpha(0f);
                    inputContainer.setTranslationY(dp16);
                    inputContainer.animate().alpha(1f).translationY(0)
                            .setStartDelay(130).setDuration(280)
                            .setInterpolator(new FastOutSlowInInterpolator()).start();
                }
            });
        }

        // Viper — Part 4: button press micro-interactions
        AnimationHelper.applyPressAnimation(sendButton);
        AnimationHelper.applyPressAnimation(micButton);

        inputField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                handleManualSend();
                return true;
            }
            return false;
        });

        // Initial launch manual query handling
        String initialMessage = getIntent().getStringExtra(EXTRA_INITIAL_MESSAGE);
        boolean fromVoice = getIntent().getBooleanExtra("from_voice_controller", false);
        String chatId = getIntent().getStringExtra("chat_id");
        if (chatId != null) {
            sessionIdProvider.setSessionId(chatId);
            loadMessagesForSession(chatId);
        }

        if (!TextUtils.isEmpty(initialMessage)) {
            if (fromVoice) {
                // Voice engine handles the backend request. Only display visually.
                chatAdapter.addMessage(new ChatMessage(initialMessage, true));
                scrollToBottom();
            } else {
                sendManualMessage(initialMessage);
            }
        }

        // Hangout Planning Mode Initialization
        chatMode = getIntent().getStringExtra("EXTRA_CHAT_MODE");
        if ("HANGOUT_PLANNING".equals(chatMode)) {
            hangoutFriendName = getIntent().getStringExtra("EXTRA_FRIEND_NAME");
            hangoutFriendUid = getIntent().getStringExtra("EXTRA_FRIEND_UID");
            double fLat = getIntent().getDoubleExtra("EXTRA_FRIEND_LAT", 0);
            double fLng = getIntent().getDoubleExtra("EXTRA_FRIEND_LNG", 0);
            double uLat = getIntent().getDoubleExtra("EXTRA_USER_LAT", 0);
            double uLng = getIntent().getDoubleExtra("EXTRA_USER_LNG", 0);
            
            if (fLat != 0) hangoutFriendLatLng = new LatLng(fLat, fLng);
            if (uLat != 0) hangoutUserLatLng = new LatLng(uLat, uLng);
            
            initHangoutFlow();
        }

        com.viperai.notifications.NotificationRouter.handleIntent(this, getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        com.viperai.notifications.NotificationRouter.handleIntent(this, intent);
    }

    public void initiateHangoutPlanning(String prompt) {
        chatAdapter.addMessage(new ChatMessage("Planning hangout with " + (hangoutFriendName != null ? hangoutFriendName : "friend") + "...", false));
        scrollToBottom();

        if (orchestrator != null) {
            orchestrator.triggerHangoutPlanning(hangoutFriendName != null ? hangoutFriendName : "friend", prompt, new AssistantOrchestrator.OrchestratorCallback() {
                @Override
                public void onResponse(String response) {
                    runOnUiThread(() -> {
                        chatAdapter.removeLastMessage();
                        chatAdapter.addMessage(new ChatMessage(response, false));
                        scrollToBottom();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        chatAdapter.removeLastMessage();
                        chatAdapter.addMessage(new ChatMessage("Having trouble planning the hangout.", false));
                        scrollToBottom();
                    });
                }
            });
        }
    }

    /**
     * [BUG 2 FIX] Initialize hangout flow — run Geocoder on background thread,
     * show typing indicator immediately, validate all inputs.
     */
    private void initHangoutFlow() {
        // [BUG FIX] If friend location is missing but we have UID, try to recover it from Provider/Firestore
        if (hangoutFriendLatLng == null && hangoutFriendUid != null) {
            Log.d(TAG, "Attempting to recover friend location for " + hangoutFriendUid);
            NearbyFriendContextProvider.getInstance().loadFriendContext(hangoutFriendUid, provider -> {
                runOnUiThread(() -> {
                    if (provider.getFriendLat() != 0 && provider.isLocationFresh()) {
                        hangoutFriendLatLng = new com.rudra.myassistant.model.LatLng(
                                provider.getFriendLat(), provider.getFriendLng());
                        if (hangoutFriendName == null || hangoutFriendName.equals("your friend")) {
                            hangoutFriendName = provider.getFriendDisplayName();
                        }
                        // Retry init after recovery
                        initHangoutFlow();
                    } else {
                        chatAdapter.addMessage(new ChatMessage("Unable to start hangout planning — " + 
                            (hangoutFriendName != null ? hangoutFriendName : "your friend") + "'s location is out of date.", false));
                        scrollToBottom();
                    }
                });
            });
            return;
        }

        // Validate required data
        if (hangoutUserLatLng == null || hangoutFriendLatLng == null) {
            showLocationError();
            return;
        }

        if (hangoutFriendName == null || hangoutFriendName.isEmpty()) {
            hangoutFriendName = "your friend";
        }
        startHangoutProcessing();
    }

    private void showLocationError() {
        Log.e(TAG, "Hangout flow aborted: missing lat/lng data");
        chatAdapter.addMessage(new ChatMessage("Unable to start hangout planning — location data is missing.", false));
        scrollToBottom();
    }

    private void startHangoutProcessing() {
        // [BUG 2 FIX] Show typing indicator immediately (don't wait for Geocoder)
        chatAdapter.addMessage(new ChatMessage("Finding the best spots to meet " + hangoutFriendName + "...", false));
        scrollToBottom();

        // [BUG 2 FIX] Run Geocoder on background thread to avoid ANR/crash
        new Thread(() -> {
            String areaName = "your area";
            try {
                Geocoder geocoder = new Geocoder(ChatActivity.this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(
                        hangoutUserLatLng.latitude, hangoutUserLatLng.longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    String locality = addresses.get(0).getLocality();
                    if (locality == null) locality = addresses.get(0).getSubLocality();
                    if (locality == null) locality = addresses.get(0).getAdminArea();
                    if (locality != null) areaName = locality;
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoder failed: " + e.getMessage());
            }

            final String finalArea = areaName;

            // [BUG 2 FIX] Call HangoutSuggestionManager on main thread after Geocoder resolves
            runOnUiThread(() -> {
                hangoutManager.fetchInitialSuggestions(
                        sessionIdProvider.getSessionId(),
                        hangoutUserLatLng,
                        hangoutFriendLatLng,
                        hangoutFriendName,
                        finalArea,
                        new HangoutSuggestionManager.Callback() {
                            @Override
                            public void onSuggestionsReady(String message, List<com.rudra.myassistant.model.PlaceSuggestionModel> places) {
                                runOnUiThread(() -> {
                                    chatAdapter.removeLastMessage(); // Remove typing indicator
                                    
                                    // [BUG FIX] Calculate real distance from user to each place
                                    if (places != null && hangoutUserLatLng != null) {
                                        for (com.rudra.myassistant.model.PlaceSuggestionModel p : places) {
                                            float[] results = new float[1];
                                            android.location.Location.distanceBetween(
                                                hangoutUserLatLng.latitude, hangoutUserLatLng.longitude,
                                                p.getLatitude(), p.getLongitude(), results);
                                            p.setDistanceMeters(results[0]);
                                        }
                                    }
                                    
                                    chatAdapter.addMessage(new ChatMessage(message, false, places));
                                    scrollToBottom();
                                });
                            }

                            @Override
                            public void onError(String error) {
                                runOnUiThread(() -> {
                                    chatAdapter.removeLastMessage(); // Remove typing indicator
                                    chatAdapter.addMessage(new ChatMessage(
                                            "Having trouble finding spots right now. Want to try again?", false));
                                    scrollToBottom();
                                });
                            }
                        });
            });
        }).start();
    }

    private void setupSidebar() {
        navigationView = findViewById(R.id.chatNavigationView);
        if (navigationView == null) return;

        rvChatHistory = navigationView.findViewById(R.id.rvChatHistory);
        if (rvChatHistory == null) return;

        rvChatHistory.setLayoutManager(new LinearLayoutManager(this));
        chatHistoryAdapter = new ChatHistoryAdapter(chat -> {
            // Switch session or fresh start
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("chat_id", chat.getChatId());
            startActivity(intent);
            finish();
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

        updateSidebarProfile();
        loadChatHistory();
        setupSidebarShortcuts();
    }

    private void setupSidebarShortcuts() {
        if (drawerLayout == null || navigationView == null) return;
        
        // Find views once to avoid redundant traversals
        View eventsBtn = navigationView.findViewById(R.id.btnNavEvents);
        View healthBtn = navigationView.findViewById(R.id.btnNavHealth);
        View notesBtn = navigationView.findViewById(R.id.btnNavNotes);

        // Set listeners safely
        setSafeNavListener(eventsBtn, CalendarActivity.class);
        setSafeNavListener(healthBtn, HealthActivity.class);
        setSafeNavListener(notesBtn, NotesActivity.class);

        // Apply press animations for high-quality Feel
        if (eventsBtn != null) AnimationHelper.applyPressAnimation(eventsBtn);
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
                        Intent intent = new Intent(ChatActivity.this, activityClass);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_fade_in, R.anim.hold);
                    } catch (Exception e) {
                        Log.e("SidebarNav", "Failed to navigate to " + activityClass.getSimpleName(), e);
                    }
                }, 280); 
            } catch (Exception e) {
                Log.e("SidebarNav", "Failed to setup navigation", e);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSidebarProfile();
        // Subscribe to global voice events instantly
        if (com.rudra.myassistant.voice.VoiceStateManager.getInstance() != null) {
            com.rudra.myassistant.voice.VoiceStateManager.getInstance().addObserver(this);
        }
    }

    private void updateSidebarProfile() {
        if (navigationView == null) return;
        TextView sidebarName = navigationView.findViewById(R.id.sidebarUserName);
        TextView sidebarEmail = navigationView.findViewById(R.id.sidebarUserEmail);
        ImageView sidebarProfile = navigationView.findViewById(R.id.sidebarProfileImage);
        
        if (sidebarName != null) sidebarName.setText(sessionManager.getUserName());
        if (sidebarEmail != null && mAuth.getCurrentUser() != null) sidebarEmail.setText(mAuth.getCurrentUser().getEmail());

        if (sidebarProfile != null) {
            String photoUrl = sessionManager.getUserPhotoUrl();
            if (photoUrl != null) {
                Glide.with(this)
                        .load(photoUrl)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .placeholder(R.drawable.ic_person)
                        .circleCrop()
                        .into(sidebarProfile);
            }
        }
    }

    private void loadChatHistory() {
        if (chatHistoryAdapter != null && chatDbHelper != null) {
            chatHistoryAdapter.setChats(chatDbHelper.getAllChats());
        }
    }

    private void loadMessagesForSession(String sessionId) {
        Log.d(TAG, "Loading messages for session: " + sessionId);
        firestoreRepository.getChatsCol()
                .whereEqualTo("sessionId", sessionId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<QueryDocumentSnapshot> docs = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        docs.add(doc);
                    }
                    
                    // Manual sort to avoid composite index requirement
                    Collections.sort(docs, (d1, d2) -> {
                        com.google.firebase.Timestamp t1 = d1.getTimestamp("timestamp");
                        com.google.firebase.Timestamp t2 = d2.getTimestamp("timestamp");
                        if (t1 == null) return -1;
                        if (t2 == null) return 1;
                        return t1.compareTo(t2);
                    });

                    chatAdapter.clearMessages();
                    for (QueryDocumentSnapshot doc : docs) {
                        String msg = doc.getString("message");
                        String role = doc.getString("role");
                        chatAdapter.addMessage(new com.rudra.myassistant.ChatMessage(msg, "user".equals(role)));
                    }
                    scrollToBottom();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore load failed", e);
                    Toast.makeText(this, "Cloud sync failed. Showing offline mode.", Toast.LENGTH_SHORT).show();
                });
    }



    @Override
    protected void onPause() {
        super.onPause();
        // [BUG FIX] Stop TTS and wake word processing when activity is backgrounded
        com.rudra.myassistant.voice.VoicePipelineManager.getInstance(this).stop();
        VoiceStateManager.getInstance().removeObserver(this);
        stopMicAnimation();
    }

    // --- Voice State Observer Implementation ---

    @Override
    public void onStateChanged(VoiceStateManager.VoiceState state) {
        runOnUiThread(() -> {
            if (state == VoiceStateManager.VoiceState.LISTENING) {
                startMicAnimation();
            } else {
                stopMicAnimation();
            }
        });
    }

    @Override
    public void onPartialTranscriptionUpdated(String text) {
        runOnUiThread(() -> {
            if (inputField != null) {
                inputField.setText(text);
                inputField.setSelection(text.length());
            }
        });
    }

    @Override
    public void onFinalTranscription(String text) {
        runOnUiThread(() -> {
            if (inputField != null) inputField.setText(""); // clear live transcript
            if (!TextUtils.isEmpty(text)) {
                // Fulfills unified architecture requirement: 
                // Voice directly triggers same OpenAI logic as keyboard Send button.
                sendManualMessage(text);
            }
        });
    }

    @Override
    public void onAssistantResponse(String text) {
        runOnUiThread(() -> {
            if (!TextUtils.isEmpty(text)) {
                chatAdapter.addMessage(new ChatMessage(text, false));
                scrollToBottom();
            }
        });
    }

    // --- Animation Logic ---

    private void startMicAnimation() {
        if (micButton == null) return;
        if (micAnimator == null) {
            PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.3f, 1.0f);
            PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.3f, 1.0f);
            micAnimator = ObjectAnimator.ofPropertyValuesHolder(micButton, scaleX, scaleY);
            micAnimator.setDuration(1200);
            micAnimator.setRepeatCount(ObjectAnimator.INFINITE);
            micAnimator.setInterpolator(new LinearInterpolator());
        }
        micButton.setColorFilter(ContextCompat.getColor(this, R.color.purple_accent));
        micAnimator.start();
    }

    private void stopMicAnimation() {
        if (micButton == null) return;
        if (micAnimator != null) {
            micAnimator.cancel();
        }
        micButton.clearColorFilter();
        micButton.setScaleX(1.0f);
        micButton.setScaleY(1.0f);
    }

    // --- Manual Text Input Logic ---

    private void handleManualSend() {
        String text = inputField.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;
        inputField.setText("");
        sendManualMessage(text);
    }

    private void sendManualMessage(String message) {
        lastUserMessage = message;
        chatAdapter.addMessage(new com.rudra.myassistant.ChatMessage(message, true));
        scrollToBottom();

        // Follow-up Interceptor for pending actions
        if (pendingAction != null) {
            String uid = sessionManager.getUserId();
            if ("create_reminder".equals(pendingAction)) {
                // User presumably provided the time for the pending reminder
                pendingAction = null;
                handleCreateReminder(uid, pendingArgs != null ? pendingArgs : new JsonObject());
                return;
            } else if ("create_note".equals(pendingAction)) {
                // User provided the content
                if (pendingArgs != null) pendingArgs.addProperty("content", message);
                pendingAction = null;
                handleCreateNote(uid, pendingArgs);
                return;
            } else if ("create_event".equals(pendingAction)) {
                pendingAction = null;
                // Transition to Calendar Activity
                Intent intent = new Intent(ChatActivity.this, CalendarActivity.class);
                startActivity(intent);
                chatAdapter.addMessage(new ChatMessage("Opening your calendar to finalize the event details.", false));
                scrollToBottom();
                return;
            }
            pendingAction = null; // Clear if unknown
        }

        if ("HANGOUT_PLANNING".equals(chatMode)) {
            handleHangoutUserMessage(message);
            return;
        }

        // Intent Detection Layer Call
        orchestrator.handleUserMessage(message, new AssistantOrchestrator.OrchestratorCallback() {
            @Override
            public void onResponse(String response) {
                if ("[ACTION_CREATE_NOTE]".equals(response)) {
                    runOnUiThread(() -> {
                        chatAdapter.addMessage(new ChatMessage("Sure, what would you like the note to say?", false));
                        pendingAction = "create_note";
                        scrollToBottom();
                    });
                } else if ("[ACTION_CREATE_REMINDER]".equals(response)) {
                    runOnUiThread(() -> {
                        chatAdapter.addMessage(new ChatMessage("I can help with that. What should I remind you about?", false));
                        pendingAction = "create_reminder";
                        scrollToBottom();
                    });
                } else if ("[ACTION_CREATE_EVENT]".equals(response)) {
                    runOnUiThread(() -> {
                        chatAdapter.addMessage(new ChatMessage("Sure, let's schedule that. What's the title and time for the event?", false));
                        pendingAction = "create_event";
                        scrollToBottom();
                    });
                } else if (response.startsWith("[TOOL_CALL]")) {
                    runOnUiThread(() -> {
                        String json = response.substring("[TOOL_CALL]".length());
                        try {
                            JsonArray toolCalls = com.google.gson.JsonParser.parseString(json).getAsJsonArray();
                            for (int i = 0; i < toolCalls.size(); i++) {
                                JsonObject call = toolCalls.get(i).getAsJsonObject();
                                JsonObject function = call.getAsJsonObject("function");
                                String name = function.get("name").getAsString();
                                JsonObject args = com.google.gson.JsonParser.parseString(function.get("arguments").getAsString()).getAsJsonObject();
                                handleToolCall(name, args);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Tool parsing failed", e);
                        }
                    });
                } else {
                    handleAssistantResponse(response);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Assistant Failure: " + error);
                handleAssistantResponse("Sorry, I'm having trouble connecting to my brain right now.");
            }
        });

        // Save USER message to Firestore
        firestoreRepository.saveChatMessage(sessionIdProvider.getSessionId(), sessionManager.getUserId(), message, "user");
        
        // Save to local history
        updateLocalChatHistory(message);
    }

    /**
     * [BUG 5 FIX] Handle user messages in HANGOUT_PLANNING mode.
     * Intercepts the message and routes it through HangoutSuggestionManager
     * for intent extraction → Geoapify re-fetch → new place cards.
     */
    private void handleHangoutUserMessage(String message) {
        // [BUG 5 FIX] Show contextual typing indicator
        chatAdapter.addMessage(new ChatMessage("Searching for " + message + " spots...", false));
        scrollToBottom();

        hangoutManager.fetchSuggestionsForIntent(
                sessionIdProvider.getSessionId(),
                message,
                hangoutUserLatLng,
                hangoutFriendLatLng,
                new HangoutSuggestionManager.Callback() {
                    @Override
                    public void onSuggestionsReady(String response, List<com.rudra.myassistant.model.PlaceSuggestionModel> places) {
                        runOnUiThread(() -> {
                            chatAdapter.removeLastMessage(); // Remove typing indicator
                            
                            // [BUG FIX] Calculate real distance from user to each place
                            if (places != null && hangoutUserLatLng != null) {
                                for (com.rudra.myassistant.model.PlaceSuggestionModel p : places) {
                                    float[] results = new float[1];
                                    android.location.Location.distanceBetween(
                                        hangoutUserLatLng.latitude, hangoutUserLatLng.longitude,
                                        p.getLatitude(), p.getLongitude(), results);
                                    p.setDistanceMeters(results[0]);
                                }
                            }
                            
                            chatAdapter.addMessage(new ChatMessage(response, false, places));
                            scrollToBottom();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            chatAdapter.removeLastMessage(); // Remove typing indicator
                            chatAdapter.addMessage(new ChatMessage(
                                    "I couldn't find spots for that nearby. Want to try something else?", false));
                            scrollToBottom();
                        });
                    }
                });
    }

    private void handleAssistantResponse(String response) {
        runOnUiThread(() -> {
            chatAdapter.addMessage(new com.rudra.myassistant.ChatMessage(response, false));
            scrollToBottom();
            
            // Save Assistant message to Firestore
            firestoreRepository.saveChatMessage(sessionIdProvider.getSessionId(), sessionManager.getUserId(), response, "assistant");
            
            // Update local history
            updateLocalChatHistory(response);

            VoiceStateManager.getInstance().updateState(VoiceStateManager.VoiceState.RESPONDING);
            com.rudra.myassistant.voice.VoicePipelineManager.getInstance(ChatActivity.this).speak(response);
        });
    }

    private void updateLocalChatHistory(String lastMsg) {
        if (chatDbHelper == null) return;
        
        String sessionId = sessionIdProvider.getSessionId();
        List<ChatHistory> existing = chatDbHelper.getAllChats();
        
        // Generate a smart title: Use first 30 chars of the first user message
        String title = null;
        for (ChatHistory h : existing) {
            if (h.getChatId().equals(sessionId)) {
                title = h.getTitle();
                break;
            }
        }
        
        if (title == null || title.startsWith("Conversation ")) {
            // First message or placeholder — use actual content
            title = lastMsg.length() > 30 ? lastMsg.substring(0, 27) + "..." : lastMsg;
        }
        
        chatDbHelper.saveChat(new ChatHistory(sessionId, title, lastMsg, System.currentTimeMillis()));
        runOnUiThread(this::loadChatHistory);
    }

    private void handleToolCall(String functionName, JsonObject arguments) {
        Log.d(TAG, "Handling Tool Call: " + functionName + " with " + arguments.toString());
        String uid = sessionManager.getUserId();
        Map<String, Object> updates = new HashMap<>();

        switch (functionName) {
            case "create_reminder":
                handleCreateReminder(uid, arguments);
                break;

            case "delete_reminder":
                handleDeleteReminder(uid, arguments);
                break;

            case "update_reminder":
                handleUpdateReminder(uid, arguments);
                break;

            case "show_reminders":
                handleAssistantResponse("I'll open your reminders list for you.");
                startActivity(new Intent(this, RemindersActivity.class));
                break;

            case "create_note":
                handleCreateNote(uid, arguments);
                break;

            case "delete_note":
                handleDeleteNote(uid, arguments);
                break;

            case "update_note":
                handleUpdateNote(uid, arguments);
                break;

            case "show_notes":
                handleAssistantResponse("Here are your notes.");
                startActivity(new Intent(this, NotesActivity.class));
                break;

            case "create_calendar_event":
                handleCreateCalendarEvent(uid, arguments);
                break;

            case "show_calendar_events":
                handleAssistantResponse("Opening your calendar...");
                startActivity(new Intent(this, CalendarActivity.class));
                break;

            case "delete_calendar_event":
                handleDeleteCalendarEvent(uid, arguments);
                break;

            case "add_medication":
                handleAddMedication(uid, arguments);
                break;

            case "show_medications":
                handleAssistantResponse("Checking your medications list.");
                startActivity(new Intent(this, MedicationDetailActivity.class));
                break;

            case "mark_medication_taken":
                handleMarkMedicationTaken(uid, arguments);
                break;

            case "log_period_data":
                handleLogPeriodData(uid, arguments);
                break;

            case "update_health_stats":
                handleUpdateHealthStats(uid, arguments);
                break;

            case "show_health":
                handleAssistantResponse("Opening your health dashboard.");
                startActivity(new Intent(this, HealthActivity.class));
                break;

            case "navigate_to":
                handleNavigation(arguments);
                break;

            case "open_connect":
                handleAssistantResponse("Opening Connect and Map.");
                startActivity(new Intent(this, ConnectActivity.class));
                break;

            case "show_expenses":
                handleAssistantResponse("Checking your expenses.");
                Intent expenseIntent = new Intent(this, ConnectActivity.class);
                expenseIntent.putExtra("ACTION", "SHOW_EXPENSES");
                startActivity(expenseIntent);
                break;

            case "open_settings":
                handleAssistantResponse("Opening settings.");
                startActivity(new Intent(this, SettingsActivity.class));
                break;

            case "remember_personal_detail":
                handleRememberPersonalDetail(uid, arguments);
                break;

            default:
                handleAssistantResponse("I'm sorry, I'm still learning how to handle " + functionName.replace("_", " ") + ".");
                break;
        }
    }

    private void handleCreateReminder(String uid, JsonObject args) {
        String text = args.has("text") ? args.get("text").getAsString() : null;
        String time = args.has("time") ? args.get("time").getAsString() : null;

        if (text == null) {
            handleAssistantResponse("What should I remind you about?");
            pendingAction = "create_reminder";
            return;
        }

        ReminderInterpreter.InterpretResult result = ReminderInterpreter.interpret(time != null ? time : lastUserMessage, null);
        if (time == null && result.date == null) {
            handleAssistantResponse("When should I remind you to " + result.cleanText + "?");
            pendingAction = "create_reminder";
            pendingArgs = args;
            return;
        }

        firestoreRepository.addReminder(uid, result.cleanText, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).format(result.date), false, null, null, null, 100.0f, true, false)
            .addOnSuccessListener(aVoid -> handleAssistantResponse("Reminder set for " + result.cleanText + " at " + new SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(result.date)));
    }

    private void handleDeleteReminder(String uid, JsonObject args) {
        String text = args.has("text") ? args.get("text").getAsString() : null;
        if (text != null) {
            firestoreRepository.getRemindersQuery(uid).get().addOnSuccessListener(queryDocumentSnapshots -> {
                for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                    if (doc.getString("text").toLowerCase().contains(text.toLowerCase())) {
                        firestoreRepository.deleteReminder(doc.getId()).addOnSuccessListener(v -> handleAssistantResponse("Reminder '" + text + "' deleted."));
                        return;
                    }
                }
                handleAssistantResponse("I couldn't find a reminder with that text.");
            });
        } else {
            handleAssistantResponse("Which reminder should I delete?");
        }
    }

    private void handleUpdateReminder(String uid, JsonObject args) {
        int index = args.has("index") ? args.get("index").getAsInt() : -1;
        String text = args.has("text") ? args.get("text").getAsString() : null;
        String time = args.has("time") ? args.get("time").getAsString() : null;

        firestoreRepository.getRemindersQuery(uid).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (index > 0 && index <= queryDocumentSnapshots.size()) {
                String docId = queryDocumentSnapshots.getDocuments().get(index - 1).getId();
                Map<String, Object> updates = new HashMap<>();
                if (text != null) updates.put("text", text);
                if (time != null) {
                    ReminderInterpreter.InterpretResult res = ReminderInterpreter.interpret(time, null);
                    updates.put("datetime", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(res.date));
                }
                firestoreRepository.updateReminder(docId, updates).addOnSuccessListener(v -> handleAssistantResponse("Reminder updated."));
            } else {
                handleAssistantResponse("I couldn't find reminder #" + index);
            }
        });
    }

    private void handleCreateNote(String uid, JsonObject args) {
        String title = args.has("title") ? args.get("title").getAsString() : null;
        String content = args.has("content") ? args.get("content").getAsString() : null;

        if (title == null) {
            handleAssistantResponse("What's the title for your note?");
            pendingAction = "create_note";
            return;
        }
        if (content == null) {
            handleAssistantResponse("What content should I save for '" + title + "'?");
            pendingAction = "create_note";
            pendingArgs = args;
            return;
        }

        firestoreRepository.addNote(uid, title, content)
            .addOnSuccessListener(aVoid -> handleAssistantResponse("Note '" + title + "' saved successfully."));
    }

    private void handleDeleteNote(String uid, JsonObject args) {
        String title = args.has("title") ? args.get("title").getAsString() : null;
        firestoreRepository.getNotesQuery(uid).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                if (doc.getString("title").toLowerCase().contains(title.toLowerCase())) {
                    firestoreRepository.deleteNote(doc.getId()).addOnSuccessListener(v -> handleAssistantResponse("Note '" + title + "' deleted."));
                    return;
                }
            }
            handleAssistantResponse("Note not found.");
        });
    }

    private void handleUpdateNote(String uid, JsonObject args) {
        int index = args.has("index") ? args.get("index").getAsInt() : -1;
        String title = args.has("title") ? args.get("title").getAsString() : null;
        String content = args.has("content") ? args.get("content").getAsString() : null;

        firestoreRepository.getNotesQuery(uid).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (index > 0 && index <= queryDocumentSnapshots.size()) {
                String docId = queryDocumentSnapshots.getDocuments().get(index - 1).getId();
                Map<String, Object> updates = new HashMap<>();
                if (title != null) updates.put("title", title);
                if (content != null) updates.put("content", content);
                firestoreRepository.updateNote(docId, updates).addOnSuccessListener(v -> handleAssistantResponse("Note updated."));
            }
        });
    }

    private void handleCreateCalendarEvent(String uid, JsonObject args) {
        String title = args.has("title") ? args.get("title").getAsString() : null;
        String dateTimeStr = args.has("dateTime") ? args.get("dateTime").getAsString() : null;

        if (title == null) {
            handleAssistantResponse("What should I call the event?");
            return;
        }

        Date tempDate;
        if (dateTimeStr != null) {
            try {
                // OpenAI often provides YYYY-MM-DD HH:MM
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                tempDate = isoFormat.parse(dateTimeStr);
            } catch (Exception e) {
                // Fallback to interpreter
                ReminderInterpreter.InterpretResult result = ReminderInterpreter.interpret(dateTimeStr, null);
                tempDate = result.date;
            }
        } else {
            ReminderInterpreter.InterpretResult result = ReminderInterpreter.interpret(lastUserMessage, null);
            tempDate = result.date;
        }

        final Date eventDate = tempDate;

        String startStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(eventDate);
        Calendar cal = Calendar.getInstance();
        cal.setTime(eventDate);
        cal.add(Calendar.HOUR_OF_DAY, 1);
        String endStr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(cal.getTime());

        firestoreRepository.addEvent(uid, title, startStr, endStr, "")
            .addOnSuccessListener(v -> {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.US);
                handleAssistantResponse("Calendar event '" + title + "' scheduled for " + sdf.format(eventDate));
            });
    }

    private void handleDeleteCalendarEvent(String uid, JsonObject args) {
        String title = args.has("title") ? args.get("title").getAsString() : null;
        if (title == null) {
            handleAssistantResponse("Which event should I delete?");
            return;
        }
        firestoreRepository.getEventsQuery(uid).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                if (doc.getString("title").toLowerCase().contains(title.toLowerCase())) {
                    firestoreRepository.deleteEvent(doc.getId()).addOnSuccessListener(v -> handleAssistantResponse("Event '" + title + "' deleted."));
                    return;
                }
            }
            handleAssistantResponse("Event not found.");
        });
    }

    private void handleAddMedication(String uid, JsonObject args) {
        String name = args.has("name") ? args.get("name").getAsString() : null;
        String dosage = args.has("dosage") ? args.get("dosage").getAsString() : "As prescribed";
        String time = args.has("time") ? args.get("time").getAsString() : "08:00";
        List<String> days = new ArrayList<>();
        if (args.has("days")) {
            for (com.google.gson.JsonElement e : args.getAsJsonArray("days")) days.add(e.getAsString());
        } else {
            days.add("Everyday");
        }

        if (name == null) {
            handleAssistantResponse("Which medication should I add?");
            pendingAction = "add_medication";
            return;
        }

        firestoreRepository.addMedication(uid, name, dosage, time, days, null)
            .addOnSuccessListener(aVoid -> handleAssistantResponse("I've added " + name + " to your medications tracker."));
    }

    private void handleMarkMedicationTaken(String uid, JsonObject args) {
        String name = args.get("name").getAsString();
        boolean status = args.get("status").getAsBoolean();
        firestoreRepository.findMedicationByName(uid, name).addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("taken", status);
                firestoreRepository.updateMedication(queryDocumentSnapshots.getDocuments().get(0).getId(), updates)
                    .addOnSuccessListener(v -> handleAssistantResponse(status ? "Great! I've marked " + name + " as taken." : "I've unmarked " + name + "."));
            } else {
                handleAssistantResponse("I couldn't find " + name + " in your list.");
            }
        });
    }

    private void handleLogPeriodData(String uid, JsonObject args) {
        Map<String, Object> data = new HashMap<>();
        if (args.has("start_date")) data.put("start_date", args.get("start_date").getAsString());
        if (args.has("end_date")) data.put("end_date", args.get("end_date").getAsString());
        if (args.has("flow")) data.put("flow", args.get("flow").getAsString());
        if (args.has("spotting")) data.put("spotting", args.get("spotting").getAsBoolean());
        
        firestoreRepository.logPeriodData(uid, data)
            .addOnSuccessListener(v -> handleAssistantResponse("Health data logged successfully. Stay healthy!"));
    }

    private void handleUpdateHealthStats(String uid, JsonObject args) {
        Integer water = args.has("water") ? args.get("water").getAsInt() : null;
        Integer steps = args.has("steps") ? args.get("steps").getAsInt() : null;
        Double sleep = args.has("sleep_hours") ? args.get("sleep_hours").getAsDouble() : null;

        firestoreRepository.updateHealthStats(uid, water, steps, sleep)
            .addOnSuccessListener(v -> handleAssistantResponse("Daily health stats updated. Check your progress in the Health module."));
    }

    private void handleRememberPersonalDetail(String uid, JsonObject args) {
        String content = args.get("content").getAsString();
        String type = args.has("memory_type") ? args.get("memory_type").getAsString() : "preference";

        firestoreRepository.addMemory(uid, content, type)
            .addOnSuccessListener(v -> handleAssistantResponse("I've remembered that for you: " + content));
    }

    private void handleNavigation(JsonObject args) {
        String dest = args.has("destination") ? args.get("destination").getAsString() : null;
        if (dest == null) {
            handleAssistantResponse("Where would you like to go?");
            return;
        }
        handleAssistantResponse("Getting directions to " + dest + ".");
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(dest));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Fallback to browser
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(dest))));
        }
    }

    private void scrollToBottom() {
        RecyclerView recyclerView = findViewById(R.id.chat_recycler_view);
        if (recyclerView != null && chatAdapter != null) {
            recyclerView.post(() -> recyclerView.scrollToPosition(chatAdapter.getItemCount() - 1));
        }
    }

    private void performLogout() {
        // 1. Sign out from Firebase
        FirebaseAuth.getInstance().signOut();

        // 2. Clear local session
        sessionManager.logoutUser();

        // 3. Return to login screen
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
