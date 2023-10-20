package cc.calliope.mini;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.List;

import cc.calliope.mini.databinding.ActivityMain2Binding;
import no.nordicsemi.android.kotlin.ble.core.ServerDevice;
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
        viewModel.getDevices().observe(this, new Observer<List<ServerDeviceWrapper>>() {
            @Override
            public void onChanged(List<ServerDeviceWrapper> deviceList) {
                Log.w(TAG, "_________________________________________________");
                for (ServerDeviceWrapper device : deviceList){
                    int level = Log.DEBUG;
                    boolean isFound = device.getServerDevice().getName().contains("vipep");
                    if(isFound){
                        level = Log.ASSERT;
                    }
                    Log.println(level, TAG, "Name: " + device.getServerDevice().getName() + ", address: " + device.getServerDevice().getAddress() + ", isRelevant: " + device.isRelevant());
                }
            }
        });

        binding.fab.setOnClickListener(view -> {
            clicked = !clicked;
            if(clicked) {
                Log.w(TAG, "startScan");
                viewModel.startScan();
            }else {
                Log.w(TAG, "stopScan");
                viewModel.stopScan();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}