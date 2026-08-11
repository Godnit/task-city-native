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
    // Directional light is above and to the rear-left of the map. The sun itself
    // is intentionally not drawn; only its light and geometry-projected shadows
    // are visible on the lawn, like a mobile city-building game.
    private static final float[] PLANAR_SHADOW = {
            1f, 0f, 0f, 0f,
            .74f, 0f, .92f, 0f,
            0f, 0f, 1f, 0f,
            0f, .058f, 0f, 1f
    };
    private static final float[] DAY_FACE_LIGHT = {.96f,.58f,.72f,1.08f,1.24f,.48f};
    private static final float[] NIGHT_FACE_LIGHT = {.74f,.47f,.55f,.82f,.90f,.40f};
    private final FloatBuffer cubeBuffer = buffer(CUBE);
    private final FloatBuffer octaBuffer = buffer(OCTAHEDRON);
    private final FloatBuffer circleXZ = buffer(makeCircle(false, 20));
    private final FloatBuffer circleXY = buffer(makeCircle(true, 28));
    private final FloatBuffer sphereBuffer = buffer(makeSphere(14, 7));
    private final FloatBuffer cylinderBuffer = buffer(makeCylinder(12));
    private final int sphereVertexCount = sphereBuffer.capacity() / 6;
    private final int cylinderVertexCount = cylinderBuffer.capacity() / 6;
    private final FloatBuffer skyDay = buffer(SKY);
    private final FloatBuffer skyNight = buffer(NIGHT_SKY);
    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] model = new float[16];
    private final float[] temp = new float[16];
    private final float[] world = new float[16];
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
    private int litProgram;
    private int litPositionHandle;
    private int litNormalHandle;
    private int litMatrixHandle;
    private int litColorHandle;
    private int litAmbientHandle;
    private volatile int houseCount;
    private volatile int cityType = TaskItem.NORMAL;
    private volatile float desiredX;
    private volatile float desiredZ;
    private volatile float desiredDistance = 22f;
    private float cameraX;
    private float cameraZ;
    private float distance = 22f;
    private float viewportRatio = 1f;
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
        litProgram = createProgram(
                "uniform mat4 uMVP; uniform float uAmbient; " +
                        "attribute vec3 aPosition; attribute vec3 aNormal; varying float vLight; " +
                        "void main(){vec3 lightDir=normalize(vec3(-0.58,0.78,-0.72));" +
                        "vLight=uAmbient+(1.0-uAmbient)*max(dot(normalize(aNormal),lightDir),0.0);" +
                        "gl_Position=uMVP*vec4(aPosition,1.0);}",
                "precision mediump float; uniform vec4 uColor; varying float vLight; " +
                        "void main(){gl_FragColor=vec4(uColor.rgb*vLight,uColor.a);}"
        );
        litPositionHandle = GLES20.glGetAttribLocation(litProgram, "aPosition");
        litNormalHandle = GLES20.glGetAttribLocation(litProgram, "aNormal");
        litMatrixHandle = GLES20.glGetUniformLocation(litProgram, "uMVP");
        litColorHandle = GLES20.glGetUniformLocation(litProgram, "uColor");
        litAmbientHandle = GLES20.glGetUniformLocation(litProgram, "uAmbient");
        Matrix.setIdentityM(identity, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / Math.max(1, height);
        viewportRatio = Math.max(.5f, ratio);
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
        // Keep a narrow strip of sky, but frame the lawn as the subject. The sun
        // is a directional light in the scene and is never a background graphic.
        float lookY = 3.4f * (22f / Math.max(14.5f, distance));
        Matrix.setLookAtM(view, 0, eyeX, eyeY, eyeZ, cameraX, lookY, cameraZ, 0f, 1f, 0f);

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
            drawCloud(0.10f, 0.62f, 0.14f);
            drawCloud(0.72f, 0.43f, 0.11f);
        } else {
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
        float[] grass = cityType == TaskItem.NORMAL ? rgba(.43f,.76f,.27f,1f) : rgba(.22f,.46f,.30f,1f);
        float[] outerGrass = cityType == TaskItem.NORMAL ? rgba(.31f,.64f,.25f,1f) : rgba(.14f,.32f,.27f,1f);

        // Two clean rectangular slabs. There are no large octahedron "hills"
        // intersecting the camera anymore, which were the triangular cracks in v2.1.
        drawCube(0,-.46f,0,58f,.75f,58f,outerGrass);
        drawCube(0,-.055f,0,40f,.18f,40f,grass);

        // Identical, evenly spaced rounded trees around the outer lawn. They stay
        // outside the empty build area while their real silhouettes remain visible.
        float[][] trees = {
                {-18,-22},{-12,-22},{-6,-22},{0,-22},{6,-22},{12,-22},{18,-22},
                {-22,-14},{22,-14},{-22,-7},{22,-7},{-22,0},{22,0},{-22,7},{22,7},
                {-22,14},{22,14},{-18,22},{-12,22},{-6,22},{0,22},{6,22},{12,22},{18,22}
        };

        drawSceneShadows(trees);
        drawBoundaryWall();
        for (float[] tree : trees) drawTree(tree[0], tree[1], 1f);
    }

    private void drawSceneShadows(float[][] trees) {
        GLES20.glUseProgram(program);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDepthMask(false);
        float alpha = cityType == TaskItem.NORMAL ? .30f : .22f;

        // Wall and cap shadows use the actual wall meshes, not offset rectangles.
        drawProjected(cubeBuffer,36,12,0,.38f,-19.55f,40f,.78f,.72f,alpha);
        drawProjected(cubeBuffer,36,12,0,.38f, 19.55f,40f,.78f,.72f,alpha);
        drawProjected(cubeBuffer,36,12,-19.55f,.38f,0,.72f,.78f,40f,alpha);
        drawProjected(cubeBuffer,36,12, 19.55f,.38f,0,.72f,.78f,40f,alpha);
        for (float[] tree : trees) drawTreeShadow(tree[0], tree[1], 1f, alpha);

        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
    }

    private void drawBoundaryWall() {
        float[] stone = cityType == TaskItem.NORMAL
                ? rgba(.83f,.78f,.66f,1f) : rgba(.48f,.49f,.47f,1f);
        float[] cap = cityType == TaskItem.NORMAL
                ? rgba(.98f,.91f,.74f,1f) : rgba(.65f,.62f,.55f,1f);

        drawCube(0,.38f,-19.55f,40f,.78f,.72f,stone);
        drawCube(0,.38f, 19.55f,40f,.78f,.72f,stone);
        drawCube(-19.55f,.38f,0,.72f,.78f,40f,stone);
        drawCube( 19.55f,.38f,0,.72f,.78f,40f,stone);
        drawCube(0,.82f,-19.55f,40.25f,.14f,.90f,cap);
        drawCube(0,.82f, 19.55f,40.25f,.14f,.90f,cap);
        drawCube(-19.55f,.82f,0,.90f,.14f,40.25f,cap);
        drawCube( 19.55f,.82f,0,.90f,.14f,40.25f,cap);
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
        float[] trunk = cityType == TaskItem.NORMAL ? rgba(.43f,.25f,.12f,1f) : rgba(.28f,.19f,.14f,1f);
        float[] leaf = cityType == TaskItem.NORMAL ? rgba(.28f,.68f,.24f,1f) : rgba(.15f,.43f,.28f,1f);
        drawLitMesh(cylinderBuffer,cylinderVertexCount,x,.72f*scale,z,.34f*scale,1.44f*scale,.34f*scale,trunk);
        drawLitMesh(sphereBuffer,sphereVertexCount,x,1.82f*scale,z,1.12f*scale,1.04f*scale,1.12f*scale,leaf);
    }

    private void drawTreeShadow(float x,float z,float scale,float alpha) {
        drawProjected(cylinderBuffer,cylinderVertexCount,24,x,.72f*scale,z,
                .34f*scale,1.44f*scale,.34f*scale,alpha);
        drawProjected(sphereBuffer,sphereVertexCount,24,x,1.82f*scale,z,
                1.12f*scale,1.04f*scale,1.12f*scale,alpha);
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
        draw(circleXZ,60,x,.055f,z,sx,.01f,sz,rgba(.06f,.12f,.08f,alpha));
        GLES20.glEnable(GLES20.GL_CULL_FACE);
    }

    private void drawClipCircle(float x,float y,float scale,float[] color) {
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,x,y,0f);
        Matrix.scaleM(model,0,scale/viewportRatio,scale,1f);
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

    private void drawLitMesh(FloatBuffer vertices,int count,float x,float y,float z,
                             float sx,float sy,float sz,float[] color) {
        Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,x,y,z);
        Matrix.scaleM(model,0,sx,sy,sz);
        Matrix.multiplyMM(temp,0,view,0,model,0);
        Matrix.multiplyMM(mvp,0,projection,0,temp,0);
        GLES20.glUseProgram(litProgram);
        vertices.position(0);
        GLES20.glVertexAttribPointer(litPositionHandle,3,GLES20.GL_FLOAT,false,24,vertices);
        GLES20.glEnableVertexAttribArray(litPositionHandle);
        vertices.position(3);
        GLES20.glVertexAttribPointer(litNormalHandle,3,GLES20.GL_FLOAT,false,24,vertices);
        GLES20.glEnableVertexAttribArray(litNormalHandle);
        GLES20.glUniformMatrix4fv(litMatrixHandle,1,false,mvp,0);
        GLES20.glUniform4fv(litColorHandle,1,color,0);
        GLES20.glUniform1f(litAmbientHandle,cityType==TaskItem.NORMAL?.62f:.48f);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);
        GLES20.glUseProgram(program);
    }

    private void drawProjected(FloatBuffer vertices,int count,int stride,float x,float y,float z,
                               float sx,float sy,float sz,float alpha) {
        Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,x,y,z);
        Matrix.scaleM(model,0,sx,sy,sz);
        Matrix.multiplyMM(world,0,PLANAR_SHADOW,0,model,0);
        Matrix.multiplyMM(temp,0,view,0,world,0);
        Matrix.multiplyMM(mvp,0,projection,0,temp,0);
        vertices.position(0);
        GLES20.glVertexAttribPointer(positionHandle,3,GLES20.GL_FLOAT,false,stride,vertices);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glUniformMatrix4fv(matrixHandle,1,false,mvp,0);
        GLES20.glUniform4fv(colorHandle,1,
                cityType==TaskItem.NORMAL?rgba(.055f,.10f,.065f,alpha):rgba(.025f,.05f,.07f,alpha),0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);
    }

    private void drawOcta(float x,float y,float z,float sx,float sy,float sz,float[] color) {
        draw(octaBuffer,24,x,y,z,sx,sy,sz,color);
    }

    private void drawLitOcta(float x,float y,float z,float sx,float sy,float sz,float[] color) {
        Matrix.setIdentityM(model,0);
        Matrix.translateM(model,0,x,y,z);
        Matrix.scaleM(model,0,sx,sy,sz);
        Matrix.multiplyMM(temp,0,view,0,model,0);
        Matrix.multiplyMM(mvp,0,projection,0,temp,0);
        octaBuffer.position(0);
        GLES20.glVertexAttribPointer(positionHandle,3,GLES20.GL_FLOAT,false,12,octaBuffer);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glUniformMatrix4fv(matrixHandle,1,false,mvp,0);
        float[] light = cityType == TaskItem.NORMAL
                ? new float[]{1.18f,1.03f,.76f,.58f,.88f,.72f,.50f,.42f}
                : new float[]{.88f,.78f,.60f,.48f,.69f,.57f,.43f,.37f};
        for (int face=0;face<8;face++) {
            float factor=light[face];
            shadedColor[0]=clamp(color[0]*factor,0f,1f);
            shadedColor[1]=clamp(color[1]*factor,0f,1f);
            shadedColor[2]=clamp(color[2]*factor,0f,1f);
            shadedColor[3]=color[3];
            GLES20.glUniform4fv(colorHandle,1,shadedColor,0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,face*3,3);
        }
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

    private static float[] makeSphere(int segments,int rings) {
        float[] data=new float[segments*rings*6*6];
        int o=0;
        for(int ring=0;ring<rings;ring++) {
            float v0=(float)ring/rings;
            float v1=(float)(ring+1)/rings;
            float p0=(float)(-Math.PI/2+Math.PI*v0);
            float p1=(float)(-Math.PI/2+Math.PI*v1);
            for(int seg=0;seg<segments;seg++) {
                float a0=(float)(seg*Math.PI*2/segments);
                float a1=(float)((seg+1)*Math.PI*2/segments);
                float[] n00=spherePoint(p0,a0), n01=spherePoint(p0,a1);
                float[] n10=spherePoint(p1,a0), n11=spherePoint(p1,a1);
                o=putVertex(data,o,n00,n00); o=putVertex(data,o,n10,n10); o=putVertex(data,o,n11,n11);
                o=putVertex(data,o,n00,n00); o=putVertex(data,o,n11,n11); o=putVertex(data,o,n01,n01);
            }
        }
        return data;
    }

    private static float[] makeCylinder(int segments) {
        float[] data=new float[segments*12*6];
        int o=0;
        for(int i=0;i<segments;i++) {
            float a0=(float)(i*Math.PI*2/segments), a1=(float)((i+1)*Math.PI*2/segments);
            float c0=(float)Math.cos(a0), s0=(float)Math.sin(a0);
            float c1=(float)Math.cos(a1), s1=(float)Math.sin(a1);
            float[] b0={c0,-.5f,s0}, b1={c1,-.5f,s1}, t0={c0,.5f,s0}, t1={c1,.5f,s1};
            float[] n0={c0,0,s0}, n1={c1,0,s1};
            o=putVertex(data,o,b0,n0); o=putVertex(data,o,t0,n0); o=putVertex(data,o,t1,n1);
            o=putVertex(data,o,b0,n0); o=putVertex(data,o,t1,n1); o=putVertex(data,o,b1,n1);
            float[] top={0,.5f,0}, up={0,1,0};
            o=putVertex(data,o,top,up); o=putVertex(data,o,t1,up); o=putVertex(data,o,t0,up);
            float[] bottom={0,-.5f,0}, down={0,-1,0};
            o=putVertex(data,o,bottom,down); o=putVertex(data,o,b0,down); o=putVertex(data,o,b1,down);
        }
        return data;
    }

    private static float[] spherePoint(float pitch,float yaw) {
        float cp=(float)Math.cos(pitch);
        return new float[]{cp*(float)Math.cos(yaw),(float)Math.sin(pitch),cp*(float)Math.sin(yaw)};
    }

    private static int putVertex(float[] out,int o,float[] position,float[] normal) {
        out[o++]=position[0]; out[o++]=position[1]; out[o++]=position[2];
        out[o++]=normal[0]; out[o++]=normal[1]; out[o++]=normal[2];
        return o;
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
