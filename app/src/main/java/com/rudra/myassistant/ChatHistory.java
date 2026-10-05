package com.rudra.myassistant;

public class ChatHistory {
    private String chatId;
    private String title;
    private String lastMessage;
    private long timestamp;

    public ChatHistory(String chatId, String title, String lastMessage, long timestamp) {
        this.chatId = chatId;
        this.title = title;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
    }

    public String getChatId() { return chatId; }
    public String getTitle() { return title; }
    public String getLastMessage() { return lastMessage; }
    public long getTimestamp() { return timestamp; }
}
