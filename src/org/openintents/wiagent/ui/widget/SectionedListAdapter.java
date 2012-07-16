//package org.openintents.wiagent.ui.widget;
//
//import java.util.LinkedHashMap;
//import java.util.Map;
//
//import org.openintents.wiagent.R;
//
//import android.content.Context;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Adapter;
//import android.widget.ArrayAdapter;
//import android.widget.BaseAdapter;
//
//public class SectionedListAdapter extends BaseAdapter {
//    
//    Map<String, Adapter> sections = new LinkedHashMap<String, Adapter>();
//    
//    ArrayAdapter<String> headers;
//    
//    public SectionedListAdapter(Context context) {
//        headers = new ArrayAdapter<String>(context, R.layout.list_header);
//    }
//    
//    public void addSection(String header, Adapter adapter) {
//        headers.add(header);
//        sections.put(header, adapter);
//    }
//
//    @Override
//    public int getCount() {
//        // TODO Auto-generated method stub
//        return 0;
//    }
//
//    @Override
//    public Object getItem(int arg0) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public long getItemId(int position) {
//        // TODO Auto-generated method stub
//        return 0;
//    }
//
//    @Override
//    public View getView(int position, View convertView, ViewGroup parent) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//}
