package cc.calliope.mini_v2.fragment.help;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.databinding.FragmentHelpBinding;
import cc.calliope.mini_v2.fragment.editors.EditorsFragmentDirections;

public class HelpFragment extends Fragment {
    private FragmentHelpBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHelpBinding.inflate(inflater, container, false);

        binding.settingsActionButton.setOnClickListener(this::onSettingsClicked);

        TextView appInfo = binding.appInfo;
        appInfo.setMovementMethod(LinkMovementMethod.getInstance());
        Spanned spanned = Html.fromHtml(getString(R.string.info_app));
        appInfo.setText(spanned);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void onSettingsClicked(View view){
        Activity activity = getActivity();
        if (activity == null){
            return;
        }

        NavController navController = Navigation.findNavController(activity, R.id.navigation_host_fragment);
        navController.navigate(R.id.navigation_settings);
    }
}