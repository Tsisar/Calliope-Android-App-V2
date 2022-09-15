package cc.calliope.mini_v2;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import cc.calliope.mini_v2.databinding.ActivityMainBinding;
import cc.calliope.mini_v2.ui.dialog.PatternDialogFragment;
import cc.calliope.mini_v2.utils.Utils;
import cc.calliope.mini_v2.viewmodels.ScannerLiveData;
import cc.calliope.mini_v2.viewmodels.ScannerViewModel;


public class MainActivity extends AppCompatActivity implements DialogInterface.OnDismissListener {
    private static final String TAG = "MAIN";

    private static final int REQUEST_CODE = 1022; // random number
    private static boolean requestWasSent = false;

    private ActivityMainBinding binding;
    private ScannerViewModel scannerViewModel;

    @IntDef({BLUETOOTH, LOCATION})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RequestType {
    }

    public static final int BLUETOOTH = 0;
    public static final int LOCATION = 1;

    @RequiresApi(api = Build.VERSION_CODES.S)
    private static final String[] BLUETOOTH_PERMISSIONS_S = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT};

    private static final String[] BLUETOOTH_PERMISSIONS = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN};

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private static final String[] LOCATION_PERMISSIONS_Q = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION};

    private static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION};

    private String[] getPermissionsArray(@RequestType int requestType) {
        switch (requestType) {
            case BLUETOOTH:
            default:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    return BLUETOOTH_PERMISSIONS_S;
                }
                return BLUETOOTH_PERMISSIONS;
            case LOCATION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    return LOCATION_PERMISSIONS_Q;
                }
                return LOCATION_PERMISSIONS;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);

        binding.fab.setOnClickListener(view -> {
            view.startAnimation(new AlphaAnimation(1F, 0.75F));
            showPatternDialog(new FobParams(
                    view.getWidth(),
                    view.getHeight(),
                    view.getX(),
                    view.getY()
            ));
        });

        scannerViewModel = new ViewModelProvider(this).get(ScannerViewModel.class);
        scannerViewModel.getScannerState().observe(this, this::scanResults);

        binding.infoNoPermission.btnNoPermissionAction.setOnClickListener(v -> requestPermissions());
        binding.infoNoPermission.btnNoPermissionSettings.setOnClickListener(v -> requestAppSettings());
    }

    @Override
    public void onResume() {
        super.onResume();
        checkPermission();
    }

    @Override
    public void onPause() {
        super.onPause();
        scannerViewModel.stopScan();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        //Fragment dialog had been dismissed
        binding.fab.setVisibility(View.VISIBLE);
    }

//    @Override
//    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions,
//                                           @NonNull final int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        Log.d(TAG, "onRequestPermissionsResult: requestCode: " + requestCode + ", "
//                + "permissions: " + Arrays.toString(permissions) + ", "
//                + "grantResults: " + Arrays.toString(grantResults));
//    }

    private void scanResults(final ScannerLiveData state) {
        // Bluetooth must be enabled
//        Log.e("SCANNER", "current device: " + state.getCurrentDevice());

        if(hasOpenedDialogs(this))
            return;

        if (state.isBluetoothEnabled()) {
            binding.fab.setBackgroundTintList(
                    ColorStateList.valueOf(
                            getColor(
                                    state.getCurrentDevice() != null
                                            ? R.color.green : R.color.aqua_200
                            )
                    )
            );
        } else {
            Utils.showErrorMessage(binding.getRoot(), "Bluetooth is disable");
            openBluetoothEnableActivity();
        }
    }

    private void checkPermission() {
        boolean isBluetoothAccessGranted = isAccessGranted(BLUETOOTH);
        boolean isLocationAccessGranted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || isAccessGranted(LOCATION);

        if (!isBluetoothAccessGranted || !isLocationAccessGranted) {
            showInfoNoPermission(isBluetoothAccessGranted, isLocationAccessGranted);
        } else {
            showContainer();
            scannerViewModel.startScan();
        }
//        Log.e(TAG, "isNetworkConnected: " + Utils.isNetworkConnected(this));
//        Log.e(TAG, "isInternetAvailable: " + Utils.isInternetAvailable());
    }

    public void openBluetoothEnableActivity() {
        if (!requestWasSent) {
            requestWasSent = true;
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            bluetoothEnableResultLauncher.launch(enableBtIntent);
        }
    }

    ActivityResultLauncher<Intent> bluetoothEnableResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.i(TAG, "BluetoothEnabled");
                    checkPermission();
                }
            });

    private void showPatternDialog(FobParams params) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        PatternDialogFragment dialogFragment = PatternDialogFragment.newInstance(params);
        dialogFragment.show(fragmentManager, "fragment_pattern");

//        binding.fab.setVisibility(View.GONE);
        binding.fab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.aqua_200)));
    }

    //TODO for refactoring
    private void showInfoNoPermission(boolean isBluetoothAccessGranted, boolean isLocationAccessGranted) {
        ContentNoPermission content = ContentNoPermission.BLUETOOTH;
        boolean deniedForever = false;

        binding.container.setVisibility(View.GONE);
        binding.infoNoPermission.getRoot().setVisibility(View.VISIBLE);

        if (!isBluetoothAccessGranted) {
            deniedForever = Utils.isPermissionDeniedForever(this, getPermissionsArray(BLUETOOTH)[0]);
        } else if (!isLocationAccessGranted) {
            deniedForever = Utils.isPermissionDeniedForever(this, getPermissionsArray(LOCATION)[0]);
            content = ContentNoPermission.LOCATION;
        }

        binding.infoNoPermission.ivNoPermission.setImageResource(content.getIcResId());
        binding.infoNoPermission.tvNoPermissionTitle.setText(content.getTitleResId());
        binding.infoNoPermission.tvNoPermissionInfo.setText(content.getInfoResId());

        binding.infoNoPermission.btnNoPermissionAction.setVisibility(deniedForever ? View.GONE : View.VISIBLE);
        binding.infoNoPermission.btnNoPermissionSettings.setVisibility(deniedForever ? View.VISIBLE : View.GONE);
    }

    private void showContainer() {
        binding.container.setVisibility(View.VISIBLE);
        binding.infoNoPermission.getRoot().setVisibility(View.GONE);
    }

    private void requestPermissions() {
        Utils.markPermissionRequested(this);
        ActivityCompat.requestPermissions(this, getPermissionsArray(isAccessGranted(BLUETOOTH) ? LOCATION : BLUETOOTH), REQUEST_CODE);
    }

    private void requestAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private boolean isAccessGranted(@RequestType int requestType) {
        String[] permissions = getPermissionsArray(requestType);

        for (String permission : permissions) {
            boolean granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
            Log.d("PERMISSION", permission + (granted ? " granted" : " denied"));
            if (!granted) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasOpenedDialogs(AppCompatActivity activity) {
        List<Fragment> fragments = activity.getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof DialogFragment) {
                return true;
            }
        }
        return false;
    }
}