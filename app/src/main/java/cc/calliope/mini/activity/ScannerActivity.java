package cc.calliope.mini.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RatingBar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import cc.calliope.mini.App;
import cc.calliope.mini.MyDeviceKt;
import cc.calliope.mini.ScanViewModelKt;
import cc.calliope.mini.dialog.pattern.PatternEnum;
import cc.calliope.mini.popup.PopupAdapter;
import cc.calliope.mini.popup.PopupItem;
import cc.calliope.mini.R;
import cc.calliope.mini.ExtendedBluetoothDevice;
import cc.calliope.mini.dialog.pattern.PatternDialogFragment;
import cc.calliope.mini.utils.Permission;
import cc.calliope.mini.utils.Utils;
import cc.calliope.mini.utils.Version;
import cc.calliope.mini.viewmodels.ScannerLiveData;
import cc.calliope.mini.viewmodels.ScannerViewModel;
import cc.calliope.mini.views.FobParams;
import cc.calliope.mini.views.MovableFloatingActionButton;
import no.nordicsemi.android.kotlin.ble.core.scanner.BleScanResults;

public abstract class ScannerActivity extends AppCompatActivity implements DialogInterface.OnDismissListener {
    private static final int SNACKBAR_DURATION = 10000; // how long to display the snackbar message.
    private static boolean requestWasSent = false;
//    private ScannerViewModel scannerViewModel;
    private ScanViewModelKt viewModel;
    private MovableFloatingActionButton patternFab;
    private ConstraintLayout rootView;
    private int screenWidth;
    private int screenHeight;
    private App app;
    private PopupWindow popupWindow;
    private int popupMenuWidth;
    private int popupMenuHeight;

    ActivityResultLauncher<Intent> bluetoothEnableResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = (App) getApplication();

//        scannerViewModel = new ViewModelProvider(this).get(ScannerViewModel.class);
//        scannerViewModel.getScannerState().observe(this, this::scanResults);

