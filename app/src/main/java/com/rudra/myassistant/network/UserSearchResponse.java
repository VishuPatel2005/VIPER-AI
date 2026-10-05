package com.rudra.myassistant.network;

import com.google.gson.annotations.SerializedName;

public class UserSearchResponse {
    @SerializedName("user_id")
    private String userId;
    private String name;
    private String username;
    @SerializedName("photo_url")
    private String photoUrl;
    @SerializedName("is_friend")
    private boolean isFriend;

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getPhotoUrl() { return photoUrl; }
    public boolean isFriend() { return isFriend; }
}
