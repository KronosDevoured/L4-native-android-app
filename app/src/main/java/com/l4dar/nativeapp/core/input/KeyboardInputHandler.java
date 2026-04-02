package com.l4dar.nativeapp.core.input;

import android.view.KeyEvent;

import com.l4dar.nativeapp.core.settings.SettingsManager;

import java.util.Objects;

/**
 * Keyboard input mapping for desktop/emulator control.
 * Left stick: WASD / arrow keys
 * DAR: Q (left), E (right)
 */
public final class KeyboardInputHandler {
    private final SettingsManager settings;
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean upPressed;
    private boolean downPressed;

    private boolean darLeftPressed;
    private boolean darRightPressed;

    public KeyboardInputHandler(SettingsManager settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public boolean onKeyDown(int keyCode) {
        boolean toggleMode = settings.getAirRollIsToggle();

        switch (keyCode) {
            case KeyEvent.KEYCODE_A:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                leftPressed = true;
                return true;
            case KeyEvent.KEYCODE_D:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                rightPressed = true;
                return true;
            case KeyEvent.KEYCODE_W:
            case KeyEvent.KEYCODE_DPAD_UP:
                upPressed = true;
                return true;
            case KeyEvent.KEYCODE_S:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                downPressed = true;
                return true;
            case KeyEvent.KEYCODE_Q:
                if (toggleMode) {
                    boolean next = !(darLeftPressed && !darRightPressed);
                    darLeftPressed = next;
                    darRightPressed = false;
                } else {
                    darLeftPressed = true;
                }
                return true;
            case KeyEvent.KEYCODE_E:
                if (toggleMode) {
                    boolean next = !(darRightPressed && !darLeftPressed);
                    darRightPressed = next;
                    darLeftPressed = false;
                } else {
                    darRightPressed = true;
                }
                return true;
            default:
                return false;
        }
    }

    public boolean onKeyUp(int keyCode) {
        boolean toggleMode = settings.getAirRollIsToggle();

        switch (keyCode) {
            case KeyEvent.KEYCODE_A:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                leftPressed = false;
                return true;
            case KeyEvent.KEYCODE_D:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                rightPressed = false;
                return true;
            case KeyEvent.KEYCODE_W:
            case KeyEvent.KEYCODE_DPAD_UP:
                upPressed = false;
                return true;
            case KeyEvent.KEYCODE_S:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                downPressed = false;
                return true;
            case KeyEvent.KEYCODE_Q:
                if (!toggleMode) {
                    darLeftPressed = false;
                }
                return true;
            case KeyEvent.KEYCODE_E:
                if (!toggleMode) {
                    darRightPressed = false;
                }
                return true;
            default:
                return false;
        }
    }

    public float getLeftStickX() {
        float x = 0f;
        if (leftPressed) x -= 1f;
        if (rightPressed) x += 1f;
        return x;
    }

    public float getLeftStickY() {
        float y = 0f;
        if (upPressed) y += 1f;
        if (downPressed) y -= 1f;
        return y;
    }

    public float getRightStickX() {
        return 0f;
    }

    public float getRightStickY() {
        return 0f;
    }

    public float getThrottle() {
        float t = 0f;
        if (upPressed) t += 1f;
        if (downPressed) t -= 1f;
        return t;
    }

    public int getDARState() {
        if (darLeftPressed && !darRightPressed) return DARButtonManager.LEFT;
        if (darRightPressed && !darLeftPressed) return DARButtonManager.RIGHT;
        return DARButtonManager.NONE;
    }

    public float getDARIntensity() {
        return getDARState() == DARButtonManager.NONE ? 0f : 1f;
    }

    public void setDARState(int state) {
        darLeftPressed = state == DARButtonManager.LEFT;
        darRightPressed = state == DARButtonManager.RIGHT;
    }
}
