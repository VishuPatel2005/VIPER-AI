package com.rudra.myassistant;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class MedicationDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "medications.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_MEDICATIONS = "medications";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_DOSAGE = "dosage";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_DAYS = "days";
    public static final String COLUMN_IS_TAKEN = "is_taken";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_MEDICATIONS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT, " +
                    COLUMN_DOSAGE + " TEXT, " +
                    COLUMN_TIME + " TEXT, " +
                    COLUMN_DAYS + " TEXT, " +
                    COLUMN_IS_TAKEN + " INTEGER DEFAULT 0" +
                    ");";

    public MedicationDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEDICATIONS);
        onCreate(db);
    }

    public long addMedication(Medication med) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, med.getName());
        values.put(COLUMN_DOSAGE, med.getDosage());
        values.put(COLUMN_TIME, med.getTime());
        values.put(COLUMN_DAYS, med.getDays());
        values.put(COLUMN_IS_TAKEN, med.isTaken() ? 1 : 0);
        return db.insert(TABLE_MEDICATIONS, null, values);
    }

    public List<Medication> getAllMedications() {
        List<Medication> meds = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MEDICATIONS, null, null, null, null, null, COLUMN_TIME + " ASC");

        if (cursor.moveToFirst()) {
            do {
                Medication med = new Medication();
                med.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                med.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
                med.setDosage(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DOSAGE)));
                med.setTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME)));
                med.setDays(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DAYS)));
                med.setTaken(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_TAKEN)) == 1);
                meds.add(med);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return meds;
    }

    public void updateMedicationStatus(long id, boolean isTaken) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_TAKEN, isTaken ? 1 : 0);
        db.update(TABLE_MEDICATIONS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void deleteMedication(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MEDICATIONS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public Medication getMedication(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MEDICATIONS, null, COLUMN_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            Medication med = new Medication();
            med.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            med.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
            med.setDosage(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DOSAGE)));
            med.setTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME)));
            med.setDays(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DAYS)));
            med.setTaken(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_TAKEN)) == 1);
            cursor.close();
            return med;
        }
        return null;
    }

    public int updateMedication(Medication med) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, med.getName());
        values.put(COLUMN_DOSAGE, med.getDosage());
        values.put(COLUMN_TIME, med.getTime());
        values.put(COLUMN_DAYS, med.getDays());
        return db.update(TABLE_MEDICATIONS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(med.getId())});
    }
}
