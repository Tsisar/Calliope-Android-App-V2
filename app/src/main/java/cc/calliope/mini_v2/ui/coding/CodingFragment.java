package cc.calliope.mini_v2.ui.coding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;
import cc.calliope.mini_v2.ClickableViewPager;
import cc.calliope.mini_v2.CustomPagerAdapter;
import cc.calliope.mini_v2.PagerItem;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.WebFragment;
import cc.calliope.mini_v2.databinding.FragmentCodingBinding;

public class CodingFragment extends Fragment implements ClickableViewPager.OnItemClickListener {

    private FragmentCodingBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentCodingBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.viewpager.setAdapter(new CustomPagerAdapter(getActivity()));
        binding.viewpager.setOnItemClickListener(this);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void showWebFragment(String url, String editorName) {
        Fragment webFragment = WebFragment.newInstance(url, editorName);
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();

        transaction.replace(R.id.frameLayout, webFragment);
//        transaction.setReorderingAllowed(true);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onItemClick(int position) {
        PagerItem item = PagerItem.values()[position];
        showWebFragment(item.getUrl(), getString(item.getTitleResId()));
    }
}