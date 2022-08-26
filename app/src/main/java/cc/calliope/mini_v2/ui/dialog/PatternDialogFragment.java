package cc.calliope.mini_v2.ui.dialog;

import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import cc.calliope.mini_v2.DeviceViewModel;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.adapter.ExtendedBluetoothDevice;
import cc.calliope.mini_v2.databinding.DialogPatternBinding;
import cc.calliope.mini_v2.utils.Utils;
import cc.calliope.mini_v2.viewmodels.ScannerLiveData;
import cc.calliope.mini_v2.viewmodels.ScannerViewModel;

public class PatternDialogFragment extends DialogFragment {

    private DialogPatternBinding binding;
    private final List<String> patternList = Arrays.asList("XX", "XX", "XX", "XX", "XX");

    private ScannerViewModel scannerViewModel;
    private HashSet<ExtendedBluetoothDevice> devices;

    public PatternDialogFragment() {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
    }

    public static PatternDialogFragment newInstance(String pattern) {
        PatternDialogFragment patternDialogFragment = new PatternDialogFragment();

        Bundle args = new Bundle();
        args.putString("pattern", pattern);
        patternDialogFragment.setArguments(args);

        return patternDialogFragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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

        Bundle bundle = getArguments();
        if (bundle != null) {
            Log.e("Pattern", bundle.getString("pattern", "00000"));
        }
        //TODO we need save & restore it
//        binding.patternA.setPattern(2);
//        binding.patternB.setPattern(5);
//        binding.patternC.setPattern(3);
//        binding.patternD.setPattern(1);
//        binding.patternE.setPattern(2);

        binding.patternMatrix.patternA.setOnRatingBarChangeListener((ratingBar, v, b) -> onPatternChange(0, v));
        binding.patternMatrix.patternB.setOnRatingBarChangeListener((ratingBar, v, b) -> onPatternChange(1, v));
        binding.patternMatrix.patternC.setOnRatingBarChangeListener((ratingBar, v, b) -> onPatternChange(2, v));
        binding.patternMatrix.patternD.setOnRatingBarChangeListener((ratingBar, v, b) -> onPatternChange(3, v));
        binding.patternMatrix.patternE.setOnRatingBarChangeListener((ratingBar, v, b) -> onPatternChange(4, v));

        binding.buttonAction.setOnClickListener(view1 -> sendResult());
    }


    private void onPatternChange(int index, float newPatten) {
        patternList.set(index, Pattern.forCode(newPatten).toString());
        setButtonBackground();
    }

    private void setButtonBackground() {
        if (getDeviceByPattern(patternList) != null) {
            binding.buttonAction.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.btn_connect_green, null));
        } else {
            binding.buttonAction.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.btn_connect_aqua, null));
        }
    }

    // Call this method to send the data back to the parent fragment
    public void sendResult() {
        ExtendedBluetoothDevice device = getDeviceByPattern(patternList);
        if (device != null) {
//            Bundle result = new Bundle();
//            result.putParcelable("pattern_key", testDevice);
//            getParentFragmentManager().setFragmentResult("pattern_request_key", result);

            DeviceViewModel viewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);
            viewModel.setDevice(device);
        }

        dismiss();
    }

    private ExtendedBluetoothDevice getDeviceByPattern(List<String> pattern) {
        if (devices != null) {
            for (ExtendedBluetoothDevice device : devices) {
                int coincide = 0;
                for (int i = 0; i < 5; i++) {
                    char character = device.getPattern().charAt(i);
                    if (pattern.get(i) != null && pattern.get(i).contains(String.valueOf(character))) {
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
        Log.v("Scan Permission: ", Utils.isBluetoothScanPermissionsGranted(getActivity()) + "");
        Log.v("Location Permission: ", Utils.isLocationPermissionsGranted(getActivity()) + "");
        Log.v("Version", Build.VERSION.SDK_INT + "");

        // First, check the Location permission. This is required on Marshmallow onwards in order to scan for Bluetooth LE devices.
        if (Utils.isLocationPermissionsGranted(getActivity())
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || Utils.isBluetoothScanPermissionsGranted(getActivity()))
        ) {
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
    }
}