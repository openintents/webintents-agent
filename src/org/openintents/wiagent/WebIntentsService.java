package org.openintents.wiagent;

import java.util.ArrayList;
import java.util.List;

import org.openintents.wiagent.provider.WebIntentsProvider;
import org.openintents.wiagent.ui.WebIntentsAgentActivity;

import org.openintents.wiagent.ui.widget.AndroidAppArrayAdapter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Message;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class WebIntentsService {
    
//    private Context mContext;
//    
//    public WebIntentsService(Context mContext) {        
//        this.mContext = mContext;
//    }
//
//    public String doWork (String action, String type) {
//        
//        Dialog d = new Dialog(mContext);
//        d.setContentView(R.layout.dialog_suggested_apps);        
//        d.setTitle("Suggested Applications");
//        
//        ListView webAppListView = (ListView) d.findViewById(R.id.web_app);
//        ListView androidAppListView = (ListView) d.findViewById(R.id.android_app);            
//        
//        String[] projectionWebIntents = {
//                WebIntentsProvider.Intents.ID,    
//                WebIntentsProvider.Intents.TITLE,
//                WebIntentsProvider.Intents.HREF
//        };
//
//        String selectionWebIntents = WebIntentsProvider.Intents.ACTION + " = ?";
//        String[] selectionArgsWebIntents = { action };
//
//        Cursor cursorWebIntents = mContext.getContentResolver().query(WebIntentsProvider.Intents.CONTENT_URI, 
//                projectionWebIntents, selectionWebIntents, selectionArgsWebIntents, null);            
//
//        String[] columnWebIntents = {
//                WebIntentsProvider.Intents.TITLE,
//                WebIntentsProvider.Intents.HREF
//        };
//
//        int[] mViewIDs = {
//                android.R.id.text1,
//                android.R.id.text2
//        };
//        
//        webAppListView.setAdapter(new SimpleCursorAdapter(mContext, 
//                android.R.layout.simple_list_item_2, cursorWebIntents, columnWebIntents, mViewIDs, 0));
//       
//        webAppListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position,
//                    long id) {                    
////                Cursor cursor = ((SimpleCursorAdapter) parent.getAdapter()).getCursor();
////                cursor.moveToPosition(position);
////                String href = cursor.getString(2);
////                WebIntentsDismissMessage obj = new WebIntentsDismissMessage();
////                obj.intent = finalIntent;
////                obj.href = href;
////                mOnDismissMessage = Message.obtain();
////                mOnDismissMessage.obj = obj;
////                d.dismiss();
//            }
//
//        });
//        
//        String[] projectionAndroidIntents = {
//                WebIntentsProvider.WebAndroidMap.ID,    
//                WebIntentsProvider.WebAndroidMap.ANDROID_ACTION
//        };
//        
//        String selectionAndroidIntents = WebIntentsProvider.WebAndroidMap.WEB_ACTION + " = ?";
//        String[] selectionArgsAndroidIntents = { intent.action };
//        
//        Cursor cursorAndroidIntents = mContext.getContentResolver().query(WebIntentsProvider.WebAndroidMap.CONTENT_URI, 
//                projectionAndroidIntents, selectionAndroidIntents, selectionArgsAndroidIntents, null);
//        
//        PackageManager pm = mContext.getPackageManager();
//        
//        List<ResolveInfo> androidApps = new ArrayList<ResolveInfo>();
//
//        
//        if (cursorAndroidIntents != null) {
//            while (cursorAndroidIntents.moveToNext()) {
//                String androidAction = cursorAndroidIntents.
//                        getString(cursorAndroidIntents.getColumnIndex(WebIntentsProvider.WebAndroidMap.ANDROID_ACTION));
//                android.content.Intent androidIntent = new android.content.Intent(androidAction);
//                androidIntent.setType(intent.type);
//                List<ResolveInfo> listPerIntent = pm.queryIntentActivities(androidIntent, PackageManager.MATCH_DEFAULT_ONLY);
//                for (ResolveInfo ri : listPerIntent) {                        
//                    if (!androidApps.contains(ri)) {
//                        androidApps.add(ri);
//                    }
//                }
//            }
//        }
//        
//        androidAppListView.setAdapter(new AndroidAppListAdapter(mContext, R.layout.list_item_android_app, androidApps));
//        androidAppListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position,
//                    long id) {                    
//                Adapter adapter = (AndroidAppListAdapter) parent.getAdapter();
//                ResolveInfo ri = (ResolveInfo) adapter.getItem(position);
//                android.content.Intent intent = new android.content.Intent();
//                intent.setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
//                mContext.startActivity(intent);
//            }
//
//        });
//        
//        d.setOnDismissListener(WebIntentsAgentActivity.this);            
//
//        d.show();
//    }
   
}
