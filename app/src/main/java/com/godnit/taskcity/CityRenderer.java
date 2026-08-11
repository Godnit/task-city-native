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

    private final FloatBuffer cubeBuffer = buffer(CUBE_VERTICES);
    private final FloatBuffer pyramidBuffer = buffer(PYRAMID_VERTICES);
    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] model = new float[16];
    private final float[] pv = new float[16];
    private final float[] mvp = new float[16];

    private volatile List<CityStore.HouseRecord> houses = new ArrayList<>();
    private volatile float camX = 0f;
    private volatile float camZ = 1.5f;
    private volatile float zoom = 19f;
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
        float s = zoom * 0.0032f;
        camX += (-dx - dy * 0.70f) * s;
        camZ += ( dx - dy * 0.70f) * s;
        camX = clamp(camX, -12f, 12f);
        camZ = clamp(camZ, -10f, 12f);
    }

    public void zoomBy(float delta) {
        zoom = clamp(zoom + delta, 10.5f, 27f);
    }

    public void resetCamera() {
        camX = 0f;
        camZ = 1.5f;
        zoom = 19f;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.33f, 0.77f, 0.94f, 1f);
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
        Matrix.orthoM(projection, 0, -horizontal, horizontal, -zoom, zoom, 1f, 120f);
        Matrix.setLookAtM(view, 0,
                camX + 27f, 29f, camZ + 27f,
                camX, 0.8f, camZ,
                0f, 1f, 0f);
        Matrix.multiplyMM(pv, 0, projection, 0, view, 0);

        drawWorld();
    }

    private void drawWorld() {
        drawBox(0,-0.18f,2f, 48f,0.35f,50f, rgb("77C95F"));
        drawBox(0,-0.03f,2f, 45f,0.08f,47f, rgb("8BDB72"));

        float[] roadCenters = {-8f, 0f, 8f};
        for (float x : roadCenters) {
            drawBox(x,0.02f,0f, 3.0f,0.07f,43f, rgb("505B67"));
            drawBox(x-1.9f,0.035f,0f,0.65f,0.08f,43f,rgb("D9D4C7"));
            drawBox(x+1.9f,0.035f,0f,0.65f,0.08f,43f,rgb("D9D4C7"));
        }
        for (float z : roadCenters) {
            drawBox(0,0.02f,z, 43f,0.07f,3.0f, rgb("505B67"));
            drawBox(0,0.035f,z-1.9f,43f,0.08f,0.65f,rgb("D9D4C7"));
            drawBox(0,0.035f,z+1.9f,43f,0.08f,0.65f,rgb("D9D4C7"));
        }
        drawBox(0,0.02f,16f, 34f,0.07f,2.7f,rgb("505B67"));
        drawBox(0,0.035f,14.25f,34f,0.08f,0.55f,rgb("D9D4C7"));
        drawBox(0,0.035f,17.75f,34f,0.08f,0.55f,rgb("D9D4C7"));

        boolean[] occupied = new boolean[PLOT_COUNT];
        for (CityStore.HouseRecord h : houses) if (h.plot >= 0 && h.plot < PLOT_COUNT) occupied[h.plot] = true;
        for (int i = 0; i < PLOT_COUNT; i++) {
            float x = PLOTS[i][0], z = PLOTS[i][1];
            if (!occupied[i]) {
                drawBox(x,0.055f,z,5.4f,0.04f,5.2f,rgb("91DC78"));
                drawBox(x,0.09f,z,0.8f,0.035f,0.8f,rgb("C4E8A5"));
            }
        }

        float[][] trees = {
                {-21,-17},{-18,-16},{-21,-2},{-20,16},{-16,21},{-6,22},{7,22},{18,20},{21,12},{21,-1},{20,-17},
                {-14,-8},{-2,-8},{6,-8},{14,-8},{-14,0},{14,0},{-14,8},{-2,8},{14,8}
        };
        for (int i=0;i<trees.length;i++) drawTree(trees[i][0],trees[i][1],0.85f + (i%3)*0.12f);

        long now = System.currentTimeMillis();
        for (CityStore.HouseRecord house : houses) {
            if (house.plot < 0 || house.plot >= PLOT_COUNT) continue;
            float scale = 1f;
            boolean demolishing = house.plot == demolishPlot;
            if (house.plot == buildPlot) {
                float t = clamp((now - buildStart) / 720f, 0f, 1f);
                scale = 0.08f + 0.92f * easeOutBack(t);
                if (t >= 1f) buildPlot = -1;
            }
            if (demolishing) {
                float t = clamp((now - demolishStart) / 620f, 0f, 1f);
                scale = Math.max(0.035f, 1f - t);
            }
            drawHouse(PLOTS[house.plot][0], PLOTS[house.plot][1], house.variant, scale, demolishing);
        }
    }

    private void drawHouse(float x, float z, int variant, float s, boolean demolishing) {
        int[][] walls = {
                {248,224,181},{243,231,207},{248,211,139},{225,232,210},{232,220,190},{241,225,202}
        };
        int[][] roofs = {
                {226,84,47},{35,139,179},{52,145,119},{206,74,49},{36,128,169},{229,101,48}
        };
        float yOffset = demolishing ? -0.7f * (1f-s) : 0f;
        float bodyW = 4.2f*s, bodyD = 3.7f*s, bodyH = 3.25f*s;

        if (!demolishing) {
            drawBox(x,0.065f,z,6.1f,0.06f,5.8f,rgb("73CF5F"));
            drawBox(x+0.45f,0.085f,z+0.38f,4.9f,0.035f,3.9f,rgb("BDE99C"));
            drawFence(x,z,s);
        }
        drawBox(x+0.35f*s,0.10f+yOffset,z+0.42f*s,4.8f*s,0.10f,3.9f*s,new float[]{0.18f,0.25f,0.26f,0.20f});

        float[] wall = new float[]{walls[variant%walls.length][0]/255f,walls[variant%walls.length][1]/255f,walls[variant%walls.length][2]/255f,1f};
        float[] roof = new float[]{roofs[variant%roofs.length][0]/255f,roofs[variant%roofs.length][1]/255f,roofs[variant%roofs.length][2]/255f,1f};
        drawBox(x,0.11f+yOffset,z,bodyW,bodyH,bodyD,wall);
        drawPyramid(x,0.11f+yOffset+bodyH,z, bodyW+0.85f*s,2.05f*s,bodyD+0.85f*s,roof);

        drawBox(x,0.13f+yOffset,z+bodyD*0.52f,2.25f*s,0.28f*s,1.15f*s,rgb("D8C19B"));
        drawBox(x,0.40f+yOffset,z+bodyD*0.53f,1.55f*s,0.18f*s,0.95f*s,rgb("F0E6D1"));
        drawBox(x,0.18f+yOffset,z+bodyD*0.515f+0.02f,0.80f*s,1.75f*s,0.14f*s,rgb("9C5B35"));
        drawBox(x-1.25f*s,1.05f*s+yOffset,z+bodyD*0.515f+0.035f,0.78f*s,0.82f*s,0.13f*s,rgb("BEE8F7"));
        drawBox(x+1.25f*s,1.05f*s+yOffset,z+bodyD*0.515f+0.035f,0.78f*s,0.82f*s,0.13f*s,rgb("BEE8F7"));
        drawBox(x+bodyW*0.515f+0.035f,1.12f*s+yOffset,z-0.62f*s,0.13f*s,0.86f*s,0.82f*s,rgb("BEE8F7"));
        drawBox(x+1.18f*s,bodyH*0.85f+yOffset,z-0.78f*s,0.55f*s,1.05f*s,0.55f*s,rgb("B56E53"));

        if (!demolishing && s > 0.8f) {
            drawBox(x-2.25f,0.16f,z+1.95f,0.25f,0.18f,0.25f,rgb("F05B5B"));
            drawBox(x-1.75f,0.16f,z+2.08f,0.25f,0.18f,0.25f,rgb("FFD45B"));
            drawBox(x+2.1f,0.16f,z+1.92f,0.25f,0.18f,0.25f,rgb("F386B8"));
        }
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

    private void drawTree(float x,float z,float s) {
        drawBox(x,0.06f,z,0.55f*s,1.65f*s,0.55f*s,rgb("8A5A36"));
        float[] g1 = rgb("4EAD4F");
        float[] g2 = rgb("69C75A");
        drawBox(x,1.15f*s,z,2.05f*s,1.85f*s,2.0f*s,g1);
        drawBox(x-0.48f*s,2.05f*s,z+0.15f*s,1.45f*s,1.5f*s,1.45f*s,g2);
        drawBox(x+0.55f*s,1.92f*s,z-0.30f*s,1.35f*s,1.4f*s,1.35f*s,g2);
    }

    private void drawBox(float x,float y,float z,float sx,float sy,float sz,float[] base) {
        Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,x,y,z);
        Matrix.scaleM(model,0,sx,sy,sz);
        float[] shades = {1f,0.76f,0.88f,0.68f,1.08f,0.60f};
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
        for (int face=0; face<4; face++) {
            float k = 1f - face*0.08f;
            float[] c = {clamp(base[0]*k,0,1),clamp(base[1]*k,0,1),clamp(base[2]*k,0,1),1f};
            drawRange(pyramidBuffer,face*3,3,model,c);
        }
        drawRange(pyramidBuffer,12,6,model,base);
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

    private static float[] rgb(String hex) {
        int c = (int)Long.parseLong(hex,16);
        return new float[]{((c>>16)&255)/255f,((c>>8)&255)/255f,(c&255)/255f,1f};
    }
}
