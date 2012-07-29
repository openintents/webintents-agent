package org.openintents.wiagent;

import java.util.ArrayList;
import java.util.List;

import org.openintents.wiagent.provider.WebIntentsProvider;
import org.openintents.wiagent.ui.WebIntentsAgentActivity;
import org.openintents.wiagent.ui.widget.AndroidAppArrayAdapter;
import org.openintents.wiagent.ui.widget.WebAppArrayAdapter;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ListView;

public class WebIntentsHelperActivity extends Activity {
    
    private String mAction;
    private String mType;
    private String mData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        final Dialog d = new Dialog(this);
        
        Intent intent = getIntent();
        
        mAction = intent.getStringExtra("action");
        mType = intent.getStringExtra("type");
        mData = intent.getStringExtra("data");
        
        AsyncTask<String, Void, ArrayList<WebApp>> webActionQuery = new AsyncTask<String, Void, ArrayList<WebApp>>() {

            @Override
            protected ArrayList<WebApp> doInBackground(String... params) {
                ContentResolver cr = getContentResolver();
                
                String[] projection = {
                        WebIntentsProvider.WebAndroidMap.WEB_ACTION
                };
                String selection = WebIntentsProvider.WebAndroidMap.ANDROID_ACTION + " = ?";
                String[] selectionArgs = {
                        params[0]
                };
                
                Cursor cursor = cr.query(WebIntentsProvider.WebAndroidMap.CONTENT_URI, projection, selection, selectionArgs, null);
                
                ArrayList<WebApp> webAppList = new ArrayList<WebApp>();
                ArrayList<String> webAppHrefList = new ArrayList<String>();
                
                while (cursor.moveToNext()) {
                    String webAction = cursor.getString(cursor.getColumnIndex(WebIntentsProvider.WebAndroidMap.WEB_ACTION));
                    
                    projection = new String[3];
                    projection[0] = WebIntentsProvider.WebIntents.ID;
                    projection[1] = WebIntentsProvider.WebIntents.HREF;
                    projection[2] = WebIntentsProvider.WebIntents.TITLE;
                    selection = WebIntentsProvider.WebIntents.ACTION + " = ?";
                    selectionArgs = new String[1];
                    selectionArgs[0] = webAction;
                    
                    Cursor cursor1 = cr.query(WebIntentsProvider.WebIntents.CONTENT_URI, 
                            projection, selection, selectionArgs, null);
                    
                    while (cursor1.moveToNext()) {
                        String href = cursor1.getString(cursor1.getColumnIndex(WebIntentsProvider.WebIntents.HREF));
                        String title = cursor1.getString(cursor1.getColumnIndex(WebIntentsProvider.WebIntents.TITLE));
                        
                        if (!webAppHrefList.contains(title)) {
                            webAppHrefList.add(title);
                            webAppList.add(new WebApp(title, href));
                        }
                    }
                }
                
                return webAppList;
            }

            @Override
            protected void onPostExecute(ArrayList<WebApp> webAppList) {
                d.setContentView(R.layout.dialog_suggested_apps);
                
                ListView webAppListView = (ListView) d.findViewById(R.id.web_app);
                final WebAppArrayAdapter webAppArrayAdapter = new WebAppArrayAdapter(WebIntentsHelperActivity.this, webAppList);
                webAppListView.setAdapter(webAppArrayAdapter);
                
                webAppListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                            int position, long id) {
                        d.dismiss();
                        
                        WebApp webApp = webAppArrayAdapter.getItem(position);
                        Intent intent = new Intent(getApplication(), WebIntentsAgentActivity.class);
                        intent.putExtra("href", webApp.href);
                        intent.putExtra("action", mAction);
                        intent.putExtra("type", mType);
                        intent.putExtra("data", mData);
                        
                        startActivity(intent);
                    }
                });
                
                ListView androidAppListView = (ListView) d.findViewById(R.id.android_app);

                Intent intent = new Intent(mAction);
                intent.setType(mType);
                List<ResolveInfo> androidAppList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY); 
                
                AndroidAppArrayAdapter androidAppArrayAdapter = new AndroidAppArrayAdapter(WebIntentsHelperActivity.this, androidAppList);
                
                androidAppListView.setAdapter(androidAppArrayAdapter);
                
                androidAppListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {  
                        d.dismiss();
                        Adapter adapter = (AndroidAppArrayAdapter) parent.getAdapter();
                        ResolveInfo ri = (ResolveInfo) adapter.getItem(position);
                        Intent intent = new Intent();
                        intent.setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
                        intent.putExtra(Intent.EXTRA_TEXT, mData);
                        startActivity(intent);
                    }
                });
                
                d.setTitle("Suggested Applications");
                d.show();
            }          
        };
        
        webActionQuery.execute(mAction);
    }
}
