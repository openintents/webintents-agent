package org.openintents.wiagent;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class WebIntentsService {
    
    private Context mContext;    
    
    public WebIntentsService(Context mContext) {
        super();
        this.mContext = mContext;
    }

    public void startActivity(String action, String type) {
        Intents intents = new Intents(mContext);
        intents.startActivity(new Intent(action, type));        
    }
    
    public void alert(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(msg)
            .setCancelable(false)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                     dialog.cancel();
                }
            });
        AlertDialog alert = builder.create();
        alert.show();
    }
    
}
