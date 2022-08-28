package cc.calliope.mini_v2;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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
    private static final int REQUEST_LOCATION_ACCESS_CODE = 1022; // random number
    private static final int REQUEST_BLUETOOTH_ACCESS_CODE = 1023; // random number

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

    private ActivityMainBinding binding;


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
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        });

        binding.noLocationPermission.actionGrantLocationPermission.setOnClickListener(v -> requestLocationAccess());
        binding.noLocationPermission.actionPermissionSettings.setOnClickListener(v -> openAppPermissionScreen());
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.v("MAIN", "onResume");
        checkPermission();
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.v("MAIN", "onRequestPermissionsResult: requestCode: " + requestCode + ", " + "permissions: " +  Arrays.toString(permissions) + ", " + "grantResults: " +  Arrays.toString(grantResults));
    }

    private void showPatternDialog() {
        FragmentManager parentFragmentManager = getSupportFragmentManager();
        PatternDialogFragment patternDialogFragment = PatternDialogFragment.newInstance();
        patternDialogFragment.show(parentFragmentManager, "fragment_pattern");
    }

    private void requestLocationAccess() {
        Utils.markLocationPermissionRequested(this);
        ActivityCompat.requestPermissions(this,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? LOCATION_PERMISSIONS_Q : LOCATION_PERMISSIONS,
                REQUEST_LOCATION_ACCESS_CODE);
    }

    private void openAppPermissionScreen(){
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void requestBluetoothAccess() {
        ActivityCompat.requestPermissions(this,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? BLUETOOTH_PERMISSIONS_S : BLUETOOTH_PERMISSIONS,
                REQUEST_BLUETOOTH_ACCESS_CODE);
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!Utils.isBluetoothScanPermissionsGranted(this)) {
                requestBluetoothAccess();
                Log.e("UTILS", "isBluetoothScanPermissionsGranted: " + Utils.isBluetoothScanPermissionsGranted(this));
            }else {
                allPermissionsGranted();
            }
        } else {
            if (!Utils.isBluetoothAdminPermissionsGranted(this)) {
//                requestLocationPermissions(BLUETOOTH_PERMISSIONS);
                Log.e("UTILS", "isBluetoothAdminPermissionsGranted: " + Utils.isBluetoothAdminPermissionsGranted(this));
            }else if(!Utils.isLocationPermissionsGranted(this)) {
                noLocationPermission();
                Log.e("UTILS", "isLocationPermissionsGranted: " + Utils.isLocationPermissionsGranted(this));
            }else {
                allPermissionsGranted();
            }

        }

        Log.e("UTILS", "isBluetoothEnabled: " + Utils.isBluetoothEnabled());
        Log.e("UTILS", "isLocationEnabled: " + Utils.isLocationEnabled(this));

        Log.e("UTILS", "isNetworkConnected: " + Utils.isNetworkConnected(this));
        Log.e("UTILS", "isInternetAvailable: " + Utils.isInternetAvailable());
    }

    private void noLocationPermission() {
        final boolean deniedForever = Utils.isLocationPermissionDeniedForever(this);

        binding.container.setVisibility(View.GONE);
        binding.noLocationPermission.getRoot().setVisibility(View.VISIBLE);
        binding.noLocationPermission.actionGrantLocationPermission
                .setVisibility(deniedForever ? View.GONE : View.VISIBLE);
        binding.noLocationPermission.actionPermissionSettings
                .setVisibility(deniedForever ? View.VISIBLE : View.GONE);
    }

    private void allPermissionsGranted(){
        binding.container.setVisibility(View.VISIBLE);
        binding.noLocationPermission.getRoot().setVisibility(View.GONE);
    }
}