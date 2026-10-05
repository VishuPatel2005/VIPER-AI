package com.rudra.myassistant;

import android.provider.BaseColumns;

public final class ReminderContract {

    private ReminderContract() {}

    public static class ReminderEntry implements BaseColumns {
        public static final String TABLE_NAME = "reminders";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_FLAGGED = "flagged";
        public static final String COLUMN_NAME_URGENT = "urgent";
        public static final String COLUMN_NAME_COMPLETED = "completed";
        public static final String COLUMN_NAME_DATE_TIMESTAMP = "date_timestamp";
        public static final String COLUMN_NAME_HOUR = "hour";
        public static final String COLUMN_NAME_MINUTE = "minute";
        public static final String COLUMN_NAME_LOCATION_NAME = "location_name";
        public static final String COLUMN_NAME_LATITUDE = "latitude";
        public static final String COLUMN_NAME_LONGITUDE = "longitude";
        public static final String COLUMN_NAME_RADIUS = "radius";
        public static final String COLUMN_NAME_ARRIVE = "remind_on_arrival";
        public static final String COLUMN_NAME_DEPART = "remind_on_departure";
    }
}
