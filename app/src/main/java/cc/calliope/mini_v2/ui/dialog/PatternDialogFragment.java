package cc.calliope.mini_v2.ui.dialog;

import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import cc.calliope.mini_v2.utils.Utils;
import cc.calliope.mini_v2.viewmodels.DeviceViewModel;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.adapter.ExtendedBluetoothDevice;
import cc.calliope.mini_v2.databinding.DialogPatternBinding;
import cc.calliope.mini_v2.viewmodels.ScannerLiveData;
import cc.calliope.mini_v2.viewmodels.ScannerViewModel;

public class PatternDialogFragment extends DialogFragment {

    private DialogPatternBinding binding;
    private final List<Float> patternList = Arrays.asList(0f, 0f, 0f, 0f, 0f);

    private ScannerViewModel scannerViewModel;
    private HashSet<ExtendedBluetoothDevice> devices;

    public PatternDialogFragment() {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
    }

    public static PatternDialogFragment newInstance() {
        return new PatternDialogFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        loadPattern();
        binding = DialogPatternBinding.inflate(inflater, container, false);
        // Create view model containing utility methods for scanning
        scannerViewModel = new ViewModelProvider(this).get(ScannerViewModel.class);
        scannerViewModel.getScannerState().observe(this, this::startScan);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getDialog() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        binding.patternMatrix.patternA.setPattern(patternList.get(0));
        binding.patternMatrix.patternB.setPattern(patternList.get(1));
        binding.patternMatrix.patternC.setPattern(patternList.get(2));
        binding.patternMatrix.patternD.setPattern(patternList.get(3));
        binding.patternMatrix.patternE.setPattern(patternList.get(4));

        binding.patternMatrix.patternA.setOnRatingBarChangeListener((ratingBar, v, b) -> onPatternChange(0, v));
        binding.patternMatrix.patternB.setOnRatingBarChangeListener((ratingBar, v, b) -> onPatternChange(1, v));
        binding.patternMatrix.patternC.setOnRatingBarChangeListener((ratingBar, v, b) -> onPatternChange(2, v));
        binding.patternMatrix.patternD.setOnRatingBarChangeListener((ratingBar, v, b) -> onPatternChange(3, v));
        binding.patternMatrix.patternE.setOnRatingBarChangeListener((ratingBar, v, b) -> onPatternChange(4, v));

        binding.buttonAction.setOnClickListener(view1 -> sendResult());
    }


    private void onPatternChange(int index, float newPatten) {
        patternList.set(index, newPatten);
        setButtonBackground();
    }

    private void setButtonBackground() {
        if (getDeviceByPattern(patternList) != null) {
            binding.buttonAction.setBackgroundResource(R.drawable.btn_connect_green);
        } else {
            binding.buttonAction.setBackgroundResource(R.drawable.btn_connect_aqua);
        }
    }

    // Call this method to send the data back to the parent fragment
    public void sendResult() {
        ExtendedBluetoothDevice device = getDeviceByPattern(patternList);
        if (device != null) {
            DeviceViewModel viewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);
            viewModel.setDevice(device);
            pairDevice(device.getDevice());
        }
        dismiss();
    }

    private ExtendedBluetoothDevice getDeviceByPattern(List<Float> pattern) {
        if (devices != null) {
            for (ExtendedBluetoothDevice device : devices) {
                int coincide = 0;
                for (int i = 0; i < 5; i++) {
                    char character = device.getPattern().charAt(i);
                    String iPattern = Pattern.forCode(pattern.get(i)).toString();
                    if (iPattern.contains(String.valueOf(character))) {
                        coincide++;
                    }
                }
                if (coincide == 5) {
                    return device;
                }
            }
        }
        return null;
    }

    private void startScan(final ScannerLiveData state) {
        //TODO Винеси перевірки на дозволи в актівіті, залишити тільки перевірки активності BT, GPS

        // First, check the Location permission. This is required on Marshmallow onwards in order to scan for Bluetooth LE devices.
        if((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && Utils.isBluetoothScanPermissionsGranted(getActivity()))
                || (Utils.isLocationPermissionsGranted(getActivity()) && Utils.isBluetoothAdminPermissionsGranted(getActivity()))){
            Log.v("DIALOG: ", "All ok, permission granted");

            // Bluetooth must be enabled
            if (state.isBluetoothEnabled()) {
                // We are now OK to start scanning
                Log.v("DIALOG: ", "Start scanning");
                scannerViewModel.startScan();
                devices = new LinkedHashSet<>(state.getDevices());

                setButtonBackground();

                for (ExtendedBluetoothDevice device : state.getDevices()) {
                    Log.e("DIALOG: ", "Device: " + device.getName() + " " + device.getRssi() + " " + device.getAddress());
                }

                if (state.isEmpty()) {
                    Log.e("DIALOG: ", "Devices list is entry");
                    if (!Utils.isLocationRequired(getActivity()) || Utils.isLocationEnabled(getActivity())) {
                        Log.v("DIALOG: ", "Location enabled");
                    } else {
                        Log.e("DIALOG: ", "Location disabled");
                    }
                } else {
                    Log.v("DIALOG: ", "Scanning finished");
                }
            } else {
                Log.e("DIALOG: ", "Bluetooth disabled");
            }
        } else {
            Log.e("DIALOG: ", "No location or bt permission");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        scannerViewModel.stopScan();
        savePattern();
    }

    public void savePattern() {
        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        for (int i = 0; i < 5; i++) {
            edit.putFloat("PATTERN_" + i, patternList.get(i));
        }
        edit.apply();
    }

    public void loadPattern() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        for (int i = 0; i < 5; i++) {
            patternList.set(i, preferences.getFloat("PATTERN_" + i, 0f));
        }
    }

    //TODO are we need it?
    private void pairDevice(BluetoothDevice device) {
        try {
            Log.d("PAIRING", "Start Pairing...");

            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

            Log.d("PAIRING", "Pairing finished.");
        } catch (Exception e) {
            Log.e("PAIRING", e.getMessage());
        }
    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Log.e("PAIRING", e.getMessage());
        }
    }
}