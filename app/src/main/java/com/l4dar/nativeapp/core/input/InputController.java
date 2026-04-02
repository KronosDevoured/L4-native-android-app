package com.l4dar.nativeapp.core.input;

import android.graphics.PointF;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;

import com.l4dar.nativeapp.core.settings.SettingsManager;

/**
 * High-level input orchestration: collects gamepad/keyboard/touch input
 * and produces InputSnapshot for physics consumption.
 */
public class InputController {
    private static final String TAG = "InputController";

    private final SettingsManager settings;
    private final TouchJoystick leftJoystick;
    private final TouchJoystick rightJoystick;
    private final DARButtonManager darButtonManager;
    private final GamepadInputHandler gamepadHandler;
    private final KeyboardInputHandler keyboardHandler;

    private final PointF darButtonCenter = new PointF();
    private float darButtonRadius = 56f;
    private int darPointerId = MotionEvent.INVALID_POINTER_ID;
    private int displayRotation = Surface.ROTATION_0;
    private int lastLoggedDarState = Integer.MIN_VALUE;
    private InputSource lastLoggedSource = null;

    private InputSource inputSource = InputSource.TOUCH; // Default

    public enum InputSource {
        TOUCH, GAMEPAD, KEYBOARD
    }

    public InputController(SettingsManager settings) {
        this.settings = settings;
        this.leftJoystick = new TouchJoystick(settings);
        this.rightJoystick = new TouchJoystick(settings);
        this.darButtonManager = new DARButtonManager(settings);
        this.gamepadHandler = new GamepadInputHandler(settings);
        this.keyboardHandler = new KeyboardInputHandler(settings);
    }

    /**
     * Called from L4SurfaceView in response to display size or rotation.
     * @param density display density (dp-to-px ratio) for size scaling.
     */
    public void layoutTouchControls(int screenWidth, int screenHeight, float density) {
        if (density <= 0f) {
            density = 1f;
        }

        // All sizes are in dp; multiply by density to get physical pixels.
        float margin = 120f * density;
        float innerPad = 80f * density;
        float baseRadius = 50f * density;

        leftJoystick.setPosition(margin + innerPad, screenHeight - margin);
        leftJoystick.setBaseRadius(baseRadius);

        // Single dedicated on-screen DAR button on the right side.
        darButtonRadius = 60f * density;  // Increased from 42f for visibility
        darButtonCenter.set(
            screenWidth - (margin + innerPad),
            screenHeight - margin
        );
    }

    public boolean onTouchEvent(MotionEvent event) {
        inputSource = InputSource.TOUCH;

        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);
        boolean darHandled = false;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                float x = event.getX(pointerIndex);
                float y = event.getY(pointerIndex);
                boolean inDarButton = isInDARButton(x, y, darButtonCenter);
                if (inDarButton) {
                    if (darPointerId == MotionEvent.INVALID_POINTER_ID) {
                        darPointerId = pointerId;
                        darButtonManager.onTouchButtonPressed();
                    }
                    // Always consume touches inside the DAR hit area so floating-stick
                    // relocation never steals them.
                    darHandled = true;
                } else if (!leftJoystick.isDragging() && !leftJoystick.contains(x, y)) {
                    // Floating-stick behavior: touching open space re-anchors the left stick.
                    leftJoystick.beginRelocatedDrag(pointerId, x, y);
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                if (pointerId == darPointerId) {
                    darPointerId = MotionEvent.INVALID_POINTER_ID;
                    darButtonManager.onTouchButtonReleased();
                    darHandled = true;
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                if (darPointerId != MotionEvent.INVALID_POINTER_ID) {
                    darPointerId = MotionEvent.INVALID_POINTER_ID;
                    darButtonManager.onTouchButtonReleased();
                    darHandled = true;
                }
                break;
            }
            default:
                break;
        }

        boolean isDownEvent = action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN;
        // If DAR consumed a down event, do not also start joystick handling from the same touch.
        // Still forward move/up/cancel so joystick state can clear reliably.
        boolean leftHandled = (!darHandled || !isDownEvent) && leftJoystick.onTouchEvent(event);
        return darHandled || leftHandled;
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        int source = event.getSource();
        boolean isGamepadSource = (source & android.view.InputDevice.SOURCE_GAMEPAD) != 0
                || (source & android.view.InputDevice.SOURCE_JOYSTICK) != 0;
        if (!isGamepadSource) {
            return false;
        }

        inputSource = InputSource.GAMEPAD;
        return gamepadHandler.onGenericMotionEvent(event);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((event.getSource() & android.view.InputDevice.SOURCE_GAMEPAD) != 0
                || (event.getSource() & android.view.InputDevice.SOURCE_JOYSTICK) != 0) {
            inputSource = InputSource.GAMEPAD;
            return gamepadHandler.onKeyDown(keyCode, event);
        }

