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
        if (event.getPointerCount() == 1 && !scaleDetector.isInProgress()) {
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                float dx = event.getX() - previousX;
                float dy = event.getY() - previousY;
                cityRenderer.pan(dx, dy);
            }
            previousX = event.getX();
            previousY = event.getY();
        }
        return true;
    }
}
