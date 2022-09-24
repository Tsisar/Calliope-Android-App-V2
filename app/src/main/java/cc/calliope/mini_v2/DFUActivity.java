package cc.calliope.mini_v2;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import cc.calliope.mini_v2.adapter.ExtendedBluetoothDevice;
import cc.calliope.mini_v2.service.DfuService;
import no.nordicsemi.android.dfu.DfuBaseService;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

public class DFUActivity extends AppCompatActivity {

    private TextView deviceInfo;
    private TextView timerText;
    private ProgressBar progressBar;

    private static final String TAG = DFUActivity.class.getSimpleName();

    public static final String BROADCAST_PROGRESS = "org.microbit.android.partialflashing.broadcast.BROADCAST_PROGRESS";
    public static final String EXTRA_PROGRESS = "org.microbit.android.partialflashing.extra.EXTRA_PROGRESS";

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(EXTRA_PROGRESS);
            Log.e("PF receiver", "PROGRESS: " + message);
        }
    };

    private final DfuProgressListener mDfuProgressListener = new DfuProgressListenerAdapter() {
        @Override
        public void onDeviceConnecting(@NonNull final String deviceAddress) {
            timerText.setText("Device Connecting");
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.e(TAG, method);
        }

        @Override
        public void onDfuProcessStarting(@NonNull final String deviceAddress) {
            timerText.setText("Process Starting");
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.e(TAG, method);
        }

        @Override
        public void onEnablingDfuMode(@NonNull final String deviceAddress) {
            timerText.setText("Enabling Dfu Mode");
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.e(TAG, method);
        }

        @Override
        public void onFirmwareValidating(@NonNull final String deviceAddress) {
            timerText.setText("Firmware Validating");
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.e(TAG, method);
        }

        @Override
        public void onDeviceDisconnecting(@NonNull final String deviceAddress) {
            timerText.setText("Device Disconnecting");
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.e(TAG, method);
        }

        @Override
        public void onDfuCompleted(@NonNull final String deviceAddress) {
            timerText.setText("Dfu Completed");
            finish();
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.e(TAG, method);
        }

        @Override
        public void onDfuAborted(@NonNull final String deviceAddress) {
            timerText.setText("Dfu Aborted");
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.e(TAG, method);
        }

        @Override
        public void onProgressChanged(@NonNull final String deviceAddress, final int percent, final float speed, final float avgSpeed, final int currentPart, final int partsTotal) {
            deviceInfo.setText(percent + "%");
            timerText.setText("Uploading");
            progressBar.setProgress(39 + percent / 3);

            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.e(TAG, method + " percent: " + percent);
        }

        @Override
        public void onError(@NonNull final String deviceAddress, final int error, final int errorType, final String message) {
            deviceInfo.setText("ERROR");
            timerText.setText(message);
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.e(TAG, method + " error: " + message);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dfu);

//        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
//                new IntentFilter(BROADCAST_PROGRESS));

        deviceInfo = findViewById(R.id.statusInfo);
        timerText = findViewById(R.id.timerText);
        progressBar = findViewById(R.id.progressBar);

        timerText.setText("Device Connecting");

        initiateFlashing();
    }

    @Override
    protected void onDestroy() {
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();

        DfuServiceListenerHelper.registerProgressListener(this, mDfuProgressListener);
    }

    @Override
    public void onPause() {
        super.onPause();

        DfuServiceListenerHelper.unregisterProgressListener(this, mDfuProgressListener);
    }

    /**
     * Prepares for flashing process.
     * <p/>
     * <p>>Unregisters DFU receiver, sets activity state to the find device state,
     * registers callbacks requisite for flashing and starts flashing.</p>
     */
    protected void initiateFlashing() {
        startFlashing();
    }

    protected void startFlashingPF() {
        final Intent intent = getIntent();
        final ExtendedBluetoothDevice device = intent.getParcelableExtra("cc.calliope.mini.EXTRA_DEVICE");

        Bundle extras = intent.getExtras();
        final String filePath = extras.getString("EXTRA_FILE");
        final String deviceAddress = device.getAddress();

        Log.v("MicrobitDFU", "Start Partial Flash");

        final Intent service = new Intent(this, PartialFlashingService.class);
        service.putExtra("deviceAddress", deviceAddress);
        service.putExtra("filepath", filePath); // a path or URI must be provided.

        startService(service);
    }

    /**
     * Creates and starts service to flash a program to a micro:bit board.
     */
    protected void startFlashing() {
        final Intent intent = getIntent();
        final ExtendedBluetoothDevice device = intent.getParcelableExtra("cc.calliope.mini.EXTRA_DEVICE");

        if(device == null) {
            return;
        }

        Bundle extras = intent.getExtras();
        final String filePath = extras.getString("EXTRA_FILE");

        Log.i("DFUExtra", "mAddress: " + device.getAddress());
        Log.i("DFUExtra", "mPattern: " + device.getName());
        Log.i("DFUExtra", "filePath: " + filePath);
        Log.i("DFUExtra", "Start Flashing");

        final DfuServiceInitiator starter = new DfuServiceInitiator(device.getAddress())
                .setDeviceName(device.getName())
                //TODO Modify HexInputStream
                .setMbrSize(0x18000)
                //TODO  Android Oreo or newer may kill a background service few moments after user closes the application.
                // Consider enabling foreground service
                // Bud some time we have exception:
                // android.app.ForegroundServiceDidNotStartInTimeException: Context.startForegroundService() did not then call Service.startForeground()
//                .setForeground(false)
//                .setNumberOfRetries(3)
                .setRebootTime(1000)
                .setKeepBond(false);

        //TODO
        // Reboot after fw download

        starter.setBinOrHex(DfuBaseService.TYPE_APPLICATION, filePath);
        starter.start(this, DfuService.class);
    }
}