package cc.calliope.mini_v2.viewmodels;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import org.microbit.android.partialflashing.PartialFlashingBaseService;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import cc.calliope.mini_v2.service.DfuService;
import no.nordicsemi.android.dfu.DfuBaseService;
import no.nordicsemi.android.error.GattError;


public class ProgressViewModel extends AndroidViewModel {
    private static final String TAG = "ProgressViewModel";
    public static final int PROGRESS_ERROR = -100;
    private BroadcastReceiver broadcastReceiver;
    private final ProgressLiveData progress;
    private final Application application;

    public void log(int priority, @NonNull String message) {
        // Log from here.
        String hash = "";
        if (broadcastReceiver != null) {
            hash = Integer.toHexString(broadcastReceiver.hashCode());
        }

        Log.println(priority, TAG, "### " + Thread.currentThread().getId() + " # " + hash + " # " + message);
    }

    public ProgressViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
        progress = new ProgressLiveData();
    }

    @Override
    public void onCleared() {
        unregisterBroadcastReceiver();
    }

    public void registerBroadcastReceiver() {
        if (broadcastReceiver == null) {
            broadcastReceiver = new FlashingResultReceiver();
            log(Log.WARN, "register Broadcast Receiver");
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

//          application.registerReceiver(broadcastReceiver, filter);
            LocalBroadcastManager.getInstance(application).registerReceiver(broadcastReceiver, filter);
        }
    }

    public void unregisterBroadcastReceiver() {
        if (broadcastReceiver != null) {
            log(Log.WARN, "unregister Broadcast Receiver");
//          application.unregisterReceiver(broadcastReceiver);
            LocalBroadcastManager.getInstance(application).unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
    }

    public ProgressLiveData getProgress() {
        return progress;
    }

    private class FlashingResultReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case DfuService.BROADCAST_PROGRESS -> {
                    int value = intent.getIntExtra(DfuService.EXTRA_DATA, 0);
                    progress.setProgress(value);
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

                    progress.setProgress(PROGRESS_ERROR);
                    progress.setError(errorCode, errorMessage);
                    log(Log.ERROR, "ERROR: " + errorCode + ", " + errorMessage);
                }
                case PartialFlashingBaseService.BROADCAST_PROGRESS -> {
                    int value = intent.getIntExtra(PartialFlashingBaseService.EXTRA_PROGRESS, 0);
                    progress.setProgress(value);
                }
                case PartialFlashingBaseService.BROADCAST_START ->
                        progress.setProgress(DfuBaseService.PROGRESS_STARTING);
                case PartialFlashingBaseService.BROADCAST_COMPLETE ->
                        progress.setProgress(DfuBaseService.PROGRESS_COMPLETED);
                case PartialFlashingBaseService.BROADCAST_PF_FAILED ->
                        progress.setProgress(DfuBaseService.PROGRESS_ABORTED);
            }
        }
    }
}