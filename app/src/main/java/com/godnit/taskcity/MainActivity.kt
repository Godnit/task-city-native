package com.godnit.taskcity

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Locale
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private enum class CityMode { NORMAL, URGENT }

private data class TaskItem(
    val id: Long,
    val title: String,
    val urgent: Boolean,
    val deadline: Long
)

class MainActivity : Activity() {
    private lateinit var glView: CityGLView
    private lateinit var normalButton: Button
    private lateinit var urgentButton: Button
    private lateinit var cityLabel: TextView
    private var mode = CityMode.NORMAL
    private val prefs by lazy { getSharedPreferences("task_city_native", MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())

    private val expiryCheck = object : Runnable {
        override fun run() {
            checkExpiredUrgentTasks()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(7, 17, 31)
        window.navigationBarColor = Color.BLACK
        setContentView(buildUi())
        refreshCity()
        handler.post(expiryCheck)
    }

    override fun onResume() {
        super.onResume()
        if (::glView.isInitialized) glView.onResume()
        checkExpiredUrgentTasks()
    }

    override fun onPause() {
        if (::glView.isInitialized) glView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(expiryCheck)
        super.onDestroy()
    }

    private fun buildUi(): View {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(7, 17, 31))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        glView = CityGLView(this)
        root.addView(glView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            topMargin = dp(112)
            bottomMargin = dp(78)
        })

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(10), dp(14), dp(8))
            setBackgroundColor(Color.rgb(8, 20, 34))
        }
        val title = TextView(this).apply {
            text = "مدينتي"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        top.addView(title, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(42)
        ))

        val selectors = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        normalButton = selectorButton("مدينة الإنجاز") { switchMode(CityMode.NORMAL) }
        urgentButton = selectorButton("مدينة التحدي") { switchMode(CityMode.URGENT) }
        selectors.addView(normalButton, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginEnd = dp(5) })
        selectors.addView(urgentButton, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginStart = dp(5) })
        top.addView(selectors, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(48)
        ))
        root.addView(top, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(112), Gravity.TOP
        ))

        cityLabel = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = rounded(Color.argb(190, 8, 20, 34), 18f)
        }
        root.addView(cityLabel, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, dp(38), Gravity.TOP or Gravity.CENTER_HORIZONTAL
        ).apply { topMargin = dp(122) })

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundColor(Color.rgb(7, 17, 31))
        }
        val tasks = navButton("المهام") { showTasksDialog() }
        val add = navButton("+ مهمة") { showAddTaskDialog() }
        val city = navButton("المدينة") { refreshCity() }
        bottom.addView(tasks, LinearLayout.LayoutParams(0, dp(56), 1f))
        bottom.addView(add, LinearLayout.LayoutParams(0, dp(56), 1f).apply {
            marginStart = dp(6); marginEnd = dp(6)
        })
        bottom.addView(city, LinearLayout.LayoutParams(0, dp(56), 1f))
        root.addView(bottom, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(78), Gravity.BOTTOM
        ))

        return root
    }

    private fun selectorButton(label: String, click: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 14f
        isAllCaps = false
        setTextColor(Color.WHITE)
        setPadding(dp(4), 0, dp(4), 0)
        setOnClickListener { click() }
    }

    private fun navButton(label: String, click: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 14f
        isAllCaps = false
        setTextColor(Color.WHITE)
        background = rounded(Color.rgb(18, 35, 52), 14f)
        setOnClickListener { click() }
    }

    private fun switchMode(newMode: CityMode) {
        mode = newMode
        refreshCity()
    }

    private fun refreshCity() {
        val count = if (mode == CityMode.NORMAL) normalHouses() else urgentHouses()
        glView.cityRenderer.cityMode = mode
        glView.cityRenderer.visibleHouseCount = count
        cityLabel.text = if (mode == CityMode.NORMAL) {
            "بيوت الإنجاز: $count"
        } else {
            "بيوت التحدي: $count"
        }
        normalButton.background = rounded(
            if (mode == CityMode.NORMAL) Color.rgb(20, 158, 118) else Color.rgb(21, 48, 57), 14f
        )
        urgentButton.background = rounded(
            if (mode == CityMode.URGENT) Color.rgb(115, 75, 205) else Color.rgb(42, 34, 70), 14f
        )
    }

    private fun normalHouses(): Int = prefs.getInt("normal_houses", 0)
    private fun urgentHouses(): Int = prefs.getInt("urgent_houses", 0)

    private fun setNormalHouses(value: Int) {
        prefs.edit().putInt("normal_houses", value.coerceAtLeast(0)).apply()
    }

    private fun setUrgentHouses(value: Int) {
        prefs.edit().putInt("urgent_houses", value.coerceAtLeast(0)).apply()
    }

    private fun showAddTaskDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), 0)
        }
        val title = EditText(this).apply {
            hint = "اكتب المهمة"
            textDirection = View.TEXT_DIRECTION_RTL
            gravity = Gravity.RIGHT
            maxLines = 2
        }
        box.addView(title, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        var minutesInput: EditText? = null
        if (mode == CityMode.URGENT) {
            minutesInput = EditText(this).apply {
                hint = "الوقت بالدقائق - مثال 60"
                inputType = InputType.TYPE_CLASS_NUMBER
                textDirection = View.TEXT_DIRECTION_RTL
                gravity = Gravity.RIGHT
                setText("60")
            }
            box.addView(minutesInput, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }

        AlertDialog.Builder(this)
            .setTitle(if (mode == CityMode.NORMAL) "مهمة عادية" else "مهمة ضرورية")
            .setView(box)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("إضافة") { _, _ ->
                val taskTitle = title.text.toString().trim()
                if (taskTitle.isEmpty()) return@setPositiveButton
                val urgent = mode == CityMode.URGENT
                val minutes = minutesInput?.text?.toString()?.toLongOrNull()?.coerceIn(1, 10080) ?: 60L
                val deadline = if (urgent) System.currentTimeMillis() + minutes * 60_000L else 0L
                val all = loadTasks().toMutableList()
                all.add(TaskItem(System.currentTimeMillis(), taskTitle, urgent, deadline))
                saveTasks(all)
                Toast.makeText(this, "تمت إضافة المهمة", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showTasksDialog() {
        checkExpiredUrgentTasks()
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(Color.rgb(242, 245, 247))
        }
        val heading = TextView(this).apply {
            text = if (mode == CityMode.NORMAL) "المهام العادية" else "المهام الضرورية"
            textSize = 22f
            setTextColor(Color.rgb(15, 25, 35))
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dp(10))
        }
        outer.addView(heading)

        val scroll = ScrollView(this)
        val listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val filtered = loadTasks().filter { it.urgent == (mode == CityMode.URGENT) }
        if (filtered.isEmpty()) {
            listBox.addView(TextView(this).apply {
                text = "لا توجد مهام الآن"
                textSize = 17f
                setTextColor(Color.DKGRAY)
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(30), dp(8), dp(30))
            })
        } else {
            filtered.forEach { item -> listBox.addView(taskRow(item, dialog)) }
        }
        scroll.addView(listBox)
        outer.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val close = Button(this).apply {
            text = "إغلاق"
            isAllCaps = false
            setOnClickListener { dialog.dismiss() }
        }
        outer.addView(close, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(52)
        ))
        dialog.setContentView(outer)
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.94f).toInt(),
            (resources.displayMetrics.heightPixels * 0.78f).toInt())
        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.94f).toInt(),
            (resources.displayMetrics.heightPixels * 0.78f).toInt())
    }

    private fun taskRow(item: TaskItem, dialog: Dialog): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = rounded(Color.WHITE, 12f)
        }
        val text = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.rgb(20, 30, 40))
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            textDirection = View.TEXT_DIRECTION_RTL
            val remain = if (item.urgent) formatRemaining(item.deadline - System.currentTimeMillis()) else ""
            text = if (remain.isBlank()) item.title else "${item.title}\n$remain"
        }
        row.addView(text, LinearLayout.LayoutParams(0, dp(64), 1f))
        val done = Button(this).apply {
            text = "✓"
            textSize = 24f
            setTextColor(Color.WHITE)
            background = rounded(if (item.urgent) Color.rgb(115, 75, 205) else Color.rgb(20, 158, 118), 14f)
            setOnClickListener {
                completeTask(item)
                dialog.dismiss()
                refreshCity()
                Toast.makeText(this@MainActivity, "تمت المهمة وبُني بيت جديد", Toast.LENGTH_SHORT).show()
            }
        }
        row.addView(done, LinearLayout.LayoutParams(dp(58), dp(54)).apply { marginStart = dp(8) })
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(72)))
            addView(View(this@MainActivity), LinearLayout.LayoutParams(1, dp(6)))
        }
    }

    private fun completeTask(item: TaskItem) {
        val all = loadTasks().filterNot { it.id == item.id }
        saveTasks(all)
        if (item.urgent) setUrgentHouses(urgentHouses() + 1)
        else setNormalHouses(normalHouses() + 1)
    }

    private fun checkExpiredUrgentTasks() {
        val now = System.currentTimeMillis()
        val all = loadTasks()
        var expired = 0
        val kept = all.filter {
            val isExpired = it.urgent && it.deadline > 0 && it.deadline <= now
            if (isExpired) expired++
            !isExpired
        }
        if (expired > 0) {
            saveTasks(kept)
            setUrgentHouses(max(0, urgentHouses() - expired))
            if (mode == CityMode.URGENT) refreshCity()
        }
    }

    private fun loadTasks(): List<TaskItem> {
        return try {
            val arr = JSONArray(prefs.getString("tasks", "[]") ?: "[]")
            val result = ArrayList<TaskItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                result.add(TaskItem(
                    o.optLong("id"), o.optString("title"), o.optBoolean("urgent"), o.optLong("deadline")
                ))
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveTasks(tasks: List<TaskItem>) {
        val arr = JSONArray()
        tasks.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("title", it.title)
                put("urgent", it.urgent)
                put("deadline", it.deadline)
            })
        }
        prefs.edit().putString("tasks", arr.toString()).apply()
    }

    private fun formatRemaining(ms: Long): String {
        val total = max(0L, ms / 1000L)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "متبقي %02d:%02d:%02d".format(h, m, s)
        else "متبقي %02d:%02d".format(m, s)
    }

    private fun rounded(color: Int, radiusDp: Float): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun dp(value: Float): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}

