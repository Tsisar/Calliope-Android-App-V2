package cc.calliope.mini_v2.ui.home;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class HomeAdapter extends FragmentStateAdapter {
    private static final int ITEM_COUNT = 2;

    public HomeAdapter(Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if(position == 0) {
            return WelcomeItemFragment.newInstance();
        }else{
            return BatteryItemFragment.newInstance();
        }
    }

    @Override
    public int getItemCount() {
        return ITEM_COUNT;
    }
}