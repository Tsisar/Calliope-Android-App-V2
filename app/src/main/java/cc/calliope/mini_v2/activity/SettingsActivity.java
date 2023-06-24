package cc.calliope.mini_v2.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.fragment.settings.SettingsFragment;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, new SettingsFragment())
                .commit();
    }
}