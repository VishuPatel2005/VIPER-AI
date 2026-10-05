package com.rudra.myassistant;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ReminderDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "Reminders.db";

    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + ReminderContract.ReminderEntry.TABLE_NAME + " (" +
            ReminderContract.ReminderEntry._ID + " INTEGER PRIMARY KEY," +
            ReminderContract.ReminderEntry.COLUMN_NAME_TITLE + " TEXT," +
            ReminderContract.ReminderEntry.COLUMN_NAME_FLAGGED + " INTEGER," +
            ReminderContract.ReminderEntry.COLUMN_NAME_URGENT + " INTEGER," +
            ReminderContract.ReminderEntry.COLUMN_NAME_COMPLETED + " INTEGER," +
            ReminderContract.ReminderEntry.COLUMN_NAME_DATE_TIMESTAMP + " INTEGER," +
            ReminderContract.ReminderEntry.COLUMN_NAME_HOUR + " INTEGER," +
            ReminderContract.ReminderEntry.COLUMN_NAME_MINUTE + " INTEGER," +
            ReminderContract.ReminderEntry.COLUMN_NAME_LOCATION_NAME + " TEXT," +
            ReminderContract.ReminderEntry.COLUMN_NAME_LATITUDE + " REAL," +
            ReminderContract.ReminderEntry.COLUMN_NAME_LONGITUDE + " REAL," +
            ReminderContract.ReminderEntry.COLUMN_NAME_RADIUS + " REAL," +
            ReminderContract.ReminderEntry.COLUMN_NAME_ARRIVE + " INTEGER," +
            ReminderContract.ReminderEntry.COLUMN_NAME_DEPART + " INTEGER)";

    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + ReminderContract.ReminderEntry.TABLE_NAME;

    public ReminderDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + ReminderContract.ReminderEntry.TABLE_NAME + " ADD COLUMN " + ReminderContract.ReminderEntry.COLUMN_NAME_URGENT + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + ReminderContract.ReminderEntry.TABLE_NAME + " ADD COLUMN " + ReminderContract.ReminderEntry.COLUMN_NAME_COMPLETED + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + ReminderContract.ReminderEntry.TABLE_NAME + " ADD COLUMN " + ReminderContract.ReminderEntry.COLUMN_NAME_DATE_TIMESTAMP + " INTEGER");
            db.execSQL("ALTER TABLE " + ReminderContract.ReminderEntry.TABLE_NAME + " ADD COLUMN " + ReminderContract.ReminderEntry.COLUMN_NAME_HOUR + " INTEGER");
            db.execSQL("ALTER TABLE " + ReminderContract.ReminderEntry.TABLE_NAME + " ADD COLUMN " + ReminderContract.ReminderEntry.COLUMN_NAME_MINUTE + " INTEGER");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + ReminderContract.ReminderEntry.TABLE_NAME + " ADD COLUMN " + ReminderContract.ReminderEntry.COLUMN_NAME_LOCATION_NAME + " TEXT");
            db.execSQL("ALTER TABLE " + ReminderContract.ReminderEntry.TABLE_NAME + " ADD COLUMN " + ReminderContract.ReminderEntry.COLUMN_NAME_LATITUDE + " REAL");
            db.execSQL("ALTER TABLE " + ReminderContract.ReminderEntry.TABLE_NAME + " ADD COLUMN " + ReminderContract.ReminderEntry.COLUMN_NAME_LONGITUDE + " REAL");
            db.execSQL("ALTER TABLE " + ReminderContract.ReminderEntry.TABLE_NAME + " ADD COLUMN " + ReminderContract.ReminderEntry.COLUMN_NAME_RADIUS + " REAL DEFAULT 100");
            db.execSQL("ALTER TABLE " + ReminderContract.ReminderEntry.TABLE_NAME + " ADD COLUMN " + ReminderContract.ReminderEntry.COLUMN_NAME_ARRIVE + " INTEGER DEFAULT 1");
            db.execSQL("ALTER TABLE " + ReminderContract.ReminderEntry.TABLE_NAME + " ADD COLUMN " + ReminderContract.ReminderEntry.COLUMN_NAME_DEPART + " INTEGER DEFAULT 0");
        }
    }
}
