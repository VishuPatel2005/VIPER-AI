package com.rudra.myassistant;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class CalendarEvent implements Serializable {
    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("end_time")
    private String endTime;

    @SerializedName("description")
    private String description;

    @SerializedName("location")
    private String location;

    @SerializedName("created_at")
    private String createdAt;

    public CalendarEvent(String id, String title, String startTime, String endTime, String description, String location) {
        this.id = id;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
        this.location = location;
    }

    public CalendarEvent(String title, String startTime, String endTime, String description, String location) {
        this(null, title, startTime, endTime, description, location);
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public String getCreatedAt() { return createdAt; }
}
