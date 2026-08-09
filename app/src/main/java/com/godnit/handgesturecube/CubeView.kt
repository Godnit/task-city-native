package com.godnit.handgesturecube

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import kotlin.math.min

class CubeView(context: Context) : View(context) {
    private var handX = 0.5f
    private var handY = 0.5f
    private var cubeScale = 1f
    private val front = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(35, 174, 255) }
    private val top = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(74, 235, 193) }
    private val side = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(18, 111, 210) }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = dp(2.2f)
        style = Paint.Style.STROKE
    }

    fun update(x: Float, y: Float, scale: Float) {
        handX += (x.coerceIn(0.08f, 0.92f) - handX) * 0.24f
        handY += (y.coerceIn(0.10f, 0.90f) - handY) * 0.24f
        cubeScale += (scale.coerceIn(0.55f, 1.65f) - cubeScale) * 0.20f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val base = min(width, height) * 0.19f * cubeScale
        val depth = base * 0.34f
        val cx = width * handX
        val cy = height * handY
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
        canvas.drawPath(topPath, top); canvas.drawPath(sidePath, side); canvas.drawPath(frontPath, front)
        canvas.drawPath(topPath, edge); canvas.drawPath(sidePath, edge); canvas.drawPath(frontPath, edge)
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density
}
