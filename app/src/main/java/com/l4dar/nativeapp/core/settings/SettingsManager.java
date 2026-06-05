package com.l4dar.nativeapp.core.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.l4dar.nativeapp.core.physics.L4PhysicsDefaults;

public final class SettingsManager {
    private static final String PREF_FILE = "darSettings";

    private static final String KEY_MAX_ACCEL_PITCH = "maxAccelPitch";
    private static final String KEY_MAX_ACCEL_YAW = "maxAccelYaw";
    private static final String KEY_MAX_ACCEL_ROLL = "maxAccelRoll";
    private static final String KEY_DAMP = "damp";
    private static final String KEY_DAMP_DAR = "dampDAR";
    private static final String KEY_TOUCH_DEADZONE = "touchDeadzone";
    private static final String KEY_GAME_SPEED = "gameSpeed";
    private static final String KEY_SELECTED_CAR_BODY = "selectedCarBody";
    private static final String KEY_AIR_ROLL = "airRoll";
    private static final String KEY_DAR_TOGGLE = "airRollIsToggle";
    private static final String KEY_DAR_STATE = "darState";
    private static final String KEY_TOUCH_DAR_DIRECTION = "touchDarDirection";
    private static final String KEY_NIGHT_MODE = "nightMode";
    private static final String KEY_GP_LEFT_STICK_DEADZONE = "gpLeftStickDeadzone";
    private static final String KEY_GP_RIGHT_STICK_DEADZONE = "gpRightStickDeadzone";
    private static final String KEY_STICK_SIZE = "stickSize";

    // Bindable gamepad action keys
    private static final String KEY_GP_BIND_STEER_X = "gpBindSteerX";
    private static final String KEY_GP_BIND_STEER_Y = "gpBindSteerY";
    private static final String KEY_GP_BIND_CAMERA_X = "gpBindCameraX";
    private static final String KEY_GP_BIND_CAMERA_Y = "gpBindCameraY";
    private static final String KEY_GP_BIND_THROTTLE_FWD = "gpBindThrottleForward";
    private static final String KEY_GP_BIND_THROTTLE_REV = "gpBindThrottleReverse";
    private static final String KEY_GP_BIND_TOGGLE_DAR = "gpBindToggleDAR";
    private static final String KEY_GP_BIND_AIRROLL_LEFT = "gpBindAirRollLeft";
    private static final String KEY_GP_BIND_AIRROLL_RIGHT = "gpBindAirRollRight";
    private static final String KEY_GP_BIND_ROLL_FREE = "gpBindRollFree";
    private static final String KEY_GP_ALLOW_RIGHT_STICK_STEER = "gpAllowRightStickSteer";

    private final SharedPreferences prefs;

    private float maxAccelPitchDeg = L4PhysicsDefaults.ACCEL_PITCH_DEG;
    private float maxAccelYawDeg = L4PhysicsDefaults.ACCEL_YAW_DEG;
    private float maxAccelRollDeg = L4PhysicsDefaults.ACCEL_ROLL_DEG;
    private float damp = L4PhysicsDefaults.DAMP;
    private float dampDar = L4PhysicsDefaults.DAMP_DAR;
    private float touchDeadzone = 0.09f;
    private float gameSpeed = 1.0f;
    private String selectedCarBody = "octane";
    private int airRoll = 0;
    private boolean airRollIsToggle = false;
    private boolean nightMode = false;
    private int darState = 0; // 0 = OFF, -1 = LEFT, 1 = RIGHT, 2 = FREE
    private int touchDarDirection = -1;
    private float gpLeftStickDeadzone = 0.15f;
    private float gpRightStickDeadzone = 0.15f;
    private int stickSize = 100;

