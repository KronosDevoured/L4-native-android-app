package com.l4dar.nativeapp;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.InputDevice;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import com.l4dar.nativeapp.core.settings.SettingsManager;
import com.l4dar.nativeapp.render.L4SurfaceView;

import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int DAY_ROOT = Color.parseColor("#F4F7FB");
    private static final int NIGHT_ROOT = Color.parseColor("#000000");
    private static final int DAY_SCRIM = Color.parseColor("#66000000");
    private static final int NIGHT_SCRIM = Color.parseColor("#CC000000");
    private static final int DAY_PANEL = Color.parseColor("#EEF2F6");
    private static final int NIGHT_PANEL = Color.parseColor("#1A1D22");
    private static final int DAY_CARD = Color.parseColor("#FFFFFF");
    private static final int NIGHT_CARD = Color.parseColor("#2A2F36");
    private static final int DAY_TEXT_PRIMARY = Color.parseColor("#101418");
    private static final int NIGHT_TEXT_PRIMARY = Color.parseColor("#F2F5F7");
    private static final int DAY_TEXT_SECONDARY = Color.parseColor("#49515A");
    private static final int NIGHT_TEXT_SECONDARY = Color.parseColor("#C1CAD4");
    private static final int DAY_BUTTON = Color.parseColor("#DCE6F2");
    private static final int NIGHT_BUTTON = Color.parseColor("#3A4554");
    private static final int DAY_BUTTON_ACTIVE = Color.parseColor("#3D74B8");
    private static final int NIGHT_BUTTON_ACTIVE = Color.parseColor("#4C86CF");
    private static final int DEFAULT_STICK_SIZE = 100;

    private L4SurfaceView surfaceView;
    private SettingsManager settingsManager;
    private View menuOverlay;
    private View ringModeOverlay;
    private Button themeBtn;
    private Spinner carBodySpinner;
    private ArrayAdapter<String> carBodyAdapter;
    private TextView darModeLabel;
    private boolean startPressed = false;
    private boolean sharePressed = false;
    private boolean backExitArmed = false;
    private boolean exitingFromDialog = false;
    private boolean gameplayPausedForExitDialog = false;
    private AlertDialog exitConfirmationDialog;
    private OnBackInvokedCallback backInvokedCallback;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable darBadgeRefresh = new Runnable() {
        @Override
        public void run() {
            updateDarModeLabelText(settingsManager.getTouchDarDirection());
            updateDarModeBadgeState();
            uiHandler.postDelayed(this, 50L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        enterImmersiveFullscreen();

        settingsManager = new SettingsManager(this);
        surfaceView = new L4SurfaceView(this, settingsManager);

        FrameLayout renderHost = findViewById(R.id.renderSurfaceHost);
        renderHost.addView(surfaceView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));

        menuOverlay = findViewById(R.id.menuOverlay);
        ringModeOverlay = findViewById(R.id.ringModeOverlay);

        Button menuBtn = findViewById(R.id.menuBtn);
        Button menuCloseBtn = findViewById(R.id.menuCloseBtn);
        Button ringModeBtn = findViewById(R.id.ringModeBtn);
        Button ringModeCloseBtn = findViewById(R.id.ringModeCloseBtn);
        themeBtn = findViewById(R.id.themeBtn);
        darModeLabel = findViewById(R.id.darModeLabel);
        if (darModeLabel != null) {
            darModeLabel.setEnabled(true);
            darModeLabel.setClickable(false);
            darModeLabel.setLongClickable(false);
            darModeLabel.setFocusable(false);
            darModeLabel.setOnTouchListener((v, event) -> false);
        }

        menuBtn.setOnClickListener(v -> menuOverlay.setVisibility(View.VISIBLE));
        menuCloseBtn.setOnClickListener(v -> menuOverlay.setVisibility(View.GONE));
        if (ringModeBtn != null) {
            ringModeBtn.setVisibility(View.GONE);
        }
        if (ringModeOverlay != null) {
            ringModeOverlay.setVisibility(View.GONE);
        }
        if (ringModeCloseBtn != null && ringModeOverlay != null) {
            ringModeCloseBtn.setOnClickListener(v -> ringModeOverlay.setVisibility(View.GONE));
        }
        themeBtn.setOnClickListener(v -> {
            toggleThemeMode();
        });

        menuOverlay.setOnClickListener(v -> menuOverlay.setVisibility(View.GONE));
        findViewById(R.id.menuPanel).setOnClickListener(v -> {
            // Consume clicks so tapping inside panel does not close overlay.
        });

        if (ringModeOverlay != null) {
            ringModeOverlay.setOnClickListener(v -> ringModeOverlay.setVisibility(View.GONE));
        }
        View ringModePanel = findViewById(R.id.ringModePanel);
        if (ringModePanel != null) {
            ringModePanel.setOnClickListener(v -> {
                // Consume clicks so tapping inside panel does not close overlay.
            });
        }

        setupMenuControls();
        applyTheme(settingsManager.isNightMode());
        positionDarModeBadge();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backInvokedCallback = this::handleDeviceBackPressed;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                backInvokedCallback
            );
        }
    }

    private void setupMenuControls() {
        Button restartBtn = findViewById(R.id.restartBtn);
        Button airRollModeBtn = findViewById(R.id.airRollModeBtn);
        Button touchDarDirectionBtn = findViewById(R.id.touchDarDirectionBtn);
        SeekBar gameSpeedSeek = findViewById(R.id.gameSpeedSeek);
        TextView gameSpeedValue = findViewById(R.id.gameSpeedValue);

        SeekBar touchDeadzoneSeek = findViewById(R.id.touchDeadzoneSeek);
        TextView touchDeadzoneValue = findViewById(R.id.touchDeadzoneValue);
        TextView gamepadStatusText = findViewById(R.id.gamepadStatusText);
        TextView gamepadBindingsSummary = findViewById(R.id.gamepadBindingsSummary);
        TextView gpLeftStickDeadzoneValue = findViewById(R.id.gpLeftStickDeadzoneValue);
        TextView gpRightStickDeadzoneValue = findViewById(R.id.gpRightStickDeadzoneValue);
        SeekBar gpLeftStickDeadzoneSeek = findViewById(R.id.gpLeftStickDeadzoneSeek);
        SeekBar gpRightStickDeadzoneSeek = findViewById(R.id.gpRightStickDeadzoneSeek);
        TextView stickSizeValue = findViewById(R.id.stickSizeValue);
        SeekBar stickSizeSeek = findViewById(R.id.stickSizeSeek);
        TextView dynamicsAccelSummary = findViewById(R.id.dynamicsAccelSummary);
        TextView dynamicsDampSummary = findViewById(R.id.dynamicsDampSummary);

        carBodySpinner = findViewById(R.id.carBodySpinner);

        restartBtn.setOnClickListener(v -> {
            if (surfaceView != null
                    && surfaceView.getRenderer() != null
                    && surfaceView.getRenderer().getPhysicsEngine() != null
                    && surfaceView.getRenderer().getCarQuaternion() != null) {
                surfaceView.getRenderer().getPhysicsEngine().reset();
                surfaceView.getRenderer().getCarQuaternion().identity();
            }
        });

        updateAirRollModeText(airRollModeBtn, settingsManager.getAirRollIsToggle());
        airRollModeBtn.setOnClickListener(v -> {
            boolean next = !settingsManager.getAirRollIsToggle();
            settingsManager.setAirRollIsToggle(next);
            updateAirRollModeText(airRollModeBtn, next);
        });

        updateTouchDarDirectionText(touchDarDirectionBtn, settingsManager.getTouchDarDirection());
        touchDarDirectionBtn.setOnClickListener(v -> {
            int currentDirection = settingsManager.getTouchDarDirection();
            int nextDirection;
            if (currentDirection == -1) {
                nextDirection = 1;
            } else if (currentDirection == 1) {
                nextDirection = 0;
            } else {
                nextDirection = -1;
            }
            settingsManager.setTouchDarDirection(nextDirection);
            updateTouchDarDirectionText(touchDarDirectionBtn, nextDirection);
            updateDarModeLabelText(nextDirection);
        });

        updateDarModeLabelText(settingsManager.getTouchDarDirection());
        updateDarModeBadgeState();

        int gameSpeedProgress = Math.round((settingsManager.getGameSpeed() - 0.05f) / 0.01f);
        gameSpeedSeek.setProgress(gameSpeedProgress);
        updateGameSpeedText(gameSpeedValue, settingsManager.getGameSpeed());
        gameSpeedSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = 0.05f + (progress * 0.01f);
                settingsManager.setGameSpeed(value);
                updateGameSpeedText(gameSpeedValue, settingsManager.getGameSpeed());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        int deadzoneProgress = Math.round(settingsManager.getTouchDeadzone() * 100f);
        touchDeadzoneSeek.setProgress(deadzoneProgress);
        updateDeadzoneText(touchDeadzoneValue, settingsManager.getTouchDeadzone());
        touchDeadzoneSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = progress / 100f;
                settingsManager.setTouchDeadzone(value);
                updateDeadzoneText(touchDeadzoneValue, settingsManager.getTouchDeadzone());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        if (gamepadStatusText != null) {
            gamepadStatusText.setText("Status: Waiting for gamepad");
        }
        updateGamepadBindingsSummary(gamepadBindingsSummary);
        if (gpLeftStickDeadzoneSeek != null) {
            int gpLeftDeadzoneProgress = Math.round(settingsManager.getGpLeftStickDeadzone() * 100f);
            gpLeftStickDeadzoneSeek.setProgress(gpLeftDeadzoneProgress);
            updateLabeledValueText(gpLeftStickDeadzoneValue, "Left Stick Deadzone", settingsManager.getGpLeftStickDeadzone());
            gpLeftStickDeadzoneSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    float value = progress / 100f;
                    settingsManager.setGpLeftStickDeadzone(value);
                    updateLabeledValueText(gpLeftStickDeadzoneValue, "Left Stick Deadzone", settingsManager.getGpLeftStickDeadzone());
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) { }
            });
        }

        if (settingsManager != null && gpRightStickDeadzoneSeek != null && gpRightStickDeadzoneValue != null) {
            int gpRightDeadzoneProgress = Math.round(settingsManager.getGpRightStickDeadzone() * 100f);
            gpRightStickDeadzoneSeek.setProgress(gpRightDeadzoneProgress);
            updateLabeledValueText(gpRightStickDeadzoneValue, "Right Stick Deadzone", settingsManager.getGpRightStickDeadzone());
            gpRightStickDeadzoneSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    float value = progress / 100f;
                    settingsManager.setGpRightStickDeadzone(value);
                    updateLabeledValueText(gpRightStickDeadzoneValue, "Right Stick Deadzone", settingsManager.getGpRightStickDeadzone());
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) { }
            });
        }

        if (stickSizeSeek != null) {
            int initialStickSize = settingsManager != null ? settingsManager.getStickSize() : DEFAULT_STICK_SIZE;
            stickSizeSeek.setProgress(initialStickSize - 60);
            updateStickSizeText(stickSizeValue, initialStickSize);
            applyStickSizeToInputController();
            stickSizeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int value = 60 + progress;
                    settingsManager.setStickSize(value);
                    updateStickSizeText(stickSizeValue, settingsManager.getStickSize());
                    applyStickSizeToInputController();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) { }
            });
        }

        updateDynamicsSummary(dynamicsAccelSummary, dynamicsDampSummary);

        String[] carBodies = new String[] { "octane", "fennec", "dominus" };
        carBodyAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, carBodies) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                styleSpinnerText(view, settingsManager.isNightMode());
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                styleSpinnerText(view, settingsManager.isNightMode());
                view.setBackgroundColor(settingsManager.isNightMode() ? NIGHT_PANEL : DAY_CARD);
                return view;
            }
        };
        carBodyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        carBodySpinner.setAdapter(carBodyAdapter);

        String currentCarBody = settingsManager.getSelectedCarBody();
        for (int i = 0; i < carBodies.length; i++) {
            if (carBodies[i].equals(currentCarBody)) {
                carBodySpinner.setSelection(i);
                break;
            }
        }

        carBodySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String modelName = carBodies[position];
                settingsManager.setSelectedCarBody(modelName);
                if (surfaceView != null) {
                    surfaceView.setSelectedCarBody(modelName);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void updateAirRollModeText(Button airRollModeBtn, boolean isToggle) {
        airRollModeBtn.setText(isToggle ? "Air Roll Mode: Toggle" : "Air Roll Mode: Hold");
    }

    private void updateTouchDarDirectionText(Button touchDarDirectionBtn, int direction) {
        touchDarDirectionBtn.setText("Touch Air Roll: " + touchDirectionName(direction));
    }

    private void updateDarModeLabelText(int direction) {
        if (darModeLabel != null) {
            darModeLabel.setText(touchDirectionName(direction));
            positionDarModeBadge();
        }
    }

    private String touchDirectionName(int direction) {
        if (direction < 0) {
            return "Left";
        }
        if (direction > 0) {
            return "Right";
        }
        return "Free";
    }

    private void updateGameSpeedText(TextView gameSpeedValue, float speed) {
        int pct = Math.round(speed * 100f);
        gameSpeedValue.setText(String.format(Locale.US, "Game Speed: %d%%", pct));
    }

    private void updateDeadzoneText(TextView deadzoneValue, float deadzone) {
        deadzoneValue.setText(String.format(Locale.US, "Touch Deadzone: %.2f", deadzone));
    }

    private void updateStickSizeText(TextView stickSizeValue, int stickSize) {
        if (stickSizeValue != null) {
            stickSizeValue.setText(String.format(Locale.US, "Stick Size: %d", stickSize));
        }
    }

    private void applyStickSizeToInputController() {
        if (surfaceView == null) {
            return;
        }
        if (surfaceView.getInputController() != null) {
            surfaceView.getInputController().applyConfiguredStickSize();
        }
    }

    private void updateLabeledValueText(TextView textView, String label, float value) {
        if (textView != null) {
            textView.setText(String.format(Locale.US, "%s: %.2f", label, value));
        }
    }

    private void updateGamepadBindingsSummary(TextView textView) {
        if (textView == null) {
            return;
        }
        String toggleDar = formatGamepadBindingLabel(settingsManager.getGpBindToggleDar());
        String rollLeft = formatGamepadBindingLabel(settingsManager.getGpBindAirRollLeft());
        String rollRight = formatGamepadBindingLabel(settingsManager.getGpBindAirRollRight());
        String rollFree = formatGamepadBindingLabel(settingsManager.getGpBindRollFree());
        String summary = String.format(
            Locale.US,
            "Default bindings: Toggle DAR = %s · Air Roll Left = %s · Air Roll Right = %s · Air Roll (Free) = %s",
            toggleDar,
            rollLeft,
            rollRight,
            rollFree);
        String reservedConflict = settingsManager.getReservedGamepadBindingConflictSummary();
        if (!reservedConflict.isEmpty()) {
            summary = summary + "\nConflict: " + reservedConflict;
        }
        textView.setText(summary);
    }

    private void updateDynamicsSummary(TextView accelSummary, TextView dampSummary) {
        if (accelSummary != null) {
            accelSummary.setText(String.format(
                Locale.US,
                "Pitch %.0f°/s² · Yaw %.0f°/s² · Roll %.0f°/s²",
                settingsManager.getSetting("maxAccelPitch", 733f),
                settingsManager.getSetting("maxAccelYaw", 528f),
                settingsManager.getSetting("maxAccelRoll", 898f)));
        }
        if (dampSummary != null) {
            dampSummary.setText(String.format(
                Locale.US,
                "Damp %.2f · Damp (DAR) %.2f",
                settingsManager.getSetting("damp", 2.96f),
                settingsManager.getSetting("dampDar", 4.35f)));
        }
    }

    private String formatGamepadBindingLabel(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_B:
                return "B / Circle";
            case KeyEvent.KEYCODE_BUTTON_R1:
                return "R1 / RB";
            case KeyEvent.KEYCODE_BUTTON_L1:
                return "L1 / LB";
            case KeyEvent.KEYCODE_BUTTON_R2:
                return "R2 / RT";
            case KeyEvent.KEYCODE_BUTTON_START:
                return "Start / Options";
            default:
                String raw = KeyEvent.keyCodeToString(keyCode);
                return raw != null ? raw.replace("KEYCODE_", "") : "Unbound";
        }
    }

    private void applyTheme(boolean nightMode) {
        View rootShell = findViewById(R.id.rootShell);
        View menuPanel = findViewById(R.id.menuPanel);
        View ringModePanel = findViewById(R.id.ringModePanel);
        View darModeBadge = findViewById(R.id.darModeLabel);
        View rotationCard = findViewById(R.id.rotationCard);
        View gamepadCard = findViewById(R.id.gamepadCard);
        View dynamicsCard = findViewById(R.id.dynamicsCard);
        View carCard = findViewById(R.id.carCard);
        View viewHudCard = findViewById(R.id.viewHudCard);

        rootShell.setBackgroundColor(nightMode ? NIGHT_ROOT : DAY_ROOT);
        menuOverlay.setBackgroundColor(nightMode ? NIGHT_SCRIM : DAY_SCRIM);
        if (ringModeOverlay != null) {
            ringModeOverlay.setBackgroundColor(nightMode ? NIGHT_SCRIM : DAY_SCRIM);
        }
        menuPanel.setBackgroundColor(nightMode ? NIGHT_PANEL : DAY_PANEL);
        if (ringModePanel != null) {
            ringModePanel.setBackgroundColor(nightMode ? NIGHT_PANEL : DAY_PANEL);
        }
        darModeBadge.setBackgroundColor(nightMode ? Color.parseColor("#80455A72") : Color.parseColor("#80405670"));
        rotationCard.setBackgroundColor(nightMode ? NIGHT_CARD : DAY_CARD);
        gamepadCard.setBackgroundColor(nightMode ? NIGHT_CARD : DAY_CARD);
        dynamicsCard.setBackgroundColor(nightMode ? NIGHT_CARD : DAY_CARD);
        carCard.setBackgroundColor(nightMode ? NIGHT_CARD : DAY_CARD);
        viewHudCard.setBackgroundColor(nightMode ? NIGHT_CARD : DAY_CARD);

        updateThemeButtonText(nightMode);
        applyTextTheme((ViewGroup) menuPanel, nightMode);
        if (ringModePanel instanceof ViewGroup) {
            applyTextTheme((ViewGroup) ringModePanel, nightMode);
        }
        applyButtonTheme((ViewGroup) findViewById(R.id.topActionRow), nightMode);
        applyButtonTheme((ViewGroup) menuPanel, nightMode);
        if (ringModePanel instanceof ViewGroup) {
            applyButtonTheme((ViewGroup) ringModePanel, nightMode);
        }
        applySeekBarTheme(findViewById(R.id.gameSpeedSeek), nightMode);
        applySeekBarTheme(findViewById(R.id.touchDeadzoneSeek), nightMode);
        applySeekBarTheme(findViewById(R.id.gpLeftStickDeadzoneSeek), nightMode);
        applySeekBarTheme(findViewById(R.id.gpRightStickDeadzoneSeek), nightMode);
        applySeekBarTheme(findViewById(R.id.stickSizeSeek), nightMode);

        if (carBodySpinner != null) {
            carBodySpinner.setBackgroundTintList(ColorStateList.valueOf(nightMode ? NIGHT_BUTTON : DAY_BUTTON));
        }
        updateDarModeBadgeState();
        if (carBodyAdapter != null) {
            carBodyAdapter.notifyDataSetChanged();
        }
    }

    private void updateDarModeBadgeState() {
        if (darModeLabel == null) {
            return;
        }

        boolean nightMode = settingsManager.isNightMode();
        boolean darActive = false;
        if (surfaceView != null
                && surfaceView.getInputController() != null
                && surfaceView.getInputController().getDARButtonManager() != null) {
            darActive = surfaceView.getInputController().getDARButtonManager().isTouchDAROn();
        }

        int background = darActive
            ? (nightMode ? NIGHT_BUTTON_ACTIVE : DAY_BUTTON_ACTIVE)
            : (nightMode ? NIGHT_BUTTON : DAY_BUTTON);
        int textColor = darActive ? Color.WHITE : (nightMode ? NIGHT_TEXT_PRIMARY : DAY_TEXT_PRIMARY);
        darModeLabel.setVisibility(View.VISIBLE);
        darModeLabel.setAlpha(1f);
        darModeLabel.setBackgroundColor(background);
        darModeLabel.setTextColor(textColor);
        darModeLabel.bringToFront();
    }

    private void positionDarModeBadge() {
        if (darModeLabel == null) {
            return;
        }

        View root = findViewById(R.id.rootShell);
        if (root == null) {
            return;
        }

        root.post(() -> {
            int rootW = root.getWidth();
            int rootH = root.getHeight();
            int badgeW = darModeLabel.getWidth();
            int badgeH = darModeLabel.getHeight();
            if (badgeW <= 0 || badgeH <= 0) {
                darModeLabel.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                badgeW = darModeLabel.getMeasuredWidth();
                badgeH = darModeLabel.getMeasuredHeight();
            }
            if (rootW <= 0 || rootH <= 0 || badgeW <= 0 || badgeH <= 0) {
                return;
            }

            float density = getResources().getDisplayMetrics().density;
            int margin = Math.round(120f * density);
            int innerPad = Math.round(80f * density);

            int darCenterX = rootW - (margin + innerPad);
            int darCenterY = rootH - margin;
            int left = Math.max(0, Math.min(rootW - badgeW, darCenterX - (badgeW / 2)));
            int top = Math.max(0, Math.min(rootH - badgeH, darCenterY - (badgeH / 2)));

            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) darModeLabel.getLayoutParams();
            lp.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
            lp.leftMargin = left;
            lp.topMargin = top;
            darModeLabel.setLayoutParams(lp);
            if (root instanceof FrameLayout) {
                ((FrameLayout) root).bringChildToFront(darModeLabel);
            }
            darModeLabel.bringToFront();
        });
    }

    private void updateThemeButtonText(boolean nightMode) {
        if (themeBtn != null) {
            themeBtn.setText(nightMode ? "Night" : "Day");
        }
    }

    private void applyTextTheme(ViewGroup parent, boolean nightMode) {
        int primary = nightMode ? NIGHT_TEXT_PRIMARY : DAY_TEXT_PRIMARY;
        int secondary = nightMode ? NIGHT_TEXT_SECONDARY : DAY_TEXT_SECONDARY;
        float sectionTitleThresholdPx = 18f * getResources().getDisplayMetrics().scaledDensity;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof Button) {
                ((Button) child).setTextColor(primary);
            } else if (child instanceof TextView) {
                TextView textView = (TextView) child;
                CharSequence text = textView.getText();
                boolean isSectionTitle = textView.getTextSize() >= sectionTitleThresholdPx || (text != null && text.toString().endsWith(":")) || (textView.getTypeface() != null && textView.getTypeface().isBold());
                textView.setTextColor(isSectionTitle ? primary : secondary);
            } else if (child instanceof ViewGroup) {
                applyTextTheme((ViewGroup) child, nightMode);
            }
        }
    }

    private void applyButtonTheme(ViewGroup parent, boolean nightMode) {
        int buttonColor = nightMode ? NIGHT_BUTTON : DAY_BUTTON;
        int textColor = nightMode ? NIGHT_TEXT_PRIMARY : DAY_TEXT_PRIMARY;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof Button) {
                Button button = (Button) child;
                button.setBackgroundTintList(ColorStateList.valueOf(buttonColor));
                button.setTextColor(textColor);
            } else if (child instanceof ViewGroup) {
                applyButtonTheme((ViewGroup) child, nightMode);
            }
        }
    }

    private void applySeekBarTheme(SeekBar seekBar, boolean nightMode) {
        if (seekBar == null) {
            return;
        }
        int accent = nightMode ? Color.parseColor("#89A7C8") : Color.parseColor("#476E95");
        int track = nightMode ? Color.parseColor("#556273") : Color.parseColor("#B6C6D8");
        seekBar.setProgressTintList(ColorStateList.valueOf(accent));
        seekBar.setThumbTintList(ColorStateList.valueOf(accent));
        seekBar.setProgressBackgroundTintList(ColorStateList.valueOf(track));
    }

    private void styleSpinnerText(View view, boolean nightMode) {
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(nightMode ? NIGHT_TEXT_PRIMARY : DAY_TEXT_PRIMARY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        enterImmersiveFullscreen();
        if (surfaceView != null) {
            surfaceView.onResume();
            surfaceView.restorePersistentState();
            surfaceView.setNightMode(settingsManager.isNightMode());
            surfaceView.requestFocus();
        }
        applyTheme(settingsManager.isNightMode());
        uiHandler.removeCallbacks(darBadgeRefresh);
        uiHandler.post(darBadgeRefresh);
    }

    @Override
    protected void onPause() {
        uiHandler.removeCallbacks(darBadgeRefresh);
        if (surfaceView != null) {
            surfaceView.onPause();
            surfaceView.savePersistentState();
        }
        super.onPause();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (isStartMenuToggleKey(keyCode, event)) {
                if (!startPressed) {
                    startPressed = true;
                    toggleMenuOverlay();
                }
                return true;
            }

            if (isShareThemeToggleKey(keyCode, event)) {
                if (!sharePressed) {
                    sharePressed = true;
                    toggleThemeMode();
                }
                return true;
            }

            if (isMenuOpen() && keyCode == KeyEvent.KEYCODE_BUTTON_B) {
                closeMenuOverlay();
                return true;
            }

            if (isMenuOpen() && isGamepadEvent(event) && keyCode == KeyEvent.KEYCODE_BACK) {
                closeMenuOverlay();
                return true;
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            if (isStartMenuToggleKey(keyCode, event)) {
                startPressed = false;
                return true;
            }

            if (isShareThemeToggleKey(keyCode, event)) {
                sharePressed = false;
                return true;
            }

            if (isMenuOpen() && keyCode == KeyEvent.KEYCODE_BUTTON_B) {
                return true;
            }

            if (isMenuOpen() && isGamepadEvent(event) && keyCode == KeyEvent.KEYCODE_BACK) {
                return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        handleDeviceBackPressed();
    }

    private void handleDeviceBackPressed() {
        if (isMenuOpen()) {
            closeMenuOverlay();
            return;
        }

        if (isExitDialogShowing()) {
            return;
        }

        if (!backExitArmed) {
            backExitArmed = true;
            Toast.makeText(this, "Press Back again to exit", Toast.LENGTH_SHORT).show();
            return;
        }

        showExitConfirmationDialog();
    }

    @Override
    protected void onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && backInvokedCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backInvokedCallback);
            backInvokedCallback = null;
        }
        super.onDestroy();
    }

    private boolean isGamepadEvent(KeyEvent event) {
        int source = event.getSource();
        return (source & InputDevice.SOURCE_GAMEPAD) != 0
                || (source & InputDevice.SOURCE_JOYSTICK) != 0;
    }

    private boolean isStartMenuToggleKey(int keyCode, KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_BUTTON_START;
    }

    private boolean isShareThemeToggleKey(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            return true;
        }
        return isGamepadEvent(event)
                && (keyCode == KeyEvent.KEYCODE_BUTTON_SELECT || keyCode == KeyEvent.KEYCODE_BUTTON_MODE);
    }

    private void toggleThemeMode() {
        boolean nextNightMode = !settingsManager.isNightMode();
        settingsManager.setNightMode(nextNightMode);
        applyTheme(nextNightMode);
        if (surfaceView != null) {
            surfaceView.setNightMode(nextNightMode);
        }
    }

    private boolean isMenuOpen() {
        return menuOverlay != null && menuOverlay.getVisibility() == View.VISIBLE;
    }

    private void openMenuOverlay() {
        if (menuOverlay != null) {
            menuOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void closeMenuOverlay() {
        if (menuOverlay != null) {
            menuOverlay.setVisibility(View.GONE);
        }
    }

    private void toggleMenuOverlay() {
        if (isMenuOpen()) {
            closeMenuOverlay();
        } else {
            openMenuOverlay();
        }
    }

    private void showExitConfirmationDialog() {
        if (isExitDialogShowing()) {
            return;
        }

        exitingFromDialog = false;
        pauseGameplayForExitDialog();
        exitConfirmationDialog = new AlertDialog.Builder(this)
            .setTitle("Leave app?")
            .setMessage("Do you want to exit the app?")
            .setPositiveButton("Yes", (dialog, which) -> {
                exitingFromDialog = true;
                clearBackExitState();
                requestExplicitAppExit();
            })
            .setNegativeButton("No", (dialog, which) -> {
                exitingFromDialog = false;
                clearBackExitState();
                dialog.dismiss();
            })
            .setCancelable(false)
            .create();
        exitConfirmationDialog.setOnDismissListener(dialog -> {
            exitConfirmationDialog = null;
            if (!exitingFromDialog && !isFinishing()) {
                resumeGameplayAfterExitDialog();
            }
        });
        exitConfirmationDialog.show();
    }

    private void requestExplicitAppExit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
            return;
        }
        finishAffinity();
        finish();
    }

    private boolean isExitDialogShowing() {
        return exitConfirmationDialog != null && exitConfirmationDialog.isShowing();
    }

    private void pauseGameplayForExitDialog() {
        if (surfaceView == null || gameplayPausedForExitDialog) {
            return;
        }
        surfaceView.onPause();
        gameplayPausedForExitDialog = true;
    }

    private void resumeGameplayAfterExitDialog() {
        if (surfaceView == null || !gameplayPausedForExitDialog) {
            return;
        }
        surfaceView.onResume();
        surfaceView.requestFocus();
        gameplayPausedForExitDialog = false;
    }

    private void clearBackExitState() {
        backExitArmed = false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enterImmersiveFullscreen();
            positionDarModeBadge();
        }
    }

    private void enterImmersiveFullscreen() {
        View decorView = getWindow().getDecorView();
        if (decorView == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }
}
