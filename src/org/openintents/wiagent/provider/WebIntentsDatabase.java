package org.openintents.wiagent.provider;

import org.openintents.wiagent.provider.WebIntentsProvider.LocalServiceDomain;
import org.openintents.wiagent.provider.WebIntentsProvider.WebIntents;
import org.openintents.wiagent.provider.WebIntentsProvider.WebAndroidMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class WebIntentsDatabase extends SQLiteOpenHelper {

	/**
	 * The file name of the database for this class
	 */
	public static final String DATABASE_NAME = "webintents.db";

	public static final int DATABASE_VERSION = 1;

	public WebIntentsDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {        
		String sql;
		sql = "CREATE TABLE " + WebIntents.TABLE_NAME + " (" +
				WebIntents._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				WebIntents.ACTION + " TEXT NOT NULL, " +
				WebIntents.TYPE + " TEXT NOT NULL, " +
				WebIntents.HREF + " TEXT NOT NULL, " +
				WebIntents.TITLE + " TEXT NOT NULL, " +
				WebIntents.DISPOSITION + " TEXT NOT NULL, " +
				WebIntents.BOOKMARKED + " DEFAULT '0' NOT NULL, " +
				WebIntents.REMOVED + " DEFAULT '0' NOT NULL" +
				");";
		db.execSQL(sql);

		ContentValues values = new ContentValues();

		sql = "CREATE TABLE " + LocalServiceDomain.TABLE_NAME + " (" +
				LocalServiceDomain._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				LocalServiceDomain.WEB_HERF + " TEXT NOT NULL, " +
				LocalServiceDomain.WEB_DOMAIN + " TEXT NOT NULL" +
				");";
		db.execSQL(sql);

		// Insert domains for local services
		values.clear();
		values.put(LocalServiceDomain.WEB_HERF, "file:///android_asset/www/service/shorten_with_Goo.gl.html");
		values.put(LocalServiceDomain.WEB_DOMAIN, "http://demos.webintents.org/");
		db.insert(LocalServiceDomain.TABLE_NAME, null, values);

		values.clear();
		values.put(LocalServiceDomain.WEB_HERF, "file:///android_asset/www/service/chromerly_URL-shortener/2.1_0/intent/intent.html");
		values.put(LocalServiceDomain.WEB_DOMAIN, "http://urly.fi/");
		db.insert(LocalServiceDomain.TABLE_NAME, null, values);

		sql = "CREATE TABLE " + WebAndroidMap.TABLE_NAME + " (" +
				WebAndroidMap._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				WebAndroidMap.WEB_ACTION + " TEXT NOT NULL, " +
				WebAndroidMap.ANDROID_ACTION + " TEXT NOT NULL, " +
				WebAndroidMap.DATA_TYPE + " TEXT NOT NULL, " +
				WebAndroidMap.DATA_MAP_SCHEME + " TEXT NOT NULL" +
				");";
		db.execSQL(sql);

		values.clear();

		values.put(WebAndroidMap.WEB_ACTION, "http://webintents.org/share");
		values.put(WebAndroidMap.ANDROID_ACTION, android.content.Intent.ACTION_SEND);
		values.put(WebAndroidMap.DATA_TYPE, "text/uri-list");
		values.put(WebAndroidMap.DATA_MAP_SCHEME, android.content.Intent.EXTRA_TEXT);

		db.insert(WebAndroidMap.TABLE_NAME, null, values);       
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub       
	}

}
