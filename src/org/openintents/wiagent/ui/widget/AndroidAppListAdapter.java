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

public class AndroidAppListAdapter extends ArrayAdapter<ResolveInfo> {
    
    private Context mContext;
    private List<ResolveInfo> objects;
    private int textViewResourceId;
    

    public AndroidAppListAdapter(Context context, int textViewResourceId,
            List<ResolveInfo> objects) {
        super(context, textViewResourceId, objects);
        this.mContext = context;
        this.objects = objects;
        this.textViewResourceId = textViewResourceId;
    }
    
    @Override
    public View getView(int position, View convertView,
            ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
     
        View rowView = inflater.inflate(textViewResourceId, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.android_app_label);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.android_app_icon);
        PackageManager pm = mContext.getPackageManager();
        textView.setText(objects.get(position).loadLabel(pm).toString());
        imageView.setImageDrawable(objects.get(position).loadIcon(pm));
        return rowView;
    }

}
