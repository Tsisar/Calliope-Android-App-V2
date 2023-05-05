package cc.calliope.mini_v2.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Permission {
    private static final String SHARED_PREFERENCES_NAME = "cc.calliope.mini_v2.preferences";
    public static final int UNDEFINED = -1;
    public static final int BLUETOOTH = 0;
    public static final int LOCATION = 1;

    @IntDef({UNDEFINED, BLUETOOTH, LOCATION})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RequestType {
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private static final String[] BLUETOOTH_PERMISSIONS_S = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT};

    private static final String[] BLUETOOTH_PERMISSIONS = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN};

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private static final String[] LOCATION_PERMISSIONS_Q = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION};

    private static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION};

    public static String[] getPermissionsArray(@RequestType int requestType) {
        if (requestType == LOCATION) {
            return Version.upperQuinceTart ? LOCATION_PERMISSIONS_Q : LOCATION_PERMISSIONS;
        }
        return Version.upperSnowCone ? BLUETOOTH_PERMISSIONS_S : BLUETOOTH_PERMISSIONS;
    }

    public static boolean isAccessGranted(Activity activity, @RequestType int requestType) {
        String[] permissions = getPermissionsArray(requestType);

        for (String permission : permissions) {
            boolean granted = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
            Log.d("PERMISSION", permission + (granted ? " granted" : " denied"));
            if (!granted) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAccessDeniedForever(Activity activity, @RequestType int requestType) {
        SharedPreferences preferences = activity.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        String permission = getPermissionsArray(requestType)[0];

        return !isAccessGranted(activity, requestType) // Location permission must be denied
                && preferences.getBoolean(permission, false) // Permission must have been requested before
                && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission); // This method should return false
    }

    public static void markPermissionRequested(Activity activity, @RequestType int requestType) {
        SharedPreferences preferences = activity.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        String permission = getPermissionsArray(requestType)[0];

        preferences.edit().putBoolean(permission, true).apply();
    }
}
