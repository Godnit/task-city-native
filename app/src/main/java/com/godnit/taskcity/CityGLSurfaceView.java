package com.godnit.taskcity;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

final class CityGLSurfaceView extends GLSurfaceView {
    private final CityRenderer cityRenderer;
    private final ScaleGestureDetector scaleDetector;
    private float previousX;
    private float previousY;
    private boolean multiTouchGesture;

    CityGLSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);
        cityRenderer = new CityRenderer();
        setRenderer(cityRenderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
        scaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScaleBegin(ScaleGestureDetector detector) {
                        multiTouchGesture = true;
                        cityRenderer.beginZoom();
                        return true;
                    }

                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        cityRenderer.zoom(detector.getScaleFactor());
                        return true;
                    }
                });
    }

    CityRenderer getCityRenderer() {
        return cityRenderer;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            multiTouchGesture = false;
            previousX = event.getX(0);
            previousY = event.getY(0);
            return true;
        }

        if (action == MotionEvent.ACTION_POINTER_DOWN || event.getPointerCount() > 1) {
            multiTouchGesture = true;
        }

        if (event.getPointerCount() == 1
                && action == MotionEvent.ACTION_MOVE
                && !multiTouchGesture
                && !scaleDetector.isInProgress()) {
            float currentX = event.getX(0);
            float currentY = event.getY(0);
            float dx = currentX - previousX;
            float dy = currentY - previousY;
            cityRenderer.pan(dx, dy);
            previousX = currentX;
            previousY = currentY;
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            multiTouchGesture = false;
        }
        return true;
    }
}
