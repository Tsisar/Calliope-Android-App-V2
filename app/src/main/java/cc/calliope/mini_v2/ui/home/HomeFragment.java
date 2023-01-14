package cc.calliope.mini_v2.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import cc.calliope.mini_v2.views.ZoomOutPageTransformer;
import cc.calliope.mini_v2.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);

        binding.welcomeViewpager.setAdapter(new WelcomePagerAdapter(getActivity()));
//        binding.welcomeViewpager.setOnItemClickListener(this);
//        binding.welcomeViewpager.setPageTransformer(false, new ZoomOutPageTransformer());
        binding.welcomeTabDots.setupWithViewPager(binding.welcomeViewpager, true);

        binding.batteryViewpager.setAdapter(new BatteryPagerAdapter(getActivity()));
//        binding.welcomeViewpager.setOnItemClickListener(this);
//        binding.batteryViewpager.setPageTransformer(false, new ZoomOutPageTransformer());
        binding.batteryTabDots.setupWithViewPager(binding.batteryViewpager, true);

        return binding.getRoot();
    }

    @Override
    public void onViewStateRestored(Bundle bundle) {
        super.onViewStateRestored(bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}