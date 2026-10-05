package com.rudra.myassistant;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rudra.myassistant.network.FirestoreRepository;
import com.rudra.myassistant.network.P2PMessage;
import java.util.ArrayList;
import java.util.List;

public class FriendMessageActivity extends BaseEdgeToEdgeActivity {

    private RecyclerView recyclerView;
    private FriendMessageAdapter adapter;
    private FirestoreRepository firestoreRepo;
    private EditText inputEdit;
    private String myUid;
    private String friendUid;
    private String conversationId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_message);

        myUid = FirebaseAuth.getInstance().getUid();
        firestoreRepo = new FirestoreRepository();

        conversationId = getIntent().getStringExtra("conversationId");
        friendUid = getIntent().getStringExtra("friendUid");
        String friendName = getIntent().getStringExtra("friendName");
        String friendPhoto = getIntent().getStringExtra("friendPhoto");

        setupHeader(friendName, friendPhoto);

        recyclerView = findViewById(R.id.messagesRecyclerView);
        inputEdit = findViewById(R.id.chatInputEdit);
        ImageView btnSend = findViewById(R.id.btnChatSend);

        adapter = new FriendMessageAdapter(myUid);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnBackMessage).setOnClickListener(v -> finish());

        loadMessageHistory();
        markAsRead();
    }

    private void setupHeader(String name, String photo) {
        TextView nameText = findViewById(R.id.chatFriendName);
        ImageView avatar = findViewById(R.id.chatFriendAvatar);
        
        nameText.setText(name != null ? name : "Friend");
        if (photo != null) {
            Glide.with(this).load(photo).circleCrop().into(avatar);
        }
    }

    private void loadMessageHistory() {
        if (conversationId == null) return;

        firestoreRepo.getMessagesQuery(conversationId).addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e("FriendMessage", "Listen failed", e);
                return;
            }

            if (snapshot != null) {
                List<P2PMessage> messages = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshot) {
                    messages.add(doc.toObject(P2PMessage.class));
                }
                adapter.setMessages(messages);
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                
                // If the last message is from the friend, we've now 'seen' it
                if (!messages.isEmpty()) {
                    P2PMessage last = messages.get(messages.size() - 1);
                    if (!last.getSenderId().equals(myUid)) {
                        markAsRead();
                    }
                }
            }
        });
    }

    private void markAsRead() {
        if (conversationId != null) {
            firestoreRepo.markConversationAsRead(conversationId);
        }
    }

    private void sendMessage() {
        String text = inputEdit.getText().toString().trim();
        if (TextUtils.isEmpty(text) || conversationId == null) return;

        inputEdit.setText("");
        firestoreRepo.sendP2PMessage(conversationId, myUid, friendUid, text)
                .addOnFailureListener(e -> {
                    Log.e("FriendMessage", "Send failed", e);
                });
    }
}
