package com.l4dar.nativeapp.render;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import com.l4dar.nativeapp.core.math.Quat;
import com.l4dar.nativeapp.core.math.Vec3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * Renders car mesh from GLB models using OpenGL ES 3.0.
 * Loads 3D car models (octane, dominus, fennec) from assets.
 */
public final class CarMeshRenderer {
    private static final float TARGET_MAX_DIMENSION = 1.35f;

    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "layout(location = 0) in vec3 aPosition;\n" +
        "layout(location = 1) in vec3 aNormal;\n" +
        "layout(location = 2) in vec4 aColor;\n" +
        "uniform mat4 uProjection;\n" +
        "uniform mat4 uView;\n" +
        "uniform mat4 uModel;\n" +
        "out vec3 vNormal;\n" +
        "out vec3 vPosition;\n" +
        "out vec4 vColor;\n" +
        "void main() {\n" +
        "  vPosition = vec3(uModel * vec4(aPosition, 1.0));\n" +
        "  vNormal = normalize(mat3(uModel) * aNormal);\n" +
        "  vColor = aColor;\n" +
        "  gl_Position = uProjection * uView * vec4(vPosition, 1.0);\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "in vec3 vNormal;\n" +
        "in vec3 vPosition;\n" +
        "in vec4 vColor;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        "  // Studio lighting matching web sceneManager.js\n" +
        "  vec3 Lkey   = normalize(vec3( 0.0,    0.40,  0.92));\n" +
        "  vec3 LfillL = normalize(vec3(-0.858,  0.514, 0.0));\n" +
        "  vec3 LfillR = normalize(vec3( 0.858,  0.514, 0.0));\n" +
        "  vec3 Lback  = normalize(vec3( 0.0,    0.316,-0.949));\n" +
        "  float dKey   = max(0.0, dot(vNormal, Lkey));\n" +
        "  float dFillL = max(0.0, dot(vNormal, LfillL));\n" +
        "  float dFillR = max(0.0, dot(vNormal, LfillR));\n" +
        "  float dBack  = max(0.0, dot(vNormal, Lback));\n" +
        "  float light = 0.25 + 0.40*dKey + 0.20*dFillL + 0.20*dFillR + 0.15*dBack;\n" +
        "  light = clamp(light, 0.0, 1.5);\n" +
        "  fragColor = vec4(vColor.rgb * light, vColor.a);\n" +
        "}\n";

    private int program;
    private int positionAttrib;
    private int normalAttrib;
    private int colorAttrib;
    private int projectionUniform;
    private int viewUniform;
    private int modelUniform;

    private int indexCount;
    private int vao;
    private int vbo;
    private int ibo;
    private boolean initialized = false;
    private boolean programReady = false;
    private boolean hasTransparency = false;
    private float modelUniformScale = 1.0f;
    private float modelCenterX = 0.0f;
    private float modelCenterY = 0.0f;
    private float modelCenterZ = 0.0f;
    private Context context;
    private String modelName = "octane";

    public CarMeshRenderer(Context context, String modelName) {
        this.context = context;
        this.modelName = modelName;
        // Defer GL initialization until GL context exists
    }

    public void initialize() {
        if (initialized) return;
        if (!setupShaders()) {
            initialized = false;
            return;
        }
        if (!setupMesh()) {
            initialized = false;
            return;
        }
        initialized = true;
    }

    public void cleanup() {
        if (!initialized) return;
        if (vao != 0) {
            int[] vaos = {vao};
            GLES30.glDeleteVertexArrays(1, vaos, 0);
            vao = 0;
        }
        if (vbo != 0) {
            int[] vbos = {vbo};
            GLES30.glDeleteBuffers(1, vbos, 0);
            vbo = 0;
        }
        if (ibo != 0) {
            int[] ibos = {ibo};
            GLES30.glDeleteBuffers(1, ibos, 0);
            ibo = 0;
        }
        if (program != 0) {
            GLES30.glDeleteProgram(program);
            program = 0;
        }
        programReady = false;
        indexCount = 0;
        initialized = false;
    }

