package cc.calliope.mini_v2.activity;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import cc.calliope.mini_v2.FlashingManager;
import cc.calliope.mini_v2.PartialFlashingService;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.adapter.ExtendedBluetoothDevice;
import cc.calliope.mini_v2.databinding.ActivityDfuBinding;
import cc.calliope.mini_v2.service.DfuService;
import cc.calliope.mini_v2.utils.StaticExtra;
import cc.calliope.mini_v2.viewmodels.ProgressLiveData;
import cc.calliope.mini_v2.viewmodels.ProgressViewModel;
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
    private ProgressViewModel progressViewModel;


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

        progressViewModel = new ViewModelProvider(this).get(ProgressViewModel.class);
        progressViewModel.getProgress().observe(this, this::setFlashingProcess);

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
        progressViewModel.registerBroadcastReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        progressViewModel.unregisterBroadcastReceiver();
    }

    private void setFlashingProcess(ProgressLiveData progress) {
        switch (progress.getProgress()) {
            case DfuBaseService.PROGRESS_CONNECTING ->
                    timerTextView.setText(R.string.flashing_device_connecting);
            case DfuBaseService.PROGRESS_STARTING ->
                    timerTextView.setText(R.string.flashing_process_starting);
            case DfuBaseService.PROGRESS_ENABLING_DFU_MODE ->
                    timerTextView.setText(R.string.flashing_enabling_dfu_mode);
            case DfuBaseService.PROGRESS_VALIDATING ->
                    timerTextView.setText(R.string.flashing_firmware_validating);
            case DfuBaseService.PROGRESS_DISCONNECTING ->
                    timerTextView.setText(R.string.flashing_device_disconnecting);
            case DfuBaseService.PROGRESS_COMPLETED -> {
                statusTextView.setText(String.format(getString(R.string.flashing_percent), 100));
                timerTextView.setText(R.string.flashing_completed);
                timerHandler.postDelayed(deferredFinish, DELAY_TO_FINISH_ACTIVITY);
                progressBar.setProgress(DfuService.PROGRESS_COMPLETED);
            }
            case DfuBaseService.PROGRESS_ABORTED -> {
                timerTextView.setText(R.string.flashing_aborted);
                timerHandler.postDelayed(deferredFinish, DELAY_TO_FINISH_ACTIVITY);
            }
            case ProgressViewModel.PROGRESS_ERROR ->{
                statusTextView.setText(String.format(getString(R.string.flashing_error), progress.getErrorCode()));
                timerTextView.setText(progress.getErrorMessage());
                timerHandler.postDelayed(deferredFinish, DELAY_TO_FINISH_ACTIVITY);
            }
            default -> {
                statusTextView.setText(String.format(getString(R.string.flashing_percent), progress.getProgress()));
                timerTextView.setText(R.string.flashing_uploading);
                progressBar.setProgress(progress.getProgress());
            }

        }
    }

    private void initFlashing() {
        Intent intent = getIntent();
        ExtendedBluetoothDevice extendedDevice = intent.getParcelableExtra(StaticExtra.EXTRA_DEVICE);
        String filePath = intent.getExtras().getString(StaticExtra.EXTRA_FILE_PATH);

        if (extendedDevice == null) {
            return;
        }

        log(Log.INFO, "device: " + extendedDevice.getAddress() + " " + extendedDevice.getName());
        log(Log.INFO, "filePath: " + filePath);

        initFlashing(extendedDevice, filePath);
    }

    private void initFlashing(ExtendedBluetoothDevice extendedDevice, String filePath) {
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

    private void startPartialFlashingService(ExtendedBluetoothDevice extendedDevice,  String filePath) {
        log(Log.INFO, "Starting PartialFlashing Service...");

        Intent service = new Intent(this, PartialFlashingService.class);
        service.putExtra("deviceAddress", extendedDevice.getAddress());
        service.putExtra("filepath", filePath); // a path or URI must be provided.

        startService(service);
    }

    @SuppressWarnings("deprecation")
    private void startDfuService(ExtendedBluetoothDevice extendedDevice,  String filePath) {
        log(Log.INFO, "Starting DFU Service...");

        final DfuServiceInitiator starter = new DfuServiceInitiator(extendedDevice.getAddress())
                .setDeviceName(extendedDevice.getPattern())
                //TODO Modify HexInputStream
                .setMbrSize(0x18000)
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