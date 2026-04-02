package com.l4dar.nativeapp.render;

import android.opengl.GLES30;
import android.opengl.Matrix;
import android.graphics.PointF;

import com.l4dar.nativeapp.core.input.DARButtonManager;
import com.l4dar.nativeapp.core.input.InputController;
import com.l4dar.nativeapp.core.input.TouchJoystick;

/**
 * Renders touch joystick overlays on top of 3D scene using OpenGL.
 * Draws left and right joystick UI at bottom of screen.
 */
public final class JoystickOverlayRenderer {
    private static final String VERTEX_SHADER =
        "#version 300 es\n" +
        "layout(location = 0) in vec2 aPosition;\n" +
        "uniform mat4 uProjection;\n" +
        "uniform mat4 uModel;\n" +
        "void main() {\n" +
        "  gl_Position = uProjection * uModel * vec4(aPosition, 0.0, 1.0);\n" +
        "}\n";

    private static final String FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "uniform vec4 uColor;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        "  fragColor = uColor;\n" +
        "}\n";

    private int program;
    private int positionAttrib;
    private int projectionUniform;
    private int modelUniform;
    private int colorUniform;

    private int circleVao;
    private int circleVbo;
    private int circleVertexCount;

    public JoystickOverlayRenderer() {
        // Don't initialize here - wait for GL context in onSurfaceCreated
    }

    public void initialize() {
        if (program != 0 && circleVao != 0) {
            return;
        }
        setupShaders();
        if (program != 0) {
            setupCircleMesh();
        }
    }

    private void setupShaders() {
        int vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        if (vertexShader == 0 || fragmentShader == 0) {
            if (vertexShader != 0) GLES30.glDeleteShader(vertexShader);
            if (fragmentShader != 0) GLES30.glDeleteShader(fragmentShader);
            program = 0;
            return;
        }

        program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);
        GLES30.glLinkProgram(program);

        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            GLES30.glDeleteProgram(program);
            program = 0;
            GLES30.glDeleteShader(vertexShader);
            GLES30.glDeleteShader(fragmentShader);
            return;
        }

        positionAttrib = GLES30.glGetAttribLocation(program, "aPosition");
        projectionUniform = GLES30.glGetUniformLocation(program, "uProjection");
        modelUniform = GLES30.glGetUniformLocation(program, "uModel");
        colorUniform = GLES30.glGetUniformLocation(program, "uColor");

        GLES30.glDeleteShader(vertexShader);
        GLES30.glDeleteShader(fragmentShader);
    }

    private int compileShader(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);

        int[] compileStatus = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            GLES30.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private void setupCircleMesh() {
        // Create circle geometry for joystick base and stick
        int segments = 32;
        float[] vertices = new float[(segments + 2) * 2];
        
        // Center
        vertices[0] = 0;
        vertices[1] = 0;
        
        // Circle points
        for (int i = 0; i <= segments; i++) {
            float angle = 2.0f * (float) Math.PI * i / segments;
            vertices[(i + 1) * 2] = (float) Math.cos(angle);
            vertices[(i + 1) * 2 + 1] = (float) Math.sin(angle);
        }

        circleVertexCount = segments + 2;

        java.nio.ByteBuffer vertexBuffer = java.nio.ByteBuffer.allocateDirect(vertices.length * 4);
        vertexBuffer.order(java.nio.ByteOrder.nativeOrder());
        java.nio.FloatBuffer floatBuffer = vertexBuffer.asFloatBuffer();
        floatBuffer.put(vertices);
        floatBuffer.position(0);

        int[] vaos = new int[1];
        GLES30.glGenVertexArrays(1, vaos, 0);
        circleVao = vaos[0];
        GLES30.glBindVertexArray(circleVao);

        int[] vbos = new int[1];
        GLES30.glGenBuffers(1, vbos, 0);
        circleVbo = vbos[0];
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, circleVbo);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, floatBuffer.capacity() * 4, 
            floatBuffer, GLES30.GL_STATIC_DRAW);

        // Use the program before setting vertex attributes
        GLES30.glUseProgram(program);
        
        GLES30.glEnableVertexAttribArray(positionAttrib);
        GLES30.glVertexAttribPointer(positionAttrib, 2, GLES30.GL_FLOAT, false, 8, 0);
    }

    public void render(InputController inputController, int screenWidth, int screenHeight) {
        if (program == 0 || circleVao == 0) {
            return;
        }
        GLES30.glUseProgram(program);

        // Match Android view coordinates so top-left positioned controls render in the same place.
        float[] projection = new float[16];
        Matrix.orthoM(projection, 0, 0f, (float) screenWidth, (float) screenHeight, 0f, -1f, 1f);
        GLES30.glUniformMatrix4fv(projectionUniform, 1, false, projection, 0);

        // Disable depth test and face culling for the 2-D overlay pass.
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glDisable(GLES30.GL_CULL_FACE);
        
        // Enable alpha blending for transparency
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Left joystick
        drawJoystick(inputController.getLeftJoystick());

        // Dedicated DAR touch buttons (share state machine with gamepad/keyboard modes).
        drawDARButtons(inputController);

        // Restore state for next 3D render
        GLES30.glDisable(GLES30.GL_BLEND);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }

    private void drawJoystick(TouchJoystick joystick) {
        PointF base = joystick.getBaseCenter();
        PointF stick = joystick.getStickPosition();
        if (base == null || stick == null) {
            return;
        }

        // Draw base circle (translucent darker)
        drawCircle(base.x, base.y, joystick.getBaseRadius(), 0.5f, 0.5f, 0.5f, 0.8f);

        // Draw stick circle (bright and opaque)
        drawCircle(stick.x, stick.y, 40f, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void drawDARButtons(InputController inputController) {
        PointF center = inputController.getDARButtonCenter();
        float radius = inputController.getDARButtonRadius();
        int darState = inputController.getTouchDARState();

        if (darState != DARButtonManager.NONE) {
            drawCircle(center.x, center.y, radius, 0.0f, 1.0f, 1.0f, 1.0f);  // Cyan when active
        } else {
            drawCircle(center.x, center.y, radius, 0.5f, 0.5f, 0.5f, 0.9f);  // Gray when inactive
        }
    }

    private void drawCircle(float x, float y, float radius, float r, float g, float b, float a) {
        float[] model = new float[16];
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, x, y, 0);
        Matrix.scaleM(model, 0, radius, radius, 1);

        GLES30.glUniformMatrix4fv(modelUniform, 1, false, model, 0);
        GLES30.glUniform4f(colorUniform, r, g, b, a);
        GLES30.glBindVertexArray(circleVao);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, circleVertexCount);
    }

    public void cleanup() {
        if (circleVao != 0) {
            int[] vaos = {circleVao};
            GLES30.glDeleteVertexArrays(1, vaos, 0);
            circleVao = 0;
        }
        if (circleVbo != 0) {
            int[] vbos = {circleVbo};
            GLES30.glDeleteBuffers(1, vbos, 0);
            circleVbo = 0;
        }
        if (program != 0) {
            GLES30.glDeleteProgram(program);
            program = 0;
        }
    }
}
