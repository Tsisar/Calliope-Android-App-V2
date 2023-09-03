package cc.calliope.mini;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import org.microbit.android.partialflashing.AlternativePartialFlashingBaseService;
import org.microbit.android.partialflashing.PartialFlashingBaseService;

import androidx.annotation.NonNull;
import cc.calliope.mini.activity.NotificationActivity;
import cc.calliope.mini.utils.Version;
import no.nordicsemi.android.dfu.DfuServiceInitiator;

public class PartialFlashingService extends AlternativePartialFlashingBaseService {
    private App app;

//    @Override
//    protected Class<? extends Activity> getNotificationTarget() {
//        return NotificationActivity.class;
//    }

    @Override
    public void onCreate() {
        super.onCreate();
        app = (App) getApplication();
        app.setAppState(App.APP_STATE_FLASHING);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        app.setAppState(App.APP_STATE_STANDBY);
    }
}