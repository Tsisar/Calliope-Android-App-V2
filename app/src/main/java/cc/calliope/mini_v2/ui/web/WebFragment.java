package cc.calliope.mini_v2.ui.web;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import cc.calliope.mini_v2.DFUActivity;
import cc.calliope.mini_v2.R;
import cc.calliope.mini_v2.adapter.ExtendedBluetoothDevice;
import cc.calliope.mini_v2.utils.Utils;
import cc.calliope.mini_v2.viewmodels.ScannerViewModel;

import android.os.StrictMode;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
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
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;

import org.apache.commons.io.FilenameUtils;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WebFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WebFragment extends Fragment implements DownloadListener {

    private static final String TAG = "WEB_VIEW";

    private static final String TARGET_URL = "TARGET_URL";
    private static final String TARGET_NAME = "TARGET_NAME";

    private static final String FILE_EXTENSION = ".hex";

    private String editorUrl;
    private String editorName;

    private WebView webView;

    private ExtendedBluetoothDevice device;

    public WebFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param editorName Editor name.
     * @param url        Editor URL.
     * @return A new instance of fragment WebFragment.
     */
    public static WebFragment newInstance(String url, String editorName) {
        WebFragment fragment = new WebFragment();
        Bundle args = new Bundle();
        args.putString(TARGET_URL, url);
        args.putString(TARGET_NAME, editorName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Bundle arguments = getArguments();
        if (arguments != null) {
            editorUrl = arguments.getString(TARGET_URL);
            editorName = arguments.getString(TARGET_NAME);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_web, container, false);

        ScannerViewModel scannerViewModel = new ViewModelProvider(requireActivity()).get(ScannerViewModel.class);
        scannerViewModel.getScannerState().observe(getViewLifecycleOwner(), result -> device = result.getCurrentDevice());

        webView = view.findViewById(R.id.webView);
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(editorUrl);
        }

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setDatabaseEnabled(true);

        settings.setDefaultTextEncodingName("utf-8");
        webView.addJavascriptInterface(new JavaScriptInterface(getContext()), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(webView, url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Utils.errorSnackbar(webView, "Oh no! " + error.getDescription()).show();
                }else {
                    Utils.errorSnackbar(webView, "Oh no! onReceivedError").show();
                }
            }
        });

        webView.setDownloadListener(this);
        return view;
    }

    //TODO завантажувати xml і ділитися ними

    @Override
    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        Log.v(TAG, "URL: " + url);
        Log.v(TAG, "mimetype: " + mimetype);

        selectDownloadMethod(url, mimetype);
    }

    private void selectDownloadMethod(String url, String mimetype) {
        boolean result = false;
        File file = null;

        if (url.startsWith("blob:")) {  // TODO: BLOB Download
            Log.w(TAG, "BLOB");

            String javaScript = JavaScriptInterface.getBase64StringFromBlobUrl(url, mimetype);
            webView.loadUrl(javaScript);
            Log.v(TAG, "JS: " + javaScript);
            return;
        } else if (url.startsWith("data:text/hex")){
            Log.w(TAG, "HEX");

            file = getFile("firmware");
            if(file != null) {
                result = createAndSaveFileFromHexUrl(url, file);
            }
        } else if (url.startsWith("data:") && url.contains("base64")){
            Log.w(TAG, "BASE64");

            String name = Utils.getFileNameFromPrefix(url);

            file = getFile(name);
            if(file != null) {
                result = createAndSaveFileFromBase64Url(url, file);
            }
        } else if (URLUtil.isValidUrl(url) && url.endsWith(".hex")){
            Log.w(TAG, "DOWNLOAD");

            String name = FilenameUtils.getBaseName(url);
            String extension = "." + FilenameUtils.getExtension(url);

            file = getFile(name, extension);
            if(file != null) {
                result = downloadFileFromURL(url, file);
            }
        }

        if (result) {
            startDFUActivity(file);
        } else if (file != null && !file.delete()) {
            Log.w(TAG, "Delete Error, deleting: " + file);
        } else {
            Utils.errorSnackbar(webView, "Download error").show();
        }
    }

    private File getFile(String filename) {
        return getFile(filename, FILE_EXTENSION);
    }

    //TODO db
    // String file absolute path
    // String editor name
    // boolean don't ask
    // boolean rewrite
    private File getFile(String filename, String extension) {
        Activity activity = getActivity();
        if (activity == null)
            return null;

        File dir = new File(activity.getFilesDir().toString() + File.separator + editorName);
        if(!dir.exists() && !dir.mkdirs()){
            return null;
        }

        Log.e(TAG, "DIR: " + dir);
        File file = new File(dir.getAbsolutePath() + File.separator + filename + extension);

        int i = 1;
        while (file.exists()) {
            String number = String.format("(%s)", ++i);
            file = new File(dir.getAbsolutePath() + File.separator + filename + number + extension);
        }

        try {
            if (file.createNewFile()) {
                Log.e(TAG, "createNewFile: " + file);
                return file;
            } else {
                Log.w(TAG, "CreateFile Error, deleting: " + file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private File createFile(){
        return null;
    }

    public boolean createAndSaveFileFromHexUrl(String url, File file) {
        try {
            String hexEncodedString = url.substring(url.indexOf(",") + 1);
            String decodedHex = URLDecoder.decode(hexEncodedString, "utf-8");
            OutputStream outputStream = new FileOutputStream(file);
            try (Writer writer = new OutputStreamWriter(outputStream, "UTF-8")) {
                writer.write(decodedHex);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        Log.i(TAG, "createAndSaveFileFromHexUrl: " + file.toString());
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
        Log.i(TAG, "createAndSaveFileFromBase64Url: " + file.toString());
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
        Log.i(TAG, "downloadFileFromURL: " + file.toString());
        return true;
    }

    private void startDFUActivity(File file) {
        if (device != null && device.isRelevant()) {
            Log.e(TAG, "start DFU Activity");
            final Intent intent = new Intent(getActivity(), DFUActivity.class);
            intent.putExtra("cc.calliope.mini.EXTRA_DEVICE", device);
            intent.putExtra("EXTRA_FILE", file.getAbsolutePath());
            startActivity(intent);
        } else {
            Utils.errorSnackbar(webView, "No mini connected").show();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }
}