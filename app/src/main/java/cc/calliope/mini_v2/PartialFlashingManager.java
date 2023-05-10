package cc.calliope.mini_v2;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import org.microbit.android.partialflashing.HexUtils;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.UUID;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.callback.DataReceivedCallback;
import no.nordicsemi.android.ble.data.Data;

public class PartialFlashingManager extends BleManager {
    private static final String TAG = "PartialFlashingManager";
    public static final UUID PARTIAL_FLASHING_SERVICE = UUID.fromString("E97DD91D-251D-470A-A062-FA1922DFA9A8");
    public static final UUID PARTIAL_FLASHING_CHARACTERISTIC = UUID.fromString("E97D3B10-251D-470A-A062-FA1922DFA9A8");
    private static final UUID DFU_V1_CONTROL_SERVICE = UUID.fromString("E95D93B0-251D-470A-A062-FA1922DFA9A8");
    private static final UUID DFU_V1_CONTROL_CHARACTERISTIC = UUID.fromString("E95D93B1-251D-470A-A062-FA1922DFA9A8");
    private static final UUID DFU_V2_CONTROL_SERVICE = UUID.fromString("0000FE59-0000-1000-8000-00805F9B34FB");
    private static final UUID DFU_V2_CONTROL_CHARACTERISTIC = UUID.fromString("8EC90004-F315-4F60-9FB8-838830DAEA50");
    private static final String PXT_MAGIC = "708E3B92C615A841C49866C975EE5197";
    private static final String UPY_MAGIC = ".*FE307F59.{16}9DD7B1C1.*";
    private static final byte MODE_TYPE_PAIRING = 0x00;
    private static final byte MODE_TYPE_APPLICATION = 0x01;

    @IntDef({MODE_TYPE_PAIRING, MODE_TYPE_APPLICATION})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ResetType {
    }

    private BluetoothGattCharacteristic partialFlashingServiceCharacteristic;
    private BluetoothGattCharacteristic dfuV1ControlServiceCharacteristic;
    private BluetoothGattCharacteristic dfuV2ControlServiceCharacteristic;
    private OnDisconnectListener onDisconnectListener;
    private static String dalHash;
    private final String filePath;
    private boolean isPartialFlashAvailable = false;

    public interface OnDisconnectListener {
        void onDisconnect();

        void onStartDFUService();

        void onStartPartialFlashingService();
    }

    public PartialFlashingManager(@NonNull final Context context, String filePath) {
        super(context);
        if (context instanceof OnDisconnectListener) {
            this.onDisconnectListener = (OnDisconnectListener) context;
        }
        this.filePath = filePath;
    }

    public void setOnDisconnectListener(OnDisconnectListener onDisconnectListener) {
        this.onDisconnectListener = onDisconnectListener;
    }

    @Override
    public int getMinLogPriority() {
        // Use to return minimal desired logging priority.
        return Log.VERBOSE;
    }

    @Override
    public void log(int priority, @NonNull String message) {
        // Log from here.
        Log.println(priority, TAG, "### " + Thread.currentThread().getId() + " # " + message);
    }

    @NonNull
    @Override
    public BleManagerGattCallback getGattCallback() {
        return new MyGattCallbackImpl();
    }

    private class MyGattCallbackImpl extends BleManagerGattCallback {
        @Override
        protected boolean isRequiredServiceSupported(@NonNull BluetoothGatt gatt) {
            log(Log.INFO, "Checking if the flashing services is supported...");
            dalHash = null;
            // Here get instances of your characteristics.
            // Return false if a required service has not been discovered.
            BluetoothGattService partialFlashingService = gatt.getService(PARTIAL_FLASHING_SERVICE);
            if (partialFlashingService != null) {
                partialFlashingServiceCharacteristic = partialFlashingService.getCharacteristic(PARTIAL_FLASHING_CHARACTERISTIC);
            } else {
                log(Log.WARN, "Partial Flashing service isn't supported");
            }

            BluetoothGattService dfuV1ControlService = gatt.getService(DFU_V1_CONTROL_SERVICE);
            if (dfuV1ControlService != null) {
                dfuV1ControlServiceCharacteristic = dfuV1ControlService.getCharacteristic(DFU_V1_CONTROL_CHARACTERISTIC);
            } else {
                log(Log.WARN, "DFU Control service isn't supported");
            }

            BluetoothGattService dfuV2ControlService = gatt.getService(DFU_V2_CONTROL_SERVICE);
            if (dfuV2ControlService != null) {
                dfuV2ControlServiceCharacteristic = dfuV2ControlService.getCharacteristic(DFU_V2_CONTROL_CHARACTERISTIC);
            } else {
                log(Log.WARN, "Secure DFU Control service isn't supported");
            }

            return partialFlashingService != null || dfuV1ControlService != null || dfuV2ControlService != null;
        }

