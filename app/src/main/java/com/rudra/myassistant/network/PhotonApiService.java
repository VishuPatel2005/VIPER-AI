package com.rudra.myassistant.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface PhotonApiService {
    @GET("api/")
    Call<PhotonResponse> searchPlaces(
        @Query("q") String query,
        @Query("limit") int limit
    );
}
