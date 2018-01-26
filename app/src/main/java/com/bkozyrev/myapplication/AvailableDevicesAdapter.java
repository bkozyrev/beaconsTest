package com.bkozyrev.myapplication;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AvailableDevicesAdapter extends RecyclerView.Adapter<AvailableDevicesAdapter.DeviceViewHolder> {

    private final List<NimbDevice> devices;

    private OnDeviceClickListener onDeviceClickListener;

    public AvailableDevicesAdapter() {
        devices = Collections.synchronizedList(new ArrayList<>());
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_available_device, parent, false);
        return new DeviceViewHolder(v);
    }

    @Override
    public void onBindViewHolder(DeviceViewHolder holder, int position) {
        holder.bind(devices.get(position));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public synchronized void addDevice(NimbDevice device) {
        if (devices.isEmpty()) {
            devices.add(device);
        } else {
            boolean alreadyIn = false;
            for (NimbDevice d : devices) {
                if (TextUtils.equals(d.address(), device.address())) {
                    alreadyIn = true;
                    break;
                }
            }
            if (!alreadyIn) {
                devices.add(device);
            }
        }
    }

    public void setOnDeviceClickListener(OnDeviceClickListener onDeviceClickListener) {
        this.onDeviceClickListener = onDeviceClickListener;
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.viewRoot) View viewRoot;
        @BindView(R.id.textViewName) TextView textViewName;
        @BindView(R.id.imageViewIcon) ImageView imageViewIcon;

        public DeviceViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(NimbDevice device) {
            if (TextUtils.isEmpty(device.name())) {
                textViewName.setText(device.address());
            } else {
                textViewName.setText(device.name());
            }
            viewRoot.setOnClickListener(view -> {
                if (onDeviceClickListener != null) {
                    onDeviceClickListener.onDeviceClick(device);
                }
            });
            /*switch (ViewUtil.getDeviceColor(device)) {
                case NimbDeviceService.DEVICE_COLOR_BLACK:
                    imageViewIcon.setImageResource(R.drawable.available_black_device);
                    break;
                case NimbDeviceService.DEVICE_COLOR_WHITE:
                    imageViewIcon.setImageResource(R.drawable.available_white_device);
                    break;
                default:
                    imageViewIcon.setImageDrawable(null);
                    break;
            }*/
        }
    }

    public interface OnDeviceClickListener {
        void onDeviceClick(NimbDevice device);
    }
}
