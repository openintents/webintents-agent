package org.openintents.wiagent.ui;

import org.openintents.wiagent.R;
import org.openintents.wiagent.provider.WebIntentsProvider;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ActionBar.Tab;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.FrameLayout;

/**
 * The activity for the menu item Application Management
 * @author Cheng Zheng
 *
 */
public class WebAppManagementActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.fragment_webapp_list);
		
		boolean tablet = false;
		
		// Detect the size of screen, if large or xlarge, the device is a tablet
		switch (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) {
		case Configuration.SCREENLAYOUT_SIZE_LARGE:
		case Configuration.SCREENLAYOUT_SIZE_XLARGE:
			tablet = true;
			break;

		default:
			break;
		}

		final ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		if (tablet) {
			final FrameLayout subcontainer = (FrameLayout) findViewById(R.id.subcontainer);
			// Query the table 'web_intents' to see if there is any new
			// found applications
			AsyncTask<Void, Void, Boolean> hasNewApplicationTask =  new AsyncTask<Void, Void, Boolean> () {

				@Override
				protected Boolean doInBackground(Void... params) {
					String[] projection = {
							WebIntentsProvider.WebIntents._ID
					};
					String selection = WebIntentsProvider.WebIntents.BOOKMARKED + " = '0'";

					Cursor cursor = getContentResolver().query(
							WebIntentsProvider.WebIntents.CONTENT_URI, 
							projection, selection, null, null);

					// Indicate if there is new application found and let the cursor close
					// before returning
					boolean hasNewApp = false;
					if (cursor.moveToNext()) {
						hasNewApp = true;
					}
					cursor.close();
					return hasNewApp;
				}

				@Override
				protected void onPostExecute(Boolean hasNewApp) {
					if (hasNewApp) {
						final WebAppListFragment newWebAppListFragment = WebAppListFragment.newInstance(false);
						ActionBar.Tab newApp = bar.newTab()
								.setText("NEW FOUND")
								.setTabListener(new ActionBar.TabListener() {

									@Override
									public void onTabUnselected(Tab tab, FragmentTransaction ft) {
										ft.remove(newWebAppListFragment);
										subcontainer.removeAllViews();
									}

									@Override
									public void onTabSelected(Tab tab, FragmentTransaction ft) {
										ft.add(R.id.main_container, newWebAppListFragment);
									}

									@Override
									public void onTabReselected(Tab tab, FragmentTransaction ft) {
										ft.add(R.id.main_container, newWebAppListFragment);
									}
								});
						bar.addTab(newApp, 0);
						bar.selectTab(newApp);
					}
				}
			};

			hasNewApplicationTask.execute();

			final WebAppListFragment myWebAppListFragment = WebAppListFragment.newInstance(true);
			bar.addTab(bar.newTab()
					.setText("MY APP")
					.setTabListener(new ActionBar.TabListener() {

						@Override
						public void onTabUnselected(Tab tab, FragmentTransaction ft) {
							ft.remove(myWebAppListFragment);
							subcontainer.removeAllViews();
						}

						@Override
						public void onTabSelected(Tab tab, FragmentTransaction ft) {
							ft.add(R.id.main_container, myWebAppListFragment);
						}

						@Override
						public void onTabReselected(Tab tab, FragmentTransaction ft) {
							ft.add(R.id.main_container, myWebAppListFragment);
						}
					}));
		} else {
			// Query the table 'web_intents' to see if there is any new
			// found applications
			AsyncTask<Void, Void, Boolean> hasNewApplicationTask =  new AsyncTask<Void, Void, Boolean> () {

				@Override
				protected Boolean doInBackground(Void... params) {
					String[] projection = {
							WebIntentsProvider.WebIntents._ID
					};
					String selection = WebIntentsProvider.WebIntents.BOOKMARKED + " = '0'";

					Cursor cursor = getContentResolver().query(
							WebIntentsProvider.WebIntents.CONTENT_URI, 
							projection, selection, null, null);
					
					// Indicate if there is new application found and let the cursor close
					// before returning
					boolean hasNewApp = false;
					if (cursor.moveToNext()) {
						hasNewApp = true;
					}
					cursor.close();
					return hasNewApp;
				}

				@Override
				protected void onPostExecute(Boolean hasNewApp) {
					if (hasNewApp) {
					// Create a new app tab and add it to the first position of 
					// the action bar
						final WebAppListFragment newWebAppListFragment = WebAppListFragment.newInstance(false);
						ActionBar.Tab newApp = bar.newTab()
								.setText("NEW FOUND")
								.setTabListener(new ActionBar.TabListener() {

									@Override
									public void onTabUnselected(Tab tab, FragmentTransaction ft) {
										ft.remove(newWebAppListFragment);
									}

									@Override
									public void onTabSelected(Tab tab, FragmentTransaction ft) {
										// false means the list of Web applications are unbookmarked
										// that is new found application
										ft.add(R.id.main_container, newWebAppListFragment);
									}

									@Override
									public void onTabReselected(Tab tab, FragmentTransaction ft) {
										ft.add(R.id.main_container, newWebAppListFragment);
									}
								});
						bar.addTab(newApp, 0);
						bar.selectTab(newApp);
					}
				}
			};

			hasNewApplicationTask.execute();

			// The tab of my bookmarked applications
			final WebAppListFragment myWebAppListFragment = WebAppListFragment.newInstance(true);
			bar.addTab(bar.newTab()
					.setText("MY APP")
					.setTabListener(new ActionBar.TabListener() {

						@Override
						public void onTabUnselected(Tab tab, FragmentTransaction ft) {
							ft.remove(myWebAppListFragment);
						}

						@Override
						public void onTabSelected(Tab tab, FragmentTransaction ft) {
							ft.add(R.id.main_container, myWebAppListFragment);
						}

						@Override
						public void onTabReselected(Tab tab, FragmentTransaction ft) {
							ft.add(R.id.main_container, myWebAppListFragment);
						}
					}));
		}
	}
}