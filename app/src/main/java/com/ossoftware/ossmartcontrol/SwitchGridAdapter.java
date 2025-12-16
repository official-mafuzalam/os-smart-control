package com.ossoftware.ossmartcontrol;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class SwitchGridAdapter extends BaseAdapter {

    private Context context;
    private List<DeviceModel> switchList;
    private OnSwitchClickListener listener;

    public interface OnSwitchClickListener {
        void onSwitchClick(int position, DeviceModel device);

        void onSwitchLongClick(int position, DeviceModel device);
    }

    public SwitchGridAdapter(Context context, List<DeviceModel> switchList, OnSwitchClickListener listener) {
        this.context = context;
        this.switchList = new ArrayList<>(switchList); // Create new list
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return switchList.size();
    }

    @Override
    public DeviceModel getItem(int position) {
        return switchList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_grid_switch, parent, false);
            holder = new ViewHolder();
            holder.btnSwitchToggle = convertView.findViewById(R.id.btnSwitchToggle);
            holder.txtSwitchState = convertView.findViewById(R.id.txtSwitchState);
            holder.txtSwitchName = convertView.findViewById(R.id.txtSwitchName);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        DeviceModel device = switchList.get(position);

        holder.txtSwitchName.setText(device.getName());
        updateSwitchUI(holder.btnSwitchToggle, holder.txtSwitchState, device.isOn());

        // Set click listeners
        final int pos = position;
        final DeviceModel currentDevice = device;

        holder.btnSwitchToggle.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSwitchClick(pos, currentDevice);
            }
        });

        convertView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onSwitchLongClick(pos, currentDevice);
                return true;
            }
            return false;
        });

        return convertView;
    }

    private void updateSwitchUI(RelativeLayout switchButton, TextView stateText, boolean isOn) {
        if (isOn) {
            switchButton.setBackgroundResource(R.drawable.bg_switch_on);
            stateText.setText("ON");
        } else {
            switchButton.setBackgroundResource(R.drawable.bg_switch_off);
            stateText.setText("OFF");
        }
    }

    public void updateSwitchState(int position, boolean isOn) {
        if (position >= 0 && position < switchList.size()) {
            DeviceModel device = switchList.get(position);
            device.setOn(isOn);
            notifyDataSetChanged();
        }
    }

    public void updateAllSwitches(List<DeviceModel> newList) {
        switchList.clear();
        switchList.addAll(newList);
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        RelativeLayout btnSwitchToggle;
        TextView txtSwitchState;
        TextView txtSwitchName;
    }
}