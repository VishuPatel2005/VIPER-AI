package com.rudra.myassistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rudra.myassistant.network.MedicationDto;
import java.util.List;

public class MedicationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    // Use Object to hold either a String header or a MedicationDto item
    private List<Object> items;
    private OnMedicationClickListener listener;

    public interface OnMedicationClickListener {
        void onMarkTakenClick(MedicationDto medication);
        void onInfoClick(View view, MedicationDto medication);
    }

    public MedicationAdapter(List<Object> items, OnMedicationClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof String) {
            return TYPE_HEADER;
        }
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medication_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medication, parent, false);
            return new MedicationViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            String header = (String) items.get(position);
            ((HeaderViewHolder) holder).headerText.setText(header);
        } else {
            MedicationDto medication = (MedicationDto) items.get(position);
            ((MedicationViewHolder) holder).bind(medication, listener);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<Object> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerText;
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.header_text);
        }
    }

    static class MedicationViewHolder extends RecyclerView.ViewHolder {
        TextView medTime, medName, medDosage;
        View untakenActions, takenMarker;
        Button btnMarkTaken;
        ImageView btnInfo;

        public MedicationViewHolder(@NonNull View itemView) {
            super(itemView);
            medTime = itemView.findViewById(R.id.med_time);
            medName = itemView.findViewById(R.id.med_name);
            medDosage = itemView.findViewById(R.id.med_dosage);
            untakenActions = itemView.findViewById(R.id.untaken_actions);
            takenMarker = itemView.findViewById(R.id.taken_marker);
            btnMarkTaken = itemView.findViewById(R.id.btn_mark_taken);
            btnInfo = itemView.findViewById(R.id.btn_info);
        }

        public void bind(MedicationDto medication, OnMedicationClickListener listener) {
            medTime.setText(medication.getTime());
            medName.setText(medication.getName());
            medDosage.setText(medication.getDosage());

            if (medication.isTaken()) {
                untakenActions.setVisibility(View.GONE);
                takenMarker.setVisibility(View.VISIBLE);
                itemView.setAlpha(0.7f);
            } else {
                untakenActions.setVisibility(View.VISIBLE);
                takenMarker.setVisibility(View.GONE);
                itemView.setAlpha(1.0f);
            }

            btnMarkTaken.setOnClickListener(v -> listener.onMarkTakenClick(medication));
            btnInfo.setOnClickListener(v -> listener.onInfoClick(v, medication));
        }
    }
}
