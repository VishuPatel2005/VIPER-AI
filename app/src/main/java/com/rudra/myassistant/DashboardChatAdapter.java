package com.rudra.myassistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.rudra.myassistant.network.Conversation;
import java.util.ArrayList;
import java.util.List;

public class DashboardChatAdapter extends RecyclerView.Adapter<DashboardChatAdapter.ViewHolder> {

    private List<Conversation> conversations = new ArrayList<>();
    private final OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(Conversation conversation);
    }

    public DashboardChatAdapter(OnChatClickListener listener) {
        this.listener = listener;
    }

    public void setConversations(List<Conversation> conversations) {
        this.conversations = conversations;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);
        
        if (conversation.getFriend() != null) {
            holder.name.setText(conversation.getFriend().getName());
            Glide.with(holder.itemView.getContext())
                    .load(conversation.getFriend().getPhotoUrl() != null ? conversation.getFriend().getPhotoUrl() : "https://ui-avatars.com/api/?name=" + conversation.getFriend().getName())
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .into(holder.avatar);
        } else {
            holder.name.setText("User");
            holder.avatar.setImageResource(R.drawable.ic_person);
        }

        holder.lastMessage.setText(conversation.getLastMessage() != null ? conversation.getLastMessage() : "No messages yet");
        holder.itemView.setOnClickListener(v -> listener.onChatClick(conversation));
    }

    @Override
    public int getItemCount() {
        return Math.min(conversations.size(), 3); // Only show top 3 on dashboard
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView name;
        TextView lastMessage;

        ViewHolder(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.chatAvatar);
            name = itemView.findViewById(R.id.chatName);
            lastMessage = itemView.findViewById(R.id.chatLastMessage);
        }
    }
}
