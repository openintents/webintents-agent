package org.openintents.wiagent.ui;

import org.openintents.wiagent.R;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;

/**
 * This class is for displaying Web Intents for normal devices
 * @author Cheng Zheng
 *
 */
public class WebIntentsByAppActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_webintents_by_app_list);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        boolean bookmarked = getIntent().getBooleanExtra("bookmarked", true);
        String href = getIntent().getStringExtra("href");
        ft.replace(R.id.subcontainer, WebIntentsByAppListFragment.newInstance(bookmarked, href));
        ft.commit();
	}

}
