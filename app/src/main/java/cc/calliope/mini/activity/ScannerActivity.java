package cc.calliope.mini.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import cc.calliope.mini.App;
import cc.calliope.mini.R;
import cc.calliope.mini.ExtendedBluetoothDevice;
import cc.calliope.mini.dialog.pattern.PatternDialogFragment;
import cc.calliope.mini.utils.Permission;
import cc.calliope.mini.utils.Utils;
import cc.calliope.mini.utils.Version;
import cc.calliope.mini.viewmodels.ScannerLiveData;
import cc.calliope.mini.viewmodels.ScannerViewModel;
import cc.calliope.mini.views.DimView;
import cc.calliope.mini.views.FabMenuView;
import cc.calliope.mini.views.FobParams;
import cc.calliope.mini.views.MovableFloatingActionButton;

public abstract class ScannerActivity extends AppCompatActivity implements DialogInterface.OnDismissListener {
    public static final int GRAVITY_START = 0;
    public static final int GRAVITY_END = 1;
    public static final int GRAVITY_TOP = 3;
    public static final int GRAVITY_BOTTOM = 4;
    private static final int SNACKBAR_DURATION = 10000; // how long to display the snackbar message.
    private static boolean requestWasSent = false;
    private ScannerViewModel scannerViewModel;
    private MovableFloatingActionButton patternFab;
    private ConstraintLayout rootView;
    private Animation fabOpenAnimation;
    private Animation fabCloseAnimation;
    private int screenWidth;
    private int screenHeight;
    private App app;

    ActivityResultLauncher<Intent> bluetoothEnableResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = (App) getApplication();

        fabOpenAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_open);
        fabCloseAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_close);

        scannerViewModel = new ViewModelProvider(this).get(ScannerViewModel.class);
        scannerViewModel.getScannerState().observe(this, this::scanResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void readDisplayMetrics() {
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
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
    public void onBackPressed() {
        if (patternFab != null && patternFab.isFabMenuOpen()) {
            collapseFabMenu();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        //Fragment dialog had been dismissed
//        fab.setVisibility(View.VISIBLE);
    }

    public void setContentView(ConstraintLayout view) {
        super.setContentView(view);
        this.rootView = view;
    }

    public void setPatternFab(MovableFloatingActionButton patternFab) {
        this.patternFab = patternFab;
        this.patternFab.setOnClickListener(this::onFabClick);
    }

    private void checkPermission() {
        boolean isBluetoothAccessGranted = Permission.isAccessGranted(this, Permission.BLUETOOTH_PERMISSIONS);
        boolean isLocationAccessGranted = Version.VERSION_S_AND_NEWER || Permission.isAccessGranted(this, Permission.LOCATION_PERMISSIONS);
        boolean isNotificationAccessGranted = Version.VERSION_TIRAMISU_AND_NEWER &&
                (Permission.isAccessGranted(this, Permission.POST_NOTIFICATIONS) || Permission.isAccessDeniedForever(this, Permission.POST_NOTIFICATIONS));

        if (isBluetoothAccessGranted && isLocationAccessGranted && isNotificationAccessGranted) {
            if (!Utils.isBluetoothEnabled()) {
                showBluetoothDisabledWarning();
            } else if (!Version.VERSION_S_AND_NEWER && !Utils.isLocationEnabled(this)) {
                showLocationDisabledWarning();
            }
            scannerViewModel.startScan();
        } else {
            startNoPermissionActivity();
        }
    }

    protected void scanResults(ScannerLiveData state) {
        if (hasOpenedPatternDialog()) {
            return;
        }

        if (!state.isBluetoothEnabled() && !requestWasSent) {
            showBluetoothDisabledWarning();
        }

        setDevice(state.getCurrentDevice());
    }

    protected void setDevice(ExtendedBluetoothDevice device) {
        if (patternFab != null) {
            boolean colorGreen = (device != null && device.isRelevant()) || app.getAppState() != App.APP_STATE_STANDBY;
            int color = colorGreen ? R.color.green : R.color.orange;
            patternFab.setColor(color);
        }
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
        if (patternFab.isFabMenuOpen()) {
            collapseFabMenu();
        } else {
            expandFabMenu();
        }
    }

    public void onItemFabMenuClicked(View view) {
        if (view.getId() == R.id.fabConnect) {
            if (app.getAppState() == App.APP_STATE_STANDBY) {
                showPatternDialog(new FobParams(
                        patternFab.getWidth(),
                        patternFab.getHeight(),
                        patternFab.getX(),
                        patternFab.getY()
                ));
            } else {
                final Intent intent = new Intent(this, FlashingActivity.class);
                startActivity(intent);
            }
        }
        collapseFabMenu();
    }

    private void expandFabMenu() {
        readDisplayMetrics();
        patternFab.setFabMenuOpen(true);
        DimView dimView = new DimView(this);
        dimView.setOnClickListener((View.OnClickListener) v -> collapseFabMenu());
        rootView.addView(dimView);

        ViewCompat.animate(patternFab)
                .rotation(45.0F)
                .withLayer().setDuration(300)
                .setInterpolator(new OvershootInterpolator(10.0F))
                .start();
        FabMenuView famMenuView = new FabMenuView(this, getHorizontalGravity(patternFab) == GRAVITY_START ?
                FabMenuView.TYPE_LEFT :
                FabMenuView.TYPE_RIGHT);
        customizeFabMenu(famMenuView);
        famMenuView.setOnItemClickListener(this::onItemFabMenuClicked);
        famMenuView.setLayoutParams(getParams(patternFab));
        famMenuView.startAnimation(fabOpenAnimation);
        rootView.addView(famMenuView);
    }

    public void customizeFabMenu(FabMenuView fabMenuView) {
    }

    public void collapseFabMenu() {
        if (patternFab.isFabMenuOpen()) {
            patternFab.setFabMenuOpen(false);
            View dimView = rootView.getViewById(R.id.dimView);
            rootView.removeView(dimView);

            ViewCompat.animate(patternFab)
                    .rotation(0.0F)
                    .withLayer().setDuration(300)
                    .setInterpolator(new OvershootInterpolator(10.0F))
                    .start();
            View famMenuView = rootView.getViewById(R.id.menuFab);
            famMenuView.startAnimation(fabCloseAnimation);
            rootView.removeView(famMenuView);
        }
    }

    private ConstraintLayout.LayoutParams getParams(View view) {
        int x = Math.round(view.getX());
        int y = Math.round(view.getY());
        int width = view.getWidth();
        int height = view.getHeight();

        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );

        // 1 | 2
        // -----
        // 3 | 4
        if (getHorizontalGravity(view) == GRAVITY_START && getVerticalGravity(view) == GRAVITY_TOP) { // 1
            params.startToStart = rootView.getId();
            params.topToTop = rootView.getId();
            params.setMargins(
                    x,
                    y + height,
                    0,
                    0
            );
        } else if (getHorizontalGravity(view) == GRAVITY_END && getVerticalGravity(view) == GRAVITY_TOP) { // 2
            params.endToEnd = rootView.getId();
            params.topToTop = rootView.getId();
            params.setMargins(
                    0,
                    y + height,
                    screenWidth - x - width,
                    0
            );
        } else if (getHorizontalGravity(view) == GRAVITY_START) { // 3
            params.startToStart = rootView.getId();
            params.bottomToBottom = rootView.getId();
            params.setMargins(
                    x,
                    0,
                    0,
                    screenHeight - y
            );
        } else { // 4
            params.endToEnd = rootView.getId();
            params.bottomToBottom = rootView.getId();
            params.setMargins(
                    0,
                    0,
                    screenWidth - x - width,
                    screenHeight - y
            );
        }

        return params;
    }

    private int getHorizontalGravity(View view) {
        if (Math.round(view.getX()) <= screenWidth / 2) {
            return GRAVITY_START;
        }
        return GRAVITY_END;
    }

    private int getVerticalGravity(View view) {
        if (Math.round(view.getY()) <= screenHeight / 2) {
            return GRAVITY_TOP;
        }
        return GRAVITY_BOTTOM;
    }

    private boolean hasOpenedPatternDialog() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof PatternDialogFragment) {
                return true;
            }
        }
        return false;
    }
}