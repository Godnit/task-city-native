package com.godnit.handgesturecube

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), HandLandmarkerHelper.Listener {
    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var cubeView: CubeView
    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private lateinit var fpsText: TextView
    private lateinit var gestureText: TextView
    private lateinit var fingersText: TextView
    private lateinit var actionButton: Button
    private lateinit var hintText: TextView
    private lateinit var executor: ExecutorService
    private var helper: HandLandmarkerHelper? = null
    private var handFrames = 0
    private var totalResultFrames = 0
    private var lastResultAt = 0L
    private var smoothFps = 0f
    private var cubeMode = false
    private var cubeGrabbed = false
    private var missedHandFrames = 0
    private val uiHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(7, 17, 31)
        window.navigationBarColor = Color.rgb(7, 17, 31)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        executor = Executors.newSingleThreadExecutor()
        if (intent.getBooleanExtra(TRACKER_SELF_TEST, false)) {
            runCompatibilitySelfTest()
            return
        }
        buildUi()
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startNativeTracker()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST)
        }
    }

    private fun runCompatibilitySelfTest() {
        setContentView(TextView(this).apply {
            text = "Android 8 tracker self-test"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
            gravity = Gravity.CENTER
        })
        executor.execute {
            try {
                HandLandmarkerHelper.runCompatibilitySelfTest(applicationContext)
            } catch (error: Throwable) {
                android.util.Log.e("HandGestureCube", "HAND_TRACKER_SELF_TEST_FAILED", error)
                throw error
            }
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(12))
            setBackgroundColor(Color.rgb(7, 17, 31))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        val title = TextView(this).apply {
            text = "اختبار تتبع اليد"
            textSize = 23f
            setTextColor(Color.WHITE)
            gravity = Gravity.START
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        statusText = TextView(this).apply {
            text = "جاري تجهيز المتتبع الأصلي…"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(55, 232, 178))
            background = rounded(Color.rgb(14, 42, 57), 18f, Color.rgb(28, 112, 105))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        header.addView(title, LinearLayout.LayoutParams(0, dp(58), 1f))
        header.addView(statusText, LinearLayout.LayoutParams(WRAP, WRAP))
        root.addView(header, LinearLayout.LayoutParams(MATCH, dp(64)))

        val stage = FrameLayout(this).apply {
            background = rounded(Color.rgb(8, 28, 47), 28f, Color.rgb(23, 65, 96))
            clipToOutline = true
        }
        previewView = PreviewView(this).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
        overlayView = OverlayView(this)
        cubeView = CubeView(this).apply { visibility = View.GONE }
        hintText = TextView(this).apply {
            text = "🖐️\nارفع يدًا واحدة أمام الكاميرا\nاجعل كامل الكف والأصابع داخل الإطار"
            textSize = 19f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setShadowLayer(10f, 0f, 2f, Color.BLACK)
        }
        fpsText = TextView(this).apply {
            text = "FPS 0  •  -- ms"
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textDirection = View.TEXT_DIRECTION_LTR
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            background = rounded(Color.argb(170, 5, 18, 32), 14f, Color.TRANSPARENT)
            setPadding(dp(9), dp(5), dp(9), dp(5))
        }
        stage.addView(previewView, FrameLayout.LayoutParams(MATCH, MATCH))
        stage.addView(overlayView, FrameLayout.LayoutParams(MATCH, MATCH))
        stage.addView(cubeView, FrameLayout.LayoutParams(MATCH, MATCH))
        stage.addView(hintText, FrameLayout.LayoutParams(MATCH, MATCH))
        stage.addView(fpsText, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM or Gravity.START).apply {
            setMargins(dp(12), dp(12), dp(12), dp(12))
        })
        root.addView(stage, LinearLayout.LayoutParams(MATCH, 0, 1f).apply { bottomMargin = dp(12) })

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.rgb(9, 31, 52), 24f, Color.rgb(22, 61, 91))
        }
        resultText = TextView(this).apply {
            text = "بانتظار تشغيل المتتبع…"
            textSize = 18f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.START
        }
        gestureText = TextView(this).apply {
            text = "الإيماءة: —"
            textSize = 15f
            setTextColor(Color.rgb(109, 200, 255))
            gravity = Gravity.START
            setPadding(0, dp(5), 0, dp(5))
        }
        fingersText = TextView(this).apply {
            text = "الإبهام —   السبابة —   الوسطى —   البنصر —   الخنصر —"
            textSize = 13f
            setTextColor(Color.rgb(190, 207, 222))
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(7), dp(6), dp(9))
        }
        actionButton = Button(this).apply {
            text = "ابدأ التحكم بالمكعب"
            textSize = 17f
            isEnabled = false
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(43, 77, 105), 20f, Color.TRANSPARENT)
            setOnClickListener {
                cubeMode = !cubeMode
                cubeGrabbed = false
                cubeView.release()
                cubeView.visibility = if (cubeMode) View.VISIBLE else View.GONE
                overlayView.visibility = if (cubeMode) View.GONE else View.VISIBLE
                hintText.visibility = View.GONE
                text = if (cubeMode) "العودة إلى اختبار اليد" else "ابدأ التحكم بالمكعب"
                title.text = if (cubeMode) "تحكم بالمكعب بيدك" else "اختبار تتبع اليد"
                resultText.text = if (cubeMode) {
                    "المكعب ثابت • ضم الإبهام والسبابة 👌 للإمساك به"
                } else {
                    "الخطوط تتبع يدك بنجاح"
                }
            }
        }
        panel.addView(resultText, LinearLayout.LayoutParams(MATCH, WRAP))
        panel.addView(gestureText, LinearLayout.LayoutParams(MATCH, WRAP))
        panel.addView(fingersText, LinearLayout.LayoutParams(MATCH, WRAP))
        panel.addView(actionButton, LinearLayout.LayoutParams(MATCH, dp(54)))
        root.addView(panel, LinearLayout.LayoutParams(MATCH, WRAP))
        setContentView(root)
    }

    private fun startNativeTracker() {
        statusText.text = "تحميل نموذج Android الأصلي…"
        executor.execute {
            helper = HandLandmarkerHelper(applicationContext, this)
            helper?.setup()
        }
        uiHandler.postDelayed({
            if (totalResultFrames == 0 && !isFinishing) {
                statusText.text = "الكاميرا تعمل، ننتظر أول نتيجة من النموذج…"
                statusText.setTextColor(Color.rgb(255, 196, 92))
            }
        }, 8000)
    }

    override fun onReady(delegateName: String) {
        runOnUiThread {
            statusText.text = "النموذج جاهز • تسريع $delegateName"
            resultText.text = "المتتبع يعمل — ارفع يدك الآن"
            bindCamera()
        }
    }

    @SuppressLint("MissingPermission")
    private fun bindCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                val provider = future.get()
                val selector = CameraSelector.DEFAULT_FRONT_CAMERA
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    // The bundled palm/landmark models consume 192x192 and
                    // 224x224 inputs. 240x320 preserves enough source detail
                    // while avoiding four times the camera preprocessing work.
                    .setTargetResolution(Size(240, 320))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                analysis.setAnalyzer(executor) { image -> helper?.detect(image, true) ?: image.close() }
                provider.unbindAll()
                provider.bindToLifecycle(this, selector, preview, analysis)
                statusText.text = "المتتبع يعمل • ابحث عن اليد"
            } catch (error: Throwable) {
                onError("تعذّر تشغيل الكاميرا الأمامية: ${error.message ?: error.javaClass.simpleName}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onResults(bundle: HandLandmarkerHelper.ResultBundle) {
        runOnUiThread {
            totalResultFrames++
            val now = System.currentTimeMillis()
            if (lastResultAt > 0) {
                val instant = 1000f / (now - lastResultAt).coerceAtLeast(1)
                smoothFps = if (smoothFps == 0f) instant else smoothFps * 0.86f + instant * 0.14f
            }
            lastResultAt = now
            fpsText.text = "FPS ${smoothFps.toInt()}  •  ${bundle.inferenceMs} ms"

            val landmarks = bundle.result.landmarks().firstOrNull()
            if (landmarks == null) {
                overlayView.clear()
                handFrames = (handFrames - 1).coerceAtLeast(0)
                missedHandFrames++
                if (missedHandFrames >= 3) {
                    statusText.text = "المتتبع يعمل • لم يجد يدًا"
                    if (!cubeMode) {
                        resultText.text = "لم تظهر اليد — أبعدها قليلًا واجعل الكف كاملًا"
                        hintText.visibility = View.VISIBLE
                    } else if (cubeGrabbed) {
                        cubeGrabbed = false
                        cubeView.release()
                        resultText.text = "توقّف المكعب • أظهر يدك ثم أمسكه مجددًا 👌"
                    }
                }
                return@runOnUiThread
            }

            missedHandFrames = 0
            handFrames++
            overlayView.setResults(bundle.result, bundle.inputWidth, bundle.inputHeight)
            hintText.visibility = View.GONE
            statusText.text = "تم اكتشاف اليد ✓"
            statusText.setTextColor(Color.rgb(55, 232, 178))
            val hand = analyze(landmarks)
            gestureText.text = "${hand.emoji}  ${hand.name}"
            fingersText.text = listOf(
                "الإبهام ${mark(hand.thumb)}", "السبابة ${mark(hand.index)}", "الوسطى ${mark(hand.middle)}",
                "البنصر ${mark(hand.ring)}", "الخنصر ${mark(hand.pinky)}"
            ).joinToString("   ")
            if (!cubeMode) resultText.text = "الخطوط تتبع يدك بنجاح • ${handFrames.coerceAtMost(20) * 5}%"
            if (handFrames >= 8) {
                actionButton.isEnabled = true
                actionButton.background = rounded(Color.rgb(37, 174, 141), 20f, Color.TRANSPARENT)
            }
            if (cubeMode) {
                val palm = distance(landmarks[5], landmarks[17]).coerceAtLeast(0.001f)
                val pinch = distance(landmarks[4], landmarks[8]) / palm
                // Two different thresholds stop the cube rapidly alternating
                // between held/released when fingertips hover near the limit.
                if (!cubeGrabbed && pinch < GRAB_THRESHOLD) cubeGrabbed = true
                if (cubeGrabbed && pinch > RELEASE_THRESHOLD) cubeGrabbed = false

                val pinchX = (landmarks[4].x() + landmarks[8].x()) * 0.5f
                val pinchY = (landmarks[4].y() + landmarks[8].y()) * 0.5f
                cubeView.setGrab(cubeGrabbed, pinchX, pinchY)
                if (cubeGrabbed) {
                    gestureText.text = "👌  تم الإمساك بالمكعب"
                    resultText.text = "حرّك يدك • افتح الإبهام والسبابة لترك المكعب"
                } else {
                    resultText.text = "المكعب ثابت • ضم الإبهام والسبابة 👌 للإمساك به"
                }
            }
        }
    }

    override fun onError(message: String) {
        runOnUiThread {
            statusText.text = "خطأ في المتتبع"
            statusText.setTextColor(Color.rgb(255, 110, 110))
            resultText.text = message
            fpsText.text = "المتتبع متوقف"
        }
    }

    private fun analyze(p: List<NormalizedLandmark>): HandState {
        val thumb = extended(p, intArrayOf(1, 2, 3, 4), true)
        val index = extended(p, intArrayOf(5, 6, 7, 8), false)
        val middle = extended(p, intArrayOf(9, 10, 11, 12), false)
        val ring = extended(p, intArrayOf(13, 14, 15, 16), false)
        val pinky = extended(p, intArrayOf(17, 18, 19, 20), false)
        val count = listOf(thumb, index, middle, ring, pinky).count { it }
        val pinch = distance(p[4], p[8]) / distance(p[5], p[17]).coerceAtLeast(0.001f)
        val gesture = when {
            pinch < 0.38f -> "ممتاز" to "👌"
            count >= 4 -> "كف مفتوح" to "🖐️"
            count <= 1 && pinch > 0.55f -> "قبضة" to "✊"
            index && middle && !ring && !pinky -> "إصبعان" to "✌️"
            thumb && index && pinky && !middle && !ring -> "إشارة" to "🤟"
            else -> "أتتبع الحركة" to "🖖"
        }
        return HandState(thumb, index, middle, ring, pinky, gesture.first, gesture.second)
    }

    private fun extended(p: List<NormalizedLandmark>, ids: IntArray, thumb: Boolean): Boolean {
        val a = angle(p[ids[0]], p[ids[1]], p[ids[2]])
        val b = angle(p[ids[1]], p[ids[2]], p[ids[3]])
        val straight = if (thumb) a > 125 && b > 130 else a > 140 && b > 140
        val gain = distance(p[ids[3]], p[0]) > distance(p[ids[1]], p[0]) * if (thumb) 1.02f else 1.08f
        return straight && gain
    }

    private fun angle(a: NormalizedLandmark, b: NormalizedLandmark, c: NormalizedLandmark): Double {
        val ab = floatArrayOf(a.x() - b.x(), a.y() - b.y(), a.z() - b.z())
        val cb = floatArrayOf(c.x() - b.x(), c.y() - b.y(), c.z() - b.z())
        val dot = ab[0] * cb[0] + ab[1] * cb[1] + ab[2] * cb[2]
        val mag = hypot(hypot(ab[0].toDouble(), ab[1].toDouble()), ab[2].toDouble()) *
            hypot(hypot(cb[0].toDouble(), cb[1].toDouble()), cb[2].toDouble())
        if (mag == 0.0) return 0.0
        return Math.toDegrees(acos((dot / mag).coerceIn(-1.0, 1.0)))
    }

    private fun distance(a: NormalizedLandmark, b: NormalizedLandmark): Float {
        val dx = a.x() - b.x()
        val dy = a.y() - b.y()
        val dz = a.z() - b.z()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun mark(open: Boolean) = if (open) "✓" else "●"
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun rounded(fill: Int, radiusDp: Float, stroke: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fill)
        cornerRadius = radiusDp * resources.displayMetrics.density
        if (Color.alpha(stroke) > 0) setStroke(dp(1), stroke)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == CAMERA_REQUEST && results.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startNativeTracker()
        } else {
            onError("لم تسمح بإذن الكاميرا. افتح إعدادات التطبيق ثم فعّل الكاميرا.")
        }
    }

    override fun onDestroy() {
        helper?.close()
        executor.shutdown()
        super.onDestroy()
    }

    data class HandState(
        val thumb: Boolean, val index: Boolean, val middle: Boolean,
        val ring: Boolean, val pinky: Boolean, val name: String, val emoji: String
    )

    companion object {
        private const val CAMERA_REQUEST = 301
        private const val TRACKER_SELF_TEST = "tracker_self_test"
        private const val GRAB_THRESHOLD = 0.40f
        private const val RELEASE_THRESHOLD = 0.55f
        private const val MATCH = -1
        private const val WRAP = -2
    }
}
