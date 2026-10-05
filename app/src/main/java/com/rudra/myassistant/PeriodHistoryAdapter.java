package com.rudra.myassistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Calendar;

public class PeriodHistoryAdapter extends RecyclerView.Adapter<PeriodHistoryAdapter.ViewHolder> {

    private List<Map<String, String>> cycles;
    private Map<String, Map<String, Object>> logs;
    private SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
    private SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());

    public PeriodHistoryAdapter(List<Map<String, String>> cycles, Map<String, Map<String, Object>> logs) {
        this.cycles = cycles;
        this.logs = logs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_period_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> cycle = cycles.get(position);
        String start = cycle.get("start");
        String end = cycle.get("end");

        if (start == null || end == null) return;

        try {
            Date startDate = inputFormat.parse(start);
            Date endDate = inputFormat.parse(end);
            
            holder.monthText.setText(monthFormat.format(startDate));
            holder.dayText.setText(dayFormat.format(startDate));
            
            int cycleNum = cycles.size() - position - 1;
            holder.titleText.setText("Cycle " + cycleNum + " (" + monthFormat.format(startDate) + " " + dayFormat.format(startDate) + "):");
            
            // Calculate days
            long diff = endDate.getTime() - startDate.getTime();
            long days = (diff / (1000 * 60 * 60 * 24)) + 1;
            holder.summaryText.setText(days + " days, Live Tracking Data");

            // Setup Expansion
            holder.expansionContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());

            // Add divider
            View line = new View(holder.itemView.getContext());
            line.setBackgroundColor(0x1AFFFFFF);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
            lp.setMargins(0, 0, 0, 24);
            holder.expansionContainer.addView(line, lp);

            for (int i = 0; i < days; i++) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(startDate);
                cal.add(Calendar.DATE, i);
                String dateStr = inputFormat.format(cal.getTime());

                View row = inflater.inflate(android.R.layout.simple_list_item_1, holder.expansionContainer, false);
                TextView tv = row.findViewById(android.R.id.text1);
                tv.setTextColor(0xFF8A8A8E);
                tv.setTextSize(12);
                tv.setPadding(0, 0, 0, 12);

                String detail = "Day " + (i + 1) + ": ";
                Map<String, Object> log = logs.get(dateStr);
                if (log != null) {
                    String flow = (String) log.get("flow");
                    detail += (flow != null ? flow : "No") + " Flow";
                    List<String> symptoms = (List<String>) log.get("symptoms");
                    if (symptoms != null && !symptoms.isEmpty()) {
                        detail += " • " + String.join(", ", symptoms);
                    } else {
                        detail += " • No Symptoms";
                    }
                } else {
                    detail += "No Details Logged";
                }
                tv.setText(detail);
                holder.expansionContainer.addView(row);
            }

            holder.itemView.setOnClickListener(v -> {
                boolean isExpanded = holder.expansionContainer.getVisibility() == View.VISIBLE;
                holder.expansionContainer.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            });

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return cycles != null ? cycles.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView monthText, dayText, titleText, summaryText;
        LinearLayout expansionContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            monthText = itemView.findViewById(R.id.history_month);
            dayText = itemView.findViewById(R.id.history_day);
            titleText = itemView.findViewById(R.id.history_cycle_title);
            summaryText = itemView.findViewById(R.id.history_summary);
            expansionContainer = itemView.findViewById(R.id.details_expansion);
        }
    }
}
