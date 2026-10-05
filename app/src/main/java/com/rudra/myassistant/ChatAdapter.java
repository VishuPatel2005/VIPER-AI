package com.rudra.myassistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.widget.Button;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.rudra.myassistant.model.PlaceSuggestionModel;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_ASSISTANT = 2;

    private final List<ChatMessage> messages = new ArrayList<>();

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void removeLastMessage() {
        if (!messages.isEmpty()) {
            int lastIndex = messages.size() - 1;
            messages.remove(lastIndex);
            notifyItemRemoved(lastIndex);
        }
    }

    public void clearMessages() {
        int size = messages.size();
        messages.clear();
        notifyItemRangeRemoved(0, size);
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_ASSISTANT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_USER) {
            View view = inflater.inflate(R.layout.item_chat_user, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_chat_assistant, parent, false);
            return new AssistantMessageViewHolder(view);
        }
    }

    private int lastAnimatedPosition = -1;

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(message);
        } else if (holder instanceof AssistantMessageViewHolder) {
            ((AssistantMessageViewHolder) holder).bind(message);
        }

        // Animated entrance for new messages
        if (position > lastAnimatedPosition) {
            AnimationHelper.applySlideUpFade(holder.itemView, 0); // index 0 for immediate but smooth
            lastAnimatedPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView messageText;

        UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getText());
        }
    }

    static class AssistantMessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView messageText;
        private final RecyclerView rvPlaces;

        AssistantMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            rvPlaces = itemView.findViewById(R.id.rvPlaces);
            if (rvPlaces != null) {
                rvPlaces.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
            }
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getText());
            if (rvPlaces != null) {
                if (message.getPlaces() != null && !message.getPlaces().isEmpty()) {
                    rvPlaces.setVisibility(View.VISIBLE);
                    rvPlaces.setAdapter(new PlacesAdapter(message.getPlaces()));
                } else {
                    rvPlaces.setVisibility(View.GONE);
                }
            }
        }
    }

    static class PlacesAdapter extends RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder> {
        private final List<PlaceSuggestionModel> places;

        PlacesAdapter(List<PlaceSuggestionModel> places) {
            this.places = places;
        }

        @NonNull
        @Override
        public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_place_suggestion_card, parent, false);
            return new PlaceViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
            PlaceSuggestionModel p = places.get(position);
            holder.name.setText(p.getPlaceName());
            holder.distance.setText(p.getFormattedDistance());
            holder.btn.setOnClickListener(v -> {
                String uri = String.format(java.util.Locale.ENGLISH, "google.navigation:q=%f,%f(%s)", p.getLatitude(), p.getLongitude(), p.getPlaceName());
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                try {
                    v.getContext().startActivity(intent);
                } catch (android.content.ActivityNotFoundException e) {
                    // Fallback to generic ACTION_VIEW if Google Maps is not installed
                    intent.setPackage(null);
                    v.getContext().startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() { return places.size(); }

        static class PlaceViewHolder extends RecyclerView.ViewHolder {
            TextView name, distance;
            Button btn;
            PlaceViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.tvPlaceName);
                distance = itemView.findViewById(R.id.tvDistance);
                btn = itemView.findViewById(R.id.btnDirections);
            }
        }
    }
}

