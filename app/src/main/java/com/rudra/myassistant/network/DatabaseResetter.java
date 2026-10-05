package com.rudra.myassistant.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DatabaseResetter {

    private static final String PREF_NAME = "DatabaseResetPrefs";
    private static final String KEY_RESET_DONE = "is_full_identity_migration_reset_v2";

    public static void checkAndReset(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            if (!prefs.getBoolean(KEY_RESET_DONE, false)) {
                Log.i("DatabaseResetter", "Triggering full migration reset...");
                resetLocalTables(context);
                
                // Force logout to trigger onboarding for the new system
                UserSessionManager sessionManager = new UserSessionManager(context);
                sessionManager.logoutUser();
                
                // Only attempt Firebase signout if apps are initialized
                if (!com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                    try {
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                    } catch (Exception e) {
                        Log.w("DatabaseResetter", "Firebase signout failed (possibly not initialized)");
                    }
                }

                prefs.edit().putBoolean(KEY_RESET_DONE, true).apply();
                Log.d("DatabaseResetter", "Full migration reset complete. Forced logout.");
            }
        } catch (Exception e) {
            Log.e("DatabaseResetter", "Fatal error during database reset: " + e.getMessage(), e);
        }
    }

    private static void resetLocalTables(Context context) {
        Log.d("DatabaseResetter", "Starting local database reset for Firebase migration...");
        
        String[] dbNames = {"chat_history.db", "medications.db", "reminders.db"};
        String[] tablesToClear = {
            "messages", "sessions", "medications", "reminders"
        };

        // We clear rows from the tables to avoid schema conflicts while keeping the structure
        for (String dbName : dbNames) {
            try (SQLiteDatabase db = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)) {
                // Check which tables exist and clear them
                for (String table : tablesToClear) {
                    try {
                        db.delete(table, null, null);
                        Log.d("DatabaseResetter", "Cleared table: " + table + " in " + dbName);
                    } catch (Exception e) {
                        // Table might not exist in this specific DB, which is fine
                    }
                }
            } catch (Exception e) {
                Log.e("DatabaseResetter", "Error resetting DB " + dbName + ": " + e.getMessage());
            }
        }
        
        // Also clear 'users' and 'friends' if they exist in any local DB
        // Based on previous logs, users were in a separate 'users.db' or similar
        try (SQLiteDatabase db = context.openOrCreateDatabase("users.db", Context.MODE_PRIVATE, null)) {
            db.delete("users", null, null);
            db.delete("friends", null, null);
            Log.d("DatabaseResetter", "Cleared users and friends tables");
        } catch (Exception e) {
            // Ignore if users.db doesn't exist
        }

        Log.d("DatabaseResetter", "Database reset complete.");
    }
}
