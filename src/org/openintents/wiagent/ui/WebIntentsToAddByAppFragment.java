package org.openintents.wiagent.ui;

import org.openintents.wiagent.provider.WebIntentsProvider;

import android.app.ListFragment;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;

public class WebIntentsToAddByAppFragment extends ListFragment {
    
    public static WebIntentsToAddByAppFragment newInstance(String href) {
        WebIntentsToAddByAppFragment f = new WebIntentsToAddByAppFragment();
        
        Bundle args = new Bundle();
        args.putString("href", href);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        String href = getArguments().getString("href");
        QueryTask task = new QueryTask();
        task.execute(href);
    }
    
    private class QueryTask extends AsyncTask<String, Void, Cursor> {

        @Override
        protected Cursor doInBackground(String... params) {
            // TODO Auto-generated method stub
            String[] projection = {
                WebIntentsProvider.WebIntents.ID,
                WebIntentsProvider.WebIntents.ACTION,
                WebIntentsProvider.WebIntents.TYPE
            };
            String selection = WebIntentsProvider.WebIntents.HREF + "=?";
            String[] selectionArgs = {
                params[0]
            }; 
            Cursor cursor = getActivity().getContentResolver().query(
                    WebIntentsProvider.WebIntents.CONTENT_URI_INMEMORY, 
                    projection, selection, selectionArgs, null);
            return cursor;
        }
        
        String[] columnWebIntents = {
                WebIntentsProvider.WebIntents.ACTION,
                WebIntentsProvider.WebIntents.TYPE
        };

        int[] viewIDs = {
                android.R.id.text1,
                android.R.id.text2
        };

        @Override
        protected void onPostExecute(Cursor result) {
            // TODO Auto-generated method stub
            WebIntentsToAddByAppFragment.this.setListAdapter(new SimpleCursorAdapter(getActivity(), 
                    android.R.layout.simple_list_item_2, result, columnWebIntents, viewIDs, 0));
        
        }
        
    }
    
}
