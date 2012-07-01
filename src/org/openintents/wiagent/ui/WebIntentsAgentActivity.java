package org.openintents.wiagent.ui;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openintents.wiagent.CustomWebView;
import org.openintents.wiagent.WebIntent;
import org.openintents.wiagent.Intents;
import org.openintents.wiagent.R;
import org.openintents.wiagent.provider.WebIntentsProvider;
import org.openintents.wiagent.ui.widget.AndroidAppListAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class WebIntentsAgentActivity extends Activity
        implements SearchView.OnQueryTextListener {
    
    private CustomWebView mWebView;
    private SearchView mSearchView;
    
    private static final int DISMISSMESSAGE_WHAT_WEBINTENTS = 1;
    private Message mOnDismissMessage;
    
    private ValueCallback<Uri> mUploadFile;
    private final static int FILECHOOSER_RESULTCODE = 1;
    
    private Handler mUIThreadHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mUIThreadHandler = new Handler();
        
        setContentView(R.layout.main);
        
        mWebView = (CustomWebView) findViewById(R.id.webView);
        mWebView.setContext(this);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient() { 

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view,
                    String url) {
                CustomWebView v = (CustomWebView) view;                
                if (v.getIframeUrls().contains(url)) {
                    HttpURLConnection urlConnection = null;
                    InputStream in = null;
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
                       
                        in = new BufferedInputStream (urlConnection.getInputStream());
                       
                        Document doc = Jsoup.parse(in, "UTF-8", url.toString());
                        
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
                        
                        doc.head().prependElement("script")
                            .attr("type", "text/javascript")
                            .html(scriptToInject);
                        in = new ByteArrayInputStream(doc.outerHtml().getBytes("UTF-8"));                         
                    } catch (IOException e) {
                        e.printStackTrace();                        
                    } finally {
                        if (urlConnection != null) {
                            urlConnection.disconnect();
                        } 
                    }
                    
                    if (in != null) {
                        return new WebResourceResponse("text/html", "utf8", in);
                    } else {
                        return null;
                    }
                } else {
                    return super.shouldInterceptRequest(view, url);
                }                
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                WebView.HitTestResult r = view.getHitTestResult();
                return true;
            }
            
        });
        mWebView.setWebChromeClient(new WebChromeClient() {
            
            // File chooser of WebView, hidden by the document
            public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType) {
                mUploadFile = uploadFile;
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);  
                intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);  
                intent.setType(acceptType);  
                WebIntentsAgentActivity.this.startActivityForResult(intent.createChooser(intent, "Choose file using"), FILECHOOSER_RESULTCODE); 
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message,
                    JsResult result) {
                // TODO Auto-generated method stub
                return super.onJsAlert(view, url, message, result);
            }
            
        });
        
        mWebView.addJavascriptInterface(new Navigator(this), "navigatorAndroid");
//        mWebView.loadUrl("http://examples.webintents.org/intents/share/share.html");
//        mWebView.loadUrl("javascritp:");
        mWebView.loadUrl("http://examples.webintents.org/usage/startActivity/index.html");
