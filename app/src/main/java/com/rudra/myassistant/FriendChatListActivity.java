package com.rudra.myassistant;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rudra.myassistant.network.Conversation;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.Friend;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.bumptech.glide.Glide;
import com.rudra.myassistant.network.UserSessionManager;
import android.widget.ImageView;

public class FriendChatListActivity extends BaseEdgeToEdgeActivity {

    private RecyclerView recyclerView;
    private ConversationAdapter adapter;
    private FirestoreRepository firestoreRepo;
    private ProgressBar progressBar;
    private TextView emptyText;
    private String myUid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_chat_list);

        myUid = FirebaseAuth.getInstance().getUid();
        firestoreRepo = new FirestoreRepository();

        recyclerView = findViewById(R.id.conversationsRecyclerView);
        progressBar = findViewById(R.id.chatListProgress);
        emptyText = findViewById(R.id.emptyChatText);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConversationAdapter(myUid, this::openChat);
        recyclerView.setAdapter(adapter);

        ImageView profileBtn = findViewById(R.id.btnSnapProfile);
        profileBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        // Load Real Profile Photo
        UserSessionManager sessionManager = new UserSessionManager(this);
        String photoUrl = sessionManager.getUserPhotoUrl();
        if (photoUrl != null) {
            Glide.with(this)
                .load(photoUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(profileBtn);
        }

        findViewById(R.id.fabNewChat).setOnClickListener(v -> showFriendsListForNewChat());

        loadConversations();
    }

    private void loadConversations() {
        if (myUid == null) return;
        progressBar.setVisibility(View.VISIBLE);

        // Optimized query: Fetch all conversations containing 'myUid'
        // Sorting in-memory to avoid composite index requirements for initial pilot
        firestoreRepo.getConversationsQuery(myUid).addSnapshotListener((snapshot, e) -> {
            progressBar.setVisibility(View.GONE);
            if (e != null) {
                Log.e("ChatList", "Listen failed", e);
                return;
            }

            if (snapshot == null || snapshot.isEmpty()) {
                emptyText.setVisibility(View.VISIBLE);
                adapter.setConversations(new ArrayList<>());
                return;
            }

            emptyText.setVisibility(View.GONE);
            List<Conversation> conversations = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snapshot) {
                Conversation conv = doc.toObject(Conversation.class);
                // The ID from POJO mapping might be missing if not field, set it from doc
                // But I'll handle it during resolution
                String otherUid = null;
                for (String p : conv.getParticipants()) {
                    if (!p.equals(myUid)) {
                        otherUid = p;
                        break;
                    }
                }

                if (otherUid != null) {
                    final String finalOtherUid = otherUid;
                    final Conversation finalConv = new Conversation(
                        doc.getId(), 
                        conv.getParticipants(), 
                        conv.getLastMessage(), 
                        conv.getLastTimestamp(),
                        conv.getLastSenderId(),
                        conv.isRead()
                    );
                    
                    // Resolve friend profile
                    firestoreRepo.getUser(finalOtherUid).addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            Friend friend = mapDocToFriend(userDoc);
                            finalConv.setFriend(friend);
                            
                            // Check if already in list to update, or add new
                            boolean exists = false;
                            for (int i = 0; i < conversations.size(); i++) {
                                if (conversations.get(i).getId().equals(finalConv.getId())) {
                                    conversations.set(i, finalConv);
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) conversations.add(finalConv);
                            
                            // Sorting by timestamp after resolution
                            Collections.sort(conversations, (c1, c2) -> {
                                if (c1.getLastTimestamp() == null && c2.getLastTimestamp() == null) return 0;
                                if (c1.getLastTimestamp() == null) return 1;
                                if (c2.getLastTimestamp() == null) return -1;
                                return c2.getLastTimestamp().compareTo(c1.getLastTimestamp());
                            });
                            adapter.setConversations(conversations);
                        }
                    });
                }
            }
        });
    }

    private void openChat(Conversation conversation) {
        Intent intent = new Intent(this, FriendMessageActivity.class);
        intent.putExtra("conversationId", conversation.getId());
        if (conversation.getFriend() != null) {
            intent.putExtra("friendName", conversation.getFriend().getName());
            intent.putExtra("friendUid", conversation.getFriend().getId());
            intent.putExtra("friendPhoto", conversation.getFriend().getPhotoUrl());
        }
        startActivity(intent);
    }

    private void showFriendsListForNewChat() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.CustomBottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.activity_connect, null); // Reusing search UI if possible or create new
        // For simplicity, let's just fetch all friends and show them in a simple list here
        
        View sheetView = getLayoutInflater().inflate(R.layout.popup_layout, null);
        TextView title = sheetView.findViewById(R.id.popupTitle);
        title.setText("New Message");
        title.setTextColor(getResources().getColor(R.color.white));
        
        RecyclerView rv = sheetView.findViewById(R.id.popupRecyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));
        
        FriendAdapter friendAdapter = new FriendAdapter(friend -> {
            startChatWithFriend(friend);
            dialog.dismiss();
        });
        rv.setAdapter(friendAdapter);

        firestoreRepo.getFriendsQuery(myUid).get().addOnSuccessListener(snapshot -> {
            List<com.google.android.gms.tasks.Task<DocumentSnapshot>> tasks = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                String friendUid = doc.getString("friendUid");
                if (friendUid != null) tasks.add(firestoreRepo.getUser(friendUid));
            }
            com.google.android.gms.tasks.Tasks.whenAllSuccess(tasks).addOnSuccessListener(docs -> {
                List<Friend> friends = new ArrayList<>();
                for (Object o : docs) {
                    Friend f = mapDocToFriend((DocumentSnapshot) o);
                    if (f != null) friends.add(f);
                }
                friendAdapter.setFriends(friends);
            });
        });

        dialog.setContentView(sheetView);
        dialog.show();
    }

    private void startChatWithFriend(Friend friend) {
        // Generate conversation ID: uid1_uid2 alphabetical
        String convId;
        if (myUid.compareTo(friend.getId()) < 0) convId = myUid + "_" + friend.getId();
        else convId = friend.getId() + "_" + myUid;

        Intent intent = new Intent(this, FriendMessageActivity.class);
        intent.putExtra("conversationId", convId);
        intent.putExtra("friendName", friend.getName());
        intent.putExtra("friendUid", friend.getId());
        intent.putExtra("friendPhoto", friend.getPhotoUrl());
        startActivity(intent);
    }

    private Friend mapDocToFriend(DocumentSnapshot doc) {
        try {
            Double lat = doc.getDouble("latitude");
            Double lng = doc.getDouble("longitude");
            return new Friend(
                    doc.getId(),
                    doc.getString("name"),
                    doc.getString("username"),
                    doc.getString("email"),
                    doc.getString("profile_photo_url"),
                    lat != null ? lat : 0,
                    lng != null ? lng : 0,
                    "online".equals(doc.getString("status")),
                    true, true, true // Added proximitySharingEnabled=true as default for chat
            );
        } catch (Exception e) {
            return null;
        }
    }
}
