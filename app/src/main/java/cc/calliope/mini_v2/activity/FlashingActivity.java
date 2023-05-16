package cc.calliope.mini_v2.activity;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import cc.calliope.mini_v2.FlashingManager;
import cc.calliope.mini_v2.PartialFlashingService;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.StateService;
import cc.calliope.mini_v2.adapter.ExtendedBluetoothDevice;
import cc.calliope.mini_v2.databinding.ActivityDfuBinding;
import cc.calliope.mini_v2.service.DfuService;
import cc.calliope.mini_v2.utils.StaticExtra;
import cc.calliope.mini_v2.utils.Utils;
import cc.calliope.mini_v2.views.BoardProgressBar;
import no.nordicsemi.android.ble.PhyRequest;
import no.nordicsemi.android.dfu.DfuBaseService;
import no.nordicsemi.android.dfu.DfuServiceInitiator;

public class FlashingActivity extends AppCompatActivity {
    private static final String TAG = "FlashingActivity";
    private static final int NUMBER_OF_RETRIES = 3;
    private static final int INTERVAL_OF_RETRIES = 500; // ms
    private static final int REBOOT_TIME = 2000; // time required by the device to reboot, ms
    private static final long CONNECTION_TIMEOUT = 10000; // default connection timeout is 30000 ms
    private static final int DELAY_TO_FINISH_ACTIVITY = 5000; //delay to finish activity after flashing
    private ActivityDfuBinding binding;
    private TextView statusTextView;
    private TextView timerTextView;
    private BoardProgressBar progressBar;
    private final Handler timerHandler = new Handler();
    private final Runnable deferredFinish = this::finish;
    private ProgressReceiver broadcastReceiver;

