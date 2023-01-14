package cc.calliope.mini_v2.ui.editors;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import cc.calliope.mini_v2.views.ZoomOutPageTransformer;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.ui.web.WebFragment;
import cc.calliope.mini_v2.databinding.FragmentEditorsBinding;
import cc.calliope.mini_v2.utils.Utils;

public class EditorsFragment extends Fragment implements OnEditorClickListener {

    private FragmentEditorsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentEditorsBinding.inflate(inflater, container, false);

        EditorsAdapter adapter = new EditorsAdapter(this);
        adapter.setOnEditorClickListener(this);

        binding.editorViewpager.setAdapter(adapter);
        binding.editorViewpager.setPageTransformer(new ZoomOutPageTransformer());

        new TabLayoutMediator(binding.tabDots, binding.editorViewpager,
                (tab, position) -> tab.setTabLabelVisibility(TabLayout.TAB_LABEL_VISIBILITY_UNLABELED))
                .attach();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onEditorClick(Editor editor) {
        if (getActivity() != null && Utils.isNetworkConnected(getActivity())) {
            if (editor != Editor.SCRIPTS) {
                showWebFragment(editor.getUrl(), editor.toString());
            }
        } else {
            Utils.errorSnackbar(binding.getRoot(), "No internet available").show();
        }
    }

    private void showWebFragment(String url, String editorName) {
        Fragment webFragment = WebFragment.newInstance(url, editorName);

        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, webFragment)
                .setReorderingAllowed(true)
                .addToBackStack("WEB")
                .commit();
    }
}