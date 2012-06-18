package org.openintents.wiagent.ui;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openintents.wiagent.CustomWebView;
import org.openintents.wiagent.Intent;
import org.openintents.wiagent.Intents;
import org.openintents.wiagent.R;
import org.openintents.wiagent.provider.WebIntentsProvider;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.os.Bundle;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;

public class WebIntentsAgentActivity extends Activity
        implements SearchView.OnQueryTextListener, DialogInterface.OnDismissListener {
    
    private CustomWebView mWebView;
    private SearchView mSearchView;
    private Message mOnDismissMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
        mWebView = (CustomWebView) findViewById(R.id.webView);
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
                        urlConnection = (HttpURLConnection) new URL(url).openConnection(); 
                        
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
//                                      "this.href = href;" +
                                "};" +
                                "var Navigator = function() {};" +
                                "Navigator.prototype.startActivity = function(intent) {" +
                                    "navigatorAndroid.startActivity(intent.action, intent.type);" +
                                "};" +
                                "window.navigator = new Navigator();";
                        doc.head().appendElement("script")
                            .attr("type", "text/javascript")
                            .html(jsIntents);
                        in = new ByteArrayInputStream(doc.outerHtml().getBytes("UTF-8"));                         
                    } catch (IOException e) {
                        e.printStackTrace();                        
                    } finally {
                        if (urlConnection != null) {
                            urlConnection.disconnect();
                        } 
                        if (in != null) {
                            return new WebResourceResponse("text/html", "utf8", in);
                        } else {
                            return null;
                        }
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
        mWebView.setWebChromeClient(new WebChromeClient());
        
        mWebView.addJavascriptInterface(new Navigator(this), "navigatorAndroid");
        mWebView.loadUrl("http://examples.webintents.org/intents/share/share.html");
        
//        mWebView.loadUrl("http://examples.webintents.org/usage/startActivity/index.html");
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

    @Override
    public void onDismiss(DialogInterface dialog) {
        String url = ((Intent) mOnDismissMessage.obj).url.toString();
        mWebView.loadUrl(url);        
    }
    
    private class Navigator implements Intents {
        private Context mContext;    

        public Navigator(Context mContext) {
            super();
            this.mContext = mContext;
        }

        public void alert(String msg) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage(msg)
            .setCancelable(false)
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }

        public void startActivity(String action, String type) {
            startActivity(new Intent(action, type)); 
        }

        @Override
        public void startActivity(Intent intent) {

            final Dialog d = new Dialog(mContext);
            d.setTitle("Suggested Applications");
            String[] mProjection = {
                    WebIntentsProvider.Intents.ID,    
                    WebIntentsProvider.Intents.TITLE,
                    WebIntentsProvider.Intents.HREF
            };

            String selection = WebIntentsProvider.Intents.ACTION + " = ?";
            String[] selectionArgs = { intent.action };

            Cursor mCursor = mContext.getContentResolver().query(WebIntentsProvider.Intents.CONTENT_URI, 
                    mProjection, selection, selectionArgs, null);

            final ListView mAppList = new ListView(mContext);

            String[] mColumns = {
                    WebIntentsProvider.Intents.TITLE,
                    WebIntentsProvider.Intents.HREF
            };

            int[] mViewIDs = {
                    android.R.id.text1,
                    android.R.id.text2
            };

            mAppList.setAdapter(new SimpleCursorAdapter(mContext, 
                    android.R.layout.simple_list_item_2, mCursor, mColumns, mViewIDs));

            mAppList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position,
                        long id) {
                    Cursor cursor = ((SimpleCursorAdapter) mAppList.getAdapter()).getCursor();
                    cursor.moveToPosition(position);
                    String href = cursor.getString(2);
                    Intent intent = new Intent();
                    intent.url = Uri.parse(href);
                    mOnDismissMessage = Message.obtain();
                    mOnDismissMessage.obj = intent;
                    d.dismiss();
                }

            });

            d.setContentView(mAppList);
            d.setOnDismissListener(WebIntentsAgentActivity.this);
            d.show();
        }
    }

}
