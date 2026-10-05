package com.rudra.myassistant.network;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ViperApiService {

    @GET("users/search")
    Call<List<UserSearchResponse>> searchUsers(
            @Query("query") String query,
            @Query("user_id") String userId
    );

    @GET("users/{user_id}/settings")
    Call<UserSettings> getSettings(
            @Path("user_id") String userId
    );

    @PUT("users/{user_id}/settings")
    Call<Void> updateSettings(
            @Path("user_id") String userId,
            @Body UserSettings settings
    );

    @PUT("friends/{user_id}/proximity")
    Call<Void> updateFriendProximity(
            @Path("user_id") String userId,
            @Body FriendProximityUpdate request
    );

    @GET("api/context/suggestions")
    Call<List<ContextSuggestion>> getContextSuggestions(
            @Query("user_id") String userId,
            @Query("latitude") double lat,
            @Query("longitude") double lon
    );

    @GET("friends/user/{user_id}")
    Call<List<Friend>> getUserFriends(
            @Path("user_id") String userId
    );

    @POST("friends/add")
    Call<Friend> addFriend(
            @Body FriendAddRequest request
    );

    class ContextSuggestion {
        public String title;
        public String message;
        public String type;
        public Double lat;
        public Double lng;
    }

    class UserSettings {
        public Boolean proximity_sharing_enabled;
        public String timezone;
        public String dnd_start;
        public String dnd_end;
    }

    class FriendProximityUpdate {
        public int friend_id;
        public boolean proximity_sharing_enabled;

        public FriendProximityUpdate(int friend_id, boolean enabled) {
            this.friend_id = friend_id;
            this.proximity_sharing_enabled = enabled;
        }
    }

    class FriendAddRequest {
        public String user_id;
        public String friend_uid;
        public String name;

        public FriendAddRequest(String user_id, String friend_uid, String name) {
            this.user_id = user_id;
            this.friend_uid = friend_uid;
            this.name = name;
        }
    }
}
