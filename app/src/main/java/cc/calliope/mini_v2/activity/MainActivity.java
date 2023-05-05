package cc.calliope.mini_v2.activity;

import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.databinding.ActivityMainBinding;
import cc.calliope.mini_v2.dialog.scripts.ScriptsFragment;
import cc.calliope.mini_v2.views.DimView;
import cc.calliope.mini_v2.views.FabMenuView;
import cc.calliope.mini_v2.views.MovableFloatingActionButton;

public class MainActivity extends ScannerActivity {
    private ActivityMainBinding binding;
    private Boolean isFullScreen = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setPatternFab(binding.patternFab);

        NavController navController = Navigation.findNavController(this, R.id.navigation_host_fragment);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (binding.patternFab.isFabMenuOpen()) {
                collapseFabMenu();
            }
        });
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
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
        if (isFullScreen) {
            disableFullScreenMode();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onItemFabMenuClicked(View view) {
        super.onItemFabMenuClicked(view);
        if (view.getId() == R.id.fabFullScreen) {
            if (isFullScreen) {
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
        isFullScreen = true;
        binding.bottomNavigation.setVisibility(View.GONE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void disableFullScreenMode() {
        isFullScreen = false;
        binding.bottomNavigation.setVisibility(View.VISIBLE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public void customizeFabMenu(FabMenuView fabMenuView) {
        fabMenuView.setScriptsVisibility(View.VISIBLE);
        fabMenuView.setFullScreenVisibility(View.VISIBLE);
        fabMenuView.setFullScreenImageResource(isFullScreen ?
                R.drawable.ic_disable_full_screen_24dp :
                R.drawable.ic_enable_full_screen_24dp);
    }
}