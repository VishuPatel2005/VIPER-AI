package com.rudra.myassistant.model;

/**
 * Plain Java data model for place suggestions.
 * Used in Hangout planning flow.
 */
public class PlaceSuggestionModel {
    private String placeName;
    private String placeCategory;
    private double latitude;
    private double longitude;
    private float distanceMeters;
    private String geoapifyPlaceId;

    public PlaceSuggestionModel(String placeName, String placeCategory, double latitude, double longitude, float distanceMeters, String geoapifyPlaceId) {
        this.placeName = placeName;
        this.placeCategory = placeCategory;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceMeters = distanceMeters;
        this.geoapifyPlaceId = geoapifyPlaceId;
    }

    public String getPlaceName() { return placeName; }
    public String getPlaceCategory() { return placeCategory; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public float getDistanceMeters() { return distanceMeters; }
    public String getGeoapifyPlaceId() { return geoapifyPlaceId; }
    
    public void setDistanceMeters(float distance) {
        this.distanceMeters = distance;
    }

    public String getFormattedDistance() {
        if (distanceMeters < 1000) {
            return String.format("%.0fm away", distanceMeters);
        } else {
            return String.format("%.1fkm away", distanceMeters / 1000.0);
        }
    }

    public String toDisplayString() {
        return String.format("%s (%s) - %s", placeName, placeCategory, getFormattedDistance());
    }
}
