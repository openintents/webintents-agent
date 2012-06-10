package org.openintents.wiagent;

import org.openintents.wiagent.provider.WebIntentsProvider;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class WebIntentsService  {
    
//    private Context mContext;    
//    
//    public WebIntentsService(Context mContext) {
//        super();
//        this.mContext = mContext;
//    }
//
//
//    
//    public void alert(String msg) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
//        builder.setMessage(msg)
//            .setCancelable(false)
//            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int id) {
//                     dialog.cancel();
//                }
//            });
//        AlertDialog alert = builder.create();
//        alert.show();
//    }
//    
//    public void startActivity(String action, String type) {
//        startActivity(new Intent(action, type)); 
//    }
//
//    @Override
//    public void startActivity(Intent intent) {
//        // TODO Auto-generated method stub
////      AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
////      builder.setMessage("action:" + intent.action + "\n" +
////              "type:" + intent.type)
////          .setCancelable(false)
////          .setPositiveButton("OK", new DialogInterface.OnClickListener() {
////              public void onClick(DialogInterface dialog, int id) {
////                   dialog.cancel();
////              }
////          });
////      AlertDialog alert = builder.create();
////      alert.show();
//        
//        final Dialog d = new Dialog(mContext);
//        d.setTitle("Suggested Applications");
//        String[] mProjection = {
//            WebIntentsProvider.Intents.ID,    
//            WebIntentsProvider.Intents.TITLE,
//            WebIntentsProvider.Intents.HREF
//        };
//        
//        String selection = WebIntentsProvider.Intents.ACTION + " = ?";
//        String[] selectionArgs = { intent.action };
//        
//        Cursor mCursor = mContext.getContentResolver().query(WebIntentsProvider.Intents.CONTENT_URI, 
//                mProjection, selection, selectionArgs, null);
//        
//        final ListView mAppList = new ListView(mContext);
//        
//        String[] mColumns = {
//            WebIntentsProvider.Intents.TITLE,
//            WebIntentsProvider.Intents.HREF
//        };
//        
//        int[] mViewIDs = {
//            android.R.id.text1,
//            android.R.id.text2
//        };
//        
//        mAppList.setAdapter(new SimpleCursorAdapter(mContext, 
//                android.R.layout.simple_list_item_2, mCursor, mColumns, mViewIDs));
//        
//        mAppList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position,
//                    long id) {
//                Cursor cursor = ((SimpleCursorAdapter) mAppList.getAdapter()).getCursor();
//                cursor.moveToPosition(position);
//                String href = cursor.getString(2);
//                Intent intent = new Intent();
//                intent.url = Uri.parse(href);
//                Message msg = Message.obtain().setTarget(();
//                msg.obj = intent;
//                
//                
//                d.dismiss();
//            }
//            
//        });
//        
//        d.setContentView(mAppList);
//        d.setOnDismissListener((DialogInterface.OnDismissListener) mContext);
//        d.show();
//    }
    
}
