package com.rudra.myassistant;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import com.rudra.myassistant.R;

public class NoteDetailActivity extends BaseEdgeToEdgeActivity {

    private EditText etTitle;
    private EditText etContent;
    private ImageView btnBack;
    private ImageView btnDelete;
    private ImageView btnSave;
    private ImageView btnPin;
    private NoteViewModel viewModel;

    private String noteId = null;
    private boolean isPinned = false;
    private boolean isNewNote = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        viewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        btnBack = findViewById(R.id.btn_back);
        btnDelete = findViewById(R.id.btn_delete);
        btnSave = findViewById(R.id.btn_save);
        btnPin = findViewById(R.id.btn_pin);
        // Check intent
        if (getIntent().hasExtra("note_id")) {
            noteId = getIntent().getStringExtra("note_id");
            isNewNote = false;
            etTitle.setText(getIntent().getStringExtra("note_title"));
            etContent.setText(getIntent().getStringExtra("note_content"));
            isPinned = getIntent().getBooleanExtra("note_pinned", false);
            updatePinIcon();
        }

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        btnPin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPinned = !isPinned;
                updatePinIcon();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isNewNote && noteId != null) {
                    viewModel.deleteNote(noteId);
                    Toast.makeText(NoteDetailActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    finish();
                }
            }
        });

        viewModel.getError().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String error) {
                if (error != null) {
                    Toast.makeText(NoteDetailActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "Note is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String finalTitle = title.isEmpty() ? "Untitled Note" : title;

        if (isNewNote) {
            viewModel.addNote(finalTitle, content, isPinned);
            Toast.makeText(this, "Note Created", Toast.LENGTH_SHORT).show();
        } else {
            if (noteId != null) {
                viewModel.updateNote(noteId, finalTitle, content, isPinned, null);
                Toast.makeText(this, "Note Updated", Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }

    private void updatePinIcon() {
        if (isPinned) {
            btnPin.setAlpha(1.0f);
            btnPin.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.purple_accent));
        } else {
            btnPin.setAlpha(0.5f);
            btnPin.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.white));
        }
    }
}
