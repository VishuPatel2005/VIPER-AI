package com.rudra.myassistant.network;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;
import android.content.Context;

public class RetrofitClient {

    private static Retrofit geoapifyRetrofit;
    private static Retrofit viperRetrofit;
    private static final String GEOAPIFY_BASE_URL = "https://api.geoapify.com/";

    public static void init(Context context) {
        UserSessionManager sessionManager = new UserSessionManager(context);
        String baseUrl = sessionManager.getServerUrl();
        
        // Defensive check: ensure baseUrl is a valid URL string
        if (baseUrl == null || baseUrl.trim().isEmpty() || !baseUrl.startsWith("http")) {
            android.util.Log.e("RetrofitClient", "Invalid baseUrl found: [" + baseUrl + "]. Falling back to default.");
            baseUrl = UserSessionManager.DEFAULT_SERVER_URL;
        }

        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        viperRetrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    /**
     * Returns a Retrofit instance pointing to Geoapify.
     */
    public static Retrofit getGeoapifyInstance() {
        if (geoapifyRetrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            geoapifyRetrofit = new Retrofit.Builder()
                    .baseUrl(GEOAPIFY_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return geoapifyRetrofit;
    }

    /**
     * Returns a Retrofit instance pointing to our FastAPI backend.
     */
    public static Retrofit getViperInstance() {
        return viperRetrofit;
    }
}
