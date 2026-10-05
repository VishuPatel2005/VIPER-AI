package com.rudra.myassistant.viewmodel;

public class HomeSuggestionState {
    private final boolean isVisible;
    private final String friendName;
    private final double amount;

    public HomeSuggestionState(boolean isVisible, String friendName, double amount) {
        this.isVisible = isVisible;
        this.friendName = friendName;
        this.amount = amount;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public String getFriendName() {
        return friendName;
    }

    public double getAmount() {
        return amount;
    }
}
