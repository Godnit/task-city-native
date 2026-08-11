package com.godnit.taskcity

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max
import kotlin.math.sqrt

class CityRenderer : GLSurfaceView.Renderer {
    enum class HouseAnimation { NONE, BUILD, DEMOLISH }

    private data class Mesh(val buffer: FloatBuffer, val vertexCount: Int)
    private data class Plot(val x: Float, val z: Float)

    private var program = 0
    private var aPosition = 0
    private var aNormal = 0
    private var uModel = 0
    private var uVP = 0
    private var uColor = 0
    private var uLightDir = 0
    private var uShadow = 0

    private lateinit var cube: Mesh
    private lateinit var roof: Mesh
    private lateinit var octa: Mesh

    private val model = FloatArray(16)
    private val view = FloatArray(16)
    private val projection = FloatArray(16)
    private val vp = FloatArray(16)

    @Volatile private var cameraPanX = 0f
    @Volatile private var cameraPanZ = 0f
    @Volatile private var cameraZoom = 11.8f

    @Volatile private var targetHouseCount = 6
    @Volatile private var animation = HouseAnimation.NONE
    @Volatile private var animationStartedAt = 0L
    @Volatile private var previousHouseCount = 6

    private var surfaceWidth = 1
    private var surfaceHeight = 1

    private val sunDirection = normalize(floatArrayOf(-0.52f, -1.0f, -0.36f))

    private val roofPalette = arrayOf(
        c(0.87f, 0.25f, 0.12f),
        c(0.08f, 0.47f, 0.58f),
        c(0.09f, 0.31f, 0.58f),
        c(0.80f, 0.38f, 0.12f),
        c(0.24f, 0.46f, 0.28f),
        c(0.58f, 0.20f, 0.16f)
    )

    private val wallPalette = arrayOf(
        c(0.96f, 0.84f, 0.62f),
        c(0.88f, 0.93f, 0.82f),
        c(0.72f, 0.86f, 0.91f),
        c(0.97f, 0.90f, 0.75f),
        c(0.88f, 0.77f, 0.55f),
        c(0.92f, 0.86f, 0.73f)
    )

    fun setHouseCount(count: Int, houseAnimation: HouseAnimation) {
        val safe = count.coerceAtLeast(0)
        previousHouseCount = targetHouseCount
        targetHouseCount = safe
        animation = houseAnimation
        animationStartedAt = System.currentTimeMillis()
    }

    fun panBy(screenDx: Float, screenDy: Float) {
        val scale = cameraZoom * 0.00125f
        cameraPanX = (cameraPanX + (screenDx + screenDy) * scale * 0.72f).coerceIn(-8f, 8f)
        cameraPanZ = (cameraPanZ + (-screenDx + screenDy) * scale * 0.72f).coerceIn(-9f, 18f)
    }

    fun zoomBy(factor: Float) {
        cameraZoom = (cameraZoom * factor).coerceIn(6.8f, 17.5f)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.30f, 0.76f, 0.96f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LESS)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        aPosition = GLES20.glGetAttribLocation(program, "aPosition")
        aNormal = GLES20.glGetAttribLocation(program, "aNormal")
        uModel = GLES20.glGetUniformLocation(program, "uModel")
        uVP = GLES20.glGetUniformLocation(program, "uVP")
        uColor = GLES20.glGetUniformLocation(program, "uColor")
        uLightDir = GLES20.glGetUniformLocation(program, "uLightDir")
        uShadow = GLES20.glGetUniformLocation(program, "uShadow")

        cube = mesh(CUBE_VERTICES)
        roof = mesh(ROOF_VERTICES)
        octa = mesh(createOctahedron())
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = max(1, width)
        surfaceHeight = max(1, height)
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        updateCamera()

        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(uVP, 1, false, vp, 0)
        GLES20.glUniform3fv(uLightDir, 1, sunDirection, 0)
        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glEnableVertexAttribArray(aNormal)

        drawEnvironment()

