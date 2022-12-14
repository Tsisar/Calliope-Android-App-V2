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
import cc.calliope.mini_v2.views.Editor;
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

        binding.editorViewpager.setAdapter(new EditorsPagerAdapter(getActivity()));
        binding.editorViewpager.setOnItemClickListener(this);

        binding.tabDots.setupWithViewPager(binding.editorViewpager, true);

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
        if (getActivity() != null && Utils.isNetworkConnected(getActivity())) {
            Editor editor = Editor.values()[position];
            showWebFragment(editor.getUrl(), editor.toString());
        } else {
            Utils.errorSnackbar(binding.getRoot(), "No internet available").show();
        }
    }
}