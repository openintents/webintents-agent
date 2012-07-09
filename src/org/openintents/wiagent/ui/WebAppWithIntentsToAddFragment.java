package org.openintents.wiagent.ui;

import org.openintents.wiagent.R;
import org.openintents.wiagent.provider.WebIntentsProvider;

import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;

public class WebAppWithIntentsToAddFragment extends ListFragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onActivityCreated(savedInstanceState);
        QueryTask task = new QueryTask();
        task.execute();
    }
    
    private class QueryTask extends AsyncTask<Void, Void, Cursor> {

        @Override
        protected Cursor doInBackground(Void... params) {
            String[] projection = {
                "DISTINCT " + WebIntentsProvider.WebIntents.TITLE,
                WebIntentsProvider.WebIntents.HREF + " as _id"
            };
            Cursor cursor = getActivity().getContentResolver().query(
                    WebIntentsProvider.WebIntents.CONTENT_URI_INMEMORY, 
                    projection, null, null, null);
            return cursor;
        }
        
        String[] columnWebIntents = {
                WebIntentsProvider.WebIntents.TITLE,
                "_id"
        };

        int[] viewIDs = {
                android.R.id.text1,
                android.R.id.text2
        };

        @Override
        protected void onPostExecute(Cursor result) {
            WebAppWithIntentsToAddFragment.this.setListAdapter(new SimpleCursorAdapter(getActivity(), 
                    android.R.layout.simple_list_item_2, result, columnWebIntents, viewIDs, 0));
            
            result.moveToFirst();
            String href = result.getString(result.getColumnIndex("_id"));
            
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.webintents_by_app, WebIntentsToAddByAppFragment.newInstance(href));
            ft.commit();
        }
        
    }
    
    
}
