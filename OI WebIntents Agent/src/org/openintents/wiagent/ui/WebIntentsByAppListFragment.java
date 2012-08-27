package org.openintents.wiagent.ui;

import org.openintents.wiagent.provider.WebIntentsProvider;
import android.app.ListFragment;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;

/**
 * The fragment showing intents of a Web app
 * @author Cheng Zheng
 *
 */
public class WebIntentsByAppListFragment extends ListFragment {

	private static final String ARG_TAG_HREF = "href";

	public static WebIntentsByAppListFragment newInstance(String href) {
		WebIntentsByAppListFragment f = new WebIntentsByAppListFragment();

		Bundle args = new Bundle();
		args.putString(ARG_TAG_HREF, href);
		f.setArguments(args);

		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AsyncTask<Void, Void, Cursor> webIntentsQueryByAppTask = new AsyncTask<Void, Void, Cursor>() {

			@Override
			protected Cursor doInBackground(Void... params) {
				String[] projection = {
						WebIntentsProvider.WebIntents._ID,
						WebIntentsProvider.WebIntents.ACTION,
						WebIntentsProvider.WebIntents.TYPE
				};
				String selection = WebIntentsProvider.WebIntents.HREF + " = ?";
				String[] selectionArgs = {
						getArguments().getString(ARG_TAG_HREF)
				};

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
			}
		};

		webIntentsQueryByAppTask.execute();
	}
}