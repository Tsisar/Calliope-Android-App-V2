package cc.calliope.mini_v2.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.ui.editors.Editor;

public class WelcomePagerAdapter extends PagerAdapter {

    private final Context context;

    public WelcomePagerAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup collection, int position) {
        LayoutInflater inflater = LayoutInflater.from(context);
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.item_welcome_pager, collection, false);

//        ImageView icon = layout.findViewById(R.id.icon_image_view);

//        icon.setImageResource(editor.getIconResId());

        collection.addView(layout);
        return layout;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, @NonNull Object view) {
        collection.removeView((View) view);
    }

    @Override
    public int getCount() {
//        return Editor.values().length;
        return 2;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return null;
//        ContentCodingViewPager customPagerEnum = ContentCodingViewPager.values()[position];
//        return context.getString(customPagerEnum.getTitleResId());
    }

}