package cc.calliope.mini_v2.viewmodels;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import cc.calliope.mini_v2.service.DfuService;

public class ProgressViewModel extends AndroidViewModel {

    private final BroadcastReceiver dfuResultReceiver;
    private final MutableLiveData<Integer> progress;

    public ProgressViewModel(@NonNull Application application) {
        super(application);

        Log.e("PROGRESS", "register");
        progress = new MutableLiveData<>();
        progress.setValue(0);
        dfuResultReceiver = new DFUResultReceiver();
        registerCallbacksForFlashing();
    }

    @Override
    protected void onCleared() {
        LocalBroadcastManager.getInstance(getApplication()).unregisterReceiver(dfuResultReceiver);
        Log.e("PROGRESS", "onCleared");
    }

    private void registerCallbacksForFlashing() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(DfuService.BROADCAST_PROGRESS);
        filter.addAction(DfuService.BROADCAST_ERROR);

        LocalBroadcastManager.getInstance(getApplication()).registerReceiver(dfuResultReceiver, filter);
    }

    public LiveData<Integer> getProgress() {
        return progress;
    }

    private class DFUResultReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(DfuService.BROADCAST_ERROR)) {
                String state = intent.getStringExtra(DfuService.EXTRA_DATA);
                Log.e("DFULogStatus", "ERROR: " + state);
            } else if (intent.getAction().equals(DfuService.BROADCAST_PROGRESS)) {
                int state = intent.getIntExtra(DfuService.EXTRA_DATA, 0);
                progress.setValue(state);
            }
        }
    }
}