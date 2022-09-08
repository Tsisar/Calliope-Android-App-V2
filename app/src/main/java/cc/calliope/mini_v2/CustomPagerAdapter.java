package cc.calliope.mini_v2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

public class CustomPagerAdapter extends PagerAdapter {

    private final Context context;

    public CustomPagerAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup collection, int position) {
        PagerItem pagerItem = PagerItem.values()[position];

        LayoutInflater inflater = LayoutInflater.from(context);
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.item_pager, collection, false);

        TextView title = layout.findViewById(R.id.title_text_view);
        ImageView icon = layout.findViewById(R.id.icon_image_view);
        TextView info = layout.findViewById(R.id.info_text_view);

        title.setText(pagerItem.getTitleResId());
        icon.setImageResource(pagerItem.getIconResId());
        info.setText(pagerItem.getInfoResId());

        collection.addView(layout);
        return layout;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
    }

    @Override
    public int getCount() {
        return PagerItem.values().length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        PagerItem customPagerEnum = PagerItem.values()[position];
        return context.getString(customPagerEnum.getTitleResId());
    }

}