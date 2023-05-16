package cc.calliope.mini_v2.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import androidx.annotation.NonNull;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.adapter.ExtendedBluetoothDevice;
import cc.calliope.mini_v2.databinding.ActivityWebBinding;
import cc.calliope.mini_v2.dialog.scripts.ScriptsFragment;
import cc.calliope.mini_v2.utils.FileUtils;
import cc.calliope.mini_v2.utils.StaticExtra;
import cc.calliope.mini_v2.utils.Utils;
import cc.calliope.mini_v2.utils.Version;
import cc.calliope.mini_v2.views.FabMenuView;

public class WebActivity extends ScannerActivity implements DownloadListener {
    private static final String TAG = "WebActivity";
    private static final String UTF_8 = "UTF-8";
    private ActivityWebBinding binding;
    private Boolean isFullScreen = false;
    private String editorUrl;
    private String editorName;
    private WebView webView;
    private ExtendedBluetoothDevice device;

    public void log(int priority, @NonNull String message) {
        // Log from here.
        Log.println(priority, TAG, "### " + Thread.currentThread().getId() + " # " + message);
    }

    private class JavaScriptInterface {
        private final Context context;
        public JavaScriptInterface(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public void getBase64FromBlobData(String url, String name) {
            log(Log.DEBUG, "base64Data: " + url);
            log(Log.DEBUG, "name: " + name);

            File file = FileUtils.getFile(context, editorName, name);
            if (file == null) {
                Utils.errorSnackbar(binding.getRoot(), getString(R.string.error_snackbar_save_file_error)).show();
            } else {
                if (createAndSaveFileFromBase64Url(url, file)) {
                    startDFUActivity(file);
                } else {
                    Utils.errorSnackbar(binding.getRoot(), getString(R.string.error_snackbar_download_error)).show();
                }
            }
        }

        public static String getBase64StringFromBlobUrl(String blobUrl, String mimeType) {
            if (blobUrl.startsWith("blob")) {
                return "javascript: " +
                        "var xhr = new XMLHttpRequest();" +
                        "xhr.open('GET', '" + blobUrl + "', true);" +
                        "xhr.setRequestHeader('Content-type','" + mimeType + ";charset=UTF-8');" +
                        "xhr.responseType = 'blob';" +
                        "xhr.onload = function(e) {" +
                        "    if (this.status == 200) {" +
                        "        var blobFile = this.response;" +
                        "        var name = blobFile.name;" +
                        "        var reader = new FileReader();" +
                        "        reader.readAsDataURL(blobFile);" +
                        "        reader.onloadend = function() {" +
                        "            base64data = reader.result;" +
                        "            Android.getBase64FromBlobData(base64data, name);" +
                        "        }" +
                        "    }" +
                        "};" +
                        "xhr.send();";
            }
            return "javascript: console.log('It is not a Blob URL');";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityWebBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Intent intent = getIntent();
        editorUrl = intent.getExtras().getString(StaticExtra.EXTRA_URL);
        editorName = intent.getExtras().getString(StaticExtra.EXTRA_EDITOR_NAME);

        createWebView(savedInstanceState);
        setPatternFab(binding.patternFab);
    }

    @Override
    protected void setDevice(ExtendedBluetoothDevice device) {
        super.setDevice(device);
        this.device = device;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void createWebView(Bundle savedInstanceState){
        webView = binding.webView;
        WebSettings webSettings = webView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDefaultTextEncodingName("utf-8");

        webView.addJavascriptInterface(new JavaScriptInterface(this), "Android");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (Version.upperMarshmallow) {
                    Utils.errorSnackbar(webView, "Oh no! " + error.getDescription()).show();
                } else {
                    Utils.errorSnackbar(webView, "Oh no! onReceivedError").show();
                }
            }
        });
        webView.setDownloadListener(this);