        inputSource = InputSource.KEYBOARD;
        return keyboardHandler.onKeyDown(keyCode);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if ((event.getSource() & android.view.InputDevice.SOURCE_GAMEPAD) != 0
                || (event.getSource() & android.view.InputDevice.SOURCE_JOYSTICK) != 0) {
            return gamepadHandler.onKeyUp(keyCode, event);
        }

        return keyboardHandler.onKeyUp(keyCode);
    }

    /**
     * Produces a snapshot of current input state for physics consumption.
     */
    public InputSnapshot getInputSnapshot() {
        float inputX, inputY;
        float rightX, rightY;
        float throttle;
        int darState;
        float darIntensity;

        if (inputSource == InputSource.GAMEPAD) {
            // Poll gamepad state
            inputX = gamepadHandler.getLeftStickX();
            inputY = gamepadHandler.getLeftStickY();
            rightX = gamepadHandler.getRightStickX();
            rightY = gamepadHandler.getRightStickY();
            throttle = gamepadHandler.getThrottle();
            darState = gamepadHandler.getDARState();
            darIntensity = gamepadHandler.getDARIntensity();
        } else if (inputSource == InputSource.KEYBOARD) {
            inputX = keyboardHandler.getLeftStickX();
            inputY = keyboardHandler.getLeftStickY();
            rightX = keyboardHandler.getRightStickX();
            rightY = keyboardHandler.getRightStickY();
            throttle = keyboardHandler.getThrottle();
            darState = keyboardHandler.getDARState();
            darIntensity = keyboardHandler.getDARIntensity();
        } else {
            // TOUCH input
            // The on-screen joystick already reports screen-relative controls:
            //   X = left/right, Y = up/down (up positive).
            // Do not rotate these again for physics consumption, or pitch/yaw channels swap in landscape.
            inputX = leftJoystick.getNormalizedX();
            inputY = leftJoystick.getNormalizedY();
            rightX = 0f;
            rightY = 0f;
            // Touch throttle uses upward drag on the left joystick.
            throttle = Math.max(0f, inputY);
            darState = darButtonManager.getTouchDARState();
            darIntensity = darButtonManager.isTouchDAROn() ? 1f : 0f;
        }

        if (darState != lastLoggedDarState || inputSource != lastLoggedSource) {
            Log.d(TAG, "DAR/FAR state change: source=" + inputSource
                    + " airRoll=" + darState
                    + " intensity=" + darIntensity
                    + " freeActive=" + (darState == DARButtonManager.FREE));
            lastLoggedDarState = darState;
            lastLoggedSource = inputSource;
        }

        return new InputSnapshot(
            inputX, inputY,
            leftJoystick.getBaseRadius(),
            rightX, rightY, throttle,
            darState, darIntensity,
            darState != DARButtonManager.NONE
        );
    }

    public void drawTouchControls(android.graphics.Canvas canvas) {
        leftJoystick.draw(canvas);
        rightJoystick.draw(canvas);
    }

    public DARButtonManager getDARButtonManager() {
        return darButtonManager;
    }

    public TouchJoystick getLeftJoystick() {
        return leftJoystick;
    }

    public TouchJoystick getRightJoystick() {
        return rightJoystick;
    }

    public InputSource getCurrentInputSource() {
        return inputSource;
    }

    public PointF getDARButtonCenter() {
        return darButtonCenter;
    }

    public float getDARButtonRadius() {
        return darButtonRadius;
    }

    public int getTouchDARState() {
        return darButtonManager.getTouchDARState();
    }

    public GamepadInputHandler getGamepadHandler() {
        return gamepadHandler;
    }

    public void restoreDARState(int darState) {
        darButtonManager.setSaveState(darState);
        gamepadHandler.setDARState(darState);
        keyboardHandler.setDARState(darState);
    }

    public void setDisplayRotation(int rotation) {
        this.displayRotation = rotation;
    }

    private float[] remapTouchAxesForDisplay(float x, float y) {
        switch (displayRotation) {
            case Surface.ROTATION_90:
                return new float[] { y, -x };
            case Surface.ROTATION_270:
                return new float[] { -y, x };
            case Surface.ROTATION_180:
                return new float[] { -x, -y };
            case Surface.ROTATION_0:
            default:
                return new float[] { x, y };
        }
    }

    private boolean isInDARButton(float x, float y, PointF center) {
        float dx = x - center.x;
        float dy = y - center.y;
        return (dx * dx + dy * dy) <= (darButtonRadius * darButtonRadius);
    }
}
