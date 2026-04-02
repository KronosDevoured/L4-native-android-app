package com.l4dar.nativeapp.render;

import android.opengl.GLES30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Renders an infinite-looking grid plane at y=0 using a procedural GLSL shader.
 * The quad is always centered on the car's XZ position so the grid extends
 * in every direction without a visible boundary at typical flight altitudes.
 *
 * Matching the web version's aesthetic:
 *   - Minor grid lines every 5 world units (light blue-grey)
 *   - Major grid lines every 50 world units (darker blue-grey)
 *   - Ground fill colour: light grey-blue
 *   - Distance fade to sky colour at ~400 units from the car
 */
public final class GroundRenderer {

    // -------------------------------------------------------------------------
    // Shaders
    // -------------------------------------------------------------------------

    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "layout(location = 0) in vec2 aXY;\n" +         // local XY offsets from car
        "uniform mat4 uProjection;\n" +
        "uniform mat4 uView;\n" +
        "uniform vec2 uCarXY;\n" +                       // car world XY for centering
        "out vec3 vWorldPos;\n" +
        "out float vLocalDist;\n" +
        "void main() {\n" +
        // Vertical grid sits slightly behind the car at Z = -0.5
        "  vWorldPos = vec3(aXY.x + uCarXY.x, aXY.y + uCarXY.y, -0.5);\n" +
        "  vLocalDist = length(aXY);\n" +
        "  gl_Position = uProjection * uView * vec4(vWorldPos, 1.0);\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision highp float;\n" +
        "in vec3 vWorldPos;\n" +
        "in float vLocalDist;\n" +
        "uniform float uNightMode;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        // Grid lines run along X and Y (the wall surface axes)
        "  vec2 minorC = vWorldPos.xy / 5.0;\n" +
        "  vec2 minorD = fwidth(minorC);\n" +
        "  vec2 minorG = abs(fract(minorC - 0.5) - 0.5) / max(minorD, vec2(0.001));\n" +
        "  float minor = 1.0 - clamp(min(minorG.x, minorG.y), 0.0, 1.0);\n" +
        // Major grid lines every 50 world units
        "  vec2 majorC = vWorldPos.xy / 50.0;\n" +
        "  vec2 majorD = fwidth(majorC);\n" +
        "  vec2 majorG = abs(fract(majorC - 0.5) - 0.5) / max(majorD, vec2(0.001));\n" +
        "  float major = 1.0 - clamp(min(majorG.x, majorG.y), 0.0, 1.0);\n" +
        // Day palette
        "  vec3 skyDay    = vec3(0.93, 0.95, 0.98);\n" +
        "  vec3 groundDay = vec3(0.82, 0.86, 0.91);\n" +
        "  vec3 minDay    = vec3(0.60, 0.65, 0.78);\n" +
        "  vec3 majDay    = vec3(0.30, 0.38, 0.58);\n" +
        // Night palette (true black background, subtle dark grid)
        "  vec3 skyNight    = vec3(0.00, 0.00, 0.00);\n" +
        "  vec3 groundNight = vec3(0.00, 0.00, 0.00);\n" +
        "  vec3 minNight    = vec3(0.09, 0.10, 0.12);\n" +
        "  vec3 majNight    = vec3(0.14, 0.16, 0.20);\n" +
        "  vec3 sky    = mix(skyDay, skyNight, uNightMode);\n" +
        "  vec3 ground = mix(groundDay, groundNight, uNightMode);\n" +
        "  vec3 mincol = mix(minDay, minNight, uNightMode);\n" +
        "  vec3 majcol = mix(majDay, majNight, uNightMode);\n" +
        // Composite: ground → minor overlay → major overlay
        "  vec3 col = ground;\n" +
        "  col = mix(col, mincol, minor * 0.55);\n" +
        "  col = mix(col, majcol, major * 0.85);\n" +
        // Distance fade from centre — fade toward sky at the edges
        "  col = mix(col, sky, smoothstep(120.0, 450.0, vLocalDist));\n" +
        // Keep day semi-transparent; make night effectively opaque black/dark grid
        "  float alpha = mix(0.85, 1.0, uNightMode);\n" +
        "  fragColor = vec4(col, alpha);\n" +
        "}\n";

