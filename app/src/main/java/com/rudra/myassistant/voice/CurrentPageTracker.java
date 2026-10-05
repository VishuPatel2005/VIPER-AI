package com.rudra.myassistant.voice;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

public class CurrentPageTracker implements Application.ActivityLifecycleCallbacks {

    public enum PageType {
        REMINDER,
        NOTES,
        HEALTH,
        CHAT,
        CALENDAR,
        MAIN,
        OTHER
    }

    private PageType currentPage = PageType.MAIN;
    private WeakReference<Activity> currentActivityRef;

    public PageType getCurrentPage() {
        return currentPage;
    }

    public Activity getCurrentActivity() {
        if (currentActivityRef != null) {
            return currentActivityRef.get();
        }
        return null;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivityRef = new WeakReference<>(activity);
        String name = activity.getClass().getSimpleName().toLowerCase();

        if (name.contains("reminder")) {
            currentPage = PageType.REMINDER;
        } else if (name.contains("note")) {
            currentPage = PageType.NOTES;
        } else if (name.contains("health") || name.contains("medication") || name.contains("period") || name.contains("symptom") || name.contains("spotting")) {
            currentPage = PageType.HEALTH;
        } else if (name.contains("chat")) {
            currentPage = PageType.CHAT;
        } else if (name.contains("calendar")) {
            currentPage = PageType.CALENDAR;
        } else if (name.contains("main")) {
            currentPage = PageType.MAIN;
        } else {
            currentPage = PageType.OTHER;
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        // Keep the last activity known unless it's destroyed, or just leave it.
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}
    @Override
    public void onActivityStarted(@NonNull Activity activity) {}
    @Override
    public void onActivityStopped(@NonNull Activity activity) {}
    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (currentActivityRef != null && currentActivityRef.get() == activity) {
            currentActivityRef.clear();
        }
    }
}
