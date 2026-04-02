package com.l4dar.nativeapp.render;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.l4dar.nativeapp.core.input.DARButtonManager;
import com.l4dar.nativeapp.core.input.InputController;
import com.l4dar.nativeapp.core.input.InputSnapshot;
import com.l4dar.nativeapp.core.settings.SettingsManager;

public final class L4SurfaceView extends GLSurfaceView {
    private final SettingsManager settingsManager;
    private final L4Renderer renderer;

    public L4SurfaceView(Context context, SettingsManager settingsManager) {
        super(context);
        this.settingsManager = settingsManager;

        setEGLContextClientVersion(3);
        float density = context.getResources().getDisplayMetrics().density;
        renderer = new L4Renderer(context, settingsManager, density);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        
        // Enable gamepad input
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);
        setLongClickable(true);
        post(new Runnable() {
            @Override
            public void run() {
                requestFocus();
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        renderer.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        InputController input = renderer.getInputController();
        if (input.onGenericMotionEvent(event)) {
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        InputController input = renderer.getInputController();
        if (input.onKeyDown(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        InputController input = renderer.getInputController();
        if (input.onKeyUp(keyCode, event)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    public void savePersistentState() {
        // Save current DAR state (across touch/gamepad/keyboard) before saving preferences
        InputController input = renderer.getInputController();
        InputSnapshot snapshot = input.getInputSnapshot();
        settingsManager.setDARState(snapshot.airRoll);
        
        settingsManager.save();
    }

    public L4Renderer getRenderer() {
        return renderer;
    }

    public void restorePersistentState() {
        // Reload persisted settings (including bindings) before restoring runtime state.
        settingsManager.load();

        queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.setCarModel(settingsManager.getSelectedCarBody());
            }
        });

        // Restore DAR state across all input handlers
        InputController input = renderer.getInputController();
        int savedDARState = settingsManager.getDARState();
        input.restoreDARState(savedDARState);
    }

    public InputController getInputController() {
        return renderer.getInputController();
    }

    public void setSelectedCarBody(final String modelName) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.setCarModel(modelName);
            }
        });
    }

    public void setNightMode(final boolean nightMode) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.setNightMode(nightMode);
            }
        });
    }
}
