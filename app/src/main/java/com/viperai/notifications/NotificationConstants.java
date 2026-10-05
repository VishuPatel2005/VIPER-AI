package com.viperai.notifications;

public class NotificationConstants {
    // Channel IDs
    public static final String CHANNEL_REMINDERS = "channel_reminders";
    public static final String CHANNEL_EVENTS = "channel_events";
    public static final String CHANNEL_SUGGESTIONS = "channel_suggestions";
    public static final String CHANNEL_FRIENDS = "channel_friends";
    public static final String CHANNEL_HANGOUT = "channel_hangout";
    public static final String CHANNEL_EXPENSES = "channel_expenses";
    public static final String CHANNEL_NUDGE = "channel_nudge";

    // Notification IDs
    public static final int NOTIF_ID_REMINDER = 1001;
    public static final int NOTIF_ID_EVENT = 1002;
    public static final int NOTIF_ID_SUGGESTION = 1003;
    public static final int NOTIF_ID_FRIEND = 1004;
    public static final int NOTIF_ID_HANGOUT = 1005;
    public static final int NOTIF_ID_EXPENSE = 1006;
    public static final int NOTIF_ID_NUDGE = 1007;

    // Intent extras
    public static final String EXTRA_REMINDER_ID = "reminder_id";
    public static final String EXTRA_EVENT_ID = "event_id";
    public static final String EXTRA_FRIEND_UID = "friend_uid";
    public static final String EXTRA_FRIEND_NAME = "friend_name";
    public static final String EXTRA_EXPENSE_ID = "expense_id";
    public static final String EXTRA_HANGOUT_PROMPT = "hangout_prompt";
    public static final String EXTRA_SCROLL_TO_SUGGESTIONS = "scroll_to_suggestions";

    // Worker tags
    public static final String TAG_SUGGESTION_WORKER = "SuggestionWorker";
    public static final String TAG_FRIEND_WORKER = "FriendProximityWorker";
    public static final String TAG_EXPENSE_WORKER = "ExpenseWorker";
    public static final String TAG_EVENT_WORKER = "EventWorker";
    public static final String TAG_REMINDER_WORKER = "ReminderWorker";
    public static final String TAG_HANGOUT_WORKER = "HangoutWorker";

    // SharedPreferences keys for throttle timestamps
    public static final String PREF_LAST_SUGGESTION_NOTIF = "last_suggestion_notif_time";
    public static final String PREF_LAST_FRIEND_NOTIF = "last_friend_notif_time";
    public static final String PREF_LAST_EXPENSE_NOTIF = "last_expense_notif_time";
    public static final String PREF_LAST_NUDGE_NOTIF = "last_nudge_notif_time";

    // SharedPreferences keys for user toggles
    public static final String PREF_NOTIF_REMINDERS = "notif_reminders_enabled";
    public static final String PREF_NOTIF_EVENTS = "notif_events_enabled";
    public static final String PREF_NOTIF_SUGGESTIONS = "notif_suggestions_enabled";
    public static final String PREF_NOTIF_FRIENDS = "notif_friends_enabled";
    public static final String PREF_NOTIF_HANGOUT = "notif_hangout_enabled";
    public static final String PREF_NOTIF_EXPENSES = "notif_expenses_enabled";

    // SharedPrefs File Name
    public static final String PREFS_NAME = "viper_notif_prefs";
}
