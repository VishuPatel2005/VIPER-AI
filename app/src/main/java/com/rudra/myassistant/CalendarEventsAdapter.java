package com.rudra.myassistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rudra.myassistant.CalendarEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarEventsAdapter extends RecyclerView.Adapter<CalendarEventsAdapter.EventViewHolder> {

    public interface OnEventDeleteListener {
        void onDelete(String eventId);
    }

    private List<CalendarEvent> events = new ArrayList<>();
    private final OnEventDeleteListener deleteListener;

    public CalendarEventsAdapter(OnEventDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setEvents(List<CalendarEvent> newEvents) {
        this.events = newEvents;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        CalendarEvent event = events.get(position);
        holder.bind(event, deleteListener);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTv;
        private final TextView timeTv;
        private final View deleteBtn;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTv = itemView.findViewById(R.id.event_title);
            timeTv = itemView.findViewById(R.id.event_time);
            deleteBtn = itemView.findViewById(R.id.event_delete);
        }

        public void bind(CalendarEvent event, OnEventDeleteListener listener) {
            titleTv.setText(event.getTitle());
            
            // Simplified time formatting for display
            try {
                // Assuming startTime is ISO string: 2026-02-15T15:00:00
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = isoFormat.parse(event.getStartTime());
                if (date != null) {
                    SimpleDateFormat displayFormat = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
                    timeTv.setText(displayFormat.format(date));
                } else {
                    timeTv.setText(event.getStartTime());
                }
            } catch (Exception e) {
                timeTv.setText(event.getStartTime());
            }

            deleteBtn.setOnClickListener(v -> listener.onDelete(event.getId()));
        }
    }
}
