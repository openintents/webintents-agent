package org.openintents.wiagent.util;

import org.openintents.wiagent.WebIntent;

public class HistoryStackEntry {

    public String historyUrl;
    public WebIntent historyWebIntent;
    
    public HistoryStackEntry(String historyUrl, WebIntent historyWebIntent) {
        this.historyUrl = historyUrl;
        this.historyWebIntent = historyWebIntent;
    }
}
