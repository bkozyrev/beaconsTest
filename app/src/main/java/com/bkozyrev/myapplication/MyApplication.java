package com.bkozyrev.myapplication;

import android.app.Application;

public class MyApplication extends Application {

    private static final String TAG = MyApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        //startService(new Intent(this, DetectionService.class));

        /*BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive: " + intent.getAction());
                startService(new Intent(MyApplication.this, DetectionService.class));
            }
        };

        Log.d(TAG, "registerReceiver");
        registerReceiver(broadcastReceiver, new IntentFilter("ACTION_START_DETECTING_SERVICE"));*/
    }
}
