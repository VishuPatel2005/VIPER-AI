package com.viperai.workers;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.rudra.myassistant.EventRepository;
import com.rudra.myassistant.CalendarEvent;
import com.viperai.notifications.NotificationConstants;
import com.viperai.notifications.NotificationHelper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventWorker extends Worker {
    private final EventRepository repository;

    public EventWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        repository = new EventRepository(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(NotificationConstants.PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(NotificationConstants.PREF_NOTIF_EVENTS, true)) {
            return Result.success();
        }

        List<CalendarEvent> events = repository.getUpcomingEvents();
        long now = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

        for (CalendarEvent event : events) {
            try {
                Date startDate = sdf.parse(event.getStartTime());
                if (startDate == null) continue;
                long startTime = startDate.getTime();
                long diff = startTime - now;

                String window = null;
                String body = null;

                if (Math.abs(diff - 24 * 3600000L) <= 150000) { // Slightly wider tolerance for 5-min intervals
                    window = "24h";
                    body = "starts tomorrow";
                } else if (Math.abs(diff - 3600000L) <= 150000) {
                    window = "1h";
                    body = "starts in 1 hour";
                } else if (Math.abs(diff - 600000L) <= 150000) {
                    window = "10m";
                    body = "starts in 10 minutes";
                }

                if (window != null) {
                    String cacheKey = "event_notif_" + event.getId() + "_" + window;
                    if (!prefs.getBoolean(cacheKey, false)) {
                        NotificationHelper.showEventNotification(getApplicationContext(), event.getId(), event.getTitle(), body);
                        prefs.edit().putBoolean(cacheKey, true).apply();
                    }
                }
            } catch (Exception e) {
                // Ignore parse errors
            }
        }
        return Result.success();
    }
}
