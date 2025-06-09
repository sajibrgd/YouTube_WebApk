package com.sajib.youtube;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private FrameLayout rootLayout;
    private FrameLayout fullScreenContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private int originalOrientation;
    private int originalSystemUiVisibility;
    private MyWebChromeClient myWebChromeClient;

    private ImageView swipeIndicator;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private float edgeZonePx;
    private float downX, downY;
    private boolean isEdgeSwipe = false;
    private final float SWIPE_THRESHOLD = 100f;

    private static final Set<String> blockedHosts = new HashSet<>();

    static {
        String[] hosts = new String[]{
                "ads.google.com", "pagead2.googlesyndication.com", "googlesyndication.com",
                "youtubeads.google.com", "youtubeads.googleapis.com", "googleads.g.doubleclick.net",
                "doubleclick.net", "google-analytics.com", "analytics.google.com",
                "adservice.google.com", "adservice.google.com.bd", "partner.googleadservices.com",
                "googletagmanager.com", "static.doubleclick.net", "googleads4.g.doubleclick.net",
                "pagead-googlehosted.l.google.com", "video-ad-stats.googlesyndication.com",
                "ade.googlesyndication.com", "partnerad.l.doubleclick.net", "mtalk.google.com",
                "beacons.gvt2.com", "ads.yahoo.com", "adserver.yahoo.com",
                "ads.youtube.com", "pixel.facebook.com", "connect.facebook.net", "facebook.com",
                "track.adform.net", "adform.net", "ads.twitter.com", "adservice.google.co.in",
                "g.doubleclick.net"
        };
        for (String host : hosts) blockedHosts.add(host);
    }

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

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        rootLayout = findViewById(R.id.rootLayout);
        webView = findViewById(R.id.webView);
        fullScreenContainer = findViewById(R.id.fullScreenContainer);
        swipeIndicator = findViewById(R.id.swipeIndicator);

        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new BlockingWebViewClient());
        myWebChromeClient = new MyWebChromeClient();
        webView.setWebChromeClient(myWebChromeClient);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.loadUrl("https://m.youtube.com");

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

        edgeZonePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics()
        );

        webView.setOnTouchListener((v, event) -> {
            if (customView != null) return false;

            float screenWidth = getResources().getDisplayMetrics().widthPixels;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getX();
                    downY = event.getY();
                    isEdgeSwipe = (downX <= edgeZonePx) || (downX >= (screenWidth - edgeZonePx));
                    break;

                case MotionEvent.ACTION_UP:
                    if (!isEdgeSwipe) break;

                    float deltaX = event.getX() - downX;
                    float deltaY = event.getY() - downY;

                    if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > SWIPE_THRESHOLD) {
                        if (deltaX > 0 && downX <= edgeZonePx && webView.canGoBack()) {
                            webView.goBack();
                            showSwipeIndicator(true);
                            return true;
                        } else if (deltaX < 0 && downX >= (screenWidth - edgeZonePx) && webView.canGoForward()) {
                            webView.goForward();
                            showSwipeIndicator(false);
                            return true;
                        }
                    }
                    break;
            }
            return false;
        });
    }
    private void showSwipeIndicator(boolean isBack) {
        Drawable icon = ContextCompat.getDrawable(this,
                isBack ? R.drawable.ic_arrow_back : R.drawable.ic_arrow_forward);
        swipeIndicator.setImageDrawable(icon);
        swipeIndicator.setVisibility(View.VISIBLE);
        handler.postDelayed(() -> swipeIndicator.setVisibility(View.GONE), 1000);
    }

    private class BlockingWebViewClient extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String host = request.getUrl().getHost();
            if (host != null && isBlockedHost(host)) {
                return new WebResourceResponse("text/plain", "utf-8",
                        new ByteArrayInputStream("".getBytes()));
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            String js = "javascript:(function() { " +
                    "if (window.MediaSource && MediaSource.isTypeSupported) { " +
                    "   const original = MediaSource.isTypeSupported.bind(MediaSource); " +
                    "   const blocked = ['vp8', 'vp9', 'vp09']; " +
                    "   MediaSource.isTypeSupported = function(type) { " +
                    "       if (!type) return false; " +
                    "       const lower = type.toLowerCase(); " +
                    "       for (let codec of blocked) { " +
                    "           if (lower.includes(codec)) return false; " +
                    "       } return original(type); }; } })();";
            view.evaluateJavascript(js, null);
        }

        private boolean isBlockedHost(String host) {
            for (String blocked : blockedHosts) {
                if (host.equals(blocked) || host.endsWith("." + blocked)) return true;
            }
            return false;
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
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            fullScreenContainer.setVisibility(View.VISIBLE);
            webView.setVisibility(View.GONE);

            // 🔍 JavaScript to detect actual video ratio
            String ratioCheckJS = "(function() {" +
                    "var v = document.querySelector('video');" +
                    "if (v && v.videoWidth && v.videoHeight) {" +
                    " return (v.videoWidth / v.videoHeight).toFixed(2); }" +
                    "return '0'; })();";

            webView.evaluateJavascript(ratioCheckJS, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    try {
                        float ratio = Float.parseFloat(value.replace("\"", ""));
                        if (ratio > 1.0f) {
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        } else {
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        }
                    } catch (Exception e) {
                        // Fallback
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    }

                    fullScreenContainer.postDelayed(() -> getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY), 1000);
                }
            });

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
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            myWebChromeClient.onHideCustomView();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            // Show confirmation dialog before exiting
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Exit App")
                    .setMessage("Are you sure you want to exit?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Call super to allow default back behavior (exit)
                        super.onBackPressed();
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
    }
}
