package com.rudra.myassistant;

public class Reminder {

    private long id;
    private String backendId;  // UUID from backend
    private String title;
    private boolean isFlagged;
    private boolean isUrgent;
    private boolean isCompleted;
    private Long dateTimestamp; // Unix timestamp in milliseconds
    private Integer hour; // Hour (0-23)
    private Integer minute; // Minute (0-59)
    private String dateTime; // Raw ISO datetime string for easy sorting
    private String locationName;
    private Double latitude;
    private Double longitude;
    private float radius = 100.0f;
    private boolean remindOnArrival = true;
    private boolean remindOnDeparture = false;

    public Reminder(String title, boolean isFlagged) {
        this.title = title;
        this.isFlagged = isFlagged;
        this.isUrgent = false;
        this.isCompleted = false;
        this.dateTimestamp = null;
        this.hour = null;
        this.minute = null;
    }

    public Reminder(long id, String title, boolean isFlagged, boolean isUrgent, boolean isCompleted, Long dateTimestamp, Integer hour, Integer minute) {
        this.id = id;
        this.title = title;
        this.isFlagged = isFlagged;
        this.isUrgent = isUrgent;
        this.isCompleted = isCompleted;
        this.dateTimestamp = dateTimestamp;
        this.hour = hour;
        this.minute = minute;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getBackendId() {
        return backendId;
    }

    public void setBackendId(String backendId) {
        this.backendId = backendId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isFlagged() {
        return isFlagged;
    }

    public void setFlagged(boolean flagged) {
        isFlagged = flagged;
    }

    public boolean isUrgent() {
        return isUrgent;
    }

    public void setUrgent(boolean urgent) {
        isUrgent = urgent;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public Long getDateTimestamp() {
        return dateTimestamp;
    }

    public void setDateTimestamp(Long dateTimestamp) {
        this.dateTimestamp = dateTimestamp;
    }

    public Integer getHour() {
        return hour;
    }

    public void setHour(Integer hour) {
        this.hour = hour;
    }

    public Integer getMinute() {
        return minute;
    }

    public void setMinute(Integer minute) {
        this.minute = minute;
    }

    public boolean hasDate() {
        return dateTimestamp != null;
    }

    public boolean hasTime() {
        return hour != null && minute != null;
    }

    public boolean isScheduled() {
        return hasDate() || hasTime();
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public float getRadius() { return radius; }
    public void setRadius(float radius) { this.radius = radius; }

    public boolean isRemindOnArrival() { return remindOnArrival; }
    public void setRemindOnArrival(boolean remindOnArrival) { this.remindOnArrival = remindOnArrival; }

    public boolean isRemindOnDeparture() { return remindOnDeparture; }
    public void setRemindOnDeparture(boolean remindOnDeparture) { this.remindOnDeparture = remindOnDeparture; }
    
    public boolean hasLocation() { return latitude != null && longitude != null; }
}
