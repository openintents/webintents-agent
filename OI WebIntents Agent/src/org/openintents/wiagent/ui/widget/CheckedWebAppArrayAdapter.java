package org.openintents.wiagent.ui.widget;

import java.util.ArrayList;
import java.util.List;

import org.openintents.wiagent.R;
import org.openintents.wiagent.WebApp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

/**
 * A custom array adapter for web applications. This adapter is for list view
 * with checkbox
 *
 */

public class CheckedWebAppArrayAdapter extends ArrayAdapter<WebApp> {

	/**
	 * An array list of check indicators
	 */
	public ArrayList<Boolean> mCheckList;

	private List<WebApp> mWebAppList;
	private LayoutInflater mInflater;

	public CheckedWebAppArrayAdapter(Context context, List<WebApp> webAppList) {
		super(context, R.layout.list_item_webapp_checkbox, webAppList);

		mWebAppList = webAppList;
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// Initialize the check list to all unchecked
		mCheckList = new ArrayList<Boolean>();
		for (int i = 0; i < webAppList.size(); i++) {
			mCheckList.add(false);
		}
	}

	public void setAllChecked(boolean checked) {
		for (int i = 0; i < mCheckList.size(); i++) {
			mCheckList.set(i, checked);
		}
	}

	@Override
	public View getView(int position, View convertView,
			ViewGroup parent) {
		ViewHolder viewHolder;

		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.list_item_webapp_checkbox, null);

			viewHolder = new ViewHolder();
			viewHolder.check = (CheckBox) convertView.findViewById(R.id.webapp_check);
			viewHolder.title = (TextView) convertView.findViewById(R.id.webapp_title);
			viewHolder.href = (TextView) convertView.findViewById(R.id.webapp_href);
			
			// Set checkbox listener to change the value of the corresponding item of mCheckList
			final int location = position;
			viewHolder.check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					mCheckList.set(location, isChecked);
				}
			});
			
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		if (mCheckList.get(position)) {
			viewHolder.check.setChecked(true);
		} else {
			viewHolder.check.setChecked(false);
		}

		viewHolder.title.setTextAppearance(getContext(), android.R.style.TextAppearance_Holo_Medium);
		viewHolder.title.setText(mWebAppList.get(position).title);
		viewHolder.href.setTextAppearance(getContext(), android.R.style.TextAppearance_Holo_Small);
		viewHolder.href.setText(mWebAppList.get(position).href);

		return convertView;
	}
	
	/**
	 * Hold the view for performance purpose
	 * @author Cheng Zheng
	 *
	 */
	private static class ViewHolder {

		CheckBox check;

		TextView title;
		TextView href;
	}
}