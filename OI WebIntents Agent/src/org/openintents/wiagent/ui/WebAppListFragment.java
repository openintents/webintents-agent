package org.openintents.wiagent.ui;

import java.util.ArrayList;

import org.openintents.wiagent.R;
import org.openintents.wiagent.WebApp;
import org.openintents.wiagent.provider.WebIntentsProvider;
import org.openintents.wiagent.ui.widget.CheckedWebAppArrayAdapter;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

/**
 * The fragement for diplay, add and remove Web apps
 * @author Cheng Zheng
 *
 */
public class WebAppListFragment extends ListFragment {
	
	public static final int FLAG_NEW_FOUND = 1;
	
	public static final int FLAG_MYAPP = 2;
	
	public static final int FLAG_TRASH = 3;

	private static final String ARG_TAG_FLAG = "flag";

	private WebIntentsByAppListFragment mWebIntentsByAppListFragment;

	private boolean mTablet = false;

	public static WebAppListFragment newInstance(int flag) {
		WebAppListFragment f = new WebAppListFragment();

		// Save the indicator for later use, if bookmarked is true, this fragment is
		// for my app list, otherwise, new app list
		Bundle args = new Bundle();
		args.putInt(ARG_TAG_FLAG, flag);
		f.setArguments(args);

		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Indicate that the fragment has menu to contribute
		setHasOptionsMenu(true);

		// Detect the size of screen, if large or xlarge, the device is a tablet
		switch (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) {
		case Configuration.SCREENLAYOUT_SIZE_LARGE:
		case Configuration.SCREENLAYOUT_SIZE_XLARGE:
			mTablet = true;
			break;

		default:
			break;
		}

		AsyncTask<Void, Void, ArrayList<WebApp>> appQueryTask = new AsyncTask<Void, Void, ArrayList<WebApp>>() {

			@Override
			protected ArrayList<WebApp> doInBackground(Void... params) {

				String[] projection = {
						"DISTINCT " + WebIntentsProvider.WebIntents.TITLE,
						WebIntentsProvider.WebIntents.HREF
				};

				String selection;

				int flag = getArguments().getInt(ARG_TAG_FLAG);
				switch (flag) {
				case FLAG_NEW_FOUND:
					selection = WebIntentsProvider.WebIntents.BOOKMARKED + " = '0' and " +
							WebIntentsProvider.WebIntents.REMOVED + " = '0'";
					break;
					
				case FLAG_MYAPP:
					selection = WebIntentsProvider.WebIntents.BOOKMARKED + " = '1' and " +
							WebIntentsProvider.WebIntents.REMOVED + " = '0'";
					break;
					
				case FLAG_TRASH:
				default:
					selection = WebIntentsProvider.WebIntents.REMOVED + " = '1'";
					break;
				}

				Cursor cursor = getActivity().getContentResolver().query(
						WebIntentsProvider.WebIntents.CONTENT_URI, 
						projection, selection, null, null);

				ArrayList<WebApp> webAppList = new ArrayList<WebApp>();
				while (cursor.moveToNext()) {
					webAppList.add(new WebApp(
							cursor.getString(cursor.getColumnIndex(WebIntentsProvider.WebIntents.TITLE)),
							cursor.getString(cursor.getColumnIndex(WebIntentsProvider.WebIntents.HREF))));
				}
				
				cursor.close();

				return webAppList;
			}

			@Override
			protected void onPostExecute(ArrayList<WebApp> webAppList) {
				if (webAppList != null && getActivity() != null) {
					setListAdapter(new CheckedWebAppArrayAdapter(getActivity(), webAppList));
				}				
			}
		};

		appQueryTask.execute();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Get the list of checked apps
		ArrayList<String> checkedApps = new ArrayList<String>();
		CheckedWebAppArrayAdapter arrayAdapter = (CheckedWebAppArrayAdapter) getListAdapter();
		ArrayList<Boolean> checkList = arrayAdapter.mCheckList;
		for (int i = 0; i < checkList.size(); i++) {
			if (checkList.get(i)) {
				WebApp webApp = (WebApp) getListAdapter().getItem(i);
				checkedApps.add(webApp.href);
			}
		}

		ContentResolver cr = getActivity().getContentResolver();
		switch (item.getItemId()) {
		case R.id.menu_add:
			if (checkedApps.size() != 0) {
				for (String checkedApp : checkedApps) {
					String selection = WebIntentsProvider.WebIntents.HREF + " = ?";
					String[] selectionArgs = {
							checkedApp
					};

					ContentValues values = new ContentValues();
					values.put(WebIntentsProvider.WebIntents.BOOKMARKED, "1");

					cr.update(WebIntentsProvider.WebIntents.CONTENT_URI, values, selection, selectionArgs);
				}

				if (checkedApps.size() == getListAdapter().getCount()) {
					// If no app left, clear the tab in CAB
					getActivity().getActionBar().removeTabAt(0);
				} else {
					// Check if there still are some new apps, refresh the list view accordingly
					AsyncTask<Void, Void, ArrayList<WebApp>> newAppQueryTask = new AsyncTask<Void, Void, ArrayList<WebApp>>() {

						@Override
						protected ArrayList<WebApp> doInBackground(
								Void... params) {
							String[] projection = {
									"DISTINCT " + WebIntentsProvider.WebIntents.TITLE, 
									WebIntentsProvider.WebIntents.HREF
							};
							String selection = WebIntentsProvider.WebIntents.BOOKMARKED + " = '0' and " +
									WebIntentsProvider.WebIntents.REMOVED + " = '0'";

							Cursor cursor = getActivity().getContentResolver().query(
									WebIntentsProvider.WebIntents.CONTENT_URI, 
									projection, selection, null, null);

							ArrayList<WebApp> webAppList = new ArrayList<WebApp>();
							while (cursor.moveToNext()) {
								webAppList.add(new WebApp(
										cursor.getString(cursor.getColumnIndex(WebIntentsProvider.WebIntents.TITLE)),
										cursor.getString(cursor.getColumnIndex(WebIntentsProvider.WebIntents.HREF))));
							}

							cursor.close();

							return webAppList;
						}

						@Override
						protected void onPostExecute(ArrayList<WebApp> webAppList) {
							if (webAppList != null && getActivity() != null) {
								setListAdapter(new CheckedWebAppArrayAdapter(getActivity(), webAppList));
							}

							// For tablet devices
							if (mTablet) {
								// Clear the subwindow
								if (mWebIntentsByAppListFragment != null) {
									FragmentTransaction ft = getFragmentManager().beginTransaction();
									ft.remove(mWebIntentsByAppListFragment);
									ft.commit();
								}
							}
						}
					};

					newAppQueryTask.execute();
				}
			}

			break;

		case R.id.menu_subtract:
			if (checkedApps.size() != 0) {
				for (String checkedApp : checkedApps) {
					String selection = WebIntentsProvider.WebIntents.HREF + " = ?";
					String[] selectionArgs = {
							checkedApp
					};

					ContentValues values = new ContentValues();
					values.put(WebIntentsProvider.WebIntents.REMOVED, "1");

					cr.update(WebIntentsProvider.WebIntents.CONTENT_URI, values, selection, selectionArgs);                                   
				}

				// Check if there still are some new apps, refresh the list view accordingly
				AsyncTask<Void, Void, ArrayList<WebApp>> myAppQueryTask = new AsyncTask<Void, Void, ArrayList<WebApp>>() {

					@Override
					protected ArrayList<WebApp> doInBackground(
							Void... params) {
						String[] projection = {
								"DISTINCT " + WebIntentsProvider.WebIntents.TITLE, 
								WebIntentsProvider.WebIntents.HREF
						};
						String selection = WebIntentsProvider.WebIntents.BOOKMARKED + " = '1' and " +
								WebIntentsProvider.WebIntents.REMOVED + " = '0'";
		
						Cursor cursor = getActivity().getContentResolver().query(
								WebIntentsProvider.WebIntents.CONTENT_URI, 
								projection, selection, null, null);
		
						ArrayList<WebApp> webAppList = new ArrayList<WebApp>();
						while (cursor.moveToNext()) {
							webAppList.add(new WebApp(
									cursor.getString(cursor.getColumnIndex(WebIntentsProvider.WebIntents.TITLE)),
									cursor.getString(cursor.getColumnIndex(WebIntentsProvider.WebIntents.HREF))));
						}
						
						cursor.close();

						return webAppList;
					}

					@Override
					protected void onPostExecute(ArrayList<WebApp> webAppList) {
						if (webAppList != null && getActivity() != null) {
							setListAdapter(new CheckedWebAppArrayAdapter(getActivity(), webAppList));
						}

						if (mTablet) {
							// Clear the subwindow
							if (mWebIntentsByAppListFragment != null) {
								FragmentTransaction ft = getFragmentManager().beginTransaction();
								ft.remove(mWebIntentsByAppListFragment);
								ft.commit();
							}
						}
					}
				};

				myAppQueryTask.execute();
			}

			break;

		case R.id.menu_restore:
			if (checkedApps.size() != 0) {
				for (String checkedApp : checkedApps) {
					String selection = WebIntentsProvider.WebIntents.HREF + " = ?";
					String[] selectionArgs = {
							checkedApp
					};

					ContentValues values = new ContentValues();
					values.put(WebIntentsProvider.WebIntents.REMOVED, "0");

					cr.update(WebIntentsProvider.WebIntents.CONTENT_URI, values, selection, selectionArgs);
				}

				// Check if there still are some new apps, refresh the list view accordingly
				AsyncTask<Void, Void, ArrayList<WebApp>> trashAppQueryTask = new AsyncTask<Void, Void, ArrayList<WebApp>>() {

					@Override
					protected ArrayList<WebApp> doInBackground(
							Void... params) {
						String[] projection = {
								"DISTINCT " + WebIntentsProvider.WebIntents.TITLE, 
								WebIntentsProvider.WebIntents.HREF
						};
						String selection = WebIntentsProvider.WebIntents.REMOVED + " = '1'";

						Cursor cursor = getActivity().getContentResolver().query(
								WebIntentsProvider.WebIntents.CONTENT_URI, 
								projection, selection, null, null);

						ArrayList<WebApp> webAppList = new ArrayList<WebApp>();
						while (cursor.moveToNext()) {
							webAppList.add(new WebApp(
									cursor.getString(cursor.getColumnIndex(WebIntentsProvider.WebIntents.TITLE)),
									cursor.getString(cursor.getColumnIndex(WebIntentsProvider.WebIntents.HREF))));
						}
						
						cursor.close();

						return webAppList;
					}

					@Override
					protected void onPostExecute(ArrayList<WebApp> webAppList) {
						if (webAppList != null && getActivity() != null) {
							setListAdapter(new CheckedWebAppArrayAdapter(getActivity(), webAppList));
						}

						if (mTablet) {
							// Clear the subwindow
							if (mWebIntentsByAppListFragment != null) {
								FragmentTransaction ft = getFragmentManager().beginTransaction();
								ft.remove(mWebIntentsByAppListFragment);
								ft.commit();
							}
						}
					}
				};

				trashAppQueryTask.execute();
				
				break;
			}

		case R.id.menu_select_all:
			arrayAdapter.setAllChecked(true);
			arrayAdapter.notifyDataSetChanged();

			break;

		case R.id.menu_restore_all:
			String selection = WebIntentsProvider.WebIntents.REMOVED + " = '1'";

			ContentValues values = new ContentValues();
			values.put(WebIntentsProvider.WebIntents.REMOVED, "0");

			cr.update(WebIntentsProvider.WebIntents.CONTENT_URI, values, selection, null);

			// Check if there still are some new apps, refresh the list view accordingly
			AsyncTask<Void, Void, ArrayList<WebApp>> trashAppQueryTask = new AsyncTask<Void, Void, ArrayList<WebApp>>() {

				@Override
				protected ArrayList<WebApp> doInBackground(
						Void... params) {
					String[] projection = {
							"DISTINCT " + WebIntentsProvider.WebIntents.TITLE, 
							WebIntentsProvider.WebIntents.HREF
					};
					String selection = WebIntentsProvider.WebIntents.REMOVED + " = '1'";

					Cursor cursor = getActivity().getContentResolver().query(
							WebIntentsProvider.WebIntents.CONTENT_URI, 
							projection, selection, null, null);

					ArrayList<WebApp> webAppList = new ArrayList<WebApp>();
					while (cursor.moveToNext()) {
						webAppList.add(new WebApp(
								cursor.getString(cursor.getColumnIndex(WebIntentsProvider.WebIntents.TITLE)),
								cursor.getString(cursor.getColumnIndex(WebIntentsProvider.WebIntents.HREF))));
					}
					
					cursor.close();

					return webAppList;
				}

				@Override
				protected void onPostExecute(ArrayList<WebApp> webAppList) {
					if (webAppList != null && getActivity() != null) {
						setListAdapter(new CheckedWebAppArrayAdapter(getActivity(), webAppList));
					}

					if (mTablet) {
						// Clear the subwindow
						if (mWebIntentsByAppListFragment != null) {
							FragmentTransaction ft = getFragmentManager().beginTransaction();
							ft.remove(mWebIntentsByAppListFragment);
							ft.commit();
						}
					}
				}
			};

			trashAppQueryTask.execute();

			break;

		default:
			break;
		}
		return true;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		int flag = getArguments().getInt(ARG_TAG_FLAG);
		switch (flag) {
		case FLAG_MYAPP:
			inflater.inflate(R.menu.app_myapp, menu);
			break;

		case FLAG_NEW_FOUND:
			inflater.inflate(R.menu.app_newapp, menu);
			break;

		case FLAG_TRASH:
			inflater.inflate(R.menu.app_trash, menu);
			break;

		default:
			break;
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		WebApp webApp = (WebApp) getListAdapter().getItem(position);

		if (mTablet) {
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			mWebIntentsByAppListFragment = WebIntentsByAppListFragment.newInstance(webApp.href);
			ft.replace(R.id.subcontainer, mWebIntentsByAppListFragment);
			ft.commit();
		} else {
			Intent intent = new Intent(getActivity().getApplicationContext(), WebIntentsByAppActivity.class);
			intent.putExtra("href", webApp.href);
			intent.putExtra(ARG_TAG_FLAG, getArguments().getInt(ARG_TAG_FLAG));
			startActivity(intent);
		}
	}
}