    public void log(int priority, @NonNull String message) {
        // Log from here.
        Log.println(priority, TAG, "### " + Thread.currentThread().getId() + " # " + message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDfuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        statusTextView = binding.statusTextView;
        timerTextView = binding.timerTextView;
        progressBar = binding.progressBar;

//        flashingManager = new FlashingManager(this);

        initFlashing();
    }

    @Override
    protected void onDestroy() {
//        flashingManager.close();
        binding = null;
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerBroadcastReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterBroadcastReceiver();
    }

    public void registerBroadcastReceiver() {
        if (broadcastReceiver == null) {
            broadcastReceiver = new ProgressReceiver();
            Utils.log(Log.WARN, TAG, "register Progress Receiver");
            IntentFilter filter = new IntentFilter();
            filter.addAction(StateService.BROADCAST_PROGRESS);
            filter.addAction(StateService.BROADCAST_ERROR);

            getApplication().registerReceiver(broadcastReceiver, filter);
        }
    }

    public void unregisterBroadcastReceiver() {
        if (broadcastReceiver != null) {
            Utils.log(Log.WARN, TAG, "unregister Progress Receiver");
            getApplication().unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
    }

    private class ProgressReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(StateService.BROADCAST_PROGRESS)) {
                int progress = intent.getIntExtra(StateService.EXTRA_PROGRESS, StateService.PROGRESS_WAITING);
                switch (progress) {
                    case StateService.PROGRESS_CONNECTING ->
                            timerTextView.setText(R.string.flashing_device_connecting);
                    case StateService.PROGRESS_STARTING ->
                            timerTextView.setText(R.string.flashing_process_starting);
                    case StateService.PROGRESS_ENABLING_DFU_MODE ->
                            timerTextView.setText(R.string.flashing_enabling_dfu_mode);
                    case StateService.PROGRESS_VALIDATING ->
                            timerTextView.setText(R.string.flashing_firmware_validating);
                    case StateService.PROGRESS_DISCONNECTING ->
                            timerTextView.setText(R.string.flashing_device_disconnecting);
                    case StateService.PROGRESS_COMPLETED -> {
                        statusTextView.setText(String.format(getString(R.string.flashing_percent), 100));
                        timerTextView.setText(R.string.flashing_completed);
                        timerHandler.postDelayed(deferredFinish, DELAY_TO_FINISH_ACTIVITY);
                        progressBar.setProgress(DfuService.PROGRESS_COMPLETED);
                    }
                    case StateService.PROGRESS_ABORTED, StateService.PROGRESS_FAILED -> {
                        timerTextView.setText(R.string.flashing_aborted);
                        timerHandler.postDelayed(deferredFinish, DELAY_TO_FINISH_ACTIVITY);
                    }
                    default -> {
                        if (progress >= 0 && progress <= 100) {
                            statusTextView.setText(String.format(getString(R.string.flashing_percent), progress));
                            timerTextView.setText(R.string.flashing_uploading);
                            progressBar.setProgress(progress);
                        }
                    }
                }
            } else if (action.equals(StateService.BROADCAST_ERROR)) {
                int code = intent.getIntExtra(StateService.EXTRA_ERROR, 0);
                String message = intent.getStringExtra(StateService.EXTRA_MESSAGE);

                statusTextView.setText(String.format(getString(R.string.flashing_error), code));
                timerTextView.setText(message);
                timerHandler.postDelayed(deferredFinish, DELAY_TO_FINISH_ACTIVITY);
            }
        }
    }

    private void initFlashing() {
        ExtendedBluetoothDevice extendedDevice;
        String filePath;

        try {
            Intent intent = getIntent();
            extendedDevice = intent.getParcelableExtra(StaticExtra.EXTRA_DEVICE);
            filePath = intent.getStringExtra(StaticExtra.EXTRA_FILE_PATH);
        } catch (NullPointerException exception) {
            log(Log.ERROR, "NullPointerException: " + exception.getMessage());
            return;
        }

        if (extendedDevice == null || filePath == null) {
            return;
        }

        log(Log.INFO, "device: " + extendedDevice.getAddress() + " " + extendedDevice.getName());
        log(Log.INFO, "filePath: " + filePath);

        //initFlashingManager(extendedDevice, filePath);
        startDfuService(extendedDevice, filePath);
    }

    private void initFlashingManager(ExtendedBluetoothDevice extendedDevice, String filePath) {
        log(Log.INFO, "Init Flashing...");
        FlashingManager flashingManager = new FlashingManager(this, filePath);
        flashingManager.connect(extendedDevice.getDevice())
                // Automatic retries are supported, in case of 133 error.
                .retry(NUMBER_OF_RETRIES, INTERVAL_OF_RETRIES)
                .timeout(CONNECTION_TIMEOUT)
                .usePreferredPhy(PhyRequest.PHY_LE_1M_MASK | PhyRequest.PHY_LE_2M_MASK)
//                .done(this::startFlashing)
                .fail(this::connectionFail)
                .enqueue();
        flashingManager.setOnInvalidateListener(new FlashingManager.OnInvalidateListener() {
            @Override
            public void onDisconnect() {
                flashingManager.close();
            }

            @Override
            public void onStartDFUService() {
                startDfuService(extendedDevice, filePath);
            }

            @Override
            public void onStartPartialFlashingService() {
                startPartialFlashingService(extendedDevice, filePath);
            }
        });
    }

    private void startPartialFlashingService(ExtendedBluetoothDevice extendedDevice, String filePath) {
        log(Log.INFO, "Starting PartialFlashing Service...");

        Intent service = new Intent(this, PartialFlashingService.class);
        service.putExtra("deviceAddress", extendedDevice.getAddress());
        service.putExtra("filepath", filePath); // a path or URI must be provided.

        startService(service);
    }

    @SuppressWarnings("deprecation")
    private void startDfuService(ExtendedBluetoothDevice extendedDevice, String filePath) {
        log(Log.INFO, "Starting DFU Service...");

        final DfuServiceInitiator starter = new DfuServiceInitiator(extendedDevice.getAddress())
                .setDeviceName(extendedDevice.getPattern())
                //TODO Modify HexInputStream
                //.setMbrSize(0x18000)
                .setMbrSize(0x27000)
                .setPrepareDataObjectDelay(300L)
                .setNumberOfRetries(NUMBER_OF_RETRIES)
                .setRebootTime(REBOOT_TIME)
                .setKeepBond(false);

        starter.setBinOrHex(DfuBaseService.TYPE_APPLICATION, filePath);
        starter.start(this, DfuService.class);
    }

    private void connectionFail(BluetoothDevice device, int status) {
        log(Log.ERROR, "Connection error, device " + device.getAddress() + ", status: " + status);
        statusTextView.setText(R.string.flashing_connection_fail);
        timerTextView.setText(String.format(getString(R.string.flashing_status), status));
    }
}