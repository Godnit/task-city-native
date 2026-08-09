package com.godnit.handgesturecube

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.max

class OverlayView(context: Context) : View(context) {
    private var result: HandLandmarkerResult? = null
    private var imageWidth = 1
    private var imageHeight = 1

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
        result = newResult
        imageWidth = inputWidth.coerceAtLeast(1)
        imageHeight = inputHeight.coerceAtLeast(1)
        invalidate()
    }

    fun clear() {
        result = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val landmarks = result?.landmarks()?.firstOrNull() ?: return
        val scale = max(width.toFloat() / imageWidth, height.toFloat() / imageHeight)
        val shownWidth = imageWidth * scale
        val shownHeight = imageHeight * scale
        val offsetX = (width - shownWidth) / 2f
        val offsetY = (height - shownHeight) / 2f

        fun x(point: NormalizedLandmark) = point.x() * imageWidth * scale + offsetX
        fun y(point: NormalizedLandmark) = point.y() * imageHeight * scale + offsetY

        HandLandmarker.HAND_CONNECTIONS.forEach { connection ->
            val start = landmarks[connection.start()]
            val end = landmarks[connection.end()]
            canvas.drawLine(x(start), y(start), x(end), y(end), glowPaint)
            canvas.drawLine(x(start), y(start), x(end), y(end), linePaint)
        }

        landmarks.forEachIndexed { index, point ->
            val isTip = index == 4 || index == 8 || index == 12 || index == 16 || index == 20
            canvas.drawCircle(x(point), y(point), dp(if (isTip) 6f else 4.2f), if (isTip) tipPaint else pointPaint)
        }
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density
}
