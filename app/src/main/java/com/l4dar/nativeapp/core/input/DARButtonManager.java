package com.l4dar.nativeapp.core.input;

import com.l4dar.nativeapp.core.settings.SettingsManager;

/**
 * Manages directional air roll (DAR) button state with toggle vs. hold modes.
 * Implements state machine for DAR activation tracking.
 */
public class DARButtonManager {
    public static final int NONE = 0;
    public static final int LEFT = -1;
    public static final int RIGHT = 1;
    public static final int FREE = 2;

    private final SettingsManager settings;
    
    private int darState = NONE;
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean touchPressed = false;
    private int touchDarState = NONE;

    public DARButtonManager(SettingsManager settings) {
        this.settings = settings;
    }

    public void onLeftPressed() {
        if (!leftPressed) {
            leftPressed = true;
            handleDARPress(LEFT);
        }
    }

    public void onLeftReleased() {
        leftPressed = false;
        if (!shouldUseToggleMode()) {
            darState = rightPressed ? RIGHT : NONE;
        }
    }

    public void onRightPressed() {
        if (!rightPressed) {
            rightPressed = true;
            handleDARPress(RIGHT);
        }
    }

    public void onRightReleased() {
        rightPressed = false;
        if (!shouldUseToggleMode()) {
            darState = leftPressed ? LEFT : NONE;
        }
    }

    public void onTouchButtonPressed() {
        if (touchPressed) {
            return;
        }
        touchPressed = true;
        int selectedMode = settings.getTouchDarDirection();
        int direction = selectedMode == 0 ? FREE : selectedMode;
        if (shouldUseToggleMode()) {
            touchDarState = touchDarState == direction ? NONE : direction;
        } else {
            touchDarState = direction;
        }
    }

    public void onTouchButtonReleased() {
        touchPressed = false;
        if (!shouldUseToggleMode()) {
            touchDarState = NONE;
        }
    }

    private void handleDARPress(int direction) {
        if (shouldUseToggleMode()) {
            // Toggle mode: pressing same direction again toggles off
            if (darState == direction) {
                darState = NONE;
            } else {
                darState = direction;
            }
        } else {
            // Hold mode: press activates
            darState = direction;
        }
    }

    public int getDARState() {
        return darState;
    }

    public int getTouchDARState() {
        if (shouldUseToggleMode()) {
            return touchDarState;
        }
        return touchPressed ? touchDarState : NONE;
    }

    public boolean isDAROn() {
        return darState != NONE;
    }

    public boolean isTouchDAROn() {
        if (shouldUseToggleMode()) {
            return touchDarState != NONE;
        }
        return touchPressed && touchDarState != NONE;
    }

    public int getDARDirection() {
        return darState;
    }

    /**
     * Called when settings change to sync toggle mode.
     */
    public void updateFromSettings() {
        // Toggle/hold mode is read live from settings in press/release handlers.
    }

    private boolean shouldUseToggleMode() {
        return settings.getAirRollIsToggle();
    }

    public void setSaveState(int state) {
        darState = state;
        touchPressed = false;
        touchDarState = NONE;
    }

    public int getSaveState() {
        return darState;
    }
}
