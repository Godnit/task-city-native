package com.godnit.taskcity;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

final class CityRenderer implements GLSurfaceView.Renderer {
    private static final float[] CUBE = {
            -0.5f,-0.5f, 0.5f,  0.5f,-0.5f, 0.5f,  0.5f, 0.5f, 0.5f,
            -0.5f,-0.5f, 0.5f,  0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
             0.5f,-0.5f,-0.5f, -0.5f,-0.5f,-0.5f, -0.5f, 0.5f,-0.5f,
             0.5f,-0.5f,-0.5f, -0.5f, 0.5f,-0.5f,  0.5f, 0.5f,-0.5f,
            -0.5f,-0.5f,-0.5f, -0.5f,-0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
            -0.5f,-0.5f,-0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f,-0.5f,
             0.5f,-0.5f, 0.5f,  0.5f,-0.5f,-0.5f,  0.5f, 0.5f,-0.5f,
             0.5f,-0.5f, 0.5f,  0.5f, 0.5f,-0.5f,  0.5f, 0.5f, 0.5f,
            -0.5f, 0.5f, 0.5f,  0.5f, 0.5f, 0.5f,  0.5f, 0.5f,-0.5f,
            -0.5f, 0.5f, 0.5f,  0.5f, 0.5f,-0.5f, -0.5f, 0.5f,-0.5f,
            -0.5f,-0.5f,-0.5f,  0.5f,-0.5f,-0.5f,  0.5f,-0.5f, 0.5f,
            -0.5f,-0.5f,-0.5f,  0.5f,-0.5f, 0.5f, -0.5f,-0.5f, 0.5f
    };

    private static final float[] OCTAHEDRON = {
            0,1,0,  1,0,0,  0,0,1,   0,1,0,  0,0,1, -1,0,0,
            0,1,0, -1,0,0,  0,0,-1,  0,1,0,  0,0,-1, 1,0,0,
            0,-1,0, 0,0,1, 1,0,0,    0,-1,0,-1,0,0, 0,0,1,
            0,-1,0, 0,0,-1,-1,0,0,   0,-1,0,1,0,0, 0,0,-1
    };

    private static final float[] SKY = {
            -1,-1, 0.55f,0.84f,0.96f,  1,-1, 0.55f,0.84f,0.96f,  1,1, 0.12f,0.61f,0.93f,
            -1,-1, 0.55f,0.84f,0.96f,  1,1, 0.12f,0.61f,0.93f, -1,1, 0.12f,0.61f,0.93f
    };
    private static final float[] NIGHT_SKY = {
            -1,-1, 0.28f,0.47f,0.65f,  1,-1, 0.28f,0.47f,0.65f,  1,1, 0.05f,0.16f,0.34f,
            -1,-1, 0.28f,0.47f,0.65f,  1,1, 0.05f,0.16f,0.34f, -1,1, 0.05f,0.16f,0.34f
    };

