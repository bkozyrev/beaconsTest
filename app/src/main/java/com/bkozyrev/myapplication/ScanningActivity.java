package com.bkozyrev.myapplication;

import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.widget.CheckedTextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.Region;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ScanningActivity extends AppCompatActivity implements BeaconConsumer {

    @BindView(R.id.buttonStartScan) AppCompatButton buttonStartScan;
    @BindView(R.id.buttonStopScan) AppCompatButton buttonStopScan;
    @BindView(R.id.textViewLogs) AppCompatTextView textViewLogs;
    @BindView(R.id.checkedTextViewWait) CheckedTextView checkedTextViewWait2401;
    @BindView(R.id.buttonSend250B) AppCompatButton buttonSend250B;
    @BindView(R.id.buttonSend2501) AppCompatButton buttonSend2501;
    @BindView(R.id.buttonSend2502) AppCompatButton buttonSend2502;
    @BindView(R.id.buttonSend2503) AppCompatButton buttonSend2503;
    @BindView(R.id.buttonSend250C) AppCompatButton buttonSend250C;
    @BindView(R.id.buttonClearLogs) AppCompatButton buttonClearLogs;
    @BindView(R.id.buttonStopAdvertising) AppCompatButton buttonStopAdvertising;

    private AppCompatButton previousHighlightedButton;

    private BeaconManager beaconManager;
    private BeaconTransmitter beaconTransmitter;

    private String UUID;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanning);
        ButterKnife.bind(this);

        buttonStopScan.setOnClickListener(v -> {
            stopScan();
            highlightButton(null);
            buttonStartScan.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        });
        buttonStartScan.setOnClickListener(v -> {
            startScan();
            highlightButton(buttonStartScan);
        });
        buttonSend250B.setOnClickListener(v -> {
            sendMinor("0x250B", UUID);
            highlightButton(buttonSend250B);
        });
        buttonSend2501.setOnClickListener(v -> {
            sendMinor("0x2501", UUID);
            highlightButton(buttonSend2501);
        });
        buttonSend2502.setOnClickListener(v -> {
            sendMinor("0x2502", UUID);
            highlightButton(buttonSend2502);
        });
        buttonSend2503.setOnClickListener(v -> {
            sendMinor("0x2503", UUID);
            highlightButton(buttonSend2503);
        });
        buttonSend250C.setOnClickListener(v -> {
            sendMinor("0x250C", UUID);
            highlightButton(buttonSend250C);
        });
        buttonClearLogs.setOnClickListener(v -> {
            textViewLogs.setText(null);
        });
        buttonStopAdvertising.setOnClickListener(v -> {
            beaconTransmitter.stopAdvertising();
            highlightButton(null);
        });
        checkedTextViewWait2401.setOnClickListener(v -> checkedTextViewWait2401.setChecked(!checkedTextViewWait2401.isChecked()));

        BeaconParser beaconParser = new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24");

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(beaconParser);
        beaconManager.setForegroundScanPeriod(1000);
        beaconManager.setForegroundBetweenScanPeriod(0);
        beaconManager.bind(this);

        beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);

        if (getIntent() != null) {
            UUID = getIntent().getStringExtra("extra_uuid");
            if (getIntent().getBooleanExtra("extra_start_magic_please", false)) {
                startMagic();
            }
        }

        if (!TextUtils.isEmpty(UUID)) {
            buttonSend250B.setEnabled(true);
            buttonSend2501.setEnabled(true);
            buttonSend2502.setEnabled(true);
            buttonSend2503.setEnabled(true);
            buttonSend250C.setEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScan();
        beaconManager.unbind(this);
        beaconTransmitter = null;
        beaconManager = null;
    }

    @Override
    public void onBeaconServiceConnect() {
        buttonStartScan.setEnabled(true);
    }

    private void startScan() {
        setUpRangeNotifier();
        try {
            beaconManager.startRangingBeaconsInRegion(new Region("RangingId", null, null, null));
            textViewLogs.setText(textViewLogs.getText().toString() + "\n Started!");
            buttonStartScan.setEnabled(false);
            buttonStopScan.setEnabled(true);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void stopScan() {
        try {
            beaconManager.stopRangingBeaconsInRegion(new Region("RangingId", null, null, null));
            textViewLogs.setText(textViewLogs.getText().toString() + "\n Stopped!");
            buttonStartScan.setEnabled(true);
            buttonStopScan.setEnabled(false);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    String displayString;
    private void setUpRangeNotifier() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault());
        beaconManager.addRangeNotifier((collection, region) -> {
            calendar.setTimeInMillis(System.currentTimeMillis());
            for (Beacon beacon: collection) {
                UUID = beacon.getId1().toString();

                displayString = "";
                displayString += "\nTime: "; displayString += formatter.format(calendar.getTime()); displayString += "\n";
                displayString += "Name: "; displayString += beacon.getBluetoothName(); displayString += "\n";
                displayString += "Address: "; displayString += beacon.getBluetoothAddress(); displayString += "\n";
                displayString += "UUID: "; displayString += beacon.getId1(); displayString += "\n";
                displayString += "Major: "; displayString += beacon.getId2().toHexString(); displayString += "\n";
                displayString += "Minor: "; displayString += beacon.getId3().toHexString(); displayString += "\n";
                displayString += "-------------------------------"; displayString += "\n";

                Observable.just(true)
                        .take(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(aBoolean -> {
                            textViewLogs.setText(textViewLogs.getText().toString() + displayString);
                            if (!TextUtils.isEmpty(UUID)) {
                                buttonSend250B.setEnabled(true);
                                buttonSend2501.setEnabled(true);
                                buttonSend2502.setEnabled(true);
                                buttonSend2503.setEnabled(true);
                                buttonSend250C.setEnabled(true);
                            }

                            if (checkedTextViewWait2401.isChecked() && beacon.getId3().toHexString().equals("0x2401")) {

                                String tmpString = "\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!";
                                tmpString += "\nFound 0x2401 (Initiating kung-fu)";
                                tmpString += "\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n";
                                textViewLogs.setText(textViewLogs.getText().toString() + tmpString);

                                startMagic();
                                checkedTextViewWait2401.setChecked(false);
                            }
                        }, throwable -> {
                            throwable.printStackTrace();
                        });
            }
        });
    }

    private void sendMinor(String minor, String uuid) {
        Beacon beacon = new Beacon.Builder()
                .setBluetoothName("NIMB_RING_ANDROID")
                .setId1(uuid) // UUID for beacon
                .setId2("0x0000") // Major for beacon
                .setId3(minor) // Minor for beacon
                .setManufacturer(0x004C) // Radius Networks.0x0118  Change this for other beacon layouts//0x004C for iPhone
                .setTxPower(-56) // Power in dB
                .setDataFields(Arrays.asList(new Long[] {0l})) // Remove this for beacon layouts without d: fields
                .build();

        beaconTransmitter.stopAdvertising();
        beaconTransmitter.setAdvertiseMode(1);
        beaconTransmitter.startAdvertising(beacon, new AdvertiseCallback() {
            @Override
            public void onStartFailure(int errorCode) {
            }

            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                String displayString = "\n##############################";
                displayString += "\nSending minor = " + minor + "\nto uuid = " + uuid;
                displayString += "\n##############################\n";
                textViewLogs.setText(textViewLogs.getText().toString() + displayString);
            }
        });
    }

    private void highlightButton(@Nullable AppCompatButton button) {

        if (previousHighlightedButton != null) {
            previousHighlightedButton.setTextColor(ContextCompat.getColor(this, android.R.color.black));

            if (previousHighlightedButton.getId() == R.id.buttonStartScan) {
                previousHighlightedButton.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            }
        }

        if (button != null) {
            button.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
        }

        previousHighlightedButton = button;
    }

    private void startMagic() {
        sendMinor("0x2504", UUID);

        Observable.just(true)
                .delay(5000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> {
                    sendMinor("0x250A", UUID);
                }, throwable -> {
                    throwable.printStackTrace();
                });
    }
}