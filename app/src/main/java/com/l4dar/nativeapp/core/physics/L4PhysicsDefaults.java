package com.l4dar.nativeapp.core.physics;

public final class L4PhysicsDefaults {
    // Source: docs/js/modules/constants.js PHYSICS_DEFAULTS
    public static final float ACCEL_PITCH_DEG = 733.0f;
    public static final float ACCEL_YAW_DEG = 528.0f;
    public static final float ACCEL_ROLL_DEG = 898.0f;

    public static final float INPUT_CURVE = 1.0f;
    public static final float STICK_RANGE = 1.0f;

    public static final float DAMP = 2.96f;
    public static final float DAMP_DAR = 4.35f;
    public static final float BRAKE_ON_RELEASE = 0.0f;

    public static final float W_MAX = 5.5f;
    public static final float W_MAX_PITCH = 5.5f;
    public static final float W_MAX_YAW = 5.5f;
    public static final float W_MAX_ROLL = 5.5f;

    public static final float DAR_ROLL_SPEED = 5.5f;

    private L4PhysicsDefaults() {
    }
}
