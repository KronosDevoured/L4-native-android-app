package com.l4dar.nativeapp.core.input;

import android.view.KeyEvent;
import android.view.MotionEvent;

import com.l4dar.nativeapp.core.settings.SettingsManager;

/**
 * Gamepad input handling for L4 DAR.
 * Polls gamepad/joystick state and converts to normalized input values.
 * Supports standard Android gamepad layout.
 */
public final class GamepadInputHandler {
    private final SettingsManager settings;

    private float leftStickX = 0f;
    private float leftStickY = 0f;
    private float rightStickX = 0f;
    private float rightStickY = 0f;
    private float throttle = 0f;

    private int darState = DARButtonManager.NONE;
    private float darIntensity = 0f;

    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean freePressed = false;
    private boolean freeAxisPressed = false;
    private boolean freeAxisLastActive = false;
    private boolean toggleDarPressed = false;

    public GamepadInputHandler(SettingsManager settings) {
        this.settings = settings;
    }

    private float getLeftStickDeadzone() {
        return settings.getGpLeftStickDeadzone();
    }

    private float getRightStickDeadzone() {
        return settings.getGpRightStickDeadzone();
    }

    private float getTriggerDeadzone() {
        return Math.max(getLeftStickDeadzone(), getRightStickDeadzone());
    }

    /**
     * Source-backed hold-mode priority from the web input orchestration:
     * rollLeft > rollRight > rollFree.
     * This keeps simultaneous DAR button presses deterministic and independent of press order.
     */
    private void applyHoldModeDarState() {
        if (leftPressed) {
            darState = DARButtonManager.LEFT;
        } else if (rightPressed) {
            darState = DARButtonManager.RIGHT;
        } else if (isFreePressed()) {
            darState = DARButtonManager.FREE;
        } else {
            darState = DARButtonManager.NONE;
        }
        darIntensity = (darState == DARButtonManager.NONE) ? 0f : 1f;
    }

    private boolean isFreePressed() {
        return freePressed || freeAxisPressed;
    }

    private int getRollFreeTriggerAxis() {
        int freeBinding = settings.getGpBindRollFree();
        if (freeBinding == KeyEvent.KEYCODE_BUTTON_R2) {
            return MotionEvent.AXIS_RTRIGGER;
        }
        if (freeBinding == KeyEvent.KEYCODE_BUTTON_L2) {
            return MotionEvent.AXIS_LTRIGGER;
        }
        return -1;
    }

    private void updateRollFreeFromTriggerAxis(MotionEvent event) {
        int axis = getRollFreeTriggerAxis();
        if (axis < 0) {
            freeAxisPressed = false;
            freeAxisLastActive = false;
            return;
        }

        float axisValue = getPositiveAxis(event, axis, getTriggerDeadzone());
        boolean axisActive = axisValue > 0f;
        boolean toggleMode = settings.getAirRollIsToggle();

        if (toggleMode) {
            if (axisActive && !freeAxisLastActive) {
                darState = (darState == DARButtonManager.FREE) ? DARButtonManager.NONE : DARButtonManager.FREE;
                darIntensity = (darState == DARButtonManager.NONE) ? 0f : 1f;
            }
            freeAxisPressed = axisActive;
            freeAxisLastActive = axisActive;
        } else {
            freeAxisPressed = axisActive;
            freeAxisLastActive = axisActive;
            applyHoldModeDarState();
        }
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        if (event.getDevice() == null) {
            return false;
        }

        // Left stick (bindable) with source-aligned per-stick deadzone handling.
        float lx = getCenteredAxis(event, settings.getGpBindSteerX());
        float ly = getCenteredAxis(event, settings.getGpBindSteerY());
        float leftMagnitude = (float) Math.hypot(lx, ly);
        if (leftMagnitude > getLeftStickDeadzone()) {
            leftStickX = lx;
            leftStickY = -ly; // Invert Y so up = positive
        } else {
            leftStickX = 0f;
            leftStickY = 0f;
        }

        // Right stick (bindable) with source-aligned per-stick deadzone handling.
        float rx = getCenteredAxis(event, settings.getGpBindCameraX());
        float ry = getCenteredAxis(event, settings.getGpBindCameraY());
        float rightMagnitude = (float) Math.hypot(rx, ry);
        if (rightMagnitude > getRightStickDeadzone()) {
            rightStickX = rx;
            rightStickY = -ry;
        } else {
            rightStickX = 0f;
            rightStickY = 0f;
        }

        // Triggers map to throttle in [-1, 1]: forward axis minus reverse axis.
        float rt = getPositiveAxis(event, settings.getGpBindThrottleForward(), getTriggerDeadzone());
        float lt = getPositiveAxis(event, settings.getGpBindThrottleReverse(), getTriggerDeadzone());
        throttle = clamp(rt - lt, -1f, 1f);

        // Some controllers surface trigger-style FAR input primarily via motion axes.
        updateRollFreeFromTriggerAxis(event);

        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == settings.getGpBindToggleDar()) {
            if (!toggleDarPressed) {
                toggleDarPressed = true;
                boolean newToggleMode = !settings.getAirRollIsToggle();
                settings.setAirRollIsToggle(newToggleMode);
                if (!newToggleMode) {
                    applyHoldModeDarState();
                }
            }
            return true;
        }

