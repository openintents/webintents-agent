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
     
    public static final String AUTHORITY = "org.openintents.wiagent";
    
    // table 'intents'
    public static class WebIntents {
        
        public static final String TABLE_NAME = "web_intents";
        
        // columns
        public static final String ID = "_id";
        public static final String ACTION = "action";
        public static final String TYPE = "type";
        public static final String HREF = "href";
        public static final String TITLE = "title";
        public static final String DISPOSITION = "disposition";
        
        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
        
        public static final Uri CONTENT_URI_INMEMORY =
                Uri.parse("content://" + AUTHORITY + "/inmemory/" + TABLE_NAME);
        
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
    private static final int INTENTS_INMEMORY = 3;
    private static final int INTENTS_ID_INMEMORY = 4;
    private static final int WEB_ANDRIOD_MAP = 5;
    
    static {
        URI_MATCHER.addURI(AUTHORITY, WebIntents.TABLE_NAME, INTENTS);
        URI_MATCHER.addURI(AUTHORITY, WebIntents.TABLE_NAME + "/#", INTENTS_ID);
        URI_MATCHER.addURI(AUTHORITY, "inmemory/" + WebIntents.TABLE_NAME, INTENTS_INMEMORY);
        URI_MATCHER.addURI(AUTHORITY, "inmemory/" + WebIntents.TABLE_NAME + "/#", INTENTS_ID_INMEMORY);
        URI_MATCHER.addURI(AUTHORITY, WebAndroidMap.TABLE_NAME, WEB_ANDRIOD_MAP);
    }
    
    private WebIntentsDatabaseInFile mWebIntentsDatabaseInFileOpenHelper;
    private WebIntentsDatabaseInMemory mWebIntentsDatabaseInMemoryOpenHelper;

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
        SQLiteDatabase db = null;
        switch (URI_MATCHER.match(uri)) {
        case INTENTS:
            db = mWebIntentsDatabaseInFileOpenHelper.getWritableDatabase();
            break;
            
        case INTENTS_INMEMORY:
            db = mWebIntentsDatabaseInMemoryOpenHelper.getWritableDatabase();

        default:
            break;
        }
        if (db != null) {
            long rowID;
            rowID = db.insert(WebIntents.TABLE_NAME, null, values);
            if (rowID > 0) {
                return ContentUris.withAppendedId(WebIntents.CONTENT_URI, rowID);
            }
        }
        
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public boolean onCreate() {
        mWebIntentsDatabaseInFileOpenHelper = new WebIntentsDatabaseInFile(getContext());
        mWebIntentsDatabaseInMemoryOpenHelper = new WebIntentsDatabaseInMemory(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
        SQLiteDatabase db = null;
        
        switch (URI_MATCHER.match(uri)) {
        case INTENTS:
            db = mWebIntentsDatabaseInFileOpenHelper.getReadableDatabase();
            qBuilder.setTables(WebIntents.TABLE_NAME);
            break;
            
        case INTENTS_ID:
            db = mWebIntentsDatabaseInFileOpenHelper.getReadableDatabase();
            qBuilder.setTables(WebIntents.TABLE_NAME);
            qBuilder.appendWhere(WebIntents.ID + "=" + uri.getPathSegments().get(1));
            break;
            
        case INTENTS_INMEMORY:
            db = mWebIntentsDatabaseInMemoryOpenHelper.getReadableDatabase();
            qBuilder.setTables(WebIntents.TABLE_NAME);
            break;
            
        case WEB_ANDRIOD_MAP:
            db = mWebIntentsDatabaseInFileOpenHelper.getReadableDatabase();
            qBuilder.setTables(WebAndroidMap.TABLE_NAME);
            break;

        default:
            break;
        }
         
        String qs = qBuilder.buildQuery(projection, selection, null, null, sortOrder, null);
        Cursor c = qBuilder.query(db, projection, selection, selectionArgs, 
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
