package org.openintents.wiagent.ui;

import org.openintents.wiagent.R;
import org.openintents.wiagent.provider.WebIntentsProvider;
import org.openintents.wiagent.provider.WebIntentsProvider.WebIntents;

import android.app.ListFragment;
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
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class WebIntentsByAppListFragment extends ListFragment {
    
    private static final String ARG_TAG_BOOKMARKED = "bookmarked";
    private static final String ARG_TAG_HREF = "href";

    public static WebIntentsByAppListFragment newInstance(boolean bookmarked, String href) {
        WebIntentsByAppListFragment f = new WebIntentsByAppListFragment();
        
        Bundle args = new Bundle();
        args.putString(ARG_TAG_HREF, href);
        args.putBoolean(ARG_TAG_BOOKMARKED, bookmarked);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        AsyncTask<String, Void, Cursor> webIntentsQueryTask = new AsyncTask<String, Void, Cursor>() {

            @Override
            protected Cursor doInBackground(String... params) {
                String[] projection = {
                        WebIntentsProvider.WebIntents.ID,
                        WebIntentsProvider.WebIntents.ACTION,
                        WebIntentsProvider.WebIntents.TYPE
                    };
                String selection;
                String[] selectionArgs = {
                        getArguments().getString(ARG_TAG_HREF)
                };
                
                boolean bookmarked = getArguments().getBoolean(ARG_TAG_BOOKMARKED);
                if (bookmarked) {
                    selection = WebIntentsProvider.WebIntents.HREF + " = ? and " +
                            WebIntentsProvider.WebIntents.BOOKMARKED + " = '1'";
                } else {
                    selection = WebIntentsProvider.WebIntents.HREF + " = ? and " +
                            WebIntentsProvider.WebIntents.BOOKMARKED + " = '0'";
                }
                
                Cursor cursor = getActivity().getContentResolver().query(
                        WebIntentsProvider.WebIntents.CONTENT_URI, 
                        projection, selection, selectionArgs, null);   
                
                return cursor;
            }

            @Override
            protected void onPostExecute(Cursor result) {
                String[] from = {
                        WebIntentsProvider.WebIntents.ACTION,
                        WebIntentsProvider.WebIntents.TYPE
                };
                int[] to = {
                        android.R.id.text1,
                        android.R.id.text2
                };
                
                setListAdapter(new SimpleCursorAdapter(getActivity(), 
                        android.R.layout.simple_list_item_2, result, from, to, 0));
                
                ListView listView = getListView();
                
                listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
                
                final Drawable background = listView.getBackground();
                
                listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {
                    
                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        // TODO Auto-generated method stub
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
                        // TODO Auto-generated method stub
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
        
        String href = getArguments().getString(ARG_TAG_HREF);
        webIntentsQueryTask.execute(href);
    }


}
