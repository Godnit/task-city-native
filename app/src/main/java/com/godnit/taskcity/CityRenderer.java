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

    private static final float[] ROOF = {
            -0.65f,0, 0.60f,  0.65f,0, 0.60f,  0,0.72f,0,
             0.65f,0, 0.60f,  0.65f,0,-0.60f,  0,0.72f,0,
             0.65f,0,-0.60f, -0.65f,0,-0.60f,  0,0.72f,0,
            -0.65f,0,-0.60f, -0.65f,0, 0.60f,  0,0.72f,0,
            -0.65f,0,-0.60f,  0.65f,0,-0.60f,  0.65f,0,0.60f,
            -0.65f,0,-0.60f,  0.65f,0, 0.60f, -0.65f,0,0.60f
    };

    private final FloatBuffer cubeBuffer = buffer(CUBE);
    private final FloatBuffer roofBuffer = buffer(ROOF);
    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] model = new float[16];
    private final float[] temp = new float[16];
    private final float[] mvp = new float[16];

    private int program;
    private int positionHandle;
    private int matrixHandle;
    private int colorHandle;
    private volatile int houseCount;
    private volatile int cityType = TaskItem.NORMAL;
    private float yaw = 35f;
    private float pitch = 38f;
    private float distance = 16f;
    private volatile long constructionStart;
    private volatile int constructionIndex = -1;
    private volatile long demolitionStart;
    private volatile int demolitionIndex = -1;

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glClearColor(0.72f, 0.88f, 0.96f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        program = createProgram(
                "uniform mat4 uMVP; attribute vec4 aPosition; void main(){ gl_Position=uMVP*aPosition; }",
                "precision mediump float; uniform vec4 uColor; void main(){ gl_FragColor=uColor; }"
        );
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        matrixHandle = GLES20.glGetUniformLocation(program, "uMVP");
        colorHandle = GLES20.glGetUniformLocation(program, "uColor");
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / Math.max(1, height);
        Matrix.perspectiveM(projection, 0, 42f, ratio, 1f, 80f);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program);

        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        float eyeX = (float) (distance * Math.cos(pitchRad) * Math.sin(yawRad));
        float eyeY = (float) (distance * Math.sin(pitchRad));
        float eyeZ = (float) (distance * Math.cos(pitchRad) * Math.cos(yawRad));
        Matrix.setLookAtM(view, 0, eyeX, eyeY, eyeZ, 0f, 0f, 0f, 0f, 1f, 0f);

        float[] ground = cityType == TaskItem.NORMAL
                ? new float[]{0.30f, 0.66f, 0.44f, 1f}
                : new float[]{0.55f, 0.43f, 0.32f, 1f};
        drawCube(0, -0.2f, 0, 13f, 0.35f, 13f, ground);
        drawCube(0, 0f, 0, 1.1f, 0.05f, 12f, new float[]{0.22f,0.26f,0.31f,1});
        drawCube(0, 0.01f, 0, 12f, 0.05f, 1.1f, new float[]{0.22f,0.26f,0.31f,1});

        long now = System.currentTimeMillis();
        for (int i = 0; i < houseCount; i++) {
            float growth = 1f;
            if (i == constructionIndex && now - constructionStart < 1100L) {
                growth = Math.max(0.08f, (now - constructionStart) / 1100f);
            }
            drawHouse(i, growth);
        }

        if (demolitionIndex >= 0 && now - demolitionStart < 1400L) {
            drawDebris(demolitionIndex, (now - demolitionStart) / 1400f);
        }
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

    void rotate(float dx, float dy) {
        yaw += dx;
        pitch = Math.max(18f, Math.min(68f, pitch + dy));
    }

    void zoom(float scale) {
        distance = Math.max(8f, Math.min(27f, distance / scale));
    }

    private void drawHouse(int index, float growth) {
        float[] position = positionFor(index);
        float x = position[0];
        float z = position[1];
        float height = (1.25f + (index % 3) * 0.28f) * growth;
        float width = 1.15f + (index % 2) * 0.22f;
        float[] wall;
        if (cityType == TaskItem.NORMAL) {
            float[][] colors = {{0.92f,0.75f,0.45f,1},{0.35f,0.68f,0.78f,1},{0.89f,0.52f,0.43f,1}};
            wall = colors[index % colors.length];
        } else {
            float[][] colors = {{0.84f,0.39f,0.26f,1},{0.98f,0.64f,0.25f,1},{0.67f,0.31f,0.36f,1}};
            wall = colors[index % colors.length];
        }
        drawCube(x, height / 2f, z, width, height, 1.2f, wall);
        drawCube(x, 0.52f * growth, z + 0.605f, 0.28f, 0.72f * growth, 0.04f,
                new float[]{0.23f,0.16f,0.12f,1});
        drawRoof(x, height, z, width, growth,
                cityType == TaskItem.NORMAL
                        ? new float[]{0.36f,0.18f,0.14f,1}
                        : new float[]{0.42f,0.13f,0.10f,1});
    }

    private void drawDebris(int index, float progress) {
        float[] position = positionFor(index);
        for (int i = 0; i < 7; i++) {
            float angle = (float) (i * Math.PI * 2 / 7.0);
            float spread = progress * 1.5f;
            float y = Math.max(0.12f, 0.8f - progress * 0.75f + (i % 2) * 0.22f);
            drawCube(position[0] + (float)Math.cos(angle) * spread, y,
                    position[1] + (float)Math.sin(angle) * spread,
                    0.28f, 0.26f, 0.28f, new float[]{0.52f,0.22f,0.16f,1});
        }
    }

    private float[] positionFor(int index) {
        int ringIndex = index % 24;
        int ring = index / 24;
        int col = ringIndex % 6;
        int row = ringIndex / 6;
        float x = -5.0f + col * 2.0f;
        float z = -3.0f + row * 2.0f;
        if (ring > 0) {
            x += (ring % 2 == 0 ? -0.35f : 0.35f) * ring;
            z += 0.3f * ring;
        }
        if (Math.abs(x) < 0.9f) x += x >= 0 ? 1.15f : -1.15f;
        if (Math.abs(z) < 0.9f) z += z >= 0 ? 1.15f : -1.15f;
        return new float[]{x, z};
    }

    private void drawCube(float x, float y, float z, float sx, float sy, float sz, float[] color) {
        draw(cubeBuffer, 36, x, y, z, sx, sy, sz, color);
    }

    private void drawRoof(float x, float y, float z, float width, float growth, float[] color) {
        draw(roofBuffer, 18, x, y, z, width, growth * 0.75f, 1.15f, color);
    }

    private void draw(FloatBuffer vertices, int count, float x, float y, float z,
                      float sx, float sy, float sz, float[] color) {
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, x, y, z);
        Matrix.scaleM(model, 0, sx, sy, sz);
        Matrix.multiplyMM(temp, 0, view, 0, model, 0);
        Matrix.multiplyMM(mvp, 0, projection, 0, temp, 0);
        vertices.position(0);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertices);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, mvp, 0);
        GLES20.glUniform4fv(colorHandle, 1, color, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count);
    }

    private static FloatBuffer buffer(float[] data) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(data.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        buffer.put(data).position(0);
        return buffer;
    }

    private static int createProgram(String vertexSource, String fragmentSource) {
        int vertex = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        int fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertex);
        GLES20.glAttachShader(program, fragment);
        GLES20.glLinkProgram(program);
        return program;
    }

    private static int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
