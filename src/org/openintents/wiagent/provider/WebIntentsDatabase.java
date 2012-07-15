package org.openintents.wiagent.provider;

import org.openintents.wiagent.provider.WebIntentsProvider.WebIntents;
import org.openintents.wiagent.provider.WebIntentsProvider.WebAndroidMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class WebIntentsDatabase extends SQLiteOpenHelper {
    
    public static final String DATABASE_NAME = "webintents.db";
    
    public static final int DATABASE_VERSION = 1;

    public WebIntentsDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql;
        sql = "CREATE TABLE " + WebIntents.TABLE_NAME + " (" +
                WebIntents.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                WebIntents.ACTION + " TEXT NOT NULL, " +
                WebIntents.TYPE + " TEXT NOT NULL, " +
                WebIntents.HREF + " TEXT NOT NULL, " +
                WebIntents.TITLE + " TEXT NOT NULL, " +
                WebIntents.DISPOSITION + " TEXT NOT NULL, " +
                WebIntents.BOOKMARKED + " DEFAULT '0' NOT NULL" +
                ");";
        db.execSQL(sql);
        
        ContentValues values = new ContentValues();
//        values.put(WebIntents.ACTION, "http://webintents.org/share");
//        values.put(WebIntents.TYPE, "text/uri-list");
//        values.put(WebIntents.HREF, "file:///android_asset/www/service/twitter_text_share.html");
//        values.put(WebIntents.TITLE, "Share Link to Twitter");
//        values.put(WebIntents.DISPOSITION, "inline"); 
//        values.put(WebIntents.BOOKMARKED, "1");
//        db.insert(WebIntents.TABLE_NAME, null, values);
        
        sql = "CREATE TABLE " + WebAndroidMap.TABLE_NAME + " (" +
                WebAndroidMap.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                WebAndroidMap.WEB_ACTION + " TEXT NOT NULL, " +
                WebAndroidMap.ANDROID_ACTION + " TEXT NOT NULL" +
                ");";
        
        db.execSQL(sql);
        
        values.clear();
        values.put(WebAndroidMap.WEB_ACTION, "http://webintents.org/share");
        values.put(WebAndroidMap.ANDROID_ACTION, "android.intent.action.SEND");
        db.insert(WebAndroidMap.TABLE_NAME, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub       
    }

}
