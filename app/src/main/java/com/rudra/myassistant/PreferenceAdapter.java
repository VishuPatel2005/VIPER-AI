package com.rudra.myassistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PreferenceAdapter extends RecyclerView.Adapter<PreferenceAdapter.ViewHolder> {

    public interface OnPreferenceChangeListener {
        void onPreferenceChanged(Set<String> currentPrefs);
    }

    public static class PreferenceItem {
        String id;
        String title;
        int iconRes;

        public PreferenceItem(String id, String title, int iconRes) {
            this.id = id;
            this.title = title;
            this.iconRes = iconRes;
        }
    }

    private final List<PreferenceItem> items;
    private final Set<String> selectedIds;
    private final OnPreferenceChangeListener listener;

    public PreferenceAdapter(List<PreferenceItem> items, Set<String> selectedIds, OnPreferenceChangeListener listener) {
        this.items = items;
        this.selectedIds = selectedIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_preference_chip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PreferenceItem item = items.get(position);
        holder.title.setText(item.title);
        holder.icon.setImageResource(item.iconRes);

        boolean isSelected = selectedIds.contains(item.id);
        updateSelectionUI(holder, isSelected);

        holder.card.setOnClickListener(v -> {
            boolean selected = selectedIds.contains(item.id);
            if (selected) {
                selectedIds.remove(item.id);
            } else {
                selectedIds.add(item.id);
            }
            updateSelectionUI(holder, !selected);
            if (listener != null) {
                listener.onPreferenceChanged(selectedIds);
            }
        });
    }

    private void updateSelectionUI(ViewHolder holder, boolean isSelected) {
        if (isSelected) {
            holder.card.setStrokeColor(holder.itemView.getContext().getColor(R.color.purple_accent));
            holder.card.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.purple_accent_dim));
            holder.checkMark.setVisibility(View.VISIBLE);
        } else {
            holder.card.setStrokeColor(holder.itemView.getContext().getColor(R.color.viper_outline));
            holder.card.setCardBackgroundColor(holder.itemView.getContext().getColor(R.color.card_bg));
            holder.checkMark.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        ImageView icon;
        ImageView checkMark;
        TextView title;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.preferenceCard);
            icon = itemView.findViewById(R.id.ivIcon);
            checkMark = itemView.findViewById(R.id.ivCheck);
            title = itemView.findViewById(R.id.tvTitle);
        }
    }
}
