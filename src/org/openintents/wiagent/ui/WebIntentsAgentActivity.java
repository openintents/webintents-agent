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
import org.openintents.wiagent.WebIntent;
import org.openintents.wiagent.Intents;
import org.openintents.wiagent.R;
import org.openintents.wiagent.provider.WebIntentsProvider;
import org.openintents.wiagent.ui.widget.AndroidAppArrayAdapter;
import org.openintents.wiagent.ui.widget.CustomWebView;
import org.openintents.wiagent.ui.widget.UrlInputView;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;

/**
 * This is the main activity. This class includes a CustomWebView field which add Web Intents
 * supporting scripts while receiving a Web page
 * @author Cheng Zheng
 *
 */
public class WebIntentsAgentActivity extends Activity {

	/**
	 * The title for the menu item of application management of this activity
	 */
	private String mMenuItemAppManagementTitle = null;

	/**
	 * The customized webview where Web pages and Web services are running
	 */
	private CustomWebView mWebView;
	
	private UrlInputView mUrlInputView;

	private ValueCallback<Uri> mUploadFile;
	private final static int FILECHOOSER_RESULTCODE = 1;

	/**
	 * A handler attached to the main thread or UI thread, must be instantiated in the UI
	 * thread
	 */
	private Handler mUIThreadHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mUIThreadHandler = new Handler();

		setContentView(R.layout.main);

		mWebView = (CustomWebView) findViewById(R.id.webview);
		mUrlInputView = (UrlInputView) findViewById(R.id.url);

		mWebView.setContext(this);
		mWebView.getSettings().setJavaScriptEnabled(true);
		
		mWebView.setWebViewClient(new WebViewClient() { 

			/**
			 * Intercept the requests of resources in iframes and add Web Intents support
			 * to them before loading
			 */
			@Override
			public WebResourceResponse shouldInterceptRequest(WebView view,
					String url) {
				CustomWebView v = (CustomWebView) view;
				
				// No iframes for the main page
				if (v.getIframeUrls() == null) return super.shouldInterceptRequest(view, url);
				
				// The main page contains some iframes
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

						String scriptToInject = CustomWebView.sScriptToInject;

						// Inject the script to the front of the head element
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
				mUrlInputView.setText(url);
				view.loadUrl(url);
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
		});

		// Register a content observer for changing the title of menu item "Application Management"
		// according to the number of unbookmarked applications
		getContentResolver().registerContentObserver(WebIntentsProvider.WebIntents.CONTENT_URI, true, 
				new ContentObserver(mUIThreadHandler) {

					@Override
					public void onChange(boolean selfChange) {
						// Query the number of unbookmarked applications
						AsyncTask<Void, Void, Integer> webAppUnbookmarkedQueryTask = new AsyncTask<Void, Void, Integer>() {

							@Override
							protected Integer doInBackground(Void... params) {
								ContentResolver rc = getContentResolver();

								String[] projection = {
										"DISTINCT " + WebIntentsProvider.WebIntents.HREF
								};
								String selection = WebIntentsProvider.WebIntents.BOOKMARKED + "='0'";

								Cursor cursor = rc.query(WebIntentsProvider.WebIntents.CONTENT_URI, 
										projection, selection, null, null);

								int num = cursor.getCount();
								cursor.close();

								return num;
							}

							@Override
							protected void onPostExecute(Integer result) {
								if (result == 1) {
									mMenuItemAppManagementTitle = getResources().getString(R.string.app_management) + 
											" (" + result + " New App)";
								} else if (result > 1) {
									mMenuItemAppManagementTitle = getResources().getString(R.string.app_management) + 
											" (" + result + " New Apps)";
								} else {
									mMenuItemAppManagementTitle = null;
								}
							}
						};

						webAppUnbookmarkedQueryTask.execute();
					}

		});

		// Query the unbookmarked applications for the decision of menu item "Application Management"
		// when this app is launching
		AsyncTask<Void, Void, Integer> webAppDiscoveredQueryTask = new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				ContentResolver rc = getContentResolver();

				String[] projection = {
						"DISTINCT " + WebIntentsProvider.WebIntents.HREF
				};
				String selection = WebIntentsProvider.WebIntents.BOOKMARKED + " = '0'";

				Cursor cursor = rc.query(WebIntentsProvider.WebIntents.CONTENT_URI, 
						projection, selection, null, null);
				int num = cursor.getCount();
				cursor.close();
				return num;
			}

