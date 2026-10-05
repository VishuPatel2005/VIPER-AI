package com.rudra.myassistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rudra.myassistant.network.P2PMessage;
import java.util.ArrayList;
import java.util.List;

public class FriendMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 0;
    private static final int TYPE_RECEIVED = 1;

    private List<P2PMessage> messages = new ArrayList<>();
    private final String currentUserId;

    public FriendMessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<P2PMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public void addMessage(P2PMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getSenderId().equals(currentUserId) ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_p2p_message_sent, parent, false);
            return new SentViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_p2p_message_received, parent, false);
            return new ReceivedViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        P2PMessage msg = messages.get(position);
        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).text.setText(msg.getText());
        } else {
            ((ReceivedViewHolder) holder).text.setText(msg.getText());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        SentViewHolder(View v) {
            super(v);
            text = v.findViewById(R.id.msgSentText);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        ReceivedViewHolder(View v) {
            super(v);
            text = v.findViewById(R.id.msgReceivedText);
        }
    }
}
