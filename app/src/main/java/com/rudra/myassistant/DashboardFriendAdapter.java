package com.rudra.myassistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.rudra.myassistant.network.Friend;
import java.util.ArrayList;
import java.util.List;

public class DashboardFriendAdapter extends RecyclerView.Adapter<DashboardFriendAdapter.ViewHolder> {

    private List<Friend> friends = new ArrayList<>();
    private final OnFriendClickListener listener;

    public interface OnFriendClickListener {
        void onFriendClick(Friend friend);
    }

    public DashboardFriendAdapter(OnFriendClickListener listener) {
        this.listener = listener;
    }

    public void setFriends(List<Friend> friends) {
        this.friends = friends;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_friend, parent, false);
        return new ViewHolder(view);
    }

    private int lastAnimatedPosition = -1;

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Friend friend = friends.get(position);
        holder.name.setText(friend.getName());
        
        // Circular avatar with Glide
        Glide.with(holder.itemView.getContext())
                .load(friend.getPhotoUrl() != null ? friend.getPhotoUrl() : "https://ui-avatars.com/api/?name=" + friend.getName())
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .into(holder.avatar);

        // Distance labeling
        double dist = friend.getLastDistance();
        if (dist < 100) {
            holder.distance.setText("Nearby");
        } else if (dist < 1000) {
            holder.distance.setText((int)dist + " m");
        } else {
            holder.distance.setText(String.format("%.1f km", dist / 1000.0));
        }

        holder.itemView.setOnClickListener(v -> listener.onFriendClick(friend));

        if (position > lastAnimatedPosition) {
            AnimationHelper.applySlideUpFade(holder.itemView, position);
            lastAnimatedPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView name;
        TextView distance;

        ViewHolder(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.friendAvatar);
            name = itemView.findViewById(R.id.friendName);
            distance = itemView.findViewById(R.id.friendDistance);
        }
    }
}
