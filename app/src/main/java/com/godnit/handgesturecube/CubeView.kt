package com.godnit.handgesturecube

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import kotlin.math.min

class CubeView(context: Context) : View(context) {
    private var cubeX = 0.5f
    private var cubeY = 0.5f
    private var grabbed = false

    private val front = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(35, 174, 255) }
    private val top = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(74, 235, 193) }
    private val side = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(18, 111, 210) }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = dp(2.2f)
        style = Paint.Style.STROKE
    }

    /**
     * Follow the newest detected pinch position immediately. Previous versions
     * eased toward a target position, which added visible delay and made the
     * cube suddenly catch up after the user's hand had already stopped.
     */
    fun setGrab(grab: Boolean, x: Float, y: Float) {
        grabbed = grab
        if (grab) {
            cubeX = x.coerceIn(0.10f, 0.86f)
            cubeY = y.coerceIn(0.12f, 0.90f)
            postInvalidateOnAnimation()
        }
    }

    fun release() {
        grabbed = false
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val base = min(width, height) * 0.19f
        val depth = base * 0.34f
        val cx = width * cubeX
        val cy = height * cubeY
        val left = cx - base / 2f
        val right = cx + base / 2f
        val upper = cy - base / 2f
        val lower = cy + base / 2f

        val topPath = Path().apply {
            moveTo(left, upper); lineTo(left + depth, upper - depth)
            lineTo(right + depth, upper - depth); lineTo(right, upper); close()
        }
        val sidePath = Path().apply {
            moveTo(right, upper); lineTo(right + depth, upper - depth)
            lineTo(right + depth, lower - depth); lineTo(right, lower); close()
        }
        val frontPath = Path().apply {
            moveTo(left, upper); lineTo(right, upper); lineTo(right, lower); lineTo(left, lower); close()
        }
        canvas.drawPath(topPath, top)
        canvas.drawPath(sidePath, side)
        canvas.drawPath(frontPath, front)
        canvas.drawPath(topPath, edge)
        canvas.drawPath(sidePath, edge)
        canvas.drawPath(frontPath, edge)
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density
}