        boolean toggleMode = settings.getAirRollIsToggle();

        if (keyCode == settings.getGpBindAirRollLeft()) {
            if (leftPressed) {
                return true;
            }
            leftPressed = true;
            if (toggleMode) {
                darState = (darState == DARButtonManager.LEFT) ? DARButtonManager.NONE : DARButtonManager.LEFT;
                darIntensity = (darState == DARButtonManager.NONE) ? 0f : 1f;
            } else {
                applyHoldModeDarState();
            }
            return true;
        } else if (keyCode == settings.getGpBindAirRollRight()) {
            if (rightPressed) {
                return true;
            }
            rightPressed = true;
            if (toggleMode) {
                darState = (darState == DARButtonManager.RIGHT) ? DARButtonManager.NONE : DARButtonManager.RIGHT;
                darIntensity = (darState == DARButtonManager.NONE) ? 0f : 1f;
            } else {
                applyHoldModeDarState();
            }
            return true;
        } else if (keyCode == settings.getGpBindRollFree()) {
            if (isFreePressed()) {
                return true;
            }
            freePressed = true;
            if (toggleMode) {
                darState = (darState == DARButtonManager.FREE) ? DARButtonManager.NONE : DARButtonManager.FREE;
                darIntensity = (darState == DARButtonManager.NONE) ? 0f : 1f;
            } else {
                applyHoldModeDarState();
            }
            return true;
        }
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == settings.getGpBindToggleDar()) {
            toggleDarPressed = false;
            return true;
        }

        boolean toggleMode = settings.getAirRollIsToggle();

        if (keyCode == settings.getGpBindAirRollLeft()) {
            leftPressed = false;
            if (!toggleMode) {
                applyHoldModeDarState();
            }
            return true;
        } else if (keyCode == settings.getGpBindAirRollRight()) {
            rightPressed = false;
            if (!toggleMode) {
                applyHoldModeDarState();
            }
            return true;
        } else if (keyCode == settings.getGpBindRollFree()) {
            freePressed = false;
            if (!toggleMode) {
                applyHoldModeDarState();
            }
            return true;
        }
        return false;
    }

    private float getCenteredAxis(MotionEvent event, int axis) {
        android.view.InputDevice.MotionRange motionRange = event.getDevice().getMotionRange(axis, event.getSource());
        if (motionRange == null) {
            return 0f;
        }

        float min = motionRange.getMin();
        float max = motionRange.getMax();
        float center = (min + max) * 0.5f;
        float halfRange = (max - min) * 0.5f;
        float flat = motionRange.getFlat();
        float centeredValue = event.getAxisValue(axis) - center;

        if (halfRange <= flat) {
            return 0f;
        }

        // Apply deadzone around center and normalize to [-1, 1].
        if (Math.abs(centeredValue) > flat) {
            float normalized = (centeredValue - (centeredValue > 0f ? flat : -flat)) / (halfRange - flat);
            return clamp(normalized, -1f, 1f);
        }
        return 0f;
    }

    private float getPositiveAxis(MotionEvent event, int axis, float deadzone) {
        float value = event.getAxisValue(axis);
        if (value < deadzone) {
            return 0f;
        }
        return clamp((value - deadzone) / (1f - deadzone), 0f, 1f);
    }

    public float getLeftStickX() {
        return leftStickX;
    }

    public float getLeftStickY() {
        return leftStickY;
    }

    public float getRightStickX() {
        return rightStickX;
    }

    public float getRightStickY() {
        return rightStickY;
    }

    public int getDARState() {
        return darState;
    }

    public float getThrottle() {
        return throttle;
    }

    public float getDARIntensity() {
        return darIntensity;
    }

    public void setDARState(int state) {
        darState = state;
        darIntensity = (state == DARButtonManager.NONE) ? 0f : 1f;
    }

    public void reset() {
        leftStickX = 0f;
        leftStickY = 0f;
        rightStickX = 0f;
        rightStickY = 0f;
        throttle = 0f;
        darState = DARButtonManager.NONE;
        darIntensity = 0f;
        leftPressed = false;
        rightPressed = false;
        freePressed = false;
        freeAxisPressed = false;
        freeAxisLastActive = false;
        toggleDarPressed = false;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
