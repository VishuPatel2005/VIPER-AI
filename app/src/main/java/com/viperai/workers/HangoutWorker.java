package com.viperai.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.rudra.myassistant.model.PlaceSuggestionModel;
import com.rudra.myassistant.network.GeoapifyService;
import com.viperai.notifications.NotificationHelper;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class HangoutWorker extends Worker {

    public HangoutWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String friendName = getInputData().getString("friendName");
        double lat = getInputData().getDouble("lat", 0);
        double lng = getInputData().getDouble("lng", 0);

        if (friendName == null || lat == 0 || lng == 0) return Result.success();

        GeoapifyService geoService = new GeoapifyService();
        final String[] placesArr = new String[3];
        final int[] count = {0};
        CountDownLatch latch = new CountDownLatch(1);

        // categories: catering (restaurants/cafes/bars), entertainment (cinema, etc) or leisure
        geoService.fetchNearbyPlaces(lat, lng, "catering,leisure", 500, 3, new GeoapifyService.GeoapifyCallback() {
            @Override
            public void onResult(List<PlaceSuggestionModel> places) {
                for (int i = 0; i < places.size() && i < 3; i++) {
                    placesArr[i] = places.get(i).getPlaceName();
                    if (placesArr[i] == null) placesArr[i] = "Unnamed place";
                    count[0]++;
                }
                latch.countDown();
            }

            @Override
            public void onError(String error) {
                latch.countDown();
            }
        });

        try {
            latch.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(friendName).append(" is very close to you right now.\n\n");
        promptBuilder.append("Here are 3 places you could meet:\n");
        for (int i = 0; i < count[0]; i++) {
            promptBuilder.append("- ").append(placesArr[i]).append("\n");
        }
        if (count[0] == 0) {
            promptBuilder.append("- A cozy spot nearby\n");
        }
        promptBuilder.append("\nWould you like directions or more suggestions?");

        NotificationHelper.showHangoutNotification(getApplicationContext(), friendName, promptBuilder.toString());

        return Result.success();
    }
}
