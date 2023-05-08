package cc.calliope.mini_v2;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

import androidx.annotation.NonNull;
import no.nordicsemi.android.ble.BleManager;

public class FlashingManager extends BleManager {
    private static final String TAG = "FlashingManager";
    private static final UUID DFU_CONTROL_SERVICE_UUID = UUID.fromString("E95D93B0-251D-470A-A062-FA1922DFA9A8");
    private static final UUID DFU_CONTROL_CHARACTERISTIC_UUID = UUID.fromString("E95D93B1-251D-470A-A062-FA1922DFA9A8");
    private BluetoothGattCharacteristic flashServiceCharacteristic;
    private OnDisconnectListener onDisconnectListener;
    public interface OnDisconnectListener {
        void onDisconnect();
    }

    public FlashingManager(@NonNull final Context context) {
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
            log(Log.DEBUG, "Checking if the flash service is supported...");

            // Here get instances of your characteristics.
            // Return false if a required service has not been discovered.
            BluetoothGattService flashService = gatt.getService(DFU_CONTROL_SERVICE_UUID);
            if (flashService == null) {
                log(Log.WARN, "Can't find DFU_CONTROL_SERVICE_UUID");
                return false;
            }

            flashServiceCharacteristic = flashService.getCharacteristic(DFU_CONTROL_CHARACTERISTIC_UUID);
            if (flashServiceCharacteristic == null) {
                log(Log.WARN, "Can't find DFU_CONTROL_CHARACTERISTIC_UUID");
                return false;
            }

            return true;
        }

        @Override
        protected void initialize() {
            log(Log.DEBUG, "Initialize...");
            byte[] data = {0x01};
            requestMtu(23)
                    .enqueue();

            log(Log.DEBUG, "Writing flash command...");
            //Writing 0x01 initiates rebooting the Mini into the Nordic Semiconductor bootloader
            writeCharacteristic(flashServiceCharacteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    .done(this::done)
                    .fail(this::fail)
                    .enqueue();

        }

        @Override
        protected void onServicesInvalidated() {
            log(Log.DEBUG, "Services invalidated...");
            // This method is called when the services get invalidated, i.e. when the device
            // disconnects.
            // References to characteristics should be nullified here.
            flashServiceCharacteristic = null;

            if (onDisconnectListener != null) {
                onDisconnectListener.onDisconnect();
            }
        }

        private void done(BluetoothDevice device) {
            log(Log.DEBUG, "Done");
        }

        private void fail(BluetoothDevice device, int status) {
            log(Log.ERROR, "Fail, status " + status);
        }
    }
}