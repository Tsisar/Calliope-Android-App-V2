package cc.calliope.mini_v2.ui.scripts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import cc.calliope.mini_v2.R;

public class SimpleDividerItemDecoration extends RecyclerView.ItemDecoration {
    private final Drawable drawable;
 
    public SimpleDividerItemDecoration(Context context) {
        drawable = ContextCompat.getDrawable(context, R.drawable.line_divider);
    }
 
    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();
 
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
 
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
 
            int top = child.getBottom() + params.bottomMargin;
            int bottom = top + drawable.getIntrinsicHeight();
 
            drawable.setBounds(left, top, right, bottom);
            drawable.draw(c);
        }
    }
}