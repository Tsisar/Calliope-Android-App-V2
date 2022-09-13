package cc.calliope.mini_v2.ui.web;

import android.content.Intent;
import android.net.Uri;
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

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WebFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WebFragment extends Fragment implements DownloadListener {

    private static final String TARGET_URL = "TARGET_URL";
    private static final String TARGET_NAME = "TARGET_NAME";

    private String url;
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
        if (getArguments() != null) {
            url = getArguments().getString(TARGET_URL);
            editorName = getArguments().getString(TARGET_NAME);
        }
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_web, container, false);

        ScannerViewModel scannerViewModel = new ViewModelProvider(requireActivity()).get(ScannerViewModel.class);
        scannerViewModel.getScannerState().observe(getViewLifecycleOwner(), result -> this.device = result.getCurrentDevice());

        webView = view.findViewById(R.id.webView);

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(url);
        }

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
                Utils.showErrorMessage(webView, "Oh no! " + error.getDescription());
            }
        });

        webView.setDownloadListener(this);
        return view;
    }

    @Override
    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        Uri uri = Uri.parse(url);
        Boolean downloadResult = false;

        Log.i("URL", url);
        Log.i("URI", "" + uri);

        //String filetype = url.substring(url.indexOf("/") + 1, url.indexOf(";"));
        String filename = editorName + "-" + System.currentTimeMillis() + ".hex";

        File file = new File(getActivity().getFilesDir() + File.separator + filename);
        if (file.exists()) {
            file.delete();
        }

        try {
            file.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
            Log.w("CreateFile", "Error writing " + file);
        }

        if (url.startsWith("blob:")) {  // TODO: BLOB Download
            Log.i("MODUS", "BLOB");
            // Can not be parsed
        } else if (url.startsWith("data:text/hex")) {  // when url is base64 encoded data
            Log.i("MODUS", "HEX");
            downloadResult = createAndSaveFileFromHexUrl(url, file);
        } else if (url.startsWith("data:") && url.contains("base64")) {  // when url is base64 encoded data
            Log.i("MODUS", "BASE64");
            downloadResult = createAndSaveFileFromBase64Url(url, file);
        } else if (URLUtil.isValidUrl(url)) { // real download
            Log.i("MODUS", "DOWNLOAD");
            downloadResult = downloadFileFromURL(url, file);
        }

        startDFUActivity(file, downloadResult);
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


    public Boolean createAndSaveFileFromHexUrl(String url, File file) {
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

    private void startDFUActivity(File file, boolean res) {
        if (res && device != null) {
            Log.e("WEB", "start DFU Activity");
            final Intent intent2 = new Intent(getActivity(), DFUActivity.class);
            intent2.putExtra("cc.calliope.mini.EXTRA_DEVICE", device);
            intent2.putExtra("EXTRA_FILE", file.getAbsolutePath());
            startActivity(intent2);
        } else if (res) {
            Utils.showErrorMessage(webView, "No mini connected");
        } else {
            Utils.showErrorMessage(webView, "Download error");
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }
}