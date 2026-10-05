package com.rudra.myassistant;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class ChatDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "chats.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_CHATS = "chats";
    public static final String COLUMN_CHAT_ID = "chat_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_LAST_MESSAGE = "last_message";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_CHATS + " (" +
                    COLUMN_CHAT_ID + " TEXT PRIMARY KEY, " +
                    COLUMN_TITLE + " TEXT, " +
                    COLUMN_LAST_MESSAGE + " TEXT, " +
                    COLUMN_TIMESTAMP + " INTEGER" +
                    ");";

    public ChatDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHATS);
        onCreate(db);
    }

    public void saveChat(ChatHistory chat) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CHAT_ID, chat.getChatId());
        values.put(COLUMN_TITLE, chat.getTitle());
        values.put(COLUMN_LAST_MESSAGE, chat.getLastMessage());
        values.put(COLUMN_TIMESTAMP, chat.getTimestamp());
        db.replace(TABLE_CHATS, null, values);
    }

    public List<ChatHistory> getAllChats() {
        List<ChatHistory> chats = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CHATS, null, null, null, null, null, COLUMN_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHAT_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                String last = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_MESSAGE));
                long ts = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
                chats.add(new ChatHistory(id, title, last, ts));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return chats;
    }

    public void clearAllChats() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CHATS, null, null);
    }

    public void deleteOldChats(long thresholdTimestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CHATS, COLUMN_TIMESTAMP + " < ?", new String[]{String.valueOf(thresholdTimestamp)});
    }

    public void deleteChat(String chatId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CHATS, COLUMN_CHAT_ID + " = ?", new String[]{chatId});
    }
}
