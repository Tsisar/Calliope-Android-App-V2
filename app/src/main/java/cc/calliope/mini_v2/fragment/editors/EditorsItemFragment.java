package cc.calliope.mini_v2.fragment.editors;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.databinding.FragmentItemBinding;
import cc.calliope.mini_v2.fragment.web.WebFragment;
import cc.calliope.mini_v2.utils.Utils;

import static cc.calliope.mini_v2.utils.StaticExtra.SHARED_PREFERENCES_NAME;

public class EditorsItemFragment extends Fragment {
    private static final String ARG_POSITION = "arg_position";
    private static final String KEY_CUSTOM_LINK = "pref_key_custom_link";
    private FragmentItemBinding binding;
    private final AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.75F);
    private Editor editor;

    public static EditorsItemFragment newInstance(int position) {
        EditorsItemFragment fragment = new EditorsItemFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentItemBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Activity activity = getActivity();
        Bundle args = getArguments();
        if (args == null || activity == null)
            return;

        int position = args.getInt(ARG_POSITION);
        editor = Editor.values()[position];


        binding.titleTextView.setText(editor.getTitleResId());
        binding.iconImageView.setImageResource(editor.getIconResId());
        binding.infoTextView.setText(editor.getInfoResId());

        binding.infoTextView.setOnClickListener(this::openEditor);
        view.setOnClickListener(this::openEditor);
    }

    private void showWebFragment(String url, String editorName) {
        Activity activity = getActivity();
        if (activity == null){
            return;
        }

        NavController navController = Navigation.findNavController(activity, R.id.navigation_host_fragment);
        NavDirections webFragment = EditorsFragmentDirections.actionEditorsToWeb(url, editorName);
        navController.navigate(webFragment);
    }


    private String readCustomLink(Activity activity) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        return sharedPreferences.getString(KEY_CUSTOM_LINK, Editor.CUSTOM.getUrl());
    }

    private void openEditor(View view){
        Activity activity = getActivity();
        if (activity == null){
            return;
        }

        view.startAnimation(buttonClick);
        if (Utils.isNetworkConnected(activity)) {
            String url = editor.getUrl();
            if (editor == Editor.CUSTOM) {
                url = readCustomLink(activity);
            }
            showWebFragment(url, editor.toString());
        } else {
            Utils.errorSnackbar(binding.getRoot(), getString(R.string.error_snackbar_no_internet)).show();
        }
    }
}