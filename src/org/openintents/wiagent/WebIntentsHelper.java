package org.openintents.wiagent;

import java.util.ArrayList;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

/**
 * This is a helper class for external Android apps to invoke Web apps through
 * Web Intents.
 * @author Cheng Zheng
 *
 */
public class WebIntentsHelper {
    
    private Context mContext;
    
    public WebIntentsHelper(Context mContext) {
        this.mContext = mContext;
    }
    
    /**
     * The external Android app can call this method to invoke Web Intents
     * @param intent A Android intent
     */
    public void createChooserWithWebActivities(Intent intent) {
        final Intent oldIntent = intent;        
        
        AsyncTask<String, Void, Intent> queryWeb = new AsyncTask<String, Void, Intent>(){

            @Override
            protected Intent doInBackground(String... params) {
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
                
                Intent newIntent = new Intent();
                newIntent.putExtra("android_action", oldIntent.getAction());
                newIntent.putExtra("type", oldIntent.getType());
                newIntent.setComponent(new ComponentName("org.openintents.wiagent", "org.openintents.wiagent.WebIntentsHelperActivity"));
                
                ArrayList<String> webActionList = new ArrayList<String>();
                while (cursor.moveToNext()) {
                    webActionList.add(cursor.getString(cursor.getColumnIndex("web_action")));
                    newIntent.putExtra("data", oldIntent.getStringExtra(
                            cursor.getString(cursor.getColumnIndex("android_data"))));
                }
                
                cursor.close();
                
                newIntent.putExtra("web_actions", webActionList);
                
                return newIntent;
            }

            @Override
            protected void onPostExecute(Intent newIntent) {
                mContext.startActivity(newIntent);
            }
        };
        
        queryWeb.execute(intent.getAction(), intent.getType());
    }
}
