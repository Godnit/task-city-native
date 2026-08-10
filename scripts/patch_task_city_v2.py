from pathlib import Path

p = Path('app/src/main/java/com/godnit/taskcity/MainActivity.kt')
s = p.read_text()

# Fix Kotlin TextView receiver ambiguity in task rows.
s = s.replace('val text = TextView(this).apply {', 'val taskText = TextView(this).apply {', 1)
s = s.replace('row.addView(text, LinearLayout.LayoutParams(0, dp(64), 1f))', 'row.addView(taskText, LinearLayout.LayoutParams(0, dp(64), 1f))', 1)

# CI preview mode: only the emulator passes this extra, normal installs still start empty.
s = s.replace(
'''        val count = if (mode == CityMode.NORMAL) normalHouses() else urgentHouses()''',
'''        val count = if (intent.getBooleanExtra("preview_all", false)) 12
        else if (mode == CityMode.NORMAL) normalHouses() else urgentHouses()''',
1)

# Stable touch controller: two fingers ONLY zoom; first one-finger frame after pinch only resets the anchor.
start = s.index('private class CityGLView(context: Context) : GLSurfaceView(context) {')
end = s.index('\nprivate data class MaterialInfo(', start)
new_gl_view = r'''private class CityGLView(context: Context) : GLSurfaceView(context) {
    val cityRenderer = CityRenderer(context)
    private var lastX = 0f
    private var lastY = 0f
    private var pinchDistance = 0f
    private var suppressRotationAfterPinch = false

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
                lastY = event.y
                pinchDistance = 0f
                suppressRotationAfterPinch = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    pinchDistance = distance(event)
                    suppressRotationAfterPinch = true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    val now = distance(event)
                    if (pinchDistance > 0f && now > 0f) {
                        cityRenderer.zoomBy(pinchDistance / now)
                    }
                    pinchDistance = now
                    suppressRotationAfterPinch = true
                } else {
                    if (suppressRotationAfterPinch) {
                        // Do not use coordinates left over from before the two-finger gesture.
                        lastX = event.x
                        lastY = event.y
                        suppressRotationAfterPinch = false
                    } else {
                        val dx = event.x - lastX
                        val dy = event.y - lastY
                        cityRenderer.rotateBy(dx * 0.32f, dy * 0.15f)
                        lastX = event.x
                        lastY = event.y
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                pinchDistance = 0f
                suppressRotationAfterPinch = true
                val remaining = if (event.actionIndex == 0) 1 else 0
                if (remaining < event.pointerCount) {
                    lastX = event.getX(remaining)
                    lastY = event.getY(remaining)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                pinchDistance = 0f
                suppressRotationAfterPinch = false
            }
        }
        return true
    }

    private fun distance(e: MotionEvent): Float {
        if (e.pointerCount < 2) return 0f
        val dx = e.getX(0) - e.getX(1)
        val dy = e.getY(0) - e.getY(1)
        return sqrt(dx * dx + dy * dy)
    }
}
'''
s = s[:start] + new_gl_view + s[end:]

# Additional shader controls for per-house roof/wall palettes.
s = s.replace(
'''    private var uHasTex = 0
    private var uTex = 0''',
'''    private var uHasTex = 0
    private var uTex = 0
    private var uHouseRecolor = 0
    private var uRoof = 0
    private var uWall = 0''',
1)

s = s.replace('private var pitch = 25f\n    private var cameraDistance = 4.8f',
              'private var pitch = 30f\n    private var cameraDistance = 4.45f', 1)

s = s.replace(
'''        uHasTex = GLES20.glGetUniformLocation(program, "uHasTex")
        uTex = GLES20.glGetUniformLocation(program, "uTex")''',
'''        uHasTex = GLES20.glGetUniformLocation(program, "uHasTex")
        uTex = GLES20.glGetUniformLocation(program, "uTex")
        uHouseRecolor = GLES20.glGetUniformLocation(program, "uHouseRecolor")
        uRoof = GLES20.glGetUniformLocation(program, "uRoof")
        uWall = GLES20.glGetUniformLocation(program, "uWall")''',
1)

s = s.replace(
'''        if (cityMode == CityMode.NORMAL) GLES20.glClearColor(0.54f, 0.78f, 0.79f, 1f)
        else GLES20.glClearColor(0.12f, 0.10f, 0.24f, 1f)''',
'''        if (cityMode == CityMode.NORMAL) GLES20.glClearColor(0.43f, 0.76f, 0.91f, 1f)
        else GLES20.glClearColor(0.12f, 0.10f, 0.24f, 1f)''',
1)

old_loop = '''            val material = materials[batch.material] ?: MaterialInfo(batch.material)
            batch.buffer.position(0)'''
