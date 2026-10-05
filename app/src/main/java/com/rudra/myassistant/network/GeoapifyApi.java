package com.rudra.myassistant.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GeoapifyApi {
    @GET("v1/geocode/autocomplete")
    Call<AutocompleteResponseDto> getAutocomplete(
        @Query("text") String query, 
        @Query("apiKey") String apiKey
    );

    @GET("v2/places")
    Call<PlacesSearchResponseDto> searchNearby(
        @Query("categories") String categories,
        @Query("filter") String filter,
        @Query("limit") int limit,
        @Query("apiKey") String apiKey
    );

    @GET("v2/places")
    Call<PlacesSearchResponseDto> searchNearbyWithConditions(
        @Query("categories") String categories,
        @Query("filter") String filter,
        @Query("conditions") String conditions,
        @Query("limit") int limit,
        @Query("apiKey") String apiKey
    );

    @GET("v1/routing")
    Call<okhttp3.ResponseBody> getRoute(
        @Query("waypoints") String waypoints,
        @Query("mode") String mode,
        @Query("apiKey") String apiKey
    );

    @GET("v1/geocode/reverse")
    Call<okhttp3.ResponseBody> reverseGeocode(
        @Query("lat") double lat,
        @Query("lon") double lon,
        @Query("apiKey") String apiKey
    );
}
