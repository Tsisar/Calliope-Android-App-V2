package cc.calliope.mini_v2;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

import androidx.annotation.NonNull;
import no.nordicsemi.android.ble.BleManager;

public class InfoManager extends BleManager {
    private static final String TAG = "DeviceInformation";

    private static final UUID DEVICE_INFORMATION_SERVICE = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
    private static final UUID MODEL_NUMBER_STRING_CHARACTERISTICS = UUID.fromString("00002A24-0000-1000-8000-00805F9B34FB");
    private static final UUID SERIAL_NUMBER_STRING_CHARACTERISTICS = UUID.fromString("00002A25-0000-1000-8000-00805F9B34FB");
    private static final UUID FIRMWARE_REVISION_STRING_CHARACTERISTICS = UUID.fromString("00002A26-0000-1000-8000-00805F9B34FB");
    private static final UUID HARDWARE_REVISION_STRING_CHARACTERISTICS = UUID.fromString("00002A27-0000-1000-8000-00805F9B34FB");
    private static final UUID MANUFACTURER_NAME_STRING_CHARACTERISTICS = UUID.fromString("00002A29-0000-1000-8000-00805F9B34FB");
    private BluetoothGattCharacteristic modelNumberCharacteristic;
    private BluetoothGattCharacteristic serialNumberCharacteristic;
    private BluetoothGattCharacteristic firmwareRevisionCharacteristic;
    private BluetoothGattCharacteristic hardwareRevisionCharacteristic;
    private BluetoothGattCharacteristic manufacturerNameCharacteristic;
    private DeviceInfo deviceInfo;
    private OnDisconnectListener onDisconnectListener;
    public interface OnDisconnectListener {
        void onDisconnect();
    }

    public InfoManager(@NonNull final Context context) {
        super(context);
        if (context instanceof OnDisconnectListener) {
            this.onDisconnectListener = (OnDisconnectListener) context;
        }
    }

    public void setOnDisconnectListener(OnDisconnectListener onDisconnectListener){
        this.onDisconnectListener = onDisconnectListener;
    }

    @Override
    public int getMinLogPriority() {
        // Use to return minimal desired logging priority.
        return Log.VERBOSE;
    }

    @Override
    public void log(int priority, @NonNull String message) {
        // Log from here.
        Log.println(priority, TAG, "### " + Thread.currentThread().getId() + " # " + message);
    }

    @NonNull
    @Override
    public BleManagerGattCallback getGattCallback() {
        return new MyGattCallbackImpl();
    }

    private class MyGattCallbackImpl extends BleManagerGattCallback {
        @Override
        protected boolean isRequiredServiceSupported(@NonNull BluetoothGatt gatt) {
            log(Log.DEBUG, "Checking if the info service is supported...");

            // Here get instances of your characteristics.
            // Return false if a required service has not been discovered.
            BluetoothGattService infoService = gatt.getService(DEVICE_INFORMATION_SERVICE);
            if (infoService == null) {
                log(Log.WARN, "Can't find DEVICE_INFORMATION_SERVICE");
                return false;
            }

            modelNumberCharacteristic = infoService.getCharacteristic(MODEL_NUMBER_STRING_CHARACTERISTICS);
            if (modelNumberCharacteristic == null) {
                log(Log.WARN, "Can't find MODEL_NUMBER_STRING_CHARACTERISTICS");
            }

            serialNumberCharacteristic = infoService.getCharacteristic(SERIAL_NUMBER_STRING_CHARACTERISTICS);
            if (serialNumberCharacteristic == null) {
                log(Log.WARN, "Can't find SERIAL_NUMBER_STRING_CHARACTERISTICS");
            }

            firmwareRevisionCharacteristic = infoService.getCharacteristic(FIRMWARE_REVISION_STRING_CHARACTERISTICS);
            if (firmwareRevisionCharacteristic == null) {
                log(Log.WARN, "Can't find FIRMWARE_REVISION_STRING_CHARACTERISTICS");
            }

            hardwareRevisionCharacteristic = infoService.getCharacteristic(HARDWARE_REVISION_STRING_CHARACTERISTICS);
            if (hardwareRevisionCharacteristic == null) {
                log(Log.WARN, "Can't find HARDWARE_REVISION_STRING_CHARACTERISTICS");
            }

            manufacturerNameCharacteristic = infoService.getCharacteristic(MANUFACTURER_NAME_STRING_CHARACTERISTICS);
            if (manufacturerNameCharacteristic == null) {
                log(Log.WARN, "Can't find MANUFACTURER_NAME_STRING_CHARACTERISTICS");
            }
            return true;
        }

