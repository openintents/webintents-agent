package org.openintents.wiagent.ui.widget;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.openintents.wiagent.WebIntent;
import org.openintents.wiagent.provider.WebIntentsProvider;
import org.openintents.wiagent.provider.WebIntentsProvider.LocalServiceDomain;
import org.openintents.wiagent.provider.WebIntentsProvider.WebIntents;
import org.openintents.wiagent.util.HistoryStackEntry;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
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
            "var Intent = function(action, type, data) {\n" +                
                "this.action = action;\n" +
                "this.type = type;\n" +
                "this.data = data;\n" +
            "};\n" +
            "window.navigator.startActivity = function(intent, onSuccess) {\n" +
                "if (!onSuccess) { onSuccess = null } else { onSuccess = onSuccess.toString() };" +
                "navigatorAndroid.startActivity(" +
                    "intent.action, " +
                    "intent.type, " +
                    "intent.data, " +
                    "onSuccess" +
                ");\n" +
            "};\n";
    
    private boolean mHasResult;
    
    private String mHistoryUrl;
    private String mCurrentUrl;
    
    private List<String> mIframeUrls;
    
    private Context mContext;
    
    private Stack<HistoryStackEntry> mHistoryStack = new Stack<HistoryStackEntry>();
   
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
                CustomWebView.this.loadDataWithBaseURL(baseUrl, data, "text/html", "utf8", mCurrentUrl);                
                break;
            }
                
            case HANDLER_WHAT_FILEREADERTHREAD: {
                LoadDataMessage obj = (LoadDataMessage) msg.obj;
                String baseUrl = obj.mBaseUrl;
                String data = obj.mHtml;
                CustomWebView.this.mIframeUrls = obj.mIframeUrls;
                CustomWebView.this.loadDataWithBaseURL(baseUrl, data, "text/html", "utf8", mCurrentUrl);
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

    // Constructor for attributes in the AndroidManifest.xml
    public CustomWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public void goBackWithData(String data, String onSuccess) {
        if (!mHistoryStack.empty()) {
            // Pop up current url
            mCurrentUrl = ((HistoryStackEntry) mHistoryStack.pop()).url;
            if (!mHistoryStack.empty()) {
                mHistoryUrl = ((HistoryStackEntry) mHistoryStack.pop()).url;
                WebIntent webIntent = new WebIntent();
                webIntent.data = data;
                webIntent.onSuccess = onSuccess;
                loadUrl(mHistoryUrl, webIntent, true);
            }
        }
    }

    @Override
    public void loadUrl(String url) { 
        WebIntent webIntent = null;
        loadUrl(url, webIntent, false);
    }
    
    public void loadUrl(String url, WebIntent webIntent, boolean hasResult) {
        mHasResult = hasResult;        
        mCurrentUrl = url;
        
        if (!mHistoryStack.empty()) {
            mHistoryUrl = ((HistoryStackEntry) mHistoryStack.peek()).url;
        }
        
        if (!mCurrentUrl.equals(mHistoryStack)) {
            mHistoryStack.push(new HistoryStackEntry(url, webIntent));
        } 
        
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
                
                String scriptToPrepend = sScriptToInject;
                
                if (mWebIntent != null) {
                    scriptToPrepend += 
                        "var intent = new Intent();" +
                                "intent.action = " + (mWebIntent.action == null ? "null" : "'" + mWebIntent.action + "'") + ";\n" +
                                "intent.type = " + (mWebIntent.type == null ? "null" : "'" + mWebIntent.type + "'") + ";\n" +
                                "intent.data = " + (mWebIntent.data == null ? "null" : "'" + mWebIntent.data + "'") + ";\n" +
                                "intent.postResult = function(data) {\n" +
                                    "var onSuccess = " + (mWebIntent.onSuccess == null ? "null" : "'" + mWebIntent.onSuccess.replace('\n', ' ') + "'") + ";\n" +
                                    "navigatorAndroid.goBack(data, onSuccess);\n" +
                                "};\n" +
                        "window.intent = intent;\n";
                }
                
                DataNode dataNode = new DataNode(scriptToPrepend, "");                
                doc.head().prependElement("script")
                    .attr("type", "text/javascript")
                    .prependChild(dataNode);
                
                if (mHasResult) {
                    String scriptToAppend =
                            "var onSuccess = " + mWebIntent.onSuccess.replace('\n', ' ') + ";\n" +
                            "onSuccess('" + mWebIntent.data +
                            "')"; 
                    dataNode = new DataNode(scriptToAppend, "");           
                    doc.body().appendElement("script")
                        .attr("type", "text/javascript")
                        .prependChild(dataNode);
                }
                
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
                    AsyncTask<Elements, Void, Void> registerTask = new AsyncTask<Elements, Void, Void>() {

                        @Override
                        protected Void doInBackground(Elements... params) {
                            boolean isInsert = false;
                            
                            ContentResolver cr = mContext.getContentResolver();
                            
                            for (int i = 0; i < params.length; i++) {
                                Elements webintents = params[i];
                                
                                for (Element webintent : webintents) {
                                    String[] projection = {
                                        WebIntentsProvider.WebIntents._ID                         
                                    };
                                    String selection = WebIntentsProvider.WebIntents.ACTION + " = ? and " +
                                            WebIntentsProvider.WebIntents.TYPE + " = ? and " +
                                            WebIntentsProvider.WebIntents.HREF + " = ?";
                                    String[] selectionArgs = {
                                            webintent.attr("action"),
                                            webintent.attr("type"),
                                            webintent.attr("href")
                                    }; 
                                    
                                    Cursor cursor = cr.query(WebIntentsProvider.WebIntents.CONTENT_URI, projection, selection, selectionArgs, null);
                                    
                                    if (!cursor.moveToFirst()) {                        
                                        cursor = cr.query(WebIntentsProvider.WebIntents.CONTENT_URI, projection, selection, selectionArgs, null);
                                        if (!cursor.moveToFirst()) {
                                            ContentValues values = new ContentValues();
                                            values.put(WebIntents.ACTION, webintent.attr("action"));
                                            values.put(WebIntents.TYPE, webintent.attr("type"));
                                            values.put(WebIntents.HREF, webintent.attr("href"));
                                            values.put(WebIntents.TITLE, webintent.attr("title"));
                                            values.put(WebIntents.DISPOSITION, webintent.attr("disposition"));
                                            cr.insert(WebIntentsProvider.WebIntents.CONTENT_URI, values);
                                            isInsert = true;                            
                                        }
                                    }                    
                                }
                            } 
                            
                            if (isInsert == true) {
                                cr.notifyChange(WebIntents.CONTENT_URI, null);
                            }
                            
                            return null;
                        }
                    };
                    
                    registerTask.execute(webintents);
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
                            "intent.action = " + (mWebIntent.action == null ? "null" : "'" + mWebIntent.action + "'") + ";\n" +
                            "intent.type = " + (mWebIntent.type == null ? "null" : "'" + mWebIntent.type + "'") + ";\n" +
                            "intent.data = " + (mWebIntent.data == null ? "null" : "'" + mWebIntent.data + "'") + ";\n" +
                            "intent.postResult = function(data) {\n" +
                                "var onSuccess = " + (mWebIntent.onSuccess == null ? "null" : "'" + mWebIntent.onSuccess.replace('\n', ' ') + "'") + ";\n" +
                                "navigatorAndroid.goBack(data, onSuccess);\n" +
                            "};\n" +
                        "window.intent = intent;\n";
                }
                
                // Use DataNode to avoid change " to &quot; by JSoup automatically
                DataNode dataNode = new DataNode(scriptToPrepend, "");                
                doc.head().prependElement("script")
                    .attr("type", "text/javascript")
                    .prependChild(dataNode);
                
                if (mHasResult) {
                    String scriptToAppend =
                            "var onSuccess = " + mWebIntent.onSuccess.replace('\n', ' ') + ";\n" +
                            "onSuccess('" + mWebIntent.data +
                            "')"; 
                    dataNode = new DataNode(scriptToAppend, "");           
                    doc.body().appendElement("script")
                        .attr("type", "text/javascript")
                        .prependChild(dataNode);
                }
                
                obj.mBaseUrl = null;
                ContentResolver cr = mContext.getContentResolver();
                String[] projection = {
                        LocalServiceDomain.WEB_DOMAIN
                };
                String selection = LocalServiceDomain.WEB_HERF + " = ?";
                String[] selectionArgs = {
                        mUrl
                };                
                Cursor cursor = cr.query(LocalServiceDomain.CONTENT_URI, projection, selection, selectionArgs, null);
                if (cursor.moveToNext()) {
                    obj.mBaseUrl = cursor.getString(cursor.getColumnIndex(LocalServiceDomain.WEB_DOMAIN));
                }
                
                obj.mHtml = doc.outerHtml();
                mUIThreadHandler.obtainMessage(HANDLER_WHAT_HTTPURLCONNECTIONTHREAD, obj).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
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
}