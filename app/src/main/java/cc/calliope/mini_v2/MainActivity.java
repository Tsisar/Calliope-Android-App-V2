package cc.calliope.mini_v2;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import cc.calliope.mini_v2.databinding.ActivityMainBinding;

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
}