package cc.calliope.mini_v2.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import cc.calliope.mini_v2.R;

public class Utils {
    private static final String PREFS_LOCATION_NOT_REQUIRED = "location_not_required";
    private static final String PREFS_PERMISSION_REQUESTED = "permission_requested";

    /**
     * Checks whether device is connected to network
     *
     * @param context the context
     * @return true if connected, false otherwise.
     */
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return manager.getActiveNetworkInfo() != null && manager.getActiveNetworkInfo().isConnected();
    }

    /**
     * Actually checks if device is connected to internet
     * (There is a possibility it's connected to a network but not to internet)
     *
     * @return true if connected, false otherwise.
     */
    public static boolean isInternetAvailable() {
        String command = "ping -c 1 google.com";
        try {
            return Runtime.getRuntime().exec(command).waitFor() == 0;
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Checks whether Bluetooth is enabled.
     *
     * @return true if Bluetooth is enabled, false otherwise.
     */
    public static boolean isBluetoothEnabled() {
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter != null && adapter.isEnabled();
    }

    /**
     * On some devices running Android Marshmallow or newer location services must be enabled in order to scan for Bluetooth LE devices.
     * This method returns whether the Location has been enabled or not.
     *
     * @return true on Android 6.0+ if location mode is different than LOCATION_MODE_OFF.
     */
    public static boolean isLocationEnabled(final Context context) {
        int locationMode = Settings.Secure.LOCATION_MODE_OFF;
        try {
            locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
        } catch (final Settings.SettingNotFoundException e) {
            // do nothing
        }
        return locationMode != Settings.Secure.LOCATION_MODE_OFF;
    }

    /**
     * Checks two permissions were required: BLUETOOTH and BLUETOOTH_ADMIN
     * to communicate with Bluetooth LE devices on Android version 4.3 until 11
     *
     * @param context the context
     * @return true if granted, false otherwise.
     */
    public static boolean isBluetoothAdminPermissionsGranted(Context context) {
        return hasPermissions(context, Manifest.permission.BLUETOOTH) && hasPermissions(context, Manifest.permission.BLUETOOTH_ADMIN);
    }

    /**
     * Checks location permissions is granted
     *
     * @param context the context
     * @return true if granted, false otherwise.
     */
    public static boolean isLocationPermissionsGranted(final Context context) {
        return hasPermissions(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                && hasPermissions(context, Manifest.permission.ACCESS_FINE_LOCATION)
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || hasPermissions(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION));
    }

    /**
     * Checks two permissions were required: BLUETOOTH_SCAN and BLUETOOTH_CONNECT
     * to communicate with Bluetooth LE devices on Android version 4.3 until 11
     *
     * @param context the context
     * @return true if granted, false otherwise.
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    public static boolean isBluetoothScanPermissionsGranted(Context context) {
        return hasPermissions(context, Manifest.permission.BLUETOOTH_SCAN) && hasPermissions(context, Manifest.permission.BLUETOOTH_CONNECT);
    }

    /**
     * Returns true if location permission has been requested at least twice and
     * user denied it, and checked 'Don't ask again'.
     * @param activity the activity
     * @return true if permission has been denied and the popup will not come up any more, false otherwise
     */
    public static boolean isPermissionDeniedForever(final Activity activity, String permission) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);

        return !isLocationPermissionsGranted(activity) // Location permission must be denied
                && preferences.getBoolean(PREFS_PERMISSION_REQUESTED, false) // Permission must have been requested before
                && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission); // This method should return false
    }

    /**
     * Location enabled is required on some phones running Android Marshmallow or newer (for example on Nexus and Pixel devices).
     *
     * @param context the context
     * @return false if it is known that location is not required, true otherwise
     */
    public static boolean isLocationRequired(final Context context) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(PREFS_LOCATION_NOT_REQUIRED, true);
    }

    /**
     * When a Bluetooth LE packet is received while Location is disabled it means that Location
     * is not required on this device in order to scan for LE devices. This is a case of Samsung phones, for example.
     * Save this information for the future to keep the Location info hidden.
     * @param context the context
     */
    public static void markLocationNotRequired(final Context context) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean(PREFS_LOCATION_NOT_REQUIRED, false).apply();
    }

    /**
     * The first time an app requests a permission there is no 'Don't ask again' checkbox and
     * {@link ActivityCompat#shouldShowRequestPermissionRationale(Activity, String)} returns false.
     * This situation is similar to a permission being denied forever, so to distinguish both cases
     * a flag needs to be saved.
     * @param context the context
     */
    public static void markPermissionRequested(final Context context) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean(PREFS_PERMISSION_REQUESTED, true).apply();
    }


    private static boolean hasPermissions(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static void showErrorMessage(Activity activity, String message){
        if(activity != null) {
            Snackbar.make(activity.getWindow().getDecorView().getRootView(), message, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(activity.getResources().getColor(R.color.red, null))
                    .setTextColor(activity.getResources().getColor(R.color.white, null))
                    .show();
        }
    }
}
