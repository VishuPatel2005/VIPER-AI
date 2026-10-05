package com.viperai.workers;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.rudra.myassistant.network.UserSessionManager;
import com.viperai.notifications.NotificationConstants;
import com.viperai.notifications.NotificationHelper;
import java.util.Calendar;
import java.util.Set;

public class SuggestionWorker extends Worker {

    public SuggestionWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences notifPrefs = getApplicationContext().getSharedPreferences(NotificationConstants.PREFS_NAME, Context.MODE_PRIVATE);
        if (!notifPrefs.getBoolean(NotificationConstants.PREF_NOTIF_SUGGESTIONS, true)) {
            return Result.success();
        }

        long lastShown = notifPrefs.getLong(NotificationConstants.PREF_LAST_SUGGESTION_NOTIF, 0);
        if (System.currentTimeMillis() - lastShown < 3 * 3600000) {
            return Result.success();
        }

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String title = "";
        String body = "";

        if (hour >= 6 && hour <= 11) {
            title = "Morning Suggestion";
            body = "Coffee place nearby";
        } else if (hour >= 12 && hour <= 14) {
            title = "Afternoon Pick";
            body = "Lunch spot nearby";
        } else if (hour >= 15 && hour <= 18) {
            title = "Evening Suggestion";
            body = "Gym or park nearby";
        } else if (hour >= 19 && hour <= 23) {
            title = "Night Suggestion";
            body = "Dessert place nearby";
        } else {
            return Result.success();
        }

        UserSessionManager sessionManager = new UserSessionManager(getApplicationContext());
        Set<String> preferences = sessionManager.getUserPreferences();
        
        if (preferences != null && !preferences.isEmpty()) {
            if (hour >= 15 && hour <= 18 && preferences.contains("gym")) {
                body = "Gym near you";
            } else if (hour >= 12 && hour <= 14 && preferences.contains("food")) {
                body = "Restaurant near you";
            } else if (hour >= 6 && hour <= 11 && preferences.contains("cafes")) {
                body = "Cafe near you";
            } else if (hour >= 19 && hour <= 23 && preferences.contains("desserts")) {
                body = "Dessert place near you";
            }
        }

        NotificationHelper.showSuggestionNotification(getApplicationContext(), title, body);
        return Result.success();
    }
}
