package org.openintents.wiagent.ui.widget;

import java.util.List;

import org.openintents.wiagent.R;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * An array adpater class for Android apps, like WebAppArrayAdapter
 * @author Cheng Zheng
 *
 */
public class AndroidAppArrayAdapter extends ArrayAdapter<ResolveInfo> {
    
    private Context mContext;
    private LayoutInflater mInflater;
    private PackageManager mPackageManager;
    
    private List<ResolveInfo> mAndroidAppList;

    public AndroidAppArrayAdapter(Context context, List<ResolveInfo> androidAppList) {
        super(context, R.layout.list_item_android_app, androidAppList);
        mContext = context;
        mAndroidAppList = androidAppList;       
        mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPackageManager = mContext.getPackageManager();
    }
    
    @Override
    public View getView(int position, View convertView,
            ViewGroup parent) {
        ViewHolder viewHolder;
        
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item_android_app, null);
            
            viewHolder = new ViewHolder();
            viewHolder.text = (TextView) convertView.findViewById(R.id.android_app_label);
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.android_app_icon);
            
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.text.setText(mAndroidAppList.get(position).loadLabel(mPackageManager).toString());
        viewHolder.icon.setImageDrawable(mAndroidAppList.get(position).loadIcon(mPackageManager));

        return convertView;
    }
    
    private static class ViewHolder {
        
        TextView text;
        ImageView icon;
        
    }
}
