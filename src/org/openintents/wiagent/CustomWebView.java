package org.openintents.wiagent;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Context;
import android.net.http.SslCertificate;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.webkit.CookieManager;
import android.webkit.WebView;

public class CustomWebView extends WebView {
    
    static final String TAG = "CustomWebView";
    
    static final int HANDLER_WHAT_HTTPURLCONNECTIONTHREAD = 1;
        
    private List<String> iframeUrls;

    private Handler HttpURLConnectThreadHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {            
            if (msg.what == HANDLER_WHAT_HTTPURLCONNECTIONTHREAD) {
                HttpURLConnectionThreadMessageObj obj = (HttpURLConnectionThreadMessageObj) msg.obj;
                String baseUrl = obj.baseUrl;
                String data = obj.html;
                CustomWebView.this.iframeUrls = obj.iframeUrls;
                CustomWebView.this.loadDataWithBaseURL(baseUrl, data, "text/html", "utf8", null);                
            }   
        } 
        
    };

    public CustomWebView(Context context) {
        super(context);        
    }

    public CustomWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void loadUrl(String url) {
        try {
            URL urlWrapper = new URL(url);
            HttpURLConnectionThread workerThread = new HttpURLConnectionThread(urlWrapper);
            workerThread.start();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
    
    private class HttpURLConnectionThread extends Thread {
        
        private URL url;
        
        public HttpURLConnectionThread(URL url) {
            super();
            this.url = url;
        }

        @Override
        public void run() {
            HttpURLConnectionThreadMessageObj obj = new HttpURLConnectionThreadMessageObj();            

            HttpURLConnection urlConnection = null;
            try { 
                urlConnection = (HttpURLConnection) url.openConnection(); 
                
                // Set cookies in requests
                CookieManager cookieManager = CookieManager.getInstance();                    
                String cookie = cookieManager.getCookie(urlConnection.getURL().toString());
                if (cookie != null) {
                    urlConnection.setRequestProperty("Cookie", cookie);
                }                                        
                urlConnection.connect();
                
                // Get cookies from responses and save into the cookie manager
                List<String> cookieList = urlConnection.getHeaderFields().get("Set-Cookie");
                if (cookieList != null) {
                    for (String cookieTemp : cookieList) {                         
                        cookieManager.setCookie(urlConnection.getURL().toString(), cookieTemp);
                    }
                }                    
               
                InputStream in = new BufferedInputStream (urlConnection.getInputStream());
               
                Document doc = Jsoup.parse(in, "UTF-8", url.toString());
                
                Elements iframeSrcs = doc.select("iframe[src]");
                if (iframeSrcs != null) {                    
                    for (Element iframeSrc : iframeSrcs) {
                        String relatviePath = iframeSrc.attr("src");
                        URL iframeUrl = new URL(urlConnection.getURL(), relatviePath);
                        obj.iframeUrls.add(iframeUrl.toString());
                    }
                }
                
                Elements oldJsIntentsList = doc.select("script[src]");
                for (Element oldJsIntents : oldJsIntentsList) {
                    String value = oldJsIntents.attr("src");
                    if (value.contains("webintents.min.js") ||
                            value.contains("webintents-prefix.js")) {
                        oldJsIntents.remove();
                    }
                }
                String jsIntents = 
                        "var Intent = function(action, type) {" +
                            "this.action = action;" +
                            "this.type = type;" +
//                              "this.href = href;" +
                        "};" +
                        "var Navigator = function() {};" +
                        "Navigator.prototype.startActivity = function(intent) {" +
                            "navigatorAndroid.startActivity(intent.action, intent.type);" +
                        "};" +
                        "window.navigator = new Navigator();";
                doc.head().appendElement("script")
                    .attr("type", "text/javascript")
                    .html(jsIntents);
                obj.baseUrl = urlConnection.getURL().toString();
                obj.html = doc.outerHtml();
                HttpURLConnectThreadHandler.obtainMessage(HANDLER_WHAT_HTTPURLCONNECTIONTHREAD, obj).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }
        
    }
    
    /**
     * The class for obj field of message sent between HttpURLConnectionThread
     * and its handlers 
     * @author Cheng Zheng
     *
     */
    static class HttpURLConnectionThreadMessageObj {
        String baseUrl;
        String html;
        List<String> iframeUrls = new ArrayList<String>();
    }
    
    public List<String> getIframeUrls() {
        return iframeUrls;
    }

}
