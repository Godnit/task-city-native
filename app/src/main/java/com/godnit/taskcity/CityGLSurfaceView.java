package com.godnit.taskcity;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import java.util.List;

public final class CityGLSurfaceView extends GLSurfaceView {
    private final CityRenderer renderer;
    private float lastX;
    private float lastY;
    private float lastPinch;
    private boolean twoFinger;

    public CityGLSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        renderer = new CityRenderer();
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
        setPreserveEGLContextOnPause(true);
    }

    public void setHouses(List<CityStore.HouseRecord> houses) {
        renderer.setHouses(houses);
    }

    public void animateBuild(int plot) {
        renderer.animateBuild(plot);
    }

    public void animateDemolish(int plot) {
        renderer.animateDemolish(plot);
    }

    public void resetCamera() {
        renderer.resetCamera();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            lastX = event.getX();
            lastY = event.getY();
            twoFinger = false;
            return true;
        }
        if (action == MotionEvent.ACTION_POINTER_DOWN && event.getPointerCount() >= 2) {
            twoFinger = true;
            lastPinch = distance(event);
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            if (event.getPointerCount() >= 2) {
                float d = distance(event);
                if (lastPinch > 0f) renderer.zoomBy((lastPinch - d) * 0.012f);
                lastPinch = d;
                twoFinger = true;
            } else if (!twoFinger) {
                float x = event.getX();
                float y = event.getY();
                renderer.panBy(x - lastX, y - lastY);
                lastX = x;
                lastY = y;
            }
            return true;
        }
        if (action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (event.getPointerCount() <= 2) {
                lastPinch = 0f;
                twoFinger = false;
                if (event.getPointerCount() > 0) {
                    int idx = action == MotionEvent.ACTION_POINTER_UP ? (event.getActionIndex() == 0 ? 1 : 0) : 0;
                    if (idx < event.getPointerCount()) {
                        lastX = event.getX(idx);
                        lastY = event.getY(idx);
                    }
                }
            }
            return true;
        }
        return true;
    }

    private float distance(MotionEvent event) {
        if (event.getPointerCount() < 2) return 0f;
        float dx = event.getX(0) - event.getX(1);
        float dy = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(dx * dx + dy * dy);
    }
}
