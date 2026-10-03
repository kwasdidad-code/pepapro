package com.pepahaist.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final int REQ_CAMERA = 501;
    private static final int REQ_CAPTURE = 502;

    private WebView webView;
    private Uri cameraUri;
    private String cameraTarget = "progress";
    private ValueCallback<Uri[]> filePathCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowContentAccess(true);
        s.setAllowFileAccess(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setUserAgentString(s.getUserAgentString() + " PepaHaist/2.0");

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                try {
                    startActivityForResult(intent, 700);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        webView.addJavascriptInterface(new PepaBridge(), "Android");
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    public class PepaBridge {
        @JavascriptInterface
        public void openCamera(String target) {
            cameraTarget = target == null ? "progress" : target;
            runOnUiThread(() -> {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
                } else {
                    launchCamera();
                }
            });
        }

        @JavascriptInterface
        public void openSpotify() {
            runOnUiThread(() -> {
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("spotify:"));
                    startActivity(i);
                } catch (Exception e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/")));
                }
            });
        }
    }

    private void launchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File dir = new File(getCacheDir(), "camera");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, "pepa_" + System.currentTimeMillis() + ".jpg");
        cameraUri = FileProvider.getUriForFile(
                this, getPackageName() + ".fileprovider", file);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQ_CAPTURE);
        } catch (Exception e) {
            webView.evaluateJavascript("alert('Camera could not be opened on this device.')", null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_CAMERA) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) launchCamera();
            else webView.evaluateJavascript("alert('Camera permission is required for Take photo.')", null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CAPTURE) {
            if (resultCode == RESULT_OK && cameraUri != null) {
                try {
                    Bitmap bmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(cameraUri));
                    if (bmp != null) {
                        int max = 1280;
                        float scale = Math.min(1f, max / (float)Math.max(bmp.getWidth(), bmp.getHeight()));
                        if (scale < 1f) bmp = Bitmap.createScaledBitmap(bmp,
                                Math.round(bmp.getWidth()*scale), Math.round(bmp.getHeight()*scale), true);
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        bmp.compress(Bitmap.CompressFormat.JPEG, 78, out);
                        String b64 = "data:image/jpeg;base64," +
                                Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
                        String target = JSONObject.quote(cameraTarget);
                        String value = JSONObject.quote(b64);
                        webView.evaluateJavascript("window.onNativePhoto(" + value + "," + target + ")", null);
                        bmp.recycle();
                    }
                } catch (Exception ignored) {
                }
            }
            cameraUri = null;
            return;
        }

        if (requestCode == 700 && filePathCallback != null) {
            Uri result = (resultCode == RESULT_OK && data != null) ? data.getData() : null;
            filePathCallback.onReceiveValue(result == null ? null : new Uri[]{result});
            filePathCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
