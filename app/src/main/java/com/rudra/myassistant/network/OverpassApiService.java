package com.rudra.myassistant.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OverpassApiService {
    @GET("interpreter")
    Call<OverpassResponse> getNearbyPlaces(@Query("data") String query);
}
