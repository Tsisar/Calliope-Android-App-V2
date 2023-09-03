package cc.calliope.mini.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static cc.calliope.mini.utils.StaticExtra.SHARED_PREFERENCES_NAME;

public class Permission {
    public static final String[] BLUETOOTH_PERMISSIONS;

    static {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            BLUETOOTH_PERMISSIONS = new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        } else {
            BLUETOOTH_PERMISSIONS = new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN};
        }
    }

    public static final String[] LOCATION_PERMISSIONS = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

    public static boolean isAccessGranted(Activity activity, String... permissions) {
        for (String permission : permissions) {
            boolean granted = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
            Log.d("PERMISSION", permission + (granted ? " granted" : " denied"));
            if (!granted) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAccessDeniedForever(Activity activity, String... permissions) {
        SharedPreferences preferences = activity.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return !isAccessGranted(activity, permissions) // Location permission must be denied
                && preferences.getBoolean(permissions[0], false) // Permission must have been requested before
                && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions[0]); // This method should return false
    }

    public static void markPermissionRequested(Activity activity, String... permissions) {
        SharedPreferences preferences = activity.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(permissions[0], true).apply();
    }
}