        viewModel = new ViewModelProvider(this).get(ScanViewModelKt.class);
        viewModel.getDevices().observe(this, new Observer<List<BleScanResults>>() {
            @Override
            public void onChanged(List<BleScanResults> scanResults) {
//                Log.w(TAG, "_________________________________________________");
                for (BleScanResults results : scanResults) {
                    MyDeviceKt device = new MyDeviceKt(results);

                    if (!device.getPattern().isEmpty() && matchesPattern("51422", device.getPattern())) {
                        int level = device.isActual() ? Log.DEBUG : Log.ASSERT;

                        Log.println(level, "scannerViewModel",
                                "address: " + device.getAddress() + ", " +
                                "pattern: " + device.getPattern() + ", " +
                                "numPattern: " + device.getNumPattern() + ", " +
                                "bonded: " + device.isBonded() + ", " +
                                "actual: " + device.isActual());
                    }
                }
            }
        });
    }

    public void onPatternChange(int column, float value){
        Utils.log(Log.ASSERT, "BAR", "Column " + column + ": " + value);
    }

    private static boolean matchesPattern(String numberPattern, String letterPattern) {
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        requestWasSent = false;
        checkPermission();
        readDisplayMetrics();
    }

    @Override
    public void onPause() {
        super.onPause();
//        scannerViewModel.stopScan();
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        //Fragment dialog had been dismissed
//        fab.setVisibility(View.VISIBLE);
    }

    private void readDisplayMetrics() {
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
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
        boolean isNotificationAccessGranted = !Version.VERSION_TIRAMISU_AND_NEWER || Permission.isAccessGranted(this, Permission.POST_NOTIFICATIONS) ||
                Permission.isAccessDeniedForever(this, Permission.POST_NOTIFICATIONS);

        if (isBluetoothAccessGranted && isLocationAccessGranted && isNotificationAccessGranted) {
            if (!Utils.isBluetoothEnabled()) {
                showBluetoothDisabledWarning();
            } else if (!Version.VERSION_S_AND_NEWER && !Utils.isLocationEnabled(this)) {
                showLocationDisabledWarning();
            }
//            scannerViewModel.startScan();
            viewModel.startScan();
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
//        scannerViewModel.startScan(); // On older devices, "auto-start" scanning does not work after bluetooth is turned on.

        FragmentManager fragmentManager = getSupportFragmentManager();
        PatternDialogFragment dialogFragment = PatternDialogFragment.newInstance(params);
        dialogFragment.show(fragmentManager, "fragment_pattern");
    }

    private void showBluetoothDisabledWarning() {
        Utils.errorSnackbar(rootView, getString(R.string.error_snackbar_bluetooth_disable))
                .setDuration(SNACKBAR_DURATION)
                .setAction(R.string.button_enable, this::startBluetoothEnableActivity)
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
        createPopupMenu(view);
        showPopupMenu(view);
    }

    private void createPopupMenu(View view) {
        List<PopupItem> popupItems = new ArrayList<>();
        addPopupMenuItems(popupItems);

        final ListView listView = new ListView(this);
        listView.setAdapter(new PopupAdapter(this,
                (Math.round(view.getX()) <= screenWidth / 2) ? PopupAdapter.TYPE_START : PopupAdapter.TYPE_END,
                popupItems)
        );
        listView.setDivider(null);
        listView.setOnItemClickListener(this::onPopupMenuItemClick);

        //get max item measured width
        popupMenuHeight = 0;
        for (int i = 0; i < listView.getAdapter().getCount(); i++) {
            View listItem = listView.getAdapter().getView(i, null, listView);
            listItem.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int width = listItem.getMeasuredWidth();
            if (width > popupMenuWidth) {
                popupMenuWidth = width;
            }
            popupMenuHeight += listItem.getMeasuredHeight();
        }

        popupWindow = new PopupWindow(listView, popupMenuWidth, WindowManager.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOnDismissListener(() -> onDismissPopupMenu(view));
    }

    public void addPopupMenuItems(List<PopupItem> popupItems) {
        popupItems.add(new PopupItem(R.string.menu_fab_connect, R.drawable.ic_connect));
    }

    public void onPopupMenuItemClick(AdapterView<?> parent, View view, int position, long id) {
        Utils.log(Log.ASSERT, "SA", "position: " + position);
        popupWindow.dismiss();
        if (position == 0) {
            if (app.getAppState() == App.APP_STATE_STANDBY) {
                showPatternDialog(new FobParams(
                        patternFab.getWidth(),
                        patternFab.getHeight(),
                        patternFab.getX(),
                        patternFab.getY()
                ));
            } else {
                startFlashingActivity();
            }
        }
    }

    private void showPopupMenu(View view) {
        Offset offset = getOffset(view);
        popupWindow.showAsDropDown(view, offset.getX(), offset.getY());
        dimBackground(0.5f);  // затемнюємо фон до 50%
        ViewCompat.animate(view)
                .rotation(45.0F)
                .withLayer().setDuration(300)
                .setInterpolator(new OvershootInterpolator(10.0F))
                .start();
    }

    private void onDismissPopupMenu(View view) {
        dimBackground(1.0f);
        ViewCompat.animate(view)
                .rotation(0.0F)
                .withLayer().setDuration(300)
                .setInterpolator(new OvershootInterpolator(10.0F))
                .start();
    }

    private void dimBackground(float dimAmount) {
        Window window = getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.alpha = dimAmount;
        window.setAttributes(layoutParams);
    }

    private class Offset {
        private final int x;
        private final int y;

        public Offset(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        @NonNull
        @Override
        public String toString() {
            return "Offset{" +
                    "x=" + x +
                    ", y=" + y +
                    '}';
        }
    }

    private Offset getOffset(View view) {
        int x;
        int y;

        if (Math.round(view.getX()) <= screenWidth / 2) {
            x = Utils.convertDpToPixel(this, 8);
        } else {
            x = (Utils.convertDpToPixel(this, 8) - view.getWidth() + popupMenuWidth) * -1;
        }

        if (Math.round(view.getY()) <= screenHeight / 2) {
            y = Utils.convertDpToPixel(this, 4);
        } else {
            y = (Utils.convertDpToPixel(this, 4) + view.getHeight() + popupMenuHeight) * -1;
        }

        return new Offset(x, y);
    }

    private void startFlashingActivity() {
        final Intent intent = new Intent(this, FlashingActivity.class);
        startActivity(intent);
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