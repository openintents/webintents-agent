package org.openintents.wiagent.ui.widget;

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
import org.openintents.wiagent.HTMLIntentElement;
import org.openintents.wiagent.WebIntent;
import org.openintents.wiagent.provider.WebIntentsProvider;
import org.openintents.wiagent.provider.WebIntentsProvider.WebIntents;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.http.SslCertificate;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.webkit.CookieManager;
import android.webkit.WebView;

public class CustomWebView extends WebView {
    
    static final int HANDLER_WHAT_HTTPURLCONNECTIONTHREAD = 1;
    static final int HANDLER_WHAT_FILEREADERTHREAD = 2;
    
    public static final String sScriptToInject =
            "var Intent = function(action, type, data) {" +
                "this.action = action;" +
                "this.type = type;" +
                "this.data = data;" +
            "};" +
            "window.navigator.startActivity = function(intent) {" +
                "navigatorAndroid.startActivity(intent.action, intent.type, intent.data);" +
            "};";
        
    private String mCurrentUrl;
    private WebIntent mWebIntent;
    
    private List<String> mIframeUrls;
    
    private Context mContext;
   
    // Instances of CustomWebView mush be created by UIThread so that this
    // handler can be attached to it.
    private Handler mUIThreadHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {  
            switch (msg.what) {
            case HANDLER_WHAT_HTTPURLCONNECTIONTHREAD: {
                LoadDataMessage obj = (LoadDataMessage) msg.obj;
                String baseUrl = obj.mBaseUrl;
                String data = obj.mHtml;
                CustomWebView.this.mIframeUrls = obj.mIframeUrls;
                CustomWebView.this.loadDataWithBaseURL(baseUrl, data, "text/html", "utf8", null);                
                break;
            }
                
            case HANDLER_WHAT_FILEREADERTHREAD: {
                LoadDataMessage obj = (LoadDataMessage) msg.obj;
                String baseUrl = "file:///android_asset/www/service/";
                String data = obj.mHtml;
                CustomWebView.this.mIframeUrls = obj.mIframeUrls;
//                CustomWebView.this.loadData(data, "text/html", "utf8");
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
        mCurrentUrl = url;
        mWebIntent = webIntent;
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
    
    @Override
    public void reload() {
        loadUrl(mCurrentUrl, mWebIntent);
    }

    private class HttpURLConnectionThread extends Thread {
        
        private String mUrl;
        private WebIntent mWebIntent; 
        
        public HttpURLConnectionThread(String url, WebIntent webIntent) {
            super();
            this.mUrl = url;
            mWebIntent = webIntent;
        }

        @Override
        public void run() {
            LoadDataMessage obj = new LoadDataMessage(); 
            obj.mUrl = mUrl;            

            HttpURLConnection urlConnection = null;
            try { 
                urlConnection = (HttpURLConnection) (new URL(mUrl)).openConnection(); 
                
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
               
                Document doc = Jsoup.parse(in, "UTF-8", mUrl.toString());
                
                // Record src value of each iframe for later check
                Elements iframesWithSrc = doc.select("iframe[src]");
                if (iframesWithSrc != null) {                    
                    for (Element iframeWithSrc : iframesWithSrc) {
                        String relatviePath = iframeWithSrc.attr("src");
                        URL iframeUrl = new URL(urlConnection.getURL(), relatviePath);
                        obj.mIframeUrls.add(iframeUrl.toString());
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
                
                String scriptToInject = sScriptToInject;
                        
                
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
                obj.mBaseUrl = urlConnection.getURL().toString();
                obj.mHtml = doc.outerHtml();
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
            LoadDataMessage obj = new LoadDataMessage();
            obj.mUrl = mUrl;
          
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
                        obj.mIframeUrls.add(relatviePath);
                    }
                }
                
                Elements webintents = doc.select("intent");
                
                if (webintents != null && webintents.size() != 0) {
                    WebIntentsRegistrationTask task = new WebIntentsRegistrationTask();
                    task.execute(webintents);
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
                
                String scriptToPrepend = sScriptToInject;
                
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
                
                obj.mBaseUrl = null;
                obj.mHtml = doc.outerHtml();
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
    static class LoadDataMessage {
        String mUrl;
        String mBaseUrl;
        String mHtml;
        List<String> mIframeUrls = new ArrayList<String>();
    }
    
    public List<String> getIframeUrls() {
        return mIframeUrls;
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }
    
    private class WebIntentsRegistrationTask extends AsyncTask<Elements, Void, Void> {

        @Override
        protected Void doInBackground(Elements... params) { 
            boolean isInsert = false;
            ContentResolver cr = mContext.getContentResolver();
            for (int i = 0; i < params.length; i++) {
                Elements webintents = params[i];
                for (Element webintent : webintents) {
                    String[] projection = {
                        WebIntentsProvider.WebIntents.ID                         
                    };
                    String selection = WebIntentsProvider.WebIntents.ACTION + " = ? and " +
                    		WebIntentsProvider.WebIntents.TYPE + " = ? and " +
                    		WebIntentsProvider.WebIntents.HREF + " = ?";
                    String[] selectionArgs = {
                            webintent.attr("action"),
                            webintent.attr("type"),
                            webintent.attr("href")
                    }; 
                    Cursor cursor = cr.query(WebIntentsProvider.WebIntents.CONTENT_URI_INMEMORY, projection, selection, selectionArgs, null);
                    if (!cursor.moveToFirst()) {                        
                        cursor = cr.query(WebIntentsProvider.WebIntents.CONTENT_URI, projection, selection, selectionArgs, null);
                        if (!cursor.moveToFirst()) {
                            ContentValues values = new ContentValues();
                            values.put(WebIntents.ACTION, webintent.attr("action"));
                            values.put(WebIntents.TYPE, webintent.attr("type"));
                            values.put(WebIntents.HREF, webintent.attr("href"));
                            values.put(WebIntents.TITLE, webintent.attr("title"));
                            values.put(WebIntents.DISPOSITION, webintent.attr("disposition"));
                            cr.insert(WebIntentsProvider.WebIntents.CONTENT_URI_INMEMORY, values);
                            isInsert = true;                            
                        }
                    }                    
                }
            }  
            if (isInsert == true) {
                cr.notifyChange(WebIntents.CONTENT_URI_INMEMORY, null);
            }
            return null;
        }
        
    }

}
