package com.bkozyrev.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartDetectionServiceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("action_start_detecting_service".equals(intent.getAction()) || Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            context.startService(new Intent(context, DetectionService.class));
        }
    }
}
