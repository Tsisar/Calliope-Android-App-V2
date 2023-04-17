package cc.calliope.mini_v2;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import cc.calliope.mini_v2.databinding.ActivityMainBinding;
import cc.calliope.mini_v2.utils.Utils;

public class MainActivity extends ScannerActivity {
    private static final int FAB_SIZE_NORMAL = 64;
    private static final int FAB_SIZE_MINI = 52;
    private ActivityMainBinding binding;
    private Boolean isFullScreen = false;
    private int createdFob = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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

    private FloatingActionButton addFab(View view) {
        Activity activity = this;

        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;

        int x = Math.round(view.getX());
        int y = Math.round(view.getY());
        int color = ContextCompat.getColor(activity, R.color.white);
        int marginTopDp = y < screenHeight / 2 ?
                FAB_SIZE_NORMAL + FAB_SIZE_MINI * createdFob :
                -FAB_SIZE_MINI * (createdFob + 1);
        int marginTop = Utils.convertDpToPixel(marginTopDp, activity);
        int marginStart = Utils.convertDpToPixel(6, activity);
        ColorStateList tint = ColorStateList.valueOf(color);

        createdFob++;

        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );

        params.startToStart = binding.getRoot().getId();
        params.topToTop = binding.getRoot().getId();

        params.setMargins(x + marginStart, y + marginTop, 0, 0);

        FloatingActionButton fab = new FloatingActionButton(activity);
        fab.setImageResource(R.drawable.ic_edit_24);
        fab.setSize(FloatingActionButton.SIZE_MINI);
        fab.setImageTintList(tint);
        fab.setLayoutParams(params);
        binding.getRoot().addView(fab);
        return fab;
    }


    private void removeFab(View view) {
        binding.getRoot().removeView(view);
        createdFob--;
    }

    private void removeAllFab(View... views) {
        for (View view : views) {
            binding.getRoot().removeView(view);
        }
        createdFob = 0;
    }

    @Override
    public void onFabClick(View view) {
        addFab(view).setOnClickListener(this::removeFab);
    }
}