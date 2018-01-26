package com.bkozyrev.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;

public class DetectionService extends Service implements BeaconConsumer {

    public static final String TAG = DetectionService.class.getSimpleName();

    private BeaconManager beaconManager;
    private boolean isDetected;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.setForegroundScanPeriod(1000);
        beaconManager.setForegroundBetweenScanPeriod(0);
        beaconManager.bind(this);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, "notificationChannel")
                        .setSmallIcon(R.drawable.ic_sentiment_neutral_black_24dp)
                        .setContentTitle("Waiting for 0x250a")
                        .setContentText("Can't touch this. Туц туруруц.");
        startForeground(42, builder.build());

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (beaconManager != null) {
            beaconManager.unbind(this);
            beaconManager = null;
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        isDetected = false;
        beaconManager.addRangeNotifier((collection, region) -> {
            for (Beacon beacon: collection) {
                Log.d(TAG, "UUID = " + beacon.getId1());
                Log.d(TAG, "Major = " + beacon.getId2().toHexString());
                Log.d(TAG, "Minor = " + beacon.getId3().toHexString());
                if (!isDetected && beacon.getId3().toHexString().equals("0x2401")) {
                    isDetected = true;
                    Intent intent = new Intent(this, DoggyActivity.class);
                    intent.putExtra("extra_uuid", beacon.getId1().toString());
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    stopSelf();
                }
            }
            Log.d(TAG, "--------------");
        });

        try {
            //beaconManager.startRangingBeaconsInRegion(new Region("RangingId", new ArrayList<>(), null));
            //beaconManager.startRangingBeaconsInRegion(new Region("RangingId", Identifier.parse("C68C2874-9F00-4E75-8915-023966207E98"), Identifier.parse("1"), Identifier.parse("1")));
            beaconManager.startRangingBeaconsInRegion(new Region("RangingId", null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        sendBroadcast(new Intent("action_start_detecting_service"));

        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());

        PendingIntent restartServicePendingIntent = PendingIntent.getService(getApplicationContext(), 1,
                restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.set(
                    AlarmManager.ELAPSED_REALTIME,
                    System.currentTimeMillis() + 1000,
                    restartServicePendingIntent);
        }

        super.onTaskRemoved(rootIntent);
    }
}
