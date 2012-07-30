package org.openintents.wiagent;

import java.util.ArrayList;

import org.openintents.wiagent.provider.WebIntentsProvider.WebAndroidMap;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.AsyncTask;

public class WebIntentsHelper {
    
    private Context mContext;
    private Intent mIntent;
    
    public WebIntentsHelper(Context mContext) {
        this.mContext = mContext;
    }
    
    public void createChooserWithWebActivities(Intent intent) {
        mIntent = intent;
        
        AsyncTask<String, Void, Cursor> queryWeb = new AsyncTask<String, Void, Cursor>(){

            @Override
            protected Cursor doInBackground(String... params) {
                String[] projection = {
                        "web_action",
                        "android_data"
                };
                String selection = "android_action = ? and data_type = ?";
                String[] selectionArgs = {
                        params[0],
                        params[1]
                };
                
                ContentResolver cr = mContext.getContentResolver();
                Uri uri = Uri.parse("content://org.openintents.wiagent/web_android_map");
                
                Cursor cursor = cr.query(uri, projection, selection, selectionArgs, null);
                
                return cursor;
            }

            @Override
            protected void onPostExecute(Cursor cursor) {
                Intent intent = new Intent();
                intent.putExtra("android_action", mIntent.getAction());
                intent.putExtra("type", mIntent.getType());
                intent.setComponent(new ComponentName("org.openintents.wiagent", "org.openintents.wiagent.WebIntentsHelperActivity"));
                
                ArrayList<String> webActionList = new ArrayList<String>();
                while (cursor.moveToNext()) {
                    webActionList.add(cursor.getString(cursor.getColumnIndex("web_action")));
                    intent.putExtra("data", mIntent.getStringExtra(
                            cursor.getString(cursor.getColumnIndex("android_data"))));
                }
                intent.putExtra("web_actions", webActionList);
                
                mContext.startActivity(intent);
            }
        };
        
        queryWeb.execute(intent.getAction(), intent.getType());
    }
}
