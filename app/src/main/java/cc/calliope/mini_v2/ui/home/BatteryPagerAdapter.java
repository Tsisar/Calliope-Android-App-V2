package cc.calliope.mini_v2.ui.home;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.ui.editors.Editor;

public class BatteryPagerAdapter extends PagerAdapter {

    private final Context context;

    public BatteryPagerAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup collection, int position) {
        LayoutInflater inflater = LayoutInflater.from(context);
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.item_battery_pager, collection, false);

        ImageView icon = layout.findViewById(R.id.icon_image_view);
        TextView info = layout.findViewById(R.id.info_text_view);

        icon.setImageResource(getResourceId(R.array.first_steps_illustrations, position));
        info.setText(getResourceId(R.array.first_steps_descriptions, position));

        collection.addView(layout);
        return layout;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, @NonNull Object view) {
        collection.removeView((View) view);
    }

    @Override
    public int getCount() {
        return 5;
//        return Editor.values().length;
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

    public int getResourceId(int arrayId, int index){
        try {
            TypedArray array = context.getResources().obtainTypedArray(arrayId);
            int resId = array.getResourceId(index, 0);
            array.recycle();
            return resId;
        } catch (ArrayIndexOutOfBoundsException e){
            Log.e("getResourceId", "ArrayIndexOutOfBoundsException: " + e);
        }
        return 0;
    }

}