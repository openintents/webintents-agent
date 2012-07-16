package org.openintents.wiagent.ui;

import org.openintents.wiagent.R;
import org.openintents.wiagent.provider.WebIntentsProvider;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ActionBar.Tab;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.FrameLayout;

public class WebAppManagementActivity extends Activity {
    
    private FrameLayout mSubcontainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.fragment_webapp_list);
        mSubcontainer = (FrameLayout) findViewById(R.id.subcontainer);
        
        final ActionBar bar = getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        AsyncTask<Void, Void, Cursor> newAppQueryTask =  new AsyncTask<Void, Void, Cursor> () {

            @Override
            protected Cursor doInBackground(Void... params) {
                String[] projection = {
                        WebIntentsProvider.WebIntents.ID
                };
                String selection = WebIntentsProvider.WebIntents.BOOKMARKED + " = '0'";
                
                Cursor cursor = getContentResolver().query(
                        WebIntentsProvider.WebIntents.CONTENT_URI, 
                        projection, selection, null, null);
                
                return cursor;
            }

            @Override
            protected void onPostExecute(Cursor cursor) {
                if (cursor.moveToFirst()) {
                    ActionBar.Tab newApp = bar.newTab()
                            .setText("NEW FOUND")
                            .setTabListener(new ActionBar.TabListener() {
      
                                @Override
                                public void onTabUnselected(Tab tab, FragmentTransaction ft) {
                                    mSubcontainer.removeAllViews();
                                }
  
                                @Override
                                public void onTabSelected(Tab tab, FragmentTransaction ft) {
                                    ft.replace(R.id.main_container, 
                                            WebAppListFragment.newInstance(false));
                                }
  
                                @Override
                                public void onTabReselected(Tab tab, FragmentTransaction ft) {
                                // TODO Auto-generated method stub
                                }
                            });
                    bar.addTab(newApp, 0);
                    bar.selectTab(newApp);
                }
            }            
        };
        
        newAppQueryTask.execute();
        
        bar.addTab(bar.newTab()
                .setText("MY APP")
                .setTabListener(new ActionBar.TabListener() {
                    
                    @Override
                    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
                        mSubcontainer.removeAllViews();
                    }
                    
                    @Override
                    public void onTabSelected(Tab tab, FragmentTransaction ft) {
                        ft.replace(R.id.main_container, 
                                WebAppListFragment.newInstance(true));
                    }
                    
                    @Override
                    public void onTabReselected(Tab tab, FragmentTransaction ft) {
                        // TODO Auto-generated method stub
                        
                    }                    
                }));
    }

}
