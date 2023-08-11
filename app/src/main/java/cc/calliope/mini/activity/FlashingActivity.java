package cc.calliope.mini.activity;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import cc.calliope.mini.R;
import cc.calliope.mini.DfuControlServiceManager;
import cc.calliope.mini.FlashingManager;
import cc.calliope.mini.InfoManager;
import cc.calliope.mini.PartialFlashingService;
import cc.calliope.mini.ProgressListener;
import cc.calliope.mini.ProgressReceiver;
import cc.calliope.mini.ExtendedBluetoothDevice;
import cc.calliope.mini.databinding.ActivityDfuBinding;
import cc.calliope.mini.service.DfuService;
import cc.calliope.mini.utils.FileUtils;
import cc.calliope.mini.utils.StaticExtra;
import cc.calliope.mini.utils.Utils;
import cc.calliope.mini.views.BoardProgressBar;
import no.nordicsemi.android.ble.PhyRequest;
import no.nordicsemi.android.dfu.DfuBaseService;
import no.nordicsemi.android.dfu.DfuServiceInitiator;

import static cc.calliope.mini.InfoManager.BOARD_UNIDENTIFIED;
import static cc.calliope.mini.InfoManager.BOARD_V1;
import static cc.calliope.mini.InfoManager.BOARD_V2;
import static cc.calliope.mini.InfoManager.HardwareType;
import static cc.calliope.mini.InfoManager.TYPE_CONTROL;

public class FlashingActivity extends AppCompatActivity implements ProgressListener {
    private static final String TAG = "FlashingActivity";
    private static final int NUMBER_OF_RETRIES = 3;
    private static final int INTERVAL_OF_RETRIES = 500; // ms
    private static final int REBOOT_TIME = 2000; // time required by the device to reboot, ms
    private static final long CONNECTION_TIMEOUT = 10000; // default connection timeout is 30000 ms
    private static final int DELAY_TO_FINISH_ACTIVITY = 10000; // delay to finish activity after flashing
    private ActivityDfuBinding binding;
    private TextView progress;
    private TextView status;
    private BoardProgressBar progressBar;
    private final Handler timerHandler = new Handler();
    private final Runnable deferredFinish = this::finish;
    private ProgressReceiver progressReceiver;
    private int mHardwareType = BOARD_UNIDENTIFIED;
    private int mFlashingType = TYPE_CONTROL;
    private String pattern;
    private String fPath;
    private BluetoothDevice currentDevice;

