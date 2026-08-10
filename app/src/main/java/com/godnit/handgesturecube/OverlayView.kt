package com.godnit.handgesturecube

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.view.View
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max

/**
 * Draws the hand skeleton at display refresh rate instead of jumping only when
 * the (slower) ML model returns a result. A short lost-frame grace period also
 * prevents one missed detection from making the skeleton flash off and on.
 */
class OverlayView(context: Context) : View(context) {
    private val currentX = FloatArray(LANDMARK_COUNT)
    private val currentY = FloatArray(LANDMARK_COUNT)
    private val targetX = FloatArray(LANDMARK_COUNT)
    private val targetY = FloatArray(LANDMARK_COUNT)
    private var hasHand = false
    private var imageWidth = 1
    private var imageHeight = 1
    private var animationRunning = false
    private var lastAnimationMs = 0L

    private val clearRunnable = Runnable {
        hasHand = false
        animationRunning = false
        invalidate()
    }

    private val animationStep = object : Runnable {
        override fun run() {
            if (!hasHand || !isAttachedToWindow) {
                animationRunning = false
                return
            }

            val now = SystemClock.uptimeMillis()
            val dtSeconds = if (lastAnimationMs == 0L) {
                1f / 60f
            } else {
                ((now - lastAnimationMs).coerceIn(1L, 34L)) / 1000f
            }
            lastAnimationMs = now

            var largestGap = 0f
            for (index in 0 until LANDMARK_COUNT) {
                val dx = targetX[index] - currentX[index]
                val dy = targetY[index] - currentY[index]
                val gap = hypot(dx, dy)
                largestGap = max(largestGap, gap)

                // Fast response for deliberate movement, gentler response for
                // tiny model jitter. The time-based coefficient behaves the
                // same on 30 Hz and 60 Hz displays.
                val response = if (gap > 0.018f) 30f else 19f
                val alpha = 1f - exp(-response * dtSeconds)
                currentX[index] += dx * alpha
                currentY[index] += dy * alpha
            }
            invalidate()

            if (largestGap > 0.00035f) {
                postOnAnimation(this)
            } else {
                animationRunning = false
                lastAnimationMs = 0L
            }
        }
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 40, 242, 181)
        strokeWidth = dp(10f)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(55, 232, 178)
        strokeWidth = dp(3.2f)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(36, 181, 255)
        style = Paint.Style.FILL
    }
    private val tipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    fun setResults(newResult: HandLandmarkerResult, inputWidth: Int, inputHeight: Int) {
        val landmarks = newResult.landmarks().firstOrNull() ?: return
        removeCallbacks(clearRunnable)
        imageWidth = inputWidth.coerceAtLeast(1)
        imageHeight = inputHeight.coerceAtLeast(1)

        for (index in 0 until LANDMARK_COUNT) {
            val x = landmarks[index].x()
            val y = landmarks[index].y()
            targetX[index] = x
            targetY[index] = y
            if (!hasHand) {
                currentX[index] = x
                currentY[index] = y
            }
        }
        hasHand = true
        startAnimation()
    }

    /** Keep the last skeleton briefly so a single missed ML frame does not flicker. */
    fun clear() {
        removeCallbacks(clearRunnable)
        postDelayed(clearRunnable, LOST_HAND_GRACE_MS)
    }

    private fun startAnimation() {
        if (animationRunning) return
        animationRunning = true
        lastAnimationMs = 0L
        postOnAnimation(animationStep)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!hasHand) return
        val scale = max(width.toFloat() / imageWidth, height.toFloat() / imageHeight)
        val shownWidth = imageWidth * scale
        val shownHeight = imageHeight * scale
        val offsetX = (width - shownWidth) / 2f
        val offsetY = (height - shownHeight) / 2f

        fun x(index: Int) = currentX[index] * imageWidth * scale + offsetX
        fun y(index: Int) = currentY[index] * imageHeight * scale + offsetY

        HandLandmarker.HAND_CONNECTIONS.forEach { connection ->
            val start = connection.start()
            val end = connection.end()
            canvas.drawLine(x(start), y(start), x(end), y(end), glowPaint)
            canvas.drawLine(x(start), y(start), x(end), y(end), linePaint)
        }

        for (index in 0 until LANDMARK_COUNT) {
            val isTip = index == 4 || index == 8 || index == 12 || index == 16 || index == 20
            canvas.drawCircle(x(index), y(index), dp(if (isTip) 6f else 4.2f), if (isTip) tipPaint else pointPaint)
        }
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(clearRunnable)
        removeCallbacks(animationStep)
        animationRunning = false
        super.onDetachedFromWindow()
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density

    companion object {
        private const val LANDMARK_COUNT = 21
        private const val LOST_HAND_GRACE_MS = 240L
    }
}
