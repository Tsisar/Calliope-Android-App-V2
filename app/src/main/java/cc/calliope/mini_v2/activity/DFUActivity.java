package cc.calliope.mini_v2.activity;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import cc.calliope.mini_v2.FlashingManager;
import cc.calliope.mini_v2.PartialFlashingService;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.adapter.ExtendedBluetoothDevice;
import cc.calliope.mini_v2.databinding.ActivityDfuBinding;
import cc.calliope.mini_v2.service.DfuService;
import cc.calliope.mini_v2.utils.StaticExtra;
import cc.calliope.mini_v2.views.BoardProgressBar;
import no.nordicsemi.android.ble.PhyRequest;
import no.nordicsemi.android.dfu.DfuBaseService;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

public class DFUActivity extends AppCompatActivity {
    private static final String TAG = "DFUActivity";
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

    public void log(int priority, @NonNull String message) {
        // Log from here.
        Log.println(priority, TAG, "### " + Thread.currentThread().getId() + " # " + message);
    }

    private final DfuProgressListener progressListener = new DfuProgressListenerAdapter() {
        @Override
        public void onDeviceConnecting(@NonNull final String deviceAddress) {
            log(Log.INFO, Thread.currentThread().getStackTrace()[2].getMethodName());

            timerTextView.setText(R.string.flashing_device_connecting);
        }

        @Override
        public void onDfuProcessStarting(@NonNull final String deviceAddress) {
            log(Log.INFO, Thread.currentThread().getStackTrace()[2].getMethodName());

            timerTextView.setText(R.string.flashing_process_starting);
        }

        @Override
        public void onEnablingDfuMode(@NonNull final String deviceAddress) {
            log(Log.INFO, Thread.currentThread().getStackTrace()[2].getMethodName());

            timerTextView.setText(R.string.flashing_enabling_dfu_mode);
        }

        @Override
        public void onFirmwareValidating(@NonNull final String deviceAddress) {
            log(Log.INFO, Thread.currentThread().getStackTrace()[2].getMethodName());

            timerTextView.setText(R.string.flashing_firmware_validating);
        }

        @Override
        public void onDeviceDisconnecting(@NonNull final String deviceAddress) {
            log(Log.INFO, Thread.currentThread().getStackTrace()[2].getMethodName());

            timerTextView.setText(R.string.flashing_device_disconnecting);
        }

        @Override
        public void onDfuCompleted(@NonNull final String deviceAddress) {
            log(Log.WARN, Thread.currentThread().getStackTrace()[2].getMethodName());

            timerTextView.setText(R.string.flashing_dfu_completed);
            timerHandler.postDelayed(deferredFinish, DELAY_TO_FINISH_ACTIVITY);
            progressBar.setProgress(DfuService.PROGRESS_COMPLETED);
        }

        @Override
        public void onDfuAborted(@NonNull final String deviceAddress) {
            log(Log.WARN, Thread.currentThread().getStackTrace()[2].getMethodName());

            timerTextView.setText(R.string.flashing_dfu_aborted);
            timerHandler.postDelayed(deferredFinish, DELAY_TO_FINISH_ACTIVITY);
        }

        @Override
        public void onProgressChanged(@NonNull final String deviceAddress, final int percent,
                                      final float speed, final float avgSpeed,
                                      final int currentPart, final int partsTotal) {

            statusTextView.setText(String.format(getString(R.string.flashing_percent), percent));
            timerTextView.setText(R.string.flashing_uploading);
            progressBar.setProgress(percent);
        }

        @Override
        public void onError(@NonNull final String deviceAddress, final int error, final int errorType, final String message) {
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            log(Log.ERROR, method + " " + message);

            statusTextView.setText(R.string.flashing_error);
            timerTextView.setText(message);
            timerHandler.postDelayed(deferredFinish, DELAY_TO_FINISH_ACTIVITY);
        }
    };

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
        DfuServiceListenerHelper.registerProgressListener(this, progressListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        DfuServiceListenerHelper.unregisterProgressListener(this, progressListener);
    }

    private void initFlashing() {
        Intent intent = getIntent();
        ExtendedBluetoothDevice extendedDevice = intent.getParcelableExtra(StaticExtra.EXTRA_DEVICE);

        if (extendedDevice == null) {
            return;
        }

        log(Log.INFO, "device: " + extendedDevice.getAddress() + " " + extendedDevice.getName());
        log(Log.INFO, "Init flashing...");

        initPartialFlashingService(extendedDevice);
        //initBootloader(extendedDevice);
    }

    private void initPartialFlashingService(ExtendedBluetoothDevice extendedDevice){
        log(Log.WARN, "Start Partial Flash...");

        Bundle extras = getIntent().getExtras();
        String filePath = extras.getString(StaticExtra.EXTRA_FILE_PATH);

        final Intent service = new Intent(this, PartialFlashingService.class);
        service.putExtra("deviceAddress", extendedDevice.getAddress());
        service.putExtra("filepath", filePath); // a path or URI must be provided.

        startService(service);
    }

    private void initBootloader(ExtendedBluetoothDevice extendedDevice) {
        log(Log.INFO, "Init bootloader...");
        FlashingManager flashingManager = new FlashingManager(this);
        flashingManager.connect(extendedDevice.getDevice())
                // Automatic retries are supported, in case of 133 error.
                .retry(NUMBER_OF_RETRIES, INTERVAL_OF_RETRIES)
                .timeout(CONNECTION_TIMEOUT)
                .usePreferredPhy(PhyRequest.PHY_LE_1M_MASK | PhyRequest.PHY_LE_2M_MASK)
//                .done(this::startFlashing)
                .fail(this::connectionFail)
                .enqueue();
        flashingManager.setOnDisconnectListener(() -> {
            flashingManager.close();
            startFlashing(extendedDevice);
        });
    }

    @SuppressWarnings("deprecation")
    private void startFlashing(ExtendedBluetoothDevice extendedDevice) {
        log(Log.INFO, "Starting flashing...");
        Bundle extras = getIntent().getExtras();
        String filePath = extras.getString(StaticExtra.EXTRA_FILE_PATH);

        log(Log.DEBUG, "Address: " + extendedDevice.getAddress());
        log(Log.DEBUG, "Pattern: " + extendedDevice.getPattern());
        log(Log.DEBUG, "File path: " + filePath);

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