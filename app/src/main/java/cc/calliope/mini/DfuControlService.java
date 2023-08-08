package cc.calliope.mini;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.os.IBinder;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.UUID;

import androidx.annotation.IntDef;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import cc.calliope.mini.utils.Utils;
import cc.calliope.mini.utils.Version;

import static android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED;
import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_BONDING;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.bluetooth.BluetoothDevice.ERROR;
import static android.bluetooth.BluetoothDevice.EXTRA_BOND_STATE;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;


public class DfuControlService extends Service {
    private static final String TAG = "DfuControlService";
    private static final UUID DEVICE_FIRMWARE_UPDATE_CONTROL_SERVICE_UUID = UUID.fromString("E95D93B0-251D-470A-A062-FA1922DFA9A8");
    private static final UUID DEVICE_FIRMWARE_UPDATE_CONTROL_CHARACTERISTIC_UUID = UUID.fromString("E95D93B1-251D-470A-A062-FA1922DFA9A8");
    private static final UUID SECURE_DEVICE_FIRMWARE_UPDATE_SERVICE_UUID = UUID.fromString("0000FE59-0000-1000-8000-00805F9B34FB");
    private static final int DELAY_TO_CLEAR_CACHE = 2000;
    public static final String BROADCAST_START = "cc.calliope.mini.DFUControlService.BROADCAST_START";
    public static final String BROADCAST_COMPLETED = "cc.calliope.mini.DFUControlService.BROADCAST_COMPLETE";
    public static final String BROADCAST_FAILED = "cc.calliope.mini.DFUControlService.BROADCAST_FAILED";
    public static final String BROADCAST_BONDING = "cc.calliope.mini.DFUControlService.BROADCAST_BONDING";
    public static final String BROADCAST_ERROR = "cc.calliope.mini.DFUControlService.BROADCAST_ERROR";
    public static final String EXTRA_BOARD_VERSION = "cc.calliope.mini.DFUControlService.EXTRA_BOARD_VERSION";
    public static final String EXTRA_ERROR = "cc.calliope.mini.DFUControlService.EXTRA_ERROR";
    public static final String EXTRA_DEVICE_ADDRESS = "cc.calliope.mini.DFUControlService.EXTRA_DEVICE_ADDRESS";
    public static final String EXTRA_MAX_RETRIES_NUMBER = "cc.calliope.mini.DFUControlService.EXTRA_MAX_RETRIES_NUMBER";
    public static final String EXTRA_PREVIOUS_BOND_STATE = "cc.calliope.mini.DFUControlService.EXTRA_PREVIOUS_BOND_STATE";
    public static final String EXTRA_BOND_STATE = "cc.calliope.mini.DFUControlService.EXTRA_BOND_STATE";

