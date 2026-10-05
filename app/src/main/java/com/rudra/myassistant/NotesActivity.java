package com.rudra.myassistant;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.rudra.myassistant.R;
import java.util.ArrayList;
import java.util.List;

public class NotesActivity extends BaseEdgeToEdgeActivity {

    private RecyclerView notesList;
    private NotesAdapter adapter;
    private TextView notesCount;
    private CardView addNoteBtn;
    private NoteViewModel viewModel;
    private android.widget.EditText searchEditText;
    private List<Note> fullNoteList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        viewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        addNoteBtn = findViewById(R.id.add_note_btn);
        addNoteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NotesActivity.this, NoteDetailActivity.class);
                startActivity(intent);
            }
        });

        notesCount = findViewById(R.id.notes_count);
        notesList = findViewById(R.id.notes_list);
        notesList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new NotesAdapter();
        adapter.setOnNoteClickListener(new NotesAdapter.OnNoteClickListener() {
            @Override
            public void onNoteClick(Note note) {
                Intent intent = new Intent(NotesActivity.this, NoteDetailActivity.class);
                intent.putExtra("note_id", note.getId());
                intent.putExtra("note_title", note.getTitle());
                intent.putExtra("note_content", note.getContent());
                intent.putExtra("note_pinned", note.isPinned());
                startActivity(intent);
            }
        });

        adapter.setOnNoteMenuClickListener(new NotesAdapter.OnNoteMenuClickListener() {
            @Override
            public void onNoteMenuClick(Note note, View view) {
                showNoteMenu(note, view);
            }
        });

        notesList.setAdapter(adapter);
        
        searchEditText = findViewById(R.id.search_edit_text);
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNotes(s.toString());
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        observeViewModel();

        // Viper Motion System — entrance animation
        View root = findViewById(android.R.id.content);
        if (root != null) {
            float dp = root.getResources().getDisplayMetrics().density * 24;
            root.setTranslationY(dp);
            root.setAlpha(0.5f);
            root.animate().translationY(0).alpha(1f)
                    .setDuration(350)
                    .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                    .start();
        }
    }

    private void observeViewModel() {
        viewModel.getNotes().observe(this, new Observer<List<Note>>() {
            @Override
            public void onChanged(List<Note> notes) {
                fullNoteList = notes;
                filterNotes(searchEditText.getText().toString());
            }
        });

        viewModel.getError().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String error) {
                if (error != null) {
                    // Handle error if needed
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadNotes();
    }

    private void filterNotes(String query) {
        if (query.isEmpty()) {
            updateList(fullNoteList);
            return;
        }

        List<Note> filtered = new ArrayList<>();
        for (Note n : fullNoteList) {
            if (n.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                n.getContent().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(n);
            }
        }
        updateList(filtered);
    }

    private void updateList(List<Note> allNotes) {
        List<Object> items = new ArrayList<>();
        List<Note> pinned = new ArrayList<>();
        List<Note> others = new ArrayList<>();

        for (Note n : allNotes) {
            if (n.isPinned()) pinned.add(n);
            else others.add(n);
        }

        if (!pinned.isEmpty()) {
            items.add("Pinned");
            items.addAll(pinned);
        }

        if (!others.isEmpty()) {
            if (!pinned.isEmpty()) {
                items.add("Others");
            }
            items.addAll(others);
        }

        adapter.setItems(items);
        notesCount.setText(allNotes.size() + " Notes");
    }

    private void showNoteMenu(final Note note, View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.note_item_menu, popup.getMenu());

        MenuItem pinItem = popup.getMenu().findItem(R.id.action_pin);
        if (pinItem != null) {
            pinItem.setTitle(note.isPinned() ? "Unpin" : "Pin");
        }

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_pin) {
                    viewModel.updateNote(note.getId(), null, null, !note.isPinned(), null);
                    return true;
                } else if (id == R.id.action_delete) {
                    viewModel.deleteNote(note.getId());
                    Toast.makeText(NotesActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });
        popup.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
