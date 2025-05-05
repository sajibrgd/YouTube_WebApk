package com.sajib.youtube;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private FrameLayout rootLayout;
    private FrameLayout fullScreenContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private int originalOrientation;
    private int originalSystemUiVisibility;
    private MyWebChromeClient myWebChromeClient;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS |
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.setStatusBarColor(Color.BLACK);
            window.setNavigationBarColor(Color.BLACK);
        }

        // ✅ Keep screen always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        setContentView(R.layout.activity_main);

        rootLayout = findViewById(R.id.rootLayout);
        webView = findViewById(R.id.webView);
        fullScreenContainer = findViewById(R.id.fullScreenContainer);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient());
        myWebChromeClient = new MyWebChromeClient();
        webView.setWebChromeClient(myWebChromeClient);
        webView.loadUrl("https://m.youtube.com");

        // Handle insets to avoid padding in fullscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            rootLayout.setOnApplyWindowInsetsListener((v, insets) -> {
                if (customView == null) {
                    v.setPadding(
                            insets.getSystemWindowInsetLeft(),
                            insets.getSystemWindowInsetTop(),
                            insets.getSystemWindowInsetRight(),
                            insets.getSystemWindowInsetBottom()
                    );
                } else {
                    v.setPadding(0, 0, 0, 0);
                }
                return insets.consumeSystemWindowInsets();
            });
        }
    }

    private class MyWebChromeClient extends WebChromeClient {
        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (customView != null) {
                callback.onCustomViewHidden();
                return;
            }

            originalOrientation = getRequestedOrientation();
            originalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();

            customView = view;
            customViewCallback = callback;

            fullScreenContainer.addView(view, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            fullScreenContainer.setVisibility(View.VISIBLE);
            webView.setVisibility(View.GONE);

            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

            fullScreenContainer.postDelayed(() -> getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY), 1000);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                rootLayout.requestApplyInsets();
            }
        }

        @Override
        public void onHideCustomView() {
            if (customView == null) return;

            fullScreenContainer.removeView(customView);
            fullScreenContainer.setVisibility(View.GONE);
            customView = null;
            webView.setVisibility(View.VISIBLE);

            setRequestedOrientation(originalOrientation);
            getWindow().getDecorView().setSystemUiVisibility(originalSystemUiVisibility);

            if (customViewCallback != null) {
                customViewCallback.onCustomViewHidden();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                rootLayout.requestApplyInsets();
            }
        }

        public boolean isVideoFullscreen() {
            return customView != null;
        }

        public void forceExitFullscreen() {
            onHideCustomView();
        }
    }

    @Override
    public void onBackPressed() {
        if (myWebChromeClient != null && myWebChromeClient.isVideoFullscreen()) {
            myWebChromeClient.forceExitFullscreen();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}