			@Override
			protected void onPostExecute(Integer result) {
				if (result == 1) {
					mMenuItemAppManagementTitle = getResources().getString(R.string.app_management) + 
							" (" + result + " New App)";
				} else if (result > 1) {
					mMenuItemAppManagementTitle = getResources().getString(R.string.app_management) + 
							" (" + result + " New Apps)";
				} else {
					mMenuItemAppManagementTitle = null;
				}
			}
		};

		webAppDiscoveredQueryTask.execute();

		mWebView.addJavascriptInterface(new Navigator(this), "navigatorAndroid");
//        mWebView.loadUrl("http://examples.webintents.org/intents/share/share.html");
//        mWebView.loadUrl("javascritp:");
//        mWebView.loadUrl("http://examples.webintents.org/usage/startActivity/index.html");
//        mWebView.loadUrl("file:///android_asset/www/service/webintents-debugger.html");


		Intent intent = getIntent();

		if (intent.getAction() == null) {
		// This activity is launched by WebIntentsHelperActivity
            WebIntent webIntent = new WebIntent();
            webIntent.action = intent.getStringExtra("action");
            webIntent.data = intent.getStringExtra("data");
            webIntent.type = intent.getStringExtra("type");
            String href = intent.getStringExtra("href");
            mWebView.loadUrl(href, webIntent, false);
        } else {
        // This activity is launched normally
            mWebView.loadUrl(getString(R.string.home_url));
        }
        
