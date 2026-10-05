package com.rudra.myassistant.location;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.rudra.myassistant.Reminder;

import java.util.ArrayList;
import java.util.List;

public class GeofenceManager {
    private static final String TAG = "GeofenceManager";
    private final GeofencingClient geofencingClient;
    private final PendingIntent geofencePendingIntent;
    private final Context context;

    public GeofenceManager(Context context) {
        this.context = context;
        this.geofencingClient = LocationServices.getGeofencingClient(context);
        this.geofencePendingIntent = createPendingIntent();
    }

    private PendingIntent createPendingIntent() {
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        return PendingIntent.getBroadcast(context, 0, intent, flags);
    }

    @SuppressLint("MissingPermission")
    public void addGeofence(Reminder reminder) {
        if (!reminder.hasLocation()) return;

        try {
            Geofence geofence = new Geofence.Builder()
                    .setRequestId(String.valueOf(reminder.getId()))
                    .setCircularRegion(
                            reminder.getLatitude(),
                            reminder.getLongitude(),
                            reminder.getRadius()
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(
                            (reminder.isRemindOnArrival() ? Geofence.GEOFENCE_TRANSITION_ENTER : 0) |
                            (reminder.isRemindOnDeparture() ? Geofence.GEOFENCE_TRANSITION_EXIT : 0)
                    )
                    .build();

            GeofencingRequest request = new GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofence(geofence)
                    .build();

            geofencingClient.addGeofences(request, geofencePendingIntent)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Geofence added for: " + reminder.getTitle()))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to add geofence", e));
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: Missing location permissions to add geofence", e);
        } catch (Exception e) {
            Log.e(TAG, "Exception adding geofence", e);
        }
    }

    public void removeGeofence(long reminderId) {
        List<String> ids = new ArrayList<>();
        ids.add(String.valueOf(reminderId));
        geofencingClient.removeGeofences(ids)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Geofence removed for: " + reminderId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to remove geofence", e));
    }
}
