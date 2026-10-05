package com.rudra.myassistant;

public class ChatMessage {

    private final String text;
    private final boolean isUser;
    private java.util.List<com.rudra.myassistant.model.PlaceSuggestionModel> places;

    public ChatMessage(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
    }

    public ChatMessage(String text, boolean isUser, java.util.List<com.rudra.myassistant.model.PlaceSuggestionModel> places) {
        this.text = text;
        this.isUser = isUser;
        this.places = places;
    }

    public String getText() {
        return text;
    }

    public boolean isUser() {
        return isUser;
    }

    public java.util.List<com.rudra.myassistant.model.PlaceSuggestionModel> getPlaces() {
        return places;
    }
}

