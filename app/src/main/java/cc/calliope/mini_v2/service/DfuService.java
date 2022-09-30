package cc.calliope.mini_v2.service;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;


import androidx.annotation.Nullable;
import cc.calliope.mini_v2.BuildConfig;
import cc.calliope.mini_v2.NotificationActivity;
import cc.calliope.mini_v2.utils.Version;
import no.nordicsemi.android.dfu.DfuBaseService;
import no.nordicsemi.android.dfu.DfuServiceInitiator;

public class DfuService extends DfuBaseService {

    private static final String TAG = "DfuService";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    @Override
    protected Class<? extends Activity> getNotificationTarget() {
        return NotificationActivity.class;
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "### " + Thread.currentThread().getId() + " # " + "onCreate()");
        // Enable Notification Channel for Android OREO
        if (Version.upperOreo) {
            DfuServiceInitiator.createDfuNotificationChannel(getApplicationContext());
        }
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(@Nullable final Intent intent) {
        waitFor(2000);
        super.onHandleIntent(intent);
    }

    @Override
    protected boolean isDebug() {
        // Here return true if you want the service to print more logs in LogCat.
        // Library's BuildConfig in current version of Android Studio is always set to DEBUG=false, so
        // make sure you return true or your.app.BuildConfig.DEBUG here.
        return DEBUG;
    }
}