    private boolean setupShaders() {
        programReady = false;

        int vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        if (vertexShader == 0 || fragmentShader == 0) {
            if (vertexShader != 0) {
                GLES30.glDeleteShader(vertexShader);
            }
            if (fragmentShader != 0) {
                GLES30.glDeleteShader(fragmentShader);
            }
            program = 0;
            return false;
        }

        program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);
        GLES30.glLinkProgram(program);

        // Check for link errors
        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            String log = GLES30.glGetProgramInfoLog(program);
            Log.e("CarMeshRenderer", "Program link error: " + log);
            GLES30.glDeleteProgram(program);
            program = 0;
            GLES30.glDeleteShader(vertexShader);
            GLES30.glDeleteShader(fragmentShader);
            return false;
        }

        positionAttrib = GLES30.glGetAttribLocation(program, "aPosition");
        normalAttrib = GLES30.glGetAttribLocation(program, "aNormal");
        colorAttrib = GLES30.glGetAttribLocation(program, "aColor");
        projectionUniform = GLES30.glGetUniformLocation(program, "uProjection");
        viewUniform = GLES30.glGetUniformLocation(program, "uView");
        modelUniform = GLES30.glGetUniformLocation(program, "uModel");

        GLES30.glDeleteShader(vertexShader);
        GLES30.glDeleteShader(fragmentShader);

        programReady = true;
        return true;
    }

    private int compileShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);

        // Check for compile errors
        int[] compileStatus = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            String log = GLES30.glGetShaderInfoLog(shader);
            Log.e("CarMeshRenderer", "Shader compile error: " + log);
            GLES30.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    private boolean setupMesh() {
        if (!programReady) {
            return false;
        }

        // Load GLB model from assets
        GLBLoader.GLBMesh mesh = GLBLoader.loadFromAssets(context, modelName + ".glb");
        if (mesh == null) {
            Log.e("CarMeshRenderer", "Failed to load model: " + modelName + ".glb");
            indexCount = 0;
            return false;
        }

        indexCount = mesh.indexCount;
        hasTransparency = mesh.hasTransparency;

        calculateModelNormalization(mesh);

        // Create and setup VAO
        int[] vaos = new int[1];
        GLES30.glGenVertexArrays(1, vaos, 0);
        vao = vaos[0];
        GLES30.glBindVertexArray(vao);

        // Create VBO for interleaved position + normal + color data
        int[] vbos = new int[1];
        GLES30.glGenBuffers(1, vbos, 0);
        vbo = vbos[0];
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);

        // Interleave positions, normals, and material color: pos3 | norm3 | color4
        FloatBuffer vertexBuffer = ByteBuffer
            .allocateDirect(mesh.vertexCount * 10 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        for (int i = 0; i < mesh.vertexCount; i++) {
            vertexBuffer.put(mesh.positions[i * 3] - modelCenterX);
            vertexBuffer.put(mesh.positions[i * 3 + 1] - modelCenterY);
            vertexBuffer.put(mesh.positions[i * 3 + 2] - modelCenterZ);
            vertexBuffer.put(mesh.normals[i * 3]);
            vertexBuffer.put(mesh.normals[i * 3 + 1]);
            vertexBuffer.put(mesh.normals[i * 3 + 2]);
            vertexBuffer.put(mesh.colors[i * 4]);
            vertexBuffer.put(mesh.colors[i * 4 + 1]);
            vertexBuffer.put(mesh.colors[i * 4 + 2]);
            vertexBuffer.put(mesh.colors[i * 4 + 3]);
        }
        vertexBuffer.flip();

        ByteBuffer vboBuffer = ByteBuffer.allocateDirect(vertexBuffer.capacity() * 4);
        vboBuffer.order(ByteOrder.nativeOrder());
        vboBuffer.asFloatBuffer().put(vertexBuffer);
        vboBuffer.position(0);

        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vboBuffer.capacity(),
            vboBuffer, GLES30.GL_STATIC_DRAW);

        // Vertex attributes: pos3(offset=0) | norm3(offset=12) | color4(offset=24)
        int stride = 10 * 4; // 40 bytes
        GLES30.glEnableVertexAttribArray(positionAttrib);
        GLES30.glVertexAttribPointer(positionAttrib, 3, GLES30.GL_FLOAT, false, stride, 0);

        GLES30.glEnableVertexAttribArray(normalAttrib);
        GLES30.glVertexAttribPointer(normalAttrib, 3, GLES30.GL_FLOAT, false, stride, 12);

        GLES30.glEnableVertexAttribArray(colorAttrib);
        GLES30.glVertexAttribPointer(colorAttrib, 4, GLES30.GL_FLOAT, false, stride, 24);

        // Create IBO for indices
        int[] ibos = new int[1];
        GLES30.glGenBuffers(1, ibos, 0);
        ibo = ibos[0];
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ibo);

        IntBuffer indexBuffer = ByteBuffer
            .allocateDirect(mesh.indexCount * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer();
        for (int i = 0; i < mesh.indexCount; i++) {
            indexBuffer.put(mesh.indices[i]);
        }
        indexBuffer.flip();

        ByteBuffer iboBuffer = ByteBuffer.allocateDirect(indexBuffer.capacity() * 4);
        iboBuffer.order(ByteOrder.nativeOrder());
        iboBuffer.asIntBuffer().put(indexBuffer);
        iboBuffer.position(0);

        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, iboBuffer.capacity(),
            iboBuffer, GLES30.GL_STATIC_DRAW);

        GLES30.glBindVertexArray(0);

        return true;
    }

    /**
     * Render the car mesh.
     *
     * @param carOrientation car quaternion (local-to-world rotation)
     * @param carPosition    car world-space position
     * @param projection     pre-computed projection matrix (float[16], column-major)
     * @param view           pre-computed view matrix (float[16], column-major)
     */
    public void render(Quat carOrientation, Vec3 carPosition, float[] projection, float[] view) {
        if (!initialized || !programReady || indexCount == 0) return;

        GLES30.glUseProgram(program);
        GLES30.glDisable(GLES30.GL_CULL_FACE);
        if (hasTransparency) {
            GLES30.glEnable(GLES30.GL_BLEND);
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        }

        // Build model matrix with normalized scale so every car fits the native camera framing.
        float[] model = new float[16];
        float[] local = new float[16];
        float[] combined = new float[16];
        Matrix.setIdentityM(model, 0);
        applyQuaternionToMatrix(model, carOrientation);
        Matrix.setIdentityM(local, 0);
        applyModelPresetTransform(local);
        Matrix.multiplyMM(combined, 0, model, 0, local, 0);
        System.arraycopy(combined, 0, model, 0, 16);
        model[12] += carPosition.x;
        model[13] += carPosition.y;
        model[14] += carPosition.z;

        GLES30.glUniformMatrix4fv(projectionUniform, 1, false, projection, 0);
        GLES30.glUniformMatrix4fv(viewUniform, 1, false, view, 0);
        GLES30.glUniformMatrix4fv(modelUniform, 1, false, model, 0);

        GLES30.glBindVertexArray(vao);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, 0);
        GLES30.glBindVertexArray(0);

        if (hasTransparency) {
            GLES30.glDisable(GLES30.GL_BLEND);
        }
        GLES30.glEnable(GLES30.GL_CULL_FACE);
    }

    public void setModelName(String nextModelName) {
        if (nextModelName == null || nextModelName.equals(modelName)) {
            return;
        }
        cleanup();
        modelName = nextModelName;
        initialize();
    }

    private void applyQuaternionToMatrix(float[] matrix, Quat q) {
        // Convert quaternion to rotation matrix
        float x2 = q.x + q.x;
        float y2 = q.y + q.y;
        float z2 = q.z + q.z;
        float xx = q.x * x2;
        float xy = q.x * y2;
        float xz = q.x * z2;
        float yy = q.y * y2;
        float yz = q.y * z2;
        float zz = q.z * z2;
        float wx = q.w * x2;
        float wy = q.w * y2;
        float wz = q.w * z2;

        matrix[0] = 1 - (yy + zz);
        matrix[1] = xy + wz;
        matrix[2] = xz - wy;
        matrix[4] = xy - wz;
        matrix[5] = 1 - (xx + zz);
        matrix[6] = yz + wx;
        matrix[8] = xz + wy;
        matrix[9] = yz - wx;
        matrix[10] = 1 - (xx + yy);
    }

    private void applyModelPresetTransform(float[] matrix) {
        if ("dominus".equals(modelName)) {
            // Dominus GLB default forward is opposite Octane/Fennec; flip around Y only.
            Matrix.rotateM(matrix, 0, 90f, 0f, 1f, 0f);
        }
        Matrix.scaleM(matrix, 0, modelUniformScale, modelUniformScale, modelUniformScale);
    }

    private void calculateModelNormalization(GLBLoader.GLBMesh mesh) {
        if (mesh.positions.length < 9) {
            modelCenterX = 0.0f;
            modelCenterY = 0.0f;
            modelCenterZ = 0.0f;
            modelUniformScale = 1.0f;
            return;
        }

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        int vertexCount = mesh.positions.length / 3;
        float[] xs = new float[vertexCount];
        float[] ys = new float[vertexCount];
        float[] zs = new float[vertexCount];
        int vi = 0;

        for (int i = 0; i < mesh.positions.length; i += 3) {
            float x = mesh.positions[i];
            float y = mesh.positions[i + 1];
            float z = mesh.positions[i + 2];
            xs[vi] = x;
            ys[vi] = y;
            zs[vi] = z;
            vi++;
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (z < minZ) minZ = z;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            if (z > maxZ) maxZ = z;
        }

        // Trim extreme outliers that can make the whole model microscopic after normalization.
        Arrays.sort(xs);
        Arrays.sort(ys);
        Arrays.sort(zs);
        int lowIdx = Math.max(0, (int) (vertexCount * 0.01f));
        int highIdx = Math.min(vertexCount - 1, (int) (vertexCount * 0.99f));
        if (highIdx <= lowIdx) {
            highIdx = vertexCount - 1;
            lowIdx = 0;
        }

        float robustMinX = xs[lowIdx];
        float robustMaxX = xs[highIdx];
        float robustMinY = ys[lowIdx];
        float robustMaxY = ys[highIdx];
        float robustMinZ = zs[lowIdx];
        float robustMaxZ = zs[highIdx];

        float robustSizeX = robustMaxX - robustMinX;
        float robustSizeY = robustMaxY - robustMinY;
        float robustSizeZ = robustMaxZ - robustMinZ;
        float robustMaxDimension = Math.max(robustSizeX, Math.max(robustSizeY, robustSizeZ));

        float usedMinX = minX;
        float usedMaxX = maxX;
        float usedMinY = minY;
        float usedMaxY = maxY;
        float usedMinZ = minZ;
        float usedMaxZ = maxZ;
        float usedMaxDimension = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));

        if (robustMaxDimension > 1e-6f && usedMaxDimension > 1e-6f) {
            float outlierRatio = usedMaxDimension / robustMaxDimension;
            if (outlierRatio > 20f) {
                usedMinX = robustMinX;
                usedMaxX = robustMaxX;
                usedMinY = robustMinY;
                usedMaxY = robustMaxY;
                usedMinZ = robustMinZ;
                usedMaxZ = robustMaxZ;
                usedMaxDimension = robustMaxDimension;
            }
        }

        // Keep native vertical/longitudinal pivot and only enforce exact lateral centering.
        modelCenterX = (usedMinX + usedMaxX) * 0.5f;
        modelCenterY = 0.0f;
        modelCenterZ = 0.0f;

        if (usedMaxDimension > 1e-6f) {
            modelUniformScale = TARGET_MAX_DIMENSION / usedMaxDimension;
        } else {
            modelUniformScale = 1.0f;
        }

    }
}
