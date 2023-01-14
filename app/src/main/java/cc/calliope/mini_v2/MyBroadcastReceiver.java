package cc.calliope.mini_v2;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class MyBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

//        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
//        manager.cancelAll();

        String state = intent.getAction();
        if (state.equals("Action")) {
            Toast.makeText(context, "detect state: " + state, Toast.LENGTH_LONG).show();

        } else{
            Toast.makeText(context, state, Toast.LENGTH_LONG).show();
        }
    }
}
