package org.openintents.wiagent.ui.widget;

import java.util.List;

import org.openintents.wiagent.WebApp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * An custom array adapter class for Web application
 *
 */
public class WebAppArrayAdapter extends ArrayAdapter<WebApp> {

	private List<WebApp> mWebAppList;
	private LayoutInflater mInflater;

	public WebAppArrayAdapter(Context context, List<WebApp> webAppList) {
		super(context, android.R.layout.simple_list_item_2, webAppList);

		mWebAppList = webAppList;
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView,
			ViewGroup parent) {
		ViewHolder viewHolder;

		if (convertView == null) {
		// Only create new view when convert view is null to improve performance
			convertView = mInflater.inflate(android.R.layout.simple_list_item_2, null);

			viewHolder = new ViewHolder();
			viewHolder.title = (TextView) convertView.findViewById(android.R.id.text1);
			viewHolder.href = (TextView) convertView.findViewById(android.R.id.text2);

			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		viewHolder.title.setTextAppearance(getContext(), android.R.style.TextAppearance_Holo_Medium);
		viewHolder.title.setText(mWebAppList.get(position).title);
		viewHolder.href.setTextAppearance(getContext(), android.R.style.TextAppearance_Holo_Small);
		viewHolder.href.setText(mWebAppList.get(position).href);

		return convertView;
	}

	/**
	 * Hold the view for performance purpose
	 *
	 */
	private static class ViewHolder {

		TextView title;
		TextView href;
	}
}
