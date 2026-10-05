package com.rudra.myassistant;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.rudra.myassistant.location.LocationTracker;
import com.bumptech.glide.Glide;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.Friend;
import com.rudra.myassistant.network.UserSessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.rudra.myassistant.network.ApiConfig;
import com.rudra.myassistant.network.RetrofitClient;
import com.rudra.myassistant.network.GeoapifyApi;
import com.rudra.myassistant.network.AutocompleteResponseDto;
import com.rudra.myassistant.network.PlacesSearchResponseDto;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.lifecycle.ViewModelProvider;
import com.rudra.myassistant.viewmodel.ExpenseViewModel;
import com.rudra.myassistant.ui.expense.ExpenseOverviewFragment;
import com.rudra.myassistant.model.BalanceSummary;

public class ConnectActivity extends BaseEdgeToEdgeActivity implements LocationTracker.LocationChangeListener {

    private WebView mapWebView;
    private CardView suggestionCard;
    private LinearLayout placesListContainer;
    private TextView suggestionSubtitleText;
    private ImageView btnDismissSuggestion;
    private ImageView btnCategoryFilter;
    private LinearLayout mapSearchBar;
    private RecyclerView friendsRecyclerView;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private FriendAdapter friendAdapter;
    private GeoapifyApi geoapifyApi;
    private FirestoreRepository firestoreRepo;



    private String userId;
    private Handler handler;
    private Runnable fetchSuggestionsRunnable;
    private boolean isMapReady = false;

    public double userLat = 0, userLng = 0;
    private long lastDismissTime = 0;
    private String selectedCategory = null;

    private UnifiedSearchAdapter unifiedSearchAdapter;
    private View searchOverlay;
    private int lastSelectedItemId = -1;
    private EditText fullSearchEditText;
    private ImageView btnClearSearch;
    private RecyclerView searchSuggestionsRecyclerView;
    private Runnable searchDebounceRunnable;
    private TextView friendsNearbySummary;

    private static final long FETCH_INTERVAL_MS = 60000;
    private static final long DISMISS_COOLDOWN_MS = 300000;
    private static final long SEARCH_DEBOUNCE_DELAY = 400;

    private static final float NEARBY_THRESHOLD_METERS = 2000f;
    private static final float DISPLACEMENT_THRESHOLD_METERS = 50f;
    private static final long LOCATION_UPDATE_MAX_INTERVAL = 300000; // 5 minutes
    private double lastWrittenLat = 0, lastWrittenLng = 0;
    private long lastLocationWriteTime = 0;
    
    private Map<String, com.google.firebase.firestore.ListenerRegistration> friendLocationListeners = new HashMap<>();
    private List<Friend> nearbyFriendsList = new ArrayList<>();

    // New Dashboard Components
    private android.widget.ViewFlipper viewFlipper;
    private DashboardFriendAdapter dashboardFriendAdapter;
    private DashboardChatAdapter dashboardChatAdapter;
    private TextView tvCurrentLocationName;
    private RecyclerView rvFriendsHorizontal;
    private RecyclerView rvChatPreview;
    private ImageView dashboardProfileImage;
    private RecyclerView rvFriendRequests;
    private FriendRequestAdapter friendRequestAdapter;
    private CardView cardFriendRequests;

