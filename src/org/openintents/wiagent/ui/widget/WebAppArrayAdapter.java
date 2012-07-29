package org.openintents.wiagent.ui.widget;

import java.util.List;

import org.openintents.wiagent.R;
import org.openintents.wiagent.WebApp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class WebAppArrayAdapter extends ArrayAdapter<WebApp> {
    
    private List<WebApp> mWebAppList;
    private LayoutInflater mInflater;

    public WebAppArrayAdapter(Context context, List<WebApp> webAppList) {
        super(context, R.layout.list_item_webapp, webAppList);
        
        mWebAppList = webAppList;
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    
    @Override
    public View getView(int position, View convertView,
            ViewGroup parent) {
        ViewHolder viewHolder;
        
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item_webapp, null);
            
            viewHolder = new ViewHolder();
            viewHolder.title = (TextView) convertView.findViewById(R.id.webapp_title);
            viewHolder.href = (TextView) convertView.findViewById(R.id.webapp_href);
            
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.title.setText(mWebAppList.get(position).title);
        viewHolder.href.setText(mWebAppList.get(position).href);

        return convertView;
    }
    
    private static class ViewHolder {
        
        TextView title;
        TextView href;        
    }
}
