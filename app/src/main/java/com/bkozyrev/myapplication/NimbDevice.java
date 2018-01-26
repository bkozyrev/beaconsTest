package com.bkozyrev.myapplication;


import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.polidea.rxandroidble.RxBleDevice;

@AutoValue
public abstract class NimbDevice implements Parcelable {

    public static final int STATUS_CONNECTING = 10;
    public static final int STATUS_CONNECTED = 11;
    public static final int STATUS_PAIRING = 20;
    public static final int STATUS_PAIRED = 21;
    public static final int STATUS_DISCONNECTING = 30;
    public static final int STATUS_DISCONNECTED = 31;
    public static final int STATUS_ABANDONED = 32;

    public abstract String address();
    @Nullable
    public abstract String name();
    public abstract int status();
    public abstract int batteryLevel();
    @Nullable
    public abstract String firmwareVersion();
    @Nullable
    public abstract String serialNumber();

    public static NimbDevice create(RxBleDevice rxBleDevice) {
        return NimbDevice.builder()
                .address(rxBleDevice.getMacAddress())
                .name(rxBleDevice.getName())
                .status(STATUS_DISCONNECTED)
                .batteryLevel(-1)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_NimbDevice.Builder();
    }

    public static TypeAdapter<NimbDevice> typeAdapter(Gson gson) {
        return new AutoValue_NimbDevice.GsonTypeAdapter(gson);
    }

    // With-er

    public abstract NimbDevice withStatus(int status);
    public abstract NimbDevice withBatteryLevel(int batteryLevel);
    public abstract NimbDevice withFirmwareVersion(String firmwareVersion);
    public abstract NimbDevice withSerialNumber(String serialNumber);

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder address(String address);
        public abstract Builder name(String name);
        public abstract Builder status(int status);
        public abstract Builder batteryLevel(int batteryLevel);
        public abstract Builder firmwareVersion(String firmwareVersion);
        public abstract Builder serialNumber(String serialNumber);

        public abstract NimbDevice build();
    }
}
