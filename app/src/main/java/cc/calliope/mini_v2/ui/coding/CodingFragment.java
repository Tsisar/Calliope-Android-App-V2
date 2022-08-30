package cc.calliope.mini_v2.ui.coding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.WebFragment;
import cc.calliope.mini_v2.databinding.FragmentCodingBinding;

public class CodingFragment extends Fragment {

    private FragmentCodingBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentCodingBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.button.setOnClickListener(view -> showWebFragment
                ("https://makecode.calliope.cc", "MAKECODE"));
        binding.button2.setOnClickListener(view -> showWebFragment
                ("https://lab.open-roberta.org/#loadSystem&&calliope2017", "NEPO"));
        binding.button3.setOnClickListener(view -> showWebFragment
                ("https://calliope.cc/calliope-mini/25programme#17", "BIBLIOTHEK"));

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void showWebFragment(String url, String editorName) {
        // Create new fragment and transaction
        Fragment webFragment = WebFragment.newInstance(url, editorName);
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack if needed
        transaction.replace(R.id.nav_host_fragment_activity_main, webFragment);
        transaction.setReorderingAllowed(true);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }
}