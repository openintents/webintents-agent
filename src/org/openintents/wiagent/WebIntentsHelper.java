package org.openintents.wiagent;

import java.util.ArrayList;
import java.util.List;

import org.openintents.wiagent.ui.widget.AndroidAppArrayAdapter;
import org.openintents.wiagent.ui.widget.WebAppArrayAdapter;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * This is a helper class for external Android apps to invoke Web apps through
 * Web Intents.
 * @author Cheng Zheng
 *
 */
public class WebIntentsHelper {

	/**
	 * The context information of the component or application who creates 
	 * an instance of this class. The keyword final makes it thread-safe.
	 */
	final private Context mContext;

	public WebIntentsHelper(Context mContext) {
		this.mContext = mContext;
	}

	/**
	 * The external Android app can call this method to invoke Web Intents
	 * @param intent A Android intent
	 */
	public void createChooserWithWebActivities(Intent intent) {
		final Intent oldIntent = intent;

		// Get the corresponding web action and android data by android action and data type. 
		// The android data field specifies how to constitute a web intents data field from 
		// android data
		AsyncTask<Void, Void, ArrayList<WebApp>> queryWebApp = new AsyncTask<Void, Void, ArrayList<WebApp>>(){

			@Override
			protected ArrayList<WebApp> doInBackground(Void... params) {
				ContentResolver cr = mContext.getContentResolver();

				Uri uri = Uri.parse("content://org.openintents.wiagent/web_android_map");
				String[] projection = {
						"web_action",
						"data_map_scheme"
				};
				String selection = "android_action = ? and data_type = ?";
				String[] selectionArgs = {
						oldIntent.getAction(),
						oldIntent.getType()
				};

				Cursor cursor = cr.query(uri, projection, selection, selectionArgs, null);

				ArrayList<String> webActionList = new ArrayList<String>();
				
				if (cursor.moveToNext()) {
					webActionList.add(cursor.getString(cursor.getColumnIndex("web_action")));
					
					// Put webintents_data from Android intents in oldIntent
					oldIntent.putExtra("webintents_data", oldIntent.getStringExtra(
							cursor.getString(cursor.getColumnIndex("data_map_scheme"))));
				}
				
				while (cursor.moveToNext()) {
					webActionList.add(cursor.getString(cursor.getColumnIndex("web_action")));
				}
				
				cursor.close();
				
				// For each Web action, get its eligible web applications
				ArrayList<WebApp> webAppList = new ArrayList<WebApp>();
				for (String webAction : webActionList) {
					uri = Uri.parse("content://org.openintents.wiagent/web_intents");
					projection = new String[2];
					projection[0] = "href";
					projection[1] = "title";
					selection = "action = ? and type = ? and bookmarked = '1'";
					selectionArgs = new String[2];
					selectionArgs[0] = webAction;
					selectionArgs[1] = oldIntent.getType();

					cursor = cr.query(uri, projection, selection, selectionArgs, null);

					while (cursor.moveToNext()) {
						String href = cursor.getString(cursor.getColumnIndex("href"));
						String title = cursor.getString(cursor.getColumnIndex("title"));
						WebApp webApp = new WebApp(title, href);
						if (!webAppList.contains(webApp)) {
							webAppList.add(webApp);
						}
					}

					cursor.close();
				}

				return webAppList;
			}

			@Override
			protected void onPostExecute(ArrayList<WebApp> webAppList) {				
				// Create the content view of the chooser dialog from code
				LinearLayout layout = new LinearLayout(mContext);
				layout.setLayoutParams(new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT, 
						LinearLayout.LayoutParams.MATCH_PARENT));
				layout.setOrientation(LinearLayout.VERTICAL);

				TextView webAppListHeader = new TextView(mContext);
				webAppListHeader.setLayoutParams(new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT, 
						LinearLayout.LayoutParams.WRAP_CONTENT));
				webAppListHeader.setGravity(Gravity.CENTER_HORIZONTAL);
				webAppListHeader.setTextAppearance(mContext, 
						android.R.style.TextAppearance_Holo_Large);
				webAppListHeader.setText("Web Applications");

				ListView webAppListView = new ListView(mContext);
				webAppListView.setLayoutParams(new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT, 
						LinearLayout.LayoutParams.WRAP_CONTENT));

				TextView androidAppListHeader = new TextView(mContext);
				androidAppListHeader.setLayoutParams(new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT, 
						LinearLayout.LayoutParams.WRAP_CONTENT));
				androidAppListHeader.setGravity(Gravity.CENTER_HORIZONTAL);
				androidAppListHeader.setTextAppearance(mContext, 
						android.R.style.TextAppearance_Holo_Large);
				androidAppListHeader.setText("Android Applications");

				ListView androidAppListView = new ListView(mContext);
				androidAppListView.setLayoutParams(new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT, 
						LinearLayout.LayoutParams.WRAP_CONTENT));

				layout.addView(webAppListHeader);
				layout.addView(webAppListView);
				layout.addView(androidAppListHeader);
				layout.addView(androidAppListView);

				final Dialog dialog = new Dialog(mContext);
				dialog.setContentView(layout);

				// Create the list of Web apps for selection
				final WebAppArrayAdapter webAppArrayAdapter = new WebAppArrayAdapter(mContext, webAppList);
				webAppListView.setAdapter(webAppArrayAdapter);

				webAppListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						dialog.dismiss();

						WebApp webApp = webAppArrayAdapter.getItem(position);
						Intent newIntent = new Intent();
						newIntent.putExtra("href", webApp.href);
						newIntent.putExtra("action", oldIntent.getAction());
						newIntent.putExtra("type", oldIntent.getType());
						newIntent.putExtra("data", oldIntent.getStringExtra("webintents_data"));
						newIntent.setComponent(new ComponentName("org.openintents.wiagent", 
								"org.openintents.wiagent.ui.WebIntentsAgentActivity"));

						mContext.startActivity(newIntent);
					}
				});

				// Create the list of Android apps for selection. Use oldIntent as query intent
				// committed to the package manager
				List<ResolveInfo> androidAppList = mContext.getPackageManager().
						queryIntentActivities(oldIntent, PackageManager.MATCH_DEFAULT_ONLY);

				AndroidAppArrayAdapter androidAppArrayAdapter = new AndroidAppArrayAdapter(mContext, androidAppList);
				androidAppListView.setAdapter(androidAppArrayAdapter);

				androidAppListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position,
							long id) {
						dialog.dismiss();

						Adapter adapter = (AndroidAppArrayAdapter) parent.getAdapter();
						ResolveInfo ri = (ResolveInfo) adapter.getItem(position);                        
						oldIntent.setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
						mContext.startActivity(oldIntent);
					}
				});

				dialog.setTitle("Suggested Applications");
				dialog.show();
			}
		};

		queryWebApp.execute();
	}
}
