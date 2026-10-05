package com.rudra.myassistant.network;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SocialApi {
    @GET("/friends/search")
    Call<List<FriendSearchResponse>> searchFriends(@Query("user_id") String userId, @Query("query") String query);

    @POST("/send-request")
    Call<StatusResponse> sendRequest(@Body RequestBody request);

    @POST("/accept-request")
    Call<StatusResponse> acceptRequest(@Query("user_id") String userId, @Query("request_id") int requestId);

    @POST("/reject-request")
    Call<StatusResponse> rejectRequest(@Query("user_id") String userId, @Query("request_id") int requestId);

    @GET("/pending-requests/{user_id}")
    Call<List<FriendRequestResponse>> getPendingRequests(@retrofit2.http.Path("user_id") String userId);

    @GET("/friends/{user_id}")
    Call<List<Friend>> getFriends(@retrofit2.http.Path("user_id") String userId);

    class RequestBody {
        public String user_id;
        public String target_email;
        public RequestBody(String user_id, String target_email) {
            this.user_id = user_id;
            this.target_email = target_email;
        }
    }

    class StatusResponse {
        public String status;
        public String message;
    }

    class FriendSearchResponse {
        public String user_id;
        public String name;
        public String email;
        public String photo_url;
        public String friendship_status;
    }

    class FriendRequestResponse {
        public int id;
        public String sender_id;
        public String sender_name;
        public String sender_email;
        public String sender_photo;
        public String status;
        public String created_at;
    }
}
