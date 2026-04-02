package com.l4dar.nativeapp.core.math;

public final class Quat {
    public float w;
    public float x;
    public float y;
    public float z;

    public Quat() {
        identity();
    }

    public Quat identity() {
        w = 1.0f;
        x = 0.0f;
        y = 0.0f;
        z = 0.0f;
        return this;
    }

    public Quat set(float w, float x, float y, float z) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Quat copy(Quat other) {
        return set(other.w, other.x, other.y, other.z);
    }

    public Quat normalize() {
        float mag = (float) Math.sqrt(w * w + x * x + y * y + z * z);
        if (mag > 0.0f) {
            float inv = 1.0f / mag;
            w *= inv;
            x *= inv;
            y *= inv;
            z *= inv;
        }
        return this;
    }

    public Quat multiply(Quat rhs) {
        float nw = w * rhs.w - x * rhs.x - y * rhs.y - z * rhs.z;
        float nx = w * rhs.x + x * rhs.w + y * rhs.z - z * rhs.y;
        float ny = w * rhs.y - x * rhs.z + y * rhs.w + z * rhs.x;
        float nz = w * rhs.z + x * rhs.y - y * rhs.x + z * rhs.w;
        return set(nw, nx, ny, nz);
    }

    public static Quat fromAxisAngle(Vec3 axis, float angleRad, Quat out) {
        float half = 0.5f * angleRad;
        float s = (float) Math.sin(half);
        return out.set((float) Math.cos(half), axis.x * s, axis.y * s, axis.z * s).normalize();
    }
}
