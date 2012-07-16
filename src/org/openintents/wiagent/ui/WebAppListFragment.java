package org.openintents.wiagent.ui;

import java.util.ArrayList;

import org.openintents.wiagent.R;
import org.openintents.wiagent.WebApp;
import org.openintents.wiagent.provider.WebIntentsProvider;
import org.openintents.wiagent.ui.widget.WebAppArrayAdapter;

import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
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

public class WebAppListFragment extends ListFragment {
    
    private static final String ARG_TAG_BOOKMARKED = "bookmarked";
    
    private WebIntentsByAppListFragment mWebIntentsByAppListFragment;
    
    public static WebAppListFragment newInstance(boolean bookmarked) {
        WebAppListFragment f = new WebAppListFragment();
        
        Bundle args = new Bundle();
        args.putBoolean(ARG_TAG_BOOKMARKED, bookmarked);
        f.setArguments(args);
        
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
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
;                
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
                
                setListAdapter(new WebAppArrayAdapter(getActivity(), webAppList));
                
                ListView listView = getListView();
                
                listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
                
                // Save the original background for unchecked setting
                final Drawable background = listView.getBackground();

                listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {
                    
                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        return false;
                    }
                    
                    @Override
                    public void onDestroyActionMode(ActionMode mode) {
                        SparseBooleanArray checkedItems = getListView().getCheckedItemPositions();
                        ListView listView = getListView();
                        for (int i = 0; i < listView.getChildCount(); i++) {
                            if (checkedItems.get(i)) {
                                listView.getChildAt(i).setBackgroundDrawable(background);
                            }
                        }
                    }
                    
                    @Override
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        MenuInflater inflater = mode.getMenuInflater();
                        
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
                        SparseBooleanArray checkedItems = getListView().getCheckedItemPositions();

                        ArrayList<String> checkedApps = new ArrayList<String>();
                        for (int i = 0; i < getListAdapter().getCount(); i++) {
                            if (checkedItems.valueAt(i)) {
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
                                        
                                        // Clear the subwindow
                                        if (mWebIntentsByAppListFragment != null) {
                                            FragmentTransaction ft = getFragmentManager().beginTransaction();
                                            ft.remove(mWebIntentsByAppListFragment);
                                            ft.commit();
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
                                    
                                    // Clear the subwindow
                                    if (mWebIntentsByAppListFragment != null) {
                                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                                        ft.remove(mWebIntentsByAppListFragment);
                                        ft.commit();
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
                    
                    @Override
                    public void onItemCheckedStateChanged(ActionMode mode, int position,
                            long id, boolean checked) {
                        int n = getListView().getCheckedItemCount();
                        if (n > 1) {
                            mode.setSubtitle(getListView().getCheckedItemCount() + " items selected");
                        } else {
                            mode.setSubtitle(getListView().getCheckedItemCount() + " item selected");
                        }
                        
                        if (checked) {
                            getListView().getChildAt(position).setBackgroundResource(android.R.color.holo_blue_dark);
                        } else {
                            getListView().getChildAt(position).setBackgroundDrawable(background);
                        }
                    }
                });
            }
        };
        
        appQueryTask.execute();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        boolean bookmarked = getArguments().getBoolean(ARG_TAG_BOOKMARKED);
        
        WebApp webApp = (WebApp) getListAdapter().getItem(position);
        
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        mWebIntentsByAppListFragment = WebIntentsByAppListFragment.newInstance(bookmarked, webApp.href);
        ft.replace(R.id.subcontainer, mWebIntentsByAppListFragment);
        ft.commit();
    }
}