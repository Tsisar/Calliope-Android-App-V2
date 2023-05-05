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
    public static final int GRAVITY_START = 0;
    public static final int GRAVITY_END = 1;
    public static final int GRAVITY_TOP = 3;
    public static final int GRAVITY_BOTTOM = 4;
    private ActivityMainBinding binding;
    private Boolean isFullScreen = false;
    private int screenWidth;
    private int screenHeight;
    private Animation fabOpenAnimation;
    private Animation fabCloseAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;

        fabOpenAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_open);
        fabCloseAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_close);

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
    public void onBackPressed() {
        if (isFullScreen) {
            disableFullScreenMode();
        } else if (binding.patternFab.isFabMenuOpen()) {
            collapseFabMenu();
        } else {
            super.onBackPressed();
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
    public void onFabClick(View view) {
        if (binding.patternFab.isFabMenuOpen()) {
            collapseFabMenu();
        } else {
            expandFabMenu();
        }
    }

    private void onItemFabMenuClicked(View view) {
        if (view.getId() == R.id.fabConnect) {
            super.onFabClick(binding.patternFab);
        } else if (view.getId() == R.id.fabScripts) {
            ScriptsFragment scriptsFragment = new ScriptsFragment();
            scriptsFragment.show(getSupportFragmentManager(), "Bottom Sheet Dialog Fragment");
//            navController.navigate(R.id.navigation_scripts);
        } else if (view.getId() == R.id.fabFullScreen) {
            if (isFullScreen) {
                disableFullScreenMode();
            } else {
                enableFullScreenMode();
            }
        }
        Log.v("PARAMS", "itemId: " + view.getId());
        Log.v("PARAMS", "-----------------------------------------------");
        collapseFabMenu();
    }

    private void expandFabMenu() {
        MovableFloatingActionButton fab = binding.patternFab;

        fab.setFabMenuOpen(true);
        DimView dimView = new DimView(this);
        dimView.setOnClickListener((View.OnClickListener) v -> collapseFabMenu());
        binding.getRoot().addView(dimView);

        ViewCompat.animate(fab)
                .rotation(45.0F)
                .withLayer().setDuration(300)
                .setInterpolator(new OvershootInterpolator(10.0F))
                .start();
        FabMenuView famMenuView = new FabMenuView(this, getHorizontalGravity(fab) == GRAVITY_START ?
                FabMenuView.TYPE_LEFT :
                FabMenuView.TYPE_RIGHT);
        famMenuView.setFullScreenImageResource(isFullScreen ?
                R.drawable.ic_disable_full_screen_24dp :
                R.drawable.ic_enable_full_screen_24dp);
        famMenuView.setOnItemClickListener(this::onItemFabMenuClicked);
        famMenuView.setLayoutParams(getParams(fab));
        famMenuView.startAnimation(fabOpenAnimation);
        binding.getRoot().addView(famMenuView);
    }

    private void collapseFabMenu() {
        if (binding.patternFab.isFabMenuOpen()) {
            binding.patternFab.setFabMenuOpen(false);
            View dimView = binding.getRoot().getViewById(R.id.dimView);
            binding.getRoot().removeView(dimView);

            ViewCompat.animate(binding.patternFab)
                    .rotation(0.0F)
                    .withLayer().setDuration(300)
                    .setInterpolator(new OvershootInterpolator(10.0F))
                    .start();
            View famMenuView = binding.getRoot().getViewById(R.id.menuFab);
            famMenuView.startAnimation(fabCloseAnimation);
            binding.getRoot().removeView(famMenuView);
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
            params.startToStart = binding.getRoot().getId();
            params.topToTop = binding.getRoot().getId();
            params.setMargins(
                    x,
                    y + height,
                    0,
                    0
            );
        } else if (getHorizontalGravity(view) == GRAVITY_END && getVerticalGravity(view) == GRAVITY_TOP) { // 2
            params.endToEnd = binding.getRoot().getId();
            params.topToTop = binding.getRoot().getId();
            params.setMargins(
                    0,
                    y + height,
                    screenWidth - x - width,
                    0
            );
        } else if (getHorizontalGravity(view) == GRAVITY_START) { // 3
            params.startToStart = binding.getRoot().getId();
            params.bottomToBottom = binding.getRoot().getId();
            params.setMargins(
                    x,
                    0,
                    0,
                    screenHeight - y
            );
        } else { // 4
            params.endToEnd = binding.getRoot().getId();
            params.bottomToBottom = binding.getRoot().getId();
            params.setMargins(
                    0,
                    0,
                    screenWidth - x - width,
                    screenHeight - y
            );
        }

        Log.v("PARAMS", "screenWidth: " + screenWidth);
        Log.v("PARAMS", "screenHeight: " + screenHeight);
        Log.v("PARAMS", "mainX: " + x);
        Log.v("PARAMS", "mainY: " + y);
        Log.v("PARAMS", "mainWidth: " + width);
        Log.v("PARAMS", "mainHeight: " + height);
        Log.v("PARAMS", "Margins left: " + params.leftMargin + "; top: " + params.topMargin + "; right: " + params.rightMargin + "; bottom:" + params.bottomMargin + ";");
        Log.v("PARAMS", "-----------------------------------------------");
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
}