    // Gamepad binding defaults aligned to authoritative controller ownership.
    private int gpBindSteerX = MotionEvent.AXIS_X;
    private int gpBindSteerY = MotionEvent.AXIS_Y;
    private int gpBindCameraX = MotionEvent.AXIS_Z;
    private int gpBindCameraY = MotionEvent.AXIS_RZ;
    private int gpBindThrottleForward = MotionEvent.AXIS_RTRIGGER;
    private int gpBindThrottleReverse = MotionEvent.AXIS_LTRIGGER;
    private int gpBindToggleDar = KeyEvent.KEYCODE_BUTTON_A;
    private int gpBindAirRollLeft = KeyEvent.KEYCODE_BUTTON_X;
    private int gpBindAirRollRight = KeyEvent.KEYCODE_BUTTON_B;
    private int gpBindRollFree = KeyEvent.KEYCODE_BUTTON_L1;
    private boolean gpAllowRightStickSteer = false;

    public SettingsManager(Context context) {
        prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        load();
    }

    public void load() {
        maxAccelPitchDeg = prefs.getFloat(KEY_MAX_ACCEL_PITCH, L4PhysicsDefaults.ACCEL_PITCH_DEG);
        maxAccelYawDeg = prefs.getFloat(KEY_MAX_ACCEL_YAW, L4PhysicsDefaults.ACCEL_YAW_DEG);
        maxAccelRollDeg = prefs.getFloat(KEY_MAX_ACCEL_ROLL, L4PhysicsDefaults.ACCEL_ROLL_DEG);
        damp = prefs.getFloat(KEY_DAMP, L4PhysicsDefaults.DAMP);
        dampDar = prefs.getFloat(KEY_DAMP_DAR, L4PhysicsDefaults.DAMP_DAR);
        touchDeadzone = clamp(prefs.getFloat(KEY_TOUCH_DEADZONE, 0.09f), 0.0f, 0.5f);
        gameSpeed = clamp(prefs.getFloat(KEY_GAME_SPEED, 1.0f), 0.05f, 1.5f);
        String loadedCarBody = prefs.getString(KEY_SELECTED_CAR_BODY, "octane");
        if ("octane".equals(loadedCarBody) || "fennec".equals(loadedCarBody) || "dominus".equals(loadedCarBody)) {
            selectedCarBody = loadedCarBody;
        } else {
            selectedCarBody = "octane";
        }
        airRoll = prefs.getInt(KEY_AIR_ROLL, 0);
        airRollIsToggle = prefs.getBoolean(KEY_DAR_TOGGLE, false);
        nightMode = prefs.getBoolean(KEY_NIGHT_MODE, false);
        gpLeftStickDeadzone = clamp(prefs.getFloat(KEY_GP_LEFT_STICK_DEADZONE, 0.15f), 0.0f, 0.5f);
        gpRightStickDeadzone = clamp(prefs.getFloat(KEY_GP_RIGHT_STICK_DEADZONE, 0.15f), 0.0f, 0.5f);
        stickSize = clampInt(prefs.getInt(KEY_STICK_SIZE, 100), 60, 180);
        darState = prefs.getInt(KEY_DAR_STATE, 0);
        int loadedTouchDarDirection = prefs.getInt(KEY_TOUCH_DAR_DIRECTION, -1);
        if (loadedTouchDarDirection < -1 || loadedTouchDarDirection > 1) {
            touchDarDirection = -1;
        } else {
            touchDarDirection = loadedTouchDarDirection;
        }

        gpBindSteerX = prefs.getInt(KEY_GP_BIND_STEER_X, MotionEvent.AXIS_X);
        gpBindSteerY = prefs.getInt(KEY_GP_BIND_STEER_Y, MotionEvent.AXIS_Y);
        gpBindCameraX = prefs.getInt(KEY_GP_BIND_CAMERA_X, MotionEvent.AXIS_Z);
        gpBindCameraY = prefs.getInt(KEY_GP_BIND_CAMERA_Y, MotionEvent.AXIS_RZ);
        gpBindThrottleForward = prefs.getInt(KEY_GP_BIND_THROTTLE_FWD, MotionEvent.AXIS_RTRIGGER);
        gpBindThrottleReverse = prefs.getInt(KEY_GP_BIND_THROTTLE_REV, MotionEvent.AXIS_LTRIGGER);
        gpBindToggleDar = prefs.getInt(KEY_GP_BIND_TOGGLE_DAR, KeyEvent.KEYCODE_BUTTON_A);
        gpBindAirRollLeft = prefs.getInt(KEY_GP_BIND_AIRROLL_LEFT, KeyEvent.KEYCODE_BUTTON_X);
        gpBindAirRollRight = prefs.getInt(KEY_GP_BIND_AIRROLL_RIGHT, KeyEvent.KEYCODE_BUTTON_B);
        gpBindRollFree = prefs.getInt(KEY_GP_BIND_ROLL_FREE, KeyEvent.KEYCODE_BUTTON_L1);
        gpAllowRightStickSteer = prefs.getBoolean(KEY_GP_ALLOW_RIGHT_STICK_STEER, false);
    }