        if (savedInstanceState != null) {
            log(Log.INFO, "restoreState: " + savedInstanceState.getBundle("webViewState"));
            webView.restoreState(savedInstanceState.getBundle("webViewState"));
        } else {
            webView.loadUrl(editorUrl);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        disableFullScreenMode();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override
    public void onBackPressed() {
        if (isFullScreen) {
            disableFullScreenMode();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onItemFabMenuClicked(View view) {
        super.onItemFabMenuClicked(view);
        if (view.getId() == R.id.fabFullScreen) {
            if (isFullScreen) {
                disableFullScreenMode();
            } else {
                enableFullScreenMode();
            }
        } else if (view.getId() == R.id.fabScripts) {
            ScriptsFragment scriptsFragment = new ScriptsFragment();
            scriptsFragment.show(getSupportFragmentManager(), "Bottom Sheet Dialog Fragment");
        }
    }

    @Override
    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        log(Log.INFO, "editorName: " + editorName);
        log(Log.INFO, "URL: " + url);
        log(Log.INFO, "userAgent: " + userAgent);
        log(Log.INFO, "contentDisposition: " + contentDisposition);
        log(Log.INFO, "mimetype: " + mimetype);
        log(Log.INFO, "contentLength: " + contentLength);

        try {
            String decodedUrl = URLDecoder.decode(url, UTF_8);
            if (decodedUrl.startsWith("blob:")) {
                String javaScript = JavaScriptInterface.getBase64StringFromBlobUrl(url, mimetype);
                log(Log.DEBUG, "javaScript: " + javaScript);
                webView.loadUrl(javaScript);
            } else {
                selectDownloadMethod(decodedUrl);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        log(Log.WARN, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        Bundle bundle = new Bundle();
        webView.saveState(bundle);
        outState.putBundle("webViewState", bundle);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        log(Log.WARN, "onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        log(Log.WARN, "onConfigurationChanged");
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            log(Log.WARN, "ORIENTATION_LANDSCAPE");
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            log(Log.WARN, "ORIENTATION_PORTRAIT");
        }
        super.onConfigurationChanged(newConfig);
    }

    private void selectDownloadMethod(String url) {
        String name = FileUtils.getFileName(url);
        File file = FileUtils.getFile(this, editorName, name);
        boolean result = false;

        if (file == null) {
            Utils.errorSnackbar(webView, getString(R.string.error_snackbar_save_file_error)).show();
        } else {
            if (url.startsWith("data:text/hex")) {
                result = createAndSaveFileFromHexUrl(url, file);
            } else if (url.startsWith("data:") && url.contains("base64")) {
                result = createAndSaveFileFromBase64Url(url, file);
            } else if (URLUtil.isValidUrl(url) && url.endsWith(".hex")) {
                result = downloadFileFromURL(url, file);
            }
            if (result) {
                startDFUActivity(file);
            } else {
                Utils.errorSnackbar(webView, getString(R.string.error_snackbar_download_error)).show();
            }
        }
    }

    public boolean createAndSaveFileFromHexUrl(String url, File file) {
        try {
            String hexEncodedString = url.substring(url.indexOf(",") + 1);
            OutputStream outputStream = new FileOutputStream(file);
            try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                writer.write(hexEncodedString);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        log(Log.INFO, "createAndSaveFileFromHexUrl: " + file.toString());
        return true;
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
        log(Log.INFO, "createAndSaveFileFromBase64Url: " + file.toString());
        return true;
    }

    public Boolean downloadFileFromURL(String link, File file) {
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
        log(Log.INFO, "downloadFileFromURL: " + file.toString());
        return true;
    }

    private void startDFUActivity(File file) {
        if (device != null && device.isRelevant()) {
            log(Log.INFO, "start DFU Activity");
            final Intent intent = new Intent(this, FlashingActivity.class);
            intent.putExtra(StaticExtra.EXTRA_DEVICE, device);
            intent.putExtra(StaticExtra.EXTRA_FILE_PATH, file.getAbsolutePath());
            startActivity(intent);
        } else {
            Utils.errorSnackbar(webView, getString(R.string.error_snackbar_no_connected)).show();
        }
    }

    private void enableFullScreenMode() {
        isFullScreen = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void disableFullScreenMode() {
        isFullScreen = false;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public void customizeFabMenu(FabMenuView fabMenuView) {
        fabMenuView.setScriptsVisibility(View.VISIBLE);
        fabMenuView.setFullScreenVisibility(View.VISIBLE);
        fabMenuView.setFullScreenImageResource(isFullScreen ?
                R.drawable.ic_disable_full_screen_24dp :
                R.drawable.ic_enable_full_screen_24dp);
    }
}