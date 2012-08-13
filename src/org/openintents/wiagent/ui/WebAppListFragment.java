package org.openintents.wiagent.ui;

import java.util.ArrayList;

import org.openintents.wiagent.R;
import org.openintents.wiagent.WebApp;
import org.openintents.wiagent.provider.WebIntentsProvider;
import org.openintents.wiagent.ui.widget.CheckedWebAppArrayAdapter;
import org.openintents.wiagent.ui.widget.WebAppArrayAdapter;

import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

/**
 * The fragement for diplay, add and remove Web apps
 * @author Cheng Zheng
 *
 */
public class WebAppListFragment extends ListFragment {

	private static final String ARG_TAG_BOOKMARKED = "bookmarked";

	private WebIntentsByAppListFragment mWebIntentsByAppListFragment;

	private boolean mTablet = false;
	
	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
		
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) { }
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();

			// Set action items as per if this is for bookmarked apps
			boolean bookmarked = getArguments().getBoolean(ARG_TAG_BOOKMARKED);
			if (bookmarked) {
				inflater.inflate(R.menu.cab_myapp, menu);
			} else {
				inflater.inflate(R.menu.cab_newapp, menu);
			}

			return true;
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			// Get the list of checked apps
			ArrayList<String> checkedApps = new ArrayList<String>();
			ArrayList<Boolean> checkList = ((CheckedWebAppArrayAdapter) getListAdapter()).mCheckList;			
			for (int i = 0; i < getListAdapter().getCount(); i++) {
				if (checkList.get(i)) {
					WebApp webApp = (WebApp) getListAdapter().getItem(i);
					checkedApps.add(webApp.href);
				}
			}

			ContentResolver cr = getActivity().getContentResolver();
			int count = 0;

			switch (item.getItemId()) {
			case R.id.menu_add:
				for (String checkedApp : checkedApps) {
					String selection = WebIntentsProvider.WebIntents.HREF + " = ?";
					String[] selectionArgs = {
							checkedApp
					};
					
					ContentValues values = new ContentValues();
					values.put(WebIntentsProvider.WebIntents.BOOKMARKED, "1");

					count += cr.update(WebIntentsProvider.WebIntents.CONTENT_URI, values, selection, selectionArgs);                                    
				}

				// If there is any update, notify registered observers.
				if (count > 0) {
					cr.notifyChange(WebIntentsProvider.WebIntents.CONTENT_URI, null);
				}

				if (checkedApps.size() == getListAdapter().getCount()) {
					// If no app left, clear the tab in CAB
					getActivity().getActionBar().removeTabAt(0);
				} else {
					// Check if there still are some new apps, refresh the list view accordingly
					AsyncTask<Void, Void, Cursor> newAppQueryTask = new AsyncTask<Void, Void, Cursor>() {

						@Override
						protected Cursor doInBackground(
								Void... params) {
							String[] projection = {
									"DISTINCT " + WebIntentsProvider.WebIntents.TITLE,
									WebIntentsProvider.WebIntents.HREF + " as _id"
							};
							String selection = WebIntentsProvider.WebIntents.BOOKMARKED + " = '0'";

							Cursor cursor = getActivity().getContentResolver().query(
									WebIntentsProvider.WebIntents.CONTENT_URI, 
									projection, selection, null, null);

							return cursor;
						}

						@Override
						protected void onPostExecute(Cursor cursor) {
							String[] from = {
									WebIntentsProvider.WebIntents.TITLE,
									"_id"
							};

							int[] to = {
									android.R.id.text1,
									android.R.id.text2
							};

							setListAdapter(new SimpleCursorAdapter(getActivity(), 
									android.R.layout.simple_list_item_2, cursor, 
									from, to, 0));

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

				break;

			case R.id.menu_remove:
				for (String checkedApp : checkedApps) {
					String selection = WebIntentsProvider.WebIntents.HREF + " = ?";
					String[] selectionArgs = {
							checkedApp
					};
					
					count += cr.delete(WebIntentsProvider.WebIntents.CONTENT_URI, selection, selectionArgs);                                   
				}

				// If there is any update, notify registered observers.
				if (count > 0) {
					cr.notifyChange(WebIntentsProvider.WebIntents.CONTENT_URI, null);
				}

				// Check if there still are some new apps, refresh the list view accordingly
				AsyncTask<Void, Void, Cursor> myAppQueryTask = new AsyncTask<Void, Void, Cursor>() {

					@Override
					protected Cursor doInBackground(
							Void... params) {
						String[] projection = {
								"DISTINCT " + WebIntentsProvider.WebIntents.TITLE,
								WebIntentsProvider.WebIntents.HREF + " as _id"
						};
						String selection = WebIntentsProvider.WebIntents.BOOKMARKED + " = '1'";

						Cursor cursor = getActivity().getContentResolver().query(
								WebIntentsProvider.WebIntents.CONTENT_URI, 
								projection, selection, null, null);

						return cursor;
					}

					@Override
					protected void onPostExecute(Cursor cursor) {
						String[] from = {
								WebIntentsProvider.WebIntents.TITLE,
								"_id"
						};

						int[] to = {
								android.R.id.text1,
								android.R.id.text2
						};

						setListAdapter(new SimpleCursorAdapter(getActivity(), 
								android.R.layout.simple_list_item_2, cursor, 
								from, to, 0));

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

				break;

			default:
				break;
			}

			mode.finish();

			return false;
		}
	};

	public static WebAppListFragment newInstance(boolean bookmarked) {
		WebAppListFragment f = new WebAppListFragment();

		// Save the indicator for later use, if bookmarked is true, this fragment is
		// for my app list, otherwise, new app list
		Bundle args = new Bundle();
		args.putBoolean(ARG_TAG_BOOKMARKED, bookmarked);
		f.setArguments(args);

		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

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

				boolean bookmarked = getArguments().getBoolean(ARG_TAG_BOOKMARKED);
				if (bookmarked) {
					selection = WebIntentsProvider.WebIntents.BOOKMARKED + " = '1'";
				} else {
					selection = WebIntentsProvider.WebIntents.BOOKMARKED + " = '0'";
				}

				Cursor cursor = getActivity().getContentResolver().query(
						WebIntentsProvider.WebIntents.CONTENT_URI, 
						projection, selection, null, null);

				// Fix a bug when CursorAdapter has _id of non integer type, use ArrayList instead
				ArrayList<WebApp> webAppList = new ArrayList<WebApp>();
				while (cursor.moveToNext()) {
					webAppList.add(new WebApp(
							cursor.getString(cursor.getColumnIndex(WebIntentsProvider.WebIntents.TITLE)),
							cursor.getString(cursor.getColumnIndex(WebIntentsProvider.WebIntents.HREF))));
				}

				return webAppList;
			}

			@Override
			protected void onPostExecute(ArrayList<WebApp> webAppList) {

				setListAdapter(new CheckedWebAppArrayAdapter(getActivity(), webAppList));

				getActivity().startActionMode(mActionModeCallback);
//
//				listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
//				
//				// Save the original background for check state change
//				final Drawable background = listView.getBackground();
//				
				// Create an instance of subclass of MultiChoiceModeListener and ActionMode.Callback				
//				MultiChoiceModeListener multiChoiceModeListener = new MultiChoiceModeListener() {
//
//					@Override
//					public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
//						return false;
//					}
//
//					@Override
//					public void onDestroyActionMode(ActionMode mode) {
//						// Reset the background color while exiting action mode
//						SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
//						for (int i = 0; i < listView.getChildCount(); i++) {
//							if (checkedItems.get(i)) {
//								listView.getChildAt(i).setBackgroundDrawable(background);
//							}
//						}
//					}
//
//					@Override
//					public boolean onCreateActionMode(ActionMode mode, Menu menu) {
//						MenuInflater inflater = mode.getMenuInflater();
//
//						boolean bookmarked = getArguments().getBoolean(ARG_TAG_BOOKMARKED);                           
//						if (bookmarked) {
//							inflater.inflate(R.menu.cab_myapp, menu);
//						} else {
//							inflater.inflate(R.menu.cab_newapp, menu);
//						}
//
//						return true;
//					}
//
//					@Override
//					public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
//						SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
//
//						ArrayList<String> checkedApps = new ArrayList<String>();
//						for (int i = 0; i < getListAdapter().getCount(); i++) {
//							if (checkedItems.valueAt(i)) {
//								WebApp webApp = (WebApp) getListAdapter().getItem(i);
//								checkedApps.add(webApp.href);
//							}
//						}
//
//                        ContentResolver cr = getActivity().getContentResolver();
//                        int count = 0;
//                        
//                        switch (item.getItemId()) {
//                        case R.id.menu_add:
//                            for (String checkedApp : checkedApps) {
//                                String selection = WebIntentsProvider.WebIntents.HREF + " = ?";
//                                String[] selectionArgs = {
//                                        checkedApp
//                                };
//                                
//                                ContentValues values = new ContentValues();
//                                values.put(WebIntentsProvider.WebIntents.BOOKMARKED, "1");
//                                
//                                count += cr.update(WebIntentsProvider.WebIntents.CONTENT_URI, values, selection, selectionArgs);                                    
//                            }
//                            
//                            // If there is any update, notify registered observers.
//                            if (count > 0) {
//                                cr.notifyChange(WebIntentsProvider.WebIntents.CONTENT_URI, null);
//                            }
//
//                            if (checkedApps.size() == getListAdapter().getCount()) {
//                                // If no app left, clear the tab in CAB
//                                getActivity().getActionBar().removeTabAt(0);
//                            } else {
//                                // Check if there still are some new apps, refresh the list view accordingly
//                                AsyncTask<Void, Void, Cursor> newAppQueryTask = new AsyncTask<Void, Void, Cursor>() {
//
//                                    @Override
//                                    protected Cursor doInBackground(
//                                            Void... params) {
//                                        String[] projection = {
//                                                "DISTINCT " + WebIntentsProvider.WebIntents.TITLE,
//                                                WebIntentsProvider.WebIntents.HREF + " as _id"
//                                        };
//                                        String selection = WebIntentsProvider.WebIntents.BOOKMARKED + " = '0'";
//
//                                        Cursor cursor = getActivity().getContentResolver().query(
//                                                WebIntentsProvider.WebIntents.CONTENT_URI, 
//                                                projection, selection, null, null);
//                                        
//                                        return cursor;
//                                    }
//
//                                    @Override
//                                    protected void onPostExecute(Cursor cursor) {
//                                        String[] from = {
//                                                WebIntentsProvider.WebIntents.TITLE,
//                                                "_id"
//                                        };
//                                        
//                                        int[] to = {
//                                                android.R.id.text1,
//                                                android.R.id.text2
//                                        };
//                                        
//                                        setListAdapter(new SimpleCursorAdapter(getActivity(), 
//                                                android.R.layout.simple_list_item_2, cursor, 
//                                                from, to, 0));
//                                        
//                                        if (mTablet) {
//                                            // Clear the subwindow
//                                            if (mWebIntentsByAppListFragment != null) {
//                                                FragmentTransaction ft = getFragmentManager().beginTransaction();
//                                                ft.remove(mWebIntentsByAppListFragment);
//                                                ft.commit();
//                                            }
//                                        }
//                                    }
//                                };
//                                
//                                newAppQueryTask.execute();
//                            }
//                            
//                            break;
//                            
//                        case R.id.menu_remove:
//                            for (String checkedApp : checkedApps) {
//                                String selection = WebIntentsProvider.WebIntents.HREF + " = ?";
//                                String[] selectionArgs = {
//                                        checkedApp
//                                };
//                                
//                                count += cr.delete(WebIntentsProvider.WebIntents.CONTENT_URI, selection, selectionArgs);                                   
//                            }
//                            
//                            // If there is any update, notify registered observers.
//                            if (count > 0) {                                    
//                                cr.notifyChange(WebIntentsProvider.WebIntents.CONTENT_URI, null);
//                            }
//                            
//                            // Check if there still are some new apps, refresh the list view accordingly
//                            AsyncTask<Void, Void, Cursor> myAppQueryTask = new AsyncTask<Void, Void, Cursor>() {
//
//                                @Override
//                                protected Cursor doInBackground(
//                                        Void... params) {
//                                    String[] projection = {
//                                            "DISTINCT " + WebIntentsProvider.WebIntents.TITLE,
//                                            WebIntentsProvider.WebIntents.HREF + " as _id"
//                                    };
//                                    String selection = WebIntentsProvider.WebIntents.BOOKMARKED + " = '1'";
//
//                                    Cursor cursor = getActivity().getContentResolver().query(
//                                            WebIntentsProvider.WebIntents.CONTENT_URI, 
//                                            projection, selection, null, null);
//                                    
//                                    return cursor;
//                                }
//
//                                @Override
//                                protected void onPostExecute(Cursor cursor) {
//                                    String[] from = {
//                                            WebIntentsProvider.WebIntents.TITLE,
//                                            "_id"
//                                    };
//                                    
//                                    int[] to = {
//                                            android.R.id.text1,
//                                            android.R.id.text2
//                                    };
//                                    
//                                    setListAdapter(new SimpleCursorAdapter(getActivity(), 
//                                            android.R.layout.simple_list_item_2, cursor, 
//                                            from, to, 0));
//                                    
//                                    if (mTablet) {
//                                        // Clear the subwindow
//                                        if (mWebIntentsByAppListFragment != null) {
//                                            FragmentTransaction ft = getFragmentManager().beginTransaction();
//                                            ft.remove(mWebIntentsByAppListFragment);
//                                            ft.commit();
//                                        }
//                                    }
//                                }
//                            };
//                            
//                            myAppQueryTask.execute();
//                            
//                            break;
//
//                        default:
//                            break;
//                        }
//                        
//                        mode.finish();
//                        
//                        return false;
//                    }
//                    
//                    @Override
//                    public void onItemCheckedStateChanged(ActionMode mode, int position,
//                            long id, boolean checked) {
//                        int n = getListView().getCheckedItemCount();
//                        if (n > 1) {
//                            mode.setSubtitle(getListView().getCheckedItemCount() + " items selected");
//                        } else {
//                            mode.setSubtitle(getListView().getCheckedItemCount() + " item selected");
//                        }
//                        
//                        // Set the item background to indicate if it is checked
//                        if (checked) {
//                            getListView().getChildAt(position).setBackgroundResource(android.R.color.holo_blue_dark);
//                        } else {
//                            getListView().getChildAt(position).setBackgroundDrawable(background);
//                        }
//                    }
//                };
//
//				// Set the contextual action mode
//				listView.setMultiChoiceModeListener(multiChoiceModeListener);
            }
        };
        
        appQueryTask.execute();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        boolean bookmarked = getArguments().getBoolean(ARG_TAG_BOOKMARKED);
        
        WebApp webApp = (WebApp) getListAdapter().getItem(position);
        
        if (mTablet) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            mWebIntentsByAppListFragment = WebIntentsByAppListFragment.newInstance(bookmarked, webApp.href);
            ft.replace(R.id.subcontainer, mWebIntentsByAppListFragment);
            ft.commit();
        } else {
        	Intent intent = new Intent(getActivity().getApplicationContext(), WebIntentsByAppActivity.class);
        	intent.putExtra("bookmarked", bookmarked);
        	intent.putExtra("href", webApp.href);
        	startActivity(intent);
        }
    }
}