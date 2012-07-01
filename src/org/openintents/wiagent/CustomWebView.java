package org.openintents.wiagent;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.http.SslCertificate;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.webkit.CookieManager;
import android.webkit.WebView;

public class CustomWebView extends WebView {
    
    static final int HANDLER_WHAT_HTTPURLCONNECTIONTHREAD = 1;
    static final int HANDLER_WHAT_FILEREADERTHREAD = 2;
        
    private List<String> mIframeUrls;
    
    private Context mContext;
   
    // Instances of CustomWebView mush be created by UIThread so that this
    // handler can be attached to it.
    private Handler mUIThreadHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {  
            switch (msg.what) {
            case HANDLER_WHAT_HTTPURLCONNECTIONTHREAD: {
                HttpURLConnectionThreadMessageObj obj = (HttpURLConnectionThreadMessageObj) msg.obj;
                String baseUrl = obj.baseUrl;
                String data = obj.html;
                CustomWebView.this.mIframeUrls = obj.iframeUrls;
                CustomWebView.this.loadDataWithBaseURL(baseUrl, data, "text/html", "utf8", null);                
                break;
            }
                
            case HANDLER_WHAT_FILEREADERTHREAD: {
                HttpURLConnectionThreadMessageObj obj = (HttpURLConnectionThreadMessageObj) msg.obj;
                String baseUrl = obj.baseUrl;
                String data = obj.html;
                CustomWebView.this.mIframeUrls = obj.iframeUrls;
                CustomWebView.this.loadDataWithBaseURL(baseUrl, data, "text/html", "utf8", null);
                break;
            }

            default:
                break;
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
        WebIntent weIntent = null;
        loadUrl(url, weIntent);
    }
    
    public void loadUrl(String url, WebIntent webIntent) {
        if (url.indexOf("http") == 0 || url.indexOf("https") == 0) {
            HttpURLConnectionThread workerThread = new HttpURLConnectionThread(url, webIntent);
            workerThread.start();
        } else if (url.indexOf("file:") == 0) {
            FileReaderThread workerThread = new FileReaderThread(url, webIntent);
            workerThread.start();
        } else {
            super.loadUrl(url);
        } 
    }
    
    private class HttpURLConnectionThread extends Thread {
        
        private String url;
        private WebIntent mWebIntent; 
        
        public HttpURLConnectionThread(String url, WebIntent webIntent) {
            super();
            this.url = url;
            mWebIntent = webIntent;
        }

        @Override
        public void run() {
            HttpURLConnectionThreadMessageObj obj = new HttpURLConnectionThreadMessageObj();            

            HttpURLConnection urlConnection = null;
            try { 
                urlConnection = (HttpURLConnection) (new URL(url)).openConnection(); 
                
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
                
                // Record src value of each iframe for later check
                Elements iframesWithSrc = doc.select("iframe[src]");
                if (iframesWithSrc != null) {                    
                    for (Element iframeWithSrc : iframesWithSrc) {
                        String relatviePath = iframeWithSrc.attr("src");
                        URL iframeUrl = new URL(urlConnection.getURL(), relatviePath);
                        obj.iframeUrls.add(iframeUrl.toString());
                    }
                }
                
                // Remove shim files
                Elements scriptsWithSrc = doc.select("script[src]");
                for (Element scriptWithSrc : scriptsWithSrc) {
                    String value = scriptWithSrc.attr("src");
                    if (value.contains("webintents.min.js") ||
                            value.contains("webintents-prefix.js")) {
                        scriptWithSrc.remove();
                    }
                }
                
                String scriptToInject = 
                        "var Intent = function(action, type, data) {" +
                            "this.action = action;" +
                            "this.type = type;" +
                            "this.data = data;" +
                        "};" +
                        "var Navigator = function() {};" +
                        "Navigator.prototype.startActivity = function(intent) {" +
                            "navigatorAndroid.startActivity(intent.action, intent.type, intent.data);" +
                        "};" +
                        "window.navigator = new Navigator();";
                
                if (mWebIntent != null) {
                    scriptToInject += 
                            "var intent = new Intent();" +
                                "intent.action = '" + (mWebIntent.action == null ? "null" : mWebIntent.action) + "';" +
                                "intent.type = '" + (mWebIntent.type == null ? "null" : mWebIntent.type) + "';" +
                                "intent.data = '" + (mWebIntent.data == null ? "null" : mWebIntent.data) + "';" +
                            "window.intent = intent";
                }
                
                doc.head().prependElement("script")
                    .attr("type", "text/javascript")
                    .html(scriptToInject);
                obj.baseUrl = urlConnection.getURL().toString();
                obj.html = doc.outerHtml();
                mUIThreadHandler.obtainMessage(HANDLER_WHAT_HTTPURLCONNECTIONTHREAD, obj).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }
        
    }
    
    private class FileReaderThread extends Thread {
        
        private String mUrl;
        private WebIntent mWebIntent; 
        
        public FileReaderThread(String url, WebIntent webIntent) {
            super();
            this.mUrl = url;
            mWebIntent = webIntent;
        }

        @Override
        public void run() {
            HttpURLConnectionThreadMessageObj obj = new HttpURLConnectionThreadMessageObj();            

            InputStream in = null;
            try {
                AssetManager assetManager = CustomWebView.this.mContext.getAssets(); 

                String path  = mUrl.substring("file:///android_asset/".length());
                in = assetManager.open(path);
                Document doc = Jsoup.parse(in, "UTF-8", mUrl.toString());
                
                // Record src value of each iframe for later check
                Elements iframesWithSrc = doc.select("iframe[src]");
                if (iframesWithSrc != null) {                    
                    for (Element iframeWithSrc : iframesWithSrc) {
                        String relatviePath = iframeWithSrc.attr("src");
                        obj.iframeUrls.add(relatviePath);
                    }
                }
                
                // Remove shim files
                Elements scriptsWithSrc = doc.select("script[src]");
                for (Element scriptWithSrc : scriptsWithSrc) {
                    String value = scriptWithSrc.attr("src");
                    if (value.contains("webintents.min.js") ||
                            value.contains("webintents-prefix.js")) {
                        scriptWithSrc.remove();
                    }
                }
                
                String scriptToPrepend = 
                        "var Intent = function(action, type, data) {" +
                            "this.action = action;" +
                            "this.type = type;" +
                            "this.data = data;" +
                        "};" +
                        "var Navigator = function() {};" +
                        "Navigator.prototype.startActivity = function(intent) {" +
                            "navigatorAndroid.startActivity(intent.action, intent.type, intent.data);" +
                        "};" +
                        "window.navigator = new Navigator();";
                
                if (mWebIntent != null) {
                    scriptToPrepend += 
                            "var intent = new Intent();" +
                                "intent.action = '" + (mWebIntent.action == null ? "null" : mWebIntent.action) + "';" +
                                "intent.type = '" + (mWebIntent.type == null ? "null" : mWebIntent.type) + "';" +
                                "intent.data = '" + (mWebIntent.data == null ? "null" : mWebIntent.data) + "';" +
                            "window.intent = intent;";
                }
                
                doc.head().prependElement("script")
                    .attr("type", "text/javascript")
                    .html(scriptToPrepend);
                
                obj.baseUrl = null;
                obj.html = doc.outerHtml();
                mUIThreadHandler.obtainMessage(HANDLER_WHAT_HTTPURLCONNECTIONTHREAD, obj).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
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
        return mIframeUrls;
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

}
