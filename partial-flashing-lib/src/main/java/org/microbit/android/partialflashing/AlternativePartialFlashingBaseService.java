package org.microbit.android.partialflashing;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED;
import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_BONDING;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.bluetooth.BluetoothDevice.ERROR;
import static android.bluetooth.BluetoothDevice.EXTRA_BOND_STATE;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

public abstract class AlternativePartialFlashingBaseService extends Service {
    private static final String TAG = "PartialFlashingService";
    private static final int DELAY_TO_CLEAR_CACHE = 2000;
    public static final String EXTRA_DEVICE_ADDRESS = "org.microbit.android.partialflashing.EXTRA_DEVICE_ADDRESS";
    public static final String BROADCAST_START = "org.microbit.android.partialflashing.broadcast.BROADCAST_START";
    public static final String BROADCAST_PROGRESS = "org.microbit.android.partialflashing.broadcast.BROADCAST_PROGRESS";
    public static final String BROADCAST_COMPLETE = "org.microbit.android.partialflashing.broadcast.BROADCAST_COMPLETE";
    public static final String BROADCAST_PF_FAILED = "org.microbit.android.partialflashing.broadcast.BROADCAST_PF_FAILED";
    public static final String BROADCAST_PF_ATTEMPT_DFU = "org.microbit.android.partialflashing.broadcast.BROADCAST_PF_ATTEMPT_DFU";
    public static final String EXTRA_PROGRESS = "org.microbit.android.partialflashing.extra.EXTRA_PROGRESS";
    private final Object mLock = new Object();
    private String deviceAddress;
    private int bondState;
    private Looper serviceLooper;
    private ServiceHandler serviceHandler;

    private static final String[] BLUETOOTH_PERMISSIONS;
    static {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            BLUETOOTH_PERMISSIONS = new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        } else {
            BLUETOOTH_PERMISSIONS = new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN};
        }
    }

    private final BroadcastReceiver bondStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null || !device.getAddress().equals(deviceAddress)) {
                return;
            }

            final String action = intent.getAction();
            if (action == null) {
                return;
            }

            // Take action depending on new bond state
            if (action.equals(ACTION_BOND_STATE_CHANGED)) {
                bondState = intent.getIntExtra(EXTRA_BOND_STATE, ERROR);
                switch (bondState) {
                    case BOND_BONDING -> {
                        log(Log.WARN, "Bonding started");
                        try {
                            synchronized (mLock) {
                                while (bondState == BOND_BONDING)
                                    mLock.wait();
                            }
                        } catch (final InterruptedException e) {
                            log(Log.ERROR, "Sleeping interrupted, " + e);
                        }
                    }
                    case BOND_BONDED, BOND_NONE -> {
                        log(Log.WARN, "Bonding " + (bondState == BOND_BONDED));
                        synchronized (mLock) {
                            mLock.notifyAll();
                        }
                    }
                }
            }
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            log(Log.ASSERT, "onConnectionStateChange(gatt: " + gatt + ", status: " + status + ", newState: " + newState + ")");
            if (status == GATT_SUCCESS) {
                if (newState == STATE_CONNECTED) {
                    if (bondState == BOND_NONE || bondState == BOND_BONDED) {
                        if (bondState == BOND_BONDED && Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                            waitFor(1600);
                        }
                        boolean result = gatt.discoverServices();
                        if (!result) {
                            log(Log.ERROR, "discoverServices failed to start");
                        }
                    } else if (bondState == BOND_BONDING) {
                        log(Log.WARN, "waiting for bonding to complete");
                    }
                } else if (newState == STATE_DISCONNECTED) {
                    clearServicesCache(gatt);
                    gatt.close();
                    stopSelf();
                }
            } else {
                gatt.disconnect();
                sendProgressBroadcastFailed();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            log(Log.ASSERT, "onServicesDiscovered(gatt: " + gatt + ", status: " + status + ")");
            if (status == GATT_SUCCESS) {
                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService service : services) {
                    log(Log.DEBUG, "service: " + service.getUuid());
                }
            } else {
                gatt.disconnect();
                sendProgressBroadcastFailed();
            }
        }

        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
            super.onCharacteristicRead(gatt, characteristic, value, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);
        }

        @Override
        public void onDescriptorRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattDescriptor descriptor, int status, @NonNull byte[] value) {
            super.onDescriptorRead(gatt, descriptor, status, value);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }

        @Override
        public void onServiceChanged(@NonNull BluetoothGatt gatt) {
            super.onServiceChanged(gatt);
        }
    };

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (isPermissionGranted(BLUETOOTH_PERMISSIONS)) {
                sendProgressBroadcastStart();
                connect();
            } else {
                log(Log.ERROR, "no Permission Granted");
                sendProgressBroadcastFailed();
                stopSelf(msg.arg1);
            }
        }
    }

    @Override
    public void onCreate() {
        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work doesn't disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log(Log.WARN, "service starting");

        registerReceiver(bondStateReceiver, new IntentFilter(ACTION_BOND_STATE_CHANGED));
        deviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        log(Log.WARN, "service done");

        unregisterReceiver(bondStateReceiver);
    }

    private boolean isPermissionGranted(String... permissions) {
        for (String permission : permissions) {
            boolean granted = ContextCompat.checkSelfPermission(getApplicationContext(), permission) == PackageManager.PERMISSION_GRANTED;
            if (!granted) {
                return false;
            }
        }
        return true;
    }

    private void connect() {
        log(Log.DEBUG, "Connecting to the device...");
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            return;
        }

        BluetoothDevice device = adapter.getRemoteDevice(deviceAddress);
        bondState = device.getBondState();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE, BluetoothDevice.PHY_LE_1M_MASK | BluetoothDevice.PHY_LE_2M_MASK);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
            device.connectGatt(this, false, gattCallback);
        }
    }

    protected void clearServicesCache(BluetoothGatt gatt) {
        try {
            //noinspection JavaReflectionMemberAccess
            final Method refresh = gatt.getClass().getMethod("refresh");
            //noinspection ConstantConditions
            final boolean success = (boolean) refresh.invoke(gatt);
            log(Log.DEBUG, "Refreshing result: " + success);
        } catch (final Exception e) {
            log(Log.ERROR, "An exception occurred while refreshing device. " + e);
        }
        waitFor(DELAY_TO_CLEAR_CACHE);
    }

    protected void waitFor(final long millis) {
        synchronized (mLock) {
            try {
                log(Log.DEBUG, "Wait for " + millis + " millis");
                mLock.wait(millis);
            } catch (final InterruptedException e) {
                log(Log.ERROR, "Sleeping interrupted, " + e);
            }
        }
    }

    private void sendProgressBroadcast(final int progress) {
        Log.v(TAG, "Sending progress broadcast: " + progress + "%");
        final Intent broadcast = new Intent(BROADCAST_PROGRESS);
        broadcast.putExtra(EXTRA_PROGRESS, progress);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    private void sendProgressBroadcastStart() {
        Log.v(TAG, "Sending progress broadcast start");
        final Intent broadcast = new Intent(BROADCAST_START);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    private void sendProgressBroadcastComplete() {
        Log.v(TAG, "Sending progress broadcast complete");
        final Intent broadcast = new Intent(BROADCAST_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    private void sendProgressBroadcastFailed() {
        Log.v(TAG, "Sending progress broadcast complete");
        final Intent broadcast = new Intent(BROADCAST_PF_FAILED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    public void log(int priority, @NonNull String message) {
        Log.println(priority, TAG, "### " + Thread.currentThread().getId() + " # " + message);
    }
}