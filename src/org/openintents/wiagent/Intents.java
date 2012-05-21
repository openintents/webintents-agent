package org.openintents.wiagent;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class Intents {
    
    private Context mContext;
    
	public Intents(Context mContext) {
        super();
        this.mContext = mContext;
    }

    public void startActivity (Intent intent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage("action:" + intent.action + "\n" +
                "type:" + intent.type)
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
