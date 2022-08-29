package cc.calliope.mini_v2.ui.help;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import cc.calliope.mini_v2.WebActivity;
import cc.calliope.mini_v2.databinding.FragmentHelpBinding;
import cc.calliope.mini_v2.viewmodels.DeviceViewModel;

public class HelpFragment extends Fragment {

    private FragmentHelpBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HelpViewModel helpViewModel =
                new ViewModelProvider(this).get(HelpViewModel.class);

        binding = FragmentHelpBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.button.setOnClickListener(view -> startWebActivity());
        binding.button2.setOnClickListener(view -> startTestActivity());

        final TextView textView = binding.textNotifications;
        helpViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void startWebActivity() {
        final Intent intent = new Intent(getActivity(), WebActivity.class);
        DeviceViewModel viewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);
        viewModel.getDevice().observe(getViewLifecycleOwner(), device -> {
            intent.putExtra("cc.calliope.mini.EXTRA_DEVICE", device);
        });
        intent.putExtra("cc.calliope.mini.EXTRA_DEVICE", viewModel.getDevice().getValue());
        intent.putExtra("TARGET_NAME", "BIBLIOTHEK");
        intent.putExtra("TARGET_URL", "https://calliope.cc/calliope-mini/25programme#17");
        startActivity(intent);
    }

    private void startTestActivity() {
//        final Intent intent = new Intent(getActivity(), TestActivity.class);
//        startActivity(intent);
    }
}