package com.rudra.myassistant.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PhotonResponse {
    @SerializedName("features")
    private List<PhotonFeature> features;

    public List<PhotonFeature> getFeatures() {
        return features;
    }

    public static class PhotonFeature {
        @SerializedName("geometry")
        private Geometry geometry;

        @SerializedName("properties")
        private Properties properties;

        public Geometry getGeometry() {
            return geometry;
        }

        public Properties getProperties() {
            return properties;
        }
    }

    public static class Geometry {
        @SerializedName("coordinates")
        private List<Double> coordinates; // [longitude, latitude]

        public List<Double> getCoordinates() {
            return coordinates;
        }

        public double getLat() {
            return coordinates.get(1);
        }

        public double getLng() {
            return coordinates.get(0);
        }
    }

    public static class Properties {
        @SerializedName("name")
        private String name;

        @SerializedName("city")
        private String city;

        @SerializedName("country")
        private String country;

        @SerializedName("state")
        private String state;

        @SerializedName("street")
        private String street;

        @SerializedName("housenumber")
        private String houseNumber;

        public String getName() {
            return name != null ? name : (street != null ? street : "Unknown Place");
        }

        public String getFormattedAddress() {
            StringBuilder sb = new StringBuilder();
            if (city != null) sb.append(city);
            if (state != null) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(state);
            }
            if (country != null) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(country);
            }
            return sb.toString();
        }
    }
}
