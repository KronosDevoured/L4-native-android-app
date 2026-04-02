package com.l4dar.nativeapp;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.l4dar.nativeapp.core.settings.SettingsManager;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal native settings editor for bindable gamepad actions.
 * Opened via Menu/Start from MainActivity.
 */
public final class NativeSettingsActivity extends Activity {
    private SettingsManager settings;
    private final Map<String, EditText> fields = new LinkedHashMap<>();
    private boolean hadInvalidInput = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new SettingsManager(this);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = 24;
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Native Gamepad Bindings");
        title.setTextSize(22f);
        root.addView(title);

        TextView help = new TextView(this);
        help.setText("Use Android key/axis constants. Example defaults: AXIS_X=0, AXIS_Y=1, AXIS_Z=11, AXIS_RZ=14, AXIS_LTRIGGER=17, AXIS_RTRIGGER=18, X=99, B=97.");
        help.setPadding(0, 12, 0, 18);
        root.addView(help);

        addNumberField(root, "Steer X Axis", "steerX", settings.getGpBindSteerX());
        addNumberField(root, "Steer Y Axis", "steerY", settings.getGpBindSteerY());
        addNumberField(root, "Camera X Axis", "cameraX", settings.getGpBindCameraX());
        addNumberField(root, "Camera Y Axis", "cameraY", settings.getGpBindCameraY());
        addNumberField(root, "Throttle Forward Axis", "throttleFwd", settings.getGpBindThrottleForward());
        addNumberField(root, "Throttle Reverse Axis", "throttleRev", settings.getGpBindThrottleReverse());
        addNumberField(root, "Air Roll Left Button", "airRollLeft", settings.getGpBindAirRollLeft());
        addNumberField(root, "Air Roll Right Button", "airRollRight", settings.getGpBindAirRollRight());

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, 20, 0, 0);

        Button defaults = new Button(this);
        defaults.setText("Defaults");
        defaults.setOnClickListener(v -> applyDefaultsToFields());

        Button save = new Button(this);
        save.setText("Save");
        save.setOnClickListener(v -> saveAndClose());

        Button cancel = new Button(this);
        cancel.setText("Cancel");
        cancel.setOnClickListener(v -> finish());

        actions.addView(defaults, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        actions.addView(save, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        actions.addView(cancel, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(actions);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void addNumberField(LinearLayout root, String labelText, String key, int value) {
        TextView label = new TextView(this);
        label.setText(labelText);
        label.setPadding(0, 8, 0, 4);
        root.addView(label);

        EditText edit = new EditText(this);
        edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        edit.setText(String.valueOf(value));
        root.addView(edit);

        fields.put(key, edit);
    }

    private void applyDefaultsToFields() {
        setField("steerX", 0);
        setField("steerY", 1);
        setField("cameraX", 11);
        setField("cameraY", 14);
        setField("throttleFwd", 18);
        setField("throttleRev", 17);
        setField("airRollLeft", 99);
        setField("airRollRight", 97);
    }

    private void setField(String key, int value) {
        EditText f = fields.get(key);
        if (f != null) {
            f.setText(String.valueOf(value));
        }
    }

    private int parseField(String key, int fallback) {
        EditText f = fields.get(key);
        if (f == null) {
            return fallback;
        }
        String text = f.getText() == null ? "" : f.getText().toString().trim();
        if (text.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            hadInvalidInput = true;
            return fallback;
        }
    }

    private void saveAndClose() {
        hadInvalidInput = false;
        int steerX = parseField("steerX", settings.getGpBindSteerX());
        int steerY = parseField("steerY", settings.getGpBindSteerY());
        int cameraX = parseField("cameraX", settings.getGpBindCameraX());
        int cameraY = parseField("cameraY", settings.getGpBindCameraY());
        int throttleFwd = parseField("throttleFwd", settings.getGpBindThrottleForward());
        int throttleRev = parseField("throttleRev", settings.getGpBindThrottleReverse());
        int airRollLeft = parseField("airRollLeft", settings.getGpBindAirRollLeft());
        int airRollRight = parseField("airRollRight", settings.getGpBindAirRollRight());

        settings.setGpBindingAxes(steerX, steerY, cameraX, cameraY, throttleFwd, throttleRev);
        settings.setGpBindingButtons(airRollLeft, airRollRight);
        settings.save();
        if (hadInvalidInput) {
            Toast.makeText(this, "Some invalid values were replaced with previous settings.", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
