package cc.calliope.mini_v2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import cc.calliope.mini_v2.databinding.ActivityMainBinding;
import cc.calliope.mini_v2.ui.dialog.PatternDialogFragment;
import cc.calliope.mini_v2.utils.Utils;
import cc.calliope.mini_v2.viewmodels.DeviceViewModel;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MAIN";

    private static final int REQUEST_CODE = 1022; // random number

    private ActivityMainBinding binding;

    private boolean isBluetoothAccessGranted = true;
    private boolean isLocationAccessGranted = true;

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

        DeviceViewModel viewModel = new ViewModelProvider(this).get(DeviceViewModel.class);
        viewModel.getDevice().observe(this, device -> {
            // Perform an action with the latest item data
            Log.e("TEST_DEVICE", device.getPattern());
        });

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);

        binding.fab.setOnClickListener(view -> {
            view.startAnimation(new AlphaAnimation(1F, 0.3F));
            showPatternDialog();
        });

        binding.infoNoPermission.btnNoPermissionAction.setOnClickListener(v -> requestPermissions());
        binding.infoNoPermission.btnNoPermissionSettings.setOnClickListener(v -> requestAppSettings());
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        checkPermission();
    }

//    @Override
//    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions,
//                                           @NonNull final int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        Log.d(TAG, "onRequestPermissionsResult: requestCode: " + requestCode + ", "
//                + "permissions: " + Arrays.toString(permissions) + ", "
//                + "grantResults: " + Arrays.toString(grantResults));
//    }

    private void checkPermission() {
        isBluetoothAccessGranted = isAccessGranted(BLUETOOTH);
        isLocationAccessGranted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || isAccessGranted(LOCATION);

        if (!isBluetoothAccessGranted || !isLocationAccessGranted) {
            showInfoNoPermission();
        } else {
            showContainer();
        }

        Log.e(TAG, "isBluetoothEnabled: " + Utils.isBluetoothEnabled());
        Log.e(TAG, "isLocationEnabled: " + Utils.isLocationEnabled(this));

        Log.e(TAG, "isNetworkConnected: " + Utils.isNetworkConnected(this));
        Log.e(TAG, "isInternetAvailable: " + Utils.isInternetAvailable());
    }

    private void showPatternDialog() {
        FragmentManager parentFragmentManager = getSupportFragmentManager();
        PatternDialogFragment patternDialogFragment = PatternDialogFragment.newInstance();
        patternDialogFragment.show(parentFragmentManager, "fragment_pattern");
    }

    private void showInfoNoPermission() {
        boolean deniedForever = false;

        binding.container.setVisibility(View.GONE);
        binding.infoNoPermission.getRoot().setVisibility(View.VISIBLE);

        if (!isBluetoothAccessGranted) {
            deniedForever = Utils.isPermissionDeniedForever(this, getPermissionsArray(BLUETOOTH)[0]);

            binding.infoNoPermission.ivNoPermission.setImageResource(R.drawable.ic_bluetooth_disabled);
            binding.infoNoPermission.tvNoPermissionTitle.setText(R.string.bluetooth_permission_title);
            binding.infoNoPermission.tvNoPermissionInfo.setText(R.string.bluetooth_permission_info);
        } else if (!isLocationAccessGranted) {
            deniedForever = Utils.isPermissionDeniedForever(this, getPermissionsArray(LOCATION)[0]);

            binding.infoNoPermission.ivNoPermission.setImageResource(R.drawable.ic_location_disabled);
            binding.infoNoPermission.tvNoPermissionTitle.setText(R.string.location_permission_title);
            binding.infoNoPermission.tvNoPermissionInfo.setText(R.string.location_permission_info);
        }

        binding.infoNoPermission.btnNoPermissionAction.setVisibility(deniedForever ? View.GONE : View.VISIBLE);
        binding.infoNoPermission.btnNoPermissionSettings.setVisibility(deniedForever ? View.VISIBLE : View.GONE);
    }

    private void showContainer() {
        binding.container.setVisibility(View.VISIBLE);
        binding.infoNoPermission.getRoot().setVisibility(View.GONE);
    }

    private void requestPermissions() {
        Utils.markPermissionRequested(this);
        ActivityCompat.requestPermissions(this, getPermissionsArray(isBluetoothAccessGranted ? LOCATION : BLUETOOTH), REQUEST_CODE);
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
}