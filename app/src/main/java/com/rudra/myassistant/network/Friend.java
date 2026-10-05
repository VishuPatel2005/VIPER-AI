package com.rudra.myassistant.network;

public class Friend {
    private String id;
    private String name;
    private String username;
    private String email;
    private String photoUrl;
    private double latitude;
    private double longitude;
    private boolean online;
    private boolean iAmSharingWithThem;
    private boolean theyAreSharingWithMe;
    private boolean proximitySharingEnabled;
    private double lastDistance;

    public Friend(String id, String name, String username, String email, String photoUrl, double latitude, double longitude, boolean online, boolean iAmSharingWithThem, boolean theyAreSharingWithMe, boolean proximitySharingEnabled) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.email = email;
        this.photoUrl = photoUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.online = online;
        this.iAmSharingWithThem = iAmSharingWithThem;
        this.theyAreSharingWithMe = theyAreSharingWithMe;
        this.proximitySharingEnabled = proximitySharingEnabled;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPhotoUrl() { return photoUrl; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public boolean isOnline() { return online; }
    public boolean iAmSharingWithThem() { return iAmSharingWithThem; }
    public boolean theyAreSharingWithMe() { return theyAreSharingWithMe; }
    public boolean isProximitySharingEnabled() { return proximitySharingEnabled; }
    public void setProximitySharingEnabled(boolean enabled) { this.proximitySharingEnabled = enabled; }
    
    public double getLastDistance() { return lastDistance; }
    public void setLastDistance(double lastDistance) { this.lastDistance = lastDistance; }

    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
