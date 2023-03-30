package cc.calliope.mini_v2.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.res.ResourcesCompat;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.utils.Utils;

public class CustomView extends AppCompatImageView {

    private static final String TAG = "CustomView";

    private Paint paint;
    private int progress = 0;

    public CustomView(Context context) {
        super(context);
        init();
    }

    public CustomView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
//        setProgress(50);
    }

    public void setProgress(int progress) {
        this.progress = Math.max(progress, 0);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.progressbar, null);

        int width = getWidth();
        int height = getHeight();

        if (drawable instanceof LayerDrawable) {
            Log.w(TAG, "It`s LayerDrawable");
            LayerDrawable layerDrawable = (LayerDrawable) drawable;

            Drawable background = layerDrawable.findDrawableByLayerId(R.id.background);
            background.setBounds(0, 0, width, height);
            background.draw(canvas);

            int N = layerDrawable.getNumberOfLayers();

            for (int i = 0; i < N; i++) {
                final int id = layerDrawable.getId(i);
                Log.w(TAG, "Layer_" + i + ": " + id);
            }

            Drawable p = layerDrawable.getDrawable(progress/4);
            p.setBounds(0, 0, width, height);
            p.draw(canvas);
        }

//        Drawable test = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_ble_01, null);
//        test.setBounds(0, 0, width, height);
//        test.draw(canvas);

//        int width = getWidth();
//        int height = getHeight();
//
//        Log.w(TAG, "width: " + width);
//        Log.w(TAG, "height: " + height);
//
//        int firstX = (int) (width * 0.345);
//        int firstY = (int) (height * 0.5625);
//        int ledWidth = (int) (width * 0.03375);
//        int ledHeight = (int) (height * 0.055);
//        int offsetX = (int) (width * 0.072);
//        int offsetY = (int) (height * 0.0705);
//
//        paint.setColor(Color.RED);
//
//        for (int i = 0; i < 5; i++) {
//            for (int j = 0; j < 5; j++) {
//                canvas.drawRect(
//                        firstX + offsetX * i,
//                        firstY - offsetY * j,
//                        firstX + ledWidth + offsetX * i,
//                        firstY + ledHeight - offsetY * j,
//                        paint
//                );
//            }
//        }

        super.onDraw(canvas);
    }
}