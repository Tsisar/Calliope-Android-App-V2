package cc.calliope.mini_v2;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
    private TextView statusTextView;
    private TextView timerTextView;
    private BoardProgressBar progressBar;
    private String pattern;
    private boolean onPause;
    private FlashingManager flashingManager;
    private final Handler timerHandler = new Handler();
    private final Runnable deferredFinish = this::finish;

    private final DfuProgressListener progressListener = new DfuProgressListenerAdapter() {
        @Override
        public void onDeviceConnecting(@NonNull final String deviceAddress) {
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.w(TAG, "### " + Thread.currentThread().getId() + " # " + method);

            timerTextView.setText(R.string.flashing_device_connecting);
        }

        @Override
        public void onDfuProcessStarting(@NonNull final String deviceAddress) {
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.w(TAG, "### " + Thread.currentThread().getId() + " # " + method);

            timerTextView.setText(R.string.flashing_process_starting);
        }

        @Override
        public void onEnablingDfuMode(@NonNull final String deviceAddress) {
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.w(TAG, "### " + Thread.currentThread().getId() + " # " + method);

            timerTextView.setText(R.string.flashing_enabling_dfu_mode);
        }

        @Override
        public void onFirmwareValidating(@NonNull final String deviceAddress) {
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.w(TAG, "### " + Thread.currentThread().getId() + " # " + method);

            timerTextView.setText(R.string.flashing_firmware_validating);
        }

        @Override
        public void onDeviceDisconnecting(@NonNull final String deviceAddress) {
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.w(TAG, "### " + Thread.currentThread().getId() + " # " + method);

            timerTextView.setText(R.string.flashing_device_disconnecting);
        }

        @Override
        public void onDfuCompleted(@NonNull final String deviceAddress) {
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.w(TAG, "### " + Thread.currentThread().getId() + " # " + method);

            timerTextView.setText(R.string.flashing_dfu_completed);
            timerHandler.postDelayed(deferredFinish, DELAY_TO_FINISH_ACTIVITY);
            progressBar.setProgress(DfuService.PROGRESS_COMPLETED);
        }

        @Override
        public void onDfuAborted(@NonNull final String deviceAddress) {
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.w(TAG, "### " + Thread.currentThread().getId() + " # " + method);

            timerTextView.setText(R.string.flashing_dfu_aborted);
            timerHandler.postDelayed(deferredFinish, DELAY_TO_FINISH_ACTIVITY);
        }

        @Override
        public void onProgressChanged(@NonNull final String deviceAddress, final int percent,
                                      final float speed, final float avgSpeed,
                                      final int currentPart, final int partsTotal) {
//            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
//            Log.w(TAG, "### " + Thread.currentThread().getId() + " # " + method + " percent: " + percent +
//                    "; speed: " + speed + "; avgSpeed: " + avgSpeed +
//                    "; currentPart " + currentPart + "; partsTotal: " + partsTotal + ";");

            if (!onPause) {
                statusTextView.setText(String.format(getString(R.string.flashing_percent), percent));
                timerTextView.setText(R.string.flashing_uploading);
                progressBar.setProgress(percent);
            }
        }

        @Override
        public void onError(@NonNull final String deviceAddress, final int error, final int errorType, final String message) {
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.w(TAG, "### " + Thread.currentThread().getId() + " # " + method + " " + message);

            statusTextView.setText(R.string.flashing_error);
            timerTextView.setText(message);
            timerHandler.postDelayed(deferredFinish, DELAY_TO_FINISH_ACTIVITY);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityDfuBinding binding = ActivityDfuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        statusTextView = binding.statusTextView;
        timerTextView = binding.timerTextView;
        progressBar = binding.progressBar;

        flashingManager = new FlashingManager(this);

        initFlashing();
    }

    @Override
    protected void onDestroy() {
        flashingManager.close();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        onPause = false;
        DfuServiceListenerHelper.registerProgressListener(this, progressListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        onPause = true;
        DfuServiceListenerHelper.unregisterProgressListener(this, progressListener);
    }

    private void initFlashing() {
        Intent intent = getIntent();
        ExtendedBluetoothDevice device = intent.getParcelableExtra(StaticExtra.EXTRA_DEVICE);

        if (device == null) {
            return;
        }
        pattern = device.getPattern();

        Log.i(TAG, "device: " + device.getAddress() + " " + device.getName());
        Log.d(TAG, "### " + Thread.currentThread().getId() + " # " + "Init flashing...");

        connect(device.getDevice());
    }

    private void connect(BluetoothDevice device) {
        Log.d(TAG, "### " + Thread.currentThread().getId() + " # " + "Connecting...");
        flashingManager.connect(device)
                // Automatic retries are supported, in case of 133 error.
                .retry(NUMBER_OF_RETRIES, INTERVAL_OF_RETRIES)
                .timeout(CONNECTION_TIMEOUT)
                .usePreferredPhy(PhyRequest.PHY_LE_1M_MASK | PhyRequest.PHY_LE_2M_MASK)
                .done(this::writeCharacteristic)
                .fail(this::connectionFail)
                .enqueue();
    }

    private void writeCharacteristic(BluetoothDevice device) {
        Log.d(TAG, "### " + Thread.currentThread().getId() + " # " + "Done");
        Log.d(TAG, "### " + Thread.currentThread().getId() + " # " + "Writing characteristic...");
        flashingManager.writeCharacteristic()
                .done(this::done)
                .fail(this::fail)
                .then(this::disconnect)
                .enqueue();
    }

    private void disconnect(BluetoothDevice device) {
        Log.d(TAG, "### " + Thread.currentThread().getId() + " # " + "Disconnecting...");
        flashingManager.disconnect()
                .done(this::done)
                .fail(this::fail)
                .then(this::startFlashing)
                .enqueue();
    }

    private void startFlashing(BluetoothDevice device) {
        Log.w(TAG, "### " + Thread.currentThread().getId() + " # " + "Starting flashing");
        Bundle extras = getIntent().getExtras();
        String filePath = extras.getString(StaticExtra.EXTRA_FILE_PATH);

        Log.i(TAG, "Address: " + device.getAddress());
        Log.i(TAG, "Pattern: " + pattern);
        Log.i(TAG, "File path: " + filePath);
        Log.i(TAG, "Start flashing");

        final DfuServiceInitiator starter = new DfuServiceInitiator(device.getAddress())
                .setDeviceName(pattern)
                //TODO Modify HexInputStream
                .setMbrSize(0x18000)
                .setNumberOfRetries(NUMBER_OF_RETRIES)
                .setRebootTime(REBOOT_TIME)
                .setKeepBond(false);

        starter.setBinOrHex(DfuBaseService.TYPE_APPLICATION, filePath);
        starter.start(this, DfuService.class);
    }

    private void done(BluetoothDevice device) {
        Log.d(TAG, "### " + Thread.currentThread().getId() + " # " + "Done");
    }

    private void fail(BluetoothDevice device, int status) {
        Log.e(TAG, "### " + Thread.currentThread().getId() + " # " + "Fail, status " + status);
    }

    private void connectionFail(BluetoothDevice device, int status) {
        Log.e(TAG, "Connection error, device " + device.getAddress() + ", status: " + status);
        statusTextView.setText(R.string.flashing_connection_fail);
        timerTextView.setText(String.format(getString(R.string.flashing_status), status));
    }
}