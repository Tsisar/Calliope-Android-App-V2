package cc.calliope.mini_v2.utils;

import android.app.Activity;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class FileUtils {
    private static final String TAG = "FileUtils";

    public static File getFile(Activity activity, String editorName, String filename, String extension) {

        File dir = new File(activity.getFilesDir().toString() + File.separator + editorName);
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        Log.w(TAG, "DIR: " + dir);

        File file = new File(dir.getAbsolutePath() + File.separator + filename + extension);
        int i = 1;
        while (file.exists()) {
            String number = String.format("(%s)", ++i);
            file = new File(dir.getAbsolutePath() + File.separator + filename + number + extension);
        }

        try {
            if (file.createNewFile()) {
                Log.w(TAG, "createNewFile: " + file);
                return file;
            } else {
                Log.w(TAG, "CreateFile Error, deleting: " + file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
