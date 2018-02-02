package com.bkozyrev.myapplication;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.helpers.ValueInterpreter;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class DeviceActivity extends AppCompatActivity implements BeaconConsumer {

    public static final String TAG = DeviceActivity.class.getSimpleName();

    public static final String ACTION_NIMB_DEVICE_EVENT_RECEIVED = "ru.handh.nimb.actions.ACTION_NIMB_DEVICE_EVENT_RECEIVED";
    public static final String EXTRA_NIMB_DEVICE_EVENT_ID = "ru.handh.nimb.extras.EXTRA_NIMB_DEVICE_EVENT_ID";
    public static final String EXTRA_NIMB_DEVICE_DATA = "ru.handh.nimb.extras.EXTRA_NIMB_DEVICE_DATA";

    public static final int NIMB_DEVICE_EVENT_UNDEFINED = 0x0;
    public static final int NIMB_DEVICE_EVENT_BATTERY_LEVEL_CHANGED = 0x1;
    public static final int NIMB_DEVICE_EVENT_BUTTON_EVENT_FIRED = 0x2;
    public static final int NIMB_DEVICE_EVENT_FIRMWARE_LOADED = 0x3;
    public static final int NIMB_DEVICE_EVENT_SERIAL_NUMBER_LOADED = 0x4;


    private static final String SERVICE_DEVICE_INFORMATION = "180a";
    private static final String CHARACTERISTIC_FIRMWARE = "2a26";
    private static final String CHARACTERISTIC_SERIAL_NUMBER = "2a25";
    private static final String SERVICE_BATTERY = "180f";
    private static final String CHARACTERISTIC_BATTERY = "2a19";
    private static final String SERVICE_LED_BUTTON = "1523";
    private static final String CHARACTERISTIC_BUTTON_EVENT = "1524";

    private NimbDevice device;

    private BeaconManager beaconManager;
    private BeaconTransmitter beaconTransmitter;

    private BluetoothGattCharacteristic batteryCharacteristic;
    private BluetoothGattCharacteristic buttonEventCharacteristic;
    private BluetoothGattCharacteristic firmwareCharacteristic;
    private BluetoothGattCharacteristic serialNumberCharacteristic;

    private Subscription connectionSubscription;

    private RxBleClient rxBleClient;

    public String serialNumberString, firmwareString, UUID;

    @BindView(R.id.textViewSerialNumber) AppCompatTextView textViewSerialNumber;
    @BindView(R.id.textViewFirmware) AppCompatTextView textViewFirmware;
    @BindView(R.id.textViewUUID) AppCompatTextView textViewUUID;
    @BindView(R.id.textViewConvertion) AppCompatTextView textViewConvertion;
    @BindView(R.id.buttonStartRanging) AppCompatButton buttonStartRanging;
    @BindView(R.id.buttonStartAsBeacon) AppCompatButton buttonStartAsBeacon;
    @BindView(R.id.linearLayoutButtonsContainer) LinearLayout linearLayoutButtonsContainer;
    @BindView(R.id.textViewLogs) AppCompatTextView textViewLogs;
    @BindView(R.id.buttonGoToScan) AppCompatButton buttonGoToScan;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        ButterKnife.bind(this);

        beaconManager = BeaconManager.getInstanceForApplication(this);

        device = getIntent().getParcelableExtra("extra_device");

        rxBleClient = RxBleClient.create(this);

        registerReceiver(nimbDeviceEventReceiver, new IntentFilter(ACTION_NIMB_DEVICE_EVENT_RECEIVED));
        establishConnection(device);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(nimbDeviceEventReceiver);
    }

    private void stopScan() {
        try {
            beaconManager.stopRangingBeaconsInRegion(new Region("RangingId", Identifier.parse(UUID), null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        connectionSubscription.unsubscribe();
        stopScan();
        beaconManager = null;
        if (beaconTransmitter != null) {
            beaconTransmitter.stopAdvertising();
        }
        beaconTransmitter = null;
    }

    public void establishConnection(NimbDevice device) {

        RxBleDevice rxBleDevice = rxBleClient.getBleDevice(device.address());
        connectionSubscription = rxBleDevice.establishConnection(false)
                .flatMap(connection -> connection.discoverServices()
                                                 .map(rxBleDeviceServices -> {
                                                     if (rxBleDeviceServices.getBluetoothGattServices().size() < 3) {
                                                         throw new RuntimeException("Services size < 3");
                                                     }
                                                     findCharacteristics(rxBleDeviceServices.getBluetoothGattServices());
                                                     return connection;
                                                 }))
                .delay(200, TimeUnit.MILLISECONDS)
                .flatMap(rxBleConnection ->
                        Observable.zip(
                                readButton(rxBleConnection),
                                readBattery(rxBleConnection),
                                readFirmware(rxBleConnection),
                                readSerialNumber(rxBleConnection),
                                (connection1, connection2, connection3, connection4) -> rxBleConnection)
                )
                .delay(200, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        connection -> {
                            Log.d(TAG, "Connected");
                            calculateUUID();
                            buttonGoToScan.setEnabled(true);
                            buttonGoToScan.setOnClickListener(v -> {
                                Intent intent = new Intent(this, ScanningActivity.class);
                                intent.putExtra("extra_start_magic_please", false);
                                intent.putExtra("extra_uuid", UUID);
                                startActivity(intent);
                            });
                        },
                        throwable -> {
                            throwable.printStackTrace();
                            establishConnection(device);
                        }
                );
    }

    private Observable<RxBleConnection> readButton(RxBleConnection connection) {
        return connection.readCharacteristic(buttonEventCharacteristic)
                .flatMap(value -> {
                    return Observable.just(connection);
                })
                .onErrorResumeNext(throwable -> Observable.just(connection));
    }

    private Observable<RxBleConnection> readBattery(RxBleConnection connection) {
        return connection.readCharacteristic(batteryCharacteristic)
                .doOnNext(value -> sendBroadcast(packNimbDeviceEventIntent(NIMB_DEVICE_EVENT_BATTERY_LEVEL_CHANGED, value)))
                .flatMap(value -> Observable.just(connection));
    }

    private Observable<RxBleConnection> readFirmware(RxBleConnection connection) {
        return connection.readCharacteristic(firmwareCharacteristic)
                .doOnNext(value -> sendBroadcast(packNimbDeviceEventIntent(NIMB_DEVICE_EVENT_FIRMWARE_LOADED, value)))
                .flatMap(value -> Observable.just(connection));
    }

    private Observable<RxBleConnection> readSerialNumber(RxBleConnection connection) {
        return connection.readCharacteristic(serialNumberCharacteristic)
                .doOnNext(value -> sendBroadcast(packNimbDeviceEventIntent(NIMB_DEVICE_EVENT_SERIAL_NUMBER_LOADED, value)))
                .flatMap(value -> Observable.just(connection));
    }

    private synchronized void findCharacteristics(List<? extends BluetoothGattService> services) {
        for (BluetoothGattService service : services) {
            String serviceUuid = service.getUuid().toString();
            String serviceAlias = serviceUuid.substring(4, 8);

            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {

                String categoryUuid = characteristic.getUuid().toString();
                String categoryAlias = categoryUuid.substring(4, 8);

                switch (serviceAlias) {
                    case SERVICE_DEVICE_INFORMATION:
                        switch (categoryAlias) {
                            case CHARACTERISTIC_FIRMWARE:
                                firmwareCharacteristic = characteristic;
                                break;
                            case CHARACTERISTIC_SERIAL_NUMBER:
                                serialNumberCharacteristic = characteristic;
                                break;
                        }
                        break;
                    case SERVICE_BATTERY:
                        switch (categoryAlias) {
                            case CHARACTERISTIC_BATTERY:
                                batteryCharacteristic = characteristic;
                                break;
                        }
                        break;
                    case SERVICE_LED_BUTTON:
                        switch (categoryAlias) {
                            case CHARACTERISTIC_BUTTON_EVENT:
                                buttonEventCharacteristic = characteristic;
                                break;
                        }
                        break;
                }

            }
        }
    }

    private Intent packNimbDeviceEventIntent(final int eventId, final byte[] data) {
        Intent intent = new Intent(ACTION_NIMB_DEVICE_EVENT_RECEIVED);
        intent.putExtra(EXTRA_NIMB_DEVICE_EVENT_ID, eventId);
        intent.putExtra(EXTRA_NIMB_DEVICE_DATA, data);
        return intent;
    }

    private final BroadcastReceiver nimbDeviceEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TextUtils.equals(action, ACTION_NIMB_DEVICE_EVENT_RECEIVED)) {
                final int eventId = intent.getIntExtra(EXTRA_NIMB_DEVICE_EVENT_ID, NIMB_DEVICE_EVENT_UNDEFINED);
                final byte[] data = intent.getByteArrayExtra(EXTRA_NIMB_DEVICE_DATA);
                switch (eventId) {
                    case NIMB_DEVICE_EVENT_BATTERY_LEVEL_CHANGED:
                        int batteryLevel = ValueInterpreter.getIntValue(data,
                                ValueInterpreter.FORMAT_UINT8, 0);
                        Log.d(TAG, "batteryLevel = " + batteryLevel);
                        break;
                    case NIMB_DEVICE_EVENT_BUTTON_EVENT_FIRED:
                        int buttonEventValue = ValueInterpreter.getIntValue(data,
                                ValueInterpreter.FORMAT_UINT8, 0);
                        break;
                    case NIMB_DEVICE_EVENT_FIRMWARE_LOADED:
                        String firmware = ValueInterpreter.getStringValue(data, 0);
                        device = device.withFirmwareVersion(firmware);
                        Log.d(TAG, "Firmware: " + firmware);
                        textViewFirmware.setText(firmware);
                        firmwareString = firmware;
                        break;
                    case NIMB_DEVICE_EVENT_SERIAL_NUMBER_LOADED:
                        String serialNumber = ValueInterpreter.getStringValue(data, 0);
                        device = device.withSerialNumber(serialNumber);
                        textViewSerialNumber.setText(serialNumber);
                        Log.d(TAG, "Serial Number: " + serialNumber);
                        serialNumberString = serialNumber;
                        break;
                }
            }
        }
    };

    private void calculateUUID() {
        String displayString = "";
        UUID = "";
        String deviceType = serialNumberString.substring(0, 2);
        String ringVersion = serialNumberString.substring(2, 4);
        String batchNumber = serialNumberString.substring(4, 6);
        String color = serialNumberString.substring(17, 18);
        String size = serialNumberString.substring(18, 20);
        String baseFirmware = firmwareString.substring(0, 3);
        String subFirmware = firmwareString.substring(4, 7);

        UUID += Integer.toHexString((int) deviceType.charAt(0));
        UUID += Integer.toHexString((int) deviceType.charAt(1));
        UUID += ringVersion;
        UUID += Integer.toHexString((int) batchNumber.charAt(0));
        UUID += Integer.toHexString((int) batchNumber.charAt(1));
        String hexDate = Integer.toHexString(Integer.valueOf(serialNumberString.substring(6, 10)));
        for (int i = 0; i < 4 - hexDate.length(); i++) {
            UUID += "0";
        }
        UUID += hexDate;
        String hexDeviceNumber = Integer.toHexString(Integer.valueOf(serialNumberString.substring(10, 16)));
        for (int i = 0; i < 8 - hexDeviceNumber.length(); i++) {
            UUID += "0";
        }
        UUID += hexDeviceNumber;
        UUID += Integer.toHexString((int) color.charAt(0));
        String hexSize = Integer.toHexString(Integer.valueOf(size));
        if (hexSize.length() < 2) {
            UUID += "0";
        }
        UUID += Integer.toHexString(Integer.valueOf(size));
        UUID += Integer.toHexString(Integer.valueOf(baseFirmware));
        String hexSubFirmware = Integer.toHexString(Integer.valueOf(subFirmware));
        for (int i = 0; i < 4 - hexSubFirmware.length(); i++) {
            UUID += "0";
        }
        UUID += Integer.toHexString(Integer.valueOf(subFirmware));

        displayString += deviceType;
        displayString += ringVersion;
        displayString += " -> ";
        displayString += UUID.substring(0, 6); displayString += "\n";

        displayString += batchNumber;
        displayString += " -> ";
        displayString += UUID.substring(6, 10); displayString += "\n";

        displayString += serialNumberString.substring(6, 10);
        displayString += " -> ";
        displayString += UUID.substring(10, 14); displayString += "\n";

        displayString += serialNumberString.substring(10, 16);
        displayString += " -> ";
        displayString += UUID.substring(14, 22); displayString += "\n";

        displayString += color; displayString += size;
        displayString += " -> ";
        displayString += UUID.substring(22, 26); displayString += "\n";

        displayString += baseFirmware; displayString += subFirmware;
        displayString += " -> ";
        displayString += UUID.substring(26, 32);

        String tmpUUID = "";
        tmpUUID += UUID.substring(0, 8) + "-";
        tmpUUID += UUID.substring(8, 12) + "-";
        tmpUUID += UUID.substring(12, 16) + "-";
        tmpUUID += UUID.substring(16, 20) + "-";
        tmpUUID += UUID.substring(20);

        UUID = tmpUUID;

        Log.d(TAG, "UUID = " + UUID);

        textViewUUID.setText(UUID);
        textViewConvertion.setText(displayString);

        linearLayoutButtonsContainer.setVisibility(View.VISIBLE);
        buttonStartRanging.setOnClickListener(v -> startRanging());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            buttonStartAsBeacon.setOnClickListener(v -> startAsBeacon());
        }
    }

    private void startRanging() {
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.setForegroundScanPeriod(1000);
        beaconManager.setForegroundBetweenScanPeriod(0);
        try {
            beaconManager.updateScanPeriods();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        beaconManager.bind(this);
    }

    String displayString = "", tmpString, timeString;
    @Override
    public void onBeaconServiceConnect() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault());
        beaconManager.addRangeNotifier((collection, region) -> {
            calendar.setTimeInMillis(System.currentTimeMillis());
            for (Beacon beacon: collection) {
                tmpString = "";
                timeString = "";
                timeString += "Time: "; tmpString += formatter.format(calendar.getTime()); tmpString += "\n";
                tmpString += "Name: "; tmpString += beacon.getBluetoothName(); tmpString += "\n";
                tmpString += "Address: "; tmpString += beacon.getBluetoothAddress(); tmpString += "\n";
                tmpString += "UUID: "; tmpString += beacon.getId1(); tmpString += "\n";
                tmpString += "Major: "; tmpString += beacon.getId2(); tmpString += "\n";
                tmpString += "Minor: "; tmpString += beacon.getId3().toHexString(); tmpString += "\n";
                tmpString += "-------------------------------"; tmpString += "\n";

                displayString = tmpString + displayString;

                Log.d(TAG, "MAC = " + beacon.getBluetoothAddress());
                Log.d(TAG, "Major = " + beacon.getId2());
                Log.d(TAG, "Minor = " + beacon.getId3().toHexString());

                Observable.just(true)
                        .take(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(aBoolean -> textViewLogs.setText(timeString + displayString));

                if (beacon.getId3().toHexString().equals("0x2401")) {
                    try {
                        beaconManager.stopRangingBeaconsInRegion(new Region("RangingId", new ArrayList<>(), null));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    Observable.just(true)
                            .take(1)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(aBoolean -> textViewLogs.setText("Stopped scanning \n" + displayString));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        startAsBeacon();
                    }
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("RangingId", new ArrayList<>(), null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startAsBeacon() {
        Beacon beacon = new Beacon.Builder()
                .setBluetoothName("NIMB_RING_ANDROID")
                .setId1(UUID) // UUID for beacon
                .setId2("0x0000") // Major for beacon
                .setId3("0x2504") // Minor for beacon
                .setManufacturer(0x004C) // Radius Networks.0x0118  Change this for other beacon layouts//0x004C for iPhone
                .setTxPower(-56) // Power in dB
                .setDataFields(Arrays.asList(new Long[] {0l})) // Remove this for beacon layouts without d: fields
                .build();

        BeaconParser beaconParser = new BeaconParser()
                .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24");

        beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);
        beaconTransmitter.setAdvertiseMode(1);
        beaconTransmitter.startAdvertising(beacon, new AdvertiseCallback() {
            @Override
            public void onStartFailure(int errorCode) {
                Log.d(TAG, "Advertisement start failed with code: " + errorCode);
            }

            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                displayString = "Sent 0x2504\n" + displayString;
                textViewLogs.setText(displayString);
                Log.d(TAG, "Advertisement start succeeded.");
            }
        });

        Observable.just(true)
                .delay(5000, TimeUnit.MILLISECONDS)
                .subscribe(aBoolean -> {
                    Beacon beacon1 = new Beacon.Builder()
                            .setBluetoothName("NIMB RING ANDROID")
                            .setId1(UUID) // UUID for beacon
                            .setId2("0x0000") // Major for beacon
                            .setId3("0x250A") // Minor for beacon
                            .setManufacturer(0x004C) // Radius Networks.0x0118  Change this for other beacon layouts//0x004C for iPhone
                            .setTxPower(-56) // Power in dB
                            .setDataFields(Arrays.asList(new Long[] {0l})) // Remove this for beacon layouts without d: fields
                            .build();
                    beaconTransmitter.stopAdvertising();
                    beaconTransmitter.startAdvertising(beacon1, new AdvertiseCallback() {
                        @Override
                        public void onStartFailure(int errorCode) {
                            Log.d(TAG, "Advertisement start failed with code: " + errorCode);
                        }

                        @Override
                        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                            textViewLogs.setText("Sent 0x250A\n" + displayString);
                            Log.d(TAG, "Advertisement start succeeded.");
                        }
                    });
                });
    }
}