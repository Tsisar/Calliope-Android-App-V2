package cc.calliope.mini_v2;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.UUID;

import androidx.annotation.NonNull;
import no.nordicsemi.android.ble.BleManager;

public class DfuControlServiceManager extends BleManager {
    private static final String TAG = "DFUControlManager";
    public static final String BROADCAST_DFU_CONTROL_SERVICE = "cc.calliope.mini_v2.BROADCAST_DFU_CONTROL_SERVICE";
    public static final String EXTRA_DFU_CONTROL_SERVICE = "cc.calliope.mini_v2.EXTRA_DFU_CONTROL_SERVICE";
    public static final int EXTRA_ENABLING = 0;
    public static final int EXTRA_FAIL = -1;
    public static final int EXTRA_DONE = 1;

    private final Context context;
    private static final UUID DFU_CONTROL_SERVICE = UUID.fromString("E95D93B0-251D-470A-A062-FA1922DFA9A8");
    private static final UUID DFU_CONTROL_CHARACTERISTIC = UUID.fromString("E95D93B1-251D-470A-A062-FA1922DFA9A8");
    private BluetoothGattCharacteristic dfuControlServiceCharacteristic;
    private OnInvalidateListener onInvalidateListener;
    public interface OnInvalidateListener {
        void onDisconnect();
    }

    public DfuControlServiceManager(@NonNull final Context context) {
        super(context);
        this.context = context;
        if (context instanceof OnInvalidateListener) {
            this.onInvalidateListener = (OnInvalidateListener) context;
        }
    }

    public void setOnInvalidateListener(OnInvalidateListener onInvalidateListener) {
        this.onInvalidateListener = onInvalidateListener;
    }

    @Override
    public int getMinLogPriority() {
        // Use to return minimal desired logging priority.
        return Log.VERBOSE;
    }

    public void log(@NonNull String message) {
        log(getMinLogPriority(), message);
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
            log("Checking if the flashing services is supported...");
            sendBroadcast(EXTRA_ENABLING);

            BluetoothGattService dfuControlService = gatt.getService(DFU_CONTROL_SERVICE);
            if (dfuControlService != null) {
                dfuControlServiceCharacteristic = dfuControlService.getCharacteristic(DFU_CONTROL_CHARACTERISTIC);
            } else {
                log(Log.WARN, "DFU Control service isn't supported");
                sendBroadcast(EXTRA_FAIL);
                return false;
            }

            if(dfuControlServiceCharacteristic == null){
                log(Log.WARN, "Unable to get Control Service Characteristic");
                sendBroadcast(EXTRA_FAIL);
                return false;
            }
            return true;
        }

        @Override
        protected void initialize() {
            log("Initialize...");
            byte[] data = {(byte) 0x01};
            writeCharacteristic(dfuControlServiceCharacteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
                    .done(this::done)
                    .fail(this::fail)
                    .enqueue();
        }

        @Override
        protected void onServicesInvalidated() {
            log("Services invalidated...");
            dfuControlServiceCharacteristic = null;

            if (onInvalidateListener != null) {
                onInvalidateListener.onDisconnect();
            }
        }

        private void done(BluetoothDevice device) {
            log(Log.DEBUG, "Device: " + device.getAddress() + ". Done");
            sendBroadcast(EXTRA_DONE);
        }

        private void fail(BluetoothDevice device, int status) {
            log(Log.ERROR, "Device: " + device.getAddress() + ". Fail, status " + status);
            sendBroadcast(EXTRA_FAIL);
        }

        private void sendBroadcast(int extra) {
            Intent broadcast = new Intent(BROADCAST_DFU_CONTROL_SERVICE);
            broadcast.putExtra(EXTRA_DFU_CONTROL_SERVICE, extra);
            context.sendBroadcast(broadcast);
        }
    }
}