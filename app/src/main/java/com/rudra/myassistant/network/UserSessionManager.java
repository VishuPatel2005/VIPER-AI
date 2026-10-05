package com.rudra.myassistant.network;

import android.content.Context;
import android.content.SharedPreferences;

public class UserSessionManager {
    private static final String PREF_NAME = "ViperAssistantPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_USERNAME = "user_username";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_PHOTO_URL = "user_photo_url";
    private static final String KEY_LOGGED_IN = "is_logged_in";
    private static final String KEY_SERVER_URL = "server_url";
    private static final String KEY_USER_PREFERENCES = "user_preferences";
    private static final String KEY_SHARE_LOCATION = "share_location";
    private static final String KEY_THEME = "app_theme";
    public static final String DEFAULT_SERVER_URL = "https://myassistant-buup.onrender.com/";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Context context;

    public UserSessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(String userId, String name, String email, String photoUrl, String username) {
        editor.putBoolean(KEY_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_USERNAME, username);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_PHOTO_URL, photoUrl);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_LOGGED_IN, false);
    }

    public String getUserId() {
        return pref.getString(KEY_USER_ID, null);
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, "User");
    }

    public String getUsername() {
        return pref.getString(KEY_USER_USERNAME, null);
    }

    public void setUsername(String username) {
        editor.putString(KEY_USER_USERNAME, username);
        editor.apply();
    }

    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, null);
    }

    public String getUserPhotoUrl() {
        return pref.getString(KEY_USER_PHOTO_URL, null);
    }

    public void setPhotoUrl(String url) {
        editor.putString(KEY_USER_PHOTO_URL, url);
        editor.apply();
    }

    public String getServerUrl() {
        return pref.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL);
    }

    public void setServerUrl(String url) {
        editor.putString(KEY_SERVER_URL, url);
        editor.apply();
    }

    public void setUserPreferences(java.util.Set<String> prefs) {
        editor.putStringSet(KEY_USER_PREFERENCES, prefs);
        editor.apply();
    }

    public java.util.Set<String> getUserPreferences() {
        return pref.getStringSet(KEY_USER_PREFERENCES, new java.util.HashSet<>());
    }

    public void logoutUser() {
        // Preserve server URL across logout
        String serverUrl = getServerUrl();
        editor.clear();
        editor.putString(KEY_SERVER_URL, serverUrl);
        editor.apply();
    }

    public boolean isLocationSharingEnabled() {
        return pref.getBoolean(KEY_SHARE_LOCATION, true);
    }

    public void setLocationSharingEnabled(boolean enabled) {
        editor.putBoolean(KEY_SHARE_LOCATION, enabled);
        editor.apply();
    }

    public int getAppTheme() {
        return pref.getInt(KEY_THEME, 2); // 0: Light, 1: Dark, 2: System Default
    }

    public void setAppTheme(int theme) {
        editor.putInt(KEY_THEME, theme);
        editor.apply();
    }
}