//        mWebView.loadUrl("http://examples.webintents.org/intents/shorten/shorten.html");
//        mWebView.loadUrl("https://twitter.com/intent/session");
//        mWebView.loadUrl("https://m.facebook.com/"); 
//        mWebView.loadUrl("http://ie.microsoft.com/testdrive/HTML5/DOMContentLoaded/Default.html");
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_bar, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_addr_bar);        
        setupSearchView(searchItem);
        return true;
    }
        
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            android.content.Intent data) {
        switch (requestCode) {
        case FILECHOOSER_RESULTCODE:
            Uri result = null;
            if (resultCode == RESULT_OK) {
                result = data.getData();
                mUploadFile.onReceiveValue(result);
            }
            break;

        default:
            break;
        }
    }

    private void setupSearchView(MenuItem searchItem) {
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setIconifiedByDefault(false);       
        mSearchView.setOnQueryTextListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        switch (item.getItemId()) {
        case R.id.menu_backward:
            if (mWebView.canGoBack()) {
                mWebView.goBack();
            }         
            break;
            
        case R.id.menu_forward:
            if (mWebView.canGoForward()) {
                mWebView.goForward();
            }       
            break;
            
        case R.id.menu_home:
            mWebView.loadUrl("http://webintents.org/");
            break;
            
        case R.id.menu_refresh:
            mWebView.reload();
            break;
            
        case R.id.menu_registered_apps:
            Dialog d = new Dialog(this);
            d.setTitle("Registered Applications");
            String[] mProjection = {
                WebIntentsProvider.Intents.ID,    
                WebIntentsProvider.Intents.TITLE,
                WebIntentsProvider.Intents.HREF
            };
            
            Cursor mCursor = getContentResolver().query(WebIntentsProvider.Intents.CONTENT_URI, 
                    mProjection, null, null, null);
            
            ListView mAppList = new ListView(this);
            
            String[] mColumns = {
                WebIntentsProvider.Intents.TITLE,
                WebIntentsProvider.Intents.HREF
            };
            
            int[] mViewIDs = {
                android.R.id.text1,
                android.R.id.text2
            };
            
            mAppList.setAdapter(new SimpleCursorAdapter(this, 
                    android.R.layout.simple_list_item_2, mCursor, mColumns, mViewIDs));
            d.setContentView(mAppList);
            d.show();

        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mWebView.loadUrl(query);
        return false;
    }

    private class Navigator implements Intents {
        
        private Context mContext;
        
        public Navigator(Context mContext) {
            super();
            this.mContext = mContext;
        }

        public void startActivity(String action, String type, String data) {
            startActivity(new WebIntent(action, type, data));
        }

        @Override
        public void startActivity(final WebIntent webIntent) {

            final Dialog d = new Dialog(mContext);
            d.setContentView(R.layout.dialog_suggested_apps);
            
            d.setTitle("Suggested Applications");
            
            ListView webAppListView = (ListView) d.findViewById(R.id.web_app);
            ListView androidAppListView = (ListView) d.findViewById(R.id.android_app);            
            
            String[] projectionWebIntents = {
                    WebIntentsProvider.Intents.ID,    
                    WebIntentsProvider.Intents.TITLE,
                    WebIntentsProvider.Intents.HREF
            };

            String selectionWebIntents = WebIntentsProvider.Intents.ACTION + " = ?";
            String[] selectionArgsWebIntents = { webIntent.action };

            Cursor cursorWebIntents = mContext.getContentResolver().query(WebIntentsProvider.Intents.CONTENT_URI, 
                    projectionWebIntents, selectionWebIntents, selectionArgsWebIntents, null);            

            String[] columnWebIntents = {
                    WebIntentsProvider.Intents.TITLE,
                    WebIntentsProvider.Intents.HREF
            };

            int[] mViewIDs = {
                    android.R.id.text1,
                    android.R.id.text2
            };
            
            webAppListView.setAdapter(new SimpleCursorAdapter(mContext, 
                    android.R.layout.simple_list_item_2, cursorWebIntents, columnWebIntents, mViewIDs, 0));
            
            final WebIntent fWebIntent = webIntent;
           
            webAppListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position,
                        long id) { 
                    d.dismiss();
                    Cursor cursor = ((SimpleCursorAdapter) parent.getAdapter()).getCursor();
                    cursor.moveToPosition(position);
                    final String href = cursor.getString(2); 
                    
                    mUIThreadHandler.post(new Runnable() {
                        
                        @Override
                        public void run() {
                            mWebView.loadUrl(href, fWebIntent);    
                        }
                        
                    });
                }

            });
            
            String[] projectionAndroidIntents = {
                    WebIntentsProvider.WebAndroidMap.ID,    
                    WebIntentsProvider.WebAndroidMap.ANDROID_ACTION
            };
            
            String selectionAndroidIntents = WebIntentsProvider.WebAndroidMap.WEB_ACTION + " = ?";
            String[] selectionArgsAndroidIntents = { webIntent.action };
            
            Cursor cursorAndroidIntents = mContext.getContentResolver().query(WebIntentsProvider.WebAndroidMap.CONTENT_URI, 
                    projectionAndroidIntents, selectionAndroidIntents, selectionArgsAndroidIntents, null);
            
            PackageManager pm = mContext.getPackageManager();
            
            List<ResolveInfo> androidApps = new ArrayList<ResolveInfo>();
            
            if (cursorAndroidIntents != null) {
                while (cursorAndroidIntents.moveToNext()) {
                    String androidAction = cursorAndroidIntents.
                            getString(cursorAndroidIntents.getColumnIndex(WebIntentsProvider.WebAndroidMap.ANDROID_ACTION));
                    Intent androidIntent = new Intent(androidAction);
                    androidIntent.setType(webIntent.type);
                    List<ResolveInfo> listPerIntent = pm.queryIntentActivities(androidIntent, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo ri : listPerIntent) {                        
                        if (!androidApps.contains(ri)) {
                            androidApps.add(ri);
                        }
                    }
                }
            }
            
            androidAppListView.setAdapter(new AndroidAppListAdapter(mContext, R.layout.list_item_android_app, androidApps));
            androidAppListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position,
                        long id) {  
                    d.dismiss();
                    Adapter adapter = (AndroidAppListAdapter) parent.getAdapter();
                    ResolveInfo ri = (ResolveInfo) adapter.getItem(position);
                    Intent intent = new Intent();
                    intent.setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
                    intent.putExtra(Intent.EXTRA_TEXT, webIntent.data);
                    mContext.startActivity(intent);
                }

            });
            
            d.show();
        }
    }

}
