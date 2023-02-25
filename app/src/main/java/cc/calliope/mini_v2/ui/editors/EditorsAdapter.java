package cc.calliope.mini_v2.ui.editors;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import cc.calliope.mini_v2.ui.scripts.ScriptsFragment;

public class EditorsAdapter extends FragmentStateAdapter{
    public EditorsAdapter(Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if(position == 3){
            return new ScriptsFragment();
        }else {
            return EditorsItemFragment.newInstance(position);
        }
    }

    @Override
    public int getItemCount() {
        return Editor.values().length;
    }
}