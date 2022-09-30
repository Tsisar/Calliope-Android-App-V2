package cc.calliope.mini_v2;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.lang.reflect.Method;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import cc.calliope.mini_v2.adapter.ExtendedBluetoothDevice;
import cc.calliope.mini_v2.service.DfuService;
import no.nordicsemi.android.ble.PhyRequest;
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
    private static final String TAG_PL = "DfuProgressListener";

    public static final String BROADCAST_PROGRESS = "org.microbit.android.partialflashing.broadcast.BROADCAST_PROGRESS";
    public static final String EXTRA_PROGRESS = "org.microbit.android.partialflashing.extra.EXTRA_PROGRESS";

    private FlashingManager flashingManager;

    private final Handler timerHandler = new Handler();
    private final Runnable deferredFinish = this::finish;
    private static final int DELAY_TO_FINISH = 5000;

    private boolean onPause;


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
            Log.e(TAG_PL, method);
        }

        @Override
        public void onDfuProcessStarting(@NonNull final String deviceAddress) {
            timerText.setText("Process Starting");
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.e(TAG_PL, method);
        }

        @Override
        public void onEnablingDfuMode(@NonNull final String deviceAddress) {
            timerText.setText("Enabling Dfu Mode");
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.e(TAG_PL, method);
        }

        @Override
        public void onFirmwareValidating(@NonNull final String deviceAddress) {
            timerText.setText("Firmware Validating");
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.e(TAG_PL, method);
        }

        @Override
        public void onDeviceDisconnecting(@NonNull final String deviceAddress) {
            timerText.setText("Device Disconnecting");
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.e(TAG_PL, method);
        }

        @Override
        public void onDfuCompleted(@NonNull final String deviceAddress) {
            timerText.setText("Dfu Completed");
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.e(TAG_PL, method);
            timerHandler.postDelayed(deferredFinish, DELAY_TO_FINISH);
        }

        @Override
        public void onDfuAborted(@NonNull final String deviceAddress) {
            timerText.setText("Dfu Aborted");
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.e(TAG_PL, method);
            timerHandler.postDelayed(deferredFinish, DELAY_TO_FINISH);
        }

        @Override
        public void onProgressChanged(@NonNull final String deviceAddress, final int percent, final float speed, final float avgSpeed, final int currentPart, final int partsTotal) {
            if(!onPause) {
                deviceInfo.setText(percent + "%");
                timerText.setText("Uploading, avgSpeed: " + avgSpeed);
                progressBar.setProgress(39 + percent / 3);
            }

            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.e(TAG, method + " percent: " + percent);
        }

        @Override
        public void onError(@NonNull final String deviceAddress, final int error, final int errorType, final String message) {
            deviceInfo.setText("ERROR");
            timerText.setText(message);
            String method = Thread.currentThread().getStackTrace()[2].getMethodName();
            Log.e(TAG_PL, method + " error: " + message);
            timerHandler.postDelayed(deferredFinish, DELAY_TO_FINISH);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dfu);

        flashingManager = new FlashingManager(this);

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
        flashingManager.close();
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
        onPause = true;
        //DfuServiceListenerHelper.unregisterProgressListener(this, mDfuProgressListener);
    }

    protected void initiateFlashing() {
        final Intent intent = getIntent();
        final ExtendedBluetoothDevice device = intent.getParcelableExtra("cc.calliope.mini.EXTRA_DEVICE");

        if (device == null) {
            return;
        }

        // Check if the peripheral is cached or not
        int deviceType = device.getDevice().getType();
        if(deviceType == BluetoothDevice.DEVICE_TYPE_UNKNOWN) {
            Log.e("BLE", "The peripheral is not cached");
        } else {
            Log.v("BLE", "The peripheral is cached");
        }

        Log.v("BLE", "device: " + device.getAddress() + " " + device.getName());

        Log.w("BLE", "### " + Thread.currentThread().getId() + " # " + "initiateFlashing");

        flashingManager.connect(device.getDevice())
                // Automatic retries are supported, in case of 133 error.
                .retry(3 /* times, with */, 100 /* ms interval */)
                .timeout(10000 /* ms */)
                .usePreferredPhy(PhyRequest.PHY_LE_1M_MASK | PhyRequest.PHY_LE_2M_MASK)
                .done(this::writeCharacteristic)
                .fail(this::connectionFail)
                .enqueue();
    }

    private void writeCharacteristic(BluetoothDevice device){
        Log.w("BLE", "### " + Thread.currentThread().getId() + " # " + "writeCharacteristic");
        flashingManager.writeCharacteristic()
                .done(device1 -> Log.w("BLE", "### " + Thread.currentThread().getId() + " # " + "onRequestCompleted: done"))
                .fail((device2, s) -> Log.w("BLE", "### " + Thread.currentThread().getId() + " # " + "onRequestCompleted: fail, status " + s))
                .then(this::disconnect)
                .enqueue();
    }

    private void disconnect(BluetoothDevice device) {
        Log.w("BLE", "### " + Thread.currentThread().getId() + " # " + "disconnect");
        flashingManager.disconnect()
                .done(device1 -> Log.w("BLE", "### " + Thread.currentThread().getId() + " # " + "onRequestCompleted: done"))
                .fail((device2, s) -> Log.w("BLE", "### " + Thread.currentThread().getId() + " # " + "onRequestCompleted: fail, status " + s))
                .then(this::startFlashing)
                .enqueue();

        //refreshDeviceCache();
    }

    private void connectionFail(BluetoothDevice device, int status) {
        Log.e("BLE", "Connection error to device " + device.getAddress() + ", status: " + status);
        timerText.setText("Connection error to device " + device.getAddress() + ", status: " + status);
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
     * @param device BluetoothDevice
     */
    protected void startFlashing(BluetoothDevice device) {
        Log.w("BLE", "### " + Thread.currentThread().getId() + " # " + "startFlashing");
        Bundle extras = getIntent().getExtras();
        final String filePath = extras.getString("EXTRA_FILE");

        Log.i("DFUExtra", "mAddress: " + device.getAddress());
//        Log.i("DFUExtra", "mPattern: " + device.getName());
        Log.i("DFUExtra", "filePath: " + filePath);
        Log.i("DFUExtra", "Start Flashing");

        final DfuServiceInitiator starter = new DfuServiceInitiator(device.getAddress())
//                .setDeviceName(device.getName())
                //TODO Modify HexInputStream
                .setMbrSize(0x18000)
//                .setForeground(false)
                .setNumberOfRetries(3)
                .setRebootTime(1000)
                .setKeepBond(false);

        starter.setBinOrHex(DfuBaseService.TYPE_APPLICATION, filePath);
        starter.start(this, DfuService.class);
    }

    protected void refreshDeviceCache() {
        BluetoothGatt gatt = flashingManager.getGatt();
        if(gatt != null) {
            try {
                //noinspection JavaReflectionMemberAccess
                final Method refresh = gatt.getClass().getMethod("refresh");
                if (refresh != null) {
                    refresh.invoke(gatt);
                    Log.v("BLE", "Refresh device cache");
                }
            } catch (final Exception e) {
                Log.e("BLE", "An exception occurred while refreshing device" + e);
            }
        }
    }
}