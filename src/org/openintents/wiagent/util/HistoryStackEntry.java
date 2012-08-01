package org.openintents.wiagent.util;

import org.openintents.wiagent.WebIntent;

public class HistoryStackEntry {

    public String url;
    public WebIntent webIntent;
    
    public HistoryStackEntry(String historyUrl, WebIntent historyWebIntent) {
        this.url = historyUrl;
        this.webIntent = historyWebIntent;
    }
}