    public void log(int priority, @NonNull String message) {
        // Log from here.
        Log.println(priority, TAG, "### " + Thread.currentThread().getId() + " # " + message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDfuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        status = binding.statusTextView;
        progress = binding.progressTextView;
        progressBar = binding.progressBar;

        progressReceiver = new ProgressReceiver(this);
        progressReceiver.setProgressListener(this);

        initFlashing();
    }

    @Override
    protected void onDestroy() {
        binding = null;
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        progressReceiver.registerReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        progressReceiver.unregisterReceiver();
    }

    @Override
    public void onDeviceConnecting() {
        status.setText(R.string.flashing_device_connecting);
        Utils.log(Log.WARN, TAG, "onDeviceConnecting");
    }

    @Override
    public void onProcessStarting() {
        status.setText(R.string.flashing_process_starting);
        Utils.log(Log.WARN, TAG, "onProcessStarting");
    }

    @Override
    public void onEnablingDfuMode() {
        status.setText(R.string.flashing_enabling_dfu_mode);
        Utils.log(Log.WARN, TAG, "onEnablingDfuMode");
    }

    @Override
    public void onFirmwareValidating() {
        status.setText(R.string.flashing_firmware_validating);
        Utils.log(Log.WARN, TAG, "onFirmwareValidating");
    }

    @Override
    public void onDeviceDisconnecting() {
        status.setText(R.string.flashing_device_disconnecting);
        timerHandler.postDelayed(deferredFinish, DELAY_TO_FINISH_ACTIVITY);
        Utils.log(Log.WARN, TAG, "onDeviceDisconnecting");
    }

    @Override
    public void onCompleted() {
        progress.setText(String.format(getString(R.string.flashing_percent), 100));
        status.setText(R.string.flashing_completed);
        progressBar.setProgress(DfuService.PROGRESS_COMPLETED);
        Utils.log(Log.WARN, TAG, "onCompleted");
    }

    @Override
    public void onAborted() {
        status.setText(R.string.flashing_aborted);
        Utils.log(Log.WARN, TAG, "onAborted");
    }

    @Override
    public void onProgressChanged(int percent) {
        if (percent >= 0 && percent <= 100) {
            progress.setText(String.format(getString(R.string.flashing_percent), percent));
            status.setText(R.string.flashing_uploading);
            progressBar.setProgress(percent);
        }
    }

    @Override
    public void onBonding(@NonNull BluetoothDevice device, int bondState, int previousBondState) {

    }

    @Override
    public void onStartDfuService(int hardwareVersion){

    }

    @Override
    public void onError(int code, String message) {
        if (code == 4110) {
            pairDevice(currentDevice);
        }
        progress.setText(String.format(getString(R.string.flashing_error), code));
        status.setText(message);
        Utils.log(Log.ERROR, TAG, "ERROR " + code + ", " + message);
    }

    private void initFlashing() {
        ExtendedBluetoothDevice extendedDevice;

        try {
            Intent intent = getIntent();
            extendedDevice = intent.getParcelableExtra(StaticExtra.EXTRA_DEVICE);
            fPath = intent.getStringExtra(StaticExtra.EXTRA_FILE_PATH);
        } catch (NullPointerException exception) {
            log(Log.ERROR, "NullPointerException: " + exception.getMessage());
            return;
        }

        if (extendedDevice == null || fPath == null) {
            Utils.log(Log.ERROR, TAG, "No Extra received");
            return;
        }

        currentDevice = extendedDevice.getDevice();

        log(Log.INFO, "device: " + extendedDevice.getAddress() + " " + extendedDevice.getName());
        log(Log.INFO, "filePath: " + fPath);

        pattern = extendedDevice.getPattern();
        mHardwareType = BOARD_V1;
        startFlashing(currentDevice);
        //readInfo(currentDevice);
    }

    private void readInfo(BluetoothDevice device) {
        InfoManager infoManager = new InfoManager(this);
        infoManager.setOnInfoListener(this::setHardwareType);
        infoManager.connect(device)
                .retry(NUMBER_OF_RETRIES, INTERVAL_OF_RETRIES)
                .timeout(CONNECTION_TIMEOUT)
                .usePreferredPhy(PhyRequest.PHY_LE_1M_MASK | PhyRequest.PHY_LE_2M_MASK)
                .done(this::startFlashing)
                .fail(this::connectionFail)
                .enqueue();
    }


    private void setHardwareType(int hardwareType, int flashingType) {
        mHardwareType = hardwareType;
        mFlashingType = flashingType;
        Utils.log(Log.ASSERT, "DeviceInformation", "Hardware type: " + hardwareType + "; Flashing type: " + flashingType);
    }


    private void startFlashing(BluetoothDevice device) {
        Utils.log(Log.WARN, "DeviceInformation", "DONE");
        if (mHardwareType == BOARD_V1) {
            new DfuControlServiceManager(this)
                    .connect(device)
                    .retry(NUMBER_OF_RETRIES, INTERVAL_OF_RETRIES)
                    .timeout(CONNECTION_TIMEOUT)
                    .usePreferredPhy(PhyRequest.PHY_LE_1M_MASK | PhyRequest.PHY_LE_2M_MASK)
                    .done(this::startDfuService)
                    .fail(this::connectionFail)
                    .enqueue();
        } else {
            startDfuService(device);
        }
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
//                startDfuService(extendedDevice, filePath);
            }

            @Override
            public void onStartPartialFlashingService() {
//                startPartialFlashingService(extendedDevice, filePath);
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
    private void startDfuService(BluetoothDevice device) {
        log(Log.ASSERT, "Starting DFU Service...");
        log(Log.ASSERT, "hardwareType: " + mHardwareType);

        if (mHardwareType == BOARD_UNIDENTIFIED) {
            Utils.log(Log.ERROR, TAG, "BOARD_UNIDENTIFIED");
            return;
        }

        HexToDFU hexToDFU = universalHexToDFU(fPath, mHardwareType);
        String hexPath = hexToDFU.getPath();
        int hexSize = hexToDFU.getSize();

        log(Log.INFO, "Path: " + hexPath);
        log(Log.INFO, "Size: " + hexSize);

        if (hexSize == -1) {
            return;
        }

        if (mHardwareType == BOARD_V1) {
            new DfuServiceInitiator(device.getAddress())
                    .setDeviceName(pattern)
                    .setPrepareDataObjectDelay(300L)
                    .setNumberOfRetries(NUMBER_OF_RETRIES)
                    .setRebootTime(REBOOT_TIME)
                    .setForceDfu(true)
                    .setKeepBond(true)
                    .setMbrSize(0x1000)
                    .setBinOrHex(DfuBaseService.TYPE_APPLICATION, hexPath)
                    .start(this, DfuService.class);
        } else {
            String initPacketPath;
            String zipPath;

            try {
                initPacketPath = createDFUInitPacket(hexSize);
                zipPath = createDFUZip(initPacketPath, hexPath);
            } catch (IOException e) {
                Utils.log(Log.ERROR, TAG, "Failed to create init packet");
                e.printStackTrace();
                return;
            }

            if (zipPath == null) {
                Utils.log(Log.ERROR, TAG, "Failed to create ZIP");
                return;
            }

            new DfuServiceInitiator(device.getAddress())
                    .setDeviceName(pattern)
                    .setPrepareDataObjectDelay(300L)
                    .setNumberOfRetries(NUMBER_OF_RETRIES)
                    .setRebootTime(REBOOT_TIME)
                    .setKeepBond(true)
                    .setPacketsReceiptNotificationsEnabled(true)
                    .setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
                    .setZip(zipPath)
                    .start(this, DfuService.class);
        }
    }

    private void connectionFail(BluetoothDevice device, int status) {
        log(Log.ERROR, "Connection error, device " + device.getAddress() + ", status: " + status);
        this.progress.setText(R.string.flashing_connection_fail);
        this.status.setText(String.format(getString(R.string.flashing_status), status));
    }

    /**
     * Create zip for DFU
     */
    private String createDFUZip(String... srcFiles) throws IOException {
        byte[] buffer = new byte[1024];

        File zipFile = new File(getCacheDir() + "/update.zip");
        if (zipFile.exists()) {
            if (zipFile.delete()) {
                if (!zipFile.createNewFile()) {
                    return null;
                }
            } else {
                return null;
            }
        }

        FileOutputStream fileOutputStream = new FileOutputStream(getCacheDir() + "/update.zip");
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);

        for (String file : srcFiles) {

            File srcFile = new File(file);
            FileInputStream fileInputStream = new FileInputStream(srcFile);
            zipOutputStream.putNextEntry(new ZipEntry(srcFile.getName()));

            int length;
            while ((length = fileInputStream.read(buffer)) > 0) {
                zipOutputStream.write(buffer, 0, length);
            }

            zipOutputStream.closeEntry();
            fileInputStream.close();

        }

        // close the ZipOutputStream
        zipOutputStream.close();

        return getCacheDir() + "/update.zip";
    }

    private String createDFUInitPacket(int hexLength) throws IOException {
        ByteArrayOutputStream outputInitPacket;
        outputInitPacket = new ByteArrayOutputStream();

        Log.v(TAG, "DFU App Length: " + hexLength);

        outputInitPacket.write("microbit_app".getBytes()); // "microbit_app"
        outputInitPacket.write(new byte[]{0x1, 0, 0, 0});  // Init packet version
        outputInitPacket.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(hexLength).array());  // App size
        outputInitPacket.write(new byte[]{0, 0, 0, 0x0});  // Hash Size. 0: Ignore Hash
        outputInitPacket.write(new byte[]{
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0
        }); // Hash

        // Write to temp file
        File initPacket = new File(this.getCacheDir() + "/application.dat");
        if (initPacket.exists()) {
            initPacket.delete();
        }
        initPacket.createNewFile();

        FileOutputStream outputStream;
        outputStream = new FileOutputStream(initPacket);
        outputStream.write(outputInitPacket.toByteArray());
        outputStream.flush();

        // Should return from here
        return initPacket.getAbsolutePath();
    }

