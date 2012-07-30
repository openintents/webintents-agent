package org.openintents.wiagent.sample;

import org.openintents.wiagent.R;
import org.openintents.wiagent.WebIntentsHelper;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class OIWebIntentsAndroidSampleAppActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.sample_main);
        
        Button btShare = (Button) findViewById(R.id.bt_share);
        final EditText etShare = (EditText) findViewById(R.id.et_share);
        
        btShare.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);                
                intent.setType("text/uri-list");                
                intent.putExtra(android.content.Intent.EXTRA_TEXT, etShare.getEditableText().toString());
                
                WebIntentsHelper helper = new WebIntentsHelper(OIWebIntentsAndroidSampleAppActivity.this);
                
                helper.createChooserWithWebActivities(intent);
            }
        });
        
//        ContentResolver cr = getContentResolver();
//        
//        String[] projection = {
//                "web_action"
//        };
//        
//        Uri contentUri = Uri.parse("content://org.openintents.wiagent/web_android_map");
//        Cursor cursor = cr.query(contentUri, projection, null, null, null);
//        cursor.moveToFirst();
    }
}