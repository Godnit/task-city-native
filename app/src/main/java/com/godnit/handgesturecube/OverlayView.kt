package com.godnit.handgesturecube

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.max

/**
 * Draws the newest hand result immediately. There is deliberately no visual
 * interpolation here: smoothing looked pleasant but made the skeleton visibly
 * trail behind the real hand on slower phones.
 */
class OverlayView(context: Context) : View(context) {
    private val pointX = FloatArray(LANDMARK_COUNT)
    private val pointY = FloatArray(LANDMARK_COUNT)
    private var hasHand = false
    private var imageWidth = 1
    private var imageHeight = 1

    private val clearRunnable = Runnable {
        hasHand = false
        invalidate()
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

    fun setResults(
        newResult: HandLandmarkerResult,
        inputWidth: Int,
        inputHeight: Int,
        mirrorX: Boolean
    ) {
        val landmarks = newResult.landmarks().firstOrNull() ?: return
        removeCallbacks(clearRunnable)
        imageWidth = inputWidth.coerceAtLeast(1)
        imageHeight = inputHeight.coerceAtLeast(1)

        for (index in 0 until LANDMARK_COUNT) {
            val rawX = landmarks[index].x()
            pointX[index] = if (mirrorX) 1f - rawX else rawX
            pointY[index] = landmarks[index].y()
        }
        hasHand = true
        postInvalidateOnAnimation()
    }

    /** Keep the last skeleton briefly so one missed ML result does not flicker. */
    fun clear() {
        removeCallbacks(clearRunnable)
        postDelayed(clearRunnable, LOST_HAND_GRACE_MS)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!hasHand) return
        val scale = max(width.toFloat() / imageWidth, height.toFloat() / imageHeight)
        val shownWidth = imageWidth * scale
        val shownHeight = imageHeight * scale
        val offsetX = (width - shownWidth) / 2f
        val offsetY = (height - shownHeight) / 2f

        fun x(index: Int) = pointX[index] * imageWidth * scale + offsetX
        fun y(index: Int) = pointY[index] * imageHeight * scale + offsetY

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
        super.onDetachedFromWindow()
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density

    companion object {
        private const val LANDMARK_COUNT = 21
        private const val LOST_HAND_GRACE_MS = 180L
    }
}
