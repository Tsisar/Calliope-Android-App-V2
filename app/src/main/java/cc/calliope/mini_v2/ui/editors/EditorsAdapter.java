package cc.calliope.mini_v2.ui.editors;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import cc.calliope.mini_v2.ui.scripts.ScriptsFragment;

public class EditorsAdapter extends FragmentStateAdapter{
    private OnEditorClickListener listener;

    public void setOnEditorClickListener(OnEditorClickListener listener) {
        this.listener = listener;
    }

    public EditorsAdapter(Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Log.e("createFragment", "position: " + position);
        if(position == 3){
            ScriptsFragment fragment = new ScriptsFragment();
            return fragment;
        }else {
            EditorsItemFragment fragment = EditorsItemFragment.newInstance(position);
            if (listener != null) {
                fragment.setOnEditorClickListener(url -> listener.onEditorClick(url));
            }
            return fragment;
        }
    }

    @Override
    public int getItemCount() {
        return Editor.values().length;
    }
}