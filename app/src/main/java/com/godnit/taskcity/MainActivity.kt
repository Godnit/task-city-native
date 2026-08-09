package com.godnit.taskcity

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.Gravity
import android.view.SurfaceView
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.filament.Renderer
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.math.max

class MainActivity : AppCompatActivity() {
    enum class CityMode { NORMAL, URGENT }

    data class TaskItem(
        val id: String,
        val title: String,
        val urgent: Boolean,
        val deadline: Long
    )

    private lateinit var modelViewer: ModelViewer
    private lateinit var surfaceView: SurfaceView
    private lateinit var titleView: TextView
    private lateinit var subtitleView: TextView
    private lateinit var taskList: LinearLayout
    private lateinit var addButton: Button
    private lateinit var normalNav: TextView
    private lateinit var urgentNav: TextView
    private lateinit var sceneCard: FrameLayout

    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("task_city", MODE_PRIVATE) }
    private var mode = CityMode.NORMAL
    private val tasks = mutableListOf<TaskItem>()
    private var normalHouses = 0
    private var urgentHouses = 0
    private var modelReady = false

    private val buildingNames = listOf(
        "House_World ap_0",
        "House_2_World ap_0",
        "House_3_World ap_0",
        "Shop_World ap_0"
    )

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            Choreographer.getInstance().postFrameCallback(this)
            modelViewer.render(frameTimeNanos)
            if (!modelReady && modelViewer.progress >= 0.99f) {
                modelReady = true
                applyHouseVisibility()
            }
        }
    }

    private val timerTick = object : Runnable {
        override fun run() {
            processExpiredTasks()
            refreshTaskList()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(7, 14, 25)
        window.navigationBarColor = Color.rgb(7, 14, 25)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL

        Utils.init()
        Gltfio.init()
        loadState()
        buildUi()
        setup3d()
        switchCity(CityMode.NORMAL)
        handler.post(timerTick)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(7, 14, 25))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(8))
        }
        titleView = TextView(this).apply {
            text = "مدينة الإنجاز"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.END
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        subtitleView = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.rgb(165, 181, 201))
            gravity = Gravity.END
            setPadding(0, dp(4), 0, 0)
        }
        header.addView(titleView)
        header.addView(subtitleView)
        root.addView(header, LinearLayout.LayoutParams(-1, -2))

        sceneCard = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(12, 47, 51))
        }
        surfaceView = SurfaceView(this)
        sceneCard.addView(surfaceView, FrameLayout.LayoutParams(-1, -1))
        val hint = TextView(this).apply {
            text = "اسحب لتدوير المدينة • قرّب بإصبعين"
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.argb(145, 5, 12, 20))
            setPadding(dp(12), dp(7), dp(12), dp(7))
            gravity = Gravity.CENTER
        }
        sceneCard.addView(hint, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            bottomMargin = dp(12)
        })
        root.addView(sceneCard, LinearLayout.LayoutParams(-1, 0, 4.8f))

        val tasksPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(8))
            setBackgroundColor(Color.rgb(10, 19, 32))
        }
        val taskHeader = TextView(this).apply {
            text = "المهام"
            textSize = 20f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.END
            setPadding(0, 0, 0, dp(6))
        }
        tasksPanel.addView(taskHeader)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        taskList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(taskList)
        tasksPanel.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        addButton = Button(this).apply {
            text = "+  إضافة مهمة"
            textSize = 16f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(44, 196, 132))
            setOnClickListener { showAddTaskDialog() }
        }
        tasksPanel.addView(addButton, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(6) })
        root.addView(tasksPanel, LinearLayout.LayoutParams(-1, 0, 3.6f))

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setBackgroundColor(Color.rgb(8, 15, 26))
        }
        normalNav = navItem("⌂\nمدينة الإنجاز") { switchCity(CityMode.NORMAL) }
        val center = navItem("✓\nالمهام") { refreshTaskList() }
        urgentNav = navItem("◈\nمدينة التحدي") { switchCity(CityMode.URGENT) }
        nav.addView(normalNav, LinearLayout.LayoutParams(0, dp(62), 1f))
        nav.addView(center, LinearLayout.LayoutParams(0, dp(62), 1f))
        nav.addView(urgentNav, LinearLayout.LayoutParams(0, dp(62), 1f))
        root.addView(nav, LinearLayout.LayoutParams(-1, dp(70)))

        setContentView(root)
    }

    private fun navItem(label: String, action: () -> Unit): TextView = TextView(this).apply {
        text = label
        textSize = 12f
        gravity = Gravity.CENTER
        setTextColor(Color.rgb(165, 181, 201))
        setOnClickListener { action() }
    }

    private fun setup3d() {
        modelViewer = ModelViewer(surfaceView)
        surfaceView.setOnTouchListener(modelViewer)
        val glb = readAssetBuffer("city/city_lite.glb")
        modelViewer.loadModelGlb(glb)
        modelViewer.transformToUnitCube()
        modelViewer.view.isPostProcessingEnabled = true
        setSceneColors()
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun setSceneColors() {
        if (!::modelViewer.isInitialized) return
        val options: Renderer.ClearOptions = modelViewer.renderer.clearOptions
        options.clear = true
        if (mode == CityMode.NORMAL) {
            options.clearColor = floatArrayOf(0.05f, 0.28f, 0.34f, 1f)
        } else {
            options.clearColor = floatArrayOf(0.06f, 0.035f, 0.16f, 1f)
        }
        modelViewer.renderer.clearOptions = options
    }

    private fun switchCity(newMode: CityMode) {
        mode = newMode
        val normal = mode == CityMode.NORMAL
        titleView.text = if (normal) "مدينة الإنجاز" else "مدينة التحدي"
        sceneCard.setBackgroundColor(if (normal) Color.rgb(12, 73, 77) else Color.rgb(31, 20, 72))
        addButton.setBackgroundColor(if (normal) Color.rgb(44, 196, 132) else Color.rgb(124, 82, 255))
        normalNav.setTextColor(if (normal) Color.rgb(66, 231, 162) else Color.rgb(165, 181, 201))
        urgentNav.setTextColor(if (!normal) Color.rgb(167, 119, 255) else Color.rgb(165, 181, 201))
        setSceneColors()
        applyHouseVisibility()
        refreshTaskList()
    }

    private fun refreshHeader() {
        val count = if (mode == CityMode.NORMAL) normalHouses else urgentHouses
        val open = tasks.count { it.urgent == (mode == CityMode.URGENT) }
        subtitleView.text = "$count مبانٍ  •  $open مهام مفتوحة"
    }

    private fun refreshTaskList() {
        if (!::taskList.isInitialized) return
        refreshHeader()
        taskList.removeAllViews()
        val visible = tasks.filter { it.urgent == (mode == CityMode.URGENT) }
        if (visible.isEmpty()) {
            taskList.addView(TextView(this).apply {
                text = if (mode == CityMode.NORMAL) "لا توجد مهام عادية الآن." else "لا توجد مهام ضرورية الآن."
                textSize = 15f
                setTextColor(Color.rgb(150, 164, 184))
                gravity = Gravity.CENTER
                setPadding(0, dp(30), 0, dp(30))
            })
            return
        }
        for (task in visible) taskList.addView(taskRow(task))
    }

    private fun taskRow(task: TaskItem): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(if (task.urgent) Color.rgb(22, 22, 52) else Color.rgb(12, 37, 36))
        }
        val check = Button(this).apply {
            text = "✓"
            textSize = 18f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { completeTask(task) }
        }
        row.addView(check, LinearLayout.LayoutParams(dp(52), dp(48)))
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }
        info.addView(TextView(this).apply {
            text = task.title
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.END
            maxLines = 2
        })
        if (task.urgent) {
            info.addView(TextView(this).apply {
                val remain = max(0L, task.deadline - System.currentTimeMillis())
                text = "الوقت المتبقي: ${formatTime(remain)}"
                textSize = 13f
                setTextColor(Color.rgb(244, 94, 114))
                gravity = Gravity.END
            })
        }
        row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(row, LinearLayout.LayoutParams(-1, -2))
            setPadding(0, 0, 0, dp(7))
        }
    }

    private fun showAddTaskDialog() {
        val urgent = mode == CityMode.URGENT
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val input = EditText(this).apply {
            hint = "اكتب المهمة..."
            textDirection = View.TEXT_DIRECTION_RTL
            gravity = Gravity.END
            maxLines = 3
        }
        box.addView(input, LinearLayout.LayoutParams(-1, -2))

        val minutes = if (urgent) NumberPicker(this).apply {
            minValue = 1
            maxValue = 360
            value = 60
            wrapSelectorWheel = false
        } else null
        if (minutes != null) {
            box.addView(TextView(this).apply {
                text = "المدة بالدقائق (حتى 6 ساعات)"
                gravity = Gravity.END
                setPadding(0, dp(12), 0, dp(4))
            })
            box.addView(minutes)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (urgent) "مهمة ضرورية جديدة" else "مهمة عادية جديدة")
            .setView(box)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("إضافة", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = input.text.toString().trim()
                if (title.isEmpty()) {
                    input.error = "اكتب اسم المهمة"
                    return@setOnClickListener
                }
                val deadline = if (urgent) System.currentTimeMillis() + (minutes!!.value * 60_000L) else 0L
                tasks.add(TaskItem(UUID.randomUUID().toString(), title, urgent, deadline))
                saveState()
                refreshTaskList()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun completeTask(task: TaskItem) {
        tasks.removeAll { it.id == task.id }
        if (task.urgent) urgentHouses++ else normalHouses++
        saveState()
        applyHouseVisibility()
        refreshTaskList()
        Toast.makeText(this, "أحسنت! تم بناء مبنى جديد", Toast.LENGTH_SHORT).show()
    }

    private fun processExpiredTasks() {
        val now = System.currentTimeMillis()
        val expired = tasks.filter { it.urgent && it.deadline > 0 && now >= it.deadline }
        if (expired.isEmpty()) return
        expired.forEach { tasks.removeAll { t -> t.id == it.id } }
        repeat(expired.size) { if (urgentHouses > 0) urgentHouses-- }
        saveState()
        applyHouseVisibility()
        Toast.makeText(this, "انتهى الوقت، تم هدم مبنى من مدينة التحدي", Toast.LENGTH_LONG).show()
    }

    private fun applyHouseVisibility() {
        if (!modelReady || !::modelViewer.isInitialized) return
        val asset = modelViewer.asset ?: return
        val count = if (mode == CityMode.NORMAL) normalHouses else urgentHouses
        buildingNames.forEachIndexed { index, name ->
            val entity = asset.getFirstEntityByName(name)
            if (entity != 0) {
                if (index < count) modelViewer.scene.addEntity(entity)
                else modelViewer.scene.removeEntity(entity)
            }
        }
    }

    private fun loadState() {
        normalHouses = prefs.getInt("normal_houses", 0)
        urgentHouses = prefs.getInt("urgent_houses", 0)
        val raw = prefs.getString("tasks", "[]") ?: "[]"
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                tasks.add(TaskItem(o.getString("id"), o.getString("title"), o.getBoolean("urgent"), o.optLong("deadline", 0)))
            }
        }
    }

    private fun saveState() {
        val arr = JSONArray()
        tasks.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("urgent", t.urgent)
                put("deadline", t.deadline)
            })
        }
        prefs.edit()
            .putInt("normal_houses", normalHouses)
            .putInt("urgent_houses", urgentHouses)
            .putString("tasks", arr.toString())
            .apply()
    }

    private fun readAssetBuffer(path: String): ByteBuffer {
        val bytes = assets.open(path).use { it.readBytes() }
        return ByteBuffer.wrap(bytes)
    }

    private fun formatTime(ms: Long): String {
        val total = ms / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        handler.removeCallbacks(timerTick)
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        if (::modelViewer.isInitialized) modelViewer.destroy()
        super.onDestroy()
    }
}
