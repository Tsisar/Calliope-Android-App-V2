package cc.calliope.mini_v2.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import cc.calliope.mini_v2.R;

public class DimView extends FrameLayout {

    public DimView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public DimView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public DimView(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        setId(R.id.dimView);
        addView(inflate(getContext(), R.layout.dim_view, null));
    }
}