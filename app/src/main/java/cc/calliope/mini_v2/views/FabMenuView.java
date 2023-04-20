package cc.calliope.mini_v2.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;
import cc.calliope.mini_v2.R;

public class FabMenuView extends LinearLayout implements View.OnClickListener {
    public static final int TYPE_LEFT = 0;
    public static final int TYPE_RIGHT = 1;

    @IntDef({TYPE_LEFT, TYPE_RIGHT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FabMenuType {
    }
    FloatingActionButton fullScreenFab;
    private OnItemClickListener onItemClickListener;
    private LinearLayout scriptsLinerLayout;

    public interface OnItemClickListener {
        void onItemClick(View view);
    }

    public FabMenuView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (context instanceof OnItemClickListener) {
            this.onItemClickListener = (OnItemClickListener) context;
        }
        initView(TYPE_RIGHT);
    }

    public FabMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (context instanceof OnItemClickListener) {
            this.onItemClickListener = (OnItemClickListener) context;
        }
        initView(TYPE_RIGHT);
    }

    public FabMenuView(Context context) {
        super(context);
        if (context instanceof OnItemClickListener) {
            this.onItemClickListener = (OnItemClickListener) context;
        }
        initView(TYPE_RIGHT);
    }

    public FabMenuView(Context context, @FabMenuType int type) {
        super(context);
        if (context instanceof OnItemClickListener) {
            this.onItemClickListener = (OnItemClickListener) context;
        }
        initView(type);
    }

    private void initView(@FabMenuType int type) {
        setId(R.id.menuFab);
        View view = inflate(getContext(), type == TYPE_LEFT ? R.layout.menu_fab_left : R.layout.menu_fab_right, null);
        scriptsLinerLayout = view.findViewById(R.id.itemScripts);

        FloatingActionButton connectFab = view.findViewById(R.id.fabConnect);
        FloatingActionButton scriptsFab = view.findViewById(R.id.fabScripts);
        fullScreenFab = view.findViewById(R.id.fabFullScreen);

        connectFab.setOnClickListener(this);
        scriptsFab.setOnClickListener(this);
        fullScreenFab.setOnClickListener(this);


        addView(view);
    }

    @Override
    public void onClick(View view) {
        if (onItemClickListener != null) {
            onItemClickListener.onItemClick(view);
        }
    }

    public void setFullScreenImageResource(int imageResource) {
        fullScreenFab.setImageResource(imageResource);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void setScriptsVisibility(int visibility) {
        scriptsLinerLayout.setVisibility(visibility);
    }
}