package cc.calliope.mini_v2.ui.web;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import androidx.core.content.FileProvider;

public class JavaScriptInterface {
    private static String fileMimeType;
    private final Context context;

    public JavaScriptInterface(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    public void getBase64FromBlobData(String base64Data) throws IOException {
//        convertBase64StringToFileAndStoreIt(base64Data);
        Log.e("JavaScriptInterfac", "base64Data: " + base64Data);
//        selectDownloadMethod(base64Data, mimeType);
    }

    public static String getBase64StringFromBlobUrl(String blobUrl, String mimeType) {
        if (blobUrl.startsWith("blob")) {
            fileMimeType = mimeType;
            return "javascript: var xhr = new XMLHttpRequest();" +
                    "xhr.open('GET', '" + blobUrl + "', true);" +
                    "xhr.setRequestHeader('Content-type','" + mimeType + ";charset=UTF-8');" +
                    "xhr.responseType = 'blob';" +
                    "xhr.onload = function(e) {" +
                    "    if (this.status == 200) {" +
                    "        var blobFile = this.response;" +
                    "        var reader = new FileReader();" +
                    "        reader.readAsDataURL(blobFile);" +
                    "        var name = blobFile.name;"+                //TODO remove it
                    "        var size = blobFile.size;"+                //
                    "        console.log(name);" +                      //
                    "        console.log(size);" +                      //
                    "        reader.onloadend = function() {" +
                    "            base64data = reader.result;" +
                    "            Android.getBase64FromBlobData(base64data);" +
                    "        }" +
                    "    }" +
                    "};" +
                    "xhr.send();";
        }
        return "javascript: console.log('It is not a Blob URL');";
    }

    private void convertBase64StringToFileAndStoreIt(String base64PDf) throws IOException {
        final int notificationId = 1;

        String currentDateTime = DateFormat.getDateTimeInstance().format(new Date());

        String newTime = currentDateTime.replaceFirst(", ", "_").replaceAll(" ", "_").replaceAll(":", "-");

        Log.d("fileMimeType ====> ", fileMimeType);
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String extension = mimeTypeMap.getExtensionFromMimeType(fileMimeType);

        final File dwldsPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + newTime + "_." + extension);

        String regex = "^data:" + fileMimeType + ";base64,";
        byte[] pdfAsBytes = Base64.decode(base64PDf.replaceFirst(regex, ""), 0);
        try {
            FileOutputStream os = new FileOutputStream(dwldsPath);
            os.write(pdfAsBytes);
            os.flush();
            os.close();
        } catch (Exception e) {
            Toast.makeText(context, "FAILED TO DOWNLOAD THE FILE!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        if (dwldsPath.exists()) {
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            Uri apkURI = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", dwldsPath);
            intent.setDataAndType(apkURI, MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            String CHANNEL_ID = "MYCHANNEL";
            final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "name", NotificationManager.IMPORTANCE_LOW);
            Notification notification = new Notification.Builder(context, CHANNEL_ID)
                    .setContentText("You have got something new!")
                    .setContentTitle("File downloaded")
                    .setContentIntent(pendingIntent)
                    .setChannelId(CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .build();
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
                notificationManager.notify(notificationId, notification);
            }
        }
        Toast.makeText(context, "FILE DOWNLOADED!", Toast.LENGTH_SHORT).show();
    }
}