package com.rudra.myassistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rudra.myassistant.network.Friend;
import java.util.ArrayList;
import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    public interface OnFriendClickListener {
        void onFriendClick(Friend friend);
    }

    public interface OnAddFriendClickListener {
        void onAddFriendClick(Friend friend);
    }

    public interface OnFriendActionClickListener {
        void onFriendActionClick(Friend friend);
    }

    private List<Friend> friends = new ArrayList<>();
    private final OnFriendClickListener listener;
    private OnAddFriendClickListener addListener;
    private OnFriendActionClickListener actionListener;
    private boolean isSearchMode = false;
    private double userLat = 0, userLng = 0;

    public FriendAdapter(OnFriendClickListener listener) {
        this.listener = listener;
    }

    public void setOnAddFriendClickListener(OnAddFriendClickListener addListener) {
        this.addListener = addListener;
    }

    public void setOnFriendActionClickListener(OnFriendActionClickListener actionListener) {
        this.actionListener = actionListener;
    }

    public void setSearchMode(boolean searchMode) {
        this.isSearchMode = searchMode;
    }

    public void setCurrentLocation(double lat, double lng) {
        this.userLat = lat;
        this.userLng = lng;
        notifyDataSetChanged();
    }

    public void setFriends(List<Friend> newFriends) {
        this.friends = newFriends;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    private int lastAnimatedPosition = -1;

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        Friend friend = friends.get(position);
        holder.bind(friend, userLat, userLng, listener, addListener, actionListener, isSearchMode);
        
        if (position > lastAnimatedPosition) {
            AnimationHelper.applySlideUpFade(holder.itemView, position);
            lastAnimatedPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView usernameText;
        private final TextView distanceText;
        private final ImageView avatarImage;
        private final View statusIndicator;
        private final TextView addFriendButton;
        private final View actionIcon;
        private final View dismissButton;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.friendName);
            usernameText = itemView.findViewById(R.id.friendUsername);
            distanceText = itemView.findViewById(R.id.friendDistance);
            avatarImage = itemView.findViewById(R.id.friendAvatar);
            statusIndicator = itemView.findViewById(R.id.onlineStatusIndicator);
            addFriendButton = itemView.findViewById(R.id.btnAddFriend);
            actionIcon = itemView.findViewById(R.id.friendActionIcon);
            dismissButton = itemView.findViewById(R.id.btnDismissFriend);
        }

        public void bind(Friend friend, double userLat, double userLng, OnFriendClickListener listener, OnAddFriendClickListener addListener, OnFriendActionClickListener actionListener, boolean isSearchMode) {
            nameText.setText(friend.getName());
            if (friend.getUsername() != null && !friend.getUsername().isEmpty()) {
                usernameText.setText("@" + friend.getUsername().toLowerCase());
                usernameText.setVisibility(View.VISIBLE);
            } else {
                usernameText.setVisibility(View.GONE);
            }
            
            if (friend.getLatitude() != 0 && userLat != 0) {
                float[] distResults = new float[1];
                android.location.Location.distanceBetween(userLat, userLng, friend.getLatitude(), friend.getLongitude(), distResults);
                float distanceInMeters = distResults[0];
                if (distanceInMeters >= 1000) {
                    distanceText.setText(String.format("%.1f km away", distanceInMeters / 1000.0));
                } else {
                    distanceText.setText(String.format("%d m away", (int)distanceInMeters));
                }
                distanceText.setVisibility(View.VISIBLE);
            } else {
                distanceText.setText("Last active just now"); // Snapchat feeling
                distanceText.setVisibility(View.VISIBLE);
            }

            // Load profile photo using Glide
            String photoUrl = friend.getPhotoUrl();
            com.bumptech.glide.Glide.with(itemView.getContext())
                    .load(photoUrl != null && !photoUrl.isEmpty() ? photoUrl : "https://ui-avatars.com/api/?name="+friend.getName()+"&background=random")
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(avatarImage);
            
            // Online status indicator
            statusIndicator.setVisibility(View.VISIBLE);
            statusIndicator.setBackgroundResource(friend.isOnline() ? R.drawable.indicator_online : R.drawable.indicator_offline);
            
            if (isSearchMode) {
                addFriendButton.setVisibility(View.VISIBLE);
                actionIcon.setVisibility(View.GONE);
                addFriendButton.setOnClickListener(v -> {
                    if (addListener != null) addListener.onAddFriendClick(friend);
                });
            } else {
                addFriendButton.setVisibility(View.GONE);
                actionIcon.setVisibility(View.VISIBLE);
                if (actionListener != null) {
                    actionIcon.setOnClickListener(v -> actionListener.onFriendActionClick(friend));
                }
            }

            itemView.setOnClickListener(v -> listener.onFriendClick(friend));
        }
    }
}
