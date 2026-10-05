package com.rudra.myassistant;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rudra.myassistant.model.SuggestionCardModel;
import com.rudra.myassistant.suggestions.routing.SuggestionActionRouter;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

public class SuggestionsPagerAdapter extends RecyclerView.Adapter<SuggestionsPagerAdapter.ViewHolder> {

    private final Context context;
    private final List<SuggestionCardModel> suggestions = new ArrayList<>();

    public SuggestionsPagerAdapter(Context context) {
        this.context = context;
    }

    public void setSuggestions(List<SuggestionCardModel> list) {
        synchronized (suggestions) {
            this.suggestions.clear();
            if (list != null) {
                this.suggestions.addAll(list);
            }
        }
        notifyDataSetChanged();
    }

    public void addSuggestion(SuggestionCardModel s) {
        if (s == null) return;
        synchronized (suggestions) {
            // Basic deduplication
            for (SuggestionCardModel exists : suggestions) {
                if (exists.getTitle() != null && exists.getTitle().equals(s.getTitle())) return;
            }
            this.suggestions.add(s); // Append to end to prevent index-shift for current user view
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_suggestion_unified, parent, false));
    }

    private int lastAnimatedPosition = -1;

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(suggestions.get(position));
        if (position > lastAnimatedPosition) {
            AnimationHelper.applySlideUpFade(holder.itemView, position);
            lastAnimatedPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        Chip chipTag;
        TextView tvTitle, tvSubtitle, tvDescription;
        View btnAction, btnGetDirections;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivCardIcon);
            chipTag = itemView.findViewById(R.id.chipTagLabel);
            tvTitle = itemView.findViewById(R.id.tvCardTitle);
            tvSubtitle = itemView.findViewById(R.id.tvCardSubtitle);
            tvDescription = itemView.findViewById(R.id.tvCardDescription);
            btnAction = itemView.findViewById(R.id.btnCardAction);
            btnGetDirections = itemView.findViewById(R.id.btnGetDirections);

            if (btnAction != null) AnimationHelper.applyPressAnimation(btnAction);
            if (btnGetDirections != null) AnimationHelper.applyPressAnimation(btnGetDirections);
            AnimationHelper.applyPressAnimation(itemView, 0.98f);
        }

        public void bind(SuggestionCardModel s) {
            if (s == null) return;

            if (tvTitle != null) tvTitle.setText(s.getTitle() != null ? s.getTitle() : "Suggestion");
            if (tvSubtitle != null) tvSubtitle.setText(s.getSubtitle());
            if (tvDescription != null) {
                tvDescription.setText(s.getDescription());
                tvDescription.setVisibility(s.getDescription() != null ? View.VISIBLE : View.GONE);
            }
            
            if (chipTag != null) {
                if (s.getTagLabel() != null) {
                    chipTag.setText(s.getTagLabel());
                    chipTag.setVisibility(View.VISIBLE);
                } else {
                    chipTag.setVisibility(View.GONE);
                }
            }

            if (ivIcon != null) {
                if (s.getIconResName() != null) {
                    int resId = context.getResources().getIdentifier(s.getIconResName(), "drawable", context.getPackageName());
                    if (resId != 0) ivIcon.setImageResource(resId);
                } else {
                    ivIcon.setImageResource(R.drawable.ic_globe);
                }
            }

            if (btnAction != null) {
                if (btnAction instanceof TextView && s.getPrimaryButtonLabel() != null) {
                    ((TextView)btnAction).setText(s.getPrimaryButtonLabel());
                }
                btnAction.setOnClickListener(v -> SuggestionActionRouter.route(context, s));
            }

            if (btnGetDirections != null) {
                String key = s.getActionKey();
                boolean isLocation = SuggestionActionRouter.ACTION_OPEN_MAP_NAVIGATION.equals(key) || 
                                     SuggestionActionRouter.ACTION_SEARCH_MAP.equals(key);
                
                if (isLocation) {
                    btnGetDirections.setVisibility(View.VISIBLE);
                    btnGetDirections.setOnClickListener(v -> SuggestionActionRouter.route(context, s));
                } else {
                    btnGetDirections.setVisibility(View.GONE);
                }
            }
            
        }
    }
}
