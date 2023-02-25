package cc.calliope.mini_v2.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.databinding.FragmentBasicItemBinding;


public class WelcomeItemFragment extends Fragment {
    private FragmentBasicItemBinding binding;

    public static WelcomeItemFragment newInstance() {
        return new WelcomeItemFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBasicItemBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.iconImageView.setImageResource(R.drawable.welcome);
        binding.titleTextView.setText(R.string.title_welcome);
        binding.infoTextView.setText(R.string.info_welcome);
    }
}