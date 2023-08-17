package cc.calliope.mini;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import org.microbit.android.partialflashing.PartialFlashingBaseService;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import cc.calliope.mini.service.DfuService;
import cc.calliope.mini.utils.Utils;
import no.nordicsemi.android.dfu.DfuBaseService;
import no.nordicsemi.android.error.GattError;

import static cc.calliope.mini.DfuControlService.UNIDENTIFIED;
import static cc.calliope.mini.DfuControlService.EXTRA_BOARD_VERSION;
import static cc.calliope.mini.DfuControlService.EXTRA_ERROR_MESSAGE;

public class ProgressReceiver extends BroadcastReceiver {
    private static final String TAG = "DfuServiceReceiver";
    private final Context context;
    private ProgressListener listener;

    public ProgressReceiver(@NonNull Context context) {
        this.context = context;
        if (context instanceof ProgressListener) {
            this.listener = (ProgressListener) context;
        }
    }

    public void setProgressListener(ProgressListener listener) {
        this.listener = listener;
    }

    public void registerReceiver() {
        Utils.log(Log.WARN, TAG, "register DfuService Receiver");

        IntentFilter filter = new IntentFilter();

        //DfuService
        filter.addAction(DfuService.BROADCAST_PROGRESS);
        filter.addAction(DfuService.BROADCAST_ERROR);

        //PartialFlashingService
        filter.addAction(PartialFlashingBaseService.BROADCAST_PROGRESS);
        filter.addAction(PartialFlashingBaseService.BROADCAST_START);
        filter.addAction(PartialFlashingBaseService.BROADCAST_COMPLETE);
        filter.addAction(PartialFlashingBaseService.BROADCAST_PF_FAILED);
        filter.addAction(PartialFlashingBaseService.BROADCAST_PF_ATTEMPT_DFU);

        //DfuControlServiceManager
        filter.addAction(DfuControlServiceManager.BROADCAST_DFU_CONTROL_SERVICE);

        //DfuControlService
        filter.addAction(DfuControlService.BROADCAST_START);
        filter.addAction(DfuControlService.BROADCAST_COMPLETED);
        filter.addAction(DfuControlService.BROADCAST_FAILED);
        filter.addAction(DfuControlService.BROADCAST_ERROR);

        LocalBroadcastManager.getInstance(context).registerReceiver(this, filter);
    }

    public void unregisterReceiver() {
        Utils.log(Log.WARN, TAG, "unregister DfuService Receiver");
        LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (listener != null) {
            switch (intent.getAction()) {
                //DfuService
                case DfuService.BROADCAST_PROGRESS -> {
                    int extra = intent.getIntExtra(DfuService.EXTRA_DATA, 0);
                    switch (extra) {
                        case DfuService.PROGRESS_CONNECTING -> listener.onDeviceConnecting();
                        case DfuService.PROGRESS_STARTING -> listener.onProcessStarting();
                        case DfuService.PROGRESS_ENABLING_DFU_MODE -> listener.onEnablingDfuMode();
                        case DfuService.PROGRESS_VALIDATING -> listener.onFirmwareValidating();
                        case DfuService.PROGRESS_DISCONNECTING -> listener.onDeviceDisconnecting();
                        case DfuService.PROGRESS_COMPLETED -> listener.onCompleted();
                        case DfuService.PROGRESS_ABORTED -> listener.onAborted();
                        default -> listener.onProgressChanged(extra);
                    }
                }
                case DfuService.BROADCAST_ERROR -> {
                    int code = intent.getIntExtra(DfuBaseService.EXTRA_DATA, 0);
                    int type = intent.getIntExtra(DfuBaseService.EXTRA_ERROR_TYPE, 0);
                    String message = switch (type) {
                        case DfuBaseService.ERROR_TYPE_COMMUNICATION_STATE ->
                                GattError.parseConnectionError(code);
                        case DfuBaseService.ERROR_TYPE_DFU_REMOTE ->
                                GattError.parseDfuRemoteError(code);
                        default -> GattError.parse(code);
                    };
                    listener.onError(code, message);
                }
                //PartialFlashingService
                case PartialFlashingBaseService.BROADCAST_PROGRESS -> {
                    int extra = intent.getIntExtra(PartialFlashingBaseService.EXTRA_PROGRESS, 0);
                    listener.onProgressChanged(extra);
                }
                case PartialFlashingBaseService.BROADCAST_START -> listener.onProcessStarting();
                case PartialFlashingBaseService.BROADCAST_COMPLETE -> listener.onCompleted();
                case PartialFlashingBaseService.BROADCAST_PF_FAILED -> {
                    listener.onDeviceDisconnecting();
                    listener.onError(-1, "Partial Flashing FAILED");
                }
                case PartialFlashingBaseService.BROADCAST_PF_ATTEMPT_DFU -> {
                    //TODO ATTEMPT_DFU
                }
                //DfuControlServiceManager
                case DfuControlServiceManager.BROADCAST_DFU_CONTROL_SERVICE -> {
                    int extra = intent.getIntExtra(DfuControlServiceManager.EXTRA_DFU_CONTROL_SERVICE, 0);
                    switch (extra) {
                        case DfuControlServiceManager.EXTRA_ENABLING, DfuControlServiceManager.EXTRA_DONE ->
                                listener.onEnablingDfuMode();
                        case DfuControlServiceManager.EXTRA_FAIL -> {
                            listener.onDeviceDisconnecting();
                            listener.onError(-1, "Enabling Dfu Mode FAILED");
                        }
                    }
                }
                //DfuControlService
                case DfuControlService.BROADCAST_START -> listener.onEnablingDfuMode();
                case DfuControlService.BROADCAST_COMPLETED -> {
                    int boardVersion = intent.getIntExtra(EXTRA_BOARD_VERSION, UNIDENTIFIED);
                    listener.onStartDfuService(boardVersion);
                }
                case DfuControlService.BROADCAST_FAILED -> listener.onDeviceDisconnecting();
                case DfuControlService.BROADCAST_ERROR -> {
                    String message = intent.getStringExtra(EXTRA_ERROR_MESSAGE);
                    listener.onError(-1, message);
                }
            }
        }
    }
}