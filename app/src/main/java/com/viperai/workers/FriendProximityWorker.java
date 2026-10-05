package com.viperai.workers;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Tasks;
import com.rudra.myassistant.ConnectRepository;
import com.viperai.notifications.NotificationConstants;
import com.viperai.notifications.NotificationHelper;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FriendProximityWorker extends Worker {
    private final ConnectRepository repository;

    public FriendProximityWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.repository = new ConnectRepository(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences notifPrefs = getApplicationContext().getSharedPreferences(NotificationConstants.PREFS_NAME, Context.MODE_PRIVATE);
        if (!notifPrefs.getBoolean(NotificationConstants.PREF_NOTIF_FRIENDS, true)) {
            return Result.success();
        }

        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
        Location myLocation = null;
        try {
            myLocation = Tasks.await(locationClient.getLastLocation(), 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            android.util.Log.e("FriendWorker", "Failed to get location", e);
        }

        if (myLocation == null) return Result.success();

        final Location finalMyLocation = myLocation;
        CountDownLatch latch = new CountDownLatch(1);

        repository.getFriendLocations(new ConnectRepository.FriendLocationCallback() {
            @Override
            public void onSuccess(List<ConnectRepository.FriendLocation> friends) {
                for (ConnectRepository.FriendLocation friend : friends) {
                    float[] results = new float[1];
                    Location.distanceBetween(finalMyLocation.getLatitude(), finalMyLocation.getLongitude(),
                            friend.lat, friend.lng, results);
                    float distance = results[0];

                    // Trigger friend nearby alert (< 1km)
                    if (distance < 1000) {
                        NotificationHelper.showFriendNearbyNotification(getApplicationContext(), friend.uid, friend.name, distance);
                    }

                    // Trigger hangout planning (< 100m)
                    if (distance < 100) {
                        Data inputData = new Data.Builder()
                                .putString("friendName", friend.name)
                                .putDouble("lat", friend.lat)
                                .putDouble("lng", friend.lng)
                                .build();
                        
                        OneTimeWorkRequest hangoutRequest = new OneTimeWorkRequest.Builder(HangoutWorker.class)
                                .setInputData(inputData)
                                .addTag(NotificationConstants.TAG_HANGOUT_WORKER)
                                .build();
                        
                        WorkManager.getInstance(getApplicationContext()).enqueue(hangoutRequest);
                    }
                }
                latch.countDown();
            }

            @Override
            public void onError(String error) {
                latch.countDown();
            }
        });

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return Result.success();
    }
}
