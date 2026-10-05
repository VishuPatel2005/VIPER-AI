package com.rudra.myassistant.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * Re-implemented LocationTracker to fix accidental deletion.
 * Manages fused location updates and provides a singleton access to current location.
 */
public class LocationTracker {
    private static final String TAG = "LocationTracker";
    private static LocationTracker instance;
    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private Location lastKnownLocation;
    private LocationCallback locationCallback;
    private boolean isTracking = false;

    public interface LocationChangeListener {
        void onLocationChanged(Location location);
    }

    private LocationChangeListener listener;

    private LocationTracker(Context context) {
        this.context = context.getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.context);
        
        this.locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    lastKnownLocation = location;
                    Log.d(TAG, "Location updated: " + location.getLatitude() + ", " + location.getLongitude());
                    if (listener != null) {
                        listener.onLocationChanged(location);
                    }
                }
            }
        };
    }

    public void setLocationChangeListener(LocationChangeListener listener) {
        this.listener = listener;
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new LocationTracker(context);
        }
    }

    public static synchronized LocationTracker getInstance() {
        if (instance == null) {
            throw new IllegalStateException("LocationTracker must be initialized with init(Context) before use.");
        }
        return instance;
    }

    public void startTracking() {
        if (isTracking) return;
        
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && 
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permissions not granted.");
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        isTracking = true;
        Log.i(TAG, "Location tracking started.");
    }

    public void stopTracking() {
        if (!isTracking) return;
        fusedLocationClient.removeLocationUpdates(locationCallback);
        isTracking = false;
        Log.i(TAG, "Location tracking stopped.");
    }

    public Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    public Location getCurrentLocationSync() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return lastKnownLocation;
        }
        try {
            com.google.android.gms.tasks.Task<Location> task = fusedLocationClient.getLastLocation();
            return com.google.android.gms.tasks.Tasks.await(task, 2, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            return lastKnownLocation;
        }
    }
}