    private final Object mLock = new Object();
    private int maxRetries;
    private int numOfRetries = 0;
    private boolean isComplete = false;
    private int bondState;
    private String deviceAddress;
    public static final int BOARD_UNIDENTIFIED = 0;
    public static final int BOARD_V1 = 1;
    public static final int BOARD_V2 = 2;
    @IntDef({BOARD_UNIDENTIFIED, BOARD_V1, BOARD_V2})
    @Retention(RetentionPolicy.SOURCE)
    public @interface HardwareType {
    }
    private int boardVersion = BOARD_UNIDENTIFIED;
    private final BroadcastReceiver bondStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null || !device.getAddress().equals(deviceAddress))
                return;

            final String action = intent.getAction();
            // Check if action is valid
            if (action == null) return;

            // Take action depending on new bond state
            if (action.equals(ACTION_BOND_STATE_CHANGED)) {
                final int newBondState = intent.getIntExtra(EXTRA_BOND_STATE, ERROR);
                final int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, ERROR);
                Utils.log(Log.DEBUG, TAG, "previousBondState: " + previousBondState);

                final Intent broadcast = new Intent(BROADCAST_BONDING);
                broadcast.putExtra(EXTRA_PREVIOUS_BOND_STATE, newBondState);
                broadcast.putExtra(EXTRA_PREVIOUS_BOND_STATE, previousBondState);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcast);

                bondState = newBondState;

                switch (newBondState) {
                    case BOND_BONDING -> Utils.log(Log.WARN, TAG, "Bonding started");
                    case BOND_BONDED -> {
                        Utils.log(Log.WARN, TAG, "Bonding succeeded");
                        synchronized (mLock) {
                            mLock.notifyAll();
                        }
                    }
                    case BOND_NONE -> {
                        Utils.log(Log.WARN, TAG, "Oh oh");
                        synchronized (mLock) {
                            mLock.notifyAll();
                        }
                    }
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Utils.log(Log.DEBUG, TAG, "Сервіс запущений.");
        registerReceiver(bondStateReceiver, new IntentFilter(ACTION_BOND_STATE_CHANGED));

        deviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        maxRetries = intent.getIntExtra(EXTRA_MAX_RETRIES_NUMBER, 2);

        connect();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bondStateReceiver);

        final Intent broadcast = new Intent(isComplete ? BROADCAST_COMPLETED : BROADCAST_FAILED);
        broadcast.putExtra(EXTRA_BOARD_VERSION, boardVersion);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcast);

        Utils.log(Log.DEBUG, TAG, "Сервіс зупинений.");
    }

    @SuppressWarnings({"MissingPermission"})
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            String methodName = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Utils.log(Log.ASSERT, TAG, methodName + "(gatt: " + gatt + ", status: " + status + ", newState: " + newState + ")");

            if (status == GATT_SUCCESS) {
                if (newState == STATE_CONNECTED) {
//                    if (bondState == BOND_NONE && Version.upperTiramisu) {
//                        bondState = BOND_BONDING;
//                        device.createBond();
//                    }

                    if (bondState == BOND_NONE || bondState == BOND_BONDED) {
                        if (bondState == BOND_BONDED && Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                            waitFor(1600);
                        }
                        boolean result = gatt.discoverServices();
                        if (!result) {
                            Utils.log(Log.ERROR, TAG, "discoverServices failed to start");
                        }
                    } else if (bondState == BOND_BONDING) {
                        Utils.log(Log.WARN, TAG, "waiting for bonding to complete");
                    }
                } else if (newState == STATE_DISCONNECTED) {
                    stopService(gatt);
                }
            } else {
                stopService(gatt);
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            String methodName = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Utils.log(Log.ASSERT, TAG, methodName);

            if (status == GATT_SUCCESS) {
                startLegacyDfu(gatt);
            } else {
                setError(gatt, "Services discovered not success");
            }
        }

        // Other methods just pass the parameters through
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            String methodName = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Utils.log(Log.ASSERT, TAG, methodName + " status: " + status);

            if (bondState == BOND_NONE || bondState == BOND_BONDED) {
                if (status == GATT_SUCCESS) {
                    isComplete = true;
                    boardVersion = BOARD_V1;
                } else {
                    setError(gatt, "Characteristic write not success");
                }
            } else if (bondState == BOND_BONDING) {
                Utils.log(Log.WARN, TAG, "waiting for bonding to complete");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            String methodName = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Utils.log(Log.ASSERT, TAG, methodName + "(gatt: " + gatt + ", characteristic: " + characteristic + ", status: " + status + ")");

            if (status == GATT_SUCCESS) {
                characteristic.setValue(1, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                try {
                    Utils.log(Log.DEBUG, TAG, "Writing Flash Command...");
                    gatt.writeCharacteristic(characteristic);
                } catch (Exception e) {
                    e.printStackTrace();
                    setError(gatt, e.toString());
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            String methodName = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Utils.log(Log.ASSERT, TAG, methodName);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            String methodName = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Utils.log(Log.ASSERT, TAG, methodName);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            String methodName = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Utils.log(Log.ASSERT, TAG, methodName);
        }

        @SuppressLint("NewApi")
        @Override
        public void onMtuChanged(final BluetoothGatt gatt, final int mtu, final int status) {
            String methodName = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Utils.log(Log.ASSERT, TAG, methodName);
        }

        @SuppressLint("NewApi")
        @Override
        public void onPhyUpdate(final BluetoothGatt gatt, final int txPhy, final int rxPhy, final int status) {
            String methodName = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Utils.log(Log.ASSERT, TAG, methodName);
        }
    };

    private void connect() {
        if ((Version.upperSnowCone && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            Utils.log(Log.ERROR, TAG, "BLUETOOTH permission no granted");
            return;
        }
        Utils.log(Log.DEBUG, TAG, "Connecting to the device...");

        final Intent broadcast = new Intent(BROADCAST_START);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcast);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = adapter.getRemoteDevice(deviceAddress);
        bondState = device.getBondState();

        if (Version.upperOreo) {
            Utils.log(Log.DEBUG, TAG, "gatt = device.connectGatt(autoConnect = false, TRANSPORT_LE, preferredPhy = LE_1M | LE_2M)");
            device.connectGatt(this, false, gattCallback,
                    BluetoothDevice.TRANSPORT_LE,
                    BluetoothDevice.PHY_LE_1M_MASK | BluetoothDevice.PHY_LE_2M_MASK);
        } else if (Version.upperMarshmallow) {
            Utils.log(Log.DEBUG, TAG, "gatt = device.connectGatt(autoConnect = false, TRANSPORT_LE)");
            device.connectGatt(this, false, gattCallback,
                    BluetoothDevice.TRANSPORT_LE);
        } else {
            Utils.log(Log.DEBUG, TAG, "gatt = device.connectGatt(autoConnect = false)");
            device.connectGatt(this, false, gattCallback);
        }

    }

    @SuppressWarnings({"MissingPermission"})
    private void stopService(BluetoothGatt gatt) {
        if (bondState == BOND_BONDING) {
            waitUntilBonded();
        }
        clearServicesCache(gatt);
        gatt.close();
        stopSelf();
    }

    @SuppressWarnings({"MissingPermission"})
    private void startLegacyDfu(BluetoothGatt gatt) {
        BluetoothGattService legacyDfuService = gatt.getService(DEVICE_FIRMWARE_UPDATE_CONTROL_SERVICE_UUID);
        if (legacyDfuService == null) {
            startSecureDfu(gatt);
            return;
        }

        final BluetoothGattCharacteristic legacyDfuCharacteristic = legacyDfuService.getCharacteristic(DEVICE_FIRMWARE_UPDATE_CONTROL_CHARACTERISTIC_UUID);
        if (legacyDfuCharacteristic == null) {
            setError(gatt, "Cannot find DEVICE_FIRMWARE_UPDATE_CONTROL_CHARACTERISTIC_UUID");
            return;
        }

        boolean res = gatt.readCharacteristic(legacyDfuCharacteristic);
        Utils.log(Log.WARN, TAG, "readCharacteristic: " + res);
    }

    @SuppressWarnings({"MissingPermission"})
    private void startSecureDfu(BluetoothGatt gatt) {
        BluetoothGattService secureDfuService = gatt.getService(SECURE_DEVICE_FIRMWARE_UPDATE_SERVICE_UUID);
        if (secureDfuService == null) {
            if (numOfRetries < maxRetries) {
                Utils.log(Log.WARN, TAG, "Retrying to discover services...");
                numOfRetries++;
                clearServicesCache(gatt);
                gatt.discoverServices();
            } else {
                setError(gatt, "Cannot find services");
            }
        } else {
            isComplete = true;
            boardVersion = BOARD_V2;
            gatt.disconnect();
        }
    }

    protected void clearServicesCache(BluetoothGatt gatt) {
        try {
            //noinspection JavaReflectionMemberAccess
            final Method refresh = gatt.getClass().getMethod("refresh");
            //noinspection ConstantConditions
            final boolean success = (boolean) refresh.invoke(gatt);
            Utils.log(Log.DEBUG, TAG, "Refreshing result: " + success);
        } catch (final Exception e) {
            Utils.log(Log.ERROR, TAG, "An exception occurred while refreshing device. " + e);
        }
        waitFor(DELAY_TO_CLEAR_CACHE);
    }

    protected void waitUntilBonded() {
        try {
            synchronized (mLock) {
                while (bondState == BOND_BONDING)
                    mLock.wait();
            }
        } catch (final InterruptedException e) {
            Utils.log(Log.ERROR, TAG, "Sleeping interrupted, " + e);
        }
    }

    protected void waitFor(final long millis) {
        synchronized (mLock) {
            try {
                Utils.log(Log.DEBUG, TAG, "Wait for " + millis + " millis");
                mLock.wait(millis);
            } catch (final InterruptedException e) {
                Utils.log(Log.ERROR, TAG, "Sleeping interrupted, " + e);
            }
        }
    }

    @SuppressWarnings({"MissingPermission"})
    private void setError(BluetoothGatt gatt, final String message) {
        gatt.disconnect();
        final Intent broadcast = new Intent(BROADCAST_ERROR);
        broadcast.putExtra(EXTRA_ERROR, message);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcast);
        Utils.log(Log.ERROR, TAG, message);
    }
}