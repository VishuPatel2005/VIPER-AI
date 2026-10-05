package com.rudra.myassistant;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.rudra.myassistant.network.Conversation;
import com.rudra.myassistant.network.Friend;
import java.util.ArrayList;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    private List<Conversation> conversations = new ArrayList<>();
    private final OnConversationClickListener listener;
    private final String myUid;

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    public ConversationAdapter(String myUid, OnConversationClickListener listener) {
        this.myUid = myUid;
        this.listener = listener;
    }

    public void setConversations(List<Conversation> conversations) {
        this.conversations = conversations;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation conv = conversations.get(position);
        Friend friend = conv.getFriend();
        
        if (friend != null) {
            holder.name.setText(friend.getName());
            Glide.with(holder.itemView.getContext())
                    .load(friend.getPhotoUrl() != null ? friend.getPhotoUrl() : "https://ui-avatars.com/api/?name=" + friend.getName())
                    .circleCrop()
                    .into(holder.avatar);
            
            holder.onlineIndicator.setVisibility(friend.isOnline() ? View.VISIBLE : View.GONE);
        } else {
            holder.name.setText("Snapchat User");
        }

        // --- TRADITIONAL MESSAGE PREVIEW LOGIC ---
        String lastMsg = conv.getLastMessage() != null ? conv.getLastMessage() : "New Chat";
        boolean wasMe = conv.getLastSenderId() != null && conv.getLastSenderId().equals(myUid);
        boolean isRead = conv.isRead() || wasMe;
        
        holder.statusText.setText(lastMsg);
        holder.statusIcon.setVisibility(View.GONE);
        
        int colorRes = isRead ? android.R.color.darker_gray : android.R.color.white;
        int color = ContextCompat.getColor(holder.itemView.getContext(), colorRes);
        holder.statusText.setTextColor(color);
        
        holder.statusText.setTypeface(null, isRead ? Typeface.NORMAL : Typeface.BOLD);
        holder.name.setTypeface(null, isRead ? Typeface.NORMAL : Typeface.BOLD);

        if (conv.getLastTimestamp() != null) {
            holder.time.setText(" • " + getRelativeTimeAgo(conv.getLastTimestamp().toDate().getTime()));
        } else {
            holder.time.setText("");
        }
        
        holder.itemView.setOnClickListener(v -> listener.onConversationClick(conv));
    }

    private String getRelativeTimeAgo(long time) {
        long now = System.currentTimeMillis();
        long diff = now - time;
        if (diff < 60 * 1000) return "Just now";
        if (diff < 60 * 60 * 1000) return (diff / (60 * 1000)) + "m ago";
        if (diff < 24 * 60 * 60 * 1000) return (diff / (60 * 60 * 1000)) + "h ago";
        if (diff < 7 * 24 * 60 * 60 * 1000) return (diff / (24 * 60 * 60 * 1000)) + "d ago";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(time));
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar, statusIcon;
        View onlineIndicator;
        TextView name, statusText, time;

        ViewHolder(View v) {
            super(v);
            avatar = v.findViewById(R.id.convAvatar);
            onlineIndicator = v.findViewById(R.id.convOnlineIndicator);
            name = v.findViewById(R.id.convName);
            statusText = v.findViewById(R.id.convStatusText);
            statusIcon = v.findViewById(R.id.statusIcon);
            time = v.findViewById(R.id.convTime);
        }
    }
}