    // -------------------------------------------------------------------------
    // GL state
    // -------------------------------------------------------------------------

    private int program       = 0;
    private int projUniform   = -1;
    private int viewUniform   = -1;
    private int carXYUniform  = -1;
    private int nightModeUniform = -1;
    private int vao           = 0;
    private int vbo           = 0;
    private boolean initialized = false;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void initialize() {
        if (initialized) return;
        setupShaders();
        setupMesh();
        initialized = true;
    }

    public void cleanup() {
        if (!initialized) return;
        if (vao != 0) {
            int[] arr = {vao};
            GLES30.glDeleteVertexArrays(1, arr, 0);
            vao = 0;
        }
        if (vbo != 0) {
            int[] arr = {vbo};
            GLES30.glDeleteBuffers(1, arr, 0);
            vbo = 0;
        }
        if (program != 0) {
            GLES30.glDeleteProgram(program);
            program = 0;
        }
        initialized = false;
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    /**
     * Render the vertical grid.
     *
     * @param projection pre-computed projection matrix
     * @param view       pre-computed view matrix
     * @param carX       car world X (grid follows car horizontally)
     * @param carY       car world Y (grid follows car vertically)
     */
    public void render(float[] projection, float[] view, float carX, float carY, boolean nightMode) {
        if (!initialized) return;

        // Enable alpha blending so the grid is semi-transparent over the sky colour
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        // Don't write depth — the car always renders on top regardless of order
        GLES30.glDepthMask(false);

        GLES30.glUseProgram(program);
        GLES30.glUniformMatrix4fv(projUniform, 1, false, projection, 0);
        GLES30.glUniformMatrix4fv(viewUniform, 1, false, view, 0);
        GLES30.glUniform2f(carXYUniform, carX, carY);
        GLES30.glUniform1f(nightModeUniform, nightMode ? 1.0f : 0.0f);

        GLES30.glBindVertexArray(vao);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 6);
        GLES30.glBindVertexArray(0);

        // Restore default depth-write and blending state
        GLES30.glDepthMask(true);
        GLES30.glDisable(GLES30.GL_BLEND);
    }

    // -------------------------------------------------------------------------
    // Setup helpers
    // -------------------------------------------------------------------------

    private void setupShaders() {
        int vert = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int frag = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vert);
        GLES30.glAttachShader(program, frag);
        GLES30.glLinkProgram(program);

        int[] status = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            android.util.Log.e("GroundRenderer", "Link error: " + GLES30.glGetProgramInfoLog(program));
            GLES30.glDeleteProgram(program);
            program = 0;
        }

        GLES30.glDeleteShader(vert);
        GLES30.glDeleteShader(frag);

        if (program != 0) {
            projUniform  = GLES30.glGetUniformLocation(program, "uProjection");
            viewUniform  = GLES30.glGetUniformLocation(program, "uView");
            carXYUniform = GLES30.glGetUniformLocation(program, "uCarXY");
            nightModeUniform = GLES30.glGetUniformLocation(program, "uNightMode");
        }
    }

    private int compileShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);

        int[] status = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            android.util.Log.e("GroundRenderer", "Compile error: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private void setupMesh() {
        // Large flat quad (500 world-unit half-extent) centred on car XZ.
        // Vertices are XZ pairs; Y is always 0.0 (set in vertex shader).
        float s = 500f;
        float[] verts = {
            -s, -s,
             s, -s,
             s,  s,
            -s, -s,
             s,  s,
            -s,  s,
        };

        int[] vaos = new int[1];
        GLES30.glGenVertexArrays(1, vaos, 0);
        vao = vaos[0];
        GLES30.glBindVertexArray(vao);

        int[] vbos = new int[1];
        GLES30.glGenBuffers(1, vbos, 0);
        vbo = vbos[0];
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);

        ByteBuffer bb = ByteBuffer.allocateDirect(verts.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(verts);
        fb.position(0);

        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, verts.length * 4, fb, GLES30.GL_STATIC_DRAW);

        // attribute 0: vec2 aXZ
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 8, 0);

        GLES30.glBindVertexArray(0);
    }
}
