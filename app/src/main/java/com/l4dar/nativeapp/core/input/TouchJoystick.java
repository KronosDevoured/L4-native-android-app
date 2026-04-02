package com.l4dar.nativeapp.core.input;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.view.MotionEvent;
import com.l4dar.nativeapp.core.settings.SettingsManager;

public class TouchJoystick {
    private static final float DEFAULT_BASE_RADIUS = 80f;
    private static final float STICK_RADIUS = 40f;

    private final Paint basePaint;
    private final Paint stickPaint;
    private final SettingsManager settings;
    
    private PointF baseCenter;
    private PointF stickPosition;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private boolean isDragging = false;
    
    private float baseRadius;
    private float normalizedX = 0f;
    private float normalizedY = 0f;

    public TouchJoystick(SettingsManager settings) {
        this.settings = settings;
        
        basePaint = new Paint();
        basePaint.setColor(0x44FFFFFF); // Translucent white
        basePaint.setStyle(Paint.Style.FILL);
        
        stickPaint = new Paint();
        stickPaint.setColor(0xFFFFFFFF); // Opaque white
        stickPaint.setStyle(Paint.Style.FILL);
        
        baseRadius = DEFAULT_BASE_RADIUS;
        stickPosition = new PointF();
    }

    public void setPosition(float centerX, float centerY) {
        this.baseCenter = new PointF(centerX, centerY);
        stickPosition.set(centerX, centerY);
    }

    public void setBaseRadius(float radius) {
        this.baseRadius = radius;
    }

    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);
        
        if (baseCenter == null) {
            return false;
        }
        
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (isDragging) {
                    break;
                }
                float touchX = event.getX(pointerIndex);
                float touchY = event.getY(pointerIndex);
                if (isInsideBase(touchX, touchY)) {
                    activePointerId = pointerId;
                    isDragging = true;
                    updateStickPosition(touchX, touchY);
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging && activePointerId != MotionEvent.INVALID_POINTER_ID) {
                    int activePointerIndex = event.findPointerIndex(activePointerId);
                    if (activePointerIndex >= 0) {
                        float moveX = event.getX(activePointerIndex);
                        float moveY = event.getY(activePointerIndex);
                        updateStickPosition(moveX, moveY);
                        return true;
                    } else {
                        // The active pointer is no longer present in this event stream.
                        // Reset immediately so the stick cannot remain latched.
                        isDragging = false;
                        activePointerId = MotionEvent.INVALID_POINTER_ID;
                        resetStick();
                        return true;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (activePointerId == pointerId) {
                    isDragging = false;
                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                    resetStick();
                    return true;
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                resetStick();
                return true;
        }
        return false;
    }

    public boolean beginRelocatedDrag(int pointerId, float x, float y) {
        if (isDragging) {
            return false;
        }
        setPosition(x, y);
        activePointerId = pointerId;
        isDragging = true;
        updateStickPosition(x, y);
        return true;
    }

    public boolean contains(float x, float y) {
        return isInsideBase(x, y);
    }

    private boolean isInsideBase(float x, float y) {
        if (baseCenter == null) return false;
        float dx = x - baseCenter.x;
        float dy = y - baseCenter.y;
        return (dx * dx + dy * dy) <= (baseRadius * baseRadius);
    }

    private void updateStickPosition(float x, float y) {
        if (baseCenter == null) return;
        
        float dx = x - baseCenter.x;
        float dy = y - baseCenter.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (distance > baseRadius) {
            float scale = baseRadius / distance;
            dx *= scale;
            dy *= scale;
        }
        
        stickPosition.set(baseCenter.x + dx, baseCenter.y + dy);
        
        // Return raw normalized values in [-1, 1] — deadzone ramping is applied in the
        // physics engine so both touch and gamepad go through the same logic.
        normalizedX = Math.max(-1f, Math.min(1f, dx / baseRadius));
        normalizedY = Math.max(-1f, Math.min(1f, -(dy / baseRadius))); // Invert Y: up = positive
    }

    private void resetStick() {
        if (baseCenter != null) {
            stickPosition.set(baseCenter.x, baseCenter.y);
        }
        normalizedX = 0f;
        normalizedY = 0f;
    }

    public void draw(Canvas canvas) {
        if (baseCenter == null) return;
        
        // Draw base circle
        canvas.drawCircle(baseCenter.x, baseCenter.y, baseRadius, basePaint);
        
        // Draw stick circle
        canvas.drawCircle(stickPosition.x, stickPosition.y, STICK_RADIUS, stickPaint);
    }

    public float getNormalizedX() {
        return normalizedX;
    }

    public float getNormalizedY() {
        return normalizedY;
    }

    public boolean isDragging() {
        return isDragging;
    }

    public PointF getBaseCenter() {
        return baseCenter;
    }

    public PointF getStickPosition() {
        return stickPosition;
    }

    public float getBaseRadius() {
        return baseRadius;
    }
}
