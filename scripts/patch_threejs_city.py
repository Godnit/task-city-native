from pathlib import Path

p = Path('app/src/main/java/com/godnit/taskcity/MainActivity.kt')
s = p.read_text()

# Raw source has a Kotlin receiver-name collision in taskRow; fix it before compiling.
s = s.replace('val text = TextView(this).apply {', 'val taskText = TextView(this).apply {', 1)
s = s.replace('row.addView(text, LinearLayout.LayoutParams(0, dp(64), 1f))', 'row.addView(taskText, LinearLayout.LayoutParams(0, dp(64), 1f))', 1)

# Add WebView imports while keeping the old OpenGL renderer source unused as a fallback/reference.
anchor = 'import android.widget.Toast\n'
imports = '''import android.widget.Toast
import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
'''
if anchor not in s:
    raise SystemExit('import anchor not found')
s = s.replace(anchor, imports, 1)

s = s.replace('private lateinit var glView: CityGLView', 'private lateinit var cityView: CityWebView', 1)
s = s.replace('if (::glView.isInitialized) glView.onResume()', 'if (::cityView.isInitialized) cityView.onResume()', 1)
s = s.replace('if (::glView.isInitialized) glView.onPause()', 'if (::cityView.isInitialized) cityView.onPause()', 1)
s = s.replace('glView = CityGLView(this)', 'cityView = CityWebView(this)', 1)
s = s.replace('root.addView(glView, FrameLayout.LayoutParams(', 'root.addView(cityView, FrameLayout.LayoutParams(', 1)

old_refresh = '''        val count = if (mode == CityMode.NORMAL) normalHouses() else urgentHouses()
        glView.cityRenderer.cityMode = mode
        glView.cityRenderer.visibleHouseCount = count'''
new_refresh = '''        val count = if (intent.getBooleanExtra("preview_all", false)) 20
        else if (mode == CityMode.NORMAL) normalHouses() else urgentHouses()
        cityView.setCity(mode, count)'''
if old_refresh not in s:
    raise SystemExit('refresh marker not found')
s = s.replace(old_refresh, new_refresh, 1)

marker = '\nprivate class CityGLView(context: Context) : GLSurfaceView(context) {'
if marker not in s:
    raise SystemExit('CityGLView marker not found')

webview = r'''
@SuppressLint("SetJavaScriptEnabled")
private class CityWebView(context: Context) : WebView(context) {
    private var loaded = false
    private var pendingMode = CityMode.NORMAL
    private var pendingCount = 0

    init {
        setBackgroundColor(Color.rgb(121, 211, 250))
        isHorizontalScrollBarEnabled = false
        isVerticalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = false
        settings.databaseEnabled = false
        settings.allowFileAccess = true
        settings.allowContentAccess = false
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        settings.mediaPlaybackRequiresUserGesture = true
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                loaded = true
                pushState()
            }
        }
        loadUrl("file:///android_asset/web/index.html")
    }

    fun setCity(mode: CityMode, count: Int) {
        pendingMode = mode
        pendingCount = count.coerceIn(0, 20)
        if (loaded) pushState()
    }

    private fun pushState() {
        val jsMode = if (pendingMode == CityMode.URGENT) "urgent" else "normal"
        post {
            evaluateJavascript("window.setCity('$jsMode', $pendingCount);", null)
        }
    }
}
'''
s = s.replace(marker, webview + marker, 1)

p.write_text(s)
print('TASK_CITY_THREEJS_PATCH_OK')
