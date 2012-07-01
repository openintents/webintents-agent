package org.openintents.wiagent;

public class WebIntent {

    public String action;
    public String type;
    public String data;
    
    /**
     * @deprecated
     */
    public WebIntent() {
        super();
    }

    public WebIntent(String action, String type, String data) {
        super();
        this.action = action;
        this.type = type;
        this.data = data;
    }
    
}
