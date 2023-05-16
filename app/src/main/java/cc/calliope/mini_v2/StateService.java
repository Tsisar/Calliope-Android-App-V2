package cc.calliope.mini_v2;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import org.microbit.android.partialflashing.PartialFlashingBaseService;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import cc.calliope.mini_v2.service.DfuService;
import cc.calliope.mini_v2.utils.Utils;
import no.nordicsemi.android.dfu.DfuBaseService;
import no.nordicsemi.android.error.GattError;

public class StateService extends Service {
    private static final String TAG = "StateService";

    public static final String BROADCAST_FLASHING = "cc.calliope.mini_v2.BROADCAST_FLASHING";
    public static final String EXTRA_FLASHING = "cc.calliope.mini_v2.EXTRA_FLASHING";
    public static final String BROADCAST_PROGRESS = "cc.calliope.mini_v2.BROADCAST_PROGRESS";
    public static final String EXTRA_PROGRESS = "cc.calliope.mini_v2.EXTRA_PROGRESS";
    public static final String BROADCAST_ERROR = "cc.calliope.mini_v2.BROADCAST_ERROR";
    public static final String EXTRA_ERROR = "cc.calliope.mini_v2.EXTRA_ERROR";
    public static final String EXTRA_MESSAGE = "cc.calliope.mini_v2.EXTRA_MESSAGE";
    public static final int PROGRESS_WAITING = 0;
    public static final int PROGRESS_CONNECTING = -1;
    public static final int PROGRESS_STARTING = -2;
    public static final int PROGRESS_ENABLING_DFU_MODE = -3;
    public static final int PROGRESS_VALIDATING = -4;
    public static final int PROGRESS_DISCONNECTING = -5;
    public static final int PROGRESS_COMPLETED = -6;
    public static final int PROGRESS_ABORTED = -7;
    public static final int PROGRESS_FAILED = -8;
    public static final int PROGRESS_ERROR = -9;
    private Context context;
    private FlashingReceiver broadcastReceiver;

    private class FlashingReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                //PartialFlashingService
                case PartialFlashingBaseService.BROADCAST_PROGRESS -> {
                    int percent = intent.getIntExtra(PartialFlashingBaseService.EXTRA_PROGRESS, 0);
                    sendProgressBroadcast(percent);
                }
                case PartialFlashingBaseService.BROADCAST_START -> {
                    sendProgressBroadcast(PROGRESS_STARTING);
                    sendFlashingBroadcast(true);
                }
                case PartialFlashingBaseService.BROADCAST_COMPLETE -> {
                    sendProgressBroadcast(PROGRESS_COMPLETED);
                    sendFlashingBroadcast(false);
                }
                case PartialFlashingBaseService.BROADCAST_PF_FAILED -> {
                    sendProgressBroadcast(PROGRESS_FAILED);
                    sendFlashingBroadcast(false);
                }
                //DfuService
                case DfuService.BROADCAST_PROGRESS -> {
                    int percent = intent.getIntExtra(DfuService.EXTRA_DATA, 0);
                    sendProgressBroadcast(percent);
                    if (percent == DfuService.PROGRESS_STARTING) {
                        sendFlashingBroadcast(true);
                    } else if (percent <= PROGRESS_DISCONNECTING) {
                        sendFlashingBroadcast(false);
                    }
                }
                case DfuService.BROADCAST_ERROR -> {
                    int errorCode = intent.getIntExtra(DfuBaseService.EXTRA_DATA, 0);
                    int errorType = intent.getIntExtra(DfuBaseService.EXTRA_ERROR_TYPE, 0);
                    String errorMessage = switch (errorType) {
                        case DfuBaseService.ERROR_TYPE_COMMUNICATION_STATE ->
                                GattError.parseConnectionError(errorCode);
                        case DfuBaseService.ERROR_TYPE_DFU_REMOTE ->
                                GattError.parseDfuRemoteError(errorCode);
                        default -> GattError.parse(errorCode);
                    };

                    sendProgressBroadcast(PROGRESS_ERROR);
                    sendFlashingBroadcast(false);
                    sendErrorBroadcast(errorCode, errorMessage);
                }
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        registerBroadcastReceiver();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterBroadcastReceiver();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void registerBroadcastReceiver() {
        if (broadcastReceiver == null) {
            broadcastReceiver = new FlashingReceiver();
            Utils.log(Log.WARN, TAG, "register Broadcast Receiver");
            IntentFilter filter = new IntentFilter();

            //DfuService
            filter.addAction(DfuService.BROADCAST_PROGRESS);
            filter.addAction(DfuService.BROADCAST_ERROR);

            //PartialFlashing
            filter.addAction(PartialFlashingBaseService.BROADCAST_PROGRESS);
            filter.addAction(PartialFlashingBaseService.BROADCAST_START);
            filter.addAction(PartialFlashingBaseService.BROADCAST_COMPLETE);
            filter.addAction(PartialFlashingBaseService.BROADCAST_PF_FAILED);
            filter.addAction(PartialFlashingBaseService.BROADCAST_PF_ATTEMPT_DFU);

            LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, filter);
        }
    }

    public void unregisterBroadcastReceiver() {
        if (broadcastReceiver != null) {
            Utils.log(Log.WARN, TAG, "unregister Broadcast Receiver");
            LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
    }

    private void sendProgressBroadcast(int progress) {
        Utils.log(Log.ASSERT, TAG, "PROGRESS: " + progress);

        Intent broadcast = new Intent(BROADCAST_PROGRESS);
        broadcast.putExtra(EXTRA_PROGRESS, progress);
        getApplication().sendBroadcast(broadcast);
    }

    private void sendFlashingBroadcast(boolean flashing) {
        Utils.log(Log.ASSERT, TAG, "FLASHING: " + flashing);

        Intent broadcast = new Intent(BROADCAST_FLASHING);
        broadcast.putExtra(EXTRA_FLASHING, flashing);
        getApplication().sendBroadcast(broadcast);
    }

    private void sendErrorBroadcast(int code, String message) {
        Utils.log(Log.ASSERT, TAG, "ERROR: " + code + ", " + message);

        Intent broadcast = new Intent(BROADCAST_ERROR);
        broadcast.putExtra(EXTRA_ERROR, code);
        broadcast.putExtra(EXTRA_MESSAGE, message);
        getApplication().sendBroadcast(broadcast);
    }
}