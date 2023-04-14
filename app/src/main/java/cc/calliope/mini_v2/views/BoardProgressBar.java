package cc.calliope.mini_v2.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.Nullable;
import cc.calliope.mini_v2.service.DfuService;

public class BoardProgressBar extends BoardView {

    public BoardProgressBar(Context context) {
        super(context);
    }

    public BoardProgressBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BoardProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setProgress(int progress) {
        if (progress == DfuService.PROGRESS_COMPLETED) {
            setAllLed(false);
            setLed(true, 2, 3, 4, 6, 10, 17, 19);
        } else {
            int p = Math.max(progress, 0);
            int max = p / 4;

            for (int i = 0; i <= max; i++) {
                setLed(true, i);
            }
        }
        invalidate();
    }
}