    private static final float YAW = 38f;
    private static final float[] DAY_FACE_LIGHT = {.94f,.70f,.77f,1.02f,1.16f,.62f};
    private static final float[] NIGHT_FACE_LIGHT = {.79f,.62f,.68f,.85f,.94f,.56f};
    private final FloatBuffer cubeBuffer = buffer(CUBE);
    private final FloatBuffer octaBuffer = buffer(OCTAHEDRON);
    private final FloatBuffer circleXZ = buffer(makeCircle(false, 20));
    private final FloatBuffer circleXY = buffer(makeCircle(true, 28));
    private final FloatBuffer skyDay = buffer(SKY);
    private final FloatBuffer skyNight = buffer(NIGHT_SKY);
    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] model = new float[16];
    private final float[] temp = new float[16];
    private final float[] mvp = new float[16];
    private final float[] identity = new float[16];
    private final float[] shadedColor = new float[4];

    private int program;
    private int positionHandle;
    private int matrixHandle;
    private int colorHandle;
    private int skyProgram;
    private int skyPositionHandle;
    private int skyColorHandle;
    private volatile int houseCount;
    private volatile int cityType = TaskItem.NORMAL;
    private volatile float desiredX;
    private volatile float desiredZ;
    private volatile float desiredDistance = 22f;
    private float cameraX;
    private float cameraZ;
    private float distance = 22f;
    private volatile long constructionStart;
    private volatile int constructionIndex = -1;
    private volatile long demolitionStart;
    private volatile int demolitionIndex = -1;

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glClearColor(0.15f, 0.64f, 0.93f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        program = createProgram(
                "uniform mat4 uMVP; attribute vec4 aPosition; void main(){gl_Position=uMVP*aPosition;}",
                "precision mediump float; uniform vec4 uColor; void main(){gl_FragColor=uColor;}"
        );
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        matrixHandle = GLES20.glGetUniformLocation(program, "uMVP");
        colorHandle = GLES20.glGetUniformLocation(program, "uColor");
        skyProgram = createProgram(
                "attribute vec2 aPosition; attribute vec3 aColor; varying vec3 vColor; " +
                        "void main(){vColor=aColor;gl_Position=vec4(aPosition,0.999,1.0);}",
                "precision mediump float; varying vec3 vColor; void main(){gl_FragColor=vec4(vColor,1.0);}"
        );
        skyPositionHandle = GLES20.glGetAttribLocation(skyProgram, "aPosition");
        skyColorHandle = GLES20.glGetAttribLocation(skyProgram, "aColor");
        Matrix.setIdentityM(identity, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / Math.max(1, height);
        Matrix.perspectiveM(projection, 0, 39f, ratio, 0.8f, 120f);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        drawSky();

        cameraX += (desiredX - cameraX) * 0.28f;
        cameraZ += (desiredZ - cameraZ) * 0.28f;
        distance += (desiredDistance - distance) * 0.24f;
        float yaw = (float) Math.toRadians(YAW);
        float pitch = (float) Math.toRadians(47f);
        float eyeX = cameraX + (float)(distance * Math.cos(pitch) * Math.sin(yaw));
        float eyeY = (float)(distance * Math.sin(pitch));
        float eyeZ = cameraZ + (float)(distance * Math.cos(pitch) * Math.cos(yaw));
        Matrix.setLookAtM(view, 0, eyeX, eyeY, eyeZ, cameraX, 0f, cameraZ, 0f, 1f, 0f);

        GLES20.glUseProgram(program);
        drawEnvironment();
    }

    void setCity(int type, int count) {
        cityType = type;
        houseCount = Math.max(0, count);
        constructionIndex = -1;
        demolitionIndex = -1;
    }

    void houseBuilt(int count) {
        houseCount = Math.max(0, count);
        constructionIndex = count - 1;
        constructionStart = System.currentTimeMillis();
    }

    void houseDemolished(int oldCount, int newCount) {
        houseCount = Math.max(0, newCount);
        demolitionIndex = oldCount - 1;
        demolitionStart = System.currentTimeMillis();
    }

    void pan(float dx, float dy) {
        float movement = desiredDistance * 0.0017f;
        float yaw = (float)Math.toRadians(YAW);
        float rightX = (float)Math.cos(yaw);
        float rightZ = (float)-Math.sin(yaw);
        float forwardX = (float)Math.sin(yaw);
        float forwardZ = (float)Math.cos(yaw);
        desiredX -= dx * movement * rightX + dy * movement * forwardX;
        desiredZ -= dx * movement * rightZ + dy * movement * forwardZ;
        desiredX = clamp(desiredX, -8.2f, 8.2f);
        desiredZ = clamp(desiredZ, -8.2f, 8.2f);
    }

    void zoom(float scale) {
        desiredDistance = clamp(desiredDistance / scale, 14.5f, 27f);
    }

    void beginZoom() {
        // Freeze any remaining pan interpolation so a pinch only changes distance.
        desiredX = cameraX;
        desiredZ = cameraZ;
    }

    private void drawSky() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glUseProgram(skyProgram);
        FloatBuffer sky = cityType == TaskItem.NORMAL ? skyDay : skyNight;
        sky.position(0);
        GLES20.glVertexAttribPointer(skyPositionHandle, 2, GLES20.GL_FLOAT, false, 20, sky);
        GLES20.glEnableVertexAttribArray(skyPositionHandle);
        sky.position(2);
        GLES20.glVertexAttribPointer(skyColorHandle, 3, GLES20.GL_FLOAT, false, 20, sky);
        GLES20.glEnableVertexAttribArray(skyColorHandle);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        GLES20.glUseProgram(program);
        if (cityType == TaskItem.NORMAL) {
            // A large, unmistakable sun with a soft cartoon glow.
            drawClipCircle(0.60f, 0.57f, 0.205f, rgba(1f,0.90f,0.42f,.10f));
            drawClipCircle(0.60f, 0.57f, 0.158f, rgba(1f,0.91f,0.43f,.20f));
            drawClipCircle(0.60f, 0.57f, 0.112f, rgba(1f,0.91f,0.38f,1f));
            drawClipCircle(0.575f, 0.602f, 0.045f, rgba(1f,0.98f,0.72f,.72f));
            drawCloud(-0.70f, 0.49f, 0.18f);
            drawCloud(0.23f, 0.33f, 0.16f);
        } else {
            drawClipCircle(0.60f, 0.57f, 0.165f, rgba(1f,0.84f,0.45f,.12f));
            drawClipCircle(0.60f, 0.57f, 0.105f, rgba(1f,0.84f,0.48f,1f));
            float[][] stars = {{-.78f,.78f},{-.48f,.61f},{-.12f,.82f},{.18f,.56f},{.83f,.84f},{.74f,.49f}};
            for (float[] star : stars) drawClipCircle(star[0], star[1], 0.009f, rgba(1f,0.94f,0.70f,.9f));
        }
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
    }

    private void drawCloud(float x, float y, float size) {
        float[] white = rgba(1f,1f,1f,.86f);
        drawClipCircle(x-size*.45f,y,size*.42f,white);
        drawClipCircle(x,y+size*.10f,size*.56f,white);
        drawClipCircle(x+size*.48f,y,size*.40f,white);
        drawClipCircle(x+size*.12f,y-size*.13f,size*.55f,white);
    }

    private void drawEnvironment() {
        float[] grass = cityType == TaskItem.NORMAL ? rgba(.39f,.72f,.25f,1f) : rgba(.23f,.47f,.31f,1f);
        float[] farGrass = cityType == TaskItem.NORMAL ? rgba(.27f,.57f,.23f,1f) : rgba(.15f,.33f,.28f,1f);
        drawCube(0,-.50f,0,90f,.9f,90f,farGrass);
        drawCube(0,-.07f,0,50f,.20f,50f,grass);

        drawDistantHills();
        drawGrassDetails();

        float[][] trees = {
                {-20,-18},{-15,-20},{-9,-20},{-2,-21},{6,-20},{13,-20},{20,-17},
                {-20,-10},{20,-9},{-21,-2},{21,1},{-20,8},{20,10},
                {-18,18},{-12,20},{-5,21},{3,20},{10,21},{17,18}
        };
        for (int i=0;i<trees.length;i++) drawTree(trees[i][0], trees[i][1], .82f + (i%4)*.08f);
    }

    private void drawGrassDetails() {
        float[] patch = cityType == TaskItem.NORMAL
                ? rgba(.20f,.55f,.18f,.16f) : rgba(.11f,.34f,.24f,.18f);
        float[][] patches = {
                {-11,-6,2.8f,1.8f},{-4,9,2.1f,1.5f},{7,-10,2.7f,1.6f},
                {11,5,2.2f,1.4f},{-13,13,2.4f,1.5f},{3,14,1.8f,1.2f}
        };
        for (float[] p : patches) drawGroundCircle(p[0], p[1], p[2], p[3], patch);

        float[][] bushes = {{-16,-5,.65f},{15,-13,.72f},{-14,9,.62f},{13,13,.70f}};
        for (float[] b : bushes) drawBush(b[0], b[1], b[2]);

        float[][] flowers = {{-8,5},{5,8},{9,-4},{-3,-11},{12,2}};
        for (int i=0;i<flowers.length;i++) {
            float[] f=flowers[i];
            float[] color=i%2==0 ? rgba(1f,.78f,.24f,1f) : rgba(1f,.47f,.55f,1f);
            drawOcta(f[0],.17f,f[1],.12f,.20f,.12f,color);
            drawOcta(f[0]+.28f,.15f,f[1]-.18f,.10f,.17f,.10f,color);
        }
    }

    private void drawGroundCircle(float x,float z,float sx,float sz,float[] color) {
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        draw(circleXZ,60,x,.045f,z,sx,.01f,sz,color);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
    }

    private void drawBush(float x,float z,float scale) {
        drawShadow(x+.25f,z+.22f,.60f*scale,.48f*scale,.12f);
        float[] leaf=cityType==TaskItem.NORMAL ? rgba(.17f,.55f,.18f,1f) : rgba(.10f,.37f,.24f,1f);
        drawOcta(x,.48f*scale,z,.74f*scale,.55f*scale,.65f*scale,leaf);
        drawOcta(x-.35f*scale,.38f*scale,z+.12f*scale,.48f*scale,.42f*scale,.45f*scale,leaf);
        drawOcta(x+.34f*scale,.39f*scale,z-.10f*scale,.48f*scale,.43f*scale,.45f*scale,leaf);
    }

    private void drawDistantHills() {
        float[] hill = cityType == TaskItem.NORMAL ? rgba(.27f,.60f,.32f,1f) : rgba(.14f,.31f,.31f,1f);
        float[][] points = {{-28,-28,10,4},{-12,-31,12,5},{6,-32,13,4},{24,-28,11,5},
                {-31,-8,10,4},{-31,14,12,5},{-22,29,13,4},{0,32,14,5},{22,29,12,4},{31,12,11,5}};
        for (float[] p:points) drawOcta(p[0],1.2f,p[1],p[2],p[3],p[2],hill);
    }

    private void drawRoad(float offset, boolean vertical) {
        float[] asphalt = cityType == TaskItem.NORMAL ? rgba(.28f,.31f,.34f,1f) : rgba(.22f,.25f,.29f,1f);
        float[] walk = cityType == TaskItem.NORMAL ? rgba(.81f,.78f,.69f,1f) : rgba(.53f,.52f,.48f,1f);
        if (vertical) {
            drawCube(offset-.95f,.055f,0,.28f,.10f,32f,walk);
            drawCube(offset+.95f,.055f,0,.28f,.10f,32f,walk);
            drawCube(offset,.065f,0,1.65f,.11f,32f,asphalt);
        } else {
            drawCube(0,.058f,offset-.95f,32f,.10f,.28f,walk);
            drawCube(0,.058f,offset+.95f,32f,.10f,.28f,walk);
            drawCube(0,.067f,offset,32f,.11f,1.65f,asphalt);
        }
    }

    private void drawBuildingPlots() {
        for (int row=0;row<5;row++) {
            for (int col=0;col<5;col++) {
                float x=-12f+col*6f;
                float z=-12f+row*6f;
                drawLot(x,z,(row+col)%2==0);
            }
        }
    }

    private void drawLot(float x, float z, boolean flowers) {
        float[] lawn = cityType == TaskItem.NORMAL ? rgba(.47f,.77f,.31f,1f) : rgba(.29f,.52f,.34f,1f);
        drawCube(x,.09f,z,3.7f,.12f,3.7f,lawn);
        float[] hedge = cityType == TaskItem.NORMAL ? rgba(.18f,.53f,.22f,1f) : rgba(.12f,.37f,.25f,1f);
        drawCube(x-1.72f,.32f,z,0.18f,.48f,3.5f,hedge);
        drawCube(x+1.72f,.32f,z,0.18f,.48f,3.5f,hedge);
        drawCube(x,.32f,z-1.72f,3.5f,.48f,.18f,hedge);
        if (flowers) {
            drawOcta(x-1.15f,.34f,z+1.25f,.16f,.25f,.16f,rgba(1f,.54f,.34f,1f));
            drawOcta(x-.72f,.34f,z+1.28f,.14f,.22f,.14f,rgba(1f,.82f,.31f,1f));
        }
    }

    private void drawTree(float x, float z, float scale) {
        drawShadow(x+.38f,z+.33f,.75f*scale,1.15f*scale,.16f);
        drawCube(x,.68f*scale,z,.28f*scale,1.36f*scale,.28f*scale,rgba(.34f,.20f,.10f,1f));
        float[] leaf = cityType == TaskItem.NORMAL ? rgba(.24f,.62f,.20f,1f) : rgba(.14f,.42f,.27f,1f);
        drawOcta(x,1.65f*scale,z,.92f*scale,.92f*scale,.92f*scale,leaf);
        drawOcta(x-.34f*scale,1.48f*scale,z+.12f*scale,.64f*scale,.67f*scale,.64f*scale,leaf);
        drawOcta(x+.33f*scale,1.49f*scale,z-.10f*scale,.62f*scale,.66f*scale,.62f*scale,leaf);
    }

    private void drawTaskFoundations() {
        long now=System.currentTimeMillis();
        for (int i=0;i<houseCount;i++) {
            float[] p=positionFor(i);
            float growth=1f;
            if (i==constructionIndex && now-constructionStart<900L) growth=Math.max(.08f,(now-constructionStart)/900f);
            drawShadow(p[0]+.45f,p[1]+.38f,1.25f,1.05f,.13f*growth);
            drawCube(p[0],.16f*growth,p[1],2.3f,.28f*growth,2.1f,
                    cityType==TaskItem.NORMAL?rgba(.86f,.78f,.62f,1f):rgba(.58f,.50f,.43f,1f));
        }
        if (demolitionIndex>=0 && now-demolitionStart<1200L) {
            float[] p=positionFor(demolitionIndex);
            float progress=(now-demolitionStart)/1200f;
            for(int i=0;i<6;i++) {
                float a=(float)(i*Math.PI/3.0);
                drawCube(p[0]+(float)Math.cos(a)*progress,.18f+progress*.32f,
                        p[1]+(float)Math.sin(a)*progress,.18f,.18f,.18f,rgba(.60f,.48f,.34f,1f));
            }
        }
    }

    private float[] positionFor(int index) {
        int col=index%5;
        int row=(index/5)%5;
        return new float[]{-12f+col*6f,-12f+row*6f};
    }

    private void drawShadow(float x,float z,float sx,float sz,float alpha) {
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        draw(circleXZ,60,x,.145f,z,sx,.01f,sz,rgba(.08f,.16f,.12f,alpha));
        GLES20.glEnable(GLES20.GL_CULL_FACE);
    }

    private void drawClipCircle(float x,float y,float scale,float[] color) {
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,x,y,0f);
        Matrix.scaleM(model,0,scale,scale,1f);
        circleXY.position(0);
        GLES20.glVertexAttribPointer(positionHandle,3,GLES20.GL_FLOAT,false,12,circleXY);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glUniformMatrix4fv(matrixHandle,1,false,model,0);
        GLES20.glUniform4fv(colorHandle,1,color,0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,84);
    }

    private void drawCube(float x,float y,float z,float sx,float sy,float sz,float[] color) {
        Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,x,y,z);
        Matrix.scaleM(model,0,sx,sy,sz);
        Matrix.multiplyMM(temp,0,view,0,model,0);
        Matrix.multiplyMM(mvp,0,projection,0,temp,0);
        cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(positionHandle,3,GLES20.GL_FLOAT,false,12,cubeBuffer);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glUniformMatrix4fv(matrixHandle,1,false,mvp,0);

        float[] daylight = cityType == TaskItem.NORMAL ? DAY_FACE_LIGHT : NIGHT_FACE_LIGHT;
        for (int face=0;face<6;face++) {
            float factor=daylight[face];
            shadedColor[0]=clamp(color[0]*factor,0f,1f);
            shadedColor[1]=clamp(color[1]*factor,0f,1f);
            shadedColor[2]=clamp(color[2]*factor,0f,1f);
            shadedColor[3]=color[3];
            GLES20.glUniform4fv(colorHandle,1,shadedColor,0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,face*6,6);
        }
    }

    private void drawOcta(float x,float y,float z,float sx,float sy,float sz,float[] color) {
        draw(octaBuffer,24,x,y,z,sx,sy,sz,color);
    }

    private void draw(FloatBuffer vertices,int count,float x,float y,float z,float sx,float sy,float sz,float[] color) {
        Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,x,y,z);
        Matrix.scaleM(model,0,sx,sy,sz);
        Matrix.multiplyMM(temp,0,view,0,model,0);
        Matrix.multiplyMM(mvp,0,projection,0,temp,0);
        vertices.position(0);
        GLES20.glVertexAttribPointer(positionHandle,3,GLES20.GL_FLOAT,false,12,vertices);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glUniformMatrix4fv(matrixHandle,1,false,mvp,0);
        GLES20.glUniform4fv(colorHandle,1,color,0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);
    }

    private static float[] makeCircle(boolean xy,int segments) {
        float[] data=new float[segments*9];
        for(int i=0;i<segments;i++) {
            double a=i*Math.PI*2/segments;
            double b=(i+1)*Math.PI*2/segments;
            int o=i*9;
            data[o]=0; data[o+1]=0; data[o+2]=0;
            if(xy) {
                data[o+3]=(float)Math.cos(a); data[o+4]=(float)Math.sin(a); data[o+5]=0;
                data[o+6]=(float)Math.cos(b); data[o+7]=(float)Math.sin(b); data[o+8]=0;
            } else {
                data[o+3]=(float)Math.cos(a); data[o+4]=0; data[o+5]=(float)Math.sin(a);
                data[o+6]=(float)Math.cos(b); data[o+7]=0; data[o+8]=(float)Math.sin(b);
            }
        }
        return data;
    }

    private static float[] rgba(float r,float g,float b,float a) { return new float[]{r,g,b,a}; }
    private static float clamp(float v,float min,float max) { return Math.max(min,Math.min(max,v)); }
    private static FloatBuffer buffer(float[] data) {
        FloatBuffer out=ByteBuffer.allocateDirect(data.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        out.put(data).position(0);
        return out;
    }

    private static int createProgram(String vertexSource,String fragmentSource) {
        int vertex=compileShader(GLES20.GL_VERTEX_SHADER,vertexSource);
        int fragment=compileShader(GLES20.GL_FRAGMENT_SHADER,fragmentSource);
        int result=GLES20.glCreateProgram();
        GLES20.glAttachShader(result,vertex);
        GLES20.glAttachShader(result,fragment);
        GLES20.glLinkProgram(result);
        return result;
    }

    private static int compileShader(int type,String source) {
        int shader=GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader,source);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
