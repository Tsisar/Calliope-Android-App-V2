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

    public void log(int priority, @NonNull String message) {
        // Log from here.
        Log.println(priority, TAG, "### " + Thread.currentThread().getId() + " # " + message);
    }
}