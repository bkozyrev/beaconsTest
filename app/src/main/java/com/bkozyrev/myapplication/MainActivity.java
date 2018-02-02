package com.bkozyrev.myapplication;

import android.content.ComponentName;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.buttonLaunchDeviceManagement).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra("extra_value", true);
            intent.setComponent(new ComponentName("com.nimb.app", "ru.handh.nimb.ui.splash.SplashActivity"));
            startActivity(intent);
        });

        findViewById(R.id.buttonLaunchBeaconsActivity).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BeaconsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.buttonLaunchService).setOnClickListener(v -> {
            startService(new Intent(this, DetectionService.class));
        });

        findViewById(R.id.buttonKillService).setOnClickListener(v -> {
            stopService(new Intent(this, DetectionService.class));
        });

        findViewById(R.id.buttonStopTransmitting).setOnClickListener(v -> {
            BeaconParser beaconParser = new BeaconParser()
                    .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24");
            BeaconTransmitter beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);
            beaconTransmitter.stopAdvertising();
        });

        findViewById(R.id.buttonStartScanning).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ScanningActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.buttonStartBeaconDebug).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BeaconDebugActivity.class);
            startActivity(intent);
        });
    }
}
