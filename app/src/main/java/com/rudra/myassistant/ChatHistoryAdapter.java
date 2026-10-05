package com.rudra.myassistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChatHistoryAdapter extends RecyclerView.Adapter<ChatHistoryAdapter.ViewHolder> {

    public interface OnChatClickListener {
        void onChatClick(ChatHistory chat);
    }

    public interface OnChatLongClickListener {
        void onChatLongClick(ChatHistory chat, View anchor);
    }

    private List<ChatHistory> chats = new ArrayList<>();
    private final OnChatClickListener listener;
    private OnChatLongClickListener longClickListener;

    public ChatHistoryAdapter(OnChatClickListener listener) {
        this.listener = listener;
    }

    public void setOnChatLongClickListener(OnChatLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public void setChats(List<ChatHistory> chats) {
        this.chats = chats;
        notifyDataSetChanged();
    }

    public List<ChatHistory> getChats() {
        return chats;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatHistory chat = chats.get(position);
        holder.tvTitle.setText(chat.getTitle());
        holder.tvLastMessage.setText(chat.getLastMessage());
        holder.itemView.setOnClickListener(v -> listener.onChatClick(chat));
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onChatLongClick(chat, v);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvLastMessage;

        ViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tvChatTitle);
            tvLastMessage = view.findViewById(R.id.tvChatLastMessage);
        }
    }
}
