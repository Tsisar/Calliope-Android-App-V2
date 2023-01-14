package cc.calliope.mini_v2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
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
import cc.calliope.mini_v2.views.MovableFloatingActionButton;


public class MainActivity extends AppCompatActivity implements DialogInterface.OnDismissListener {
    private static final int REQUEST_CODE = 1022; // random number
    private static final int SNACKBAR_DURATION = 10000; // how long to display the snackbar message.
    private static final String CHANNEL_ID = "145";
    private static boolean requestWasSent = false;

    private ActivityMainBinding binding;
    private ScannerViewModel scannerViewModel;

    private View rootView;
    private MovableFloatingActionButton patternFab;
    private ConstraintLayout mainLayout;
    private LinearLayout noPermissionLayout;
    private Button actionButton;
    private Button settingsButton;

    private Boolean gone = false;

    BroadcastReceiver broadcastReceiver;

    ActivityResultLauncher<Intent> bluetoothEnableResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    if (checkPermission()) {
                        scannerViewModel.startScan();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavController navController = Navigation.findNavController(this, R.id.navigation_host_fragment);
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

        scannerViewModel = new ViewModelProvider(this).get(ScannerViewModel.class);
        scannerViewModel.getScannerState().observe(this, this::scanResults);

        getViews();

        patternFab.setOnClickListener(this::onFabClick);
        actionButton.setOnClickListener(v -> requestPermissions());
        settingsButton.setOnClickListener(v -> requestAppSettings());

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                manager.cancelAll();

                String state = intent.getAction();
                if (state.equals("Yes")) {
                    fullScreen();
                }
            }
        };

        fsNotify();
    }

    @Override
    public void onBackPressed() {
        if(gone){
            fullScreen();
        }else {
            super.onBackPressed();
        }
    }

    private void fsNotify() {
        createNotificationChannel();

        Intent intent = new Intent(); //same
        intent.setAction("Yes");
        intent.putExtra("RES", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent1 = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_connect)
                .setContentTitle("My notification")
                .setContentText("Much longer text that cannot fit one line...")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(R.drawable.ic_device_black_24dp, "Yes", pendingIntent1);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(125, builder.build());

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("Yes");

        registerReceiver(broadcastReceiver, intentFilter);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "channel_name";
            String description = "channel_description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void getViews() {
        rootView = binding.getRoot();
        patternFab = binding.patternFab;
        actionButton = binding.noPermissionLayout.actionButton;
        settingsButton = binding.noPermissionLayout.settingsButton;
        mainLayout = binding.mainLayout;
        noPermissionLayout = binding.noPermissionLayout.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        requestWasSent = false;

        if (checkPermission()) {
            if (!Utils.isBluetoothEnabled()) {
                showBluetoothDisabledWarning();
            } else if (!Version.upperSnowCone && !Utils.isLocationEnabled(this)) {
                showLocationDisabledWarning();
            }
            scannerViewModel.startScan();
        }
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
//        fab.setVisibility(View.VISIBLE);
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

    private void fullScreen() {
        if (gone) {
            binding.bottomNavigation.setVisibility(View.VISIBLE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            binding.bottomNavigation.setVisibility(View.GONE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        gone = !gone;
    }

    private int getColorWrapper(int id) {
        if (Version.upperMarshmallow) {
            return super.getColor(id);
        } else {
            //noinspection deprecation
            return getResources().getColor(id);
        }
    }

    private void scanResults(final ScannerLiveData state) {
//        Log.v("SCANNER", "current device: " + state.getCurrentDevice());

        if (hasOpenedDialogs())
            return;

        if (!state.isBluetoothEnabled() && !requestWasSent) {
            showBluetoothDisabledWarning();
        }

        ExtendedBluetoothDevice device = state.getCurrentDevice();
        int color = getColorWrapper(device != null && device.isRelevant() ? R.color.green : R.color.orange);
        patternFab.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    private boolean checkPermission() {
        boolean isBluetoothAccessGranted = Permission.isAccessGranted(this, Permission.BLUETOOTH);
        boolean isLocationAccessGranted = Version.upperSnowCone || Permission.isAccessGranted(this, Permission.LOCATION);

        if (isBluetoothAccessGranted && isLocationAccessGranted) {
            showContent();
            return true;
        } else {
            showInfoNoPermission(isBluetoothAccessGranted ? Permission.LOCATION : Permission.BLUETOOTH);
            return false;
        }
    }

    private void showBluetoothDisabledWarning() {
        Snackbar snackbar = Utils.errorSnackbar(rootView, "Bluetooth is disable");
        snackbar.setDuration(SNACKBAR_DURATION);
        snackbar.setAction("Enable", this::openBluetoothEnableActivity)
                .show();
    }

    private void showLocationDisabledWarning() {
        Utils.errorSnackbar(rootView, "Location is disable")
                .show();
    }

    private void showContent() {
        mainLayout.setVisibility(View.VISIBLE);
        noPermissionLayout.setVisibility(View.GONE);
    }

    private void showInfoNoPermission(@Permission.RequestType int requestType) {
        ContentNoPermission content = ContentNoPermission.getContent(requestType);
        boolean deniedForever = Permission.isAccessDeniedForever(this, requestType);

        mainLayout.setVisibility(View.GONE);
        noPermissionLayout.setVisibility(View.VISIBLE);

        binding.noPermissionLayout.iconImageView.setImageResource(content.getIcResId());
        binding.noPermissionLayout.titleTextView.setText(content.getTitleResId());
        binding.noPermissionLayout.messageTextView.setText(content.getMessageResId());

        actionButton.setVisibility(deniedForever ? View.GONE : View.VISIBLE);
        settingsButton.setVisibility(deniedForever ? View.VISIBLE : View.GONE);
    }

    public void openBluetoothEnableActivity(View view) {
        requestWasSent = true;
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        bluetoothEnableResultLauncher.launch(enableBtIntent);
    }

    private void showPatternDialog(FobParams params) {
//        patternFab.setBackgroundTintList(ColorStateList.valueOf(getColorWrapper(R.color.orange)));
        scannerViewModel.startScan(); // On older devices, "auto-start" scanning does not work after bluetooth is turned on.

        FragmentManager fragmentManager = getSupportFragmentManager();
        PatternDialogFragment dialogFragment = PatternDialogFragment.newInstance(params);
        dialogFragment.show(fragmentManager, "fragment_pattern");
//        fab.setVisibility(View.GONE);
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

    private boolean hasOpenedDialogs() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof DialogFragment) {
                return true;
            }
        }
        return false;
    }
}