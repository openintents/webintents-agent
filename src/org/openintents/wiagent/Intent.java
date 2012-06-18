package org.openintents.wiagent;

import android.net.Uri;

public class Intent {

    public String action;
    public String type;
    public Uri url;
    
    /**
     * @deprecated
     */
    public Intent() {
        super();
    }

    public Intent(String action, String type) {
        super();
        this.action = action;
        this.type = type;
    }
    
}
