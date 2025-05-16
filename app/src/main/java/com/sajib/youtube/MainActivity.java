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
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

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

    // Blocked hosts list (ads + trackers, can be extended)
    private static final Set<String> blockedHosts = new HashSet<>();

    static {
        String[] hosts = new String[]{
                // Google Ads & Analytics
                "ads.google.com",
                "pagead2.googlesyndication.com",
                "googlesyndication.com",
                "doubleclick.net",
                "google-analytics.com",
                "analytics.google.com",
                "adservice.google.com",
                "partner.googleadservices.com",
                "googletagmanager.com",
                // Yahoo Ads
                "ads.yahoo.com",
                "adserver.yahoo.com",
                // YouTube Ads
                "ads.youtube.com",
                // Facebook & Meta
                "pixel.facebook.com",
                "connect.facebook.net",
                "facebook.com",
                // Adform
                "track.adform.net",
                "adform.net",
                // Twitter Ads
                "ads.twitter.com",
                // Other ad networks
                "adservice.google.co.in",
                "securepubads.g.doubleclick.net",
                "cdn.adsafeprotected.com",
                "ads.pubmatic.com",
                "ads.rubiconproject.com",
                "ads.exoclick.com",
                "ads.adroll.com",
                "ads.adnetmedia.net",
                "adserver.adtechus.com",
                "ads.contextweb.com",
                "ads.media.net",
                "tags.bluekai.com",
                "pixel.quantserve.com",
                "www.googletagservices.com",
                "cm.g.doubleclick.net",
                "adclick.g.doubleclick.net",
                "tpc.googlesyndication.com",
                "bid.g.doubleclick.net",
                "secure-ds.serving-sys.com",
                "ads.crwdcntrl.net",
                "ads.pubmatic.com",
                "ads.adaptv.advertising.com",
                "s0.2mdn.net",
                "cdn.taboola.com",
                "trc.taboola.com",
                // Common trackers and analytics
                "trackersandbox.com",
                "adsafeprotected.com",
                "quantcast.com",
                "scorecardresearch.com",
                "adnxs.com",
                "advertising.com",
                "criteo.com",
                "imrworldwide.com",
                "outbrain.com",
                "revcontent.com",
                "zedo.com"
        };
        for (String host : hosts) {
            blockedHosts.add(host);
        }
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

        // Keep screen always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        rootLayout = findViewById(R.id.rootLayout);
        webView = findViewById(R.id.webView);
        fullScreenContainer = findViewById(R.id.fullScreenContainer);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        // Set custom WebViewClient that blocks ads & trackers and injects h264ify JS
        webView.setWebViewClient(new BlockingWebViewClient());

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

    // Custom WebViewClient that blocks requests to ad/tracker hosts and injects JS to force H264 codec
    private class BlockingWebViewClient extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String host = request.getUrl().getHost();
            if (host != null && isBlockedHost(host)) {
                // Block request by returning an empty response
                return new WebResourceResponse("text/plain", "utf-8",
                        new ByteArrayInputStream("".getBytes()));
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            // Inject JavaScript to mimic h264ify extension by disabling VP8/VP9 codecs
            String js = "javascript:(function() { " +
                    "let original = window.MediaSource && window.MediaSource.isTypeSupported;" +
                    "if (original) {" +
                    "   let blocked = ['vp8', 'vp9', 'webm'];" +
                    "   MediaSource.isTypeSupported = function(type) {" +
                    "       for (let codec of blocked) {" +
                    "           if (type.includes(codec)) return false;" +
                    "       }" +
                    "       return original.call(this, type);" +
                    "   }" +
                    "}" +
                    "})();";

            view.evaluateJavascript(js, null);
        }

        // Check if the host is in blockedHosts
        private boolean isBlockedHost(String host) {
            return blockedHosts.contains(host);
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
