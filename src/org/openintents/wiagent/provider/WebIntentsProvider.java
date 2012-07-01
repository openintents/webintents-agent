package org.openintents.wiagent.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class WebIntentsProvider extends ContentProvider {
    
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "intents.db";    
    public static final String AUTHORITY = "WebIntentsProvider";
    
    // table 'intents'
    public static class Intents {
        
        public static final String TABLE_NAME = "intents";
        
        // columns
        public static final String ID = "_id";
        public static final String ACTION = "action";
        public static final String TYPE = "type";
        public static final String HREF = "href";
        public static final String TITLE = "title";
        public static final String DISPOSITION = "disposition";
        
        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
        
    }
    
    // table 'web_android_map'
    public static class WebAndroidMap {
        
        public static final String TABLE_NAME = "web_android_map";
        
        // columns
        public static final String ID = "_id";
        public static final String WEB_ACTION = "web_action";
        public static final String ANDROID_ACTION ="android_action";
        
        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
        
    }    
    
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    
    // Uri code
    private static final int INTENTS = 1;
    private static final int INTENTS_ID = 2;
    private static final int WEB_ANDRIOD_MAP = 3;
    
    static {
        URI_MATCHER.addURI(AUTHORITY, Intents.TABLE_NAME, INTENTS);
        URI_MATCHER.addURI(AUTHORITY, Intents.TABLE_NAME + "/#", INTENTS_ID);
        URI_MATCHER.addURI(AUTHORITY, WebAndroidMap.TABLE_NAME, WEB_ANDRIOD_MAP);
    }
    
    private static class DatabaseOpenHelper extends SQLiteOpenHelper {

        public DatabaseOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String sql;
            sql = "CREATE TABLE " + Intents.TABLE_NAME + " (" +
                    Intents.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    Intents.ACTION + " TEXT NOT NULL, " +
                    Intents.TYPE + " TEXT NOT NULL, " +
                    Intents.HREF + " TEXT NOT NULL, " +
                    Intents.TITLE + " TEXT NOT NULL, " +
                    Intents.DISPOSITION + " TEXT NOT NULL" +
                    ");";
            db.execSQL(sql);
            
            ContentValues values = new ContentValues();
            values.put(Intents.ACTION, "http://webintents.org/share");
            values.put(Intents.TYPE, "text/uri-list");
            values.put(Intents.HREF, "file:///android_asset/www/service/twitter_text_share.html");
            values.put(Intents.TITLE, "Share Link to Twitter");
            values.put(Intents.DISPOSITION, "inline");
            db.insert(Intents.TABLE_NAME, null, values);
            
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
            db.execSQL("DROP TABLE IF EXISTS " + Intents.TABLE_NAME);
        }
        
    }
    
    private DatabaseOpenHelper dbOpenHelper;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
        long rowID;
        rowID = db.insert(Intents.TABLE_NAME, null, values);
        if (rowID > 0) {
            return ContentUris.withAppendedId(Intents.CONTENT_URI, rowID);
        }
        db.close();
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public boolean onCreate() {
        dbOpenHelper = new DatabaseOpenHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (URI_MATCHER.match(uri)) {
        case INTENTS:
            qb.setTables(Intents.TABLE_NAME);
            break;
            
        case INTENTS_ID:
            qb.setTables(Intents.TABLE_NAME);
            qb.appendWhere(Intents.ID + "=" + uri.getPathSegments().get(1));
            break;
            
        case WEB_ANDRIOD_MAP:
            qb.setTables(WebAndroidMap.TABLE_NAME);
            break;

        default:
            break;
        }
        
        SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
        String qs = qb.buildQuery(projection, selection, null, null, sortOrder, null);
        Cursor c = qb.query(db, projection, selection, selectionArgs, 
                null, null, sortOrder);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

}
