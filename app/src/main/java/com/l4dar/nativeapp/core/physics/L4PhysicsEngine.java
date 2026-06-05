package com.l4dar.nativeapp.core.physics;

import com.l4dar.nativeapp.core.input.InputSnapshot;
import com.l4dar.nativeapp.core.input.DARButtonManager;
import com.l4dar.nativeapp.core.math.Vec3;
import com.l4dar.nativeapp.core.math.Quat;
import com.l4dar.nativeapp.core.settings.SettingsManager;

/**
 * L4 Physics Engine - Handles angular velocity integration and quaternion updates.
 * Based on: docs/js/modules/physics.js
 *
 * PD control (KP=200 for all axes) drives angular velocity toward stick-derived
 * target velocities. Exponential damping applied only when all inputs are released.
 * During DAR (tornado spin), no damping is applied: the car spins freely.
 */
public final class L4PhysicsEngine {
    private final SettingsManager settingsManager;

    // Angular velocity (rad/s)
    private final Vec3 w = new Vec3(0, 0, 0);

    // World position – car COM locked to vertical grid (Z = 0)
    private final Vec3 position = new Vec3(0, 0, 0);

    // PD proportional gain – same value for every axis in every mode (matches web)
    private static final float KP = 200.0f;

    public L4PhysicsEngine(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    /**
     * Perform one physics step, matching docs/js/modules/physics.js updatePhysics().
     *
     * Key design decisions (ported from web):
     *  - KP=200 for every axis in every mode (DAR-specific KpPitchDAR etc. are defined
     *    in the web but never actually switched to during performStep).
     *  - DAR mode targets:  roll = airRoll * DAR_ROLL_SPEED * intensity (fixed ±5.5 rad/s)
     *                       pitch/yaw share a 50%-of-wMax budget (~2.75 rad/s each).
     *  - Damping is applied ONLY when noInput=true (no stick AND not DAR active).
     *    During DAR with centered stick the car keeps spinning – no decay.
     *  - Per-axis caps first, then global magnitude cap (DAR only).
     *  - Yaw sign: web uses ux=-jx (stick RIGHT → yaw in -Y direction).
     */
    public void performStep(InputSnapshot input, float deltaTime, Quat quaternion) {
        // Settings (angular accel limits stored in deg/s², convert here)
        float accelPitch = settingsManager.getSetting("maxAccelPitch", L4PhysicsDefaults.ACCEL_PITCH_DEG) * (float) Math.PI / 180f;
        float accelYaw   = settingsManager.getSetting("maxAccelYaw",   L4PhysicsDefaults.ACCEL_YAW_DEG)   * (float) Math.PI / 180f;
        float accelRoll  = settingsManager.getSetting("maxAccelRoll",  L4PhysicsDefaults.ACCEL_ROLL_DEG)  * (float) Math.PI / 180f;
        float damp           = settingsManager.getSetting("damp",    L4PhysicsDefaults.DAMP);
        float brakeOnRelease = L4PhysicsDefaults.BRAKE_ON_RELEASE;
        float adjustedDt     = deltaTime * settingsManager.getGameSpeed();

        // Raw stick values (-1 to 1). jx: right = positive; jy: up = positive.
        // Web updatePhysics() currently uses raw stick values directly.
        float jx = input.joyPixelsX;
        float jy = input.joyPixelsY;

        // Apply deadzone ramping identical to the web physics.js:
        //   eff = (clampedMag - deadzone) / (1 - deadzone)   (matches web stickDeadzone handling)
        //   wx_des = wMaxPitch * eff * jy   (eff scales the magnitude; jy is the raw direction component)
        float mag = Math.min(1f, (float) Math.sqrt(jx * jx + jy * jy));
        float stickDeadzone = settingsManager.getTouchDeadzone();
        float eff = 0f;
        float deadzoneDenominator = 1f - stickDeadzone;
        if (mag > stickDeadzone && deadzoneDenominator > 1e-6f) {
            eff = (mag - stickDeadzone) / deadzoneDenominator;
        }

        boolean hasStickInput = eff >= 0.02f;
        boolean isDARActive   = input.darOn;
        boolean isFreeAirRoll = isDARActive && input.airRoll == DARButtonManager.FREE;
        boolean isDirectionalDAR = isDARActive && !isFreeAirRoll;
        // "noInput": all physical inputs released and DAR not active → apply damping
        boolean noInput = !hasStickInput && !isDARActive;
        boolean farCenteredNoInput = isFreeAirRoll && !isDirectionalDAR && !hasStickInput;
        boolean effectiveNoInput = noInput || farCenteredNoInput;

        // --- 1. Compute target angular velocities ---
        // Native body axes are parity-mapped as:
        //   w.x => roll, w.y => yaw, w.z => pitch.
        // This preserves the web control intent:
        //   left/right stick -> yaw, up/down stick -> pitch.
        float wx_des, wy_des, wz_des;
        if (isDARActive) {
            // Tornado spin: pitch/yaw each capped at 50 % of wMax
            float darCap      = L4PhysicsDefaults.W_MAX * 0.50f;              // 2.75 rad/s
            float pitchCapDar = Math.min(L4PhysicsDefaults.W_MAX_PITCH, darCap);
            float yawCapDar   = Math.min(L4PhysicsDefaults.W_MAX_YAW,   darCap);

            float wx_raw;
            float wy_raw;
            float wz_raw;

            if (isFreeAirRoll) {
                // Final FREE model:
                //   horizontal stick swaps yaw to roll (yaw suppressed)
                //   vertical stick keeps pitch behavior unchanged
                //   centered stick remains non-autonomous via farRollEff ramp
                // Keep confirmed correct roll sign: +jx -> positive roll.

                float jxAbs = Math.min(1f, Math.abs(jx));
                float farRollEff = 0f;
                if (jxAbs > stickDeadzone && deadzoneDenominator > 1e-6f) {
                    farRollEff = (jxAbs - stickDeadzone) / deadzoneDenominator;
                }
                wx_raw = L4PhysicsDefaults.DAR_ROLL_SPEED * farRollEff * Math.signum(jx) * input.airRollIntensity;
                wy_raw = 0f;
                wz_raw = L4PhysicsDefaults.W_MAX_PITCH * eff * (-jy) * 0.997f;
            } else {
                wx_raw = (float) input.airRoll * input.airRollIntensity
                        * L4PhysicsDefaults.DAR_ROLL_SPEED;  // directional air roll on native X
                wy_raw = yawCapDar * eff * (-jx) * 1.00f;   // yaw on native Y
                wz_raw = pitchCapDar * eff * (-jy) * 0.997f; // pitch on native Z; invert so stick up = nose up
            }

            // Pre-normalise raw targets so the full vector never exceeds wMax
            float rawMag = (float) Math.sqrt(wx_raw * wx_raw + wy_raw * wy_raw + wz_raw * wz_raw);
            if (rawMag > 1e-6f) {
                float s = Math.min(1f, L4PhysicsDefaults.W_MAX / rawMag);
                wx_des = wx_raw * s;
                wy_des = wy_raw * s;
                wz_des = wz_raw * s;
            } else {
                wx_des = 0f; wy_des = 0f; wz_des = 0f;
            }
        } else {
            // Normal flight: yaw + pitch from stick, no roll
            wx_des = 0f;
            wy_des = L4PhysicsDefaults.W_MAX_YAW   * eff * (-jx); // yaw on native Y
            wz_des = L4PhysicsDefaults.W_MAX_PITCH * eff * (-jy); // pitch on native Z; invert so stick up = nose up
        }

        // --- 2. PD control → angular acceleration (KP=200 for all axes always) ---
        if (!effectiveNoInput) {
            float ax = clamp(KP * (wx_des - w.x), -accelRoll,  accelRoll);
            float ay = clamp(KP * (wy_des - w.y), -accelYaw,   accelYaw);
            float az = clamp(KP * (wz_des - w.z), -accelPitch, accelPitch);
            w.x += ax * adjustedDt;
            w.y += ay * adjustedDt;
            w.z += az * adjustedDt;
        }

        // --- 3. Damping – only when noInput (DAR means always spinning, no decay) ---
        if (effectiveNoInput) {
            float scale = (float) Math.exp(-(damp + brakeOnRelease) * adjustedDt);
            w.x *= scale;
            w.y *= scale;
            w.z *= scale;
        }

        // --- 4. Per-axis velocity caps ---
        float pitchCap = isDirectionalDAR ? Math.min(L4PhysicsDefaults.W_MAX_PITCH, L4PhysicsDefaults.W_MAX * 0.50f) : L4PhysicsDefaults.W_MAX_PITCH;
        float yawCap   = isDirectionalDAR ? Math.min(L4PhysicsDefaults.W_MAX_YAW,   L4PhysicsDefaults.W_MAX * 0.50f) : L4PhysicsDefaults.W_MAX_YAW;
        if (Math.abs(w.x) > L4PhysicsDefaults.W_MAX_ROLL) w.x = Math.signum(w.x) * L4PhysicsDefaults.W_MAX_ROLL;
        if (Math.abs(w.y) > yawCap)                       w.y = Math.signum(w.y) * yawCap;
        if (Math.abs(w.z) > pitchCap)                     w.z = Math.signum(w.z) * pitchCap;

        // --- 5. Global magnitude cap – DAR only (web: normal flight has no global cap) ---
        if (isDARActive) {
            float wMag = (float) Math.sqrt(w.x * w.x + w.y * w.y + w.z * w.z);
            if (wMag > L4PhysicsDefaults.W_MAX) {
                float s = L4PhysicsDefaults.W_MAX / wMag;
                w.x *= s; w.y *= s; w.z *= s;
            }
        }

        // --- 6. Integrate quaternion ---
        integrateQuaternion(quaternion, adjustedDt);
    }

    /**
     * Integrate quaternion using the same first-order derivative method as the web physics.js.
     *
     * Matches JS (docs/js/modules/physics.js):
     *   const rw = -q.x*wx - q.y*wy - q.z*wz;
     *   const rx =  q.w*wx + q.y*wz - q.z*wy;
     *   const ry =  q.w*wy + q.z*wx - q.x*wz;
     *   const rz =  q.w*wz + q.x*wy - q.y*wx;
     *   q.w += rw * halfdt; q.x += rx * halfdt; ...
     *   q.normalize();
     * This is the first-order approximation of dq/dt = 0.5 * q * omega_body.
     */
    private void integrateQuaternion(Quat quaternion, float dt) {
        float wx = w.x, wy = w.y, wz = w.z;
        float halfDt = 0.5f * dt;

        float qw = quaternion.w, qx = quaternion.x, qy = quaternion.y, qz = quaternion.z;

        // dq/dt = 0.5 * q * [wx, wy, wz, 0]  (body-frame angular velocity)
        float drw = -qx * wx - qy * wy - qz * wz;
        float drx =  qw * wx + qy * wz - qz * wy;
        float dry =  qw * wy + qz * wx - qx * wz;
        float drz =  qw * wz + qx * wy - qy * wx;

        quaternion.w += drw * halfDt;
        quaternion.x += drx * halfDt;
        quaternion.y += dry * halfDt;
        quaternion.z += drz * halfDt;
        quaternion.normalize();
    }

    public Vec3 getAngularVelocity() {
        return new Vec3(w.x, w.y, w.z);
    }

    public void setAngularVelocity(float x, float y, float z) {
        w.set(x, y, z);
    }

    public Vec3 getPosition() {
        return new Vec3(position.x, position.y, position.z);
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }

    public void reset() {
        w.set(0, 0, 0);
        position.set(0, 0, 0);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
