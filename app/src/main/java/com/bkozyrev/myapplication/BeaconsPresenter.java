package com.bkozyrev.myapplication;

import android.content.Context;
import android.os.ParcelUuid;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.scan.ScanFilter;
import com.polidea.rxandroidble.scan.ScanSettings;

import java.util.UUID;

import rx.Subscription;

public class BeaconsPresenter {

    private static final UUID[] SERVICE_UUIDS = {
            UUID.fromString("00001803-0000-1000-8000-00805f9b34fb"),
            UUID.fromString("00001802-0000-1000-8000-00805f9b34fb"),
            UUID.fromString("00001804-0000-1000-8000-00805f9b34fb"),
            UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb")
    };

    //Ring name prefix. Probably, will be G2 someday
    private static final String FILTER_NAME = "Nimb_Ring_G1_";

    private RxBleClient rxBleClient;
    private Subscription scanSubscription;
    private BeaconsView view;

    public void attachView(BeaconsView view, Context context) {
        this.view = view;
        rxBleClient = RxBleClient.create(context);
    }

    public void detachView() {
        view = null;
        if (scanSubscription != null && !scanSubscription.isUnsubscribed()) {
            scanSubscription.unsubscribe();
        }
        rxBleClient = null;
    }

    public void onPause() {
        if (scanSubscription != null && !scanSubscription.isUnsubscribed()) {
            scanSubscription.unsubscribe();
        }
    }

    public void onBluetoothEnable() {
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();
        ScanFilter[] filters = new ScanFilter[SERVICE_UUIDS.length];
        for (int i = 0; i < SERVICE_UUIDS.length; i++) {
            filters[i] = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(SERVICE_UUIDS[i])).build();
        }

        scanSubscription = rxBleClient.scanBleDevices(settings, filters)
                .subscribe(
                        result -> {
                            /*if (result.getBleDevice().getName() != null &&
                                    result.getBleDevice().getName().contains(FILTER_NAME)) {*/
                                view.addDevice(NimbDevice.create(result.getBleDevice()));
                            //}
                        }, throwable -> {
                            throwable.printStackTrace();
                        }
                );
    }
}
