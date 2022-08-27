package cc.calliope.mini_v2;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import cc.calliope.mini_v2.databinding.ActivityMainBinding;
import cc.calliope.mini_v2.ui.dialog.PatternDialogFragment;
import cc.calliope.mini_v2.utils.Utils;
import cc.calliope.mini_v2.viewmodels.DeviceViewModel;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            Log.e("UTILS", "isBluetoothScanPermissionsGranted: " + Utils.isBluetoothScanPermissionsGranted(this));
        }else{
            Log.e("UTILS", "isLocationPermissionsGranted: " + Utils.isLocationPermissionsGranted(this));
            Log.e("UTILS", "isBluetoothAdminPermissionsGranted: " + Utils.isBluetoothAdminPermissionsGranted(this));
        }

        Log.e("UTILS", "isBluetoothEnabled: " + Utils.isBluetoothEnabled());
        Log.e("UTILS", "isLocationEnabled: " + Utils.isLocationEnabled(this));
        Log.e("UTILS", "isNetworkConnected: " + Utils.isNetworkConnected(this));
        Log.e("UTILS", "isInternetAvailable: " + Utils.isInternetAvailable());


        DeviceViewModel viewModel = new ViewModelProvider(this).get(DeviceViewModel.class);
        viewModel.getDevice().observe(this, device -> {
            // Perform an action with the latest item data
            Log.e("TEST_DEVICE", device.getPattern());
        });

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_coding, R.id.navigation_help)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        binding.fab.setOnClickListener(view -> {
            showPatternDialog();
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        });
    }

    private void showPatternDialog() {
        FragmentManager parentFragmentManager = getSupportFragmentManager();
//        parentFragmentManager.setFragmentResultListener("pattern_request_key", this, (requestKey, bundle) -> {
//            ExtendedBluetoothDevice device = bundle.getParcelable("pattern_key");
//            Log.e("DEVICE", device.getAddress() + " " + device.getPattern());
//        });
        PatternDialogFragment patternDialogFragment = PatternDialogFragment.newInstance("Some Pattern");

//        Bundle bundle = new Bundle();
//        bundle.putString("TEXT","TEST TEXT");
//        patternDialogFragment.setArguments(bundle);

        patternDialogFragment.show(parentFragmentManager, "fragment_pattern");
    }
}