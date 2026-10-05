package com.rudra.myassistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SymptomsAdapter extends RecyclerView.Adapter<SymptomsAdapter.SymptomViewHolder> {

    private final List<String> symptomList;
    private final Set<String> selectedSymptoms = new HashSet<>();

    public SymptomsAdapter(List<String> symptomList) {
        this.symptomList = symptomList;
    }

    @NonNull
    @Override
    public SymptomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_symptom, parent, false);
        return new SymptomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SymptomViewHolder holder, int position) {
        String symptom = symptomList.get(position);
        holder.bind(symptom, selectedSymptoms.contains(symptom));
        
        holder.itemView.setOnClickListener(v -> {
            if (selectedSymptoms.contains(symptom)) {
                selectedSymptoms.remove(symptom);
            } else {
                selectedSymptoms.add(symptom);
            }
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return symptomList.size();
    }

    public List<String> getSelectedSymptoms() {
        return new ArrayList<>(selectedSymptoms);
    }

    static class SymptomViewHolder extends RecyclerView.ViewHolder {
        TextView symptomName;
        ImageView checkmark;

        public SymptomViewHolder(@NonNull View itemView) {
            super(itemView);
            symptomName = itemView.findViewById(R.id.symptom_name);
            checkmark = itemView.findViewById(R.id.symptom_checkmark);
        }

        public void bind(String name, boolean isSelected) {
            symptomName.setText(name);
            checkmark.setImageResource(isSelected ? R.drawable.ic_circle_filled : R.drawable.ic_circle_outline);
        }
    }
}
