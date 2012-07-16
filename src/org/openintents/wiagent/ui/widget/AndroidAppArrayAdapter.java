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

public class AndroidAppArrayAdapter extends ArrayAdapter<ResolveInfo> {
    
    private Context mContext;
    private LayoutInflater mInflater;
    private PackageManager mPackageManager;
    
    private List<ResolveInfo> mObjects;

    public AndroidAppArrayAdapter(Context context, List<ResolveInfo> objects) {
        super(context, R.layout.list_item_android_app, objects);
        mContext = context;
        mObjects = objects;       
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

        viewHolder.text.setText(mObjects.get(position).loadLabel(mPackageManager).toString());
        viewHolder.icon.setImageDrawable(mObjects.get(position).loadIcon(mPackageManager));

        return convertView;
    }
    
    private static class ViewHolder {
        
        TextView text;
        ImageView icon;
        
    }
}
