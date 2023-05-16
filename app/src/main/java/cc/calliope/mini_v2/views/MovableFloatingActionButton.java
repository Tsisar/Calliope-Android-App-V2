package cc.calliope.mini_v2.views;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.StateService;
import cc.calliope.mini_v2.utils.Utils;
import cc.calliope.mini_v2.utils.Version;

public class MovableFloatingActionButton extends FloatingActionButton implements View.OnTouchListener {
    private final static String TAG = "MovableFloatingActionButton";
    private final static float CLICK_DRAG_TOLERANCE = 10; // Often, there will be a slight, unintentional, drag when the user taps the FAB, so we need to account for this.
    private float downRawX, downRawY;
    private float dX, dY;
    private Paint paint;
    private RectF rectF;
    private int progress = 0;
    private boolean isFabMenuOpen = false;
    private Context context;
    private ProgressReceiver broadcastReceiver;

    private boolean flashing;

    public MovableFloatingActionButton(Context context) {
        super(context);
        init(context);
    }

    public MovableFloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MovableFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        setOnTouchListener(this);
        paint = new Paint();
        rectF = new RectF();
        setOnSystemUiVisibilityChangeListener(this::onFullscreenStateChanged);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        registerBroadcastReceiver();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unregisterBroadcastReceiver();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (isFabMenuOpen) {
            return false;
        }

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();

        int action = motionEvent.getAction();
        if (action == MotionEvent.ACTION_DOWN) {

            downRawX = motionEvent.getRawX();
            downRawY = motionEvent.getRawY();
            dX = view.getX() - downRawX;
            dY = view.getY() - downRawY;

            return true; // Consumed

        } else if (action == MotionEvent.ACTION_MOVE) {

            int viewWidth = view.getWidth();
            int viewHeight = view.getHeight();

            View viewParent = (View) view.getParent();
            int parentWidth = viewParent.getWidth();
            int parentHeight = viewParent.getHeight();

            float newX = motionEvent.getRawX() + dX;
            newX = Math.max(layoutParams.leftMargin, newX); // Don't allow the FAB past the left hand side of the parent
            newX = Math.min(parentWidth - viewWidth - layoutParams.rightMargin, newX); // Don't allow the FAB past the right hand side of the parent

            float newY = motionEvent.getRawY() + dY;
            newY = Math.max(layoutParams.topMargin, newY); // Don't allow the FAB past the top of the parent
            newY = Math.min(parentHeight - viewHeight - layoutParams.bottomMargin, newY); // Don't allow the FAB past the bottom of the parent

            view.animate()
                    .x(newX)
                    .y(newY)
                    .setDuration(0)
                    .start();

            return true; // Consumed

        } else if (action == MotionEvent.ACTION_UP) {

            float upRawX = motionEvent.getRawX();
            float upRawY = motionEvent.getRawY();

            float upDX = upRawX - downRawX;
            float upDY = upRawY - downRawY;

            if (Math.abs(upDX) < CLICK_DRAG_TOLERANCE && Math.abs(upDY) < CLICK_DRAG_TOLERANCE) { // A click
                return performClick();
            } else { // A drag
                return true; // Consumed
            }

        } else {
            return super.onTouchEvent(motionEvent);
        }

    }

    public void setProgress(int progress) {
        this.progress = Math.max(progress, 0);
        invalidate();
    }

    public void setColor(int resId) {
        int color;
        if (Version.upperMarshmallow) {
            color = context.getColor(resId);
        } else {
            color = getResources().getColor(resId);
        }
        setBackgroundTintList(ColorStateList.valueOf(color));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int strokeWidth = Utils.convertDpToPixel(getContext(), 4);
        int width = getWidth();
        int height = getHeight();
        int sweepAngle = (int) (360 * (progress / 100.f));

        rectF.set(strokeWidth / 2.f, strokeWidth / 2.f, width - strokeWidth / 2.f, height - strokeWidth / 2.f);

        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(Paint.Style.STROKE);

        canvas.drawArc(rectF, 270, sweepAngle, false, paint);

        super.onDraw(canvas);
    }

    public boolean isFlashing() {
        return flashing;
    }

    public boolean isFabMenuOpen() {
        return isFabMenuOpen;
    }

    public void setFabMenuOpen(boolean fabMenuOpen) {
        isFabMenuOpen = fabMenuOpen;
    }

    private void onFullscreenStateChanged(int visibility) {
        boolean fullScreen = (visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0;
        if (!fullScreen) {
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) getLayoutParams();
            View viewParent = (View) getParent();
            int parentWidth = viewParent.getWidth();
            int parentHeight = viewParent.getHeight();
            int weight = getWidth();
            int height = getHeight();
            int x = Math.round(getX());
            int y = Math.round(getY());

            if (x + weight > parentWidth) {
                animate()
                        .x(parentWidth - weight - layoutParams.rightMargin)
                        .y(y)
                        .setDuration(0)
                        .start();
            }
            if (y + height > parentHeight) {
                animate()
                        .x(x)
                        .y(parentHeight - height - layoutParams.bottomMargin)
                        .setDuration(0)
                        .start();
            }
        }
    }

    public void registerBroadcastReceiver() {
        if (broadcastReceiver == null) {
            broadcastReceiver = new ProgressReceiver();
            Utils.log(Log.WARN, TAG, "register Progress Receiver");
            IntentFilter filter = new IntentFilter();
            filter.addAction(StateService.BROADCAST_PROGRESS);
            filter.addAction(StateService.BROADCAST_FLASHING);
            context.registerReceiver(broadcastReceiver, filter);
        }
    }

    public void unregisterBroadcastReceiver() {
        if (broadcastReceiver != null) {
            Utils.log(Log.WARN, TAG, "unregister Progress Receiver");
            context.unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
    }

    private class ProgressReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action){
                case StateService.BROADCAST_FLASHING -> {
                    flashing = intent.getBooleanExtra(StateService.EXTRA_FLASHING, false);
                    if(flashing){
                        setColor(R.color.green);
                    }
                }
                case StateService.BROADCAST_PROGRESS -> {
                    int progress = intent.getIntExtra(StateService.EXTRA_PROGRESS, 0);
                    setProgress(progress);
                    if(progress > 0 && !flashing){
                        flashing = true;
                        setColor(R.color.green);
                    }
                }
            }
        }
    }
}