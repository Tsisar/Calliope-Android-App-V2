package cc.calliope.mini_v2;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;

import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import cc.calliope.mini_v2.adapter.ExtendedBluetoothDevice;
import cc.calliope.mini_v2.databinding.ActivityMainBinding;
import cc.calliope.mini_v2.ui.dialog.PatternDialogFragment;
import cc.calliope.mini_v2.utils.Permission;
import cc.calliope.mini_v2.utils.Utils;
import cc.calliope.mini_v2.utils.Version;
import cc.calliope.mini_v2.viewmodels.ScannerLiveData;
import cc.calliope.mini_v2.viewmodels.ScannerViewModel;
import cc.calliope.mini_v2.views.FobParams;


public class MainActivity extends AppCompatActivity implements DialogInterface.OnDismissListener {
    private static final String TAG = "MAIN";

    private static final int REQUEST_CODE = 1022; // random number
    private static boolean requestWasSent = false;

    private ActivityMainBinding binding;
    private ScannerViewModel scannerViewModel;

    ActivityResultLauncher<Intent> bluetoothEnableResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.i(TAG, "BluetoothEnabled");
                    checkPermission();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);

        binding.fab.setOnClickListener(this::onFabClick);

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
//        binding.fab.setVisibility(View.VISIBLE);
        scannerViewModel.startScan();
    }

    private void onFabClick(View view) {
        view.startAnimation(new AlphaAnimation(1F, 0.75F));
        showPatternDialog(new FobParams(
                view.getWidth(),
                view.getHeight(),
                view.getX(),
                view.getY()
        ));
    }

    public int getColorWrapper(int id) {
        if (Version.upperMarshmallow) {
            return getColor(id);
        } else {
            //noinspection deprecation
            return getResources().getColor(id);
        }
    }

    private void scanResults(final ScannerLiveData state) {
//        Log.v("SCANNER", "current device: " + state.getCurrentDevice());

        if (hasOpenedDialogs(this))
            return;

        if (!state.isBluetoothEnabled()) {
            showPeripheralsStatus(state.isBluetoothEnabled(), true);
        }

        ExtendedBluetoothDevice device = state.getCurrentDevice();
        int color = getColorWrapper(device != null && device.isRelevant() ? R.color.green : R.color.orange);
        binding.fab.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    private void checkPermission() {
        boolean isBluetoothAccessGranted = Permission.isAccessGranted(this, Permission.BLUETOOTH);
        boolean isLocationAccessGranted = Version.upperSnowCone || Permission.isAccessGranted(this, Permission.LOCATION);

        if (isBluetoothAccessGranted && isLocationAccessGranted) {
            showPeripheralsStatus(Utils.isBluetoothEnabled(), Utils.isLocationEnabled(this));
            showContent();
            scannerViewModel.startScan();
        } else {
            showInfoNoPermission(isBluetoothAccessGranted ? Permission.LOCATION : Permission.BLUETOOTH);
        }
    }

    private void showPeripheralsStatus(boolean isBluetoothEnabled, boolean isLocationEnabled) {
        if (!isBluetoothEnabled) {
            Utils.errorSnackbar(binding.getRoot(), "Bluetooth is disable")
                    .setAction("Enable", view -> openBluetoothEnableActivity())
                    .show();
        } else if (!Version.upperSnowCone && !isLocationEnabled) {
            Utils.errorSnackbar(binding.getRoot(), "Location is disable").show();
        }
    }

    private void showContent() {
        binding.constraintLayout.setVisibility(View.VISIBLE);
        binding.infoNoPermission.getRoot().setVisibility(View.GONE);
    }

    private void showInfoNoPermission(@Permission.RequestType int requestType) {
        ContentNoPermission content = ContentNoPermission.getContent(requestType);
        boolean deniedForever = Permission.isAccessDeniedForever(this, requestType);

        binding.constraintLayout.setVisibility(View.GONE);
        binding.infoNoPermission.getRoot().setVisibility(View.VISIBLE);

        binding.infoNoPermission.ivNoPermission.setImageResource(content.getIcResId());
        binding.infoNoPermission.tvNoPermissionTitle.setText(content.getTitleResId());
        binding.infoNoPermission.tvNoPermissionInfo.setText(content.getInfoResId());

        binding.infoNoPermission.btnNoPermissionAction
                .setVisibility(deniedForever ? View.GONE : View.VISIBLE);
        binding.infoNoPermission.btnNoPermissionSettings
                .setVisibility(deniedForever ? View.VISIBLE : View.GONE);
    }

    public void openBluetoothEnableActivity() {
        if (!requestWasSent) {
            requestWasSent = true;
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            bluetoothEnableResultLauncher.launch(enableBtIntent);
        }
    }

    private void showPatternDialog(FobParams params) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        PatternDialogFragment dialogFragment = PatternDialogFragment.newInstance(params);
        dialogFragment.show(fragmentManager, "fragment_pattern");

//        binding.fab.setVisibility(View.GONE);
        binding.fab.setBackgroundTintList(ColorStateList.valueOf(getColorWrapper(R.color.orange)));
    }

    private void requestPermissions() {
        int requestType = Permission.isAccessGranted(this, Permission.BLUETOOTH) ?
                Permission.LOCATION :
                Permission.BLUETOOTH;
        String[] permissionsArray = Permission.getPermissionsArray(requestType);

        Permission.markPermissionRequested(this, requestType);
        ActivityCompat.requestPermissions(this, permissionsArray, REQUEST_CODE);
    }

    private void requestAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
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