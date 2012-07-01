package org.openintents.wiagent;

import android.net.http.SslCertificate;

public class HtmlMessage {

    private String url;
    private String html;
    public SslCertificate certificate;
    
    public HtmlMessage(String url, String html, SslCertificate certificate) {
        super();
        this.url = url;
        this.html = html;
        this.certificate = certificate;
    }

    public String getUrl() {
        return url;
    }

    public String getHtml() {
        return html;
    }
    
}
