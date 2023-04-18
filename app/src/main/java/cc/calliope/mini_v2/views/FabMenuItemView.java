package cc.calliope.mini_v2.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;
import cc.calliope.mini_v2.R;

public class FabMenuItemView extends LinearLayout implements View.OnClickListener {

    public static final int TYPE_LEFT = 0;
    public static final int TYPE_RIGHT = 1;

    @IntDef({TYPE_LEFT, TYPE_RIGHT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FabMenuType {
    }

    private OnItemClickListener onItemClickListener;
    private TextView title;
    private FloatingActionButton fab;

    public interface OnItemClickListener {
        void onItemClick(FabMenuItemView view);
    }

    public FabMenuItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (context instanceof OnItemClickListener) {
            this.onItemClickListener = (OnItemClickListener) context;
        }
        initView(TYPE_RIGHT);
    }

    public FabMenuItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (context instanceof OnItemClickListener) {
            this.onItemClickListener = (OnItemClickListener) context;
        }
        initView(TYPE_RIGHT);
    }

    public FabMenuItemView(Context context) {
        super(context);
        if (context instanceof OnItemClickListener) {
            this.onItemClickListener = (OnItemClickListener) context;
        }
        initView(TYPE_RIGHT);
    }

    public FabMenuItemView(Context context, @FabMenuType int type) {
        super(context);
        if (context instanceof OnItemClickListener) {
            this.onItemClickListener = (OnItemClickListener) context;
        }
        initView(type);
    }

    public FabMenuItemView(Context context, @FabMenuType int type, int imageResource, String title) {
        super(context);
        if (context instanceof OnItemClickListener) {
            this.onItemClickListener = (OnItemClickListener) context;
        }
        initView(type, imageResource, title);
    }

    private void initView(@FabMenuType int type, int imageResource, String title) {
        initView(type);
        setImageResource(imageResource);
        setTitle(title);
    }

    private void initView(@FabMenuType int type) {
        View view = inflate(getContext(), type == TYPE_LEFT ? R.layout.item_fab_menu_left : R.layout.item_fab_menu_right, null);
        title = view.findViewById(R.id.titleTextView);
        fab = view.findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(this);
        addView(view);
    }

    public void setTitle(String title) {
        this.title.setText(title);
    }

    @Override
    public void onClick(View v) {
        if (onItemClickListener != null) {
            onItemClickListener.onItemClick(this);
        }
    }

    public void setImageResource(int imageResource) {
        fab.setImageResource(imageResource);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }
}