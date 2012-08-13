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
import org.jsoup.select.Elements;
import org.openintents.wiagent.R;
import org.openintents.wiagent.WebIntent;
import org.openintents.wiagent.provider.WebIntentsProvider;
import org.openintents.wiagent.provider.WebIntentsProvider.LocalServiceDomain;
import org.openintents.wiagent.provider.WebIntentsProvider.WebIntents;
import org.openintents.wiagent.ui.WebAppManagementActivity;
import org.openintents.wiagent.util.HistoryStackEntry;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.webkit.CookieManager;
import android.webkit.WebView;

/**
 * A customized WebView class which adds Web Intents supports before loading Web
 * pages
 * @author Cheng Zheng
 *
 */
public class CustomWebView extends WebView {

	/**
	 * Message code for http url connection thread
	 */
	static final int HANDLER_WHAT_HTTPURLCONNECTIONTHREAD = 1;
	
	/**
	 * Message code for file reader thread
	 */
	static final int HANDLER_WHAT_FILEREADERTHREAD = 2;
	
	/**
	 * Notification id for notifications of new application found in the status bar
	 */
	static final int NOTIFICATION_ID_NEW_APP = 3;

	/**
	 * JavaScript support to inject for Intent and startActivity support
	 */
	public static final String sScriptToInject =
			"var Intent = function(action, type, data) {\n" +                
				"this.action = action;\n" +
				"this.type = type;\n" +
				"this.data = data;\n" +
			"};\n" +
			"window.navigator.startActivity = function(intent, onSuccess) {\n" +
				"if (!onSuccess) { onSuccess = null } else { onSuccess = onSuccess.toString() };\n" +
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

	/**
	 * The urls of iframes in the loaded page
	 */
	private List<String> mIframeUrls;

	private Context mContext;

	/**
	 * A stack keep history pages and their intent object if there is any
	 */
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
	
	/**
	 * Return to the invoking page with data and postexecute script
	 * @param data the result data
	 * @param onSuccess the onSuccess callback in the form of String
	 */
	public void goBackWithData(String data, String onSuccess) {
		if (!mHistoryStack.empty()) {
			// Pop up current url
			mCurrentUrl = ((HistoryStackEntry) mHistoryStack.pop()).url;
			if (!mHistoryStack.empty()) {
				mHistoryUrl = ((HistoryStackEntry) mHistoryStack.pop()).url;
				WebIntent webIntent = new WebIntent();
				webIntent.data = data;
				webIntent.onSuccess = (onSuccess == null ? "" : onSuccess);
				loadUrl(mHistoryUrl, webIntent, true);
			}
		}
	}

	/**
	 * Call loadUrl(String url, WebIntent webIntent, boolean hasResult) with
	 * no WebIntent or result
	 */
	@Override
	public void loadUrl(String url) {
		loadUrl(url, null, false);
	}

	/**
	 * Load a Web page
	 * @param url the location of the page
	 * @param webIntent null means no intent object is bound to window object of
	 * this page, otherwise it should be attached to the property window.intent while
	 * loading the page
	 * @param hasResult true means the page is a resulting page with result data
	 * to process, false not
	 */
	public void loadUrl(String url, WebIntent webIntent, boolean hasResult) {
		mHasResult = hasResult;        
		mCurrentUrl = url;

		if (!mHistoryStack.empty()) {
			// Get the least recently history page with its intent object
			mHistoryUrl = ((HistoryStackEntry) mHistoryStack.peek()).url;
		}

		if (!mCurrentUrl.equals(mHistoryStack)) {
			// If current page is different from the top history page, push it
			// into the history stack
			mHistoryStack.push(new HistoryStackEntry(url, webIntent));
		}

		if (url.indexOf("http") == 0 || url.indexOf("https") == 0) {
			// If the scheme is http or https, start a http connection thread
			HttpURLConnectionThread workerThread = new HttpURLConnectionThread(url, webIntent);
			workerThread.start();
		} else if (url.indexOf("file:") == 0) {
			// If the scheme is file, start a file reader thread
			FileReaderThread workerThread = new FileReaderThread(url, webIntent);
			workerThread.start();
		} else {
			// Invalid url, throw it to the super method
			super.loadUrl(url);
		}
	}

	/**
	 * The inner thread class for http url connection
	 * @author Cheng Zheng
	 *
	 */
	private class HttpURLConnectionThread extends Thread {

		final private String mUrl;
		final private WebIntent mWebIntent; 

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

				Elements webintents = doc.select("intent");

