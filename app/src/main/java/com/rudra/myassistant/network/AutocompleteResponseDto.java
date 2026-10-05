package com.rudra.myassistant.network;

import java.util.List;

public class AutocompleteResponseDto {
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
        private String formatted;
        private String street;
        private String city;
        private String country;

        public String getName() { return name != null ? name : formatted; }
        public String getFormatted() { return formatted; }
        public String getAddress() { return formatted; }
    }

    public static class Geometry {
        private List<Double> coordinates;
        public List<Double> getCoordinates() { return coordinates; }
        public double getLon() { return coordinates.get(0); }
        public double getLat() { return coordinates.get(1); }
    }
}
