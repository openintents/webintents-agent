package org.openintents.wiagent;

import org.openintents.wiagent.R;
import org.openintents.wiagent.provider.WebIntentsProvider;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;

public class WIAgentWebView extends Activity implements SearchView.OnQueryTextListener {    

    private EditText etUrl;
    private WebView mWebView;
    private InputMethodManager manager;
    private SearchView mSearchView;
 
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser);
        
        mWebView = (WebView) findViewById(R.id.webView);
        
        etUrl = (EditText) findViewById(R.id.menu_addr_bar);
        
        mWebView.requestFocus();
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                // TODO Auto-generated method stub
                super.onPageFinished(view, url); 
                
                view.loadUrl("javascript:" +
                        "var s = 'function Intent(action, type) {" +
                        "           this.action = action;" +
                        "           this.type = type;" +
                        "         }" +
                        "         function Navigator() {}" +
                        "         Navigator.prototype.startActivity = function(intent) {" +
                        "           WIService.startActivity(intent.action, intent.type);" +
                        "         };" +
                        "         window.navigator = new Navigator();';" +
                        "var head = document.getElementsByTagName('head')[0];" +
                        "var jsNode = document.createElement('script');" +
                        "jsNode.setAttribute('type', 'text/javascript');" +
                        "jsNode.innerHTML = s;" +
                        "head.appendChild(jsNode);" +
                        "var iframes = document.getElementsByTagName('iframe');" +
                        "for (var i = 0; i < iframes.length; i++) {" +
                        "  var fJsNode = iframes[i].contentDocument.createElement('script');" +
                        "  fJsNode.setAttribute('type', 'text/javascript');" +
                        "  fJsNode.innerHTML = s;" +
                        "  var fHead = iframes[i].contentDocument.getElementsByTagName('head')[0];" +
                        "  fHead.appendChild(fJsNode);" +
                        "}");
            }
            
        });
        
        mWebView.addJavascriptInterface(new WebIntentsService(this), "WIService");

        manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mWebView.loadUrl("http://examples.webintents.org/usage/startActivity/index.html");  
        
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
                WebIntentsProvider.Intents.TITLE
            };
            
            Cursor mCursor = getContentResolver().query(WebIntentsProvider.Intents.CONTENT_URI, 
                    mProjection, null, null, null);
            
            ListView mAppList = new ListView(this);
            
            String[] mColumns = {
                WebIntentsProvider.Intents.TITLE
            };
            
            int[] mViewIDs = {
                android.R.id.text1
            };
            
            mAppList.setAdapter(new SimpleCursorAdapter(this, 
                    android.R.layout.simple_list_item_1, mCursor, mColumns, mViewIDs));
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
    
}
