package org.openintents.wiagent.ui;

import org.openintents.wiagent.R;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ActionBar.Tab;
import android.os.Bundle;

public class WebAppManagementActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.apps_to_add);
        
        ActionBar bar = getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        bar.addTab(bar.newTab()
                .setText("TOADD")
                .setTabListener(new ActionBar.TabListener() {
                    
                    @Override
                    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
                        // TODO Auto-generated method stub
                        
                    }
                    
                    @Override
                    public void onTabSelected(Tab tab, FragmentTransaction ft) {
                        // TODO Auto-generated method stub
//                        ft.add(android.R.id.content, new WebAppWithIntentsToAddFragment());
//                        ft.add(android.R.id.content, new WebIntentsToAddByAppFragment());
                    }
                    
                    @Override
                    public void onTabReselected(Tab tab, FragmentTransaction ft) {
                        // TODO Auto-generated method stub
                        
                    }
                    
                }));  
        
        bar.addTab(bar.newTab()
                .setText("MYAPP")
                .setTabListener(new ActionBar.TabListener() {
                    
                    @Override
                    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
                        // TODO Auto-generated method stub
                        
                    }
                    
                    @Override
                    public void onTabSelected(Tab tab, FragmentTransaction ft) {
                        // TODO Auto-generated method stub
                        
                    }
                    
                    @Override
                    public void onTabReselected(Tab tab, FragmentTransaction ft) {
                        // TODO Auto-generated method stub
                        
                    }
                    
                }));
    }

}
