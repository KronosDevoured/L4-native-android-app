package com.l4dar.nativeapp.core.input;

public final class InputSnapshot {
    public final float joyPixelsX;
    public final float joyPixelsY;
    public final float joyBaseRadius;
    public final float rightStickX;
    public final float rightStickY;
    public final float throttle;
    public final int airRoll;
    public final float airRollIntensity;
    public final boolean darOn;

    public InputSnapshot(float joyPixelsX, float joyPixelsY, float joyBaseRadius,
                         float rightStickX, float rightStickY, float throttle,
                         int airRoll, float airRollIntensity, boolean darOn) {
        this.joyPixelsX = joyPixelsX;
        this.joyPixelsY = joyPixelsY;
        this.joyBaseRadius = joyBaseRadius;
        this.rightStickX = rightStickX;
        this.rightStickY = rightStickY;
        this.throttle = throttle;
        this.airRoll = airRoll;
        this.airRollIntensity = airRollIntensity;
        this.darOn = darOn;
    }
}
