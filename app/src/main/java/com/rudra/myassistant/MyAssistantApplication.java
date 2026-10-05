package com.rudra.myassistant;

import android.app.Application;

import com.rudra.myassistant.voice.CurrentPageTracker;
import com.rudra.myassistant.location.LocationTracker;
import com.google.firebase.FirebaseApp;


public class MyAssistantApplication extends Application {

    private CurrentPageTracker currentPageTracker;

    @Override
    public void onCreate() {
        super.onCreate();
        setupUncaughtExceptionHandler();
        boolean firebaseOk = false;
        try {
            android.util.Log.d("MyAssistantApp", "Initializing Firebase...");
            FirebaseApp.initializeApp(this);
            firebaseOk = true;
        } catch (Throwable t) {
            android.util.Log.e("MyAssistantApp", "Failed to initialize Firebase", t);
        }

        try {
            android.util.Log.d("MyAssistantApp", "Initializing PageTracker...");
            currentPageTracker = new CurrentPageTracker();
            registerActivityLifecycleCallbacks(currentPageTracker);
        } catch (Throwable t) {
            android.util.Log.e("MyAssistantApp", "Failed to initialize PageTracker", t);
        }

        try {
            android.util.Log.d("MyAssistantApp", "Initializing Networking...");
            com.rudra.myassistant.network.RetrofitClient.init(this);
        } catch (Throwable t) {
            android.util.Log.e("MyAssistantApp", "Failed to initialize Networking", t);
        }

        if (firebaseOk) {
            try {
                android.util.Log.d("MyAssistantApp", "Checking DB migrations...");
                com.rudra.myassistant.network.DatabaseResetter.checkAndReset(this);
            } catch (Throwable t) {
                android.util.Log.e("MyAssistantApp", "Failed DatabaseResetter", t);
            }
        }

        try {
            android.util.Log.d("MyAssistantApp", "Initializing Location tracking...");
            com.rudra.myassistant.location.LocationTracker.init(this);
        } catch (Throwable t) {
            android.util.Log.e("MyAssistantApp", "Failed to initialize LocationTracker", t);
        }
        
        android.util.Log.d("MyAssistantApp", "Application initialization complete.");
    }


    public CurrentPageTracker getCurrentPageTracker() {
        return currentPageTracker;
    }

    private void setupUncaughtExceptionHandler() {
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            android.util.Log.e("MyAssistantApp", "CRITICAL UNCAUGHT FAILURE on thread [" + thread.getName() + "]: " + throwable.getMessage(), throwable);
            // Re-throw to let Android handle the crash (shows the "App has stopped" dialog)
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });
    }
}
