package com.godnit.taskcity

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.max

class MainActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var cityView: CityGLSurfaceView
    private lateinit var houseLabel: TextView
    private lateinit var taskPanel: LinearLayout
    private lateinit var taskList: LinearLayout
    private lateinit var taskCountLabel: TextView
    private lateinit var tasksButton: TextView

    private lateinit var store: TaskStore
    private val handler = Handler(Looper.getMainLooper())
    private val countdownViews = LinkedHashMap<String, TextView>()
    private var taskPanelVisible = false

    private val ticker = object : Runnable {
        override fun run() {
            handleExpirations()
            updateCountdowns()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(48, 157, 204)
        window.navigationBarColor = Color.rgb(238, 244, 238)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        store = TaskStore(this)
        buildUi()
        cityView.setHouseCount(store.houseCount)
        handleExpirations(showToast = false)
        rebuildTaskList()
    }

    override fun onResume() {
        super.onResume()
        cityView.onResume()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    override fun onPause() {
        handler.removeCallbacks(ticker)
        cityView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun buildUi() {
        root = FrameLayout(this).apply {
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(Color.rgb(77, 190, 238))
        }

        cityView = CityGLSurfaceView(this)
        root.addView(
            cityView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        addTopHud()
        addTaskPanel()
        addBottomBar()
        addFloatingAddButton()

        setContentView(root)
    }

    private fun addTopHud() {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            setPadding(dp(16), dp(11), dp(16), dp(11))
            background = rounded(Color.argb(225, 255, 255, 255), 18f)
            elevation = dp(6).toFloat()
        }

        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val title = TextView(this).apply {
            text = "مدينة الإنجاز"
            textSize = 20f
            setTextColor(Color.rgb(35, 62, 51))
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.RIGHT
        }
        houseLabel = TextView(this).apply {
            textSize = 17f
            setTextColor(Color.rgb(34, 103, 69))
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(dp(14), 0, 0, 0)
        }
        titleRow.addView(title)
        titleRow.addView(houseLabel)

        val hint = TextView(this).apply {
            text = "اسحب لتحريك المدينة • إصبعان للتكبير والتصغير"
            textSize = 11.5f
            setTextColor(Color.rgb(91, 111, 102))
            gravity = Gravity.RIGHT
            setPadding(0, dp(3), 0, 0)
        }
        card.addView(titleRow)
        card.addView(hint)

        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP
            setMargins(dp(14), dp(14), dp(14), 0)
        }
        root.addView(card, lp)
        updateHouseLabel()
    }

    private fun addTaskPanel() {
        taskPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(10))
            background = rounded(Color.argb(245, 248, 251, 247), 24f)
            elevation = dp(12).toFloat()
            visibility = View.GONE
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val grabber = View(this).apply {
            background = rounded(Color.rgb(193, 204, 197), 20f)
        }
        taskPanel.addView(
            grabber,
            LinearLayout.LayoutParams(dp(46), dp(5)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(8)
            }
        )

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val heading = TextView(this).apply {
            text = "المهام الحالية"
            textSize = 19f
            setTextColor(Color.rgb(35, 61, 50))
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.RIGHT
        }
        taskCountLabel = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.rgb(93, 113, 104))
            gravity = Gravity.LEFT
        }
        header.addView(heading, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(taskCountLabel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        taskPanel.addView(header)

        val explainer = TextView(this).apply {
            text = "أنهِ المهمة قبل انتهاء العداد ليُبنى بيت جديد. إذا انتهى الوقت يُهدم بيت واحد."
            textSize = 12.5f
            setTextColor(Color.rgb(93, 110, 102))
            gravity = Gravity.RIGHT
            setPadding(0, dp(4), 0, dp(8))
        }
        taskPanel.addView(explainer)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        taskList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(0, 0, 0, dp(12))
        }
        scroll.addView(taskList)
        taskPanel.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(355)
        ).apply {
            gravity = Gravity.BOTTOM
            setMargins(dp(10), 0, dp(10), dp(74))
        }
        root.addView(taskPanel, lp)
    }

    private fun addBottomBar() {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(Color.argb(242, 250, 252, 249), 22f)
            setPadding(dp(8), dp(6), dp(8), dp(6))
            elevation = dp(12).toFloat()
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        tasksButton = navButton("✓  المهام").apply {
            setOnClickListener {
                setTaskPanelVisible(!taskPanelVisible)
            }
        }
        val cityButton = navButton("⌂  المدينة").apply {
            setOnClickListener { setTaskPanelVisible(false) }
        }
        bar.addView(cityButton, LinearLayout.LayoutParams(0, dp(48), 1f))
        bar.addView(tasksButton, LinearLayout.LayoutParams(0, dp(48), 1f))

        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(62)
        ).apply {
            gravity = Gravity.BOTTOM
            setMargins(dp(12), 0, dp(12), dp(8))
        }
        root.addView(bar, lp)
    }

    private fun addFloatingAddButton() {
        val add = TextView(this).apply {
            text = "+"
            textSize = 36f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
            background = rounded(Color.rgb(46, 160, 92), 40f)
            elevation = dp(18).toFloat()
            setOnClickListener { showAddTaskDialog() }
            contentDescription = "إضافة مهمة"
        }
        val lp = FrameLayout.LayoutParams(dp(62), dp(62)).apply {
            gravity = Gravity.BOTTOM or Gravity.RIGHT
            setMargins(0, 0, dp(24), dp(82))
        }
        root.addView(add, lp)
    }

    private fun navButton(label: String): TextView = TextView(this).apply {
        text = label
        textSize = 14.5f
        gravity = Gravity.CENTER
        setTextColor(Color.rgb(52, 86, 68))
        setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        background = rounded(Color.TRANSPARENT, 16f)
    }

    private fun setTaskPanelVisible(show: Boolean) {
        taskPanelVisible = show
        taskPanel.visibility = if (show) View.VISIBLE else View.GONE
        tasksButton.background = rounded(
            if (show) Color.rgb(224, 242, 228) else Color.TRANSPARENT,
            16f
        )
        if (show) rebuildTaskList()
    }

    private fun showAddTaskDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val titleInput = EditText(this).apply {
            hint = "مثال: مراجعة درس الأحياء"
            textSize = 16f
            gravity = Gravity.RIGHT
            isSingleLine = true
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(Color.rgb(241, 246, 241), 14f, Color.rgb(205, 220, 208))
        }
        box.addView(titleInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)))

        val label = TextView(this).apply {
            text = "اختر مدة المهمة"
            textSize = 14f
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(Color.rgb(55, 83, 68))
            gravity = Gravity.RIGHT
            setPadding(0, dp(16), 0, dp(6))
        }
        box.addView(label)

        val pickerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val hourBox = pickerBox("ساعة", 0, 23, 0)
        val minuteBox = pickerBox("دقيقة", 0, 11, 6, MINUTE_LABELS)
        pickerRow.addView(hourBox.first, LinearLayout.LayoutParams(0, dp(120), 1f).apply { marginEnd = dp(6) })
        pickerRow.addView(minuteBox.first, LinearLayout.LayoutParams(0, dp(120), 1f).apply { marginStart = dp(6) })
        box.addView(pickerRow)

        val dialog = AlertDialog.Builder(this)
            .setTitle("مهمة جديدة")
            .setView(box)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("ابدأ المهمة", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = titleInput.text?.toString()?.trim().orEmpty()
                if (title.isBlank()) {
                    titleInput.error = "اكتب اسم المهمة أولاً"
                    titleInput.requestFocus()
                    return@setOnClickListener
                }

                val hours = hourBox.second.value
                val minutes = minuteBox.second.value * 5
                val totalMinutes = max(1, hours * 60 + minutes)
                store.addTask(title, totalMinutes * 60_000L)
                dialog.dismiss()
                setTaskPanelVisible(true)
                rebuildTaskList()
                Toast.makeText(this, "بدأ العداد — بالتوفيق 👍", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun pickerBox(
        title: String,
        min: Int,
        max: Int,
        initial: Int,
        displayed: Array<String>? = null
    ): Pair<LinearLayout, NumberPicker> {
        val holder = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(245, 248, 244), 14f)
        }
        val picker = NumberPicker(this).apply {
            minValue = min
            maxValue = max
            value = initial.coerceIn(min, max)
            displayedValues = displayed
            descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
            wrapSelectorWheel = true
        }
        val caption = TextView(this).apply {
            text = title
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(83, 105, 93))
        }
        holder.addView(picker, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        holder.addView(caption, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(25)))
        return holder to picker
    }

    private fun rebuildTaskList() {
        val tasks = store.getTasks()
        taskList.removeAllViews()
        countdownViews.clear()
        taskCountLabel.text = "${tasks.size} مهمة"

        if (tasks.isEmpty()) {
            val empty = TextView(this).apply {
                text = "لا توجد مهام الآن\nاضغط + وابدأ أول مهمة لبناء بيت جديد 🏠"
                textSize = 15f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(105, 123, 113))
                setPadding(dp(20), dp(36), dp(20), dp(24))
            }
            taskList.addView(empty)
            return
        }

        tasks.forEach { task -> taskList.addView(taskCard(task)) }
        updateCountdowns()
    }

    private fun taskCard(task: TaskStore.CityTask): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(11), dp(14), dp(11))
            background = rounded(Color.WHITE, 16f, Color.rgb(222, 232, 224))
            elevation = dp(2).toFloat()
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val title = TextView(this).apply {
            text = task.title
            textSize = 16f
            setTextColor(Color.rgb(40, 61, 50))
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.RIGHT
        }
        card.addView(title)

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(0, dp(8), 0, 0)
        }

        val countdown = TextView(this).apply {
            textSize = 17f
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(Color.rgb(39, 123, 77))
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        }
        countdownViews[task.id] = countdown

        val complete = Button(this).apply {
            text = "تم ✓"
            textSize = 13f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(48, 157, 88), 13f)
            minHeight = 0
            minWidth = 0
            setPadding(dp(16), 0, dp(16), 0)
            setOnClickListener { completeTask(task.id) }
        }

        bottom.addView(countdown, LinearLayout.LayoutParams(0, dp(42), 1f))
        bottom.addView(complete, LinearLayout.LayoutParams(dp(82), dp(42)))
        card.addView(bottom)

        return card.apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(9) }
        }
    }

    private fun completeTask(id: String) {
        if (!store.completeTask(id)) return
        cityView.setHouseCount(store.houseCount, CityRenderer.HouseAnimation.BUILD)
        updateHouseLabel()
        rebuildTaskList()
        Toast.makeText(this, "أحسنت! بُني بيت جديد 🏠", Toast.LENGTH_SHORT).show()
    }

    private fun handleExpirations(showToast: Boolean = true) {
        val result = store.expireDueTasks()
        if (result.expiredTasks == 0) return

        val animation = if (result.destroyedHouses > 0) {
            CityRenderer.HouseAnimation.DEMOLISH
        } else {
            CityRenderer.HouseAnimation.NONE
        }
        cityView.setHouseCount(store.houseCount, animation)
        updateHouseLabel()
        rebuildTaskList()

        if (showToast) {
            val msg = if (result.destroyedHouses > 0) {
                "انتهى وقت مهمة — هُدم بيت واحد"
            } else {
                "انتهى وقت مهمة، ولا توجد بيوت لهدمها"
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun updateCountdowns() {
        val now = System.currentTimeMillis()
        val tasks = store.getTasks()
        for (task in tasks) {
            val view = countdownViews[task.id] ?: continue
            val remaining = (task.endAt - now).coerceAtLeast(0L)
            view.text = formatDuration(remaining)
            view.setTextColor(
                if (remaining <= 5 * 60_000L) Color.rgb(196, 76, 55)
                else Color.rgb(39, 123, 77)
            )
        }
    }

    private fun updateHouseLabel() {
        if (::houseLabel.isInitialized) houseLabel.text = "🏠 ${store.houseCount}"
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) {
            String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    private fun rounded(color: Int, radiusDp: Float, strokeColor: Int? = null): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
            if (strokeColor != null) setStroke(dp(1), strokeColor)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun dp(value: Float): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    companion object {
        private val MINUTE_LABELS = arrayOf("00", "05", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55")
    }
}
