package cc.calliope.mini;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import org.microbit.android.partialflashing.PartialFlashingBaseService;

import androidx.annotation.NonNull;
import cc.calliope.mini.activity.NotificationActivity;

public class PartialFlashingService extends PartialFlashingBaseService {
    private static final String TAG = "PartialFlashingService";
    @Override
    protected Class<? extends Activity> getNotificationTarget() {
        return NotificationActivity.class;
    }

    @Override
    public void onDeviceConnecting(@NonNull BluetoothDevice device) {
        log(Log.INFO, Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    @Override
    public void onDeviceConnected(@NonNull BluetoothDevice device) {
        log(Log.INFO, Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    @Override
    public void onDeviceFailedToConnect(@NonNull BluetoothDevice device, int reason) {
        log(Log.INFO, Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    @Override
    public void onDeviceReady(@NonNull BluetoothDevice device) {
        log(Log.INFO, Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    @Override
    public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {
        log(Log.INFO, Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    @Override
    public void onDeviceDisconnected(@NonNull BluetoothDevice device, int reason) {
        log(Log.INFO, Thread.currentThread().getStackTrace()[2].getMethodName());
    }

    public void log(int priority, @NonNull String message) {
        // Log from here.
        Log.println(priority, TAG, "### " + Thread.currentThread().getId() + " # " + message);
    }
}