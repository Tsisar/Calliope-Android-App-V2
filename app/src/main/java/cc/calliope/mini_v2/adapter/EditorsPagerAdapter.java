package cc.calliope.mini_v2.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import cc.calliope.mini_v2.views.ContentCodingViewPager;
import cc.calliope.mini_v2.R;

public class EditorsPagerAdapter extends PagerAdapter {

    private final Context context;

    public EditorsPagerAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup collection, int position) {
        ContentCodingViewPager content = ContentCodingViewPager.values()[position];

        LayoutInflater inflater = LayoutInflater.from(context);
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.item_pager, collection, false);

        TextView title = layout.findViewById(R.id.title_text_view);
        ImageView icon = layout.findViewById(R.id.icon_image_view);
        TextView info = layout.findViewById(R.id.info_text_view);

        title.setText(content.getTitleResId());
        icon.setImageResource(content.getIconResId());
        info.setText(content.getInfoResId());

        collection.addView(layout);
        return layout;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, @NonNull Object view) {
        collection.removeView((View) view);
    }

    @Override
    public int getCount() {
        return ContentCodingViewPager.values().length;
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