package cc.calliope.mini_v2.ui.home;

import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.databinding.FragmentBasicItemBinding;


public class BatteryItemFragment extends Fragment {
    private FragmentBasicItemBinding binding;
    private AnimationDrawable insertBatteryAnimation;

    public static BatteryItemFragment newInstance() {
        return new BatteryItemFragment();
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
        insertBatteryAnimation.stop();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ImageView insertBatteryImageView = binding.iconImageView;
        insertBatteryImageView.setImageResource(R.drawable.anim_insert_battery);
        insertBatteryAnimation = (AnimationDrawable) insertBatteryImageView.getDrawable();
        insertBatteryAnimation.start();

        binding.titleTextView.setText(R.string.title_battery);

        Spanned spanned = Html.fromHtml(getString(R.string.info_battery));
        binding.infoTextView.setText(spanned);
    }
}