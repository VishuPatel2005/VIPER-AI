package com.rudra.myassistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.rudra.myassistant.network.FriendRequest;
import java.util.ArrayList;
import java.util.List;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {

    private List<FriendRequest> requests = new ArrayList<>();
    private final OnRequestActionListener listener;

    public interface OnRequestActionListener {
        void onAccept(FriendRequest request);
        void onDecline(FriendRequest request);
        void onBlock(FriendRequest request);
    }

    public FriendRequestAdapter(OnRequestActionListener listener) {
        this.listener = listener;
    }

    public void setRequests(List<FriendRequest> requests) {
        this.requests = requests;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_request, parent, false));
    }

    private int lastAnimatedPosition = -1;

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FriendRequest req = requests.get(position);
        holder.name.setText(req.getSenderName());
        
        Glide.with(holder.itemView.getContext())
                .load(req.getSenderPhoto() != null ? req.getSenderPhoto() : "https://ui-avatars.com/api/?name=" + req.getSenderName())
                .circleCrop()
                .into(holder.avatar);

        holder.btnAccept.setOnClickListener(v -> listener.onAccept(req));
        holder.btnDecline.setOnClickListener(v -> listener.onDecline(req));
        holder.btnBlock.setOnClickListener(v -> listener.onBlock(req));

        if (position > lastAnimatedPosition) {
            AnimationHelper.applySlideUpFade(holder.itemView, position);
            lastAnimatedPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        ImageView avatar, btnAccept, btnDecline, btnBlock;
        ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.reqName);
            avatar = v.findViewById(R.id.reqAvatar);
            btnAccept = v.findViewById(R.id.btnAcceptReq);
            btnDecline = v.findViewById(R.id.btnDeclineReq);
            btnBlock = v.findViewById(R.id.btnBlockReq);

            if (btnAccept != null) AnimationHelper.applyPressAnimation(btnAccept);
            if (btnDecline != null) AnimationHelper.applyPressAnimation(btnDecline);
            if (btnBlock != null) AnimationHelper.applyPressAnimation(btnBlock);
            AnimationHelper.applyPressAnimation(v, 0.98f);
        }
    }
}
