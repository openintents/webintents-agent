package org.openintents.wiagent;

public class HtmlMessage {

    private String url;
    private String html;
    
    public HtmlMessage(String url, String html) {
        super();
        this.url = url;
        this.html = html;
    }

    public String getUrl() {
        return url;
    }

    public String getHtml() {
        return html;
    }
    
}
