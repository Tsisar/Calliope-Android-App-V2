package cc.calliope.mini;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.SystemClock;
import android.util.Log;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.List;

import cc.calliope.mini.activity.SettingsActivity;
import cc.calliope.mini.databinding.ActivityMain2Binding;
import cc.calliope.mini.utils.Version;
import no.nordicsemi.android.kotlin.ble.core.ServerDevice;
import no.nordicsemi.android.kotlin.ble.core.scanner.BleScanResultData;
import no.nordicsemi.android.kotlin.ble.core.scanner.BleScanResults;

public class Main2Activity extends AppCompatActivity {
    private static final String TAG = "MAIN2";
    private AppBarConfiguration appBarConfiguration;
    private ActivityMain2Binding binding;
    private ScanViewModelKt viewModel;
    private boolean clicked = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMain2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        viewModel = new ViewModelProvider(this).get(ScanViewModelKt.class);
        viewModel.getDevices().observe(this, new Observer<List<BleScanResults>>() {
            @Override
            public void onChanged(List<BleScanResults> scanResults) {
//                Log.w(TAG, "_________________________________________________");
                for (BleScanResults results : scanResults) {
                    MyDeviceKt device = new MyDeviceKt(results);

                    if (device.getName().contains("tupov")) {
                        int level = device.isActual() ? Log.DEBUG : Log.ASSERT;

                        Log.println(level, TAG, "Name: " + device.getName() + ", " +
                                "address: " + device.getAddress() + ", " +
                                "actual: " + device.isActual());
                    }
                }
            }
        });

        binding.fab.setOnClickListener(view -> {
            final Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
//            clicked = !clicked;
//            if (clicked) {
//                Log.w(TAG, "startScan");
//                viewModel.startScan();
//            } else {
//                Log.w(TAG, "stopScan");
//                viewModel.stopScan();
//            }
        });

        viewModel.startScan();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}