    private static class HexToDFU {
        private final String path;
        private final int size;

        public HexToDFU(String path, int size) {
            this.path = path;
            this.size = size;
        }

        public String getPath() {
            return path;
        }

        public int getSize() {
            return size;
        }
    }

    private HexToDFU universalHexToDFU(String inputPath, @HardwareType int hardwareType) {
        FileInputStream fis;
        ByteArrayOutputStream outputHex;
        outputHex = new ByteArrayOutputStream();

        ByteArrayOutputStream test = new ByteArrayOutputStream();

        FileOutputStream outputStream;

        int application_size = 0;
        int next = 0;
        boolean records_wanted = true;
        boolean is_fat = false;
        boolean is_v2 = false;
        boolean uses_ESA = false;
        ByteArrayOutputStream lastELA = new ByteArrayOutputStream();
        ByteArrayOutputStream lastESA = new ByteArrayOutputStream();

        try {
            fis = new FileInputStream(inputPath);
            byte[] bs = new byte[Integer.valueOf(FileUtils.getFileSize(inputPath))];
            int i = 0;
            i = fis.read(bs);

            for (int b_x = 0; b_x < bs.length - 1; /* empty */) {

                // Get record from following bytes
                char b_type = (char) bs[b_x + 8];

                // Find next record start, or EOF
                next = 1;
                while ((b_x + next) < i && bs[b_x + next] != ':') {
                    next++;
                }

                // Switch type and determine what to do with this record
                switch (b_type) {
                    case 'A': // Block start
                        is_fat = true;
                        records_wanted = false;

                        // Check data for id
                        if (bs[b_x + 9] == '9' && bs[b_x + 10] == '9' && bs[b_x + 11] == '0' && bs[b_x + 12] == '0') {
                            records_wanted = (hardwareType == BOARD_V1);
                        } else if (bs[b_x + 9] == '9' && bs[b_x + 10] == '9' && bs[b_x + 11] == '0' && bs[b_x + 12] == '1') {
                            records_wanted = (hardwareType == BOARD_V1);
                        } else if (bs[b_x + 9] == '9' && bs[b_x + 10] == '9' && bs[b_x + 11] == '0' && bs[b_x + 12] == '3') {
                            records_wanted = (hardwareType == BOARD_V2);
                        }
                        break;
                    case 'E':
                        break;
                    case '4':
                        ByteArrayOutputStream currentELA = new ByteArrayOutputStream();
                        currentELA.write(bs, b_x, next);

                        uses_ESA = false;

                        // If ELA has changed write
                        if (!currentELA.toString().equals(lastELA.toString())) {
                            lastELA.reset();
                            lastELA.write(bs, b_x, next);
                            Log.v(TAG, "TEST ELA " + lastELA.toString());
                            outputHex.write(bs, b_x, next);
                        }

                        break;
                    case '2':
                        uses_ESA = true;

                        ByteArrayOutputStream currentESA = new ByteArrayOutputStream();
                        currentESA.write(bs, b_x, next);

                        // If ESA has changed write
                        if (!Arrays.equals(currentESA.toByteArray(), lastESA.toByteArray())) {
                            lastESA.reset();
                            lastESA.write(bs, b_x, next);
                            outputHex.write(bs, b_x, next);
                        }
                        break;
                    case '1':
                        // EOF
                        // Ensure KV storage is erased
                        if (hardwareType == BOARD_V1) {
                            String kv_address = ":020000040003F7\n";
                            String kv_data = ":1000000000000000000000000000000000000000F0\n";
                            outputHex.write(kv_address.getBytes());
                            outputHex.write(kv_data.getBytes());
                        }

                        // Write final block
                        outputHex.write(bs, b_x, next);
                        break;
                    case 'D': // V2 section of Universal Hex
                        // Remove D
                        bs[b_x + 8] = '0';
                        // Find first \n. PXT adds in extra padding occasionally
                        int first_cr = 0;
                        while (bs[b_x + first_cr] != '\n') {
                            first_cr++;
                        }

                        // Skip 1 word records
                        // TODO: Pad this record for uPY FS scratch
                        if (bs[b_x + 2] == '1') break;

                        // Recalculate checksum
                        int checksum = (charToInt((char) bs[b_x + first_cr - 2]) * 16) + charToInt((char) bs[b_x + first_cr - 1]) + 0xD;
                        String checksum_hex = Integer.toHexString(checksum);
                        checksum_hex = "00" + checksum_hex.toUpperCase(); // Pad to ensure we have 2 characters
                        checksum_hex = checksum_hex.substring(checksum_hex.length() - 2);
                        bs[b_x + first_cr - 2] = (byte) checksum_hex.charAt(0);
                        bs[b_x + first_cr - 1] = (byte) checksum_hex.charAt(1);
                    case '3':
                    case '5':
                    case '0':
                        // Copy record to hex
                        // Record starts at b_x, next long
                        // Calculate address of record
                        int b_a = 0;
                        if (lastELA.size() > 0 && !uses_ESA) {
                            b_a = 0;
                            b_a = (charToInt((char) lastELA.toByteArray()[9]) << 12) | (charToInt((char) lastELA.toByteArray()[10]) << 8) | (charToInt((char) lastELA.toByteArray()[11]) << 4) | (charToInt((char) lastELA.toByteArray()[12]));
                            b_a = b_a << 16;
                        }
                        if (lastESA.size() > 0 && uses_ESA) {
                            b_a = 0;
                            b_a = (charToInt((char) lastESA.toByteArray()[9]) << 12) | (charToInt((char) lastESA.toByteArray()[10]) << 8) | (charToInt((char) lastESA.toByteArray()[11]) << 4) | (charToInt((char) lastESA.toByteArray()[12]));
                            b_a = b_a * 16;
                        }

                        int b_raddr = (charToInt((char) bs[b_x + 3]) << 12) | (charToInt((char) bs[b_x + 4]) << 8) | (charToInt((char) bs[b_x + 5]) << 4) | (charToInt((char) bs[b_x + 6]));
                        int b_addr = b_a | b_raddr;

                        int lower_bound = 0;
                        int upper_bound = 0;
                        //MICROBIT_V1 lower_bound = 0x18000; upper_bound = 0x38000;
                        if (hardwareType == BOARD_V1) {
                            lower_bound = 0x18000;
                            upper_bound = 0x3BBFF;
                        }
                        //MICROBIT_V2 lower_bound = 0x27000; upper_bound = 0x71FFF;
                        if (hardwareType == BOARD_V2) {
                            lower_bound = 0x1C000;
                            upper_bound = 0x77000;
                        }

                        // Check for Cortex-M4 Vector Table
                        if (b_addr == 0x10 && bs[b_x + 41] != 'E' && bs[b_x + 42] != '0') { // Vectors exist
                            is_v2 = true;
                        }

                        if ((records_wanted || !is_fat) && b_addr >= lower_bound && b_addr < upper_bound) {

                            outputHex.write(bs, b_x, next);
                            // Add to app size
                            application_size = application_size + charToInt((char) bs[b_x + 1]) * 16 + charToInt((char) bs[b_x + 2]);
                        } else {
                            // Log.v(TAG, "TEST " + Integer.toHexString(b_addr) + " BA " + b_a + " LELA " + lastELA.toString() + " " + uses_ESA);
                            // test.write(bs, b_x, next);
                        }

                        break;
                    case 'C':
                    case 'B':
                        records_wanted = false;
                        break;
                    default:
                        Log.e(TAG, "Record type not recognised; TYPE: " + b_type);
                }

                // Record handled. Move to next ':'
                if ((b_x + next) >= i) {
                    break;
                } else {
                    b_x = b_x + next;
                }

            }

            byte[] output = outputHex.toByteArray();
            byte[] testBytes = test.toByteArray();

            Log.v(TAG, "Finished parsing HEX. Writing application HEX for flashing");

            try {
                File hexToFlash = new File(this.getCacheDir() + "/application.hex");
                if (hexToFlash.exists()) {
                    hexToFlash.delete();
                }
                hexToFlash.createNewFile();

                outputStream = new FileOutputStream(hexToFlash);
                outputStream.write(output);
                outputStream.flush();

                // Should return from here
                Log.v(TAG, hexToFlash.getAbsolutePath());

                /*
                if(hardwareType == MICROBIT_V2 && (!is_v2 && !is_fat)) {
                    ret[1] = Integer.toString(-1); // Invalidate hex file
                }
                 */

                return new HexToDFU(hexToFlash.getAbsolutePath(), application_size);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (FileNotFoundException e) {
            Log.v(TAG, "File not found.");
            e.printStackTrace();
        } catch (IOException e) {
            Log.v(TAG, "IO Exception.");
            e.printStackTrace();
        }

        // Should not reach this
        return new HexToDFU(null, -1);
    }

    /**
     * Convert a HEX char to int
     */
    int charToInt(char in) {
        // 0 - 9
        if (in - '0' >= 0 && in - '0' < 10) return (in - '0');

        // A - F
        return in - 55;
    }

    private void pairDevice(BluetoothDevice device) {
        try {
            Utils.log(TAG, "Start Pairing...");
            //waitingForBonding = true;

            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

            Utils.log(TAG, "Pairing finished.");
        } catch (Exception e) {
            Utils.log(Log.ERROR, TAG, e.getMessage());
        }
    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Utils.log(Log.ERROR, TAG, e.getMessage());
        }
    }
}