new_loop = '''            val material = materials[batch.material] ?: MaterialInfo(batch.material)
            if (buildingIndex >= 0) {
                val colors = housePalette(buildingIndex)
                GLES20.glUniform1f(uHouseRecolor, 1f)
                GLES20.glUniform3f(uRoof, colors.first[0], colors.first[1], colors.first[2])
                GLES20.glUniform3f(uWall, colors.second[0], colors.second[1], colors.second[2])
            } else {
                GLES20.glUniform1f(uHouseRecolor, 0f)
            }
            batch.buffer.position(0)'''
if old_loop not in s:
    raise SystemExit('draw loop marker not found')
s = s.replace(old_loop, new_loop, 1)

# Curated asset uses explicit HOUSE_01..HOUSE_12 groups.
s = s.replace(
'''    private fun isBuilding(name: String): Boolean {
        val n = name.lowercase(Locale.US)
        return n.contains("house") || n.contains("shop") || n.contains("building")
    }

    private fun loadTexture(reference: String): Int {''',
'''    private fun isBuilding(name: String): Boolean {
        return name.startsWith("HOUSE_") || name.lowercase(Locale.US).contains("house")
    }

    private fun housePalette(index: Int): Pair<FloatArray, FloatArray> {
        val roofs = arrayOf(
            floatArrayOf(0.87f, 0.31f, 0.18f), // terracotta
            floatArrayOf(0.10f, 0.52f, 0.55f), // teal
            floatArrayOf(0.16f, 0.42f, 0.70f), // blue
            floatArrayOf(0.30f, 0.56f, 0.34f), // green
            floatArrayOf(0.92f, 0.47f, 0.18f), // orange
            floatArrayOf(0.22f, 0.34f, 0.53f)  // slate blue
        )
        val walls = arrayOf(
            floatArrayOf(0.96f, 0.90f, 0.75f), // cream
            floatArrayOf(0.91f, 0.80f, 0.61f), // warm beige
            floatArrayOf(0.96f, 0.87f, 0.57f), // pale yellow
            floatArrayOf(0.74f, 0.88f, 0.91f), // light blue
            floatArrayOf(0.77f, 0.90f, 0.77f), // mint
            floatArrayOf(0.95f, 0.92f, 0.85f)  // off white
        )
        return Pair(roofs[index % roofs.size], walls[(index * 5 + 1) % walls.size])
    }

    private fun loadTexture(reference: String): Int {''',
1)

# House names are already zero-padded; natural lexical sorting gives task order 01..12.
s = s.replace(
'''        val buildings = batches.map { it.group }.distinct().filter { isBuilding(it) }
        val preferred = listOf("house", "house_2", "house_3", "shop")
        buildingNames = buildings.sortedWith(compareBy<String> {
            val low = it.lowercase(Locale.US)
            val exact = preferred.indexOfFirst { p -> low == p || low.contains(p) }
            if (exact >= 0) exact else 100
        }.thenBy { it })''',
'''        val buildings = batches.map { it.group }.distinct().filter { isBuilding(it) }
        buildingNames = buildings.sorted()''',
1)

old_fragment = '''        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTex;
            uniform float uHasTex;
            uniform vec4 uTint;
            uniform vec4 uBase;
            varying vec2 vUv;
            void main() {
                vec4 color = uBase;
                if (uHasTex > 0.5) color *= texture2D(uTex, vUv);
                gl_FragColor = vec4(color.rgb * uTint.rgb, color.a);
            }
        """'''
new_fragment = '''        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTex;
            uniform float uHasTex;
            uniform vec4 uTint;
            uniform vec4 uBase;
            uniform float uHouseRecolor;
            uniform vec3 uRoof;
            uniform vec3 uWall;
            varying vec2 vUv;
            void main() {
                vec4 color = uBase;
                if (uHasTex > 0.5) {
                    color *= texture2D(uTex, vUv);
                }
                if (uHouseRecolor > 0.5 && uHasTex > 0.5) {
                    // The source pack uses a red/cream palette. Preserve windows/trim,
                    // but remap the large red roof and cream wall swatches per house.
                    float roofMask = step(0.58, color.r)
                                   * step(color.g * 1.17, color.r)
                                   * (1.0 - step(0.70, color.g));
                    float wallMask = step(0.70, color.r)
                                   * step(0.64, color.g)
                                   * step(0.48, color.b);
                    float lightness = (color.r + color.g + color.b) / 3.0;
                    vec3 roofColor = uRoof * (0.72 + 0.28 * max(color.r, 0.45));
                    vec3 wallColor = uWall * (0.78 + 0.22 * lightness);
                    color.rgb = mix(color.rgb, wallColor, wallMask * 0.88);
                    color.rgb = mix(color.rgb, roofColor, roofMask * 0.96);
                }
                gl_FragColor = vec4(color.rgb * uTint.rgb, color.a);
            }
        """'''
if old_fragment not in s:
    raise SystemExit('fragment shader marker not found')
s = s.replace(old_fragment, new_fragment, 1)

p.write_text(s)
print('TASK_CITY_V2_PATCH_OK')
