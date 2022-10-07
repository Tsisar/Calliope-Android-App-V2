package cc.calliope.mini_v2;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import cc.calliope.mini_v2.databinding.FragmentConnectPageBinding;

public class ConnectPageFragment extends Fragment {
    private static final String LOG_TAG = "AndroidExample";

    private ConnectInfo connectInfo;

    private ImageView illustration;
    private TextView description;

    private FragmentConnectPageBinding binding;

    // IMPORTANT:
    // Required default public constructor.
    // If configuration change.
    // For example: User rotate the Phone,
    //  Android will create new Fragment (ConnectInfoPageFragment) via default Constructor
    //  so this.connectInfo will be null.
    public ConnectPageFragment() {

    }

    public ConnectPageFragment(ConnectInfo connectInfo) {
        this.connectInfo = connectInfo;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentConnectPageBinding.inflate(inflater, container, false);

//        binding.getRoot().setBackgroundColor(getResources().getColor(R.color.aqua_200));

        illustration = binding.illustrationImageView;
        description = binding.descriptionTextView;

        return binding.getRoot();
    }


    // Called when configuration change.
    // For example: User rotate the Phone,
    // Android will create new Fragment (EmployeePageFragment) object via default Constructor
    // so this.employee will be null.
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.i(LOG_TAG, "onSaveInstanceState: save employee data to Bundle");
        // Convert employee object to Bundle.
        Bundle dataBundle = this.connectInfoToBundle(this.connectInfo);
        outState.putAll(dataBundle);

        super.onSaveInstanceState(outState);
    }


    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onViewStateRestored");

        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);

        if (this.connectInfo == null) {
            Log.i(LOG_TAG, "Get employee data from savedInstanceState");
            // The state was saved by onSaveInstanceState(Bundle outState) method.
            this.connectInfo = this.bundleToConnectInfo(savedInstanceState);
        }
        this.showInGUI(this.connectInfo);
    }

    // Call where View ready.
    private void showInGUI(ConnectInfo connectInfo) {
        illustration.setImageResource(connectInfo.getIllustration());
        description.setText(connectInfo.getDescription());
    }

    private Bundle connectInfoToBundle(ConnectInfo connectInfo) {
        Bundle bundle = new Bundle();
        bundle.putInt("illustration", connectInfo.getIllustration());
        bundle.putInt("description", connectInfo.getDescription());

        return bundle;
    }

    private ConnectInfo bundleToConnectInfo(Bundle savedInstanceState) {
        int illustration = savedInstanceState.getInt("illustration");
        int description = savedInstanceState.getInt("description");

        return new ConnectInfo(illustration, description);
    }
}
