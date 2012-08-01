package org.openintents.wiagent.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class WebIntentsProvider extends ContentProvider {
     
    public static final String AUTHORITY = "org.openintents.wiagent";
    
    // table 'intents'
    public static class WebIntents {
        
        public static final String TABLE_NAME = "web_intents";
        
        // columns
        public static final String _ID = "_id";
        public static final String ACTION = "action";
        public static final String TYPE = "type";
        public static final String HREF = "href";
        public static final String TITLE = "title";
        public static final String DISPOSITION = "disposition";
        /**
         * Indicate if the entry is bookmarked. 1 means yes, 0 no.
         */
        public static final String BOOKMARKED = "bookmarked";
                                                                        
        
        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
    }
    
    // table 'web_android_map'
    public static class WebAndroidMap {
        
        public static final String TABLE_NAME = "web_android_map";
        
        // columns
        public static final String _ID = "_id";
        public static final String WEB_ACTION = "web_action";
        public static final String ANDROID_ACTION ="android_action";
        
        public static final String DATA_TYPE = "data_type";
        
        /**
         * For different android actions, data may be put in different fields.
         * This column keeps such information which can be used to map data from web intents
         * to android intents and vice versa.
         */
        public static final String ANDROID_DATA = "android_data";
        
        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
    }    
    
    // table 'local_service_domain', used for solve cross-domain issues caused by
    // service in 'assets/www/service'
    public static class LocalServiceDomain {
        
        public static final String TABLE_NAME = "local_service_domain";
        
        // columns
        public static final String _ID = "_id";
        public static final String WEB_HERF = "web_href";
        public static final String WEB_DOMAIN = "domain";
        
        public static final Uri CONTENT_URI =
                Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);
    }
    
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    
    // Uri code
    private static final int WEB_INTENTS = 1;
    private static final int WEB_INTENTS_ID = 2;
    private static final int WEB_ANDRIOD_MAP = 3;
    private static final int LOCAL_SERVIC_DOMAIN = 4;
    
    static {
        URI_MATCHER.addURI(AUTHORITY, WebIntents.TABLE_NAME, WEB_INTENTS);
        URI_MATCHER.addURI(AUTHORITY, WebIntents.TABLE_NAME + "/#", WEB_INTENTS_ID);
        URI_MATCHER.addURI(AUTHORITY, WebAndroidMap.TABLE_NAME, WEB_ANDRIOD_MAP);
        URI_MATCHER.addURI(AUTHORITY, LocalServiceDomain.TABLE_NAME, LOCAL_SERVIC_DOMAIN);
    }
    
    private WebIntentsDatabase mWebIntentsDatabaseOpenHelper;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mWebIntentsDatabaseOpenHelper.getWritableDatabase();
        
        int count = 0;
        
        switch (URI_MATCHER.match(uri)) {
        case WEB_INTENTS:
            count = db.delete(WebIntents.TABLE_NAME, 
                    selection, selectionArgs);
            break;

        default:
            break;
        }
        
        return count;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        SQLiteDatabase db = mWebIntentsDatabaseOpenHelper.getWritableDatabase();
        long rowID = db.insert(WebIntents.TABLE_NAME, null, values);
        
        if (rowID > 0) {
            return ContentUris.withAppendedId(WebIntents.CONTENT_URI, rowID);
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public boolean onCreate() {
        mWebIntentsDatabaseOpenHelper = new WebIntentsDatabase(getContext());
        
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
        SQLiteDatabase db = mWebIntentsDatabaseOpenHelper.getReadableDatabase();
        
        switch (URI_MATCHER.match(uri)) {
        case WEB_INTENTS:
            qBuilder.setTables(WebIntents.TABLE_NAME);
            break;
            
        case WEB_INTENTS_ID:
            qBuilder.setTables(WebIntents.TABLE_NAME);
            qBuilder.appendWhere(WebIntents._ID + "=" + uri.getPathSegments().get(1));
            break;
            
        case WEB_ANDRIOD_MAP:
            qBuilder.setTables(WebAndroidMap.TABLE_NAME);
            break;
            
        case LOCAL_SERVIC_DOMAIN:
            qBuilder.setTables(LocalServiceDomain.TABLE_NAME);
            break;

        default:
            break;
        }
        
        Cursor c = qBuilder.query(db, projection, selection, selectionArgs, 
                null, null, sortOrder);
        
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        SQLiteDatabase db = mWebIntentsDatabaseOpenHelper.getWritableDatabase();
        
        int count = 0;
        
        switch (URI_MATCHER.match(uri)) {
        case WEB_INTENTS:
            count = db.update(WebIntents.TABLE_NAME, values, selection, selectionArgs);
            break;

        default:
            break;
        }
        
        return count;
    }
}
