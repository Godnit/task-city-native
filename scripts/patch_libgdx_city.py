from pathlib import Path

p=Path('app/src/main/java/com/godnit/taskcity/MainActivity.kt')
s=p.read_text()

# Fix existing Kotlin receiver-name collision.
s=s.replace('val text = TextView(this).apply {','val taskText = TextView(this).apply {',1)
s=s.replace('row.addView(text, LinearLayout.LayoutParams(0, dp(64), 1f))','row.addView(taskText, LinearLayout.LayoutParams(0, dp(64), 1f))',1)

anchor='import kotlin.math.sqrt\n'
extra='''import kotlin.math.sqrt
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
'''
if anchor not in s:
    raise SystemExit('import marker missing')
s=s.replace(anchor,extra,1)

s=s.replace('class MainActivity : Activity() {','class MainActivity : AndroidApplication() {',1)
s=s.replace('private lateinit var glView: CityGLView','private lateinit var cityGame: TaskCityGame',1)
s=s.replace('        if (::glView.isInitialized) glView.onResume()\n','',1)
s=s.replace('        if (::glView.isInitialized) glView.onPause()\n','',1)

old='''        glView = CityGLView(this)
        root.addView(glView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            topMargin = dp(112)
            bottomMargin = dp(78)
        })'''
new='''        cityGame = TaskCityGame()
        val gameConfig = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useGyroscope = false
            useImmersiveMode = false
        }
        val gameView = initializeForView(cityGame, gameConfig)
        root.addView(gameView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            topMargin = dp(112)
            bottomMargin = dp(78)
        })'''
if old not in s:
    raise SystemExit('city view marker missing')
s=s.replace(old,new,1)

old_refresh='''        val count = if (mode == CityMode.NORMAL) normalHouses() else urgentHouses()
        glView.cityRenderer.cityMode = mode
        glView.cityRenderer.visibleHouseCount = count'''
new_refresh='''        val count = if (intent.getBooleanExtra("preview_all", false)) 20
        else if (mode == CityMode.NORMAL) normalHouses() else urgentHouses()
        cityGame.setCityState(mode == CityMode.URGENT, count)'''
if old_refresh not in s:
    raise SystemExit('refresh marker missing')
s=s.replace(old_refresh,new_refresh,1)

p.write_text(s)
print('TASKCITY_LIBGDX_PATCH_OK')
