package cc.calliope.mini_v2;

import android.app.Activity;

import org.microbit.android.partialflashing.PartialFlashingBaseService;

import cc.calliope.mini_v2.activity.NotificationActivity;

public class PartialFlashingService extends PartialFlashingBaseService {

    @Override
    protected Class<? extends Activity> getNotificationTarget() {
        return NotificationActivity.class;
    }
}