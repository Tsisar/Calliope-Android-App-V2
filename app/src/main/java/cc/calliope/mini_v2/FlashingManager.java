package cc.calliope.mini_v2;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

import androidx.annotation.NonNull;
import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.WriteRequest;

public class FlashingManager extends BleManager {
    private static final String TAG = "FlashingManager";

    private static final UUID MINI_FLASH_SERVICE_UUID = UUID.fromString("E95D93B0-251D-470A-A062-FA1922DFA9A8");
    private static final UUID MINI_FLASH_SERVICE_CONTROL_CHARACTERISTIC_UUID = UUID.fromString("E95D93B1-251D-470A-A062-FA1922DFA9A8");

    private BluetoothGattCharacteristic flashServiceCharacteristic;

    private BluetoothGatt mGatt;

    public FlashingManager(@NonNull final Context context) {
        super(context);
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

    public BluetoothGatt getGatt() {
        return mGatt;
    }

    private class MyGattCallbackImpl extends BleManagerGattCallback {
        @Override
        protected boolean isRequiredServiceSupported(@NonNull BluetoothGatt gatt) {
            log(Log.DEBUG, "isRequiredServiceSupported " + gatt.getDevice().getAddress());
            mGatt = gatt;
            // Here get instances of your characteristics.
            // Return false if a required service has not been discovered.
            BluetoothGattService flashService = gatt.getService(MINI_FLASH_SERVICE_UUID);
            if (flashService == null) {
                log(Log.WARN, "Cannot find MINI_FLASH_SERVICE_UUID");
                return false;
            }

            flashServiceCharacteristic = flashService.getCharacteristic(MINI_FLASH_SERVICE_CONTROL_CHARACTERISTIC_UUID);
            if (flashServiceCharacteristic == null) {
                log(Log.WARN, "Cannot find MINI_FLASH_SERVICE_CONTROL_CHARACTERISTIC_UUID");
                return false;
            }

            return true;
        }

        @Override
        protected void initialize() {
            log(Log.DEBUG, "initialize");
            // Initialize your device.
            // This means e.g. enabling notifications, setting notification callbacks,
            // sometimes writing something to some Control Point.
            // Kotlin projects should not use suspend methods here, which require a scope.

            //requestMtu(517).enqueue();
        }

        @Override
        protected void onServicesInvalidated() {
            log(Log.DEBUG, "onServicesInvalidated");
            // This method is called when the services get invalidated, i.e. when the device
            // disconnects.
            // References to characteristics should be nullified here.
            flashServiceCharacteristic = null;
        }
    }

	public WriteRequest writeCharacteristic() {
        byte[] data = {1};
        Log.v(TAG, "Writing Flash Command...");
        return writeCharacteristic(flashServiceCharacteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
	}
}