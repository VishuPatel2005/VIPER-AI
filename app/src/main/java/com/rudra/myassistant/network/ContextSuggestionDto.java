package com.rudra.myassistant.network;

import java.util.List;

public class ContextSuggestionDto {
    private String context_type;
    private String message;
    private List<String> suggestions;
    private String place_name;
    private Double distance;

    public String getContextType() { return context_type; }
    public void setContextType(String type) { this.context_type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public String getPlaceName() { return place_name; }
    public void setPlaceName(String name) { this.place_name = name; }

    public Double getDistance() { return distance; }
    public void setDistance(Double distance) { this.distance = distance; }
}