//        mWebView.loadUrl("http://examples.webintents.org/intents/shorten/shorten.html");
//        mWebView.loadUrl("https://twitter.com/intent/session");
//        mWebView.loadUrl("https://m.facebook.com/"); 
//        mWebView.loadUrl("http://ie.microsoft.com/testdrive/HTML5/DOMContentLoaded/Default.html");
    }

	/**
	 * Prepare the menu items in action bar
	 */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Remove existing items in case of duplicates
        menu.clear();
        
        // Inflate with new items
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.nav_main, menu);
        if (mMenuItemAppManagementTitle != null) {
            MenuItem menuItem = menu.findItem(R.id.menu_app_management);
            menuItem.setTitle(mMenuItemAppManagementTitle);
        }
        
        return true;
    }

    /**
     * Activity result for file chooser in a Web page
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            android.content.Intent data) {
        switch (requestCode) {
        case FILECHOOSER_RESULTCODE:
            Uri result = null;
            if (resultCode == RESULT_OK) {
                result = data.getData();
                String type = data.getType();
                mUploadFile.onReceiveValue(result);
            }
            break;

        default:
            break;
        }
    }

    /**
     * Set actions for the events of selecting menu items
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
        	mUrlInputView.setText(getString(R.string.home_url));
			mWebView.loadUrl(getString(R.string.home_url));
            break;
            
        case R.id.menu_refresh:
            mWebView.reload();
            break;
            
        case R.id.menu_app_management:
            Context appContext = getApplicationContext();
            Intent intent = new Intent(appContext, WebAppManagementActivity.class);
            startActivity(intent);
            break;

        default:
            break;
        }
        return true;
    }

    /**
     * Android native class working as Javascript interface of WebView
     * @author Cheng Zheng
     *
     */
    private class Navigator implements Intents {
        
        private Context mContext;
        
        public Navigator(Context mContext) {
            super();
            this.mContext = mContext;
        }
        
        public void goBack(String data, String onSuccess) {
            mWebView.goBackWithData(data, onSuccess);
        }

        public void startActivity(String action, String type, String data, String onSuccess) {
            WebIntent webIntent = new WebIntent();
            
            webIntent.action = action;
            webIntent.type = type;
            webIntent.data = data;
            
            webIntent.onSuccess = onSuccess;
            
            startActivity(webIntent);
        }

        @Override
        public void startActivity(final WebIntent webIntent) {

            final Dialog d = new Dialog(mContext);
            d.setContentView(R.layout.dialog_suggested_apps);
            
            d.setTitle("Suggested Applications");
            
            ListView webAppListView = (ListView) d.findViewById(R.id.web_app);
            ListView androidAppListView = (ListView) d.findViewById(R.id.android_app);            
            
            // Create the list of Web apps for selection
            String[] projectionWebIntents = {
                    WebIntentsProvider.WebIntents._ID,    
                    WebIntentsProvider.WebIntents.TITLE,
                    WebIntentsProvider.WebIntents.HREF
            };

			String selectionWebIntents = WebIntentsProvider.WebIntents.ACTION + " = ? and " +
					WebIntentsProvider.WebIntents.TYPE + " = ? and " +
					WebIntentsProvider.WebIntents.BOOKMARKED + " = '1'";
			String[] selectionArgsWebIntents = {
					webIntent.action,
					webIntent.type
			};

            Cursor cursorWebIntents = mContext.getContentResolver().query(WebIntentsProvider.WebIntents.CONTENT_URI, 
                    projectionWebIntents, selectionWebIntents, selectionArgsWebIntents, null);            

            String[] columnWebIntents = {
                    WebIntentsProvider.WebIntents.TITLE,
                    WebIntentsProvider.WebIntents.HREF
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
                    cursor.close();
                    
                    mUIThreadHandler.post(new Runnable() {
                        
                        @Override
                        public void run() {
                            mWebView.loadUrl(href, fWebIntent, false);    
                        }
                        
                    });
                }

            });
            
            // Query the corresponding Android intents and Android data fields of the Web intent
            String[] projectionAndroidIntents = {
                    WebIntentsProvider.WebAndroidMap._ID,
                    WebIntentsProvider.WebAndroidMap.ANDROID_ACTION,
                    WebIntentsProvider.WebAndroidMap.ANDROID_DATA
            };
            
            String selectionAndroidIntents = WebIntentsProvider.WebAndroidMap.WEB_ACTION + " = ? and " +
            		WebIntentsProvider.WebAndroidMap.DATA_TYPE + " = ?";
            String[] selectionArgsAndroidIntents = { 
            		webIntent.action,
            		webIntent.type
            };
            
            Cursor cursorAndroidIntents = mContext.getContentResolver().query(WebIntentsProvider.WebAndroidMap.CONTENT_URI, 
                    projectionAndroidIntents, selectionAndroidIntents, selectionArgsAndroidIntents, null);
            
            // Create the list of Android apps for selection
            PackageManager pm = mContext.getPackageManager();
            
            List<ResolveInfo> androidApps = new ArrayList<ResolveInfo>();
            final List<String> androidDataList = new ArrayList<String>();
            
            if (cursorAndroidIntents != null) {
                while (cursorAndroidIntents.moveToNext()) {
                    String androidAction = cursorAndroidIntents.
                            getString(cursorAndroidIntents.getColumnIndex(WebIntentsProvider.WebAndroidMap.ANDROID_ACTION));
                    String androidData = cursorAndroidIntents.
                            getString(cursorAndroidIntents.getColumnIndex(WebIntentsProvider.WebAndroidMap.ANDROID_DATA));
                    Intent androidIntent = new Intent(androidAction);
                    androidIntent.setType(webIntent.type);
                    List<ResolveInfo> listPerIntent = pm.queryIntentActivities(androidIntent, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo ri : listPerIntent) {                        
                        if (!androidApps.contains(ri)) {
                            androidApps.add(ri);
                            androidDataList.add(androidData);
                        }
                    }
                }
            }
            
            androidAppListView.setAdapter(new AndroidAppArrayAdapter(mContext, androidApps));
            
            androidAppListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position,
                        long id) {  
                    d.dismiss();
                    Adapter adapter = (AndroidAppArrayAdapter) parent.getAdapter();
                    ResolveInfo ri = (ResolveInfo) adapter.getItem(position);
                    Intent intent = new Intent();
                    intent.setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
                    intent.putExtra(androidDataList.get(position), webIntent.data);
                    mContext.startActivity(intent);
                }
            });
            
            d.show();
        }
    }
}