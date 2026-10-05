package com.rudra.myassistant.network;

import java.util.List;

public class MedicationDto {
    private String user_id;
    private String id;
    private String name;
    private String dosage;
    private String time;
    private List<String> days;
    private boolean is_taken;
    private String specific_date;
    private String created_at;

    public String getUserId() { return user_id; }
    public void setUserId(String userId) { this.user_id = userId; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public List<String> getDays() { return days; }
    public void setDays(List<String> days) { this.days = days; }

    public boolean isTaken() { return is_taken; }
    public void setTaken(boolean taken) { this.is_taken = taken; }

    public String getSpecificDate() { return specific_date; }
    public void setSpecificDate(String specificDate) { this.specific_date = specificDate; }

    public String getCreatedAt() { return created_at; }
    public void setCreatedAt(String createdAt) { this.created_at = createdAt; }
}
