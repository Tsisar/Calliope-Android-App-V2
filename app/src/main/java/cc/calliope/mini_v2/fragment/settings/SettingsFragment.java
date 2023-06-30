package cc.calliope.mini_v2.fragment.settings;

import android.os.Bundle;
import android.view.View;

import androidx.preference.PreferenceFragmentCompat;
import cc.calliope.mini_v2.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}