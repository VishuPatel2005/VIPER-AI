package com.rudra.myassistant.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class OverpassResponse {
    @SerializedName("elements")
    private List<Element> elements;

    public List<Element> getElements() {
        return elements;
    }

    public static class Element {
        @SerializedName("type")
        private String type;

        @SerializedName("id")
        private long id;

        @SerializedName("lat")
        private double lat;

        @SerializedName("lon")
        private double lon;

        @SerializedName("tags")
        private Map<String, String> tags;

        public double getLat() {
            return lat;
        }

        public double getLon() {
            return lon;
        }

        public String getName() {
            if (tags != null && tags.containsKey("name")) {
                return tags.get("name");
            }
            return "Nearby Place";
        }

        public String getCategory() {
            if (tags != null) {
                if (tags.containsKey("amenity")) return tags.get("amenity");
                if (tags.containsKey("shop")) return tags.get("shop");
                if (tags.containsKey("leisure")) return tags.get("leisure");
                if (tags.containsKey("tourism")) return tags.get("tourism");
            }
            return "Place";
        }
    }
}
