package org.openintents.wiagent;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class WebIntentsHelper {
    
    private Context mContext;
    
    public WebIntentsHelper(Context mContext) {
        this.mContext = mContext;
    }
    
    public void applicationChooser(Intent intent) {        
        intent.putExtra("action", intent.getAction());
        intent.putExtra("type", intent.getType());
        intent.putExtra("data", intent.getStringExtra(android.content.Intent.EXTRA_TEXT));
        intent.setComponent(new ComponentName("org.openintents.wiagent", "org.openintents.wiagent.WebIntentsHelperActivity"));
        
        mContext.startActivity(intent);
    }
}
