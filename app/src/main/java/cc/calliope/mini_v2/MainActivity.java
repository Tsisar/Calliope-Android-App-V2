package cc.calliope.mini_v2;

import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import cc.calliope.mini_v2.databinding.ActivityMainBinding;
import cc.calliope.mini_v2.utils.Utils;
import cc.calliope.mini_v2.views.FabMenuItemView;

public class MainActivity extends ScannerActivity {
    public static final int GRAVITY_START = 0;
    public static final int GRAVITY_END = 1;
    public static final int GRAVITY_TOP = 3;
    public static final int GRAVITY_BOTTOM = 4;
    private static final int MARGIN = 4; //dp
    private ActivityMainBinding binding;
    private Boolean isFullScreen = false;
    private int createdFob = 0;
    private int screenWidth;
    private int screenHeight;

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

        setPatternFab(binding.patternFab);

        NavController navController = Navigation.findNavController(this, R.id.navigation_host_fragment);
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

        ImageButton fullScreenButton = binding.fullscreenButton;
        if (fullScreenButton != null) {
            fullScreenButton.setOnClickListener(this::enableFullScreenMode);
        }
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
    public void onPause() {
        super.onPause();
        disableFullScreenMode();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    private void enableFullScreenMode(View view) {
        isFullScreen = true;
        binding.bottomNavigation.setVisibility(View.GONE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void disableFullScreenMode() {
        isFullScreen = false;
        binding.bottomNavigation.setVisibility(View.VISIBLE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void switchFullScreenMode(View view){
        if(isFullScreen){
            disableFullScreenMode();
        }else {
            enableFullScreenMode(view);
        }
    }

    @Override
    public void onFabClick(View fab) {
        if(createdFob == 0) {
            ViewCompat.animate(binding.patternFab).rotation(45.0F).withLayer().setDuration(300).setInterpolator(new OvershootInterpolator(10.0F)).start();
            FabMenuItemView connect = addFabMenuItem(fab, R.drawable.ic_connect, "Connect");
            FabMenuItemView scripts = addFabMenuItem(fab, R.drawable.ic_home_black_24dp, "Scripts");
            FabMenuItemView fullScreen = addFabMenuItem(fab, isFullScreen ? R.drawable.ic_disable_full_screen_24dp : R.drawable.ic_enable_full_screen_24dp, "Full screen");
            connect.setOnItemClickListener(view -> {
                MainActivity.super.onFabClick(fab);
                removeView(connect, scripts, fullScreen);
            });
            scripts.setOnItemClickListener(new FabMenuItemView.OnItemClickListener() {
                @Override
                public void onItemClick(FabMenuItemView view) {
                    removeView(connect, scripts, fullScreen);
                }
            });
            fullScreen.setOnItemClickListener(view -> {
                switchFullScreenMode(view);
                removeView(connect, scripts, fullScreen);
            });
            //TODO loop?
            fab.setOnClickListener(v -> {
                v.setOnClickListener(this::onFabClick);
                removeView(connect, scripts, fullScreen);
            });
        }
    }

    private FabMenuItemView addFabMenuItem(View view, int imageResource, String title) {
        FabMenuItemView itemView = new FabMenuItemView(this,
                getHorizontalGravity(view) == GRAVITY_START ? FabMenuItemView.TYPE_LEFT : FabMenuItemView.TYPE_RIGHT,
                imageResource,
                title
        );
        itemView.setLayoutParams(getParams(view));
        binding.getRoot().addView(itemView);
        createdFob++;
        return itemView;
    }

    private void removeView(View... views) {
        ViewCompat.animate(binding.patternFab).rotation(0.0F).withLayer().setDuration(300).setInterpolator(new OvershootInterpolator(10.0F)).start();
        for (View view : views) {
            binding.getRoot().removeView(view);
        }
        createdFob = 0;
    }

    private ConstraintLayout.LayoutParams getParams(View mainFab) {
        int mainX = Math.round(mainFab.getX());
        int mainY = Math.round(mainFab.getY());
        int mainWidth = mainFab.getWidth();
        int mainHeight = mainFab.getHeight();
        int margin = Utils.convertDpToPixel(this, MARGIN);

        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );

        // 1 | 2
        // -----
        // 3 | 4
        if (getHorizontalGravity(mainFab) == GRAVITY_START && getVerticalGravity(mainFab) == GRAVITY_TOP) { // 1
            params.startToStart = binding.getRoot().getId();
            params.topToTop = binding.getRoot().getId();
            params.setMargins(
                    mainX + margin,
                    mainY + mainHeight + (mainHeight - margin) * createdFob,
                    0,
                    0
            );
        } else if (getHorizontalGravity(mainFab) == GRAVITY_END && getVerticalGravity(mainFab) == GRAVITY_TOP) { // 2
            params.endToEnd = binding.getRoot().getId();
            params.topToTop = binding.getRoot().getId();
            params.setMargins(
                    0,
                    mainY + mainHeight + (mainHeight - margin) * createdFob,
                    screenWidth - mainX - mainWidth + margin,
                    0
            );
        } else if (getHorizontalGravity(mainFab) == GRAVITY_START) { // 3
            params.startToStart = binding.getRoot().getId();
            params.bottomToBottom = binding.getRoot().getId();
            params.setMargins(
                    mainX + margin,
                    0,
                    0,
                    screenHeight - mainY + (mainHeight - margin) * createdFob
            );
        } else { // 4
            params.endToEnd = binding.getRoot().getId();
            params.bottomToBottom = binding.getRoot().getId();
            params.setMargins(
                    0,
                    0,
                    screenWidth - mainX - mainWidth + margin,
                    screenHeight - mainY + (mainHeight - margin) * createdFob
            );
        }

        Log.v("PARAMS", "screenWidth: " + screenWidth);
        Log.v("PARAMS", "screenHeight: " + screenHeight);
        Log.v("PARAMS", "mainX: " + mainX);
        Log.v("PARAMS", "mainY: " + mainY);
        Log.v("PARAMS", "mainWidth: " + mainWidth);
        Log.v("PARAMS", "mainHeight: " + mainHeight);
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