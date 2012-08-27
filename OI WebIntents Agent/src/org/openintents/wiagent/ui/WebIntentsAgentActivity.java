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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * This is the main activity launched by OI WebIntents Agent. It
 * contains a custom WebView which injects scripts supporting Web Intents while
 * loading a Web page.
 *
 */
public class WebIntentsAgentActivity extends Activity {
	
	/**
	 * The custom WebView, loading and displaying a Web page
	 */
	private CustomWebView mWebView;
	
	/**
	 * An input view for displaying the url when loading a Web page, 
	 * not editable yet.
	 */
	// TODO: Add url input and loading function
	private UrlInputView mUrlInputView;

	/**
	 * The uploaded file callback for the file chooser of a web page
	 */
	private ValueCallback<Uri> mUploadFile;
	private final static int FILECHOOSER_RESULTCODE = 1;

	/**
	 * A thread handler attached to the UI thread. This handler can help a forked thread
	 * post some executive code to the UI thread.
	 */
	private Handler mUIThreadHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mUIThreadHandler = new Handler();

		setContentView(R.layout.main);

		// Get views of the main layout
		mWebView = (CustomWebView) findViewById(R.id.webview);
		mUrlInputView = (UrlInputView) findViewById(R.id.url);
		final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);

		// Set context of the webview, enable 
		// Javascript support and zoom controls
		mWebView.setContext(this);		
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.getSettings().setBuiltInZoomControls(true);

		mWebView.setWebViewClient(new WebViewClient() { 

			/**
			 * Intercept the requests of resources in iframes and add Web Intents support
			 * to them before loading into the webview
			 */
			@Override
			public WebResourceResponse shouldInterceptRequest(WebView view,
					String url) {
				CustomWebView v = (CustomWebView) view;

				// If no iframes in the loaded web page return ordinary super method
				if (v.getIframeUrls() == null) return super.shouldInterceptRequest(view, url);

				// If the web page contains the resource represented by the url,
				// use the following block to
				// request them and inject Web Intents supports, else return the ordinary super
				// function
				if (v.getIframeUrls().contains(url)) {
					// Http connection and the input stream bound to it
					HttpURLConnection urlConnection = null;
					InputStream in = null;

					try {
						urlConnection = (HttpURLConnection) (new URL(url)).openConnection(); 

						// Set cookies in the request message
						CookieManager cookieManager = CookieManager.getInstance();
						String cookie = cookieManager.getCookie(urlConnection.getURL().toString());
						if (cookie != null) {
							urlConnection.setRequestProperty("Cookie", cookie);
						}
						
						// Make a http request so that the response message is loaded in the connection
						urlConnection.connect();

						// Get cookies from the response message and save them in the cookie manager
						List<String> cookieList = urlConnection.getHeaderFields().get("Set-Cookie");
						if (cookieList != null) {
							for (String cookieTemp : cookieList) {
								cookieManager.setCookie(urlConnection.getURL().toString(), cookieTemp);
							}
						}

						// Get the input stream for the page data in the response message
						in = new BufferedInputStream (urlConnection.getInputStream());

						// Use Jsoup to parse the document
						Document doc = Jsoup.parse(in, "UTF-8", url.toString());

						// Remove the shim files. The shim files added by webintents.org
						// may cause exceptions when triggering the injected scripts by this block
						Elements scriptsWithSrc = doc.select("script[src]");
						for (Element scriptWithSrc : scriptsWithSrc) {
							String value = scriptWithSrc.attr("src");
							if (value.contains("webintents.min.js") ||
									value.contains("webintents-prefix.js")) {
								scriptWithSrc.remove();
							}
						}

						// The script supporting a Javascript Intent class and the
						// startActivity method for the window object
						String scriptToInject = CustomWebView.sScriptToInject;

						// Prepend the script to the head element of the web page
						doc.head().prependElement("script")
							.attr("type", "text/javascript")
							.html(scriptToInject);
						
						// Transform the result from String to InputStream
						in = new ByteArrayInputStream(doc.outerHtml().getBytes("UTF-8"));                         
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						// Close the connection
						if (urlConnection != null) {
							urlConnection.disconnect();
						} 
					}

					// Return the result if there exists
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
				// Set for displaying the loading url in the url view
				mUrlInputView.setText(url);

				// Load the new url
				view.loadUrl(url);

				return true;
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				// Set the progress bar visible before starting to load the page
				progressBar.setVisibility(View.VISIBLE);
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				// Set the progress bar to invisible and take no space when the loading is
				// completed
				progressBar.setVisibility(View.GONE);
			}

		});

		mWebView.setWebChromeClient(new WebChromeClient() {

			// Overwrite the file chooser of WebView. This method is
			// hidden by the document of Android SDK
			public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType) {
				// Set the uploaded file callback for other method use
				mUploadFile = uploadFile;

				// Start an application for choosing the file for uploading
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);  
				intent.addCategory(Intent.CATEGORY_OPENABLE);  
				intent.setType(acceptType);                
				WebIntentsAgentActivity.this.startActivityForResult(intent.
						createChooser(intent, "Choose file using"), FILECHOOSER_RESULTCODE);
			}

			/**
			 * Set animation when loading a new Web page
			 */
			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				// Set the activity title to loading when loading is in progress
				WebIntentsAgentActivity.this.setTitle("Loading ...");

				// Set the value of progress bar to show th progress
				progressBar.setProgress((newProgress * progressBar.getMax()) / 100);

				// Whe the loading is completed, reset the application name
				if (newProgress == 100) {
					WebIntentsAgentActivity.this.setTitle(R.string.app_name);
				}
			}
		});

		// Add native Java code support for handling window.navigator functions to support
		// Web Intents
		mWebView.addJavascriptInterface(new Navigator(this), "navigatorAndroid");

		// Support for external Android app to load Web Intents.
		// If the activity is launched by an external app, its relevant data should be put
		// into fields of a WebIntent instance
		Intent intent = getIntent();		
		if (intent.getAction() == null && intent.getStringExtra("action") != null) {
		// This activity is launched by external Android application
			WebIntent webIntent = new WebIntent();
			webIntent.action = intent.getStringExtra("action");
			webIntent.data = intent.getStringExtra("data");
			webIntent.type = intent.getStringExtra("type");
			String href = intent.getStringExtra("href");
			mWebView.loadUrl(href, webIntent, false);
		} else {
		// This activity is launched by OI WebIntents Agent
			mWebView.loadUrl(getString(R.string.home_url));
		}
	}

	/**
	 * Process the activity result for choosing a file. This file is
	 * used in the file chooser of a Web page
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			android.content.Intent data) {
		switch (requestCode) {	// Test the request code of the activity
		case FILECHOOSER_RESULTCODE:
			Uri result = null;
			if (resultCode == RESULT_OK) {
				// Get the returning data from the intent
				result = data.getData();

				// Load the data to the uploadfile callback
				mUploadFile.onReceiveValue(result);
			}
			break;

		default:
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.nav_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_backward:	// Backward to the previous page if possible
			if (mWebView.canGoBack()) {
				mWebView.goBack();
			}
			break;

		case R.id.menu_forward:	// Forward to the next page if possible
			if (mWebView.canGoForward()) {
				mWebView.goForward();
			}
			break;

		case R.id.menu_home:	// Go to the home page
			mUrlInputView.setText(getString(R.string.home_url));
			mWebView.loadUrl(getString(R.string.home_url));
			break;

		case R.id.menu_refresh:	// Refresh the current page
			mWebView.reload();
			break;

		case R.id.menu_app_management:	// Start the application management activity
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
	 * Java native class working as Javascript interface of WebView to 
	 * suppport Web Intents	 *
	 */
	private class Navigator implements Intents {

		private Context mContext;

		public Navigator(Context mContext) {
			this.mContext = mContext;
		}

		/**
		 * The interface for Javascript goBack method
		 * Go back to the previous page invoking navigator.startActivity
		 * @param data the result data
		 * @param onSuccess the code to execute when the calling is successful
		 */
		public void goBack(String data, String onSuccess) {
			mWebView.goBackWithData(data, onSuccess);
		}

		/**
		 * The interface for Javascript startActivity method
		 * @param action Web Intents action
		 * @param type Web Intents data type, in the form of MIME type
		 * @param data Web Intents data to process
		 * @param onSuccess The code to execute when invoking the Web intent is successful
		 */
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

			// Create the application choose dialog
			final Dialog d = new Dialog(mContext);
			d.setContentView(R.layout.dialog_suggested_apps);

			d.setTitle("Suggested Applications");

			// Get list views for respective web applicaiton list and android 
			// application list
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
					WebIntentsProvider.WebIntents.BOOKMARKED + " = '1' and " +
					WebIntentsProvider.WebIntents.REMOVED + " = '0'";
			String[] selectionArgsWebIntents = {
					webIntent.action,
					webIntent.type
			};

			Cursor cursorWebIntents = mContext.getContentResolver().query(WebIntentsProvider.WebIntents.CONTENT_URI, 
					projectionWebIntents, selectionWebIntents, selectionArgsWebIntents, null);

			if (cursorWebIntents.getCount() == 0) {
				TextView webAppListHeader =  (TextView) d.findViewById(R.id.web_app_header);
				webAppListHeader.setVisibility(View.GONE);
			} else {
				String[] columnWebIntents = {
						WebIntentsProvider.WebIntents.TITLE,
						WebIntentsProvider.WebIntents.HREF
				};

				int[] mViewIDs = {
						R.id.webapp_title,
						R.id.webapp_href
				};
				
				webAppListView.setAdapter(new SimpleCursorAdapter(mContext, 
						R.layout.list_item_webapp, cursorWebIntents, columnWebIntents, mViewIDs, 0));
				
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
			}

			// Query the corresponding Android intents and Android data fields of the Web intent
			String[] projectionAndroidIntents = {
					WebIntentsProvider.WebAndroidMap._ID,
					WebIntentsProvider.WebAndroidMap.ANDROID_ACTION,
					WebIntentsProvider.WebAndroidMap.DATA_MAP_SCHEME
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
							getString(cursorAndroidIntents.getColumnIndex(WebIntentsProvider.WebAndroidMap.DATA_MAP_SCHEME));
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

			if (androidApps.size() == 0) {
				TextView androidAppListHeader =  (TextView) d.findViewById(R.id.android_app_header);
				androidAppListHeader.setVisibility(View.GONE);
			} else {
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
			}

			d.show();
		}
	}
}