    public void save() {
        prefs.edit()
            .putFloat(KEY_MAX_ACCEL_PITCH, maxAccelPitchDeg)
            .putFloat(KEY_MAX_ACCEL_YAW, maxAccelYawDeg)
            .putFloat(KEY_MAX_ACCEL_ROLL, maxAccelRollDeg)
            .putFloat(KEY_DAMP, damp)
            .putFloat(KEY_DAMP_DAR, dampDar)
            .putFloat(KEY_TOUCH_DEADZONE, touchDeadzone)
            .putFloat(KEY_GAME_SPEED, gameSpeed)
            .putString(KEY_SELECTED_CAR_BODY, selectedCarBody)
            .putInt(KEY_AIR_ROLL, airRoll)
            .putBoolean(KEY_DAR_TOGGLE, airRollIsToggle)
            .putBoolean(KEY_NIGHT_MODE, nightMode)
            .putFloat(KEY_GP_LEFT_STICK_DEADZONE, gpLeftStickDeadzone)
            .putFloat(KEY_GP_RIGHT_STICK_DEADZONE, gpRightStickDeadzone)
            .putInt(KEY_STICK_SIZE, stickSize)
            .putInt(KEY_DAR_STATE, darState)
            .putInt(KEY_TOUCH_DAR_DIRECTION, touchDarDirection)
            .putInt(KEY_GP_BIND_STEER_X, gpBindSteerX)
            .putInt(KEY_GP_BIND_STEER_Y, gpBindSteerY)
            .putInt(KEY_GP_BIND_CAMERA_X, gpBindCameraX)
            .putInt(KEY_GP_BIND_CAMERA_Y, gpBindCameraY)
            .putInt(KEY_GP_BIND_THROTTLE_FWD, gpBindThrottleForward)
            .putInt(KEY_GP_BIND_THROTTLE_REV, gpBindThrottleReverse)
            .putInt(KEY_GP_BIND_TOGGLE_DAR, gpBindToggleDar)
            .putInt(KEY_GP_BIND_AIRROLL_LEFT, gpBindAirRollLeft)
            .putInt(KEY_GP_BIND_AIRROLL_RIGHT, gpBindAirRollRight)
            .putInt(KEY_GP_BIND_ROLL_FREE, gpBindRollFree)
            .putBoolean(KEY_GP_ALLOW_RIGHT_STICK_STEER, gpAllowRightStickSteer)
            .apply();
    }

    public float getTouchDeadzone() {
        return touchDeadzone;
    }

    public void setTouchDeadzone(float value) {
        touchDeadzone = clamp(value, 0.0f, 0.5f);
    }

    public float getGameSpeed() {
        return gameSpeed;
    }

    public void setGameSpeed(float value) {
        gameSpeed = clamp(value, 0.05f, 1.5f);
    }

    public String getSelectedCarBody() {
        return selectedCarBody;
    }

    public void setSelectedCarBody(String value) {
        if ("octane".equals(value) || "fennec".equals(value) || "dominus".equals(value)) {
            selectedCarBody = value;
        }
    }

    public int getDARState() {
        return darState;
    }

    public void setDARState(int state) {
        this.darState = state;
    }

    public boolean getAirRollIsToggle() {
        return airRollIsToggle;
    }

    public void setAirRollIsToggle(boolean value) {
        this.airRollIsToggle = value;
    }

