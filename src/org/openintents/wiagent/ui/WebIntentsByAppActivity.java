package org.openintents.wiagent.ui;

import org.openintents.wiagent.R;
import org.openintents.wiagent.provider.WebIntentsProvider;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * This class is for displaying Web Intents for normal devices
 * @author Cheng Zheng
 *
 */
public class WebIntentsByAppActivity extends Activity {
	
	private int mFlag;
	private String mHref;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_webintents_by_app_list);
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		mHref = getIntent().getStringExtra("href");
		mFlag = getIntent().getIntExtra("flag", 0);
		ft.replace(R.id.subcontainer, WebIntentsByAppListFragment.newInstance(mHref));
		ft.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		
		switch (mFlag) {
		case WebAppListFragment.FLAG_MYAPP:
			inflater.inflate(R.menu.app_myapp, menu);
			break;

		case WebAppListFragment.FLAG_NEW_FOUND:
			inflater.inflate(R.menu.app_newapp, menu);
			break;

		case WebAppListFragment.FLAG_TRASH:
			inflater.inflate(R.menu.app_trash, menu);
			break;

		default:
			break;
		}

		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		ContentResolver cr = getContentResolver();
		String selection = WebIntentsProvider.WebIntents.HREF + " = ?";
		String[] selectionArgs = {
				mHref
		};
		ContentValues values = new ContentValues();
		
		CharSequence text = "";		
		switch (item.getItemId()) {
		case R.id.menu_add:
			values.put(WebIntentsProvider.WebIntents.BOOKMARKED, "1");
			cr.update(WebIntentsProvider.WebIntents.CONTENT_URI, values, selection, selectionArgs);
			
			text = "The application has been added.";

			break;

		case R.id.menu_subtract:
			values.put(WebIntentsProvider.WebIntents.REMOVED, "1");
			cr.update(WebIntentsProvider.WebIntents.CONTENT_URI, values, selection, selectionArgs);
			
			text = "The application has been removed.";

			break;

		case R.id.menu_restore:
			values.put(WebIntentsProvider.WebIntents.REMOVED, "0");
			cr.update(WebIntentsProvider.WebIntents.CONTENT_URI, values, selection, selectionArgs);
			
			text = "The application has been restored.";

			break;

		default:
			break;
		}
		
		Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
		toast.show();
		
		return true;
	}

	@Override
	public void onBackPressed() {
		Intent intent = new Intent(getApplicationContext(), WebAppManagementActivity.class);
		startActivity(intent);
	}
}
