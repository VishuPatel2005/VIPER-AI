package com.rudra.myassistant.network;

import com.google.firebase.Timestamp;
import java.util.List;

public class Conversation {
    private String id;
    private List<String> participants;
    private String lastMessage;
    private Timestamp lastTimestamp;
    private String lastSenderId;
    private boolean read;
    private Friend friend; // Resolved friend object for UI

    public Conversation() {}

    public Conversation(String id, List<String> participants, String lastMessage, Timestamp lastTimestamp, String lastSenderId, boolean read) {
        this.id = id;
        this.participants = participants;
        this.lastMessage = lastMessage;
        this.lastTimestamp = lastTimestamp;
        this.lastSenderId = lastSenderId;
        this.read = read;
    }

    public String getId() { return id; }
    public List<String> getParticipants() { return participants; }
    public String getLastMessage() { return lastMessage; }
    public Timestamp getLastTimestamp() { return lastTimestamp; }
    public String getLastSenderId() { return lastSenderId; }
    public boolean isRead() { return read; }
    public Friend getFriend() { return friend; }

    public void setFriend(Friend friend) { this.friend = friend; }

    public void setId(String id) { this.id = id; }
    public void setParticipants(List<String> participants) { this.participants = participants; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public void setLastTimestamp(Timestamp lastTimestamp) { this.lastTimestamp = lastTimestamp; }
    public void setLastSenderId(String lastSenderId) { this.lastSenderId = lastSenderId; }
    public void setRead(boolean read) { this.read = read; }
}
