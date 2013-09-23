package com.tzapps.tzpalette.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.tzapps.tzpalette.db.PaletteDataContract.PaletteDataEntry;

public class PaletteDataDbHelper extends SQLiteOpenHelper
{
    private static final String TAG = "PaletteDataDbHelper";
    
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "TzPalette.db";
    
    private static final String TEXT_TYPE    = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String BLOB_TYPE    = " BLOB";
    private static final String COMMA_SEP    = ",";
    
    private static String SQL_CREATE_ENTRIES =
            "CREATE TABLE" + PaletteDataEntry.TABLE_NAME + " (" +
            PaletteDataEntry._ID + " INTEGER PRIMARY KEY," +
            PaletteDataEntry.COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP +
            PaletteDataEntry.COLUMN_NAME_COLORS + TEXT_TYPE + COMMA_SEP +
            PaletteDataEntry.COLUMN_NAME_UPDATED + INTEGER_TYPE + COMMA_SEP +
            PaletteDataEntry.COLUMN_NAME_THUMB + BLOB_TYPE + 
            ")";
    
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + PaletteDataEntry.TABLE_NAME;
    
    public PaletteDataDbHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " 
                   + newVersion + ", which will destroy all old data");
        // The upgrade policy is to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    
    public void onDownGrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        onUpgrade(db, oldVersion, newVersion);
    }
    

}