package com.rudra.myassistant.network;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * Provides a stable session_id per app install, persisted in SharedPreferences.
 */
public final class SessionIdProvider {

    private static final String PREFS_NAME = "assistant_prefs";
    private static final String KEY_SESSION_ID = "session_id";

    private final SharedPreferences prefs;

    public SessionIdProvider(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setSessionId(String id) {
        prefs.edit().putString(KEY_SESSION_ID, id).apply();
    }

    public String getSessionId() {
        String id = prefs.getString(KEY_SESSION_ID, null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_SESSION_ID, id).apply();
        }
        return id;
    }
}
