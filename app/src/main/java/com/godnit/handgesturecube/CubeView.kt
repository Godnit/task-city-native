package com.godnit.handgesturecube

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.view.View
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min

class CubeView(context: Context) : View(context) {
    private var cubeX = 0.5f
    private var cubeY = 0.5f
    private var targetX = 0.5f
    private var targetY = 0.5f
    private var grabbed = false
    private var animationRunning = false
    private var lastAnimationMs = 0L

    private val animationStep = object : Runnable {
        override fun run() {
            if (!isAttachedToWindow) {
                animationRunning = false
                return
            }
            val now = SystemClock.uptimeMillis()
            val dtSeconds = if (lastAnimationMs == 0L) 1f / 60f else
                ((now - lastAnimationMs).coerceIn(1L, 34L)) / 1000f
            lastAnimationMs = now

            val alpha = 1f - exp(-CUBE_RESPONSE * dtSeconds)
            cubeX += (targetX - cubeX) * alpha
            cubeY += (targetY - cubeY) * alpha
            invalidate()

            val moving = abs(targetX - cubeX) > 0.0003f || abs(targetY - cubeY) > 0.0003f
            if (moving) {
                postOnAnimation(this)
            } else {
                animationRunning = false
                lastAnimationMs = 0L
            }
        }
    }

    private val front = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(35, 174, 255) }
    private val top = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(74, 235, 193) }
    private val side = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(18, 111, 210) }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = dp(2.2f)
        style = Paint.Style.STROKE
    }

    /**
     * The cube receives hand coordinates only while the thumb and index finger
     * are pinched. Releasing them freezes the target at its current position.
     */
    fun setGrab(grab: Boolean, x: Float, y: Float) {
        grabbed = grab
        if (grab) {
            targetX = x.coerceIn(0.10f, 0.86f)
            targetY = y.coerceIn(0.12f, 0.90f)
        } else {
            // Freeze immediately where it was when the fingers opened.
            targetX = cubeX
            targetY = cubeY
        }
        startAnimation()
    }

    fun release() = setGrab(false, cubeX, cubeY)

    private fun startAnimation() {
        if (animationRunning) return
        animationRunning = true
        lastAnimationMs = 0L
        postOnAnimation(animationStep)
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
        canvas.drawPath(topPath, top); canvas.drawPath(sidePath, side); canvas.drawPath(frontPath, front)
        canvas.drawPath(topPath, edge); canvas.drawPath(sidePath, edge); canvas.drawPath(frontPath, edge)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(animationStep)
        animationRunning = false
        super.onDetachedFromWindow()
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density

    companion object {
        private const val CUBE_RESPONSE = 22f
    }
}
