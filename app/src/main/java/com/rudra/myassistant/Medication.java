package com.rudra.myassistant;

public class Medication {
    private long id;
    private String name;
    private String dosage;
    private String time;
    private String days; // e.g., "Mon, Tue, Wed" or "Everyday"
    private boolean isTaken;

    public Medication() {}

    public Medication(long id, String name, String dosage, String time, String days, boolean isTaken) {
        this.id = id;
        this.name = name;
        this.dosage = dosage;
        this.time = time;
        this.days = days;
        this.isTaken = isTaken;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getDays() { return days; }
    public void setDays(String days) { this.days = days; }

    public boolean isTaken() { return isTaken; }
    public void setTaken(boolean taken) { isTaken = taken; }
}
