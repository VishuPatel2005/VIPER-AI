package com.rudra.myassistant.model;

/**
 * Lightweight data container for Latitude and Longitude.
 * Replaces com.google.android.gms.maps.model.LatLng to avoid external dependency.
 */
public class LatLng {
    public final double latitude;
    public final double longitude;

    public LatLng(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "LatLng{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
