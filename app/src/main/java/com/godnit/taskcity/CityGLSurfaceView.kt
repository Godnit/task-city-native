package com.godnit.taskcity

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import kotlin.math.hypot

class CityGLSurfaceView(context: Context) : GLSurfaceView(context) {
    val cityRenderer = CityRenderer()

    private var lastX = 0f
    private var lastY = 0f
    private var lastPinchDistance = 0f

    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(cityRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun setHouseCount(count: Int, animation: CityRenderer.HouseAnimation = CityRenderer.HouseAnimation.NONE) {
        cityRenderer.setHouseCount(count, animation)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                lastPinchDistance = 0f
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    lastPinchDistance = distance(event)
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    val d = distance(event)
                    if (lastPinchDistance > 8f && d > 8f) {
                        cityRenderer.zoomBy(lastPinchDistance / d)
                    }
                    lastPinchDistance = d
                } else {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    cityRenderer.panBy(-dx, dy)
                    lastX = event.x
                    lastY = event.y
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                lastPinchDistance = 0f
                return true
            }
        }
        return true
    }

    private fun distance(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        return hypot(
            (event.getX(0) - event.getX(1)).toDouble(),
            (event.getY(0) - event.getY(1)).toDouble()
        ).toFloat()
    }
}
