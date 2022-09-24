package cc.calliope.mini_v2;

import android.app.Activity;

import org.microbit.android.partialflashing.PartialFlashingBaseService;

public class PartialFlashingService extends PartialFlashingBaseService {

    @Override
    protected Class<? extends Activity> getNotificationTarget() {
        return NotificationActivity.class;
    }
}