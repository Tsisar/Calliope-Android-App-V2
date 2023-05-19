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

public class BroadcastAggregatorService extends Service {
    private static final String TAG = "BroadcastAggregatorService";
    public static final String BROADCAST_FLASHING = "cc.calliope.mini_v2.BROADCAST_FLASHING"; //TODO are we need it?
    public static final String EXTRA_FLASHING = "cc.calliope.mini_v2.EXTRA_FLASHING";
    public static final String BROADCAST_PROGRESS = "cc.calliope.mini_v2.BROADCAST_PROGRESS";
    public static final String EXTRA_PROGRESS = "cc.calliope.mini_v2.EXTRA_PROGRESS";
    public static final String BROADCAST_ERROR = "cc.calliope.mini_v2.BROADCAST_ERROR";
    public static final String EXTRA_ERROR = "cc.calliope.mini_v2.EXTRA_ERROR";
    public static final String EXTRA_MESSAGE = "cc.calliope.mini_v2.EXTRA_MESSAGE";
    //    public static final int PROGRESS_WAITING = 0;
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
    private DfuServiceReceiver dfuServiceReceiver;
    private PartialFlashingServiceReceiver partialFlashingServiceReceiver;
    private DfuControlSMReceiver dfuControlSMReceiver;

    private class DfuServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                //DfuService
                case DfuService.BROADCAST_PROGRESS -> {
                    int extra = intent.getIntExtra(DfuService.EXTRA_DATA, 0);
                    sendProgressBroadcast(extra);
                    if (extra == DfuService.PROGRESS_STARTING) {
                        sendFlashingBroadcast(true);
                    } else if (extra <= PROGRESS_DISCONNECTING) {
                        sendFlashingBroadcast(false);
                    }
                }
                case DfuService.BROADCAST_ERROR -> {
                    int code = intent.getIntExtra(DfuBaseService.EXTRA_DATA, 0);
                    int type = intent.getIntExtra(DfuBaseService.EXTRA_ERROR_TYPE, 0);
                    String errorMessage = switch (type) {
                        case DfuBaseService.ERROR_TYPE_COMMUNICATION_STATE ->
                                GattError.parseConnectionError(code);
                        case DfuBaseService.ERROR_TYPE_DFU_REMOTE ->
                                GattError.parseDfuRemoteError(code);
                        default -> GattError.parse(code);
                    };

                    sendProgressBroadcast(PROGRESS_ERROR);
                    sendFlashingBroadcast(false);
                    sendErrorBroadcast(code, errorMessage);
                }
            }
        }
    }

    private class PartialFlashingServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case PartialFlashingBaseService.BROADCAST_PROGRESS -> {
                    int percent = intent.getIntExtra(PartialFlashingBaseService.EXTRA_PROGRESS, 0);
                    sendProgressBroadcast(percent);
                }
                case PartialFlashingBaseService.BROADCAST_START -> {
                    sendProgressBroadcast(PROGRESS_STARTING);
                    sendFlashingBroadcast(true);
                }
                case PartialFlashingBaseService.BROADCAST_COMPLETE -> {
                    sendProgressBroadcast(PROGRESS_DISCONNECTING);
                    sendProgressBroadcast(PROGRESS_COMPLETED);
                    sendFlashingBroadcast(false);
                }
                case PartialFlashingBaseService.BROADCAST_PF_FAILED -> {
                    sendProgressBroadcast(PROGRESS_DISCONNECTING);
                    sendProgressBroadcast(PROGRESS_FAILED);
                    sendFlashingBroadcast(false);
                }
                case PartialFlashingBaseService.BROADCAST_PF_ATTEMPT_DFU -> {
                    //TODO ATTEMPT_DFU
                }
            }
        }
    }

    private class DfuControlSMReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(DfuControlServiceManager.BROADCAST_DFU_CONTROL_SERVICE)) {
                int extra = intent.getIntExtra(DfuControlServiceManager.EXTRA_DFU_CONTROL_SERVICE, 0);
                switch (extra) {
                    case DfuControlServiceManager.EXTRA_ENABLING, DfuControlServiceManager.EXTRA_DONE ->
                            sendProgressBroadcast(PROGRESS_ENABLING_DFU_MODE);
                    case DfuControlServiceManager.EXTRA_FAIL ->
                            sendProgressBroadcast(PROGRESS_FAILED);
                    default -> throw new IllegalStateException("Unexpected value: " + extra);
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
        registerDfuServiceReceiver();
        registerPartialFlashingServiceReceiver();
        registerDfuControlSMReceiver();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterDfuServiceReceiver();
        unregisterPartialFlashingServiceReceiver();
        unregisterDfuControlSMReceiver();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void registerDfuServiceReceiver() {
        if (dfuServiceReceiver == null) {
            dfuServiceReceiver = new DfuServiceReceiver();
            Utils.log(Log.WARN, TAG, "register DfuService Receiver");

            IntentFilter filter = new IntentFilter();
            filter.addAction(DfuService.BROADCAST_PROGRESS);
            filter.addAction(DfuService.BROADCAST_ERROR);

            LocalBroadcastManager.getInstance(context).registerReceiver(dfuServiceReceiver, filter);
        }
    }

    public void registerPartialFlashingServiceReceiver() {
        if (partialFlashingServiceReceiver == null) {
            partialFlashingServiceReceiver = new PartialFlashingServiceReceiver();
            Utils.log(Log.WARN, TAG, "register PartialFlashingService Receiver");

            IntentFilter filter = new IntentFilter();
            filter.addAction(PartialFlashingBaseService.BROADCAST_PROGRESS);
            filter.addAction(PartialFlashingBaseService.BROADCAST_START);
            filter.addAction(PartialFlashingBaseService.BROADCAST_COMPLETE);
            filter.addAction(PartialFlashingBaseService.BROADCAST_PF_FAILED);
            filter.addAction(PartialFlashingBaseService.BROADCAST_PF_ATTEMPT_DFU);

            LocalBroadcastManager.getInstance(context).registerReceiver(partialFlashingServiceReceiver, filter);
        }
    }

    //Dfu Control Service Manager Broadcast Receiver
    public void registerDfuControlSMReceiver() {
        if (dfuControlSMReceiver == null) {
            dfuControlSMReceiver = new DfuControlSMReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(DfuControlServiceManager.BROADCAST_DFU_CONTROL_SERVICE);
            context.registerReceiver(dfuControlSMReceiver, filter);
        }
    }

    public void unregisterDfuServiceReceiver() {
        if (dfuServiceReceiver != null) {
            Utils.log(Log.WARN, TAG, "unregister DfuService Receiver");
            LocalBroadcastManager.getInstance(context).unregisterReceiver(dfuServiceReceiver);
            dfuServiceReceiver = null;
        }
    }

    public void unregisterPartialFlashingServiceReceiver() {
        if (partialFlashingServiceReceiver != null) {
            Utils.log(Log.WARN, TAG, "unregister PartialFlashingService Receiver");
            LocalBroadcastManager.getInstance(context).unregisterReceiver(partialFlashingServiceReceiver);
            partialFlashingServiceReceiver = null;
        }
    }

    public void unregisterDfuControlSMReceiver() {
        if (dfuControlSMReceiver != null) {
            context.unregisterReceiver(dfuControlSMReceiver);
            dfuControlSMReceiver = null;
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