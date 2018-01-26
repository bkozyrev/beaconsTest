package com.bkozyrev.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public class BeaconsActivity extends AppCompatActivity implements BeaconsView, AvailableDevicesAdapter.OnDeviceClickListener {

    private RecyclerView recyclerViewBeacons;
    private BeaconsPresenter beaconsPresenter;
    private AvailableDevicesAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacons);

        beaconsPresenter = new BeaconsPresenter();
        beaconsPresenter.attachView(this, this);
        recyclerViewBeacons = findViewById(R.id.recyclerViewBeacons);
        adapter = new AvailableDevicesAdapter();
        adapter.setOnDeviceClickListener(this);
        recyclerViewBeacons.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewBeacons.setAdapter(adapter);

        beaconsPresenter.onBluetoothEnable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        beaconsPresenter.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconsPresenter.detachView();
    }

    @Override
    public void addDevice(NimbDevice device) {
        adapter.addDevice(device);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDeviceClick(NimbDevice device) {
        Intent intent = new Intent(this, DeviceActivity.class);
        intent.putExtra("extra_device", device);
        startActivity(intent);
    }
}
