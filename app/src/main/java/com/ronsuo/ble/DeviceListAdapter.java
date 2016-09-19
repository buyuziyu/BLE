package com.ronsuo.ble;

import android.app.Notification;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Oxygen on 2016/9/3.
 */
public class DeviceListAdapter extends BaseAdapter {

    ArrayList<String> mDeviceList = new ArrayList<>();
    Context mContext;

    public DeviceListAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return mDeviceList.size();
    }

    @Override
    public Object getItem(int position) {
        return mDeviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = View.inflate(mContext, R.layout.item_device, null);
            holder = new ViewHolder();
            holder.tvAddress = (TextView)convertView.findViewById(R.id.address);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        holder.tvAddress.setText(mDeviceList.get(position));

        return convertView;
    }

    static class ViewHolder {
        TextView tvAddress;
    }

    public void addDevice(String address) {
        for (String device : mDeviceList) {
            if (device.equals(address)) {
                return;
            }
        }
        mDeviceList.add(address);
        notifyDataSetChanged();
    }

    public void clearDevice() {
        mDeviceList.clear();
        notifyDataSetChanged();
    }
}
