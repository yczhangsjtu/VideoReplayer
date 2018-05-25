package com.yczhang.videoreplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class IntervalArrayAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater inflater;
    private ArrayList<Interval> data;

    public IntervalArrayAdapter(Context context, ArrayList<Interval> items) {
        this.context = context;
        this.data = items;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = inflater.inflate(R.layout.interval_item, parent, false);
        TextView start = (TextView) view.findViewById(R.id.interval_start);
        TextView end = (TextView) view.findViewById(R.id.interval_end);
        start.setText(data.get(position).getStartString());
        end.setText(data.get(position).getEndString());
        return view;
    }
}
