package com.rudra.myassistant;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {
    public Reminder getReminderAt(int pos) {
        if (pos >= 0 && pos < reminders.size()) return reminders.get(pos);
        return null;
    }

    private List<Reminder> reminders;
    private Context context;
    private OnReminderChangedListener listener;

    public interface OnReminderChangedListener {
        void onReminderChanged();

        default void onReminderDeleted(Reminder reminder) {
            onReminderChanged();
        }

        default void onReminderCompleted(Reminder reminder) {
            onReminderChanged();
        }

        default void onReminderFlagged(Reminder reminder) {
            onReminderChanged();
        }
    }

    public ReminderAdapter(Context context) {
        this.context = context;
        this.reminders = new ArrayList<>();
    }

    public void setOnReminderChangedListener(OnReminderChangedListener listener) {
        this.listener = listener;
    }

    public void setReminders(List<Reminder> reminders) {
        this.reminders = reminders;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reminder, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        Reminder reminder = reminders.get(position);
        holder.titleTextView.setText(reminder.getTitle());

        // Minimal date/time display under the title
        String dateTimeText = buildDateTimeText(reminder);
        if (dateTimeText.isEmpty()) {
            holder.dateTimeTextView.setVisibility(View.GONE);
        } else {
            holder.dateTimeTextView.setText(dateTimeText);
            holder.dateTimeTextView.setVisibility(View.VISIBLE);
        }

        if (reminder.isFlagged() || reminder.isUrgent()) {
            holder.flaggedIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.flaggedIndicator.setVisibility(View.GONE);
        }

        holder.moreButton.setOnClickListener(v -> {
            showOptionsPopup(v, reminder, position);
        });

        if (isSelectionMode) {
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.moreButton.setVisibility(View.GONE);
            holder.checkBox.setChecked(selectedIds.contains(reminder.getBackendId()));

            holder.itemView.setOnClickListener(v -> {
                toggleSelection(reminder.getBackendId());
            });
            holder.checkBox.setOnClickListener(v -> {
                toggleSelection(reminder.getBackendId());
            });
        } else {
            holder.checkBox.setVisibility(View.GONE);
            holder.moreButton.setVisibility(View.VISIBLE);
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onItemLongClick();
                    setSelectionMode(true);
                    toggleSelection(reminder.getBackendId());
                    return true;
                }
                return false;
            });
        }
    }

    private String buildDateTimeText(Reminder reminder) {
        StringBuilder builder = new StringBuilder();

        if (reminder.getDateTimestamp() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
            builder.append(dateFormat.format(new Date(reminder.getDateTimestamp())));
        }

        if (reminder.getHour() != null && reminder.getMinute() != null) {
            if (builder.length() > 0) {
                builder.append(" · ");
            }
            int hour = reminder.getHour();
            int minute = reminder.getMinute();

            int displayHour = hour;
            String amPm = displayHour < 12 ? "AM" : "PM";
            if (displayHour > 12) {
                displayHour -= 12;
            } else if (displayHour == 0) {
                displayHour = 12;
            }

            builder.append(String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, amPm));
        }

        return builder.toString();
    }

    private void showOptionsPopup(View anchorView, Reminder reminder, int position) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.reminder_options_popup, null);

        popupView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int popupWidth = popupView.getMeasuredWidth();
        int popupHeight = popupView.getMeasuredHeight();

        final PopupWindow popupWindow = new PopupWindow(
                popupView,
                popupWidth,
                popupHeight,
                true);

        popupView.findViewById(R.id.action_edit).setOnClickListener(v -> {
            if (reminder.getBackendId() == null || reminder.getBackendId().isEmpty()) {
                Toast.makeText(context, "Cannot edit this reminder", Toast.LENGTH_SHORT).show();
                popupWindow.dismiss();
                return;
            }
            Intent intent = new Intent(context, EditReminderActivity.class);
            intent.putExtra("reminder_backend_id", reminder.getBackendId());
            intent.putExtra("reminder_title", reminder.getTitle());
            intent.putExtra("reminder_flagged", reminder.isFlagged());
            intent.putExtra("reminder_urgent", reminder.isUrgent());
            intent.putExtra("reminder_date", reminder.getDateTimestamp() != null ? reminder.getDateTimestamp() : -1);
            intent.putExtra("reminder_hour", reminder.getHour() != null ? reminder.getHour() : -1);
            intent.putExtra("reminder_minute", reminder.getMinute() != null ? reminder.getMinute() : -1);
            context.startActivity(intent);
            popupWindow.dismiss();
        });

        TextView flagText = popupView.findViewById(R.id.text_flag);
        flagText.setText(reminder.isFlagged() ? "Unflag" : "Flag");

        popupView.findViewById(R.id.action_flag).setOnClickListener(v -> {
            if (reminder.getBackendId() == null || reminder.getBackendId().isEmpty()) {
                Toast.makeText(context, "Cannot flag this reminder", Toast.LENGTH_SHORT).show();
                popupWindow.dismiss();
                return;
            }
            if (listener != null) {
                listener.onReminderFlagged(reminder);
            }
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.action_complete).setOnClickListener(v -> {
            if (reminder.getBackendId() == null || reminder.getBackendId().isEmpty()) {
                Toast.makeText(context, "Cannot mark this reminder as completed", Toast.LENGTH_SHORT).show();
                popupWindow.dismiss();
                return;
            }
            if (reminder.isCompleted()) {
                Toast.makeText(context, "Reminder is already completed", Toast.LENGTH_SHORT).show();
                popupWindow.dismiss();
                return;
            }
            if (listener != null) {
                listener.onReminderCompleted(reminder);
            }
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.action_delete).setOnClickListener(v -> {
            if (reminder.getBackendId() == null || reminder.getBackendId().isEmpty()) {
                Toast.makeText(context, "Cannot delete this reminder", Toast.LENGTH_SHORT).show();
                popupWindow.dismiss();
                return;
            }
            if (listener != null) {
                listener.onReminderDeleted(reminder);
            }
            popupWindow.dismiss();
        });

        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        popupWindow.setAnimationStyle(R.style.PopupAnimation);

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        int x = location[0] - popupWidth + anchorView.getWidth();
        int y = location[1] + anchorView.getHeight() + 8;

        popupWindow.showAtLocation(anchorView.getRootView(), Gravity.NO_GRAVITY, x, y);
    }

    private boolean isSelectionMode = false;
    private final List<String> selectedIds = new ArrayList<>();
    private OnItemLongClickListener longClickListener;

    public interface OnItemLongClickListener {
        void onItemLongClick();

        void onSelectionChanged(int selectedCount);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public void setSelectionMode(boolean selectionMode) {
        this.isSelectionMode = selectionMode;
        if (!selectionMode) {
            selectedIds.clear();
        }
        notifyDataSetChanged();
    }

    public void toggleSelection(String id) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id);
        } else {
            selectedIds.add(id);
        }
        notifyDataSetChanged();
        if (longClickListener != null) {
            longClickListener.onSelectionChanged(selectedIds.size());
        }
    }

    public List<String> getSelectedIds() {
        return new ArrayList<>(selectedIds);
    }

    public void selectAll() {
        selectedIds.clear();
        for (Reminder r : reminders) {
            if (r.getBackendId() != null) {
                selectedIds.add(r.getBackendId());
            }
        }
        notifyDataSetChanged();
        if (longClickListener != null) {
            longClickListener.onSelectionChanged(selectedIds.size());
        }
    }

    public List<Reminder> getReminders() {
        return reminders;
    }

    @Override
    public int getItemCount() {
        return reminders.size();
    }

    static class ReminderViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView dateTimeTextView;
        ImageView flaggedIndicator;
        ImageView moreButton;
        android.widget.CheckBox checkBox;

        ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.reminder_title);
            dateTimeTextView = itemView.findViewById(R.id.reminder_datetime);
            flaggedIndicator = itemView.findViewById(R.id.flagged_indicator);
            moreButton = itemView.findViewById(R.id.more_button);
            checkBox = itemView.findViewById(R.id.reminder_checkbox);
        }
    }
}