        val state = animationState()
        drawAllShadows(state)
        drawAllObjects(state)

        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisableVertexAttribArray(aNormal)
    }

    private data class AnimState(
        val stableCount: Int,
        val animatedIndex: Int,
        val animatedScale: Float,
        val isDemolishing: Boolean
    )

    private fun animationState(): AnimState {
        val type = animation
        if (type == HouseAnimation.NONE) {
            return AnimState(targetHouseCount, -1, 1f, false)
        }

        val elapsed = (System.currentTimeMillis() - animationStartedAt).coerceAtLeast(0L)
        val duration = if (type == HouseAnimation.BUILD) 720f else 620f
        val t = (elapsed / duration).coerceIn(0f, 1f)

        if (t >= 1f) {
            animation = HouseAnimation.NONE
            return AnimState(targetHouseCount, -1, 1f, false)
        }

        return when (type) {
            HouseAnimation.BUILD -> {
                val index = (targetHouseCount - 1).coerceAtLeast(0)
                val eased = 1f - (1f - t) * (1f - t) * (1f - t)
                AnimState((targetHouseCount - 1).coerceAtLeast(0), index, eased, false)
            }
            HouseAnimation.DEMOLISH -> {
                val index = targetHouseCount
                val shrink = (1f - t) * (1f - t)
                AnimState(targetHouseCount, index, shrink, true)
            }
            else -> AnimState(targetHouseCount, -1, 1f, false)
        }
    }

    private fun updateCamera() {
        val aspect = surfaceWidth.toFloat() / surfaceHeight.toFloat()
        val halfH = cameraZoom
        val halfW = halfH * aspect
        Matrix.orthoM(projection, 0, -halfW, halfW, -halfH, halfH, -40f, 60f)

        val targetX = cameraPanX
        val targetZ = cameraPanZ
        Matrix.setLookAtM(
            view, 0,
            targetX + 12.5f, 15.8f, targetZ + 12.5f,
            targetX, 0.25f, targetZ,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vp, 0, projection, 0, view, 0)
    }

    private fun drawEnvironment() {
        drawCube(0f, -0.10f, 5.5f, 14.5f, 0.16f, 34f, c(0.43f, 0.72f, 0.27f))

        for (x in floatArrayOf(-2f, 2f)) {
            drawCube(x, 0.01f, 5.5f, 1.25f, 0.08f, 33f, c(0.78f, 0.76f, 0.69f))
            drawCube(x, 0.055f, 5.5f, 0.82f, 0.055f, 33f, c(0.20f, 0.23f, 0.24f))
            drawRoadLine(x, 5.5f, vertical = true)
        }

        for (z in floatArrayOf(-6f, -3f, 0f, 3f, 6f, 9f, 12f, 15f, 18f)) {
            drawCube(0f, 0.01f, z, 14.2f, 0.08f, 1.25f, c(0.78f, 0.76f, 0.69f))
            drawCube(0f, 0.055f, z, 14.2f, 0.055f, 0.82f, c(0.20f, 0.23f, 0.24f))
            drawRoadLine(0f, z, vertical = false)
        }

        for (z in floatArrayOf(-3f, 3f, 9f)) {
            drawCrosswalk(-2f, z)
            drawCrosswalk(2f, z)
        }
    }

    private fun drawRoadLine(x: Float, z: Float, vertical: Boolean) {
        val yellow = c(0.92f, 0.78f, 0.24f)
        if (vertical) {
            var zz = -9f
            while (zz < 21f) {
                drawCube(x, 0.092f, zz, 0.045f, 0.018f, 0.58f, yellow)
                zz += 1.35f
            }
        } else {
            var xx = -6.4f
            while (xx < 6.4f) {
                drawCube(xx, 0.092f, z, 0.58f, 0.018f, 0.045f, yellow)
                xx += 1.35f
            }
        }
    }

    private fun drawCrosswalk(x: Float, z: Float) {
        val white = c(0.90f, 0.90f, 0.86f)
        for (i in -2..2) {
            drawCube(x + i * 0.14f, 0.102f, z, 0.07f, 0.016f, 0.65f, white)
        }
    }

    private fun drawAllShadows(state: AnimState) {
        GLES20.glDepthMask(false)

        for (i in 0 until state.stableCount.coerceAtMost(MAX_VISIBLE_HOUSES)) {
            val p = plot(i)
            drawHouse(p.x, p.z, i, 1f, shadow = true)
        }
        if (state.animatedIndex in 0 until MAX_VISIBLE_HOUSES && state.animatedScale > 0.02f) {
            val p = plot(state.animatedIndex)
            drawHouse(p.x, p.z, state.animatedIndex, state.animatedScale, shadow = true)
        }

        for (i in TREE_POSITIONS.indices) {
            val p = TREE_POSITIONS[i]
            drawTree(p[0], p[1], 0.86f + (i % 4) * 0.06f, shadow = true)
        }
        GLES20.glDepthMask(true)
    }

    private fun drawAllObjects(state: AnimState) {
        for (i in TREE_POSITIONS.indices) {
            val p = TREE_POSITIONS[i]
            drawTree(p[0], p[1], 0.86f + (i % 4) * 0.06f, shadow = false)
        }

        for (i in 0 until state.stableCount.coerceAtMost(MAX_VISIBLE_HOUSES)) {
            val p = plot(i)
            drawLotGarden(p.x, p.z, i)
            drawHouse(p.x, p.z, i, 1f, shadow = false)
        }

        if (state.animatedIndex in 0 until MAX_VISIBLE_HOUSES && state.animatedScale > 0.02f) {
            val p = plot(state.animatedIndex)
            if (!state.isDemolishing) drawLotGarden(p.x, p.z, state.animatedIndex)
            drawHouse(p.x, p.z, state.animatedIndex, state.animatedScale, shadow = false)
        }
    }

    private fun drawLotGarden(x: Float, z: Float, index: Int) {
        val lawn = if (index % 2 == 0) c(0.52f, 0.78f, 0.30f) else c(0.47f, 0.74f, 0.26f)
        drawCube(x, 0.02f, z, 2.74f, 0.07f, 2.42f, lawn)

        for (i in 0..3) {
            drawCube(x, 0.095f, z + 1.03f + i * 0.26f, 0.38f, 0.025f, 0.18f, c(0.78f, 0.70f, 0.57f))
        }

        val fence = c(0.95f, 0.94f, 0.86f)
        for (j in -3..3) {
            val xx = x + j * 0.34f
            drawCube(xx, 0.22f, z + 1.20f, 0.08f, 0.42f, 0.08f, fence)
        }

        val flowers = arrayOf(c(0.96f, 0.28f, 0.22f), c(0.98f, 0.68f, 0.16f), c(0.94f, 0.44f, 0.64f))
        for (j in 0..2) {
            val fx = x - 0.95f + j * 0.32f
            val fz = z + 0.86f + (j % 2) * 0.16f
            drawOcta(fx, 0.18f, fz, 0.12f, 0.12f, 0.12f, flowers[(index + j) % flowers.size])
        }
    }

    private fun drawHouse(x: Float, z: Float, index: Int, animScale: Float, shadow: Boolean) {
        if (animScale <= 0f) return
        val wall = wallPalette[index % wallPalette.size]
        val roofColor = roofPalette[index % roofPalette.size]
        val trim = c(0.94f, 0.91f, 0.80f)
        val door = c(0.48f, 0.24f, 0.10f)
        val glass = c(0.37f, 0.64f, 0.72f)
        val foundation = c(0.78f, 0.71f, 0.60f)

        val s = animScale
        val sink = if (animation == HouseAnimation.DEMOLISH) (1f - s) * -0.30f else 0f

        drawCube(x, 0.08f * s + sink, z, 2.18f * s, 0.15f * s, 1.86f * s, foundation, shadow)
        drawCube(x, (0.79f * s) + sink, z, 2.08f * s, 1.42f * s, 1.72f * s, wall, shadow)
        drawRoof(x, (1.48f * s) + sink, z, 1.18f * s, 0.72f * s, 0.98f * s, roofColor, shadow)
        drawCube(x + 0.58f * s, (1.88f * s) + sink, z - 0.24f * s, 0.26f * s, 0.72f * s, 0.26f * s, c(0.58f, 0.31f, 0.19f), shadow)

        if (!shadow) {
            drawCube(x, 0.48f * s + sink, z + 0.89f * s, 0.44f * s, 0.78f * s, 0.08f * s, door)
            drawCube(x - 0.67f * s, 0.78f * s + sink, z + 0.895f * s, 0.43f * s, 0.43f * s, 0.07f * s, glass)
            drawCube(x + 0.67f * s, 0.78f * s + sink, z + 0.895f * s, 0.43f * s, 0.43f * s, 0.07f * s, glass)

            drawCube(x - 0.67f * s, 0.78f * s + sink, z + 0.938f * s, 0.055f * s, 0.45f * s, 0.026f * s, trim)
            drawCube(x - 0.67f * s, 0.78f * s + sink, z + 0.938f * s, 0.45f * s, 0.055f * s, 0.026f * s, trim)
            drawCube(x + 0.67f * s, 0.78f * s + sink, z + 0.938f * s, 0.055f * s, 0.45f * s, 0.026f * s, trim)
            drawCube(x + 0.67f * s, 0.78f * s + sink, z + 0.938f * s, 0.45f * s, 0.055f * s, 0.026f * s, trim)

            drawCube(x, 1.12f * s + sink, z + 1.10f * s, 1.10f * s, 0.10f * s, 0.62f * s, roofColor)
            drawCube(x - 0.46f * s, 0.58f * s + sink, z + 1.22f * s, 0.09f * s, 1.05f * s, 0.09f * s, trim)
            drawCube(x + 0.46f * s, 0.58f * s + sink, z + 1.22f * s, 0.09f * s, 1.05f * s, 0.09f * s, trim)
        }
    }

    private fun drawTree(x: Float, z: Float, scale: Float, shadow: Boolean) {
        val trunk = c(0.42f, 0.26f, 0.12f)
        val green1 = c(0.20f, 0.50f, 0.16f)
        val green2 = c(0.29f, 0.62f, 0.20f)
        drawCube(x, 0.43f * scale, z, 0.22f * scale, 0.86f * scale, 0.22f * scale, trunk, shadow)
        drawOcta(x, 1.08f * scale, z, 0.86f * scale, 0.82f * scale, 0.86f * scale, green1, shadow)
        drawOcta(x - 0.35f * scale, 1.03f * scale, z + 0.12f * scale, 0.55f * scale, 0.58f * scale, 0.55f * scale, green2, shadow)
        drawOcta(x + 0.34f * scale, 1.02f * scale, z - 0.14f * scale, 0.56f * scale, 0.56f * scale, 0.56f * scale, green2, shadow)
    }

    private fun plot(index: Int): Plot {
        val cols = floatArrayOf(-4f, 0f, 4f)
        val col = index % 3
        val row = index / 3
        return Plot(cols[col], -7.5f + row * 3f)
    }

    private fun drawCube(
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        color: FloatArray,
        shadow: Boolean = false
    ) {
        setModel(x, y, z, sx, sy, sz)
        drawMesh(cube, color, shadow)
    }

    private fun drawRoof(
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        color: FloatArray,
        shadow: Boolean = false
    ) {
        setModel(x, y, z, sx, sy, sz)
        drawMesh(roof, color, shadow)
    }

    private fun drawOcta(
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        color: FloatArray,
        shadow: Boolean = false
    ) {
        setModel(x, y, z, sx, sy, sz)
        drawMesh(octa, color, shadow)
    }

    private fun setModel(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float) {
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, x, y, z)
        Matrix.scaleM(model, 0, sx, sy, sz)
    }

    private fun drawMesh(mesh: Mesh, color: FloatArray, shadow: Boolean) {
        GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0)
        GLES20.glUniform4fv(uColor, 1, color, 0)
        GLES20.glUniform1f(uShadow, if (shadow) 1f else 0f)

        mesh.buffer.position(0)
        GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, 6 * 4, mesh.buffer)
        mesh.buffer.position(3)
        GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, 6 * 4, mesh.buffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.vertexCount)
    }

    private fun mesh(data: FloatArray): Mesh {
        val buffer = ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(data).position(0)
        return Mesh(buffer, data.size / 6)
    }

    private fun buildProgram(vertex: String, fragment: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertex)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragment)
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, vs)
        GLES20.glAttachShader(p, fs)
        GLES20.glLinkProgram(p)
        val status = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(p)
            GLES20.glDeleteProgram(p)
            throw RuntimeException("OpenGL program link failed: $log")
        }
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        return p
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("OpenGL shader compile failed: $log")
        }
        return shader
    }

    companion object {
        private const val MAX_VISIBLE_HOUSES = 30

        private fun c(r: Float, g: Float, b: Float, a: Float = 1f) = floatArrayOf(r, g, b, a)

        private fun normalize(v: FloatArray): FloatArray {
            val len = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).coerceAtLeast(0.0001f)
            return floatArrayOf(v[0] / len, v[1] / len, v[2] / len)
        }

        private val TREE_POSITIONS = arrayOf(
            floatArrayOf(-6.2f, -8.1f), floatArrayOf(6.2f, -7.0f),
            floatArrayOf(-6.1f, -4.2f), floatArrayOf(6.1f, -2.0f),
            floatArrayOf(-6.2f, 0.9f), floatArrayOf(6.2f, 1.3f),
            floatArrayOf(-6.0f, 5.3f), floatArrayOf(6.1f, 5.1f),
            floatArrayOf(-6.0f, 10.1f), floatArrayOf(6.1f, 10.8f),
            floatArrayOf(-6.0f, 15.1f), floatArrayOf(6.0f, 15.8f),
            floatArrayOf(-1.0f, -9.1f), floatArrayOf(1.1f, -9.0f),
            floatArrayOf(-1.0f, 20.2f), floatArrayOf(1.2f, 20.0f)
        )

        private val VERTEX_SHADER = """
            uniform mat4 uModel;
            uniform mat4 uVP;
            uniform vec3 uLightDir;
            uniform float uShadow;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            varying vec3 vNormal;
            varying float vShadow;

            void main() {
                vec4 world = uModel * vec4(aPosition, 1.0);
                if (uShadow > 0.5) {
                    float t = (0.075 - world.y) / uLightDir.y;
                    world.xyz += uLightDir * t;
                    world.y = 0.075;
                }
                gl_Position = uVP * world;
                vNormal = mat3(uModel) * aNormal;
                vShadow = uShadow;
            }
        """.trimIndent()

        private val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 uColor;
            uniform vec3 uLightDir;
            varying vec3 vNormal;
            varying float vShadow;

            void main() {
                if (vShadow > 0.5) {
                    gl_FragColor = vec4(0.055, 0.075, 0.065, 0.20);
                    return;
                }
                vec3 normal = normalize(vNormal);
                float diffuse = max(dot(normal, normalize(-uLightDir)), 0.0);
                float topLight = 0.82 + 0.18 * max(normal.y, 0.0);
                float lighting = (0.58 + 0.42 * diffuse) * topLight;
                vec3 result = uColor.rgb * lighting;
                gl_FragColor = vec4(result, uColor.a);
            }
        """.trimIndent()

        private val CUBE_VERTICES = floatArrayOf(
            -0.5f,-0.5f, 0.5f, 0f,0f,1f,  0.5f,-0.5f, 0.5f, 0f,0f,1f,  0.5f, 0.5f, 0.5f, 0f,0f,1f,
            -0.5f,-0.5f, 0.5f, 0f,0f,1f,  0.5f, 0.5f, 0.5f, 0f,0f,1f, -0.5f, 0.5f, 0.5f, 0f,0f,1f,
             0.5f,-0.5f,-0.5f, 0f,0f,-1f, -0.5f,-0.5f,-0.5f, 0f,0f,-1f, -0.5f, 0.5f,-0.5f, 0f,0f,-1f,
             0.5f,-0.5f,-0.5f, 0f,0f,-1f, -0.5f, 0.5f,-0.5f, 0f,0f,-1f,  0.5f, 0.5f,-0.5f, 0f,0f,-1f,
            -0.5f,-0.5f,-0.5f,-1f,0f,0f, -0.5f,-0.5f, 0.5f,-1f,0f,0f, -0.5f, 0.5f, 0.5f,-1f,0f,0f,
            -0.5f,-0.5f,-0.5f,-1f,0f,0f, -0.5f, 0.5f, 0.5f,-1f,0f,0f, -0.5f, 0.5f,-0.5f,-1f,0f,0f,
             0.5f,-0.5f, 0.5f, 1f,0f,0f,  0.5f,-0.5f,-0.5f, 1f,0f,0f,  0.5f, 0.5f,-0.5f, 1f,0f,0f,
             0.5f,-0.5f, 0.5f, 1f,0f,0f,  0.5f, 0.5f,-0.5f, 1f,0f,0f,  0.5f, 0.5f, 0.5f, 1f,0f,0f,
            -0.5f, 0.5f, 0.5f, 0f,1f,0f,  0.5f, 0.5f, 0.5f, 0f,1f,0f,  0.5f, 0.5f,-0.5f, 0f,1f,0f,
            -0.5f, 0.5f, 0.5f, 0f,1f,0f,  0.5f, 0.5f,-0.5f, 0f,1f,0f, -0.5f, 0.5f,-0.5f, 0f,1f,0f,
            -0.5f,-0.5f,-0.5f, 0f,-1f,0f,  0.5f,-0.5f,-0.5f, 0f,-1f,0f,  0.5f,-0.5f, 0.5f, 0f,-1f,0f,
            -0.5f,-0.5f,-0.5f, 0f,-1f,0f,  0.5f,-0.5f, 0.5f, 0f,-1f,0f, -0.5f,-0.5f, 0.5f, 0f,-1f,0f
        )

        private val ROOF_VERTICES = floatArrayOf(
            -1f,0f,1f, 0f,0f,1f,   1f,0f,1f, 0f,0f,1f,   0f,1f,1f, 0f,0f,1f,
             1f,0f,-1f, 0f,0f,-1f,  -1f,0f,-1f, 0f,0f,-1f,  0f,1f,-1f, 0f,0f,-1f,
            -1f,0f,-1f, -0.707f,0.707f,0f,  -1f,0f,1f, -0.707f,0.707f,0f,  0f,1f,1f, -0.707f,0.707f,0f,
            -1f,0f,-1f, -0.707f,0.707f,0f,   0f,1f,1f, -0.707f,0.707f,0f,  0f,1f,-1f, -0.707f,0.707f,0f,
             1f,0f,1f, 0.707f,0.707f,0f,   1f,0f,-1f, 0.707f,0.707f,0f,  0f,1f,-1f, 0.707f,0.707f,0f,
             1f,0f,1f, 0.707f,0.707f,0f,   0f,1f,-1f, 0.707f,0.707f,0f,  0f,1f,1f, 0.707f,0.707f,0f
        )

        private fun createOctahedron(): FloatArray {
            val top = floatArrayOf(0f, 1f, 0f)
            val bottom = floatArrayOf(0f, -1f, 0f)
            val east = floatArrayOf(1f, 0f, 0f)
            val west = floatArrayOf(-1f, 0f, 0f)
            val north = floatArrayOf(0f, 0f, 1f)
            val south = floatArrayOf(0f, 0f, -1f)
            val triangles = arrayOf(
                arrayOf(top, north, east), arrayOf(top, east, south),
                arrayOf(top, south, west), arrayOf(top, west, north),
                arrayOf(bottom, east, north), arrayOf(bottom, south, east),
                arrayOf(bottom, west, south), arrayOf(bottom, north, west)
            )
            val out = ArrayList<Float>(triangles.size * 18)
            for (tri in triangles) {
                val ax = tri[1][0] - tri[0][0]
                val ay = tri[1][1] - tri[0][1]
                val az = tri[1][2] - tri[0][2]
                val bx = tri[2][0] - tri[0][0]
                val by = tri[2][1] - tri[0][1]
                val bz = tri[2][2] - tri[0][2]
                val n = normalize(floatArrayOf(
                    ay * bz - az * by,
                    az * bx - ax * bz,
                    ax * by - ay * bx
                ))
                for (v in tri) {
                    out += v[0]; out += v[1]; out += v[2]
                    out += n[0]; out += n[1]; out += n[2]
                }
            }
            return FloatArray(out.size) { out[it] }
        }
    }
}
