package org.openintents.wiagent.util;

import org.openintents.wiagent.WebIntent;

/**
 * Stack entry for history urls and their intents
 * @author Cheng Zheng
 *
 */
public class HistoryStackEntry {

	public String url;
	public WebIntent webIntent;

	public HistoryStackEntry(String historyUrl, WebIntent historyWebIntent) {
		this.url = historyUrl;
		this.webIntent = historyWebIntent;
	}
}
