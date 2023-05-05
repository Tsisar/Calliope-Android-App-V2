package cc.calliope.mini_v2.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.adapter.ExtendedBluetoothDevice;
import cc.calliope.mini_v2.dialog.pattern.PatternDialogFragment;
import cc.calliope.mini_v2.utils.Permission;
import cc.calliope.mini_v2.utils.Utils;
import cc.calliope.mini_v2.utils.Version;
import cc.calliope.mini_v2.viewmodels.ProgressViewModel;
import cc.calliope.mini_v2.viewmodels.ScannerLiveData;
import cc.calliope.mini_v2.viewmodels.ScannerViewModel;
import cc.calliope.mini_v2.views.FobParams;
import cc.calliope.mini_v2.views.MovableFloatingActionButton;

public abstract class ScannerActivity extends AppCompatActivity implements DialogInterface.OnDismissListener{
    private static final int SNACKBAR_DURATION = 10000; // how long to display the snackbar message.
    private static boolean requestWasSent = false;
    private ScannerViewModel scannerViewModel;
    private MovableFloatingActionButton patternFab;
    private View rootView;
    private Boolean isFlashingProcess = false; //
    ActivityResultLauncher<Intent> bluetoothEnableResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {}
    );
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        scannerViewModel = new ViewModelProvider(this).get(ScannerViewModel.class);
        scannerViewModel.getScannerState().observe(this, this::scanResults);

        ProgressViewModel progressViewModel = new ViewModelProvider(this).get(ProgressViewModel.class);
        progressViewModel.getProgress().observe(this, this::setProgress);
    }

    @Override
    public void onResume() {
        super.onResume();
        requestWasSent = false;
        checkPermission();
    }

    @Override
    public void onPause() {
        super.onPause();
        scannerViewModel.stopScan();
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        //Fragment dialog had been dismissed
//        fab.setVisibility(View.VISIBLE);
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        this.rootView = view;
    }

    public void setPatternFab(MovableFloatingActionButton patternFab) {
        this.patternFab = patternFab;
        this.patternFab.setOnClickListener(this::onFabClick);
    }

    private void checkPermission() {
        boolean isBluetoothAccessGranted = Permission.isAccessGranted(this, Permission.BLUETOOTH);
        boolean isLocationAccessGranted = Version.upperSnowCone || Permission.isAccessGranted(this, Permission.LOCATION);

        if(isBluetoothAccessGranted && isLocationAccessGranted){
            if (!Utils.isBluetoothEnabled()) {
                showBluetoothDisabledWarning();
            } else if (!Version.upperSnowCone && !Utils.isLocationEnabled(this)) {
                showLocationDisabledWarning();
            }
            scannerViewModel.startScan();
        }else{
            startNoPermissionActivity();
        }
    }

    protected void scanResults(final ScannerLiveData state) {
        if (hasOpenedDialogs() || isFlashingProcess)
            return;

        if (!state.isBluetoothEnabled() && !requestWasSent) {
            showBluetoothDisabledWarning();
        }

        ExtendedBluetoothDevice device = state.getCurrentDevice();
        int color = getColorWrapper(
                device != null && device.isRelevant()
                        ? R.color.green
                        : R.color.orange
        );
        patternFab.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    private void showPatternDialog(FobParams params) {
        scannerViewModel.startScan(); // On older devices, "auto-start" scanning does not work after bluetooth is turned on.

        FragmentManager fragmentManager = getSupportFragmentManager();
        PatternDialogFragment dialogFragment = PatternDialogFragment.newInstance(params);
        dialogFragment.show(fragmentManager, "fragment_pattern");
    }

    private void showBluetoothDisabledWarning() {
        Snackbar snackbar = Utils.errorSnackbar(rootView, getString(R.string.error_snackbar_bluetooth_disable));
        snackbar.setDuration(SNACKBAR_DURATION);
        snackbar.setAction(R.string.button_enable, this::startBluetoothEnableActivity)
                .show();
    }

    private void showLocationDisabledWarning() {
        Utils.errorSnackbar(rootView, getString(R.string.error_snackbar_location_disable))
                .show();
    }

    private void startNoPermissionActivity() {
        Intent intent = new Intent(this, NoPermissionActivity.class);
        startActivity(intent);
    }

    public void startBluetoothEnableActivity(View view) {
        requestWasSent = true;
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        bluetoothEnableResultLauncher.launch(enableBtIntent);
    }

    public void onFabClick(View view) {
        view.startAnimation(new AlphaAnimation(1F, 0.75F));

        if (isFlashingProcess) {
            final Intent intent = new Intent(this, DFUActivity.class);
            startActivity(intent);
        } else {
            showPatternDialog(new FobParams(
                    view.getWidth(),
                    view.getHeight(),
                    view.getX(),
                    view.getY()
            ));
        }
    }

    private boolean hasOpenedDialogs() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof DialogFragment) {
                return true;
            }
        }
        return false;
    }
    private void setProgress(Integer progress) {
        isFlashingProcess = progress > 0;
        patternFab.setProgress(progress);
        if (isFlashingProcess) {
            patternFab.setBackgroundTintList(
                    ColorStateList.valueOf(
                            getColorWrapper(R.color.green)
                    )
            );
        }
    }
    private int getColorWrapper(int id) {
        if (Version.upperMarshmallow) {
            return getColor(id);
        } else {
            //noinspection deprecation
            return getResources().getColor(id);
        }
    }
}