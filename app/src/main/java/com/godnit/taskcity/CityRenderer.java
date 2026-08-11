package com.godnit.taskcity;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public final class CityRenderer implements GLSurfaceView.Renderer {
    public static final int PLOT_COUNT = 24;

    private static final float[][] PLOTS = {
            {-12f,-12f},{-4f,-12f},{4f,-12f},{12f,-12f},
            {-12f,-4f},{-4f,-4f},{4f,-4f},{12f,-4f},
            {-12f,4f},{-4f,4f},{4f,4f},{12f,4f},
            {-12f,12f},{-4f,12f},{4f,12f},{12f,12f},
            {-18f,-10f},{18f,-10f},{-18f,8f},{18f,8f},
            {-12f,19f},{-4f,19f},{4f,19f},{12f,19f}
    };

    private static final float[][] TREE_CANDIDATES = {
            {-24f,-20f},{-18f,-23f},{-10f,-24f},{-1f,-24f},{8f,-24f},{17f,-23f},{24f,-19f},
            {-25f,-12f},{-25f,-3f},{-25f,7f},{-24f,17f},{-20f,24f},{-11f,25f},{-2f,25f},
            {8f,25f},{18f,24f},{24f,19f},{25f,10f},{25f,1f},{25f,-9f},
            {-21f,-16f},{-21f,20f},{21f,22f},{22f,-15f}
    };

    private static final float[] CUBE_VERTICES = {
            -0.5f,0,0.5f,  0.5f,0,0.5f,  0.5f,1,0.5f,
            -0.5f,0,0.5f,  0.5f,1,0.5f, -0.5f,1,0.5f,
             0.5f,0,-0.5f, -0.5f,0,-0.5f, -0.5f,1,-0.5f,
             0.5f,0,-0.5f, -0.5f,1,-0.5f,  0.5f,1,-0.5f,
             0.5f,0,0.5f,  0.5f,0,-0.5f, 0.5f,1,-0.5f,
             0.5f,0,0.5f,  0.5f,1,-0.5f, 0.5f,1,0.5f,
            -0.5f,0,-0.5f, -0.5f,0,0.5f, -0.5f,1,0.5f,
            -0.5f,0,-0.5f, -0.5f,1,0.5f, -0.5f,1,-0.5f,
            -0.5f,1,0.5f, 0.5f,1,0.5f, 0.5f,1,-0.5f,
            -0.5f,1,0.5f, 0.5f,1,-0.5f, -0.5f,1,-0.5f,
            -0.5f,0,-0.5f, 0.5f,0,-0.5f, 0.5f,0,0.5f,
            -0.5f,0,-0.5f, 0.5f,0,0.5f, -0.5f,0,0.5f
    };

    private static final float[] PYRAMID_VERTICES = {
            -0.5f,0,0.5f, 0.5f,0,0.5f, 0,1,0,
             0.5f,0,0.5f, 0.5f,0,-0.5f, 0,1,0,
             0.5f,0,-0.5f,-0.5f,0,-0.5f, 0,1,0,
            -0.5f,0,-0.5f,-0.5f,0,0.5f, 0,1,0,
            -0.5f,0,-0.5f, 0.5f,0,-0.5f, 0.5f,0,0.5f,
            -0.5f,0,-0.5f, 0.5f,0,0.5f,-0.5f,0,0.5f
    };

    private static final float[] OCTAHEDRON_VERTICES = {
             0,1,0,   1,0,0,   0,0,1,
             0,1,0,   0,0,1,  -1,0,0,
             0,1,0,  -1,0,0,   0,0,-1,
             0,1,0,   0,0,-1,  1,0,0,
             0,-1,0,  0,0,1,   1,0,0,
             0,-1,0, -1,0,0,   0,0,1,
             0,-1,0,  0,0,-1, -1,0,0,
             0,-1,0,  1,0,0,   0,0,-1
    };

    private final FloatBuffer cubeBuffer = buffer(CUBE_VERTICES);
    private final FloatBuffer pyramidBuffer = buffer(PYRAMID_VERTICES);
    private final FloatBuffer octaBuffer = buffer(OCTAHEDRON_VERTICES);
    private final FloatBuffer discBuffer = buffer(makeDisc(20));
    private final int discVertexCount = 20 * 3;

    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] model = new float[16];
    private final float[] pv = new float[16];
    private final float[] mvp = new float[16];

    private volatile List<CityStore.HouseRecord> houses = new ArrayList<>();
    private volatile float camX = 0f;
    private volatile float camZ = 2.2f;
    private volatile float zoom = 22.2f;
    private int width = 1;
    private int height = 1;
    private int program;
    private int posHandle;
    private int mvpHandle;
    private int colorHandle;
    private volatile int buildPlot = -1;
    private volatile long buildStart = 0L;
    private volatile int demolishPlot = -1;
    private volatile long demolishStart = 0L;

    public void setHouses(List<CityStore.HouseRecord> source) {
        List<CityStore.HouseRecord> copy = new ArrayList<>();
        for (CityStore.HouseRecord h : source) {
            CityStore.HouseRecord c = new CityStore.HouseRecord();
            c.id = h.id; c.plot = h.plot; c.variant = h.variant; c.taskId = h.taskId; c.builtAt = h.builtAt;
            copy.add(c);
        }
        houses = copy;
        if (demolishPlot >= 0) {
            boolean stillThere = false;
            for (CityStore.HouseRecord h : copy) if (h.plot == demolishPlot) stillThere = true;
            if (!stillThere) demolishPlot = -1;
        }
    }

    public void animateBuild(int plot) {
        buildPlot = plot;
        buildStart = System.currentTimeMillis();
    }

    public void animateDemolish(int plot) {
        demolishPlot = plot;
        demolishStart = System.currentTimeMillis();
    }

    public void panBy(float dx, float dy) {
        float s = zoom * 0.0030f;
        camX += (-dx - dy * 0.70f) * s;
        camZ += ( dx - dy * 0.70f) * s;
        camX = clamp(camX, -13f, 13f);
        camZ = clamp(camZ, -10f, 14f);
    }

    public void zoomBy(float delta) {
        zoom = clamp(zoom + delta, 12.0f, 30f);
    }

    public void resetCamera() {
        camX = 0f;
        camZ = 2.2f;
        zoom = 22.2f;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.31f, 0.76f, 0.94f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        program = createProgram(
                "uniform mat4 uMvp; attribute vec3 aPos; void main(){ gl_Position=uMvp*vec4(aPos,1.0); }",
                "precision mediump float; uniform vec4 uColor; void main(){ gl_FragColor=uColor; }"
        );
        posHandle = GLES20.glGetAttribLocation(program, "aPos");
        mvpHandle = GLES20.glGetUniformLocation(program, "uMvp");
        colorHandle = GLES20.glGetUniformLocation(program, "uColor");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        width = Math.max(1, w);
        height = Math.max(1, h);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);

        float aspect = width / (float)height;
        float horizontal = zoom * Math.max(0.78f, aspect);
        Matrix.orthoM(projection, 0, -horizontal, horizontal, -zoom, zoom, 1f, 135f);
        Matrix.setLookAtM(view, 0,
                camX + 29f, 31f, camZ + 29f,
                camX, 0.9f, camZ,
                0f, 1f, 0f);
        Matrix.multiplyMM(pv, 0, projection, 0, view, 0);

        drawWorld();
    }

    private void drawWorld() {
        drawBackdrop();

        drawBox(0,-0.20f,2f, 54f,0.38f,58f, rgb("65B956"));
        drawBox(0,-0.035f,2f, 51.5f,0.09f,55.5f, rgb("83D46B"));

        float[] roadCenters = {-8f, 0f, 8f};
        for (float x : roadCenters) {
            drawBox(x,0.02f,1.5f, 3.15f,0.075f,47f, rgb("4F5963"));
            drawBox(x-1.93f,0.04f,1.5f,0.63f,0.085f,47f,rgb("D7D2C5"));
            drawBox(x+1.93f,0.04f,1.5f,0.63f,0.085f,47f,rgb("D7D2C5"));
            for (float z=-18f; z<=21f; z+=6.5f) drawBox(x,0.085f,z,0.18f,0.025f,2.2f,rgb("F4E9C6"));
        }
        for (float z : roadCenters) {
            drawBox(0,0.02f,z, 47f,0.075f,3.15f, rgb("4F5963"));
            drawBox(0,0.04f,z-1.93f,47f,0.085f,0.63f,rgb("D7D2C5"));
            drawBox(0,0.04f,z+1.93f,47f,0.085f,0.63f,rgb("D7D2C5"));
            for (float x=-20f; x<=20f; x+=6.5f) drawBox(x,0.085f,z,2.2f,0.025f,0.18f,rgb("F4E9C6"));
        }
        drawBox(0,0.02f,16f, 38f,0.075f,2.9f,rgb("4F5963"));
        drawBox(0,0.04f,14.18f,38f,0.085f,0.58f,rgb("D7D2C5"));
        drawBox(0,0.04f,17.82f,38f,0.085f,0.58f,rgb("D7D2C5"));

        boolean[] occupied = new boolean[PLOT_COUNT];
        for (CityStore.HouseRecord h : houses) if (h.plot >= 0 && h.plot < PLOT_COUNT) occupied[h.plot] = true;
        for (int i = 0; i < PLOT_COUNT; i++) {
            float x = PLOTS[i][0], z = PLOTS[i][1];
            if (!occupied[i]) {
                drawBox(x,0.058f,z,5.7f,0.045f,5.45f,rgb("8FDA74"));
            }
        }

        for (int i=0;i<TREE_CANDIDATES.length;i++) {
            float x=TREE_CANDIDATES[i][0], z=TREE_CANDIDATES[i][1];
            if (canPlantTree(x,z)) drawTreeShadow(x,z,0.90f + (i%4)*0.07f);
        }
        long now = System.currentTimeMillis();
        for (CityStore.HouseRecord house : houses) {
            if (house.plot < 0 || house.plot >= PLOT_COUNT) continue;
            float s = animationScale(house, now);
            if (house.plot != demolishPlot) drawHouseShadow(PLOTS[house.plot][0],PLOTS[house.plot][1],s);
        }

        for (int i=0;i<TREE_CANDIDATES.length;i++) {
            float x=TREE_CANDIDATES[i][0], z=TREE_CANDIDATES[i][1];
            if (canPlantTree(x,z)) drawTree(x,z,0.90f + (i%4)*0.07f, i);
        }

        for (CityStore.HouseRecord house : houses) {
            if (house.plot < 0 || house.plot >= PLOT_COUNT) continue;
            float scale = animationScale(house, now);
            boolean demolishing = house.plot == demolishPlot;
            drawHouse(PLOTS[house.plot][0], PLOTS[house.plot][1], house.variant, scale, demolishing);
        }
    }

    private float animationScale(CityStore.HouseRecord house, long now) {
        float scale = 1f;
        if (house.plot == buildPlot) {
            float t = clamp((now - buildStart) / 760f, 0f, 1f);
            scale = 0.08f + 0.92f * easeOutBack(t);
            if (t >= 1f) buildPlot = -1;
        }
        if (house.plot == demolishPlot) {
            float t = clamp((now - demolishStart) / 620f, 0f, 1f);
            scale = Math.max(0.035f, 1f - t);
        }
        return scale;
    }

    private void drawBackdrop() {
        drawOcta(-31f,2.8f,-34f,17f,5.2f,13f,rgb("75B66C"));
        drawOcta(-12f,3.2f,-38f,20f,6.2f,15f,rgb("88C47A"));
        drawOcta(12f,2.4f,-40f,18f,5.0f,14f,rgb("78B86D"));

        drawOcta(-28f,17.8f,-35f,5.2f,5.2f,5.2f,color("FFE89A",0.20f));
        drawOcta(-28f,17.8f,-35f,3.55f,3.55f,3.55f,rgb("FFD45F"));
        drawOcta(-28.7f,18.6f,-35.7f,1.65f,1.65f,1.65f,rgb("FFF2AD"));

        drawCloud(-16f,14.8f,-34f,1.0f);
        drawCloud(5f,16.2f,-39f,0.9f);
    }

    private void drawCloud(float x,float y,float z,float s) {
        float[] white = color("FFFFFF",0.88f);
        float[] warm = color("F7FBFF",0.84f);
        drawOcta(x,y,z,2.7f*s,1.15f*s,1.65f*s,white);
        drawOcta(x-2.2f*s,y-0.15f*s,z+0.15f*s,2.0f*s,0.9f*s,1.35f*s,warm);
        drawOcta(x+2.1f*s,y-0.1f*s,z-0.15f*s,2.15f*s,1.0f*s,1.4f*s,white);
    }

    private boolean canPlantTree(float x,float z) {
        for (float[] p : PLOTS) {
            float dx=x-p[0], dz=z-p[1];
            if (dx*dx + dz*dz < 31.0f) return false;
        }
        return true;
    }

    private void drawHouseShadow(float x,float z,float s) {
        float a = 0.19f * clamp(s,0f,1f);
        drawDisc(x+1.25f,0.104f,z+1.00f,4.25f*s,3.45f*s,color("294438",a));
        drawDisc(x+2.00f,0.105f,z+1.65f,2.7f*s,2.05f*s,color("294438",a*0.55f));
    }

    private void drawTreeShadow(float x,float z,float s) {
        drawDisc(x+0.95f*s,0.102f,z+0.75f*s,1.75f*s,1.30f*s,color("294438",0.17f));
        drawDisc(x+1.45f*s,0.103f,z+1.15f*s,1.15f*s,0.85f*s,color("294438",0.09f));
    }

    private void drawHouse(float x, float z, int variant, float s, boolean demolishing) {
        int[][] walls = {
                {248,224,181},{243,231,207},{248,211,139},{225,232,210},{232,220,190},{241,225,202}
        };
        int[][] roofs = {
                {226,84,47},{35,139,179},{52,145,119},{206,74,49},{36,128,169},{229,101,48}
        };
        float yOffset = demolishing ? -0.72f * (1f-s) : 0f;
        float bodyW = 4.05f*s, bodyD = 3.55f*s, bodyH = (variant%3==1?3.45f:3.10f)*s;

        if (!demolishing) {
            drawBox(x,0.064f,z,6.15f,0.06f,5.85f,rgb("72CA5D"));
            drawBox(x+0.12f,0.086f,z+0.45f,5.20f,0.035f,4.25f,rgb("BDE69C"));
            drawBox(x,0.105f,z+2.42f,1.1f,0.035f,1.8f,rgb("E5D9B9"));
            drawFence(x,z,s);
        }

        float[] wall = new float[]{walls[variant%walls.length][0]/255f,walls[variant%walls.length][1]/255f,walls[variant%walls.length][2]/255f,1f};
        float[] roof = new float[]{roofs[variant%roofs.length][0]/255f,roofs[variant%roofs.length][1]/255f,roofs[variant%roofs.length][2]/255f,1f};
        float[] trim = rgb("F8F3E7");

        drawBox(x,0.11f+yOffset,z,bodyW,bodyH,bodyD,wall);
        drawPyramid(x,0.11f+yOffset+bodyH,z, bodyW+0.88f*s,2.00f*s,bodyD+0.88f*s,roof);

        drawBox(x,0.12f+yOffset,z+bodyD*0.53f,2.45f*s,0.25f*s,1.25f*s,rgb("D7BC90"));
        drawBox(x,2.02f*s+yOffset,z+bodyD*0.64f,2.75f*s,0.22f*s,1.25f*s,roof);
        drawBox(x-1.00f*s,0.33f+yOffset,z+bodyD*0.65f,0.18f*s,1.78f*s,0.18f*s,trim);
        drawBox(x+1.00f*s,0.33f+yOffset,z+bodyD*0.65f,0.18f*s,1.78f*s,0.18f*s,trim);

        drawBox(x,0.23f+yOffset,z+bodyD*0.515f+0.03f,0.82f*s,1.78f*s,0.15f*s,rgb("9A5936"));
        drawBox(x-1.28f*s,1.03f*s+yOffset,z+bodyD*0.515f+0.04f,0.88f*s,0.93f*s,0.14f*s,trim);
        drawBox(x-1.28f*s,1.12f*s+yOffset,z+bodyD*0.515f+0.055f,0.68f*s,0.70f*s,0.08f*s,rgb("AEE4F3"));
        drawBox(x+1.28f*s,1.03f*s+yOffset,z+bodyD*0.515f+0.04f,0.88f*s,0.93f*s,0.14f*s,trim);
        drawBox(x+1.28f*s,1.12f*s+yOffset,z+bodyD*0.515f+0.055f,0.68f*s,0.70f*s,0.08f*s,rgb("AEE4F3"));
        drawBox(x+bodyW*0.515f+0.04f,1.08f*s+yOffset,z-0.62f*s,0.14f*s,0.90f*s,0.85f*s,trim);
        drawBox(x+bodyW*0.515f+0.055f,1.15f*s+yOffset,z-0.62f*s,0.08f*s,0.68f*s,0.64f*s,rgb("AEE4F3"));

        if (s > 0.40f) {
            float dormY = 0.11f + bodyH + 0.43f*s + yOffset;
            drawBox(x-0.72f*s,dormY,z+0.38f*s,1.18f*s,0.82f*s,1.00f*s,wall);
            drawPyramid(x-0.72f*s,dormY+0.78f*s,z+0.38f*s,1.45f*s,0.78f*s,1.28f*s,roof);
            drawBox(x-0.72f*s,dormY+0.17f*s,z+0.90f*s,0.48f*s,0.48f*s,0.08f*s,rgb("B7E8F4"));
            drawBox(x+1.12f*s,bodyH*0.86f+yOffset,z-0.82f*s,0.52f*s,1.16f*s,0.52f*s,rgb("AF654D"));
            drawBox(x+1.12f*s,bodyH*0.86f+1.13f*s+yOffset,z-0.82f*s,0.64f*s,0.16f*s,0.64f*s,rgb("844B3E"));
        }

        if (!demolishing && s > 0.80f) {
            drawFlower(x-2.18f,z+1.90f,rgb("F05C5C"));
            drawFlower(x-1.72f,z+2.07f,rgb("FFD45A"));
            drawFlower(x+2.08f,z+1.92f,rgb("F58DB9"));
        }
    }

    private void drawFlower(float x,float z,float[] c) {
        drawBox(x,0.11f,z,0.08f,0.20f,0.08f,rgb("3F9C4D"));
        drawOcta(x,0.33f,z,0.16f,0.14f,0.16f,c);
    }

    private void drawFence(float x,float z,float s) {
        float c = Math.max(0.75f,s);
        float[] white = rgb("F7F4E9");
        float y = 0.11f;
        drawBox(x,y,z+2.78f,5.9f*c,0.25f,0.13f,white);
        drawBox(x,y,z-2.78f,5.9f*c,0.25f,0.13f,white);
        drawBox(x+2.95f,y,z,0.13f,0.25f,5.55f*c,white);
        drawBox(x-2.95f,y,z,0.13f,0.25f,5.55f*c,white);
        for (int i=-2;i<=2;i++) {
            drawBox(x+i*1.15f,y,z+2.78f,0.12f,0.62f,0.14f,white);
            drawBox(x+i*1.15f,y,z-2.78f,0.12f,0.62f,0.14f,white);
        }
    }

    private void drawTree(float x,float z,float s,int seed) {
        float[] trunk = seed%2==0 ? rgb("855A38") : rgb("775037");
        float[] dark = seed%3==0 ? rgb("3D9547") : rgb("479E49");
        float[] mid  = seed%3==1 ? rgb("61BB55") : rgb("58B352");
        float[] light= seed%3==2 ? rgb("76C960") : rgb("69C25C");

        drawBox(x,0.07f,z,0.42f*s,1.55f*s,0.42f*s,trunk);
        drawOcta(x,2.05f*s,z,1.18f*s,1.38f*s,1.18f*s,dark);
        drawOcta(x-0.63f*s,2.22f*s,z+0.18f*s,0.90f*s,1.02f*s,0.90f*s,mid);
        drawOcta(x+0.68f*s,2.18f*s,z-0.28f*s,0.95f*s,1.08f*s,0.95f*s,light);
        drawOcta(x+0.05f*s,2.88f*s,z-0.05f*s,0.85f*s,0.95f*s,0.85f*s,light);
    }

    private void drawBox(float x,float y,float z,float sx,float sy,float sz,float[] base) {
        Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,x,y,z);
        Matrix.scaleM(model,0,sx,sy,sz);
        float[] shades = {0.84f,1.00f,0.78f,0.95f,1.13f,0.60f};
        for (int face=0; face<6; face++) {
            float k = shades[face];
            float[] c = {clamp(base[0]*k,0,1),clamp(base[1]*k,0,1),clamp(base[2]*k,0,1),base.length>3?base[3]:1f};
            drawRange(cubeBuffer, face*6, 6, model, c);
        }
    }

    private void drawPyramid(float x,float y,float z,float sx,float sy,float sz,float[] base) {
        Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,x,y,z);
        Matrix.scaleM(model,0,sx,sy,sz);
        float[] shades = {0.93f,0.82f,0.76f,1.03f};
        for (int face=0; face<4; face++) {
            float k = shades[face];
            float[] c = {clamp(base[0]*k,0,1),clamp(base[1]*k,0,1),clamp(base[2]*k,0,1),base.length>3?base[3]:1f};
            drawRange(pyramidBuffer,face*3,3,model,c);
        }
        drawRange(pyramidBuffer,12,6,model,base);
    }

    private void drawOcta(float x,float y,float z,float sx,float sy,float sz,float[] c) {
        Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,x,y,z);
        Matrix.scaleM(model,0,sx,sy,sz);
        drawRange(octaBuffer,0,24,model,c);
    }

    private void drawDisc(float x,float y,float z,float sx,float sz,float[] c) {
        Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,x,y,z);
        Matrix.scaleM(model,0,sx,1f,sz);
        drawRange(discBuffer,0,discVertexCount,model,c);
    }

    private void drawRange(FloatBuffer buffer,int first,int count,float[] modelMatrix,float[] color) {
        Matrix.multiplyMM(mvp,0,pv,0,modelMatrix,0);
        GLES20.glUniformMatrix4fv(mvpHandle,1,false,mvp,0);
        GLES20.glUniform4fv(colorHandle,1,color,0);
        buffer.position(first*3);
        GLES20.glEnableVertexAttribArray(posHandle);
        GLES20.glVertexAttribPointer(posHandle,3,GLES20.GL_FLOAT,false,3*4,buffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);
        GLES20.glDisableVertexAttribArray(posHandle);
    }

    private static float[] makeDisc(int segments) {
        float[] data = new float[segments * 9];
        int p = 0;
        for (int i=0;i<segments;i++) {
            double a = Math.PI*2.0*i/segments;
            double b = Math.PI*2.0*(i+1)/segments;
            data[p++]=0f; data[p++]=0f; data[p++]=0f;
            data[p++]=(float)Math.cos(a); data[p++]=0f; data[p++]=(float)Math.sin(a);
            data[p++]=(float)Math.cos(b); data[p++]=0f; data[p++]=(float)Math.sin(b);
        }
        return data;
    }

    private static FloatBuffer buffer(float[] data) {
        FloatBuffer b = ByteBuffer.allocateDirect(data.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        b.put(data).position(0);
        return b;
    }

    private static int createProgram(String vertex,String fragment) {
        int vs = compile(GLES20.GL_VERTEX_SHADER,vertex);
        int fs = compile(GLES20.GL_FRAGMENT_SHADER,fragment);
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p,vs); GLES20.glAttachShader(p,fs); GLES20.glLinkProgram(p);
        return p;
    }

    private static int compile(int type,String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s,src); GLES20.glCompileShader(s);
        return s;
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1+1f;
        float x = t-1f;
        return 1f + c3*x*x*x + c1*x*x;
    }

    private static float clamp(float v,float min,float max) { return Math.max(min,Math.min(max,v)); }

    private static float[] rgb(String hex) { return color(hex,1f); }

    private static float[] color(String hex,float alpha) {
        int c = (int)Long.parseLong(hex,16);
        return new float[]{((c>>16)&255)/255f,((c>>8)&255)/255f,(c&255)/255f,alpha};
    }
}
