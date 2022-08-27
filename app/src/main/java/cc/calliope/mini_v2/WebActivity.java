package cc.calliope.mini_v2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import cc.calliope.mini_v2.adapter.ExtendedBluetoothDevice;
import cc.calliope.mini_v2.viewmodels.DeviceViewModel;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Base64;
import android.util.Log;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;

public class WebActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        super.onCreate(savedInstanceState);

        WebView webView = new WebView(this);
        setContentView(webView);

        final Intent intent = getIntent();
        final ExtendedBluetoothDevice device = intent.getParcelableExtra("cc.calliope.mini.EXTRA_DEVICE");
        if (device != null) {
            Log.i("WEB", "Device: " + device.getPattern());
        }
        Bundle extras = intent.getExtras();
        final String url = extras.getString("TARGET_URL");
        final String editorName = extras.getString("TARGET_NAME");

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setDatabaseEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(webView, url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                Toast.makeText(getApplicationContext(), "Oh no! " + error.getDescription(), Toast.LENGTH_SHORT).show();
            }
        });

        webView.loadUrl(url);
        webView.setDownloadListener((url1, userAgent, contentDisposition, mimetype, contentLength) -> {

            Uri uri = Uri.parse(url1);
            Log.i("URL", url1);
            Log.i("URI", "" + uri);

            //String filetype = url.substring(url.indexOf("/") + 1, url.indexOf(";"));
            String filename = editorName + "-" + System.currentTimeMillis() + ".hex";

            File file = new File(getFilesDir() + File.separator + filename);
            if (file.exists()) {
                file.delete();
            }

            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
                Log.w("CreateFile", "Error writing " + file);
            }

            Boolean downloadResult = false;

            if (url1.startsWith("blob:")) {  // TODO: BLOB Download
                Log.i("MODUS", "BLOB");
                // Can not be parsed
            } else if (url1.startsWith("data:text/hex")) {  // when url is base64 encoded data
                Log.i("MODUS", "HEX");
                downloadResult = createAndSaveFileFromHexUrl(url1, file);
            } else if (url1.startsWith("data:") && url1.contains("base64")) {  // when url is base64 encoded data
                Log.i("MODUS", "BASE64");
                downloadResult = createAndSaveFileFromBase64Url(url1, file);
            } else if (URLUtil.isValidUrl(url1)) { // real download
                Log.i("MODUS", "DOWNLOAD");
                downloadResult = downloadFileFromURL(url1, file);
            }

            if (downloadResult && device != null) {
                Log.e("WEB", "start DFU Activity");
//                    final Intent intent = new Intent(this, DFUActivity.class);
//                    intent.putExtra("cc.calliope.mini.EXTRA_DEVICE", device);
//                    intent.putExtra("EXTRA_FILE", file.getAbsolutePath());
//                    startActivity(intent);
            } else if (downloadResult) {
                Toast.makeText(getApplicationContext(), "R.string.upload_no_mini_connected", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "R.string.download_error", Toast.LENGTH_LONG).show();
            }
        });
    }


    public Boolean createAndSaveFileFromBase64Url(String url, File file) {
        try {
            String base64EncodedString = url.substring(url.indexOf(",") + 1);
            byte[] decodedBytes = Base64.decode(base64EncodedString, Base64.DEFAULT);
            OutputStream os = new FileOutputStream(file);
            os.write(decodedBytes);
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        Log.i("GESPEICHERT", file.toString());
        return true;
    }


    public Boolean createAndSaveFileFromHexUrl(String url, File file) { // TODO not working yet
        try {
            String hexEncodedString = url.substring(url.indexOf(",") + 1);
            String decodedHex = URLDecoder.decode(hexEncodedString, "utf-8");
            OutputStream os = new FileOutputStream(file);
            try (Writer w = new OutputStreamWriter(os, "UTF-8")) {
                w.write(decodedHex);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        Log.i("GESPEICHERT", file.toString());
        return true;
    }


    public Boolean downloadFileFromURL(final String link, File file) {
        try {
            URL url = new URL(link);
            URLConnection ucon = url.openConnection();
            ucon.setReadTimeout(5000);
            ucon.setConnectTimeout(10000);

            InputStream is = ucon.getInputStream();
            BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);
            FileOutputStream outStream = new FileOutputStream(file);
            byte[] buff = new byte[5 * 1024];
            int len;
            while ((len = inStream.read(buff)) != -1) {
                outStream.write(buff, 0, len);
            }
            outStream.flush();
            outStream.close();
            inStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}