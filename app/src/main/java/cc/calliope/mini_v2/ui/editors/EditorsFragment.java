package cc.calliope.mini_v2.ui.editors;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import cc.calliope.mini_v2.views.ClickableViewPager;
import cc.calliope.mini_v2.adapter.EditorsPagerAdapter;
import cc.calliope.mini_v2.views.ContentCodingViewPager;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.ui.web.WebFragment;
import cc.calliope.mini_v2.databinding.FragmentEditorsBinding;
import cc.calliope.mini_v2.utils.Utils;

public class EditorsFragment extends Fragment implements ClickableViewPager.OnItemClickListener {

    private FragmentEditorsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentEditorsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.viewpager.setAdapter(new EditorsPagerAdapter(getActivity()));
        binding.viewpager.setOnItemClickListener(this);

        binding.tabDots.setupWithViewPager(binding.viewpager, true);

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
        if (Utils.isInternetAvailable()) {
            ContentCodingViewPager content = ContentCodingViewPager.values()[position];
            showWebFragment(content.getUrl(), getString(content.getTitleResId()));
        } else {
            Utils.showErrorMessage(binding.getRoot(), "No internet available");
        }
    }
}