from pathlib import Path

p = Path('app/src/main/java/com/godnit/taskcity/MainActivity.kt')
s = p.read_text()

# Kotlin TextView receiver ambiguity in the original source.
s = s.replace('val text = TextView(this).apply {', 'val taskText = TextView(this).apply {', 1)
s = s.replace('row.addView(text, LinearLayout.LayoutParams(0, dp(64), 1f))', 'row.addView(taskText, LinearLayout.LayoutParams(0, dp(64), 1f))', 1)

# CI preview shows the whole designed neighborhood; normal users still see only earned houses.
s = s.replace(
'''        val count = if (mode == CityMode.NORMAL) normalHouses() else urgentHouses()''',
'''        val count = if (intent.getBooleanExtra("preview_all", false)) 12
        else if (mode == CityMode.NORMAL) normalHouses() else urgentHouses()''',
1)

# Two fingers are zoom ONLY. Once a pinch begins, rotation stays locked until all fingers lift.
start = s.index('private class CityGLView(context: Context) : GLSurfaceView(context) {')
end = s.index('\nprivate data class MaterialInfo(', start)
new_gl_view = r'''private class CityGLView(context: Context) : GLSurfaceView(context) {
    val cityRenderer = CityRenderer(context)
    private var lastX = 0f
    private var lastY = 0f
    private var pinchDistance = 0f
    private var rotationLockedUntilUp = false

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
                rotationLockedUntilUp = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    pinchDistance = distance(event)
                    rotationLockedUntilUp = true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    val now = distance(event)
                    if (pinchDistance > 0f && now > 0f) {
                        cityRenderer.zoomBy(pinchDistance / now)
                    }
                    pinchDistance = now
                    rotationLockedUntilUp = true
                } else if (!rotationLockedUntilUp) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    cityRenderer.rotateBy(dx * 0.30f, dy * 0.13f)
                    lastX = event.x
                    lastY = event.y
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                pinchDistance = 0f
                rotationLockedUntilUp = true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                pinchDistance = 0f
                rotationLockedUntilUp = false
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

# Lighting attributes/uniforms.
s = s.replace(
'''    private var aPos = 0
    private var aUv = 0
    private var uMvp = 0''',
'''    private var aPos = 0
    private var aUv = 0
    private var aNormal = 0
    private var uMvp = 0
    private var uModel = 0''',
1)

s = s.replace('private var pitch = 25f\n    private var cameraDistance = 4.8f',
              'private var pitch = 30f\n    private var cameraDistance = 4.30f', 1)

s = s.replace(
'''        aPos = GLES20.glGetAttribLocation(program, "aPos")
        aUv = GLES20.glGetAttribLocation(program, "aUv")
        uMvp = GLES20.glGetUniformLocation(program, "uMvp")''',
'''        aPos = GLES20.glGetAttribLocation(program, "aPos")
        aUv = GLES20.glGetAttribLocation(program, "aUv")
        aNormal = GLES20.glGetAttribLocation(program, "aNormal")
        uMvp = GLES20.glGetUniformLocation(program, "uMvp")
        uModel = GLES20.glGetUniformLocation(program, "uModel")''',
1)

# Bright daylight; challenge city stays cooler but not nearly black.
s = s.replace(
'''        if (cityMode == CityMode.NORMAL) GLES20.glClearColor(0.54f, 0.78f, 0.79f, 1f)
        else GLES20.glClearColor(0.12f, 0.10f, 0.24f, 1f)''',
'''        if (cityMode == CityMode.NORMAL) GLES20.glClearColor(0.56f, 0.84f, 0.98f, 1f)
        else GLES20.glClearColor(0.25f, 0.23f, 0.48f, 1f)''',
1)

s = s.replace(
'''        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)''',
'''        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0)''',
1)

old_loop = '''        for (batch in batches) {
            val buildingIndex = buildingNames.indexOf(batch.group)
            if (buildingIndex >= 0 && buildingIndex >= visibleHouseCount) continue
            val material = materials[batch.material] ?: MaterialInfo(batch.material)
            batch.buffer.position(0)
            GLES20.glEnableVertexAttribArray(aPos)
            GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 20, batch.buffer)
            batch.buffer.position(3)
            GLES20.glEnableVertexAttribArray(aUv)
            GLES20.glVertexAttribPointer(aUv, 2, GLES20.GL_FLOAT, false, 20, batch.buffer)
            GLES20.glUniform4f(uBase, material.r, material.g, material.b, 1f)
            if (material.textureId != 0) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, material.textureId)
                GLES20.glUniform1f(uHasTex, 1f)
            } else {
                GLES20.glUniform1f(uHasTex, 0f)
            }
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, batch.vertexCount)
        }
        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisableVertexAttribArray(aUv)'''

new_loop = '''        for (batch in batches) {
            val buildingIndex = houseIndex(batch.group)
            if (buildingIndex >= 0 && buildingIndex >= visibleHouseCount) continue
            val material = materials[batch.material] ?: MaterialInfo(batch.material)
            var baseR = material.r
            var baseG = material.g
            var baseB = material.b
            var useTexture = material.textureId != 0

            if (buildingIndex >= 0) {
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
            }

            batch.buffer.position(0)
            GLES20.glEnableVertexAttribArray(aPos)
            GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 32, batch.buffer)
            batch.buffer.position(3)
            GLES20.glEnableVertexAttribArray(aUv)
            GLES20.glVertexAttribPointer(aUv, 2, GLES20.GL_FLOAT, false, 32, batch.buffer)
            batch.buffer.position(5)
            GLES20.glEnableVertexAttribArray(aNormal)
            GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 32, batch.buffer)
            GLES20.glUniform4f(uBase, baseR, baseG, baseB, 1f)
            if (useTexture) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, material.textureId)
                GLES20.glUniform1f(uHasTex, 1f)
            } else {
                GLES20.glUniform1f(uHasTex, 0f)
            }
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, batch.vertexCount)
        }
        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisableVertexAttribArray(aUv)
        GLES20.glDisableVertexAttribArray(aNormal)'''

if old_loop not in s:
    raise SystemExit('draw loop marker not found')
s = s.replace(old_loop, new_loop, 1)

# Build triangle normals while parsing the OBJ, so the pack geometry gets soft daylight shading.
old_face = '''                    line.startsWith("f ") -> {
                        val verts = line.substring(2).trim().split(Regex("\\\\s+"))
                        if (verts.size >= 3) {
                            for (i in 1 until verts.size - 1) {
                                appendFaceVertex(builders, currentGroup, currentMaterial, verts[0], positions, uvs)
                                appendFaceVertex(builders, currentGroup, currentMaterial, verts[i], positions, uvs)
                                appendFaceVertex(builders, currentGroup, currentMaterial, verts[i + 1], positions, uvs)
                            }
                        }
                    }'''
new_face = '''                    line.startsWith("f ") -> {
                        val verts = line.substring(2).trim().split(Regex("\\\\s+"))
                        if (verts.size >= 3) {
                            for (i in 1 until verts.size - 1) {
                                val normal = faceNormal(verts[0], verts[i], verts[i + 1], positions)
                                appendFaceVertex(builders, currentGroup, currentMaterial, verts[0], positions, uvs, normal)
                                appendFaceVertex(builders, currentGroup, currentMaterial, verts[i], positions, uvs, normal)
                                appendFaceVertex(builders, currentGroup, currentMaterial, verts[i + 1], positions, uvs, normal)
                            }
                        }
                    }'''
if old_face not in s:
    raise SystemExit('face parser marker not found')
s = s.replace(old_face, new_face, 1)

s = s.replace('batches.add(DrawBatch(group, material, fb, arr.size / 5))',
              'batches.add(DrawBatch(group, material, fb, arr.size / 8))', 1)

old_append = '''    private fun appendFaceVertex(
        builders: LinkedHashMap<String, MutableList<Float>>,
        group: String,
        material: String,
        token: String,
        positions: List<FloatArray>,
        uvs: List<FloatArray>
    ) {
        val parts = token.split('/')
        if (parts.isEmpty() || parts[0].isBlank()) return
        val vi = objIndex(parts[0], positions.size)
        if (vi !in positions.indices) return
        val p = positions[vi]
        var u = 0f
        var v = 0f
        if (parts.size > 1 && parts[1].isNotBlank()) {
            val ti = objIndex(parts[1], uvs.size)
            if (ti in uvs.indices) {
                u = uvs[ti][0]
                v = 1f - uvs[ti][1]
            }
        }
        val list = builders.getOrPut("$group|$material") { ArrayList() }
        list.add(p[0]); list.add(p[1]); list.add(p[2]); list.add(u); list.add(v)
    }'''

new_append = '''    private fun faceNormal(a: String, b: String, c: String, positions: List<FloatArray>): FloatArray {
        fun point(token: String): FloatArray {
            val vi = objIndex(token.split('/')[0], positions.size)
            return if (vi in positions.indices) positions[vi] else floatArrayOf(0f, 0f, 0f)
        }
        val p0 = point(a); val p1 = point(b); val p2 = point(c)
        val ax = p1[0] - p0[0]; val ay = p1[1] - p0[1]; val az = p1[2] - p0[2]
        val bx = p2[0] - p0[0]; val by = p2[1] - p0[1]; val bz = p2[2] - p0[2]
        var nx = ay * bz - az * by
        var ny = az * bx - ax * bz
        var nz = ax * by - ay * bx
        val len = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.00001f)
        nx /= len; ny /= len; nz /= len
        return floatArrayOf(nx, ny, nz)
    }

    private fun appendFaceVertex(
        builders: LinkedHashMap<String, MutableList<Float>>,
        group: String,
        material: String,
        token: String,
        positions: List<FloatArray>,
        uvs: List<FloatArray>,
        normal: FloatArray
    ) {
        val parts = token.split('/')
        if (parts.isEmpty() || parts[0].isBlank()) return
        val vi = objIndex(parts[0], positions.size)
        if (vi !in positions.indices) return
        val p = positions[vi]
        var u = 0f
        var v = 0f
        if (parts.size > 1 && parts[1].isNotBlank()) {
            val ti = objIndex(parts[1], uvs.size)
            if (ti in uvs.indices) {
                u = uvs[ti][0]
                v = 1f - uvs[ti][1]
            }
        }
        val list = builders.getOrPut("$group|$material") { ArrayList() }
        list.add(p[0]); list.add(p[1]); list.add(p[2]); list.add(u); list.add(v)
        list.add(normal[0]); list.add(normal[1]); list.add(normal[2])
    }'''
if old_append not in s:
    raise SystemExit('append function marker not found')
s = s.replace(old_append, new_append, 1)

# House groups now have explicit WALL/ROOF/DETAIL suffixes. Parse owner number once.
old_build = '''    private fun isBuilding(name: String): Boolean {
        val n = name.lowercase(Locale.US)
        return n.contains("house") || n.contains("shop") || n.contains("building")
    }

    private fun loadTexture(reference: String): Int {'''
new_build = '''    private fun houseIndex(name: String): Int {
        if (!name.startsWith("HOUSE_") || name.length < 8) return -1
        return (name.substring(6, 8).toIntOrNull() ?: 0) - 1
    }

    private fun isBuilding(name: String): Boolean = houseIndex(name) >= 0

    private fun housePalette(index: Int): Pair<FloatArray, FloatArray> {
        val roofs = arrayOf(
            floatArrayOf(0.88f, 0.31f, 0.18f), // terracotta
            floatArrayOf(0.08f, 0.50f, 0.53f), // teal
            floatArrayOf(0.14f, 0.43f, 0.72f), // blue
            floatArrayOf(0.26f, 0.53f, 0.31f), // green
            floatArrayOf(0.93f, 0.49f, 0.18f), // orange
            floatArrayOf(0.28f, 0.36f, 0.55f)  // slate blue
        )
        val walls = arrayOf(
            floatArrayOf(0.96f, 0.91f, 0.78f), // cream
            floatArrayOf(0.93f, 0.82f, 0.65f), // warm beige
            floatArrayOf(0.97f, 0.88f, 0.59f), // pale yellow
            floatArrayOf(0.76f, 0.89f, 0.93f), // pale blue
            floatArrayOf(0.79f, 0.91f, 0.78f), // mint
            floatArrayOf(0.96f, 0.94f, 0.88f)  // off white
        )
        return Pair(roofs[index % roofs.size], walls[(index * 5 + 1) % walls.size])
    }

    private fun loadTexture(reference: String): Int {'''
if old_build not in s:
    raise SystemExit('building marker not found')
s = s.replace(old_build, new_build, 1)

# The old list contains separate part groups now; keep it only for diagnostics, not visibility logic.
s = s.replace(
'''        val buildings = batches.map { it.group }.distinct().filter { isBuilding(it) }
        val preferred = listOf("house", "house_2", "house_3", "shop")
        buildingNames = buildings.sortedWith(compareBy<String> {
            val low = it.lowercase(Locale.US)
            val exact = preferred.indexOfFirst { p -> low == p || low.contains(p) }
            if (exact >= 0) exact else 100
        }.thenBy { it })''',
'''        buildingNames = batches.map { it.group }.distinct().filter { isBuilding(it) }.sorted()''',
1)

old_vertex = '''        private const val VERTEX_SHADER = """
            uniform mat4 uMvp;
            attribute vec3 aPos;
            attribute vec2 aUv;
            varying vec2 vUv;
            void main() {
                gl_Position = uMvp * vec4(aPos, 1.0);
                vUv = aUv;
            }
        """'''
new_vertex = '''        private const val VERTEX_SHADER = """
            uniform mat4 uMvp;
            uniform mat4 uModel;
            attribute vec3 aPos;
            attribute vec2 aUv;
            attribute vec3 aNormal;
            varying vec2 vUv;
            varying float vLight;
            void main() {
                gl_Position = uMvp * vec4(aPos, 1.0);
                vUv = aUv;
                vec3 n = normalize(mat3(uModel) * aNormal);
                vec3 lightDir = normalize(vec3(-0.35, 0.90, 0.42));
                float sun = max(dot(n, lightDir), 0.0);
                vLight = 0.76 + 0.24 * sun;
            }
        """'''
if old_vertex not in s:
    raise SystemExit('vertex shader marker not found')
s = s.replace(old_vertex, new_vertex, 1)

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
            varying vec2 vUv;
            varying float vLight;
            void main() {
                vec4 color = uBase;
                if (uHasTex > 0.5) color *= texture2D(uTex, vUv);
                color.rgb *= vLight;
                gl_FragColor = vec4(color.rgb * uTint.rgb, color.a);
            }
        """'''
if old_fragment not in s:
    raise SystemExit('fragment shader marker not found')
s = s.replace(old_fragment, new_fragment, 1)

p.write_text(s)
print('TASK_CITY_V3_PATCH_OK')
