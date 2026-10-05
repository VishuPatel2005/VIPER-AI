package com.rudra.myassistant;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class CalendarViewModel extends androidx.lifecycle.AndroidViewModel {

    private final MutableLiveData<List<CalendarEvent>> eventsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final CalendarRepository repository;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public CalendarViewModel(android.app.Application application) {
        super(application);
        repository = new CalendarRepository(application);
    }

    public LiveData<List<CalendarEvent>> getEvents() {
        return eventsLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public void loadEvents() {
        repository.getEvents(new DataCallback<List<CalendarEvent>>() {
            @Override
            public void onSuccess(List<CalendarEvent> eventList) {
                eventsLiveData.postValue(eventList);
            }

            @Override
            public void onError(String error) {
                errorLiveData.postValue("Failed to load events: " + error);
            }
        });
    }

    public void addEvent(String title, String startTime, String description) {
        // endTime defaults to startTime + 1h on backend if omitted
        executorService.execute(() -> {
            try {
                repository.createEvent(title, startTime, null, description, "");
                loadEvents(); // Refresh after create
            } catch (Exception e) {
                e.printStackTrace();
                errorLiveData.postValue("Failed to create event: " + e.getMessage());
            }
        });
    }

    public void deleteEvent(String id) {
        executorService.execute(() -> {
            try {
                repository.deleteEvent(id);
                loadEvents(); // Refresh after delete
            } catch (Exception e) {
                e.printStackTrace();
                errorLiveData.postValue("Failed to delete event: " + e.getMessage());
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
