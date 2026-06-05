# REN-002 Camera Equivalence Review

Date: 2026-06-04
Status: Mixed audit + narrow implementation (Stick Size only)
Classification: Mixed approach approved - Android camera controls accepted as enhancements, not strict web parity closure

## 0) Decision update (2026-06-04)

1. `Zoom` is functionally covered by approved Android camera enhancements for now (documentation-only decision).
2. `Stick Size` is approved as a narrow direct parity implementation.
3. `Show Circle` remains ambiguous and is deferred until semantic-equivalence audit confirms safe mapping.
4. `Show Arrow` / `Arrow Size` remain blocked pending dedicated arrow-path audit.
5. `Minimal UI` remains intentionally deferred.

Stick Size implementation mapping:
1. Web slider/default: `60..180`, default `100`.
2. Existing Android baseline joystick radius: `50dp * density`.
3. Applied native mapping: `baseRadiusPx = (stickSize / 100f) * (50f * density)`.
4. This preserves default behavior exactly at `stickSize=100`.

## 1) What web version actually exposes

Source files and labels:
- docs/index.html
  - View & HUD card label: `View & HUD`
  - Controls: `Show Arrow`, `Show Circle`, `Zoom`, `Stick Size`, `Arrow Size`, `Minimal UI`
- docs/js/main.js
  - Applies `settings.zoom` through `cameraController.setZoom(settings.zoom)` and `cameraController.applyZoom()`
  - Reads/persists `showArrow`, `showCircle`, `arrowScale`, `minimalUi`, `stickSize`, `zoom`
- docs/js/modules/settings.js
  - View/HUD-related persisted keys: `zoom`, `arrowScale`, `showArrow`, `showCircle`, `minimalUi`, `stickSize`, `circleScale`, `circleTiltAngle`, `circleTiltModifier`
- docs/js/modules/cameraController.js
  - Camera zoom behavior (`setZoom`, `applyZoom`), no preset/FOV/distance/sensitivity UI contract

Web camera-related settings summary:
- Explicit camera-facing control: `Zoom`
- HUD/view controls in same surface: `Show Arrow`, `Show Circle`, `Arrow Size`, `Stick Size`, `Minimal UI`
- No web UI evidence for: `Default/Tight/Wide` camera presets, `FOV` slider, `Camera Distance` slider, `Camera Sensitivity` slider

## 2) What Android currently exposes

Source files:
- app/src/main/res/layout/activity_main.xml
  - `cameraPresetSpinner`, `cameraFovSeek`, `cameraDistanceSeek`, `cameraSensitivitySeek` in `viewHudCard`
- app/src/main/java/com/l4dar/nativeapp/MainActivity.java
  - Presets `Default`, `Tight`, `Wide`
  - FOV/Distance/Sensitivity sliders with live renderer apply
- app/src/main/java/com/l4dar/nativeapp/core/settings/SettingsManager.java
  - Persisted camera keys (`cameraPreset`, `cameraFov`, `cameraDistance`, `cameraSensitivityMultiplier`) with clamped ranges
- app/src/main/java/com/l4dar/nativeapp/render/L4SurfaceView.java
  - `setCameraOptions(fovDeg, distance, sensitivityMultiplier)`
- app/src/main/java/com/l4dar/nativeapp/render/L4Renderer.java
  - Applies perspective FOV and camera distance/look sensitivity at runtime

Android camera controls summary:
- Camera presets: `Default`, `Tight`, `Wide`
- `FOV`
- `Camera Distance`
- `Camera Sensitivity`
- Persistence: device-scoped camera settings persisted via SettingsManager
- Renderer behavior: camera options pushed to renderer and applied live

## 3) Equivalence table

| Android control/behavior | Web source evidence | Classification | Approval status | Recommendation |
|---|---|---|---|---|
| Camera preset spinner (`Default/Tight/Wide`) | No matching web preset control found | Android enhancement | Approved enhancement | Keep as approved enhancement; does not count toward strict web parity closure |
| FOV slider | Web has `Zoom` only (camera framing related) | Functional equivalent (partial, non-1:1) | Approved enhancement | Keep as approved enhancement; does not count toward strict web parity closure |
| Camera distance slider | No matching web distance slider found | Android enhancement | Approved enhancement | Keep as approved enhancement; does not count toward strict web parity closure |
| Camera sensitivity slider | No matching web camera sensitivity slider found | Android enhancement | Approved enhancement | Keep as approved enhancement; does not count toward strict web parity closure |
| Camera control persistence (`cameraPreset`, `cameraFov`, `cameraDistance`, `cameraSensitivityMultiplier`) | Web persists view/hud settings including `zoom` and HUD toggles | Functional equivalent at persistence layer, but for non-source-backed controls | Approved enhancement | Keep implementation unchanged; enhancement is accepted but not parity-closing |
| Renderer application of camera options | Web camera controller applies zoom only (`setZoom`/`applyZoom`) | Android enhancement (broader camera model) | Approved enhancement | Keep current behavior unchanged; enhancement is accepted but not parity-closing |

## 4) AI-introduced enhancements list

1. Camera preset selector (`Default/Tight/Wide`)
2. FOV slider
3. Camera distance slider
4. Camera sensitivity multiplier slider
5. Expanded renderer camera-option application model tied to these controls

## 5) True parity gaps (web -> Android)

Missing web View & HUD controls in Android surface:
1. `Zoom` control (web `zoomSlider`)
2. `Show Arrow` toggle
3. `Show Circle` toggle
4. `Arrow Size`
5. `Stick Size`
6. `Minimal UI` toggle/status

## 6) Recommendation

1. Do not mark REN-002 closed yet.
2. Keep current runtime and camera-control surface unchanged.
3. Treat current Android camera controls as approved enhancements that do not close strict web parity.
4. Keep REN-002 open until user decides which missing web View & HUD controls are required for Android V1 parity.

## 7) REN-002 split (audit-only tracking)

### Accepted Android enhancements
1. Camera preset selector (`Default/Tight/Wide`)
2. FOV slider
3. Camera distance slider
4. Camera sensitivity multiplier slider
5. Existing persistence and renderer wiring for the above controls

### Remaining web parity gaps
1. `Zoom` control
2. `Show Arrow` toggle
3. `Show Circle` toggle
4. `Arrow Size`
5. `Stick Size` (implemented as narrow parity path; keep REN-002 open for remaining items)
6. `Minimal UI` toggle/status

### Intentionally deferred items
1. No additional camera controls will be added unless explicitly approved.
2. Closure decision for REN-002 is deferred until user confirms which remaining web View & HUD controls are required for Android V1 parity.
