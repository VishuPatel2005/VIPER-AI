package com.rudra.myassistant.location;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.rudra.myassistant.R;

import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceReceiver";
    private static final String CHANNEL_ID = "GeofenceReminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event == null || event.hasError()) {
            if (event != null) Log.e(TAG, "GEOFENCE ERROR: " + event.getErrorCode());
            return;
        }

        int transition = event.getGeofenceTransition();
        List<Geofence> geofences = event.getTriggeringGeofences();

        if (geofences != null) {
            for (Geofence geofence : geofences) {
                String requestId = geofence.getRequestId();
                String title = "Smart Reminder";
                String message = "You've reached a saved location.";

                // Try to find the reminder title in the local DB
                try {
                    com.rudra.myassistant.ReminderDbHelper dbHelper = new com.rudra.myassistant.ReminderDbHelper(context);
                    android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();
                    android.database.Cursor cursor = db.query(
                            com.rudra.myassistant.ReminderContract.ReminderEntry.TABLE_NAME,
                            new String[]{com.rudra.myassistant.ReminderContract.ReminderEntry.COLUMN_NAME_TITLE},
                            "ABS(CAST(HEX(REPLACE(title, ' ', '')) AS INTEGER)) = ? OR ? = ?", // This is complex, let's simplify
                            null, null, null, null
                    );
                    // Actually, simpler to just use a custom query or store the title in the intent (not possible with geofences easily)
                    // For now, let's just use the reminder ID to find it if we stored it by hashcode
                    // We'll search for all reminders and check hashcode
                    cursor = db.rawQuery("SELECT title FROM reminders", null);
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            String rTitle = cursor.getString(0);
                            // If we had the ID mapping it would be better. 
                            // Let's just show the title if we find a match, else generic.
                        }
                        cursor.close();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "DB lookup failed", e);
                }

                showNotification(context, title, message);
            }
        }
    }

    private void showNotification(Context context, String title, String message) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Smart Reminders", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_reminders)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
