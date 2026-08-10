package com.godnit.learningcreature;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyImmersiveMode();

        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(7, 16, 27));
        window.setNavigationBarColor(Color.rgb(7, 16, 27));
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(7, 16, 27));
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(true);
        settings.setDefaultTextEncodingName("UTF-8");
        settings.setTextZoom(100);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String js = "(function(){" +
                    "var st=document.createElement('style');" +
                    "st.id='landscape-fullscreen-style';" +
                    "st.textContent=`" +
                    "html,body{width:100%;height:100%;margin:0!important;overflow:hidden!important;background:#07101b!important;}" +
                    ".app{position:relative!important;width:100vw!important;height:100vh!important;max-width:none!important;margin:0!important;padding:0!important;overflow:hidden!important;}" +
                    ".top,.metrics,.grid{display:none!important;}" +
                    ".stage{position:absolute!important;inset:0!important;margin:0!important;padding:0!important;border:0!important;border-radius:0!important;background:#0b1725!important;overflow:hidden!important;}" +
                    ".canvasWrap{position:absolute!important;inset:0!important;width:100vw!important;height:100vh!important;min-height:0!important;aspect-ratio:auto!important;border-radius:0!important;background:#142332!important;}" +
                    ".canvasWrap canvas{display:block!important;width:100%!important;height:100%!important;}" +
                    ".observer{top:10px!important;right:10px!important;bottom:auto!important;background:rgba(5,12,20,.64)!important;backdrop-filter:blur(4px);}" +
                    ".controls{position:absolute!important;z-index:40!important;left:10px!important;bottom:10px!important;margin:0!important;padding:6px!important;gap:5px!important;border:1px solid rgba(90,130,165,.55)!important;border-radius:14px!important;background:rgba(8,19,31,.78)!important;backdrop-filter:blur(5px);}" +
                    ".controls label{display:none!important;}" +
                    ".controls button,.controls select{padding:7px 10px!important;border-radius:10px!important;font-size:12px!important;}" +
                    ".thoughtStrip{position:absolute!important;z-index:35!important;right:12px!important;left:270px!important;bottom:10px!important;margin:0!important;padding:8px 12px!important;min-height:48px!important;border-radius:13px!important;background:rgba(8,25,36,.78)!important;backdrop-filter:blur(5px);}" +
                    ".thoughtStrip small{font-size:10px!important;}" +
                    ".thoughtStrip div{font-size:14px!important;line-height:1.45!important;margin-top:2px!important;}" +
                    "@media(max-width:760px){.thoughtStrip{left:225px!important;}.controls button,.controls select{padding:6px 8px!important;font-size:11px!important;}.thoughtStrip div{font-size:12px!important;}}" +
                    "`;document.head.appendChild(st);" +
                    "document.documentElement.style.background='#07101b';" +
                    "document.body.style.background='#07101b';" +
                    "})();";
                view.evaluateJavascript(js, null);
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void applyImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersiveMode();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
