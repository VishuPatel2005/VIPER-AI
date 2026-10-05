package com.rudra.myassistant.network;

import com.google.gson.annotations.SerializedName;

public class ReminderDto {
    @SerializedName("id")
    private String id;
    
    @SerializedName("text")
    private String text;
    
    @SerializedName("datetime")
    private String datetime;
    
    @SerializedName("completed")
    private boolean completed;
    
    @SerializedName("flagged")
    private boolean flagged;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("location_name")
    private String locationName;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    @SerializedName("radius")
    private float radius = 100.0f;

    @SerializedName("remind_on_arrival")
    private boolean remindOnArrival = true;

    @SerializedName("remind_on_departure")
    private boolean remindOnDeparture = false;

    public ReminderDto() {}

    public ReminderDto(String id, String text, String datetime, boolean completed, boolean flagged, String createdAt, String updatedAt) {
        this.id = id;
        this.text = text;
        this.datetime = datetime;
        this.completed = completed;
        this.flagged = flagged;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getDatetime() { return datetime; }
    public void setDatetime(String datetime) { this.datetime = datetime; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public boolean isFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
