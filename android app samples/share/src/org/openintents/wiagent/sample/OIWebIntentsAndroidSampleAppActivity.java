package org.openintents.wiagent.sample;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class OIWebIntentsAndroidSampleAppActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
        Button btShare = (Button) findViewById(R.id.bt_share);
        final EditText etShare = (EditText) findViewById(R.id.et_share);
        
        btShare.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("action", android.content.Intent.ACTION_SEND);
                intent.putExtra("type", "text/uri-list");
                intent.putExtra("data", etShare.getText().toString());
                intent.setComponent(new ComponentName("org.openintents.wiagent", "org.openintents.wiagent.WebIntentsHelperActivity"));
                
                startActivity(intent);
            }
        });
    }
}