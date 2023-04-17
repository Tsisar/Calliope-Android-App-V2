package cc.calliope.mini_v2;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
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
    private ActivityMainBinding binding;
    private Boolean isFullScreen = false;

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

        int color = ContextCompat.getColor(activity, R.color.white);
        int margin = Utils.convertDpToPixel(-8, activity);
        ColorStateList tint = ColorStateList.valueOf(color);

        ViewGroup.LayoutParams params = view.getLayoutParams();
//        params.setMarginEnd(margin);

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
    }

    @Override
    public void onFabClick(View view){
        addFab(view).setOnClickListener(this::removeFab);
    }
}