				// If there is any intent tag, register it
				if (webintents != null && webintents.size() != 0) {
					AsyncTask<Elements, Void, Void> registerTask = new AsyncTask<Elements, Void, Void>() {

						@Override
						protected Void doInBackground(Elements... params) {
							boolean hasNew = false; // Indicate if there is any new app found

							Elements webintents = params[0];
							ContentResolver cr = mContext.getContentResolver();

							for (Element webintent : webintents) {
								// Check if the service of this intent tag has been met before

								// If the url is a relative path, use current url as base url
								String href = webintent.attr("href");
								if (!href.contains("http") && !href.contains("https")) {
									if (mUrl.charAt(mUrl.length() - 1) == '/') {
										href = mUrl + href;
									} else {
										StringBuffer buff = new StringBuffer();
										String[] segments = mUrl.split("/");
										for (int i = 0; i < segments.length - 1; i++) {
											buff.append(segments[i] + "/");
										}
										buff.append(href);
										href = buff.toString();
									}
								}

								String[] projection = {
										WebIntentsProvider.WebIntents._ID
								};
								String selection = WebIntentsProvider.WebIntents.ACTION + " = ? and " +
										WebIntentsProvider.WebIntents.TYPE + " = ? and " +
										WebIntentsProvider.WebIntents.HREF + " = ?";
								String[] selectionArgs = {
										webintent.attr("action"),
										webintent.attr("type"),
										href
								}; 

								Cursor cursor = cr.query(WebIntentsProvider.WebIntents.CONTENT_URI, projection, selection, selectionArgs, null);

								if (!cursor.moveToFirst()) {
									cursor = cr.query(WebIntentsProvider.WebIntents.CONTENT_URI, projection, selection, selectionArgs, null);
									if (!cursor.moveToFirst()) {
										ContentValues values = new ContentValues();
										values.put(WebIntents.ACTION, webintent.attr("action"));
										values.put(WebIntents.TYPE, webintent.attr("type"));
										values.put(WebIntents.HREF, href);
										values.put(WebIntents.TITLE, webintent.attr("title"));
										values.put(WebIntents.DISPOSITION, webintent.attr("disposition"));
										cr.insert(WebIntentsProvider.WebIntents.CONTENT_URI, values);
										hasNew = true;
									}
								}

								cursor.close();
							}

							if (hasNew == true) {
								// Build notifications for new apps found
								NotificationManager nfManager = (NotificationManager) mContext
										.getSystemService(Context.NOTIFICATION_SERVICE);

								Intent intent =  new Intent(mContext, WebAppManagementActivity.class);
								PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, intent, 0);

								Notification notification = new Notification.Builder(mContext)
										.setContentTitle("New Applications Found")
										.setSmallIcon(R.drawable.ic_notification)
										.setContentIntent(contentIntent)
										.setAutoCancel(true)
										.getNotification();

								nfManager.notify(NOTIFICATION_ID_NEW_APP, notification);

								// Notify observers of changes of the table web_intents
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
				// Add intent object to the property of window object for this page
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
                
                // If the page contains some result data from services,
                // append onSuccess script the end of body
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
    
	/**
	 * The inner thread class for local file reader
	 * @author Cheng Zheng
	 *
	 */
	private class FileReaderThread extends Thread {

		final private String mUrl;
		final private WebIntent mWebIntent; 

		public FileReaderThread(String url, WebIntent webIntent) {
			super();
			mUrl = url;
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

				// Select the intent tags of this Web page
				Elements webintents = doc.select("intent");

				// If there is any intent tag, register it asynchronously
				if (webintents != null && webintents.size() != 0) {
					AsyncTask<Elements, Void, Void> registerTask = new AsyncTask<Elements, Void, Void>() {

						@Override
						protected Void doInBackground(Elements... params) {
							boolean hasNew = false; // Indicate if there is any new applications

                            Elements webintents = params[0];
                            ContentResolver cr = mContext.getContentResolver();

                            for (Element webintent : webintents) {
                            // Check if the service of this intent tag has been met before
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
                                	ContentValues values = new ContentValues();
                                	values.put(WebIntents.ACTION, webintent.attr("action"));
                                	values.put(WebIntents.TYPE, webintent.attr("type"));
                                	values.put(WebIntents.HREF, webintent.attr("href"));
                                	values.put(WebIntents.TITLE, webintent.attr("title"));
                                	values.put(WebIntents.DISPOSITION, webintent.attr("disposition"));
                                	cr.insert(WebIntentsProvider.WebIntents.CONTENT_URI, values);
                                	hasNew = true;
                                }
                            }

                            if (hasNew) {
                            	// Build notifications for new apps found
                            	NotificationManager nfManager = (NotificationManager) mContext
                            			.getSystemService(Context.NOTIFICATION_SERVICE);
                            	
                            	Intent intent =  new Intent(mContext, WebAppManagementActivity.class);
                            	PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
                            	
                            	Notification notification = new Notification.Builder(mContext)
                            			.setContentTitle("New Applications Found")
                            			.setSmallIcon(R.drawable.ic_notification)
                            			.setContentIntent(contentIntent)
                            			.setAutoCancel(true)
                            			.getNotification();
                            	
                            	nfManager.notify(NOTIFICATION_ID_NEW_APP, notification);

								// Notify observers of changes of the table web_intents
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
                
                // If the page contains some result data from services,
                // append onSuccess script the end of body
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