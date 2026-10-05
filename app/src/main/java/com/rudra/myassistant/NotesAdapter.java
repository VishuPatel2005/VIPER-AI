package com.rudra.myassistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rudra.myassistant.R;
import com.google.firebase.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Object> items = new ArrayList<>();
    private OnNoteClickListener onNoteClickListener;
    private OnNoteMenuClickListener onNoteMenuClickListener;

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_NOTE = 1;

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    public interface OnNoteMenuClickListener {
        void onNoteMenuClick(Note note, View view);
    }

    public void setItems(List<Object> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    public void setOnNoteClickListener(OnNoteClickListener listener) {
        this.onNoteClickListener = listener;
    }

    public void setOnNoteMenuClickListener(OnNoteMenuClickListener listener) {
        this.onNoteMenuClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof String) {
            return TYPE_HEADER;
        }
        return TYPE_NOTE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_note_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_note, parent, false);
            return new NoteViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof HeaderViewHolder && item instanceof String) {
            ((HeaderViewHolder) holder).bind((String) item);
        } else if (holder instanceof NoteViewHolder && item instanceof Note) {
            ((NoteViewHolder) holder).bind((Note) item);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView title;

        HeaderViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView; // root is TextView
        }

        void bind(String headerText) {
            title.setText(headerText);
        }
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {
        private TextView title;
        private TextView content;
        private TextView date;
        private ImageView moreBtn;
        private ImageView icon;

        NoteViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.note_title);
            content = itemView.findViewById(R.id.note_preview);
            date = itemView.findViewById(R.id.note_date);
            moreBtn = itemView.findViewById(R.id.note_more);
            icon = itemView.findViewById(R.id.note_icon);
        }

        void bind(Note note) {
            title.setText(note.getTitle());
            content.setText(note.getContent());

            if (note.getCreatedAt() != null) {
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
                date.setText(outputFormat.format(note.getCreatedAt().toDate()));
            } else {
                date.setText("");
            }

            if (note.isPinned()) {
                icon.setVisibility(View.VISIBLE);
            } else {
                icon.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onNoteClickListener != null) onNoteClickListener.onNoteClick(note);
                }
            });

            moreBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onNoteMenuClickListener != null) onNoteMenuClickListener.onNoteMenuClick(note, v);
                }
            });
        }
    }
}
