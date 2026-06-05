package com.l4dar.nativeapp.render;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.WindowManager;

import com.l4dar.nativeapp.core.input.InputController;
import com.l4dar.nativeapp.core.input.InputSnapshot;
import com.l4dar.nativeapp.core.math.Quat;
import com.l4dar.nativeapp.core.math.Vec3;
import com.l4dar.nativeapp.core.physics.L4PhysicsEngine;
import com.l4dar.nativeapp.core.settings.SettingsManager;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public final class L4Renderer implements GLSurfaceView.Renderer {
    private static final float[] DAY_CLEAR_COLOR = new float[] {0.93f, 0.95f, 0.98f, 1.0f};
    private static final float[] NIGHT_CLEAR_COLOR = new float[] {0.01f, 0.01f, 0.015f, 1.0f};

    private final Context context;
    private final SettingsManager settingsManager;
    private final InputController inputController;
    private final L4PhysicsEngine physicsEngine;
    private final CarMeshRenderer carMeshRenderer;
    private final GroundRenderer groundRenderer;
    private final JoystickOverlayRenderer joystickRenderer;
    private final ConcurrentLinkedQueue<MotionEvent> pendingTouches = new ConcurrentLinkedQueue<>();
    
    private int screenWidth = 0;
    private int screenHeight = 0;
    
    // Timing
    private long lastFrameTimeNs = 0;
    private static final float TARGET_DT = 1f / 60f; // 60 Hz default (if no prior frame)
    
    // Car state
    private final Quat carQuaternion;

    // Camera look state (right-stick driven)
    private float cameraYaw = (float) Math.PI * 0.5f;
    private float cameraPitch = 0.25f;   // slightly above horizon for a clear ground view
    private static final float CAMERA_LOOK_SPEED = 1.75f;
    private static final float CAMERA_RADIUS    = 3.0f;

    // Per-frame shared matrices (allocated once, reused every frame)
    private final float[] projection = new float[16];
    private final float[] view       = new float[16];

    private final float displayDensity;

    public L4Renderer(Context context, SettingsManager settingsManager, float displayDensity) {
        this.context = context;
        this.settingsManager = settingsManager;
        this.displayDensity = displayDensity;
        this.inputController = new InputController(settingsManager);
        this.physicsEngine = new L4PhysicsEngine(settingsManager);
        this.carMeshRenderer = new CarMeshRenderer(context, settingsManager.getSelectedCarBody());
        this.groundRenderer = new GroundRenderer();
        this.joystickRenderer = new JoystickOverlayRenderer();
        this.carQuaternion = new Quat();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        applyClearColor();
        
        // Enable depth testing for 3D rendering
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glDepthFunc(GLES30.GL_LEQUAL);
        
        // Enable back-face culling
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glCullFace(GLES30.GL_BACK);
        
        // Clean up old GL resources (handles context loss) then reinitialize
        carMeshRenderer.cleanup();
        carMeshRenderer.initialize();
        groundRenderer.cleanup();
        groundRenderer.initialize();
        joystickRenderer.cleanup();
        joystickRenderer.initialize();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES30.glViewport(0, 0, width, height);
        this.screenWidth = width;
        this.screenHeight = height;
        inputController.setDisplayRotation(getCurrentDisplayRotation());
        inputController.layoutTouchControls(width, height, displayDensity);
    }

    private int getCurrentDisplayRotation() {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null || wm.getDefaultDisplay() == null) {
            return Surface.ROTATION_0;
        }
        return wm.getDefaultDisplay().getRotation();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // GL-thread-safe clear-color application for day/night mode changes.
        applyClearColor();

        MotionEvent touch;
        while ((touch = pendingTouches.poll()) != null) {
            inputController.onTouchEvent(touch);
            touch.recycle();
        }
        
        // Calculate actual delta time to support variable refresh rates (60/90/120 Hz)
        // Physics advances at real elapsed time, preventing speed scaling on high-refresh displays
        long frameTimeNs = System.nanoTime();
        float deltaTime = TARGET_DT;
        if (lastFrameTimeNs > 0) {
            deltaTime = (frameTimeNs - lastFrameTimeNs) / 1e9f;
            // Clamp to reasonable bounds: min 8ms (125 Hz), max 33ms (30 Hz) to prevent physics instability
            deltaTime = Math.max(0.008f, Math.min(deltaTime, 0.033f));
        }
        lastFrameTimeNs = frameTimeNs;
        
        // Get input snapshot and perform physics step
        InputSnapshot input = inputController.getInputSnapshot();
        updateCameraLook(input, deltaTime);
        physicsEngine.performStep(input, deltaTime, carQuaternion);

        // Build shared projection and view matrices once per frame
        Vec3 carPosition = physicsEngine.getPosition();
        buildProjection(screenWidth, screenHeight);
        buildView(carPosition);

        // Render
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        // 1. Ground grid (vertical plane, rendered first; depth-write disabled inside renderer)
        groundRenderer.render(projection, view, carPosition.x, carPosition.y, settingsManager.isNightMode());

        // 2. Car mesh
        carMeshRenderer.render(carQuaternion, carPosition, projection, view);

        // 3. Touch controls overlay (2-D, rendered last)
        joystickRenderer.render(inputController, screenWidth, screenHeight);
    }

    private void buildProjection(int w, int h) {
        float aspect = (w == 0 || h == 0) ? 1f : (float) w / (float) h;
        Matrix.perspectiveM(projection, 0, 45f, aspect, 0.1f, 2000f);
    }

    private void buildView(Vec3 carPosition) {
        float cx = carPosition.x + (float) (Math.cos(cameraYaw) * Math.cos(cameraPitch)) * CAMERA_RADIUS;
        float cy = carPosition.y + (float)  Math.sin(cameraPitch)                         * CAMERA_RADIUS + 0.25f;
        float cz = carPosition.z + (float) (Math.sin(cameraYaw) * Math.cos(cameraPitch)) * CAMERA_RADIUS;
        Matrix.setLookAtM(view, 0,
            cx, cy, cz,
            carPosition.x, carPosition.y, carPosition.z,
            0f, 1f, 0f);
    }

    private void updateCameraLook(InputSnapshot input, float deltaTime) {
        if (!settingsManager.isGpRightStickSteerEnabled()) {
            return;
        }

        cameraYaw   += input.rightStickX * CAMERA_LOOK_SPEED * deltaTime;
        cameraPitch -= input.rightStickY * CAMERA_LOOK_SPEED * deltaTime;

        // Keep pitch within reasonable bounds so the camera never flips
        cameraPitch = Math.max(-1.2f, Math.min(1.2f, cameraPitch));
    }

    public void onTouchEvent(MotionEvent event) {
        pendingTouches.offer(MotionEvent.obtain(event));
    }

    public InputController getInputController() {
        return inputController;
    }

    public L4PhysicsEngine getPhysicsEngine() {
        return physicsEngine;
    }

    public Quat getCarQuaternion() {
        return carQuaternion;
    }

    public void setCarModel(String modelName) {
        carMeshRenderer.setModelName(modelName);
    }

    public void setNightMode(boolean nightMode) {
        settingsManager.setNightMode(nightMode);
    }

    private void applyClearColor() {
        float[] color = settingsManager.isNightMode() ? NIGHT_CLEAR_COLOR : DAY_CLEAR_COLOR;
        GLES30.glClearColor(color[0], color[1], color[2], color[3]);
    }
}
