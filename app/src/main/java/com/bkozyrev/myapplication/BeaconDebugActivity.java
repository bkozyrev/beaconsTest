package com.bkozyrev.myapplication;

import android.graphics.Point;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.jakewharton.rxbinding.widget.RxTextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

public class BeaconDebugActivity extends AppCompatActivity implements BeaconConsumer {

    @BindView(R.id.textViewLogs) AppCompatTextView textViewLogs;
    @BindView(R.id.nestedScrollViewLogsContent) NestedScrollView nestedScrollViewLogsContent;
    @BindView(R.id.floatingActionButtonScrollDown) FloatingActionButton floatingActionButtonScrollDown;
    @BindView(R.id.toolbar) Toolbar toolbar;

    private BeaconManager beaconManager;

    private String UUID = "52470141-3103-9500-0000-595705c800ef";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon_debug);
        ButterKnife.bind(this);

        BeaconParser beaconParser = new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24");

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(beaconParser);
        beaconManager.setForegroundScanPeriod(1000);
        beaconManager.setForegroundBetweenScanPeriod(1000);
        beaconManager.bind(this);

        floatingActionButtonScrollDown.setOnClickListener(v -> nestedScrollViewLogsContent.fullScroll(View.FOCUS_DOWN));

        RxTextView.textChanges(textViewLogs)
                .subscribe(charSequence -> {
                    if (nestedScrollViewLogsContent.canScrollVertically(1)) {
                        floatingActionButtonScrollDown.setVisibility(View.VISIBLE);
                    }
                }, Throwable::printStackTrace);

        toolbar.setTitle(UUID);
        toolbar.inflateMenu(R.menu.beacon_debug_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menuClearLogs:
                    textViewLogs.setText(null);
                    floatingActionButtonScrollDown.setVisibility(View.GONE);
                    break;
            }
            return true;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScan();
        beaconManager.unbind(this);
        beaconManager = null;
    }

    @Override
    public void onBeaconServiceConnect() {
        startScan();
    }

    private void startScan() {
        setUpRangeNotifier();
        try {
            beaconManager.startRangingBeaconsInRegion(new Region("RangingId", Identifier.parse(UUID), null, null));
            textViewLogs.setText("Started " + UUID + "\n");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void stopScan() {
        try {
            beaconManager.stopRangingBeaconsInRegion(new Region("RangingId", Identifier.parse(UUID), null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private String displayString;
    private void setUpRangeNotifier() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault());
        beaconManager.addRangeNotifier((collection, region) -> {
            calendar.setTimeInMillis(System.currentTimeMillis());
            if (collection != null && collection.size() != 0) {
                Beacon beacon = (Beacon) collection.toArray()[0];
                displayString = "";
                displayString += "\nTime: ";
                displayString += formatter.format(calendar.getTime());
                displayString += ", Major: ";
                displayString += beacon.getId2().toHexString();
                displayString += ", Minor: ";
                displayString += beacon.getId3().toHexString();
                displayString += "\n";
                displayString += "Description...\n";

                Observable.just(true)
                        .take(1)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(aBoolean -> textViewLogs.setText(textViewLogs.getText().toString() + displayString),
                                throwable -> throwable.printStackTrace());
            }
        });
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.beacon_debug_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }*/

    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuClearLogs:
                textViewLogs.setText(null);
                floatingActionButtonScrollDown.setVisibility(View.GONE);
                break;
        }
        return super.onOptionsItemSelected(item);
    }*/
}
