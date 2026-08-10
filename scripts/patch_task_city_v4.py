from pathlib import Path

p=Path('app/src/main/java/com/godnit/taskcity/MainActivity.kt')
s=p.read_text()

# CI preview should reveal all twenty designed homes.
s=s.replace('val count = if (intent.getBooleanExtra("preview_all", false)) 12',
            'val count = if (intent.getBooleanExtra("preview_all", false)) 20',1)

# Replace touch control with horizontal panning only. No rotate, no pinch zoom.
start=s.index('private class CityGLView(context: Context) : GLSurfaceView(context) {')
end=s.index('\nprivate data class MaterialInfo(',start)
new_view=r'''private class CityGLView(context: Context) : GLSurfaceView(context) {
    val cityRenderer = CityRenderer(context)
    private var lastX = 0f
    private var gestureBlocked = false

    init {
        setEGLContextClientVersion(2)
        setRenderer(cityRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        preserveEGLContextOnPause = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                gestureBlocked = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // Multi-touch is intentionally ignored: camera angle and zoom are locked.
                gestureBlocked = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!gestureBlocked && event.pointerCount == 1) {
                    val dx = event.x - lastX
                    cityRenderer.panBy(-dx * 0.0105f)
                    lastX = event.x
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> gestureBlocked = false
        }
        return true
    }
}
'''
s=s[:start]+new_view+s[end:]

# Fixed isometric camera plus bounded horizontal world pan.
s=s.replace('private var yaw = -35f\n    private var pitch = 30f\n    private var cameraDistance = 4.30f',
'''private val yaw = -35f
    private val pitch = 31f
    private val cameraDistance = 5.05f
    @Volatile private var panX = 0f''',1)

old_funcs='''    fun rotateBy(dx: Float, dy: Float) {
        yaw += dx
        pitch = (pitch + dy).coerceIn(8f, 55f)
    }

    fun zoomBy(factor: Float) {
        cameraDistance = (cameraDistance * factor).coerceIn(2.8f, 8.0f)
    }'''
new_funcs='''    fun panBy(delta: Float) {
        panX = (panX + delta).coerceIn(-1.55f, 1.55f)
    }'''
if old_funcs not in s:
    raise SystemExit('camera functions marker not found')
s=s.replace(old_funcs,new_funcs,1)

s=s.replace('Matrix.setLookAtM(view, 0, 0f, 0.9f, cameraDistance, 0f, 0f, 0f, 0f, 1f, 0f)',
            'Matrix.setLookAtM(view, 0, panX, 1.00f, cameraDistance, panX, 0f, 0f, 0f, 1f, 0f)',1)

# Stronger, warm daylight while keeping gentle shading.
s=s.replace('vLight = 0.76 + 0.24 * sun;', 'vLight = 0.84 + 0.30 * sun;',1)
s=s.replace('GLES20.glClearColor(0.56f, 0.84f, 0.98f, 1f)', 'GLES20.glClearColor(0.49f, 0.82f, 0.98f, 1f)',1)

# Yard parts use their real material instead of the house trim color.
old='''            if (buildingIndex >= 0) {
                val colors = housePalette(buildingIndex)
                when {
                    batch.group.endsWith("_ROOF") -> {
                        baseR = colors.first[0]; baseG = colors.first[1]; baseB = colors.first[2]
                    }
                    batch.group.endsWith("_WALL") -> {
                        baseR = colors.second[0]; baseG = colors.second[1]; baseB = colors.second[2]
                    }
                    else -> {
                        // Windows/trim stay dark blue-gray, separate from both wall and roof.
                        baseR = 0.24f; baseG = 0.39f; baseB = 0.46f
                    }
                }
                useTexture = false
            }'''
new='''            if (buildingIndex >= 0) {
                val colors = housePalette(buildingIndex)
                when {
                    batch.group.endsWith("_ROOF") -> {
                        baseR = colors.first[0]; baseG = colors.first[1]; baseB = colors.first[2]; useTexture = false
                    }
                    batch.group.endsWith("_WALL") -> {
                        baseR = colors.second[0]; baseG = colors.second[1]; baseB = colors.second[2]; useTexture = false
                    }
                    batch.group.endsWith("_DETAIL") -> {
                        baseR = 0.24f; baseG = 0.39f; baseB = 0.46f; useTexture = false
                    }
                    else -> {
                        // Fences, hedges, paths and flowers keep their own materials.
                    }
                }
            }'''
if old not in s:
    raise SystemExit('house material block not found')
s=s.replace(old,new,1)

p.write_text(s)
print('TASK_CITY_V4_PATCH_OK')