        @Override
        protected void initialize() {
            log(Log.INFO, "Initialize...");

            if (partialFlashingServiceCharacteristic != null) {
                enableNotifications(partialFlashingServiceCharacteristic).enqueue();
                setNotificationCallback(partialFlashingServiceCharacteristic).with(new DataReceivedCallback() {
                    private static final byte REGION_INFO_COMMAND = (byte) 0x00;
                    private static final byte STATUS = (byte) 0xEE;
                    private static final int REGION_DAL = 1;

                    @Override
                    public void onDataReceived(@NonNull BluetoothDevice device, @NonNull Data data) {
                        byte[] notificationValue = data.getValue();
                        if (notificationValue == null) {
                            return;
                        }

                        Log.v(TAG, "Received Notification: " + bytesToHex(notificationValue));

                        // Get Hash
                        switch (notificationValue[0]) {
                            case STATUS -> {
                                Log.w(TAG, "STATUS: " + (notificationValue[2] == MODE_TYPE_APPLICATION ? "Application Mode" : "Pairing Mode"));
                                if (notificationValue[2] == MODE_TYPE_APPLICATION) {
                                    reset(MODE_TYPE_PAIRING);
                                } else {
                                    disconnect().enqueue();
                                }
                            }
                            case REGION_INFO_COMMAND -> {
                                log(Log.VERBOSE, "Region: " + notificationValue[1]);
                                byte[] hash = Arrays.copyOfRange(notificationValue, 10, 18);
                                if (notificationValue[1] == REGION_DAL) {
                                    dalHash = bytesToHex(hash);
                                    isPartialFlashAvailable = isPartialFlashAvailable(filePath);

                                    log(Log.VERBOSE, "Hash: " + dalHash);
                                    log(Log.WARN, "Is Partial Flash available: " + isPartialFlashAvailable);

                                    if (isPartialFlashAvailable) {
                                        sendStatusRequest();
                                    } else {
                                        enterDFUBootloader();
                                    }
                                }
                            }
                            default ->
                                    throw new IllegalStateException("Unexpected value: " + notificationValue[0]);
                        }
                    }

                });
                sendHashRequest();
            } else {
                enterDFUBootloader();
            }
        }

        @Override
        protected void onServicesInvalidated() {
            log(Log.WARN, "Services invalidated...");
            // This method is called when the services get invalidated, i.e. when the device disconnects.
            // References to characteristics should be nullified here.

            partialFlashingServiceCharacteristic = null;
            dfuV1ControlServiceCharacteristic = null;
            dfuV2ControlServiceCharacteristic = null;

            if (onDisconnectListener != null) {
                onDisconnectListener.onDisconnect();
                if (isPartialFlashAvailable) {
                    onDisconnectListener.onStartPartialFlashingService();
                } else {
                    onDisconnectListener.onStartDFUService();
                }
            }
        }
    }

    public void sendStatusRequest() {
        // Get hash when connecting
        writeCharacteristicNoResponse(partialFlashingServiceCharacteristic, (byte) 0xEE);
    }

    public void sendHashRequest() {
        // Get hash when connecting
        writeCharacteristicNoResponse(partialFlashingServiceCharacteristic, (byte) 0x00, (byte) 0x01);
    }

    public void reset(@ResetType byte mode) {
        //Reset Type: 0x00 for Pairing Mode, 0x01 for Application Mode
        writeCharacteristicNoResponse(partialFlashingServiceCharacteristic, (byte) 0xFF, mode);
    }

    public void enterDFUBootloader() {
        //Writing 0x01 initiates rebooting the Mini into the Nordic Semiconductor bootloader
        if (dfuV2ControlServiceCharacteristic != null) {
            writeCharacteristicNoResponse(dfuV2ControlServiceCharacteristic, (byte) 0x01);
        } else if (dfuV1ControlServiceCharacteristic != null) {
            writeCharacteristicNoResponse(dfuV1ControlServiceCharacteristic, (byte) 0x01);
        }

    }

    private void writeCharacteristicNoResponse(BluetoothGattCharacteristic characteristic, byte... bytes) {
        writeCharacteristic(characteristic, bytes, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
                .done(this::done)
                .fail(this::fail)
                .enqueue();
    }

    private void done(BluetoothDevice device) {
        log(Log.DEBUG, "Device: " + device.getAddress() + ". Done");
    }

    private void fail(BluetoothDevice device, int status) {
        log(Log.ERROR, "Device: " + device.getAddress() + ". Fail, status " + status);
    }

    private static String bytesToHex(byte[] bytes) {
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private boolean isPartialFlashAvailable(String filePath) {
        if (dalHash == null) {
            return false;
        }

        try {
            Log.v(TAG, "attemptPartialFlash()");
            Log.v(TAG, filePath);

            HexUtils hex = new HexUtils(filePath);

            Log.v(TAG, "searchForData()");

            // шукаємо індекс магічного magic number після йкого в нас записаний хеш
            int magicIndex = hex.searchForData(PXT_MAGIC);
            Log.v(TAG, "magicIndex: " + magicIndex);

            // Якщо не знайдено пробуємо шукати якийсь інший (не зрозуміло чого так)
            if (magicIndex == -1) {
                magicIndex = hex.searchForDataRegEx(UPY_MAGIC) - 3;
                Log.v(TAG, "magicIndex: " + magicIndex);
            }

            // тут якщо ми щось таки знайшли працюємо далі, а якщо ні, то ніякого часткого перепрошиття не може бути
            Log.v(TAG, "/searchForData() = " + magicIndex);

            if (magicIndex > -1) {

                Log.v(TAG, "Found magic");

                // дивимося довжину рядка за цим індексом, певне то через можливість використання різних типів файлу,
                // знову ж таки чому не можна було зробити це раніше, коли ми шукали магічне число

                int record_length = hex.getRecordDataLengthFromIndex(magicIndex);
                Log.v(TAG, "Length of record: " + record_length);

                //вістановлюємо відступ, до розміщення хешу, чому в іншому випадку 0? UPY_MAGIC це частина хешу?
                int magic_offset = (record_length == 64) ? 32 : 0;

                // тут вже ми отримуємо сам хеш
                String hashes = hex.getDataFromIndex(magicIndex + ((record_length == 64) ? 0 : 1)); // Size of rows

                Log.v(TAG, "Hashes: " + hashes);

                //Обрізаємо рядок з хешем і порівнюємо з отриманим з борда
                if (!hashes.substring(magic_offset, magic_offset + 16).equals(dalHash)) {
                    Log.v(TAG, "No match: " + hashes.substring(magic_offset, magic_offset + 16) + " " + (dalHash));
                    return false;
                }
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}