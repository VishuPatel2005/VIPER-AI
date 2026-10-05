package com.rudra.myassistant;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.rudra.myassistant.network.AutocompleteResponseDto;
import com.rudra.myassistant.network.Friend;

import java.util.ArrayList;
import java.util.List;

public class UnifiedSearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_PLACE = 1;
    private static final int TYPE_FRIEND = 2;

    private final List<Object> items = new ArrayList<>();
    private final OnSearchItemClickListener listener;

    public interface OnSearchItemClickListener {
        void onPlaceClick(AutocompleteResponseDto.Feature place);
        void onFriendClick(Friend friend);
        void onAddFriendClick(Friend friend);
    }

    public UnifiedSearchAdapter(OnSearchItemClickListener listener) {
        this.listener = listener;
    }

    public void setResults(List<AutocompleteResponseDto.Feature> places, List<Friend> friends) {
        items.clear();
        if (friends != null && !friends.isEmpty()) {
            items.add("Friends");
            items.addAll(friends);
        }
        if (places != null && !places.isEmpty()) {
            items.add("Places");
            items.addAll(places);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof String) return TYPE_HEADER;
        if (item instanceof AutocompleteResponseDto.Feature) return TYPE_PLACE;
        if (item instanceof Friend) return TYPE_FRIEND;
        return super.getItemViewType(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(R.layout.item_search_header, parent, false));
        } else if (viewType == TYPE_PLACE) {
            return new PlaceViewHolder(inflater.inflate(R.layout.item_place_suggestion, parent, false));
        } else {
            return new FriendViewHolder(inflater.inflate(R.layout.item_friend, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).title.setText((String) item);
        } else if (holder instanceof PlaceViewHolder) {
            AutocompleteResponseDto.Feature place = (AutocompleteResponseDto.Feature) item;
            PlaceViewHolder pvh = (PlaceViewHolder) holder;
            pvh.name.setText(place.getProperties().getName());
            pvh.address.setText(place.getProperties().getFormatted());
            pvh.itemView.setOnClickListener(v -> listener.onPlaceClick(place));
        } else if (holder instanceof FriendViewHolder) {
            Friend friend = (Friend) item;
            FriendViewHolder fvh = (FriendViewHolder) holder;
            fvh.name.setText(friend.getName());
            fvh.username.setText("@" + friend.getUsername());
            fvh.distance.setVisibility(View.GONE); // Distance might not be relevant in search results

            Glide.with(fvh.itemView.getContext())
                    .load(friend.getPhotoUrl() != null ? friend.getPhotoUrl() : "https://ui-avatars.com/api/?name=" + friend.getName())
                    .circleCrop()
                    .into(fvh.avatar);

            // In search result, we might want to show "Add" button if not already friend
            // But for now, let's just make it clickable to focus on map
            fvh.itemView.setOnClickListener(v -> listener.onFriendClick(friend));
            
            // Handle add button if needed
            fvh.btnAddFriend.setVisibility(View.VISIBLE);
            fvh.btnAddFriend.setOnClickListener(v -> listener.onAddFriendClick(friend));
            fvh.actionIcon.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        HeaderViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.searchHeaderTitle);
        }
    }

    static class PlaceViewHolder extends RecyclerView.ViewHolder {
        TextView name, address;
        PlaceViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.placeNameText);
            address = v.findViewById(R.id.placeAddressText);
        }
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView name, username, distance, btnAddFriend;
        ImageView avatar, actionIcon;
        FriendViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.friendName);
            username = v.findViewById(R.id.friendUsername);
            distance = v.findViewById(R.id.friendDistance);
            avatar = v.findViewById(R.id.friendAvatar);
            btnAddFriend = v.findViewById(R.id.btnAddFriend);
            actionIcon = v.findViewById(R.id.friendActionIcon);
        }
    }
}