private class CityGLView(context: Context) : GLSurfaceView(context) {
    val cityRenderer = CityRenderer(context)
    private var lastX = 0f
    private var lastY = 0f
    private var pinch = 0f

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
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) pinch = distance(event)
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    val now = distance(event)
                    if (pinch > 0f && now > 0f) cityRenderer.zoomBy(pinch / now)
                    pinch = now
                } else {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    cityRenderer.rotateBy(dx * 0.35f, dy * 0.18f)
                    lastX = event.x
                    lastY = event.y
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> pinch = 0f
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

private data class MaterialInfo(
    val name: String,
    var r: Float = 1f,
    var g: Float = 1f,
    var b: Float = 1f,
    var texture: String? = null,
    var textureId: Int = 0
)

private data class DrawBatch(
    val group: String,
    val material: String,
    val buffer: FloatBuffer,
    val vertexCount: Int
)

private class CityRenderer(private val context: Context) : GLSurfaceView.Renderer {
    @Volatile var visibleHouseCount: Int = 0
    @Volatile var cityMode: CityMode = CityMode.NORMAL

    private var program = 0
    private var aPos = 0
    private var aUv = 0
    private var uMvp = 0
    private var uTint = 0
    private var uBase = 0
    private var uHasTex = 0
    private var uTex = 0

    private val batches = ArrayList<DrawBatch>()
    private val materials = LinkedHashMap<String, MaterialInfo>()
    private var buildingNames = emptyList<String>()
    private var cx = 0f
    private var cy = 0f
    private var cz = 0f
    private var scale = 1f
    private var yaw = -35f
    private var pitch = 25f
    private var cameraDistance = 4.8f
    private var width = 1
    private var height = 1
    private var loadFailed = false

    fun rotateBy(dx: Float, dy: Float) {
        yaw += dx
        pitch = (pitch + dy).coerceIn(8f, 55f)
    }

    fun zoomBy(factor: Float) {
        cameraDistance = (cameraDistance * factor).coerceIn(2.8f, 8.0f)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        program = makeProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        aPos = GLES20.glGetAttribLocation(program, "aPos")
        aUv = GLES20.glGetAttribLocation(program, "aUv")
        uMvp = GLES20.glGetUniformLocation(program, "uMvp")
        uTint = GLES20.glGetUniformLocation(program, "uTint")
        uBase = GLES20.glGetUniformLocation(program, "uBase")
        uHasTex = GLES20.glGetUniformLocation(program, "uHasTex")
        uTex = GLES20.glGetUniformLocation(program, "uTex")
        try {
            loadModel()
            materials.values.forEach { material ->
                material.texture?.let { material.textureId = loadTexture(it) }
            }
        } catch (e: Exception) {
            loadFailed = true
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "تعذر تحميل المدينة: ${e.message ?: "خطأ"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        width = max(1, w)
        height = max(1, h)
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (cityMode == CityMode.NORMAL) GLES20.glClearColor(0.54f, 0.78f, 0.79f, 1f)
        else GLES20.glClearColor(0.12f, 0.10f, 0.24f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (loadFailed || batches.isEmpty()) return

        val model = FloatArray(16)
        val view = FloatArray(16)
        val proj = FloatArray(16)
        val mv = FloatArray(16)
        val mvp = FloatArray(16)
        Matrix.setIdentityM(model, 0)
        Matrix.rotateM(model, 0, pitch, 1f, 0f, 0f)
        Matrix.rotateM(model, 0, yaw, 0f, 1f, 0f)
        Matrix.scaleM(model, 0, scale, scale, scale)
        Matrix.translateM(model, 0, -cx, -cy, -cz)
        Matrix.setLookAtM(view, 0, 0f, 0.9f, cameraDistance, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.perspectiveM(proj, 0, 42f, width.toFloat() / height.toFloat(), 0.1f, 100f)
        Matrix.multiplyMM(mv, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, proj, 0, mv, 0)

        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        val tint = if (cityMode == CityMode.NORMAL) floatArrayOf(1f, 1f, 1f, 1f)
        else floatArrayOf(0.82f, 0.76f, 1f, 1f)
        GLES20.glUniform4fv(uTint, 1, tint, 0)
        GLES20.glUniform1i(uTex, 0)

        for (batch in batches) {
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
        GLES20.glDisableVertexAttribArray(aUv)
    }

    private fun loadModel() {
        materials.clear()
        batches.clear()
        parseMtl("city/scene.mtl")

        val positions = ArrayList<FloatArray>()
        val uvs = ArrayList<FloatArray>()
        val builders = LinkedHashMap<String, MutableList<Float>>()
        var currentGroup = "scene"
        var currentMaterial = "default"
        materials.putIfAbsent("default", MaterialInfo("default", 0.72f, 0.78f, 0.82f))

        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY

        BufferedReader(InputStreamReader(context.assets.open("city/scene.obj"))).use { reader ->
            reader.forEachLine { raw ->
                val line = raw.trim()
                when {
                    line.startsWith("v ") -> {
                        val p = line.substring(2).trim().split(Regex("\\s+"))
                        if (p.size >= 3) {
                            val x = p[0].toFloat(); val y = p[1].toFloat(); val z = p[2].toFloat()
                            positions.add(floatArrayOf(x, y, z))
                            minX = min(minX, x); minY = min(minY, y); minZ = min(minZ, z)
                            maxX = max(maxX, x); maxY = max(maxY, y); maxZ = max(maxZ, z)
                        }
                    }
                    line.startsWith("vt ") -> {
                        val p = line.substring(3).trim().split(Regex("\\s+"))
                        if (p.size >= 2) uvs.add(floatArrayOf(p[0].toFloat(), p[1].toFloat()))
                    }
                    line.startsWith("o ") || line.startsWith("g ") -> {
                        val name = line.substring(2).trim()
                        if (name.isNotEmpty()) currentGroup = cleanGroupName(name)
                    }
                    line.startsWith("usemtl ") -> currentMaterial = line.substring(7).trim()
                    line.startsWith("f ") -> {
                        val verts = line.substring(2).trim().split(Regex("\\s+"))
                        if (verts.size >= 3) {
                            for (i in 1 until verts.size - 1) {
                                appendFaceVertex(builders, currentGroup, currentMaterial, verts[0], positions, uvs)
                                appendFaceVertex(builders, currentGroup, currentMaterial, verts[i], positions, uvs)
                                appendFaceVertex(builders, currentGroup, currentMaterial, verts[i + 1], positions, uvs)
                            }
                        }
                    }
                }
            }
        }

        if (positions.isEmpty()) throw IllegalStateException("OBJ empty")
        cx = (minX + maxX) * 0.5f
        cy = (minY + maxY) * 0.5f
        cz = (minZ + maxZ) * 0.5f
        val extent = max(maxX - minX, max(maxY - minY, maxZ - minZ))
        scale = if (extent > 0f) 2.8f / extent else 1f

        builders.forEach { (key, values) ->
            val split = key.split('|', limit = 2)
            val group = split[0]
            val material = if (split.size > 1) split[1] else "default"
            val arr = FloatArray(values.size)
            for (i in values.indices) arr[i] = values[i]
            val fb = ByteBuffer.allocateDirect(arr.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            fb.put(arr).position(0)
            batches.add(DrawBatch(group, material, fb, arr.size / 5))
        }

        val buildings = batches.map { it.group }.distinct().filter { isBuilding(it) }
        val preferred = listOf("house", "house_2", "house_3", "shop")
        buildingNames = buildings.sortedWith(compareBy<String> {
            val low = it.lowercase(Locale.US)
            val exact = preferred.indexOfFirst { p -> low == p || low.contains(p) }
            if (exact >= 0) exact else 100
        }.thenBy { it })
    }

    private fun appendFaceVertex(
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
    }

    private fun objIndex(text: String, size: Int): Int {
        val n = text.toIntOrNull() ?: return -1
        return if (n > 0) n - 1 else size + n
    }

    private fun parseMtl(path: String) {
        var current: MaterialInfo? = null
        try {
            BufferedReader(InputStreamReader(context.assets.open(path))).use { reader ->
                reader.forEachLine { raw ->
                    val line = raw.trim()
                    when {
                        line.startsWith("newmtl ") -> {
                            val name = line.substring(7).trim()
                            current = MaterialInfo(name)
                            materials[name] = current!!
                        }
                        line.startsWith("Kd ") -> {
                            val p = line.substring(3).trim().split(Regex("\\s+"))
                            if (p.size >= 3) {
                                current?.r = p[0].toFloatOrNull() ?: 1f
                                current?.g = p[1].toFloatOrNull() ?: 1f
                                current?.b = p[2].toFloatOrNull() ?: 1f
                            }
                        }
                        line.startsWith("map_Kd ") -> current?.texture = line.substring(7).trim()
                    }
                }
            }
        } catch (_: Exception) {
            // Some exporters omit MTL. Geometry will still render with a neutral color.
        }
    }

    private fun cleanGroupName(name: String): String {
        return name.substringBefore(' ').ifBlank { "scene" }
    }

    private fun isBuilding(name: String): Boolean {
        val n = name.lowercase(Locale.US)
        return n.contains("house") || n.contains("shop") || n.contains("building")
    }

    private fun loadTexture(reference: String): Int {
        val normalized = reference.replace('\\', '/').removePrefix("./")
        val candidates = listOf(
            "city/$normalized",
            "city/textures/${File(normalized).name}",
            "city/${File(normalized).name}"
        )
        var bitmap: android.graphics.Bitmap? = null
        for (candidate in candidates) {
            try {
                context.assets.open(candidate).use { bitmap = BitmapFactory.decodeStream(it) }
                if (bitmap != null) break
            } catch (_: Exception) { }
        }
        val image = bitmap ?: return 0
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, image, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        image.recycle()
        return ids[0]
    }

    private fun makeProgram(vertex: String, fragment: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertex)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragment)
        return GLES20.glCreateProgram().also { p ->
            GLES20.glAttachShader(p, vs)
            GLES20.glAttachShader(p, fs)
            GLES20.glLinkProgram(p)
            val ok = IntArray(1)
            GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0)
            if (ok[0] == 0) throw IllegalStateException(GLES20.glGetProgramInfoLog(p))
        }
    }

    private fun compileShader(type: Int, source: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val ok = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, ok, 0)
            if (ok[0] == 0) throw IllegalStateException(GLES20.glGetShaderInfoLog(shader))
        }
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 uMvp;
            attribute vec3 aPos;
            attribute vec2 aUv;
            varying vec2 vUv;
            void main() {
                gl_Position = uMvp * vec4(aPos, 1.0);
                vUv = aUv;
            }
        """
        private const val FRAGMENT_SHADER = """
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
        """
    }
}
