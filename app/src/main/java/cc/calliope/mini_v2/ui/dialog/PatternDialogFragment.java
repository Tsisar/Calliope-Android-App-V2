package cc.calliope.mini_v2.ui.dialog;

import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Method;
import java.util.Arrays;
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
    private final List<Float> currentPattern = Arrays.asList(0f, 0f, 0f, 0f, 0f);

    private ScannerViewModel scannerViewModel;
//    private ExtendedBluetoothDevice currentDevice;

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
        scannerViewModel = new ViewModelProvider(requireActivity()).get(ScannerViewModel.class);
        scannerViewModel.getScannerState().observe(getViewLifecycleOwner(), this::scanResults);
        scannerViewModel.setCurrentPattern(currentPattern);

        //TODO check permissions
//        scannerViewModel.startScan();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getDialog() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        binding.patternMatrix.columnA.setValue(currentPattern.get(0));
        binding.patternMatrix.columnB.setValue(currentPattern.get(1));
        binding.patternMatrix.columnC.setValue(currentPattern.get(2));
        binding.patternMatrix.columnD.setValue(currentPattern.get(3));
        binding.patternMatrix.columnE.setValue(currentPattern.get(4));

        binding.patternMatrix.columnA.setOnChangeListener((bar, v, b) -> onPatternChange(0, v));
        binding.patternMatrix.columnB.setOnChangeListener((bar, v, b) -> onPatternChange(1, v));
        binding.patternMatrix.columnC.setOnChangeListener((bar, v, b) -> onPatternChange(2, v));
        binding.patternMatrix.columnD.setOnChangeListener((bar, v, b) -> onPatternChange(3, v));
        binding.patternMatrix.columnE.setOnChangeListener((bar, v, b) -> onPatternChange(4, v));

        binding.buttonAction.setOnClickListener(view1 -> onConnectClick());
    }

    @Override
    public void onStop() {
        super.onStop();
        scannerViewModel.stopScan();
        savePattern();
    }

    private void onPatternChange(int index, float newPatten) {
        currentPattern.set(index, newPatten);
        scannerViewModel.setCurrentPattern(currentPattern);
    }

    private void setButtonBackground(ExtendedBluetoothDevice device) {
        if (device != null) {
            binding.buttonAction.setBackgroundResource(R.drawable.btn_connect_green);
        } else {
            binding.buttonAction.setBackgroundResource(R.drawable.btn_connect_aqua);
        }
    }

    // Call this method to send the data back to the parent fragment
    public void onConnectClick() {
        ExtendedBluetoothDevice device = scannerViewModel.getScannerState().getCurrentDevice();
        if (device != null) {
            DeviceViewModel viewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);
            viewModel.setDevice(device);
//            pairDevice(device.getDevice());
        }
        dismiss();
    }

    private void scanResults(final ScannerLiveData state) {
        // Bluetooth must be enabled
        if (state.isBluetoothEnabled()) {
            scannerViewModel.startScan();

            ExtendedBluetoothDevice currentDevice = state.getCurrentDevice();
            Log.w("DIALOG: ", "currentDevice: " + currentDevice);
            setButtonBackground(currentDevice);
        } else {
            Utils.showErrorMessage(getActivity(), "Bluetooth is disable");
            setButtonBackground(null);
            Log.e("DIALOG: ", "Bluetooth is disable");
        }
    }

    public void savePattern() {
        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        for (int i = 0; i < 5; i++) {
            edit.putFloat("PATTERN_" + i, currentPattern.get(i));
        }
        edit.apply();
    }

    public void loadPattern() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        for (int i = 0; i < 5; i++) {
            currentPattern.set(i, preferences.getFloat("PATTERN_" + i, 0f));
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