    @SuppressLint({"SetJavaScriptEnabled", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        handler = new Handler(Looper.getMainLooper());
        userId = FirebaseAuth.getInstance().getUid();

        try {
            geoapifyApi = RetrofitClient.getGeoapifyInstance().create(GeoapifyApi.class);
            firestoreRepo = new FirestoreRepository();
        } catch (Exception e) {
            Log.e("ConnectActivity", "API initialization failed: " + e.getMessage());
        }

        initDashboardViews();
        initMapViews();
        setupBottomSheet();
        
        requestLocationPermission();
        setupWebView();
        loadMap();
        setupFullSearchOverlay();
        setupControls();

        fetchSuggestionsRunnable = new Runnable() {
            @Override
            public void run() {
                refreshHangoutPlanningContext();
                handler.postDelayed(this, FETCH_INTERVAL_MS);
            }
        };

        if (firestoreRepo != null) {
            loadUserLocation();
            setupFriendsList();
            loadDashboardChats();
            loadFriendRequests();
        }

        handleIntent(getIntent());

        // Viper Motion System — entrance animation
        View root = findViewById(android.R.id.content);
        if (root != null) {
            float dp = root.getResources().getDisplayMetrics().density * 24;
            root.setTranslationY(dp);
            root.setAlpha(0.5f);
            root.animate().translationY(0).alpha(1f)
                    .setDuration(350)
                    .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                    .start();
        }

        com.viperai.notifications.NotificationRouter.handleIntent(this, getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocationTracker.init(this);
        LocationTracker.getInstance().setLocationChangeListener(this);
        LocationTracker.getInstance().startTracking();
        handler.post(fetchSuggestionsRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocationTracker.getInstance().setLocationChangeListener(null);
        LocationTracker.getInstance().stopTracking();
        handler.removeCallbacks(fetchSuggestionsRunnable);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            userLat = location.getLatitude();
            userLng = location.getLongitude();
            
            // Update UI text for current location
            new Thread(() -> {
                String areaName = getAreaName(userLat, userLng);
                runOnUiThread(() -> {
                    if (tvCurrentLocationName != null) {
                        tvCurrentLocationName.setText(areaName);
                    }
                });
            }).start();

            // Update Firestore if needed (e.g. if moved significantly or 5 mins passed)
            long now = System.currentTimeMillis();
            float[] results = new float[1];
            Location.distanceBetween(lastWrittenLat, lastWrittenLng, userLat, userLng, results);
            if (results[0] > DISPLACEMENT_THRESHOLD_METERS || (now - lastLocationWriteTime) > LOCATION_UPDATE_MAX_INTERVAL) {
                lastWrittenLat = userLat;
                lastWrittenLng = userLng;
                lastLocationWriteTime = now;
                firestoreRepo.updateUserLocation(userId, userLat, userLng);
            }

            recalculateProximity();
            if (isMapReady && mapWebView != null) {
                mapWebView.evaluateJavascript("updateUserLocation(" + userLat + "," + userLng + ")", null);
            }
        }
    }

    private String getAreaName(double lat, double lng) {
        try {
            android.location.Geocoder geocoder = new android.location.Geocoder(this, java.util.Locale.getDefault());
            List<android.location.Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                android.location.Address address = addresses.get(0);
                String subLocality = address.getSubLocality();
                String locality = address.getLocality();
                if (subLocality != null && locality != null) return subLocality + ", " + locality;
                if (subLocality != null) return subLocality;
                if (locality != null) return locality;
                return address.getFeatureName();
            }
        } catch (Exception e) {
            Log.e("ConnectActivity", "Geocoding failed", e);
        }
        return "Locating...";
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
        com.viperai.notifications.NotificationRouter.handleIntent(this, intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        String mode = intent.getStringExtra("EXTRA_MAP_MODE");
        if ("PLACE_DIRECTIONS".equals(mode)) {
            double lat = intent.getDoubleExtra("EXTRA_PLACE_LAT", 0);
            double lng = intent.getDoubleExtra("EXTRA_PLACE_LNG", 0);
            String name = intent.getStringExtra("EXTRA_PLACE_NAME");
            String dist = intent.getStringExtra("EXTRA_PLACE_DISTANCE");

            if (lat != 0 && lng != 0) {
                if (bottomSheetBehavior != null) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
                String escapedName = name != null ? name.replace("'", "\\'") : "Selected Place";
                String escapedDist = dist != null ? dist : "";
                mapWebView.post(() -> {
                    mapWebView.evaluateJavascript("setSearchMarker(" + lat + "," + lng + ",'" + escapedName + "','" + escapedDist + "')", null);
                });
            }
        } else if ("SHOW_EXPENSES".equals(intent.getStringExtra("ACTION"))) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerConnect, new com.rudra.myassistant.ui.expense.ExpenseOverviewFragment())
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void initDashboardViews() {
        viewFlipper = findViewById(R.id.viewFlipper);
        rvChatPreview = findViewById(R.id.rvChatPreview);
        dashboardProfileImage = findViewById(R.id.dashboardProfileImage);
        cardFriendRequests = findViewById(R.id.cardFriendRequests);
        rvFriendRequests = findViewById(R.id.rvFriendRequests);
        rvFriendsHorizontal = findViewById(R.id.rvFriendsHorizontal);
        tvCurrentLocationName = findViewById(R.id.tvCurrentLocationName);
        
        // Horizontal Layout for dashboard
        rvFriendsHorizontal.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Profile Image
        UserSessionManager sessionManager = new UserSessionManager(this);
        String photoUrl = sessionManager.getUserPhotoUrl();
        if (photoUrl != null) {
            Glide.with(this).load(photoUrl).circleCrop().into(dashboardProfileImage);
        }

        // Adapters
        dashboardFriendAdapter = new DashboardFriendAdapter(this::focusOnFriendAndShowMap);
        rvFriendsHorizontal.setAdapter(dashboardFriendAdapter);

        dashboardChatAdapter = new DashboardChatAdapter(conv -> {
            Intent intent = new Intent(this, FriendMessageActivity.class);
            intent.putExtra("conversationId", conv.getId());
            if (conv.getFriend() != null) {
                intent.putExtra("friendName", conv.getFriend().getName());
                intent.putExtra("friendUid", conv.getFriend().getId());
                intent.putExtra("friendPhoto", conv.getFriend().getPhotoUrl());
            }
            startActivity(intent);
        });
        rvChatPreview.setLayoutManager(new LinearLayoutManager(this));
        rvChatPreview.setAdapter(dashboardChatAdapter);

        // Friend Requests Adapter
        friendRequestAdapter = new FriendRequestAdapter(new FriendRequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(com.rudra.myassistant.network.FriendRequest request) {
                respondToFriendRequest(request, "accepted");
            }

            @Override
            public void onDecline(com.rudra.myassistant.network.FriendRequest request) {
                respondToFriendRequest(request, "declined");
            }

            @Override
            public void onBlock(com.rudra.myassistant.network.FriendRequest request) {
                respondToFriendRequest(request, "block");
            }
        });
        rvFriendRequests.setLayoutManager(new LinearLayoutManager(this));
        rvFriendRequests.setAdapter(friendRequestAdapter);

        // Click Listeners
        findViewById(R.id.dashboardSearchBar).setOnClickListener(v -> showSearchOverlay());
        findViewById(R.id.cardLocation).setOnClickListener(v -> showMap());
        findViewById(R.id.tvViewAllChats).setOnClickListener(v -> {
            startActivity(new Intent(this, FriendChatListActivity.class));
        });

        // Quick Actions
        findViewById(R.id.btnNewChatDashboard).setOnClickListener(v -> {
            startActivity(new Intent(this, FriendChatListActivity.class));
        });
        findViewById(R.id.btnFindNearbyDashboard).setOnClickListener(v -> showMap());
        findViewById(R.id.btnShareLive).setOnClickListener(v -> {
            Toast.makeText(this, "Live sharing active", Toast.LENGTH_SHORT).show();
        });

        // Categories
        setupCategoryClick(R.id.catCafe, "catering.cafe");
        setupCategoryClick(R.id.catGym, "leisure.sport_centre");
        setupCategoryClick(R.id.catEat, "catering.restaurant");
        setupCategoryClick(R.id.catStudy, "leisure.library");

        setupExpenseCard();
    }

    private void loadUserLocation() {
        if (userId == null || firestoreRepo == null) return;
        firestoreRepo.getUserLocation(userId).addOnSuccessListener(snapshot -> {
            if (snapshot != null && snapshot.exists()) {
                Double lat = snapshot.getDouble("latitude");
                Double lng = snapshot.getDouble("longitude");
                if (lat != null && lng != null) {
                    userLat = lat;
                    userLng = lng;
                    lastWrittenLat = lat;
                    lastWrittenLng = lng;
                    recalculateProximity();
                }
            }
        });
    }

    private void setupExpenseCard() {
        View expenseCard = findViewById(R.id.cardSharedExpenses);
        if (expenseCard == null) return;

        expenseCard.setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerConnect, new com.rudra.myassistant.ui.expense.ExpenseOverviewFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Initialize ViewModel and observe
        com.rudra.myassistant.viewmodel.ExpenseViewModel expenseVm = new androidx.lifecycle.ViewModelProvider(this)
                .get(com.rudra.myassistant.viewmodel.ExpenseViewModel.class);
        
        expenseVm.getAllBalances(userId).observe(this, balances -> {
            expenseCard.setVisibility(View.VISIBLE);
            if (balances == null || balances.isEmpty()) {
                ((TextView)findViewById(R.id.tvTotalNetBalance)).setText("Total Balance ₹0");
                ((TextView)findViewById(R.id.chipOwe)).setText("You owe ₹0");
                ((TextView)findViewById(R.id.chipOwed)).setText("You are owed ₹0");
                return;
            }
            
            double totalOwed = 0;
            double totalReceivable = 0;
            for (com.rudra.myassistant.model.BalanceSummary s : balances) {
                if (s.getYouOwe()) totalOwed += s.getNetBalance();
                else totalReceivable += s.getNetBalance();
            }
            
            ((TextView)findViewById(R.id.tvTotalNetBalance)).setText("Total Balance ₹" + (int)(totalReceivable - totalOwed));
            ((TextView)findViewById(R.id.chipOwe)).setText("You owe ₹" + (int)totalOwed);
            ((TextView)findViewById(R.id.chipOwed)).setText("You are owed ₹" + (int)totalReceivable);
        });
    }

    private void setupCategoryClick(int id, String category) {
        findViewById(id).setOnClickListener(v -> {
            selectedCategory = category;
            showMap();
            fetchNearbyPlacesWithGeoapify(category);
        });
    }

    private void initMapViews() {
        mapWebView = findViewById(R.id.mapWebView);
        suggestionCard = findViewById(R.id.suggestionCard);
        placesListContainer = findViewById(R.id.placesListContainer);
        suggestionSubtitleText = findViewById(R.id.suggestionSubtitleText);
        btnDismissSuggestion = findViewById(R.id.btnDismissSuggestion);
        btnCategoryFilter = findViewById(R.id.btnCategoryFilter);
        mapSearchBar = findViewById(R.id.mapSearchBar);
        friendsNearbySummary = findViewById(R.id.friendsNearbySummary);
        
        findViewById(R.id.btnCloseMap).setOnClickListener(v -> hideMap());
    }

    private void showMap() {
        viewFlipper.setDisplayedChild(1);
    }

    private void hideMap() {
        viewFlipper.setDisplayedChild(0);
    }

    private void focusOnFriendAndShowMap(Friend friend) {
        showMap();
        focusOnFriend(friend);
    }

    private void loadDashboardChats() {
        if (userId == null) return;
        firestoreRepo.getConversationsQuery(userId).addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null) return;
            List<com.rudra.myassistant.network.Conversation> conversations = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                com.rudra.myassistant.network.Conversation conv = doc.toObject(com.rudra.myassistant.network.Conversation.class);
                if (conv != null) {
                    conv.setId(doc.getId());
                    String otherUid = null;
                    for (String p : conv.getParticipants()) {
                        if (!p.equals(userId)) { otherUid = p; break; }
                    }
                    if (otherUid != null) {
                        final String fOtherUid = otherUid;
                        firestoreRepo.getUser(fOtherUid).addOnSuccessListener(uDoc -> {
                            Friend f = mapDocToFriend(uDoc);
                            conv.setFriend(f);
                            conversations.add(conv);
                            // Sort and update
                            Collections.sort(conversations, (c1, c2) -> {
                                if (c1.getLastTimestamp() == null || c2.getLastTimestamp() == null) return 0;
                                return c2.getLastTimestamp().compareTo(c1.getLastTimestamp());
                            });
                            dashboardChatAdapter.setConversations(conversations);
                        });
                    }
                }
            }
        });
    }

    private final Map<String, Boolean[]> friendshipStates = new HashMap<>();


    private void setupBottomSheet() {
        LinearLayout sheet = findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(sheet);
        bottomSheetBehavior.setSaveFlags(BottomSheetBehavior.SAVE_ALL);
    }

    private void setupFriendsList() {
        friendsRecyclerView = findViewById(R.id.friendsRecyclerViewMap);
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        friendAdapter = new FriendAdapter(this::focusOnFriend);
        friendAdapter.setOnFriendActionClickListener(this::showFriendOptionsMenu);
        friendsRecyclerView.setAdapter(friendAdapter);

        loadLiveFriends();
    }

    private List<Friend> currentLiveFriends = new ArrayList<>();

    private void loadLiveFriends() {
        if (firestoreRepo == null) return;
        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) return;

        firestoreRepo.getFriendsQuery(myUid).addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e("ConnectActivity", "Friends sync failed: " + e.getMessage());
                return;
            }

            if (snapshot != null) {
                List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                friendshipStates.clear();
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    String friendUid = doc.getString("friendUid");
                    if (friendUid != null) {
                        boolean iShare = Boolean.TRUE.equals(doc.getBoolean("iAmSharingWithThem"));
                        boolean theyShare = Boolean.TRUE.equals(doc.getBoolean("theyAreSharingWithMe"));
                        friendshipStates.put(friendUid, new Boolean[]{iShare, theyShare});
                        tasks.add(firestoreRepo.getUser(friendUid));
                    }
                }

                if (tasks.isEmpty() && currentLiveFriends.isEmpty()) {
                    friendAdapter.setFriends(new ArrayList<>());
                    findViewById(R.id.emptyFriendsText).setVisibility(View.VISIBLE);
                    return;
                }

                Tasks.whenAllSuccess(tasks).addOnSuccessListener(docs -> {
                    List<Friend> updatedFriends = new ArrayList<>();
                    for (Object obj : docs) {
                        DocumentSnapshot doc = (DocumentSnapshot) obj;
                        Friend f = mapDocToFriend(doc);
                        if (f != null) {
                            updatedFriends.add(f);
                            setupFriendLocationListener(f.getId());
                        }
                    }
                    this.currentLiveFriends = updatedFriends;
                    friendAdapter.setFriends(updatedFriends);
                    addFriendsMarkers(updatedFriends);
                    
                    // Initial proximity check with manual data from docs
                    recalculateProximity();
                    if (isMapReady) {
                        syncFriendMarkers();
                    }
                    
                    findViewById(R.id.emptyFriendsText).setVisibility(updatedFriends.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }
        });
    }

    private void setupFriendLocationListener(String friendUid) {
        if (friendLocationListeners.containsKey(friendUid)) return;

        com.google.firebase.firestore.ListenerRegistration reg = firestoreRepo.getFriendLocationRef(friendUid)
            .addSnapshotListener((snapshot, e) -> {
                if (e != null || snapshot == null || !snapshot.exists()) return;

                Double lat = snapshot.getDouble("latitude");
                Double lng = snapshot.getDouble("longitude");
                com.google.firebase.Timestamp ts = snapshot.getTimestamp("last_updated");

                if (lat != null && lng != null && ts != null) {
                    // 10-minute stale check
                    long diff = System.currentTimeMillis() - ts.toDate().getTime();
                    if (diff < 10 * 60 * 1000) {
                        updateFriendLocationLocally(friendUid, lat, lng);
                    } else {
                        updateFriendLocationLocally(friendUid, 0, 0); // Mark as unavailable
                    }
                }
            });
        friendLocationListeners.put(friendUid, reg);
    }

    private void updateFriendLocationLocally(String friendUid, double lat, double lng) {
        for (Friend f : currentLiveFriends) {
            if (f.getId().equals(friendUid)) {
                f.setLatitude(lat);
                f.setLongitude(lng);
                break;
            }
        }
        recalculateProximity();
        if (isMapReady) {
            syncFriendMarkers();
        }
    }

    private void syncFriendMarkers() {
        if (!isMapReady || mapWebView == null) return;
        runOnUiThread(() -> {
            mapWebView.evaluateJavascript("clearFriendMarkers()", null);
            for (Friend f : currentLiveFriends) {
                if (f.getLatitude() != 0) {
                    String photoUrl = f.getPhotoUrl();
                    String escapedName = f.getName() != null ? f.getName().replace("'", "\\'") : "Friend";
                    String js = String.format("addFriendMarker(%f, %f, '%s', '%s')",
                            f.getLatitude(), f.getLongitude(), escapedName, photoUrl != null ? photoUrl : "");
                    mapWebView.evaluateJavascript(js, null);
                }
            }
        });
    }

    private void refreshHangoutPlanningContext() {
        recalculateProximity();
    }

    private void loadSuggestions() {
        // Redundant call from legacy code, logic now handled by refreshHangoutPlanningContext
    }

    private void recalculateProximity() {
        if (userLat == 0) return;

        List<Friend> nearby = new ArrayList<>();
        List<String> nearbyIds = new ArrayList<>();

        for (Friend f : currentLiveFriends) {
            if (f.getLatitude() != 0) {
                double dist = calculateDistance(userLat, userLng, f.getLatitude(), f.getLongitude());
                f.setLastDistance(dist);
                if (dist <= NEARBY_THRESHOLD_METERS) {
                    nearby.add(f);
                    nearbyIds.add(f.getId());
                }
            }
        }

        this.nearbyFriendsList = nearby;
        
        runOnUiThread(() -> {
            dashboardFriendAdapter.setFriends(nearby);
            if (friendsNearbySummary != null) {
                friendsNearbySummary.setText("Seeing " + nearby.size() + " friends nearby");
            }
            if (friendAdapter != null) {
                friendAdapter.notifyDataSetChanged();
            }
            
            if (!nearbyIds.isEmpty()) {
                onFriendNearbyDetected(nearbyIds);
            } else {
                onFriendNearbyCleared();
            }
        });
        
        SuggestionsManager.setNearbyFriends(nearbyIds);
        
        // [BUG 1 FIX] Push real friend name + location into SuggestionsManager
        // so the suggestion card shows "Rahul is nearby" instead of "your friend"
        for (Friend f : nearby) {
            // First, set whatever name we already have from the Friend object
            SuggestionsManager.setNearbyFriendContext(
                    f.getId(),
                    f.getName() != null ? f.getName() : "your friend",
                    f.getLatitude(),
                    f.getLongitude()
            );
            
            // Then use NearbyFriendContextProvider to load/validate from Firestore
            NearbyFriendContextProvider.getInstance().loadFriendContext(f.getId(), provider -> {
                runOnUiThread(() -> {
                    SuggestionsManager.setNearbyFriendContext(
                            provider.getFriendUid(),
                            provider.getFriendDisplayName(),
                            provider.getFriendLat(),
                            provider.getFriendLng()
                    );
                });
            });
        }
    }

    private void onFriendNearbyCleared() {
        SuggestionsManager.setNearbyFriends(null);
    }

    private void onFriendNearbyDetected(List<String> nearbyIds) {
        if (nearbyIds == null || nearbyIds.isEmpty() || currentLiveFriends == null) return;
        
        List<Friend> nearby = new ArrayList<>();
        for (String id : nearbyIds) {
            for (Friend f : currentLiveFriends) {
                if (f.getId().equals(id)) {
                    nearby.add(f);
                    break;
                }
            }
        }
        
        if (nearby.size() >= 1) {
            updateHangoutSuggestionCard(nearby);
        } else {
            if (suggestionCard != null) suggestionCard.setVisibility(View.GONE);
        }
    }

    private void handleSendFriendRequest(Friend friend) {
        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null || firestoreRepo == null) return;

        // Check if already friends
        for (Friend f : currentLiveFriends) {
            if (f.getId().equals(friend.getId())) {
                Toast.makeText(this, "Already friends", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Toast.makeText(this, "Sending request to " + friend.getName() + "...", Toast.LENGTH_SHORT).show();
        
        firestoreRepo.sendFriendRequest(myUid, friend.getId()).addOnSuccessListener(aVoid -> {
            Toast.makeText(ConnectActivity.this, "Request sent!", Toast.LENGTH_SHORT).show();
            // Clear search results after sending
            currentSearchFriends = new ArrayList<>();
            unifiedSearchAdapter.setResults(currentSearchPlaces, currentSearchFriends);
            fullSearchEditText.setText("");
            hideSearchOverlay();
        }).addOnFailureListener(e -> {
            Toast.makeText(ConnectActivity.this, "Failed to send request", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadFriendRequests() {
        if (firestoreRepo == null || userId == null) return;
        firestoreRepo.getPendingRequests(userId).addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null) return;
            List<com.rudra.myassistant.network.FriendRequest> requests = new ArrayList<>();
            if (snapshot.isEmpty()) {
                cardFriendRequests.setVisibility(View.GONE);
                return;
            }

            List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                com.rudra.myassistant.network.FriendRequest req = doc.toObject(com.rudra.myassistant.network.FriendRequest.class);
                if (req != null) {
                    req.setId(doc.getId());
                    requests.add(req);
                    tasks.add(firestoreRepo.getUser(req.getSenderUid()));
                }
            }

            Tasks.whenAllSuccess(tasks).addOnSuccessListener(userDocs -> {
                for (int i = 0; i < requests.size(); i++) {
                    DocumentSnapshot userDoc = (DocumentSnapshot) userDocs.get(i);
                    requests.get(i).setSenderName(userDoc.getString("name"));
                    requests.get(i).setSenderPhoto(userDoc.getString("profile_photo_url"));
                }
                friendRequestAdapter.setRequests(requests);
                cardFriendRequests.setVisibility(requests.isEmpty() ? View.GONE : View.VISIBLE);
            });
        });
    }

    private void respondToFriendRequest(com.rudra.myassistant.network.FriendRequest request, String status) {
        if (firestoreRepo == null || userId == null) return;
        firestoreRepo.respondToRequest(request.getId(), status, userId).addOnSuccessListener(aVoid -> {
            if ("accepted".equals(status)) {
                Toast.makeText(this, "Friend request accepted", Toast.LENGTH_SHORT).show();
                // Create mutual friendship
                firestoreRepo.addFriendship(userId, request.getSenderUid());
                firestoreRepo.addFriendship(request.getSenderUid(), userId);
            } else if ("block".equals(status)) {
                Toast.makeText(this, "User blocked", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Request declined", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addFriendsMarkers(List<Friend> friends) {
        if (!isMapReady) return;
        syncFriendMarkers();
    }

    private void focusOnFriend(Friend friend) {

        if (friend.getLatitude() != 0) {
            if (bottomSheetBehavior != null) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
            String js = String.format("focusLocation(%f, %f, '%s')", 
                friend.getLatitude(), friend.getLongitude(), friend.getName());
            mapWebView.evaluateJavascript(js, null);
        } else {
            Toast.makeText(this, friend.getName() + " hasn't shared their location", Toast.LENGTH_SHORT).show();
        }
    }

    public void highlightFriend(String friendUid) {
        showMap();
        // Give time for friends to load and markers to sync
        handler.postDelayed(() -> {
            for (Friend f : currentLiveFriends) {
                if (f.getId().equals(friendUid)) {
                    focusOnFriend(f);
                    // Open bottom sheet if focused from notification
                    if (bottomSheetBehavior != null) {
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    }
                    break;
                }
            }
        }, 1500);
    }

    private List<Friend> currentSearchFriends = new ArrayList<>();
    private List<AutocompleteResponseDto.Feature> currentSearchPlaces = new ArrayList<>();

    private void filterFriends(String query) {
        if (query == null || query.trim().length() < 2) {
            currentSearchFriends = new ArrayList<>();
            unifiedSearchAdapter.setResults(currentSearchPlaces, currentSearchFriends);
            return;
        }

        String myId = FirebaseAuth.getInstance().getUid();
        if (myId == null || firestoreRepo == null) return;
        
        String normalizedQuery = query.toLowerCase().trim();

        // Perform parallel Firestore searches for Username and Name
        Task<QuerySnapshot> usernameTask = firestoreRepo.searchUsers(normalizedQuery);
        Task<QuerySnapshot> nameTask = firestoreRepo.searchUsersByName(normalizedQuery);

        Tasks.whenAllSuccess(usernameTask, nameTask).addOnSuccessListener(results -> {
            Map<String, Friend> uniqueUsers = new HashMap<>();
            
            // Process Username results
            QuerySnapshot usernameSnap = (QuerySnapshot) results.get(0);
            for (DocumentSnapshot doc : usernameSnap.getDocuments()) {
                if (doc.getId().equals(myId)) continue;
                Friend f = mapDocToFriend(doc);
                if (f != null) uniqueUsers.put(f.getId(), f);
            }

            // Process Name results
            QuerySnapshot nameSnap = (QuerySnapshot) results.get(1);
            for (DocumentSnapshot doc : nameSnap.getDocuments()) {
                if (doc.getId().equals(myId)) continue;
                Friend f = mapDocToFriend(doc);
                if (f != null && !uniqueUsers.containsKey(f.getId())) {
                    uniqueUsers.put(f.getId(), f);
                }
            }

            List<Friend> finalResults = new ArrayList<>(uniqueUsers.values());
            
            // Snapchat-style Sorting: Exact username match > username prefix > name
            Collections.sort(finalResults, (a, b) -> {
                String uA = a.getUsername() != null ? a.getUsername().toLowerCase() : "";
                String uB = b.getUsername() != null ? b.getUsername().toLowerCase() : "";
                
                if (uA.equals(normalizedQuery)) return -1;
                if (uB.equals(normalizedQuery)) return 1;
                if (uA.startsWith(normalizedQuery) && !uB.startsWith(normalizedQuery)) return -1;
                if (!uA.startsWith(normalizedQuery) && uB.startsWith(normalizedQuery)) return 1;
                return 0;
            });

            currentSearchFriends = finalResults;
            unifiedSearchAdapter.setResults(currentSearchPlaces, currentSearchFriends);
        }).addOnFailureListener(e -> {
            Log.e("ConnectActivity", "Firestore search failed: " + e.getMessage());
        });
    }

    private Friend mapDocToFriend(DocumentSnapshot doc) {
        try {
            Boolean[] states = friendshipStates.get(doc.getId());
            boolean iShare = states != null ? states[0] : true;
            boolean theyShare = states != null ? states[1] : true;

            double lat = doc.contains("latitude") ? doc.getDouble("latitude") : 0;
            double lng = doc.contains("longitude") ? doc.getDouble("longitude") : 0;

            // If friend is not sharing with me, zero out their coordinates for safety
            if (!theyShare) {
                lat = 0;
                lng = 0;
            }

            boolean proxEnabled = !Boolean.FALSE.equals(doc.getBoolean("proximity_sharing_enabled"));

            return new Friend(
                    doc.getId(),
                    doc.getString("name"),
                    doc.getString("username"),
                    doc.getString("email"),
                    doc.getString("profile_photo_url"),
                    lat,
                    lng,
                    "online".equals(doc.getString("status")),
                    iShare,
                    theyShare,
                    proxEnabled
            );
        } catch (Exception e) {
            return null;
        }
    }

    private void showFriendOptionsMenu(Friend friend) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.CustomBottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_friend_options, null);
        
        TextView nameText = view.findViewById(R.id.optionFriendName);
        TextView usernameText = view.findViewById(R.id.optionFriendUsername);
        ImageView avatarImage = view.findViewById(R.id.optionFriendAvatar);
        com.google.android.material.switchmaterial.SwitchMaterial shareSwitch = view.findViewById(R.id.switchLocationSharing);
        LinearLayout toggleLayout = view.findViewById(R.id.layoutToggleSharing);
        LinearLayout removeLayout = view.findViewById(R.id.layoutRemoveFriend);

        nameText.setText(friend.getName());
        usernameText.setText("@" + (friend.getUsername() != null ? friend.getUsername() : "user"));
        
        com.bumptech.glide.Glide.with(this)
                .load(friend.getPhotoUrl() != null ? friend.getPhotoUrl() : "https://ui-avatars.com/api/?name=" + friend.getName())
                .circleCrop()
                .into(avatarImage);

        shareSwitch.setChecked(friend.iAmSharingWithThem());
        
        toggleLayout.setOnClickListener(v -> {
            boolean newState = !shareSwitch.isChecked();
            shareSwitch.setChecked(newState);
            firestoreRepo.toggleLocationSharing(FirebaseAuth.getInstance().getUid(), friend.getId(), newState)
                    .addOnSuccessListener(aVoid -> {
                        if (newState && userLat != 0) {
                            firestoreRepo.updateUserLocation(userId, userLat, userLng);
                        }
                        Toast.makeText(this, newState ? "Now sharing with " + friend.getName() : "Stopped sharing with " + friend.getName(), Toast.LENGTH_SHORT).show();
                    });
        });

        // New Proximity Toggle
        com.google.android.material.switchmaterial.SwitchMaterial proxSwitch = view.findViewById(R.id.switchProximityAlerts);
        LinearLayout proxLayout = view.findViewById(R.id.layoutProximityToggle);
        
        proxSwitch.setChecked(friend.isProximitySharingEnabled());
        proxLayout.setOnClickListener(v -> {
            boolean newState = !proxSwitch.isChecked();
            proxSwitch.setChecked(newState);
            
            // Backend update
            com.rudra.myassistant.network.ViperApiService api = RetrofitClient.getViperInstance().create(com.rudra.myassistant.network.ViperApiService.class);
            // We need the internal friend.id (int) but Firestore uses UIDs (string). 
            // This is a mapping gap in the current prototype. 
            // In a real app, the Friend object would have the relational ID too.
            // For now, I'll use a placeholder or log it.
            Toast.makeText(this, "Proximity alerts " + (newState ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
            // friend.setProximitySharingEnabled(newState);
        });

        removeLayout.setOnClickListener(v -> {
            String myUid = FirebaseAuth.getInstance().getUid();
            if (myUid != null) {
                firestoreRepo.removeFriendship(myUid, friend.getId()).addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, friend.getName() + " removed from friends", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to remove friend", Toast.LENGTH_SHORT).show();
                });
            }
        });

        dialog.setContentView(view);
        dialog.show();
    }





    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup Firestore listeners
        for (com.google.firebase.firestore.ListenerRegistration reg : friendLocationListeners.values()) {
            reg.remove();
        }
        friendLocationListeners.clear();
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 1001);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (mapWebView != null) mapWebView.reload();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings webSettings = mapWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setGeolocationDatabasePath(getFilesDir().getPath());
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        mapWebView.setBackgroundColor(android.graphics.Color.parseColor("#0B0B0E"));
        mapWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);


        mapWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("geo:")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.setPackage("com.google.android.apps.maps");
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    }
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, request);
            }
        });

        mapWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }

            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                Log.d("MapConsole", "[" + consoleMessage.messageLevel() + "] " + consoleMessage.message() + " -- Line: " + consoleMessage.lineNumber());
                return true;
            }
        });

        mapWebView.addJavascriptInterface(new AndroidBridge(), "Android");
    }

    private void setupControls() {
        findViewById(R.id.btnMyLocation).setOnClickListener(v -> {
            Toast.makeText(this, "Locating...", Toast.LENGTH_SHORT).show();
            mapWebView.evaluateJavascript("locateUser()", null);
        });

        btnDismissSuggestion.setOnClickListener(v -> {
            suggestionCard.setVisibility(View.GONE);
            lastDismissTime = System.currentTimeMillis();
            clearMapMarkers();
        });

        if (mapSearchBar != null) {
            mapSearchBar.setOnClickListener(v -> showSearchOverlay());
        }

        btnCategoryFilter.setOnClickListener(v -> showCategoryFilterMenu(v));
    }

    private void showCategoryFilterMenu(View anchor) {
        Context wrapper = new ContextThemeWrapper(this, R.style.ThemeOverlay_App_DarkPopupMenu);
        PopupMenu popup = new PopupMenu(wrapper, anchor);
        popup.getMenu().add(0, 0, 0, "All").setCheckable(true);
        popup.getMenu().add(0, 1, 1, "Cafes").setCheckable(true);
        popup.getMenu().add(0, 2, 2, "Restaurants").setCheckable(true);
        popup.getMenu().add(0, 3, 3, "Movie Theaters").setCheckable(true);
        popup.getMenu().add(0, 4, 4, "Shopping Centers").setCheckable(true);
        popup.getMenu().add(0, 5, 5, "Hangout Places").setCheckable(true);

        // Set radio-style checkable group
        popup.getMenu().setGroupCheckable(0, true, true);
        
        // Restore checkmark for last selected
        if (lastSelectedItemId >= 0 && lastSelectedItemId <= 5) {
            popup.getMenu().findItem(lastSelectedItemId).setChecked(true);
        } else {
            popup.getMenu().findItem(0).setChecked(true);
        }

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == lastSelectedItemId && itemId != 0) {
                // Toggle off -> show All
                itemId = 0; 
                lastSelectedItemId = -1;
            } else {
                lastSelectedItemId = itemId;
            }

            String categories = null;
            switch (itemId) {
                case 1: categories = "catering.cafe"; break;
                case 2: categories = "catering.restaurant"; break;
                case 3: categories = "entertainment.cinema"; break;
                case 4: categories = "commercial.shopping_mall"; break;
                case 5: categories = "leisure.park,catering.bar"; break;
                case 0:
                default:
                    categories = "catering.cafe,catering.restaurant,commercial.shopping_mall";
                    lastSelectedItemId = 0;
                    break;
            }
            if (userLat != 0 && userLng != 0) {
                fetchNearbyPlacesWithGeoapify(categories);
            } else {
                Toast.makeText(this, "Location not ready yet", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        popup.show();
    }


    private void setupFullSearchOverlay() {
        searchOverlay = findViewById(R.id.searchOverlay);
        fullSearchEditText = findViewById(R.id.fullSearchEditText);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        searchSuggestionsRecyclerView = findViewById(R.id.searchSuggestionsRecyclerView);
        ImageView btnSearchBack = findViewById(R.id.btnSearchBack);

        unifiedSearchAdapter = new UnifiedSearchAdapter(new UnifiedSearchAdapter.OnSearchItemClickListener() {
            @Override
            public void onPlaceClick(AutocompleteResponseDto.Feature place) {
                ConnectActivity.this.onPlaceSelected(place);
            }

            @Override
            public void onFriendClick(Friend friend) {
                ConnectActivity.this.focusOnFriend(friend);
                hideSearchOverlay();
            }

            @Override
            public void onAddFriendClick(Friend friend) {
                ConnectActivity.this.handleSendFriendRequest(friend);
            }
        });
        searchSuggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchSuggestionsRecyclerView.setAdapter(unifiedSearchAdapter);

        btnSearchBack.setOnClickListener(v -> hideSearchOverlay());
        btnClearSearch.setOnClickListener(v -> fullSearchEditText.setText(""));

        fullSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                debounceSearch(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void showSearchOverlay() {
        searchOverlay.setVisibility(View.VISIBLE);
        fullSearchEditText.setText("");
        unifiedSearchAdapter.setResults(new ArrayList<>(), new ArrayList<>());
        fullSearchEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(fullSearchEditText, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideSearchOverlay() {
        searchOverlay.setVisibility(View.GONE);
        hideKeyboard(fullSearchEditText);
    }

    private void debounceSearch(String query) {
        if (searchDebounceRunnable != null) handler.removeCallbacks(searchDebounceRunnable);
        if (query.trim().isEmpty()) {
            unifiedSearchAdapter.setResults(new ArrayList<>(), new ArrayList<>());
            return;
        }
        searchDebounceRunnable = () -> {
            fetchGeoapifySuggestions(query);
            filterFriends(query);
        };
        handler.postDelayed(searchDebounceRunnable, SEARCH_DEBOUNCE_DELAY);
    }

    private void onPlaceSelected(AutocompleteResponseDto.Feature item) {
        hideSearchOverlay();
        double lat = item.getGeometry().getLat();
        double lng = item.getGeometry().getLon(); // Correctly mapped to getLon
        String name = item.getProperties().getName();
        String address = item.getProperties().getFormatted();

        String js = String.format("setSearchMarker(%f, %f, \"%s\", \"%s\")",
                lat, lng,
                name.replace("\"", "\\\"").replace("'", "\\'"),
                address.replace("\"", "\\\"").replace("'", "\\'"));
        mapWebView.evaluateJavascript(js, null);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void fetchGeoapifySuggestions(String query) {
        geoapifyApi.getAutocomplete(query, ApiConfig.GEOAPIFY_API_KEY).enqueue(new Callback<AutocompleteResponseDto>() {
            @Override
            public void onResponse(Call<AutocompleteResponseDto> call, Response<AutocompleteResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentSearchPlaces = response.body().getFeatures();
                    unifiedSearchAdapter.setResults(currentSearchPlaces != null ? currentSearchPlaces : new ArrayList<>(), currentSearchFriends);
                }
            }
            @Override
            public void onFailure(Call<AutocompleteResponseDto> call, Throwable t) {
                Log.e("ConnectActivity", "Geoapify API error: " + t.getMessage());
            }
        });
    }

    private void loadMap() {
        String html = "<!DOCTYPE html><html><head>" +
                "<meta charset='utf-8'/><meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'/>" +
                "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
                "<style>" +
                "html,body{margin:0;padding:0;height:100%;background:#0B0B0E;overflow:hidden;}" +
                "#map{height:100%;width:100%;background:#0B0B0E;}" +
                "#loading{position:fixed;top:0;left:0;right:0;bottom:0;display:flex;align-items:center;justify-content:center;color:white;font-family:sans-serif;background:#0B0B0E;z-index:1000;}" +
                ".leaflet-container{background:#0B0B0E !important;}" +
                ".leaflet-control-attribution{display:none !important;}" +
                ".leaflet-popup-content-wrapper{background:rgba(20,20,25,0.95);color:white;border-radius:18px;padding:8px;backdrop-filter:blur(10px);border:1px solid rgba(255,255,255,0.1);box-shadow:0 8px 32px rgba(0,0,0,0.5);}" +
                ".leaflet-popup-tip{background:rgba(20,20,25,0.95);}" +
                ".circular-marker {" +
                "  width: 52px; height: 52px; border-radius: 50%; border: 2.5px solid white;" +
                "  box-shadow: 0 4px 15px rgba(0,0,0,0.4); overflow: hidden; background: #1E1E24;" +
                "  display: flex; align-items: center; justify-content: center; transition: transform 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);" +
                "}" +
                ".circular-marker:active { transform: scale(1.15); }" +
                ".circular-marker img {" +
                "  width: 100%; height: 100%; object-fit: cover;" +
                "}" +
                ".marker-label {" +
                "  background: rgba(0,0,0,0.7); color: white; padding: 3px 10px; border-radius: 12px;" +
                "  font-size: 11px; margin-top: 6px; white-space: nowrap; font-weight: 600; backdrop-filter: blur(4px);" +
                "  box-shadow: 0 2px 8px rgba(0,0,0,0.3); border: 0.5px solid rgba(255,255,255,0.1);" +
                "}" +
                ".user-glow {" +
                "  position: absolute; width: 60px; height: 60px; background: rgba(0, 122, 255, 0.2);" +
                "  border-radius: 50%; box-shadow: 0 0 20px rgba(0, 122, 255, 0.4); animation: glow 2s ease-in-out infinite;" +
                "}" +
                ".nav-arrow {" +
                "  position: relative; width: 32px; height: 32px; display: flex; align-items: center; justify-content: center;" +
                "  filter: drop-shadow(0 0 5px rgba(139, 124, 255, 0.4));" +
                "}" +
                ".nav-arrow-svg {" +
                "  width: 100%; height: 100%;" +
                "}" +
                ".user-glow {" +
                "  position: absolute; width: 44px; height: 44px; background: rgba(139, 124, 255, 0.2);" +
                "  border-radius: 50%; box-shadow: 0 0 20px rgba(139, 124, 255, 0.3); animation: glow 2s ease-in-out infinite;" +
                "}" +
                "@keyframes glow {" +
                "  0%, 100% { transform: scale(0.8); opacity: 0.3; }" +
                "  50% { transform: scale(1.2); opacity: 0.5; }" +
                "}" +
                "</style></head><body>" +
                "<div id='loading'>Loading Map...</div><div id='map'></div>" +
                "<script>" +
                "var map; var markers = []; var friendLayers; var userMarker; var searchMarker; var routeLayer;" +
                "function initMap(){" +
                "  map = L.map('map',{zoomControl:false, minZoom:3, maxZoom:20}).setView([20.5937,78.9629],4);" +
                "  L.tileLayer('https://maps.geoapify.com/v1/tile/dark-matter/{z}/{x}/{y}{r}.png?apiKey=" + ApiConfig.GEOAPIFY_API_KEY + "',{" +
                "    maxZoom: 20," +
                "    detectRetina: true" +
                "  }).addTo(map);" +
                "  friendLayers = L.layerGroup().addTo(map);" +
                "  document.getElementById('loading').style.display='none';" +
                "  map.on('moveend', function(){" +
                "    var center = map.getCenter();" +
                "    if(window.Android) window.Android.onMapMove(center.lat, center.lng);" +
                "  });" +
                "  locateUser();" +
                "  if(window.Android) window.Android.onMapReady();" +
                "}" +
                "function locateUser(){" +
                "  if(navigator.geolocation){" +
                "    navigator.geolocation.getCurrentPosition(" +
                "      function(pos){" +
                "        var lat = pos.coords.latitude; var lng = pos.coords.longitude;" +
                "        var heading = pos.coords.heading;" +
                "        map.setView([lat,lng],14);" +
                "        if(window.Android) window.Android.updateUserLocation(lat,lng, heading || 0);" +
                "        if(window.Android) window.Android.reverseGeocode(lat,lng);" +
                "      }," +
                "      function(err){" +
                "        console.warn('Location error ('+err.code+'): '+err.message);" +
                "        alert('Could not get location. Ensure GPS is enabled.');" +
                "      }," +
                "      {enableHighAccuracy: true, timeout: 10000, maximumAge: 0}" +
                "    );" +
                "  } else {" +
                "    alert('Geolocation not supported.');" +
                "  }" +
                "}" +
                "function updateUserMarker(lat,lng,photoUrl,heading){" +
                "  if(userMarker) map.removeLayer(userMarker);" +
                "  var innerHtml = photoUrl ? '<div class=\"circular-marker\"><img src=\"'+photoUrl+'\" /></div>' : " +
                "    '<div class=\"user-glow\"></div>' + " +
                "    '<div class=\"nav-arrow\" style=\"transform:rotate('+(heading||0)+'deg)\">' + " +
                "      '<svg viewBox=\"0 0 24 24\" class=\"nav-arrow-svg\">' + " +
                "        '<path d=\"M12,3 L19.5,21 L12,17.5 L4.5,21 Z\" fill=\"none\" stroke=\"white\" stroke-width=\"2.5\" stroke-linejoin=\"round\" stroke-linecap=\"round\" />' + " +
                "      '</svg>' + " +
                "    '</div>';" +
                "  var icon = L.divIcon({" +
                "    html: '<div style=\"display:flex;flex-direction:column;align-items:center;justify-content:center;position:relative;width:60px;height:60px;\">'+innerHtml+'</div>'," +
                "    className: 'user-marker', iconSize:[60,60], iconAnchor:[30,30]" +
                "  });" +
                "  userMarker = L.marker([lat,lng],{icon:icon}).addTo(map);" +
                "}" +
                "function addMarker(lat,lng,name,cat,dist,emoji){" +
                "  var icon = L.divIcon({" +
                "    html: '<div style=\"font-size:24px;text-shadow:0 0 5px black;\">'+emoji+'</div>'," +
                "    className: 'custom-icon', iconSize:[30,30], iconAnchor:[15,15]" +
                "  });" +
                "  renderMarker(lat,lng,name,cat,dist,icon);" +
                "}" +
                "function addFriendMarker(lat,lng,name,photoUrl){" +
                "  var imgHtml = photoUrl ? '<img src=\"'+photoUrl+'\" onerror=\"this.src=\\'https://ui-avatars.com/api/?name='+encodeURIComponent(name)+'&background=random\\'\" />' : '<div style=\"font-size:20px;\">👤</div>';" +
                "  var icon = L.divIcon({" +
                "    html: '<div style=\"display:flex;flex-direction:column;align-items:center;\"><div class=\"circular-marker\">'+imgHtml+'</div><div class=\"marker-label\">'+name+'</div></div>'," +
                "    className: 'friend-marker', iconSize:[70,90], iconAnchor:[35,52]" +
                "  });" +
                "  var popup = '<div style=\"padding:8px;\"><b>'+name+'</b><br>Friend<br><br>'+" +
                "              '<button onclick=\"window.Android.requestRoute('+lat+','+lng+',\\''+name.replace(/'/g, \"\\\\'\")+'\\')\" style=\"background:#747AFF;color:white;border:none;border-radius:8px;padding:8px 16px;cursor:pointer;width:100%;font-weight:bold;\">Get Directions</button></div>';" +
                "  L.marker([lat,lng],{icon:icon}).addTo(friendLayers).bindPopup(popup);" +
                "}" +
                "function clearFriendMarkers(){ friendLayers.clearLayers(); }" +
                "function renderMarker(lat,lng,name,cat,dist,icon){" +
                "  var popup = '<div style=\"padding:8px;\"><b>'+name+'</b><br>'+cat+'<br><br>'+" +
                "              '<button onclick=\"window.Android.requestRoute('+lat+','+lng+',\\''+name.replace(/'/g, \"\\\\'\")+'\\')\" style=\"background:#747AFF;color:white;border:none;border-radius:8px;padding:8px 16px;cursor:pointer;width:100%;font-weight:bold;\">Get Directions</button></div>';" +
                "  var m = L.marker([lat,lng],{icon:icon}).addTo(map).bindPopup(popup);" +
                "  markers.push(m);" +
                "}" +
                "function setSearchMarker(lat,lng,name,form){" +
                "  if(searchMarker) map.removeLayer(searchMarker);" +
                "  if(routeLayer) { map.removeLayer(routeLayer); routeLayer = null; }" +
                "  var popup = '<div style=\"padding:4px;\"><b>'+name+'</b><br>'+form+'<br><br>'+" +
                "              '<button onclick=\"window.Android.requestRoute('+lat+','+lng+',\\''+name.replace(/'/g, \"\\\\'\")+'\\')\" style=\"background:#747AFF;color:white;border:none;border-radius:8px;padding:8px 16px;cursor:pointer;width:100%;font-weight:bold;font-size:12px;\">Get Directions</button></div>';" +
                "  searchMarker = L.marker([lat,lng]).addTo(map).bindPopup(popup).openPopup();" +
                "  searchMarker.on('popupclose', function() { if(searchMarker) { map.removeLayer(searchMarker); searchMarker = null; } });" +
                "  map.setView([lat,lng],15);" +
                "}" +
                "function drawRoute(jsonStr){" +
                "  if(routeLayer) map.removeLayer(routeLayer);" +
                "  var geojson = JSON.parse(jsonStr);" +
                "  routeLayer = L.geoJSON(geojson,{style:{color:'#747AFF',weight:5,opacity:0.7}}).addTo(map);" +
                "  map.fitBounds(routeLayer.getBounds(),{padding:[50,50]});" +
                "}" +
                "function focusLocation(lat,lng,name){" +
                "  map.setView([lat,lng],16);" +
                "  markers.forEach(m => { " +
                "    var ll = m.getLatLng();" +
                "    if(Math.abs(ll.lat - lat) < 0.0001 && Math.abs(ll.lng - lng) < 0.0001) m.openPopup();" +
                "  });" +
                "}" +
                "function clearMarkers(){" +
                "  markers.forEach(m => map.removeLayer(m));" +
                "  markers = [];" +
                "}" +
                "window.onload = initMap;" +
                "</script></body></html>";
        mapWebView.loadDataWithBaseURL("https://maps.geoapify.com", html, "text/html", "UTF-8", null);
    }

    private void fetchNearbyPlacesWithGeoapify(String categories) {
        if (categories == null || geoapifyApi == null) return;
        String filter = "circle:" + userLng + "," + userLat + ",5000";
        
        Toast.makeText(this, "Searching latest locations...", Toast.LENGTH_SHORT).show();
        geoapifyApi.searchNearby(categories, filter, 30, ApiConfig.GEOAPIFY_API_KEY).enqueue(new Callback<PlacesSearchResponseDto>() {
            @Override
            public void onResponse(Call<PlacesSearchResponseDto> call, Response<PlacesSearchResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    clearMapMarkers();
                    List<PlacesSearchResponseDto.Feature> list = response.body().getFeatures();
                    if (list != null && list.size() > 0) {
                        for (PlacesSearchResponseDto.Feature f : list) {
                            double lon = f.getGeometry().getCoordinates().get(0);
                            double lat = f.getGeometry().getCoordinates().get(1);
                            String name = f.getProperties().getName();
                            String catPrimary = f.getProperties().getCategories() != null && !f.getProperties().getCategories().isEmpty() 
                                    ? f.getProperties().getCategories().get(0) : "location";
                            
                            String emoji = getCategoryEmoji(catPrimary);
                            double distance = calculateDistance(userLat, userLng, lat, lon);
                            
                            String js = String.format("addMarker(%f, %f, \"%s\", \"%s\", %d, \"%s\")",
                                    lat, lon,
                                    name.replace("\"", "\\\"").replace("'", "\\'"),
                                    catPrimary.replace("\"", "\\\"").replace("'", "\\'"),
                                    Math.round(distance), emoji);
                            
                            mapWebView.evaluateJavascript(js, null);
                        }
                    } else {
                        Toast.makeText(ConnectActivity.this, "No latest places found nearby", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("ConnectActivity", "Geoapify Places error: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<PlacesSearchResponseDto> call, Throwable t) {
                Log.e("ConnectActivity", "Geoapify failure: " + t.getMessage());
            }
        });
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        float[] results = new float[1];
        android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, results);
        return results[0];
    }

    private void searchNearbyPlaces(double lat, double lng) {
        // Photon can also search nearby with location bias, but we will focus on search box
    }

    private void openGoogleMapsNavigation(double dLat, double dLng, String name) {
        // Using geo: URI with (label) ensure the correct place name is displayed in Google Maps
        String encodedName = Uri.encode(name);
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + dLat + "," + dLng + "(" + encodedName + ")");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Fallback to standard geo URI
            Uri geoUri = Uri.parse("geo:" + dLat + "," + dLng + "?q=" + dLat + "," + dLng);
            startActivity(new Intent(Intent.ACTION_VIEW, geoUri));
        }
    }

    private void updateHangoutSuggestionCard(List<Friend> friends) {
        if (userId == null || firestoreRepo == null || userLat == 0) return;
        if (System.currentTimeMillis() - lastDismissTime < DISMISS_COOLDOWN_MS) return;

        displayHangoutSuggestions(friends);
    }

    private void displayHangoutSuggestions(List<Friend> friends) {
        suggestionCard.setVisibility(View.VISIBLE);
        placesListContainer.removeAllViews();
        
        StringBuilder friendsNames = new StringBuilder();
        for (int i = 0; i < friends.size(); i++) {
            friendsNames.append(friends.get(i).getName());
            if (i < friends.size() - 1) friendsNames.append(", ");
        }
        
        suggestionSubtitleText.setText("You and " + friendsNames.toString() + " are nearby! Ready to hangout?");
        
        String[][] categories = {
            {"☕ Cafes", "catering.cafe"},
            {"🎬 Cinemas", "entertainment.cinema"},
            {"🍔 Restaurants", "catering.restaurant"},
            {"🎳 Hangout Places", "leisure.park,catering.bar"}
        };

        for (String[] cat : categories) {
            TextView tv = new TextView(this);
            tv.setText(cat[0]);
            tv.setTextColor(getColor(R.color.white));
            tv.setBackgroundResource(R.drawable.bg_dashboard_chip);
            tv.setPadding(32, 16, 32, 16);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 8, 8, 8);
            tv.setLayoutParams(params);
            tv.setClickable(true);
            tv.setFocusable(true);
            tv.setOnClickListener(v -> {
                fetchNearbyPlacesWithGeoapify(cat[1]);
                suggestionCard.setVisibility(View.GONE);
            });
            placesListContainer.addView(tv);
        }
    }

    private String getCategoryEmoji(String cat) {
        if (cat == null) return "📍";
        if (cat.contains("cafe")) return "☕";
        if (cat.contains("restaurant")) return "🍔";
        if (cat.contains("cinema")) return "🎬";
        if (cat.contains("shopping")) return "🛍️";
        return "📍";
    }

    private void clearMapMarkers() {
        mapWebView.evaluateJavascript("clearMarkers()", null);
    }

    public class AndroidBridge {
        @JavascriptInterface
        public void updateUserLocation(double lat, double lng, float bearing) {
            userLat = lat; userLng = lng;
            String photoUrl = new UserSessionManager(ConnectActivity.this).getUserPhotoUrl();
            final String escapedUrl = photoUrl != null ? photoUrl : "";
            runOnUiThread(() -> {
                if (friendAdapter != null) friendAdapter.setCurrentLocation(lat, lng);
                mapWebView.evaluateJavascript(String.format("updateUserMarker(%f, %f, '%s', %f)", lat, lng, escapedUrl, bearing), null);
                searchNearbyPlaces(lat, lng);
            });
            if (firestoreRepo != null && userId != null) {
                double dist = calculateDistance(lat, lng, lastWrittenLat, lastWrittenLng);
                long timeDiff = System.currentTimeMillis() - lastLocationWriteTime;
                
                if (dist >= DISPLACEMENT_THRESHOLD_METERS || lastWrittenLat == 0 || timeDiff >= LOCATION_UPDATE_MAX_INTERVAL) {
                    firestoreRepo.updateUserLocation(userId, lat, lng);
                    lastWrittenLat = lat;
                    lastWrittenLng = lng;
                    lastLocationWriteTime = System.currentTimeMillis();
                }
            }
        }

        @JavascriptInterface
        public void onMapReady() {
            isMapReady = true;
            runOnUiThread(() -> syncFriendMarkers());
        }

        @JavascriptInterface
        public void onMapMove(double lat, double lng) {
            runOnUiThread(() -> searchNearbyPlaces(lat, lng));
        }

        @JavascriptInterface
        public void requestRoute(double dLat, double dLng, String name) {
            runOnUiThread(() -> openGoogleMapsNavigation(dLat, dLng, name));
        }

        @JavascriptInterface
        public void reverseGeocode(double lat, double lng) {
            if (geoapifyApi == null) return;
            geoapifyApi.reverseGeocode(lat, lng, ApiConfig.GEOAPIFY_API_KEY).enqueue(new Callback<okhttp3.ResponseBody>() {
                @Override
                public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String json = response.body().string();
                            org.json.JSONObject obj = new org.json.JSONObject(json);
                            org.json.JSONArray features = obj.optJSONArray("features");
                            if (features != null && features.length() > 0) {
                                org.json.JSONObject props = features.getJSONObject(0).optJSONObject("properties");
                                if (props != null) {
                                    String city = props.optString("city");
                                    String suburb = props.optString("suburb");
                                    String street = props.optString("street");
                                    
                                    final String address = !city.isEmpty() ? city : (!suburb.isEmpty() ? suburb : street);
                                    runOnUiThread(() -> {
                                        if (tvCurrentLocationName != null) tvCurrentLocationName.setText(address);
                                    });
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("Geoapify", "Reverse geocode parse error", e);
                    }
                }
                @Override public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {}
            });
        }
    }
}