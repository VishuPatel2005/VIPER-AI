package com.viperai.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.rudra.myassistant.MainActivity;
import com.rudra.myassistant.ChatActivity;
import com.rudra.myassistant.RemindersActivity;
import com.rudra.myassistant.CalendarActivity;
import com.rudra.myassistant.ConnectActivity;

import static com.viperai.notifications.NotificationConstants.*;

public class NotificationHelper {

    public static void createAllChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            createChannel(manager, CHANNEL_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_HIGH, true);
            createChannel(manager, CHANNEL_EVENTS, "Events", NotificationManager.IMPORTANCE_HIGH, true);
            createChannel(manager, CHANNEL_SUGGESTIONS, "Live Suggestions", NotificationManager.IMPORTANCE_DEFAULT, false);
            createChannel(manager, CHANNEL_FRIENDS, "Nearby Friends", NotificationManager.IMPORTANCE_DEFAULT, true);
            createChannel(manager, CHANNEL_HANGOUT, "Hangout Planner", NotificationManager.IMPORTANCE_HIGH, true);
            createChannel(manager, CHANNEL_EXPENSES, "Pending Expenses", NotificationManager.IMPORTANCE_DEFAULT, true);
            createChannel(manager, CHANNEL_NUDGE, "Assistant Nudges", NotificationManager.IMPORTANCE_LOW, false);
        }
    }

    private static void createChannel(NotificationManager manager, String id, String name, int importance, boolean hasSound) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(id) != null) return;
            NotificationChannel channel = new NotificationChannel(id, name, importance);
            if (!hasSound) {
                channel.setSound(null, null);
            }
            manager.createNotificationChannel(channel);
        }
    }

    public static void showReminderNotification(Context ctx, String reminderId, String title, String body) {
        if (!isToggleEnabled(ctx, PREF_NOTIF_REMINDERS)) return;

        Intent intent = new Intent(ctx, RemindersActivity.class);
        intent.putExtra(EXTRA_REMINDER_ID, reminderId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, reminderId.hashCode(), intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = createBaseBuilder(ctx, CHANNEL_REMINDERS, title, body, pendingIntent);
        notify(ctx, NOTIF_ID_REMINDER + reminderId.hashCode(), builder.build());
    }

    public static void showEventNotification(Context ctx, String eventId, String title, String body) {
        if (!isToggleEnabled(ctx, PREF_NOTIF_EVENTS)) return;

        Intent intent = new Intent(ctx, CalendarActivity.class);
        intent.putExtra(EXTRA_EVENT_ID, eventId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, eventId.hashCode(), intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = createBaseBuilder(ctx, CHANNEL_EVENTS, title, body, pendingIntent);
        notify(ctx, NOTIF_ID_EVENT + eventId.hashCode(), builder.build());
    }

    public static void showSuggestionNotification(Context ctx, String title, String body) {
        if (!isToggleEnabled(ctx, PREF_NOTIF_SUGGESTIONS)) return;
        
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastShown = prefs.getLong(PREF_LAST_SUGGESTION_NOTIF, 0);
        if (System.currentTimeMillis() - lastShown < 3 * 3600000) return;

        Intent intent = new Intent(ctx, MainActivity.class);
        intent.putExtra(EXTRA_SCROLL_TO_SUGGESTIONS, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, NOTIF_ID_SUGGESTION, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = createBaseBuilder(ctx, CHANNEL_SUGGESTIONS, title, body, pendingIntent);
        notify(ctx, NOTIF_ID_SUGGESTION, builder.build());

        prefs.edit().putLong(PREF_LAST_SUGGESTION_NOTIF, System.currentTimeMillis()).apply();
    }

    public static void showFriendNearbyNotification(Context ctx, String friendUid, String friendName, float distanceMeters) {
        if (!isToggleEnabled(ctx, PREF_NOTIF_FRIENDS)) return;

        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String throttleKey = "last_friend_notif_" + friendUid;
        long lastShown = prefs.getLong(throttleKey, 0);
        if (System.currentTimeMillis() - lastShown < 30 * 60000) return;

        Intent intent = new Intent(ctx, ConnectActivity.class);
        intent.putExtra(EXTRA_FRIEND_UID, friendUid);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, friendUid.hashCode(), intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = "Friend Nearby";
        String body = friendName + " is " + (int)distanceMeters + "m away";

        NotificationCompat.Builder builder = createBaseBuilder(ctx, CHANNEL_FRIENDS, title, body, pendingIntent);
        notify(ctx, NOTIF_ID_FRIEND + friendUid.hashCode(), builder.build());

        prefs.edit().putLong(throttleKey, System.currentTimeMillis()).apply();
    }

    public static void showHangoutNotification(Context ctx, String friendName, String hangoutPrompt) {
        if (!isToggleEnabled(ctx, PREF_NOTIF_HANGOUT)) return;

        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String throttleKey = "last_hangout_notif_" + friendName; // Using name as proxy if UID not available or just following logic
        long lastShown = prefs.getLong(throttleKey, 0);
        if (System.currentTimeMillis() - lastShown < 60 * 60000) return;

        Intent intent = new Intent(ctx, ChatActivity.class);
        intent.putExtra(EXTRA_HANGOUT_PROMPT, hangoutPrompt);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, NOTIF_ID_HANGOUT, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = "Plan Hangout";
        String body = friendName + " is very close to you";

        NotificationCompat.Builder builder = createBaseBuilder(ctx, CHANNEL_HANGOUT, title, body, pendingIntent);
        notify(ctx, NOTIF_ID_HANGOUT, builder.build());

        prefs.edit().putLong(throttleKey, System.currentTimeMillis()).apply();
    }

    public static void showExpenseNotification(Context ctx, String expenseId, String owedTo, String amount) {
        if (!isToggleEnabled(ctx, PREF_NOTIF_EXPENSES)) return;

        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String throttleKey = "expense_notif_" + expenseId;
        long lastShown = prefs.getLong(throttleKey, 0);
        if (System.currentTimeMillis() - lastShown < 24 * 3600000) return;

        // SharedExpenseActivity doesn't exist, using MainActivity
        Intent intent = new Intent(ctx, MainActivity.class); 
        intent.putExtra(EXTRA_EXPENSE_ID, expenseId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, expenseId.hashCode(), intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = "Pending Payment";
        String body = "You owe " + owedTo + " " + amount;

        NotificationCompat.Builder builder = createBaseBuilder(ctx, CHANNEL_EXPENSES, title, body, pendingIntent);
        notify(ctx, NOTIF_ID_EXPENSE + expenseId.hashCode(), builder.build());

        prefs.edit().putLong(throttleKey, System.currentTimeMillis()).apply();
    }

    public static void showNudgeNotification(Context ctx, String title, String body) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastShown = prefs.getLong(PREF_LAST_NUDGE_NOTIF, 0);
        if (System.currentTimeMillis() - lastShown < 6 * 3600000) return;

        Intent intent = new Intent(ctx, ChatActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, NOTIF_ID_NUDGE, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = createBaseBuilder(ctx, CHANNEL_NUDGE, title, body, pendingIntent);
        notify(ctx, NOTIF_ID_NUDGE, builder.build());

        prefs.edit().putLong(PREF_LAST_NUDGE_NOTIF, System.currentTimeMillis()).apply();
    }

    private static NotificationCompat.Builder createBaseBuilder(Context ctx, String channelId, String title, String body, PendingIntent intent) {
        return new NotificationCompat.Builder(ctx, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Placeholder icon
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(intent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
    }

    private static void notify(Context ctx, int id, android.app.Notification notification) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(ctx);
        try {
            notificationManager.notify(id, notification);
        } catch (SecurityException e) {
            // Handle post notifications permission missing on Android 13+
        }
    }

    private static boolean isToggleEnabled(Context ctx, String prefKey) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(prefKey, true);
    }
}
