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
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.databinding.FragmentItemBinding;
import cc.calliope.mini_v2.fragment.web.WebFragment;
import cc.calliope.mini_v2.utils.Utils;

import static cc.calliope.mini_v2.utils.StaticExtra.SHARED_PREFERENCES_NAME;

public class EditorsItemFragment extends Fragment {
    private static final String ARG_POSITION = "arg_position";
    private static final String KEY_CUSTOM_LINK = "custom_link";
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

        if (editor == Editor.CUSTOM) {
            addEditFab();
        }

        binding.infoTextView.setOnClickListener(this::openEditor);
        view.setOnClickListener(this::openEditor);
    }

    private void startWebActivity(String url, String editorName) {
        Fragment webFragment = WebFragment.newInstance(url, editorName);

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, webFragment)
                .setReorderingAllowed(true)
                .addToBackStack(getString(R.string.title_web))
                .commit();
    }

    private void addEditFab() {
        Activity activity = getActivity();
        if (activity == null){
            return;
        }

        int maxImageSize = Utils.convertDpToPixel(activity, 32);
        int color = ContextCompat.getColor(activity, R.color.white);
        int margin = activity.getResources().getDimensionPixelSize(R.dimen.margin_half);
        ColorStateList tint = ColorStateList.valueOf(color);

        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );
        params.endToEnd = binding.doubleConstraintLayout.getId();
        params.bottomToBottom = binding.doubleConstraintLayout.getId();
        params.setMarginEnd(margin);

        FloatingActionButton fab = new FloatingActionButton(activity);
        fab.setImageResource(R.drawable.ic_edit_24);
        fab.setSize(FloatingActionButton.SIZE_AUTO);
        fab.setMaxImageSize(maxImageSize);
        fab.setImageTintList(tint);
        fab.setOnClickListener(view -> editLink(activity));
        fab.setLayoutParams(params);
        binding.basicConstraintLayout.addView(fab);
    }

    private void editLink(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_edit, activity.findViewById(R.id.layoutDialogContainer));
        builder.setView(view);

        ((TextView) view.findViewById(R.id.textTitle)).setText(R.string.title_dialog_edit_link);
        EditText editText = view.findViewById(R.id.editField);
        editText.setText(readCustomLink(activity));

        ((Button) view.findViewById(R.id.buttonYes)).setText(R.string.button_save);
        ((Button) view.findViewById(R.id.buttonNo)).setText(R.string.button_cancel);
        final AlertDialog alertDialog = builder.create();
        view.findViewById(R.id.buttonYes).setOnClickListener(view1 -> {
            saveCustomLink(activity, editText.getText().toString());
            alertDialog.dismiss();
        });
        view.findViewById(R.id.buttonNo).setOnClickListener(view12 -> alertDialog.dismiss());
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        alertDialog.show();
    }

    private void saveCustomLink(Activity activity, String link) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(KEY_CUSTOM_LINK, link).apply();
    }

    private String readCustomLink(Activity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
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
            startWebActivity(url, editor.toString());
        } else {
            Utils.errorSnackbar(binding.getRoot(), getString(R.string.error_snackbar_no_internet)).show();
        }
    }
}