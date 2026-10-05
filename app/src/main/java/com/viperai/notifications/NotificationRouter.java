package com.viperai.notifications;

import android.app.Activity;
import android.content.Intent;
import com.rudra.myassistant.MainActivity;
import com.rudra.myassistant.ChatActivity;
import com.rudra.myassistant.RemindersActivity;
import com.rudra.myassistant.CalendarActivity;
import com.rudra.myassistant.ConnectActivity;

import static com.viperai.notifications.NotificationConstants.*;

public class NotificationRouter {

    public static void handleIntent(Activity activity, Intent intent) {
        if (intent == null) return;

        if (intent.hasExtra(EXTRA_REMINDER_ID)) {
            String id = intent.getStringExtra(EXTRA_REMINDER_ID);
            if (activity instanceof RemindersActivity) {
                ((RemindersActivity) activity).highlightReminder(id);
            } else if (activity instanceof com.rudra.myassistant.TodayRemindersActivity) {
                ((com.rudra.myassistant.TodayRemindersActivity) activity).highlightReminder(id);
            }
        }

        if (intent.hasExtra(EXTRA_EVENT_ID)) {
            String id = intent.getStringExtra(EXTRA_EVENT_ID);
            if (activity instanceof CalendarActivity) {
                ((CalendarActivity) activity).highlightEvent(id);
            }
        }

        if (intent.hasExtra(EXTRA_FRIEND_UID)) {
            String uid = intent.getStringExtra(EXTRA_FRIEND_UID);
            if (activity instanceof ConnectActivity) {
                ((ConnectActivity) activity).highlightFriend(uid);
            }
        }

        if (intent.hasExtra(EXTRA_HANGOUT_PROMPT)) {
            String prompt = intent.getStringExtra(EXTRA_HANGOUT_PROMPT);
            if (activity instanceof ChatActivity) {
                ((ChatActivity) activity).initiateHangoutPlanning(prompt);
            }
        }

        if (intent.getBooleanExtra(EXTRA_SCROLL_TO_SUGGESTIONS, false)) {
            if (activity instanceof MainActivity) {
                ((MainActivity) activity).scrollToSuggestionsSection();
            }
        }

        if (intent.hasExtra(EXTRA_EXPENSE_ID)) {
            String id = intent.getStringExtra(EXTRA_EXPENSE_ID);
            if (activity instanceof MainActivity) {
                ((MainActivity) activity).scrollToExpense(id);
            }
        }
    }
}
