package com.rudra.myassistant;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.app.Application;
import java.util.ArrayList;
import java.util.List;

public class NoteViewModel extends AndroidViewModel {

    private NoteRepository repository;
    private MutableLiveData<List<Note>> notes = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();

    public NoteViewModel(@NonNull Application application) {
        super(application);
        repository = new NoteRepository(application);
    }

    public LiveData<List<Note>> getNotes() { return notes; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }

    public void loadNotes() {
        isLoading.setValue(true);
        repository.getNotes(new NoteRepository.DataCallback<List<Note>>() {
            @Override
            public void onSuccess(List<Note> data) {
                 isLoading.setValue(false);
                 notes.setValue(data != null ? data : new ArrayList<>());
            }

            @Override
            public void onError(String err) {
                 isLoading.setValue(false);
                 error.setValue(err);
            }
        });
    }

    public void addNote(String title, String content, boolean pinned) {
        isLoading.setValue(true);
        repository.createNote(title, content, pinned, new NoteRepository.DataCallback<Note>() {
             @Override
             public void onSuccess(Note data) {
                 loadNotes(); 
             }
             @Override
             public void onError(String err) {
                 isLoading.setValue(false);
                 error.setValue(err);
             }
        });
    }

    public void updateNote(String id, String title, String content, Boolean pinned, Boolean locked) {
        isLoading.setValue(true);
        // Pass the params correctly
        repository.updateNote(id, title, content, pinned, locked, new NoteRepository.DataCallback<Note>() {
             @Override
             public void onSuccess(Note data) {
                 loadNotes();
             }
             @Override
             public void onError(String err) {
                 isLoading.setValue(false);
                 error.setValue(err);
             }
        });
    }

    public void deleteNote(String id) {
        isLoading.setValue(true);
        repository.deleteNote(id, new NoteRepository.DefaultCallback() {
            @Override
            public void onSuccess() {
                loadNotes();
            }
            @Override
            public void onError(String err) {
                isLoading.setValue(false);
                error.setValue(err);
            }
        });
    }
}
