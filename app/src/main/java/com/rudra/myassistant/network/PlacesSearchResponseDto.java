package com.rudra.myassistant.network;

import java.util.List;

/**
 * Data Transfer Object for Geoapify Search Nearby response.
 */
public class PlacesSearchResponseDto {
    private List<Feature> features;

    public List<Feature> getFeatures() { return features; }

    public static class Feature {
        private Properties properties;
        private Geometry geometry;

        public Properties getProperties() { return properties; }
        public Geometry getGeometry() { return geometry; }
    }

    public static class Properties {
        private String name;
        private String street;
        private String city;
        private String country;
        private List<String> categories;
        private double lat;
        private double lon;
        private float distance;
        private String place_id;

        public String getName() { return name != null ? name : street; }
        public String getStreet() { return street; }
        public String getCity() { return city; }
        public String getCountry() { return country; }
        public List<String> getCategories() { return categories; }
        
        // Explicit unique names to avoid any conflict with Geometry methods
        public double getPropLat() { return lat; }
        public double getPropLon() { return lon; }
        public float getDistance() { return distance; }
        public String getPlaceId() { return place_id; }
    }

    public static class Geometry {
        private List<Double> coordinates;
        public List<Double> getCoordinates() { return coordinates; }
        
        // standard lat/lon based on Geoapify [lon, lat] format
        public double getLon() { 
            return (coordinates != null && !coordinates.isEmpty()) ? coordinates.get(0) : 0; 
        }
        public double getLat() { 
            return (coordinates != null && coordinates.size() > 1) ? coordinates.get(1) : 0; 
        }
    }
}
