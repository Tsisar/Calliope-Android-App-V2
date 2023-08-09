package cc.calliope.mini.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import cc.calliope.mini.R;
import cc.calliope.mini.databinding.ActivityMainBinding;
import cc.calliope.mini.dialog.scripts.ScriptsFragment;
import cc.calliope.mini.utils.Utils;
import cc.calliope.mini.utils.Version;
import cc.calliope.mini.views.FabMenuView;

public class MainActivity extends ScannerActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private boolean fullScreen = false;
    private boolean currentWeb = false;
    private final ActivityResultLauncher<String> pushNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Utils.log(Log.INFO, TAG, "NotificationPermission is Granted");
                } else {
                    Utils.log(Log.WARN, TAG, "NotificationPermission NOT Granted");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setPatternFab(binding.patternFab);

        NavController navController = Navigation.findNavController(this, R.id.navigation_host_fragment);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            currentWeb = destination.getId() == R.id.navigation_web;
            if (currentWeb) {
                binding.bottomNavigation.setVisibility(View.GONE);
            } else {
                binding.bottomNavigation.setVisibility(View.VISIBLE);
            }
            Utils.log(Log.ASSERT, TAG, "Destination id: " + destination.getId());
            Utils.log(Log.ASSERT, TAG, "Select item id: " + binding.bottomNavigation.getSelectedItemId());

            if (binding.patternFab.isFabMenuOpen()) {
                collapseFabMenu();
            }
        });
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

        if(Version.upperTiramisu) {
            requestPushNotificationPermission();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        disableFullScreenMode();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override
    public void onBackPressed() {
        if (fullScreen) {
            disableFullScreenMode();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onItemFabMenuClicked(View view) {
        super.onItemFabMenuClicked(view);
        if (view.getId() == R.id.fabFullScreen) {
            if (fullScreen) {
                disableFullScreenMode();
            } else {
                enableFullScreenMode();
            }
        } else if (view.getId() == R.id.fabScripts) {
            ScriptsFragment scriptsFragment = new ScriptsFragment();
            scriptsFragment.show(getSupportFragmentManager(), "Bottom Sheet Dialog Fragment");
        }
    }

    private void enableFullScreenMode() {
        fullScreen = true;
        binding.bottomNavigation.setVisibility(View.GONE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void disableFullScreenMode() {
        fullScreen = false;
        binding.bottomNavigation.setVisibility(View.VISIBLE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public void customizeFabMenu(FabMenuView fabMenuView) {
        fabMenuView.setScriptsVisibility(View.VISIBLE);
        fabMenuView.setFullScreenVisibility(View.VISIBLE);
        fabMenuView.setFullScreenImageResource(fullScreen ?
                R.drawable.ic_disable_full_screen_24dp :
                R.drawable.ic_enable_full_screen_24dp);
    }

    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Utils.log(Log.WARN, TAG, "onConfigurationChanged");
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Utils.log(Log.WARN, TAG, "ORIENTATION_LANDSCAPE");

        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Utils.log(Log.WARN, TAG, "ORIENTATION_PORTRAIT");
        }
        if (!currentWeb) {
            recreate();
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void requestPushNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            pushNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }
}