package com.rudra.myassistant.network;

import com.google.firebase.Timestamp;

public class FriendRequest {
    private String id;
    private String senderUid;
    private String receiverUid;
    private String senderName;
    private String senderPhoto;
    private String status;
    private Timestamp timestamp;
    private Timestamp expiresAt;
    private Timestamp cooldownUntil;

    public FriendRequest() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderUid() { return senderUid; }
    public void setSenderUid(String senderUid) { this.senderUid = senderUid; }

    public String getReceiverUid() { return receiverUid; }
    public void setReceiverUid(String receiverUid) { this.receiverUid = receiverUid; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderPhoto() { return senderPhoto; }
    public void setSenderPhoto(String senderPhoto) { this.senderPhoto = senderPhoto; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public Timestamp getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }

    public Timestamp getCooldownUntil() { return cooldownUntil; }
    public void setCooldownUntil(Timestamp cooldownUntil) { this.cooldownUntil = cooldownUntil; }
}