    public boolean isNightMode() {
        return nightMode;
    }

    public void setNightMode(boolean value) {
        this.nightMode = value;
    }

    public int getTouchDarDirection() {
        return touchDarDirection;
    }

    public void setTouchDarDirection(int direction) {
        this.touchDarDirection = Math.max(-1, Math.min(1, direction));
    }

    public int getGpBindSteerX() {
        return gpBindSteerX;
    }

    public int getGpBindSteerY() {
        return gpBindSteerY;
    }

    public int getGpBindCameraX() {
        return gpBindCameraX;
    }

    public int getGpBindCameraY() {
        return gpBindCameraY;
    }

    public int getGpBindThrottleForward() {
        return gpBindThrottleForward;
    }

    public int getGpBindThrottleReverse() {
        return gpBindThrottleReverse;
    }

    public float getGpLeftStickDeadzone() {
        return gpLeftStickDeadzone;
    }

    public void setGpLeftStickDeadzone(float value) {
        gpLeftStickDeadzone = clamp(value, 0.0f, 0.5f);
    }

    public float getGpRightStickDeadzone() {
        return gpRightStickDeadzone;
    }

    public void setGpRightStickDeadzone(float value) {
        gpRightStickDeadzone = clamp(value, 0.0f, 0.5f);
    }

    public int getStickSize() {
        return stickSize;
    }

    public void setStickSize(int value) {
        stickSize = clampInt(value, 60, 180);
    }

    public int getGpBindToggleDar() {
        return gpBindToggleDar;
    }

    public int getGpBindAirRollLeft() {
        return gpBindAirRollLeft;
    }

    public int getGpBindAirRollRight() {
        return gpBindAirRollRight;
    }

    public int getGpBindRollFree() {
        return gpBindRollFree;
    }

    public void setGpBindToggleDar(int value) {
        gpBindToggleDar = value;
    }

    public void setGpBindRollFree(int value) {
        gpBindRollFree = value;
    }

    public boolean isGpRightStickSteerEnabled() {
        return gpAllowRightStickSteer;
    }

    public void setGpRightStickSteerEnabled(boolean value) {
        gpAllowRightStickSteer = value;
    }

    public String getReservedGamepadBindingConflictSummary() {
        StringBuilder conflicts = new StringBuilder();
        appendReservedConflict(conflicts, "Toggle DAR", gpBindToggleDar);
        appendReservedConflict(conflicts, "Air Roll Left", gpBindAirRollLeft);
        appendReservedConflict(conflicts, "Air Roll Right", gpBindAirRollRight);
        appendReservedConflict(conflicts, "Air Roll Free", gpBindRollFree);
        return conflicts.toString();
    }

    private void appendReservedConflict(StringBuilder conflicts, String actionName, int keyCode) {
        if (keyCode != KeyEvent.KEYCODE_BACK) {
            return;
        }
        if (conflicts.length() > 0) {
            conflicts.append("; ");
        }
        conflicts.append(actionName).append(" uses reserved Back");
    }

    public void setGpBindingAxes(int steerX, int steerY, int cameraX, int cameraY, int throttleForward, int throttleReverse) {
        gpBindSteerX = steerX;
        gpBindSteerY = steerY;
        gpBindCameraX = cameraX;
        gpBindCameraY = cameraY;
        gpBindThrottleForward = throttleForward;
        gpBindThrottleReverse = throttleReverse;
    }

    public void setGpBindingButtons(int airRollLeft, int airRollRight) {
        gpBindAirRollLeft = airRollLeft;
        gpBindAirRollRight = airRollRight;
    }

    /**
     * Get a setting by name with a default fallback.
     * Used by physics engine and other modules to access settings dynamically.
     */
    public float getSetting(String key, float defaultValue) {
        switch (key) {
            case "maxAccelPitch": return maxAccelPitchDeg;
            case "maxAccelYaw": return maxAccelYawDeg;
            case "maxAccelRoll": return maxAccelRollDeg;
            case "damp": return damp;
            case "dampDar": return dampDar;
            default: return defaultValue;
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