        @Override
        protected void initialize() {
            log(Log.DEBUG, "Initialize...");
            // Initialize your device.
            // This means e.g. enabling notifications, setting notification callbacks,
            // sometimes writing something to some Control Point.
            // Kotlin projects should not use suspend methods here, which require a scope.

            deviceInfo = new DeviceInfo();
            requestMtu(23).enqueue();
            readModelNumberCharacteristic();
        }

        @Override
        protected void onServicesInvalidated() {
            log(Log.DEBUG, "Services invalidated...");
            // This method is called when the services get invalidated, i.e. when the device
            // disconnects.
            // References to characteristics should be nullified here.
            modelNumberCharacteristic = null;
            serialNumberCharacteristic = null;
            firmwareRevisionCharacteristic = null;
            hardwareRevisionCharacteristic = null;
            manufacturerNameCharacteristic = null;

            if (onDisconnectListener != null) {
                onDisconnectListener.onDisconnect();
            }
        }
    }


    private void readModelNumberCharacteristic() {
        if (modelNumberCharacteristic != null) {
            readCharacteristic(modelNumberCharacteristic)
                    .done(r -> {
                        deviceInfo.setModelNumber(modelNumberCharacteristic.getStringValue(0));
                        readSerialNumberCharacteristic();
                    })
                    .enqueue();
        } else {
            readSerialNumberCharacteristic();
        }
    }

    private void readSerialNumberCharacteristic() {
        if (serialNumberCharacteristic != null) {
            readCharacteristic(serialNumberCharacteristic)
                    .done(r -> {
                        deviceInfo.setSerialNumber(serialNumberCharacteristic.getStringValue(0));
                        readFirmwareRevisionCharacteristic();
                    })
                    .enqueue();
        } else {
            readFirmwareRevisionCharacteristic();
        }
    }



    private void readFirmwareRevisionCharacteristic() {
        if (firmwareRevisionCharacteristic != null) {
            readCharacteristic(firmwareRevisionCharacteristic)
                    .done(r -> {
                        deviceInfo.setFirmwareRevision(firmwareRevisionCharacteristic.getStringValue(0));
                        readHardwareRevisionCharacteristic();
                    })
                    .enqueue();
        } else {
            readHardwareRevisionCharacteristic();
        }
    }

    private void readHardwareRevisionCharacteristic() {
        if (hardwareRevisionCharacteristic != null) {
            readCharacteristic(hardwareRevisionCharacteristic)
                    .done(r -> {
                        deviceInfo.setHardwareRevision(hardwareRevisionCharacteristic.getStringValue(0));
                        readManufacturerNameCharacteristic();
                    })
                    .enqueue();
        } else {
            readManufacturerNameCharacteristic();
        }
    }

    private void readManufacturerNameCharacteristic() {
        if (manufacturerNameCharacteristic != null) {
            readCharacteristic(manufacturerNameCharacteristic)
                    .done(r -> {
                        deviceInfo.setManufacturerName(manufacturerNameCharacteristic.getStringValue(0));
                        log(Log.WARN, deviceInfo.toString());
                        disconnect().enqueue();
                    })
                    .enqueue();
        } else {
            log(Log.WARN, deviceInfo.toString());
            disconnect().enqueue();
        }
    }
}