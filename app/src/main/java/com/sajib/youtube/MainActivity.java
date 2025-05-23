package com.sajib.youtube;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.CookieManager;
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

    private GestureDetector gestureDetector;
    private ImageView swipeIndicator;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Blocked hosts list (ads + trackers)
    private static final Set<String> blockedHosts = new HashSet<>();

    static {
        String[] hosts = new String[]{
                "ads.google.com",
                "pagead2.googlesyndication.com",
                "googlesyndication.com",
                "youtubeads.google.com",
                "youtubeads.googleapis.com",
                "googleads.g.doubleclick.net",
                "doubleclick.net",
                "google-analytics.com",
                "analytics.google.com",
                "adservice.google.com",
                "adservice.google.com.bd",
                "partner.googleadservices.com",
                "googletagmanager.com",
                "static.doubleclick.net",
                "googleads4.g.doubleclick.net",
                "pagead-googlehosted.l.google.com",
                "video-ad-stats.googlesyndication.com",
                "ade.googlesyndication.com",
                "partnerad.l.doubleclick.net",
                "mtalk.google.com",
                "mtalk.google.com:5228",
                "beacons.gvt2.com",
                "beacons.gcp.gvt2.com",
                "gcp.gvt2.com",
                "e2c77.gcp.gvt2.com",
                "ads.yahoo.com",
                "adserver.yahoo.com",
                "ads.youtube.com",
                "pixel.facebook.com",
                "connect.facebook.net",
                "facebook.com",
                "track.adform.net",
                "adform.net",
                "ads.twitter.com",
                "adservice.google.co.in",
                "g.doubleclick.net",
                "pubads.g.doubleclick.net",
                "securepubads.g.doubleclick.net",
                "cdn.adsafeprotected.com",
                "ad.doubleclick.net",
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
                "www.googleadservices.com",
                "ads.crwdcntrl.net",
                "ads.adaptv.advertising.com",
                "s0.2mdn.net",
                "cdn.taboola.com",
                "trc.taboola.com",
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
        swipeIndicator = findViewById(R.id.swipeIndicator);

//  Fix 1: Set overscroll mode to avoid unnecessary redraws during scroll
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

//  Fix 2: Enable hardware acceleration (already added, good!)
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

//  Fix 3: Smarter caching to reduce unnecessary buffering
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

// Set custom WebViewClient that blocks ads & trackers and injects h264ify JS
        webView.setWebViewClient(new BlockingWebViewClient());

        myWebChromeClient = new MyWebChromeClient();
        webView.setWebChromeClient(myWebChromeClient);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true); // Important for YouTube

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

        // Setup gesture detector for swipe left/right
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY) &&
                        Math.abs(diffX) > SWIPE_THRESHOLD &&
                        Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {

                    if (diffX > 0) {
                        // Swipe right → go back
                        if (webView.canGoBack()) {
                            webView.goBack();
                            showSwipeIndicator(true);
                        }
                    } else {
                        // Swipe left → go forward
                        if (webView.canGoForward()) {
                            webView.goForward();
                            showSwipeIndicator(false);
                        }
                    }
                    return true;
                }
                return false;
            }
        });

        // Attach touch listener on WebView to detect swipe gestures
        webView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false; // Let WebView handle other touch events
        });
    }

    private void showSwipeIndicator(boolean isBack) {
        Drawable icon;
        if (isBack) {
            icon = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back);
        } else {
            icon = ContextCompat.getDrawable(this, R.drawable.ic_arrow_forward);
        }
        swipeIndicator.setImageDrawable(icon);
        swipeIndicator.setVisibility(View.VISIBLE);

        // Hide after 1.0 seconds without animation
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            swipeIndicator.setVisibility(View.GONE);
        }, 1000);
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

        private boolean isBlockedHost(String host) {
            for (String blocked : blockedHosts) {
                if (host.equals(blocked) || host.endsWith("." + blocked)) {
                    return true;
                }
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


    // Handle back press: exit fullscreen first
    @Override
    public void onBackPressed() {
        if (customView != null) {
            myWebChromeClient.onHideCustomView();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}