package cc.calliope.mini;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import no.nordicsemi.android.ble.BleManager;

public class InfoManager extends BleManager {
    private static final String TAG = "DeviceInformation";
    public static final UUID PARTIAL_FLASHING_SERVICE = UUID.fromString("E97DD91D-251D-470A-A062-FA1922DFA9A8");
    private static final UUID DFU_CONTROL_SERVICE = UUID.fromString("E95D93B0-251D-470A-A062-FA1922DFA9A8");
    private static final UUID DFU_SECURE_CONTROL_SERVICE = UUID.fromString("0000FE59-0000-1000-8000-00805F9B34FB");
    public static final int BOARD_UNIDENTIFIED = 0;
    public static final int BOARD_V1 = 1;
    public static final int BOARD_V2 = 2;
    @IntDef({BOARD_UNIDENTIFIED, BOARD_V1, BOARD_V2})
    @Retention(RetentionPolicy.SOURCE)
    public @interface HardwareType {
    }
    private int hardwareType = BOARD_UNIDENTIFIED;
    private boolean partialFlashingAvailable = false;
    private GetInfoListener getInfoListener;

    public interface GetInfoListener {
        void getInfo(int hardwareType, boolean partialFlashAvailable);
    }

    public InfoManager(@NonNull final Context context) {
        super(context);
        if (context instanceof GetInfoListener) {
            this.getInfoListener = (GetInfoListener) context;
        }
    }

    public void setOnInfoListener(GetInfoListener getInfoListener) {
        this.getInfoListener = getInfoListener;
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
            BluetoothGattService dfuControlService = gatt.getService(DFU_CONTROL_SERVICE);
            if (dfuControlService == null) {
                log(Log.WARN, "Can't find DFU_CONTROL_SERVICE");
            } else {
                hardwareType = BOARD_V1;
            }

            BluetoothGattService dfuSecureControlService = gatt.getService(DFU_SECURE_CONTROL_SERVICE);
            if (dfuSecureControlService == null) {
                log(Log.WARN, "Can't find DFU_SECURE_CONTROL_SERVICE");
            } else {
                hardwareType = BOARD_V2;
            }

            BluetoothGattService partialFlashingService = gatt.getService(PARTIAL_FLASHING_SERVICE);
            if (partialFlashingService == null) {
                log(Log.WARN, "Can't find PARTIAL_FLASHING_SERVICE");
            } else {
                partialFlashingAvailable = true;
            }

            return dfuControlService != null || dfuSecureControlService != null || partialFlashingService != null;
        }

        @Override
        protected void initialize() {
            log(Log.DEBUG, "Initialize...");
            if (getInfoListener != null) {
                getInfoListener.getInfo(hardwareType, partialFlashingAvailable);
            }
            //disconnect().enqueue();
        }

        @Override
        protected void onServicesInvalidated() {
            log(Log.DEBUG, "Services invalidated...");
        }
    }
}