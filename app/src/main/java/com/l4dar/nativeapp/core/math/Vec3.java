package com.l4dar.nativeapp.core.math;

public final class Vec3 {
    public float x;
    public float y;
    public float z;

    public Vec3() {
        this(0.0f, 0.0f, 0.0f);
    }

    public Vec3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3 set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vec3 copy(Vec3 other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        return this;
    }

    public Vec3 add(Vec3 other) {
        x += other.x;
        y += other.y;
        z += other.z;
        return this;
    }

    public Vec3 sub(Vec3 other) {
        x -= other.x;
        y -= other.y;
        z -= other.z;
        return this;
    }

    public Vec3 scale(float value) {
        x *= value;
        y *= value;
        z *= value;
        return this;
    }

    public float dot(Vec3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public float length() {
        return (float) Math.sqrt(dot(this));
    }

    public Vec3 normalize() {
        float len = length();
        if (len > 0.0f) {
            scale(1.0f / len);
        }
        return this;
    }

    public static Vec3 cross(Vec3 a, Vec3 b, Vec3 out) {
        out.set(
            a.y * b.z - a.z * b.y,
            a.z * b.x - a.x * b.z,
            a.x * b.y - a.y * b.x
        );
        return out;
    }
}
