package com.rudra.myassistant;

import com.google.firebase.Timestamp;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Note implements Serializable {
    private String id;
    private String title;
    private String content;
    private boolean pinned;
    private boolean locked;
    
    @SerializedName("created_at")
    private Timestamp createdAt;
    
    @SerializedName("updated_at")
    private Timestamp updatedAt;

    public Note() {
        // No-arg constructor for Firestore
    }

    public Note(String id, String title, String content, boolean pinned, boolean locked, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.locked = locked;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
