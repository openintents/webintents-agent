package org.openintents.wiagent.provider;

import org.openintents.wiagent.provider.WebIntentsProvider.WebIntents;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class WebIntentsDatabaseInMemory extends SQLiteOpenHelper {
    
    public static final int DATABASE_VERSION = 1;
    
    public WebIntentsDatabaseInMemory(Context context) {
        super(context, null, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        String sql;
        sql = "CREATE TABLE " + WebIntents.TABLE_NAME + " (" +
                WebIntents.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                WebIntents.ACTION + " TEXT NOT NULL, " +
                WebIntents.TYPE + " TEXT NOT NULL, " +
                WebIntents.HREF + " TEXT NOT NULL, " +
                WebIntents.TITLE + " TEXT NOT NULL, " +
                WebIntents.DISPOSITION + " TEXT NOT NULL" +
                ");";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub

    }

}
