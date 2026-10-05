package com.rudra.myassistant.network;

import com.rudra.myassistant.AssistantSuggestionMapper;
import com.rudra.myassistant.model.SuggestionCardModel;
import com.rudra.myassistant.model.PlaceSuggestionModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Response;

public class GeoapifyService {
    private final GeoapifyApi api;

    public interface GeoapifyCallback {
        void onResult(List<PlaceSuggestionModel> places);
        void onError(String error);
    }

    public GeoapifyService() {
        this.api = RetrofitClient.getGeoapifyInstance().create(GeoapifyApi.class);
    }

    /**
     * Fetches nearby suggestions with actual place names as titles.
     * Uses conditions=named to ensure only places with real names are returned.
     */
    public List<SuggestionCardModel> fetchNearbySuggestions(double lat, double lon, String categories) {
        List<SuggestionCardModel> result = new ArrayList<>();
        if (lat == 0 && lon == 0) return result; // No location, no suggestions
        
        String filter = "circle:" + lon + "," + lat + ",5000";
        
        try {
            Response<PlacesSearchResponseDto> response = api.searchNearbyWithConditions(
                    categories, filter, "named", 5, ApiConfig.GEOAPIFY_API_KEY
            ).execute();
            
            if (response.isSuccessful() && response.body() != null 
                    && response.body().getFeatures() != null 
                    && !response.body().getFeatures().isEmpty()) {
                for (PlacesSearchResponseDto.Feature f : response.body().getFeatures()) {
                    String name = f.getProperties().getName();
                    if (name != null && !name.trim().isEmpty()) {
                        float dist = f.getProperties().getDistance();
                        String distText = dist < 1000 
                                ? (int) dist + "m away" 
                                : String.format("%.1f km", dist / 1000.0);
                        result.add(AssistantSuggestionMapper.mapToModel(
                                "LOCATION_BASED", name, distText, name));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // No generic fallback — empty list is better than fake suggestions
        return result;
    }


    /**
     * [LEGACY SUPPORT] Used in HangoutSuggestionManager and other context-aware flows.
     */
    public void fetchNearbyPlaces(double lat, double lon, String categories, int radius, int limit, GeoapifyCallback callback) {
        String filter = "circle:" + lon + "," + lat + "," + radius;
        api.searchNearby(categories, filter, limit, ApiConfig.GEOAPIFY_API_KEY).enqueue(new retrofit2.Callback<PlacesSearchResponseDto>() {
            @Override
            public void onResponse(retrofit2.Call<PlacesSearchResponseDto> call, Response<PlacesSearchResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PlaceSuggestionModel> result = new ArrayList<>();
                    for (PlacesSearchResponseDto.Feature f : response.body().getFeatures()) {
                        String firstCat = (f.getProperties().getCategories() != null && !f.getProperties().getCategories().isEmpty()) 
                                ? f.getProperties().getCategories().get(0) : "leisure";
                                
                        result.add(new PlaceSuggestionModel(
                            f.getProperties().getName(),
                            firstCat,
                            f.getGeometry().getLat(),
                            f.getGeometry().getLon(),
                            f.getProperties().getDistance(),
                            f.getProperties().getPlaceId()
                        ));
                    }
                    callback.onResult(result);
                } else {
                    callback.onError("Geoapify error: " + response.code());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<PlacesSearchResponseDto> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public static double[] calculateMidpoint(double lat1, double lon1, double lat2, double lon2) {
        return new double[]{(lat1 + lat2) / 2.0, (lon1 + lon2) / 2.0};
    }
}
