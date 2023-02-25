package cc.calliope.mini_v2.ui.home;

import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import cc.calliope.mini_v2.databinding.FragmentBasicItemBinding;

public class HomeItemFragment extends Fragment {
    public static final String ARG_POSITION = "arg_position";
    private FragmentBasicItemBinding binding;
    private AnimationDrawable demoAnimation;

    public static HomeItemFragment newInstance(int position) {
        HomeItemFragment fragment = new HomeItemFragment();
        Bundle args = new Bundle();
        args.putInt(HomeItemFragment.ARG_POSITION, position);
        fragment.setArguments(args);

        return fragment;
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
        Bundle args = getArguments();
        if (args == null)
            return;

        int position = args.getInt(ARG_POSITION);
        Home home = Home.values()[position];

        ImageView insertBatteryImageView = binding.iconImageView;
        insertBatteryImageView.setImageResource(home.getIconResId());
        demoAnimation = (AnimationDrawable) insertBatteryImageView.getDrawable();

        binding.titleTextView.setText(home.getTitleResId());

        Spanned spanned = Html.fromHtml(getString(home.getInfoResId()));
        binding.infoTextView.setText(spanned);
    }

    @Override
    public void onResume() {
        super.onResume();
        demoAnimation.start();
    }

    @Override
    public void onPause() {
        demoAnimation.stop();
        super.onPause();
    }
}