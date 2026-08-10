from pathlib import Path

p = Path('app/src/main/java/com/godnit/taskcity/MainActivity.kt')
s = p.read_text()

# Fix existing Kotlin receiver-name collision.
s = s.replace('val text = TextView(this).apply {', 'val taskText = TextView(this).apply {', 1)
s = s.replace('row.addView(text, LinearLayout.LayoutParams(0, dp(64), 1f))', 'row.addView(taskText, LinearLayout.LayoutParams(0, dp(64), 1f))', 1)

anchor = 'import kotlin.math.sqrt\n'
extra = '''import kotlin.math.sqrt
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
'''
if anchor not in s:
    raise SystemExit('import marker missing')
s = s.replace(anchor, extra, 1)

s = s.replace('class MainActivity : Activity() {', 'class MainActivity : AndroidApplication() {', 1)
s = s.replace('private lateinit var glView: CityGLView', 'private lateinit var cityGame: TaskCityGame', 1)
s = s.replace('        if (::glView.isInitialized) glView.onResume()\n', '', 1)
s = s.replace('        if (::glView.isInitialized) glView.onPause()\n', '', 1)

old_oncreate = '''        window.statusBarColor = Color.rgb(7, 17, 31)
        window.navigationBarColor = Color.BLACK
        setContentView(buildUi())
        refreshCity()
        handler.post(expiryCheck)'''
new_oncreate = '''        window.statusBarColor = Color.rgb(7, 17, 31)
        window.navigationBarColor = Color.BLACK

        // Important: initialize libGDX as the Activity content first.  The previous
        // build used initializeForView() and then replaced the content view, which
        // can fail during Android startup on some devices.  Keep the game surface
        // as the real content view and place the task controls transparently above it.
        cityGame = TaskCityGame()
        val gameConfig = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useGyroscope = false
            useImmersiveMode = false
        }
        initialize(cityGame, gameConfig)
        addContentView(
            buildUi(),
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        refreshCity()
        handler.post(expiryCheck)'''
if old_oncreate not in s:
    raise SystemExit('onCreate marker missing')
s = s.replace(old_oncreate, new_oncreate, 1)

# The overlay must be transparent so the libGDX game surface stays visible behind it.
s = s.replace('setBackgroundColor(Color.rgb(7, 17, 31))\n            layoutDirection = View.LAYOUT_DIRECTION_RTL',
              'setBackgroundColor(Color.TRANSPARENT)\n            layoutDirection = View.LAYOUT_DIRECTION_RTL', 1)

# Remove the old native OpenGL/libGDX view insertion from buildUi(). The libGDX
# surface is already installed by initialize() above.
old_view = '''        glView = CityGLView(this)
        root.addView(glView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            topMargin = dp(112)
            bottomMargin = dp(78)
        })
'''
if old_view not in s:
    raise SystemExit('old city view marker missing')
s = s.replace(old_view, '', 1)

old_refresh = '''        val count = if (mode == CityMode.NORMAL) normalHouses() else urgentHouses()
        glView.cityRenderer.cityMode = mode
        glView.cityRenderer.visibleHouseCount = count'''
new_refresh = '''        val count = if (intent.getBooleanExtra("preview_all", false)) 20
        else if (mode == CityMode.NORMAL) normalHouses() else urgentHouses()
        cityGame.setCityState(mode == CityMode.URGENT, count)'''
if old_refresh not in s:
    raise SystemExit('refresh marker missing')
s = s.replace(old_refresh, new_refresh, 1)

p.write_text(s)
print('TASKCITY_LIBGDX_SAFE_STARTUP_PATCH_OK')
