package cc.calliope.mini_v2.ui.editors;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import cc.calliope.mini_v2.databinding.FragmentEditorItemBinding;


public class EditorsItemFragment extends Fragment {
    public static final String ARG_POSITION = "arg_position";
    private FragmentEditorItemBinding binding;
    private OnEditorClickListener listener;
    private final AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.75F);

    public void setOnEditorClickListener(OnEditorClickListener listener) {
        this.listener = listener;
    }

    public static EditorsItemFragment newInstance(int position) {
        EditorsItemFragment fragment = new EditorsItemFragment();
        Bundle args = new Bundle();
        args.putInt(EditorsItemFragment.ARG_POSITION, position);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditorItemBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        if(args == null)
            return;

        int position = args.getInt(ARG_POSITION);
        Editor editor = Editor.values()[position];

        binding.titleTextView.setText(editor.getTitleResId());
        binding.iconImageView.setImageResource(editor.getIconResId());
        binding.infoTextView.setText(editor.getInfoResId());

        if (listener != null) {
            view.setOnClickListener(v -> {
                v.startAnimation(buttonClick);
                listener.onEditorClick(editor);
            });
        }
    }
}