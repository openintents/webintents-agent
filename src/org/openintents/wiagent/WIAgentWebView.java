package org.openintents.wiagent;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.openintents.wiagent.R;
import org.openintents.wiagent.provider.WebIntentsProvider;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ConsoleMessage;
import android.webkit.HttpAuthHandler;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;

public class WIAgentWebView extends Activity 
        implements SearchView.OnQueryTextListener, DialogInterface.OnDismissListener {    

    private EditText etUrl;
    private WebView mWebView;
    private InputMethodManager manager;
    private SearchView mSearchView;
    private Message mOnDismissMessage;
    
    private static final int HANDLER_WHAT_HTML_REVISED = 1;
 
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser);
        
        mWebView = (WebView) findViewById(R.id.webView);
        
        etUrl = (EditText) findViewById(R.id.menu_addr_bar);
        
        mWebView.requestFocus();
        mWebView.getSettings().setJavaScriptEnabled(true);
//        mWebView.getSettings().setLoadWithOverviewMode(true);
//        mWebView.getSettings().setUseWideViewPort(true);
        
        mWebView.setWebViewClient(new WebViewClient() {
            
            
            
            @Override
            public void onReceivedHttpAuthRequest(WebView view,
                    HttpAuthHandler handler, String host, String realm) {
                // TODO Auto-generated method stub
                super.onReceivedHttpAuthRequest(view, handler, host, realm);
            }

            @Override
            public void onReceivedLoginRequest(WebView view, String realm,
                    String account, String args) {
                // TODO Auto-generated method stub
                super.onReceivedLoginRequest(view, realm, account, args);
            }
            
            

//            @Override
            public void onPageFinished(WebView view, String url) {
                // TODO Auto-generated method stub
                super.onPageFinished(view, url);
                System.out.println("This page: " + mWebView.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                super.shouldOverrideUrlLoading(view, url);
                System.out.println(url);
                try {
                    URL urlWrapper = new URL(url);
                    HttpURLConnectionThread workerThread = new HttpURLConnectionThread(urlWrapper);
                    workerThread.start();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                return true;
            }
            
        });
        
        mWebView.setWebChromeClient(new WebChromeClient());
        
        mWebView.addJavascriptInterface(new WebIntentsService(this), "WIService");

        manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        mWebView.loadUrl("http://examples.webintents.org/usage/startActivity/index.html");  
//        mWebView.loadUrl("http://examples.webintents.org/intents/pick/pick.html");
        mWebView.loadUrl("file:///android_asset/www/index.html");
//        mWebView.loadUrl("https://twitter.com/intent/tweet");
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
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        // TODO Auto-generated method stub
        mWebView.loadUrl(query);
        return false;
    }
    
    private Handler HttpURLConnectThreadHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == HANDLER_WHAT_HTML_REVISED) {
                HtmlMessage htmlMessage = (HtmlMessage) msg.obj;
                String data = htmlMessage.getHtml();
                String baseUrl = htmlMessage.getUrl();
                mWebView.loadDataWithBaseURL(baseUrl, data, "text/html", "utf8", null);
            }   
        }           
    };
    
    private class HttpURLConnectionThread extends Thread {
        
        private URL url;
        
        public HttpURLConnectionThread(URL url) {
            super();
            this.url = url;
        }

        @Override
        public void run() {
            super.run();
            HttpURLConnection urlConnection = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream (urlConnection.getInputStream());
                
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
//            		        "this.href = href;" +
        		        "};" +
        		        "var Navigator = function() {};" +
        		        "Navigator.prototype.startActivity = function(intent) {" +
        		            "WIService.startActivity(intent.action, intent.type);" +
    		            "};" +
    		            "window.navigator = new Navigator();";
                doc.head().appendElement("script")
                    .attr("type", "text/javascript")
                    .html(jsIntents);  
                HtmlMessage msg = new HtmlMessage(url.toString(), doc.outerHtml());
                HttpURLConnectThreadHandler.obtainMessage(HANDLER_WHAT_HTML_REVISED, msg).sendToTarget();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }
        
    }
    
    private class WebIntentsService implements Intents {
        private Context mContext;    

        public WebIntentsService(Context mContext) {
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
            d.setOnDismissListener(WIAgentWebView.this);
            d.show();
        }
    }
    
    @Override
    public void onDismiss(DialogInterface dialog) {
        // TODO Auto-generated method stub
        String url = ((Intent) mOnDismissMessage.obj).url.toString();
        mWebView.loadUrl(url);
    }
    
}
