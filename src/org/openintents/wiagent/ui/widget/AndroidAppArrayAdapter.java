package org.openintents.wiagent.ui.widget;

import java.util.List;

import org.openintents.wiagent.R;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * An array adpater class for Android apps, like WebAppArrayAdapter
 * @author Cheng Zheng
 *
 */
public class AndroidAppArrayAdapter extends ArrayAdapter<ResolveInfo> {

    private LayoutInflater mInflater;
    private PackageManager mPackageManager;
    
    private List<ResolveInfo> mAndroidAppList;

    public AndroidAppArrayAdapter(Context context, List<ResolveInfo> androidAppList) {
        super(context, android.R.layout.activity_list_item, androidAppList);

        mAndroidAppList = androidAppList;
        mInflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPackageManager = getContext().getPackageManager();
    }
    
    @Override
    public View getView(int position, View convertView,
            ViewGroup parent) {
        ViewHolder viewHolder;
        
        if (convertView == null) {
            convertView = mInflater.inflate(android.R.layout.activity_list_item, null);
            
            viewHolder = new ViewHolder();
            viewHolder.text = (TextView) convertView.findViewById(android.R.id.text1);
            viewHolder.icon = (ImageView) convertView.findViewById(android.R.id.icon);
            
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // Set the layout width, height and gravity of the title of 
        // the Android application
        viewHolder.text.setLayoutParams(new LinearLayout.LayoutParams(
        		LinearLayout.LayoutParams.MATCH_PARENT, 
        		LinearLayout.LayoutParams.MATCH_PARENT));
        viewHolder.text.setTextAppearance(getContext(), android.R.style.TextAppearance_Holo_Medium);
        viewHolder.text.setGravity(Gravity.CENTER_VERTICAL);
        viewHolder.text.setText(mAndroidAppList.get(position).loadLabel(mPackageManager).toString());

        // Set the layout parameters of the icon of the Android application
        viewHolder.icon.setLayoutParams(new LinearLayout.LayoutParams(
        		LinearLayout.LayoutParams.WRAP_CONTENT, 
        		LinearLayout.LayoutParams.WRAP_CONTENT));
        viewHolder.icon.setImageDrawable(mAndroidAppList.get(position).loadIcon(mPackageManager));

        return convertView;
    }
    
    private static class ViewHolder {
        
        TextView text;
        ImageView icon;
    }
}
