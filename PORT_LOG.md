# L4 Native Android Port Log

_Last updated: 2026-04-02_

## Purpose

This file records the current Android port state using the categories required by `PORT_SPEC.md`:

- **Verified from source**
- **Temporary placeholder**
- **Assumed pending confirmation**

This log exists to reduce parity drift and make Android-specific deviations explicit.

---

## Current Port Phase

**Phase:** Late Milestone 5 / early Milestone 6

Interpretation based on `ANDROID_ARCHITECTURE.md`:
- native scaffold exists
- core math / physics / settings model exist
- renderer exists
- touch + controller pipeline exist
- baseline parity pass is still in progress
- visual hardening has started, but parity documentation is incomplete

---

## Verified from Source

| Item | Evidence | Notes |
|---|---|---|
| Native Android app should preserve core behavior, settings, visual intent, and work offline | `PORT_SPEC.md` sections 1-3 | Primary product goal |
| Web app is the source of truth when Android differs | `PORT_SPEC.md` section 2 | Android should not invent behavior |
| V1 excludes `Rhythm Mode` and `Developer Mode` | `PORT_SPEC.md` section 5 | These should stay out of the initial baseline release |
| `Ring Mode` is deferred until after baseline stability | `PORT_SPEC.md` section 5 | Any Android exposure should be treated cautiously |
| Physics defaults in Android currently match key web defaults for accel/damping | `docs/js/modules/constants.js` `PHYSICS_DEFAULTS`; `app/src/main/java/com/l4dar/nativeapp/core/settings/SettingsManager.java` | `accelPitch=733`, `accelYaw=528`, `accelRoll=898`, `damp=2.96`, `dampDAR=4.35` |
| Android default `touchDeadzone` matches web default | `docs/js/modules/settings.js`; `SettingsManager.java` | Both use `0.09` |
| Android default `gameSpeed` matches web default | `docs/js/modules/settings.js`; `SettingsManager.java` | Both use `1.0` |
| Android default selected car body matches web default | `docs/js/modules/settings.js`; `SettingsManager.java` | Both default to `octane` |
| Android default air-roll toggle mode matches web default | `docs/js/modules/settings.js`; `SettingsManager.java` | Both default to `false` (hold mode) |
| Input architecture is correctly split through an input snapshot boundary | `ANDROID_ARCHITECTURE.md` section 6; `InputController.java` | Android simulation consumes `InputSnapshot` rather than raw UI events |

---

## Temporary Placeholders / Android-Specific Adaptations

| Item | Status | Evidence | Notes |
|---|---|---|---|
| Native ground/grid renderer is a platform-specific visual approximation of the web scene | Temporary placeholder / Android adaptation | `ANDROID_ARCHITECTURE.md` sections 8 and 12; `render/GroundRenderer.java` | Useful for baseline readability, not yet parity-certified |
| Native camera defaults are implementation values, not yet documented as parity-verified | Temporary placeholder | `render/L4Renderer.java` | `cameraYaw`, `cameraPitch`, `CAMERA_RADIUS` should be checked against web intent |
| Android UI layout/grouping is adapted for mobile constraints | Android-specific adaptation | `PORT_SPEC.md` section 7; `MainActivity.java`, Android layouts | Allowed, but should preserve web terminology/meaning |
| Night-mode rendering is implemented natively rather than copied from the web renderer one-to-one | Android-specific adaptation | `MainActivity.java`, `L4Renderer.java`, `GroundRenderer.java` | Acceptable if visual intent remains faithful |

---

## Assumed Pending Confirmation

| Item | Evidence | Why it is still an assumption |
|---|---|---|
| Native body-axis remap now uses `w.x => roll`, `w.y => yaw`, `w.z => pitch` to preserve the source-backed control intent | `L4PhysicsEngine.java`; on-device analog-stick bug reproduction on 2026-03-31 | This corrects the verified pitch/yaw inversion, but broader native-axis regression validation is still recommended |
| Android and web both currently use render-driven variable-`dt` stepping with one physics update per frame and no accumulator | `L4SurfaceView.java`, `L4Renderer.java`, `docs/js/main.js`, `docs/js/modules/physics.js` | This is now source-backed as the current architecture, though exact feel under frame pacing variation still needs runtime validation |
| Hardcoded touch layout geometry (`margin`, `innerPad`, control radii) is acceptable across devices | `InputController.java` `layoutTouchControls()` | Works in practice, but values are not yet source-backed from the web inventory |
| Current gamepad defaults are the right RL-style parity mapping | `SettingsManager.java`; `docs/js/modules/input/gamepadInput.js` | Android uses a simplified binding model that still needs direct comparison to web semantics |
| Persisting DAR state across restart is intended behavior | `SettingsManager.java`, `L4SurfaceView.java` | Useful behavior, but should be confirmed against desired web parity expectations |
| Touch DAR direction option set (`Left`, `Right`, `Normal`) is intended for Android baseline | `SettingsManager.java`, `DARButtonManager.java`, `MainActivity.java` | Present in Android, but should be checked against the current web UI/behavior |

---

## Current Conflicts / Drift Risks

| Issue | Evidence | Impact |
|---|---|---|
| `PORT_LOG.md` was previously empty, leaving assumptions undocumented | file state before this update | Violates `PORT_SPEC.md` engineering rule to document verified/temporary/assumed decisions |
| Timing architecture is implemented but not yet formally documented as approved | `L4Renderer.java`; `ANDROID_ARCHITECTURE.md` section 7 | High risk for parity drift |
| `Ring Mode` UI is present in Android shell even though the spec says it is deferred | `MainActivity.java` contains `ringModeBtn` / `ringModeOverlay`; `PORT_SPEC.md` section 5 | Could create scope drift unless explicitly approved as temporary exposure |
| Some recent UI/visual changes were made for usability but not logged as deviations | recent Android UI work in `MainActivity.java` | Should be documented if kept |

---

## Source-Backed Gamepad Parity Audit (2026-03-31)

### 1. Verified gamepad mappings from web source

Primary sources:
- `docs/js/modules/input/gamepadInput.js`
- `docs/js/modules/input/buttonMapper.js`
- `docs/js/modules/input.js`

#### Default / xinput-style bindings from source

| Action | Binding in source | Controller label from `buttonMapper.js` |
|---|---|---|
| `toggleDAR` | `button 1` | `B` / `Circle` |
| `rollLeft` | `button 5` | `RB` / `R1` |
| `rollRight` | `button 4` | `LB` / `L1` |
| `rollFree` | `button 7` | `RT` / `R2` |
| `boost` | `button 0` | `A` / `Cross` |
| `openMenu` | `button 6` | `LT` / `L2` |
| `pause` | `button 9` | `Start` / `Options` |
| `restart` | `button 10` | `LS Click` / `L3` |
| `retry` | `button 11` | `RS Click` / `R3` |
| `orbitCW` | `button 2` | `X` / `Square` |
| `orbitCCW` | `button 3` | `Y` / `Triangle` |
| `toggleTheme` | `button 8` | `Share` |

#### PS5 preset from source

| Action | Binding in source |
|---|---|
| `toggleDAR` | `button 0` (`Cross`) |
| `rollLeft` | `button 2` (`Square`) |
| `rollRight` | `button 1` (`Circle`) |
| `rollFree` | `button 4` (`L1`) |
| `boost` | `button 5` (`R1`) |
| `openMenu` | `button 9` (`Options`) |

---

### 2. Axis shaping rules from web source

Verified behavior from `docs/js/modules/input/gamepadInput.js` and `docs/js/modules/physics.js`:

- left stick uses raw axes `0/1`; right stick uses raw axes `2/3`
- per stick, magnitude is computed first
- if stick magnitude is **below its deadzone**, it is forced to zero
- if above deadzone, the orchestration layer scales it using sensitivity:
  - `withSensitivity = min(1, magnitude * sensitivity)`
  - `scale = withSensitivity / magnitude`
- axis-bound actions use subtractive deadzone normalization:
  - positive direction: `(val - deadzone) / (1 - deadzone)`
  - negative direction: `(abs(val) - deadzone) / (1 - deadzone)`
- physics then applies another shaping stage for the control vector:
  - `m2 = (clampedMag - stickDeadzone) / (1 - stickDeadzone)`
  - `shaped = pow(max(0, m2), inputPow)`
  - `eff = shaped * stickRange`
- direction mapping in physics is source-backed as:
  - `ux = -jx`
  - `uy = jy`
  - right stick equivalent: `rightUx = -rightStickX`, `rightUy = -rightStickY`

---

### 3. Deadzone behavior from web source

Verified from `docs/js/modules/input/gamepadInput.js`, `docs/js/modules/settings.js`, and `docs/js/modules/physics.js`:

- default left-stick gamepad deadzone: `0.15`
- default right-stick gamepad deadzone: `0.15`
- default touch deadzone: `0.09`
- gamepad deadzones are **per-stick** and setting-driven
- values under deadzone are zeroed before orchestration callbacks
- axis-bound actions rescale from deadzone edge to full intensity
- physics still applies stick shaping using the current stick deadzone formula after orchestration

---

### 4. DAR-related button behavior from web source

Verified from `docs/js/modules/input.js` and `docs/js/modules/input/airRollController.js`:

- air roll values are:
  - `-1 = left`
  - `0 = off`
  - `+1 = right`
  - `2 = free`
- default mode is **hold** (`airRollIsToggle = false`)
- in toggle mode, pressing the same direction again toggles it off
- in hold mode, `rollLeft` / `rollRight` / `rollFree` are driven continuously while pressed
- `rollFree` is a real distinct semantic in the web source (`dir = 2`)
- `toggleDAR` no longer means “hold DAR while pressed”; it flips the overall control mode between **toggle** and **hold**
- if dual-stick mode is enabled and `rightStickAssignment` is set, the **right stick has absolute priority** over DAR button states
- when right-stick DAR takes over, the previous air-roll state is saved and restored when the stick becomes inactive
- analog intensity is supported for DAR actions when the binding is axis-based

---

### 5. Android assumptions currently present

Current Android implementation assumptions observed in:
- `app/src/main/java/com/l4dar/nativeapp/core/input/GamepadInputHandler.java`
- `InputController.java`
- `DARButtonManager.java`
- `SettingsManager.java`

Assumptions currently present:

- Android uses a simplified gamepad model with only:
  - steer axes
  - camera axes
  - forward / reverse trigger axes
  - air roll left / right buttons
- Android assumes a single fixed `STICK_DEADZONE = 0.1f` rather than the web’s per-stick setting-driven `0.15`
- Android assumes right stick is for camera only; there is no source-equivalent `dualStickMode` / `rightStickAssignment` implementation yet
- Android currently assumes DAR intensity is binary (`0` or `1`) for gamepad buttons
- Android uses `touchDarDirection` values `-1`, `0`, `1`, with `0` presented as `Normal`
- Android currently uses `DARButtonManager.NORMAL = 2`, which is a semantic assumption not aligned to the web naming

---

### 6. Conflicts between Android implementation and web source

| Conflict | Web source | Current Android state | Impact |
|---|---|---|---|
| ARL / ARR default button mapping differs | Web default/xinput uses `rollLeft=button 5 (RB)` and `rollRight=button 4 (LB)` | Android defaults use `KEYCODE_BUTTON_X` and `KEYCODE_BUTTON_B` | Gamepad semantics likely drift from source on common controllers |
| `rollFree` is a real web action | Web uses `airRoll = 2` for **free air roll** | Android has no gamepad `rollFree` action and reuses `2` as `NORMAL` touch DAR | Semantic conflict with source meaning |
| `toggleDAR` is a separate source action | Web `toggleDAR` flips hold vs toggle mode | Android has no separate gamepad `toggleDAR` binding/action | Missing parity feature |
| Gamepad deadzone behavior differs | Web uses per-stick setting-driven deadzones (default `0.15`) and sensitivity controls | Android uses fixed `0.1f` deadzone and no sensitivity settings | Input feel can drift from source |
| Right-stick DAR assignment is missing | Web supports `dualStickMode` and `rightStickAssignment` with absolute priority | Android right stick is camera only | Missing source-backed controller behavior |
| Analog DAR intensity support differs | Web supports analog intensity for axis-bound DAR actions | Android DAR intensity is effectively binary for button presses | Reduced parity for analog-style mappings |
| Broader button mapping surface differs | Web also maps `boost`, `openMenu`, `pause`, `restart`, `retry`, `orbitCW`, `orbitCCW`, `toggleTheme` | Android gamepad handler currently focuses on movement/camera/throttle/ARL/ARR only | Functional parity is incomplete |

---

### 7. Recommended implementation milestone after this audit

**Recommended next milestone:** source-aligned gamepad parity implementation pass.

Safest order after this audit:
1. align Android DAR semantics with the web source (`LEFT`, `RIGHT`, `FREE`, `OFF`) and stop overloading `2` with a non-source `Normal` meaning
2. align default gamepad mappings to the verified web source or explicitly document any intentional Android deviation
3. add missing parity-critical controller concepts:
   - `toggleDAR`
   - `rollFree`
   - per-stick deadzones / sensitivity
   - optional `dualStickMode` / `rightStickAssignment` support or a documented defer decision
4. then run on-device controller validation

---

## Controller Parity Implementation Update (2026-03-31)

### Files changed

- `app/src/main/java/com/l4dar/nativeapp/core/settings/SettingsManager.java`
- `app/src/main/java/com/l4dar/nativeapp/core/input/GamepadInputHandler.java`
- `app/src/main/java/com/l4dar/nativeapp/core/input/DARButtonManager.java`
- `app/src/main/java/com/l4dar/nativeapp/core/physics/L4PhysicsEngine.java`
- `app/src/main/java/com/l4dar/nativeapp/MainActivity.java`

### Source-backed changes implemented

- Android air-roll semantics are now aligned to the web source:
  - `-1 = left`
  - `0 = off`
  - `+1 = right`
  - `2 = free`
- The conflicting Android-only `Normal` meaning was removed from the active controller semantics and renamed to **Free** in the touch-facing label path.
- Default ARL / ARR bindings were aligned to the verified web source defaults:
  - `rollLeft -> R1 / RB`
  - `rollRight -> L1 / LB`
- Added the missing parity-critical controller actions in the Android gamepad path:
  - `toggleDAR`
  - `rollFree`
- Replaced the single fixed simplified gamepad deadzone assumption with per-stick stored values:
  - `gpLeftStickDeadzone`
  - `gpRightStickDeadzone`
  - both default to the source-backed web value `0.15`

### Remaining controller parity gaps

These are still intentionally deferred or not yet source-complete:

- no controller remapping UI was added for `toggleDAR` / `rollFree` (per implementation scope)
- `dualStickMode` / `rightStickAssignment` is still not implemented in Android
- analog intensity for axis-bound DAR actions is still simplified compared with the web system
- broader non-core controller actions from the web source remain incomplete in Android:
  - `boost`
  - `openMenu`
  - `pause`
  - `restart`
  - `retry`
  - `orbitCW`
  - `orbitCCW`
  - `toggleTheme`
- on-device controller validation is still required to confirm feel and hardware-event behavior on a real connected gamepad

## DAR Behavior Verification / Parity Pass (2026-03-31)

### DAR behaviors verified against the web source

Verified against `docs/js/modules/input.js`, `docs/js/modules/input/airRollController.js`, and the Android counterparts in `GamepadInputHandler.java` / `DARButtonManager.java`:

- DAR semantic values remain source-aligned:
  - `rollLeft -> -1`
  - `rollRight -> +1`
  - `rollFree -> 2`
  - `off -> 0`
- `toggleDAR` is verified as a **mode switch** between hold and toggle, not a temporary hold-to-enable action.
- In toggle mode, pressing the same directional air-roll action again turns it off; pressing a different one switches the active DAR direction.
- In hold mode, released DAR buttons now resolve to the currently held source-backed direction set instead of depending on press order.

### Remaining DAR behavior drift found during verification

A parity-critical drift was found in `GamepadInputHandler.java`:

- the Android handler used **last-press-wins** behavior for `rollLeft` / `rollRight` / `rollFree` in hold mode
- the Android handler also lacked an explicit edge guard for repeated `onKeyDown()` events in toggle mode
- the web source does not do this; it uses edge-detected action execution plus a fixed hold-mode priority in `handleGamepadAirRollButtons()`

### Parity-critical DAR fix applied

The Android gamepad DAR state machine was corrected to match the web source more closely:

- added edge-gated handling for:
  - `rollLeft`
  - `rollRight`
  - `rollFree`
  - `toggleDAR`
- added source-backed hold-mode priority resolution:
  - `rollLeft` takes precedence over `rollRight`
  - `rollRight` takes precedence over `rollFree`
- switching from toggle mode back to hold mode now immediately resolves to the currently held buttons (or clears DAR if none are held)

### Files changed in this DAR pass

- `app/src/main/java/com/l4dar/nativeapp/core/input/GamepadInputHandler.java`
- `PORT_LOG.md`

### Still-deferred DAR/controller behavior

These remain intentionally deferred after this parity pass:

- `dualStickMode` / `rightStickAssignment` absolute-priority DAR behavior from the web source is still not implemented in Android
- analog intensity for axis-bound DAR bindings is still simplified compared with the web gamepad system
- controller remapping UI for the newly added DAR actions is still not present in Android

## Pitch/Yaw Axis Correction Pass (2026-03-31)

### Root cause - gamepad bug

- The gamepad left-stick channel path is now routing the correct channels into physics:
  - `GamepadInputHandler.onGenericMotionEvent()` -> `leftStickX = lx`, `leftStickY = -ly`
  - `InputController.getInputSnapshot()` -> `joyPixelsX = inputX`, `joyPixelsY = inputY`
- The remaining gamepad issue was a **pitch sign inversion** in `L4PhysicsEngine.performStep()` after the earlier axis-remap correction:
  - yaw was correctly sourced from horizontal input `-jx`
  - pitch was still using `+jy` on the native pitch axis, which produced the opposite nose-up / nose-down result on device

### Root cause - touch bug

- The touch joystick itself was already producing the correct screen-relative channels:
  - `TouchJoystick.updateStickPosition()` -> `normalizedX = dx / baseRadius`, `normalizedY = -(dy / baseRadius)`
  - meaning `left/right` stayed horizontal and `up/down` stayed vertical
- The touch-only swap came later in `InputController.getInputSnapshot()` where `remapTouchAxesForDisplay(rawX, rawY)` was applied before physics consumption.
- In landscape (`Surface.ROTATION_90` / `270`), that remap rotated the control vector and effectively caused:
  - `left/right` touch input to feed the pitch channel
  - `up/down` touch input to feed the yaw channel

### Files changed for this fix

- `app/src/main/java/com/l4dar/nativeapp/core/physics/L4PhysicsEngine.java`
- `app/src/main/java/com/l4dar/nativeapp/core/input/InputController.java`
- `PORT_LOG.md`

### Mapping / sign before

#### Gamepad before
- horizontal: `jx = left/right`
- vertical: `jy = up/down`
- physics mapping:
  - `yaw <- -jx` ✅
  - `pitch <- +jy` ❌ (sign inverted on device)

#### Touch before
- joystick raw output:
  - `rawX = left/right`
  - `rawY = up/down`
- touch snapshot remap in landscape:
  - `inputX,inputY = remapTouchAxesForDisplay(rawX, rawY)`
- resulting effect on device:
  - `left/right` -> pitch ❌
  - `up/down` -> yaw ❌

### Mapping / sign after

#### Gamepad after
- horizontal: `jx = left/right`
- vertical: `jy = up/down`
- physics mapping:
  - `yaw <- -jx`
  - `pitch <- -jy`
- resulting behavior:
  - `left/right` -> yaw
  - `up/down` -> pitch with corrected direction

#### Touch after
- touch path now feeds the joystick output directly into the physics snapshot:
  - `inputX = leftJoystick.getNormalizedX()`
  - `inputY = leftJoystick.getNormalizedY()`
- no display-rotation remap is applied to the physics control vector
- resulting behavior:
  - `left/right` -> yaw
  - `up/down` -> pitch

### Source-backed status

- **Control intent is source-backed:** yes. The web source in `docs/js/modules/physics.js` and the gamepad audit in this log establish that horizontal stick input controls yaw and vertical stick input controls pitch.
- **Gamepad pitch sign correction:** source-backed by the documented intent plus direct on-device verification of the inverted result.
- **Touch channel correction:** source-backed by the Android code inspection showing a touch-only rotation remap that re-swapped the channels after the joystick had already produced correct screen-relative axes.

## Timing / Update-Loop Parity Audit (2026-03-31)

### Android timing model (current)

Verified from `L4SurfaceView.java`, `L4Renderer.java`, and `L4PhysicsEngine.java`:

- render loop is driven by `GLSurfaceView.RENDERMODE_CONTINUOUSLY`
- effective render cadence is the device/native surface refresh rate (typically 60/90/120 Hz)
- simulation is stepped **once per `onDrawFrame()`**
- `deltaTime` is computed from `System.nanoTime()` each frame
- the first frame falls back to `TARGET_DT = 1f / 60f`
- subsequent frames are clamped to `0.008f .. 0.033f` seconds
- no fixed-step accumulator or catch-up loop is present
- physics then applies `adjustedDt = deltaTime * gameSpeed`

### Web timing model (current L4 source)

Verified from `docs/js/main.js` and `docs/js/modules/physics.js`:

- render/update loop is driven by `requestAnimationFrame(tick)`
- simulation is stepped **once per animation frame** via `integrate(dt)` -> `Physics.updatePhysics(dt, ...)`
- `dt` is computed from `performance.now()` frame-to-frame elapsed time
- web clamps `dt` to `0.001 .. 0.033` seconds
- no fixed-step accumulator or catch-up loop is present
- physics then applies `adjustedDt = dt * gameSpeed`

### Confirmed parity matches

- Android and web are both using a **render-driven variable-`dt` model**, not a fixed-step simulation model
- both advance physics once per frame using measured elapsed time
- both clamp unusually large frame gaps to about `33 ms`
- both scale simulation time using the user `gameSpeed` setting inside the physics step
- both architectures are designed to keep motion speed broadly consistent across different refresh rates instead of tying behavior to a hardcoded 60 Hz simulation loop

### Confirmed parity risks

- because both systems are variable-`dt` rather than fixed-step, **frame pacing jitter can still subtly affect**:
  - control feel
  - damping response
  - DAR transient behavior
  - short-term motion integration smoothness
- Android uses a tighter lower clamp (`0.008f`) than the web (`0.001`), so extremely short frames are treated slightly differently between platforms
- Android input and physics are both resolved on the GL render thread at frame boundaries, so input responsiveness is still coupled to render cadence
- the architecture doc's fixed-step recommendation remains a **future design option**, but changing only Android would no longer be parity-preserving because the current web source is also variable-`dt`

### Source-backed recommendation

- **Do not change timing behavior in this milestone.**
- The current Android timing architecture is **broadly aligned with the current web source** and no clear parity-critical timing bug was confirmed by inspection alone.
- If future measured testing shows timing drift, the safest source-backed follow-up would be to validate whether Android should match the web's `dt` clamp bounds more exactly; a broader fixed-step conversion should only be considered if done intentionally and documented against both platforms.

## On-Device Timing Validation (2026-03-31)

### Test method

Android device used: connected `SM-S938U - 16`

Validation steps used for this milestone:
1. read the current Android and web timing code paths (`L4SurfaceView.java`, `L4Renderer.java`, `docs/js/main.js`, `docs/js/modules/physics.js`)
2. force approximate **60 Hz** device conditions using ADB:
   - `settings put system peak_refresh_rate 60.0`
   - `settings put system min_refresh_rate 60.0`
3. launch `com.l4dar.nativeapp/.MainActivity`, let it run for ~8 seconds, then capture:
   - `dumpsys display`
   - `dumpsys gfxinfo com.l4dar.nativeapp`
4. repeat the same process under forced **120 Hz** conditions:
   - `settings put system peak_refresh_rate 120.0`
   - `settings put system min_refresh_rate 120.0`
5. restore system defaults after the test by deleting those settings overrides

Web comparison method for this milestone was source-backed rather than browser-instrumented:
- confirm the web loop still uses `requestAnimationFrame(tick)`
- confirm the web physics step still uses measured frame `dt`
- compare whether either platform contains any frame-count-based scaling that would change motion speed, damping, or DAR behavior across refresh rates

### Observed results

#### ~60 Hz conditions

Observed from on-device ADB capture:
- `mActiveRenderFrameRate=60.000004`
- `mActiveSfDisplayMode` switched to the 60 Hz mode
- `gfxinfo` summary for `com.l4dar.nativeapp`:
  - `Janky frames: 3 (0.42%)`
  - `50th percentile: 5ms`
  - `90th percentile: 6ms`
  - `95th percentile: 6ms`
  - `99th percentile: 7ms`

#### Higher-refresh conditions

Observed from on-device ADB capture under forced 120 Hz:
- `mActiveRenderFrameRate=120.00001`
- `mActiveSfDisplayMode` switched to the 120 Hz mode
- `gfxinfo` summary for `com.l4dar.nativeapp`:
  - `Janky frames: 0 (0.00%)`
  - `50th percentile: 5ms`
  - `90th percentile: 5ms`
  - `95th percentile: 5ms`
  - `99th percentile: 6ms`

### Confirmed parity matches

- Android correctly follows the device refresh mode between ~60 Hz and 120 Hz conditions.
- No measurable frame-pacing regression was observed when moving from ~60 Hz to 120 Hz in the current on-device run.
- The current web and Android sources both use measured elapsed time (`dt`) rather than frame-count-based stepping, so there is no source-backed sign that:
  - control feel
  - damping behavior
  - DAR response
  - motion speed
  should inherently speed up or slow down just because the display refresh is higher.

### Confirmed parity drift

- **No meaningful refresh-rate-dependent parity drift was observed in this milestone's on-device timing validation.**
- The practical checks showed the display mode changing as expected while frame pacing stayed stable, and the source comparison did not reveal a frame-rate-coupled simulation bug.
- The only still-noted architecture difference remains the lower `dt` clamp bound (`0.008` Android vs `0.001` web), but this audit did **not** surface a real-world drift issue from that difference.

### Recommended fix

- **None at this time.**
- Because no meaningful drift was actually observed, there is no source-backed reason in this milestone to change the timing architecture.

## Intentional Android UI Deviation Review (2026-03-31)

### Audit scope

Compared `activity_main.xml` and `MainActivity.java` against `docs/index.html`, `docs/js/modules/settings.js`, `PORT_SPEC.md`, and `ANDROID_ARCHITECTURE.md`.

This milestone was **review-only**: no UI behavior or layout changes were made.

### Confirmed intentional / acceptable deviations

| UI difference | Classification | Evidence | Notes |
|---|---|---|---|
| Explicit top-row `Menu` / `Theme` buttons replace the web's hamburger + icon-only theme controls | Beneficial for usability | `activity_main.xml`; `docs/index.html` (`#menuBtn`, `#themeBtn`) | Better touch discoverability on phones while preserving the same meanings |
| Android menu is an end-aligned full-height panel (`420dp`) rather than the web's wider desktop-style overlay | Required for Android | `activity_main.xml`; `PORT_SPEC.md` section 7; `ANDROID_ARCHITECTURE.md` section 9 | Reasonable mobile adaptation for narrow screens |
| Persistent DAR badge (`darModeLabel`) is surfaced near the touch controls | Beneficial for usability | `activity_main.xml`; `MainActivity.java` `positionDarModeBadge()` | Helpful Android-specific feedback without changing DAR semantics |
| The Android `Rotation` card is intentionally condensed to the touch-relevant baseline controls instead of mirroring every web quick-action button | Beneficial for usability / simplified baseline | `activity_main.xml`; `docs/index.html` Rotation card | Acceptable as long as the reduced surface is treated as a documented mobile simplification, not silent feature parity |
| `Rhythm Mode` and `Developer Mode` are not exposed in the Android shell | Required by V1 scope | `PORT_SPEC.md` section 5; absence from `activity_main.xml` | Correct exclusion, not a parity bug |

### Temporary placeholders still present

| Item | Evidence | Notes |
|---|---|---|
| `View & HUD` card currently contains only the placeholder text `State and controls are implemented in this panel before layout polish.` | `activity_main.xml` `viewHudPlaceholder`; web `docs/index.html` View & HUD card | Explicitly marked placeholder, but still missing the source-backed toggles/sliders |
| `ringModeOverlay` is only a skeletal `Rings Menu` shell with placeholder `Settings` / `Game` / `Audio` labels | `activity_main.xml` | Acceptable only as a documented placeholder for a deferred feature |

### Confirmed parity drift / scope mismatches

| Issue | Evidence | Impact |
|---|---|---|
| `ringModeBtn` is visible in the top action row and opens the placeholder ring overlay even though `Ring Mode` is deferred until after baseline stability | `activity_main.xml`; `MainActivity.java`; `PORT_SPEC.md` section 5 | Premature exposure creates scope drift and surfaces an unfinished UI path |
| `dynamicsCard` is hard-hidden with `android:visibility="gone"` | `activity_main.xml`; `PORT_SPEC.md` section 5 | Baseline dynamics remain in scope, so hiding the card entirely risks underexposing parity-critical settings |
| The `Gamepad` card mixes a touch-only deadzone slider into the gamepad section and omits most of the web's actual gamepad settings surface (`⌨ Controls`, dual-stick, per-stick deadzones, sensitivity, preset) | `activity_main.xml`; `docs/index.html`; `docs/js/modules/settings.js` | Grouping/coverage drift: the card label is source-faithful, but its contents are still incomplete |

### Review conclusion

Current Android UI status after this audit:

- **Required / acceptable Android adaptations exist and are mostly reasonable**
- **Two areas are still clearly placeholder-driven:** `View & HUD` and the ring-mode shell
- **The main parity/scope drift to clean up later is UI exposure, not core behavior:** premature `Ring Mode` entry, hidden `Dynamics`, and incomplete/mis-grouped `Gamepad` content

No implementation change was made in this milestone.

## Android UI Cleanup Pass (2026-03-31)

### Files changed

- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/java/com/l4dar/nativeapp/MainActivity.java`
- `PORT_LOG.md`

### UI drift items resolved in this pass

| Item | Resolution | Evidence |
|---|---|---|
| Premature `Ring Mode` entry point in V1 shell | Hidden / deferred from the top action row so the unfinished mode is no longer surfaced in baseline Android UI | `activity_main.xml` `ringModeBtn`; `MainActivity.java` |
| Hidden `Dynamics` section | Restored as a visible baseline card showing the current source-backed accel / damping values already driving Android physics | `activity_main.xml` `dynamicsCard`; `MainActivity.java` `updateDynamicsSummary()` |
| Placeholder-only `View & HUD` shell | Hidden from the active menu until Android has real source-aligned controls for that section | `activity_main.xml` `viewHudCard` |
| Mis-grouped `Gamepad` card | Removed the touch deadzone slider from `Gamepad`, moved it back into the main control flow, and updated the card to show source-faithful controller terminology/binding summaries | `activity_main.xml`; `MainActivity.java` `updateGamepadBindingsSummary()` |

### Intentionally deferred placeholders after cleanup

| Item | Status | Notes |
|---|---|---|
| `ringModeOverlay` skeleton | Intentionally retained but hidden | Acceptable as a deferred placeholder only because `Ring Mode` is still out of active V1 scope |
| `View & HUD` controls on Android | Still deferred | Hidden instead of shown as placeholder-only content |
| Controller remapping / `Dual Stick` UI | Still deferred | Not added in this pass because the milestone was limited to parity-critical grouping cleanup only |

### Remaining V1 scope mismatches

| Item | Evidence | Impact |
|---|---|---|
| `Gamepad` remains a simplified read-only/status card rather than the full web surface (`⌨ Controls`, `Dual Stick Mode`, per-stick sensitivity, preset selector) | `activity_main.xml`; `docs/index.html` | Meaning is now closer to the web, but controller-settings surface is still incomplete |
| `View & HUD` remains absent from the active Android menu rather than source-complete | hidden `viewHudCard`; web `View & HUD` card in `docs/index.html` | This avoids placeholder drift, but the full section is still deferred |

### Verification

Fresh verification after this pass:

- Ran `./gradlew.bat assembleDebug` in `android-native-l4`
- Result: `BUILD SUCCESSFUL in 2s`

## Remaining Controller / Settings Surface Parity Audit (2026-03-31)

### Audit scope

Compared:
- Android: `MainActivity.java`, `activity_main.xml`, `SettingsManager.java`, `GamepadInputHandler.java`, `InputController.java`, `DARButtonManager.java`
- Web source of truth: `docs/index.html`, `docs/js/modules/settings.js`, `docs/js/modules/controlsMenu.js`, `docs/js/modules/input/gamepadInput.js`, `docs/js/modules/input.js`

This milestone was **audit-only**. No implementation changes were made in this pass.

### Current Android-exposed controller/settings surface

Current Android UI surface relevant to controller/input settings now consists of:

- `Rotation` card
  - `Restart`
  - `Air Roll Mode` (`Hold` / `Toggle`)
  - `Touch Air Roll` direction cycle (`Left` / `Right` / `Free`)
  - `Game Speed` slider
  - `Touch Deadzone` slider
- `Gamepad` card
  - connection/status text
  - read-only default DAR binding summary
  - read-only `Left Stick Deadzone` / `Right Stick Deadzone` values
  - explicit deferred note for remapping / dual-stick UI
- top-row `Theme` toggle
- `Dynamics` summary card (read-only values)

Persisted Android settings currently relevant to controller/input behavior:
- `touchDeadzone`
- `airRollIsToggle`
- `touchDarDirection`
- `gameSpeed`
- `gpLeftStickDeadzone`
- `gpRightStickDeadzone`
- stored gamepad binding key codes for steer/camera/throttle/DAR actions

### Remaining gaps vs. the web source

| Gap | Web source | Current Android state | Classification | Notes |
|---|---|---|---|---|
| Adjustable `Left Stick Deadzone` / `Right Stick Deadzone` controls | Web exposes editable sliders and persisted values in the `Gamepad` card | Android only shows these values as read-only text | **Parity-critical for V1** | These settings already exist in `SettingsManager.java` and directly affect controller feel, but the Android UI still cannot edit them |
| `gpEnable` on/off toggle | Web exposes a real `Enabled / Disabled` button | Android shows status text only; there is no actual enable/disable setting or control | **Acceptable defer for V1** | Controller support still works by default, but the feature surface is narrower |
| `⌨ Controls` remapping UI / full `gpBindings` editing | Web exposes a controls modal with remapping + reset flows via `controlsMenu.js` | Android persists binding codes internally but exposes only a summary string | **Placeholder / incomplete** | The settings plumbing exists partially, but the user-facing surface is not there yet |
| `Dual Stick Mode` and `rightStickAssignment` | Web exposes the toggle and right-stick assignment options | Android has no surfaced setting and no matching behavior path | **Acceptable defer for V1** | Source default is off, so this can remain deferred if documented |
| `gpLeftStickSensitivity` / `gpRightStickSensitivity` | Web exposes editable sensitivity sliders and uses them in `gamepadInput.js` | Android has no setting schema or UI for sensitivity tuning | **Acceptable defer for V1** | Missing tuning surface narrows parity, but default behavior remains usable for baseline play |
| `gpPreset` selector | Web exposes preset selection (`PS5 / DualSense`, `Generic XInput`) | Android uses fixed source-aligned default bindings with no preset selector | **Acceptable defer for V1** | Android keycode handling is already more platform-specific than browser mappings |
| Broader controller action surface (`boost`, `pause`, `openMenu`, `retry`, `orbitCW`, `orbitCCW`, `toggleTheme`) in the controls/settings UI | Web exposes these as remappable actions in the controls menu | Android UI only summarizes core DAR bindings and `Restart` | **Placeholder / incomplete** | The baseline app remains usable, but the settings/controls surface is still reduced |

### Settings that are still mislabeled, mis-grouped, or missing source-backed meaning

| Item | Issue | Classification |
|---|---|---|
| `gamepadStatusText` (`Enabled (waiting for gamepad)`) | Reads like part of an enable/disable control even though Android currently exposes no actual `gpEnable` toggle | **Accidental drift** |
| `gpLeftStickDeadzone` / `gpRightStickDeadzone` labels | Real runtime settings are shown as informational text only rather than editable settings like the web source | **Missing source-backed meaning** |
| `Gamepad` card as a whole | Mixes true status, read-only summaries, and defer text instead of being a complete settings surface | **Placeholder / incomplete** |
| `Touch Air Roll` cycling button | Not a one-to-one copy of the web's separate `Air Roll Left / Right / Free` controls, but it preserves the meaning for touch use on Android | **Acceptable Android adaptation** |

### Source-backed settings that should be exposed for baseline parity but still are not

| Setting | Source evidence | Current Android status | Priority |
|---|---|---|---|
| `gpLeftStickDeadzone` | Web `Gamepad` card in `docs/index.html`; runtime usage in `docs/js/modules/input/gamepadInput.js` | Exists in `SettingsManager.java` and affects live controller input, but is not user-adjustable in Android UI | **V1-critical** |
| `gpRightStickDeadzone` | Web `Gamepad` card in `docs/index.html`; runtime usage in `docs/js/modules/input/gamepadInput.js` | Exists in `SettingsManager.java` and affects live controller input, but is not user-adjustable in Android UI | **V1-critical** |
| `gpEnable` | Web `gpEnable` button and `gpEnabled` setting path | Not exposed as a true Android setting/control | Acceptable defer |
| `dualStickMode` / `rightStickAssignment` | Web `controlsMenu.js`, `gamepadInput.js`, and `settings.js` | Not surfaced in Android and not implemented as a user setting path | Acceptable defer |
| `gpLeftStickSensitivity` / `gpRightStickSensitivity` | Web `Gamepad` card sliders and `settings.js` validation | No Android schema/UI exposure yet | Acceptable defer |
| `gpPreset` | Web preset selector in `docs/index.html` and `gamepadInput.js` | No Android preset selector exposed | Acceptable defer |

### V1-critical gaps after this audit

At this point, the clearest remaining **V1-critical** controller/settings surface gap is:

1. **make the existing per-stick gamepad deadzone settings user-adjustable in Android rather than read-only**

Why this is V1-critical:
- the web source already exposes both deadzone controls directly
- Android already stores and uses both values in runtime input handling
- deadzone tuning materially affects controller usability and feel

### Safe defers for V1

These are still reasonable to defer without violating the current baseline scope:

- `gpEnable` toggle
- full `Controls` remapping UI
- `Dual Stick Mode`
- `rightStickAssignment`
- stick sensitivity sliders
- preset selection UI
- broader controller action remapping surface beyond the core DAR/baseline actions

### Recommended next implementation pass

**Recommended next milestone:** narrow **controller/settings surface parity pass** only.

Safest order:
1. make `gpLeftStickDeadzone` and `gpRightStickDeadzone` editable in the Android UI
2. clean up the `Gamepad` card wording so status text does not imply a missing toggle/control
3. keep remapping UI, dual-stick features, sensitivity tuning, and preset selection explicitly deferred unless scope is widened

## Safest Next Milestone

1. perform the narrow controller/settings surface parity pass above
2. keep `Ring Mode`, `Rhythm Mode`, developer-only surfaces, rendering/timing changes, and unrelated polish out of scope until explicitly resumed

This keeps the work aligned with `PORT_SPEC.md`: parity before polish, and documented Android-specific deviations instead of silent drift.

## Physics Core Checkpoint / Parity Harness Audit (2026-04-01)

### Audit scope

Compared:
- Android: `L4PhysicsEngine.java`, `L4PhysicsDefaults.java`, `Vec3.java`, `Quat.java`, `L4Renderer.java`, `SettingsManager.java`, `PhysicsIntegrationTest.java`
- Web source of truth: `docs/js/modules/physics.js`, `docs/js/modules/constants.js`, `docs/js/modules/settings.js`, `docs/js/main.js`

This milestone was **audit-only**. No runtime implementation changes were made in this pass.

### Current Android physics inventory and web mapping

| Android class / function | Current role | Web counterpart | Checkpoint status |
|---|---|---|---|
| `core/physics/L4PhysicsEngine.performStep()` | Main angular-velocity update, damping, caps, quaternion integration | `updatePhysics()` in `docs/js/modules/physics.js` | Primary parity surface |
| `core/physics/L4PhysicsDefaults.java` | Native default constants (`accel*`, `damp*`, `wMax*`, `DAR_ROLL_SPEED`) | `PHYSICS_DEFAULTS`, `DAR_ROLL_SPEED`, `INPUT_HISTORY_SIZE` in `docs/js/modules/constants.js` | Source-backed |
| `core/math/Vec3.java` | Minimal vector math backing angular velocity/state | `THREE.Vector3` usage in `physics.js` | Safe native math substitute |
| `core/math/Quat.java` | Minimal quaternion storage + normalization/multiplication | `THREE.Quaternion` usage in `physics.js` | Safe native math substitute |
| `render/L4Renderer.onDrawFrame()` | Render-driven timing source and single physics step per frame | `tick()` in `docs/js/main.js` calling `Physics.updatePhysics(...)` | Broadly aligned |
| `core/settings/SettingsManager.java` | Persistent physics settings/defaults | `docs/js/modules/settings.js` | Partially aligned; some web tunables still stay fixed natively |
| `app/src/androidTest/java/com/l4dar/nativeapp/PhysicsIntegrationTest.java` | CSV-based Android parity harness | Web-generated `L4_*.csv` reference data under `automated_tests/` | Useful smoke coverage, not yet a full trajectory parity gate |

### Exact parity-confirmed areas

| Area | Evidence | Classification | Notes |
|---|---|---|---|
| Core defaults for accel, damping, brake, and angular-velocity caps | `L4PhysicsDefaults.java`; `docs/js/modules/constants.js` | **Exact parity** | `733 / 528 / 898`, `damp=2.96`, `dampDAR=4.35`, `brake=0.0`, `wMax*=5.5`, `DAR_ROLL_SPEED=5.5` match |
| PD controller gains and clamped acceleration path | `L4PhysicsEngine.java`; `docs/js/modules/physics.js` | **Exact parity** | Both use `KP=200`, `KD=0`, per-axis clamp to accel limits, then `w += a * adjustedDt` |
| DAR pitch/yaw budget sharing and cap order | `L4PhysicsEngine.java`; `docs/js/modules/physics.js` | **Exact parity** | Both cap DAR pitch/yaw to `min(wMax*, wMax * 0.50)`, pre-normalize the target vector, then apply per-axis caps before the global DAR-only cap |
| Quaternion integration order | `L4PhysicsEngine.integrateQuaternion()`; `docs/js/modules/physics.js` | **Exact parity** | Same first-order `q += 0.5 * q * omega_body * dt`, then normalize |
| Practical DAR damping behavior | `L4PhysicsEngine.java`; `docs/js/modules/physics.js` | **Exact parity** | Both keep release damping off while DAR is active; `dampDAR` remains stored but is not behaviorally exercised by the current `noInput` path |

### Safe native adaptations / low-risk differences

| Difference | Evidence | Classification | Notes |
|---|---|---|---|
| Native body-axis remap (`w.x => roll`, `w.y => yaw`, `w.z => pitch`) | `L4PhysicsEngine.java` comments + target mapping | **Safe native adaptation** | The axis names differ from the web internals, but the control intent and quaternion derivative remain source-aligned |
| Variable-`dt` lower clamp floor is `0.008f` natively vs `0.001` on web | `L4Renderer.java`; `docs/js/main.js` | **Low-risk parity difference** | Both are still render-driven, one-step-per-frame, no-accumulator loops; the only mismatch is the lower floor |
| Physics is split away from visual/ring-mode logic in native code | `L4PhysicsEngine.java`; `L4Renderer.java`; `physics.js` | **Safe architecture adaptation** | Android separation is cleaner, but still maps back to the same core update stages |

### Confirmed parity risks / drift

| Issue | Evidence | Classification | Impact |
|---|---|---|---|
| Native 3-frame input smoothing is active, but the web `smoothInput()` helper is currently defined and never called | `L4PhysicsEngine.java` calls `smoothInput(...)`; `docs/js/modules/physics.js` only defines `smoothInput()` and has no callsite | **Confirmed drift** | Adds extra response lag / shaping that is not source-backed in the current web loop |
| Near-center release threshold differs (`eff >= 0.01f` native vs `eff >= 0.02` web) | `L4PhysicsEngine.java`; `docs/js/modules/physics.js` | **Minor confirmed drift** | Changes exactly when the system transitions into release damping near stick center |
| Native physics still hard-wires several source-backed web tunables to defaults | `L4PhysicsEngine.java`; `SettingsManager.getSetting(...)`; `docs/js/modules/settings.js` | **Parity risk** | `inputPow`, `stickRange`, `brakeOnRelease`, `wMax`, `wMaxPitch`, `wMaxYaw`, and `wMaxRoll` are part of the web model but are not dynamically surfaced into the native physics step |
| Right-stick / dual-stick rotation path from the web physics flow is not present in the native physics core | `docs/js/modules/physics.js`; `InputSnapshot.java`; `L4PhysicsEngine.java` | **Known scoped defer** | Already documented in the controller/settings audit; not a new surprise, but it remains a behavior gap vs the full web source |

### Parity harness checkpoint

| Harness item | Current state | Classification | Notes |
|---|---|---|---|
| `PhysicsIntegrationTest.java` loads the web-generated CSV references from Android assets | Present and source-backed | Useful baseline smoke test | Good foundation for future parity gating |
| The harness currently resets `physicsEngine` and `carQuaternion` **for every CSV row** before a single `performStep(...)` | `PhysicsIntegrationTest.validateScenario()` | **Confirmed harness weakness** | This validates one-frame response from identity repeatedly, not a continuous multi-frame trajectory |
| The harness only exercises a fixed `DT = 1/60f` path | `PhysicsIntegrationTest.java`; `L4Renderer.java`; `docs/js/main.js` | **Parity risk** | It does not currently validate the approved variable-`dt` runtime path or clamp-floor differences |
| The harness allows up to 5% diverged frames | `PhysicsIntegrationTest.java` | Acceptable temporary tolerance | Fine for smoke coverage, but too soft to be treated as the final parity gate on its own |

### Checkpoint conclusion

At this checkpoint:

- **The core native physics loop is still broadly source-aligned in its main structure**:
  - variable-`dt`
  - `adjustedDt = dt * gameSpeed`
  - PD-controlled angular acceleration
  - DAR budget sharing
  - cap ordering
  - quaternion integration

- **The main correction-needed drift is now narrower and more specific**:
  1. active native input smoothing that the current web source does not actually use
  2. the small `0.01` vs `0.02` release-threshold mismatch
  3. the parity harness being too weak to certify continuous multi-frame behavior

### Required correction pass before more feature work

**Recommended next milestone:** narrow **physics correction / parity harness hardening pass** only.

Safest order:
1. remove or explicitly justify the native rolling-average smoothing so the control path is source-backed again
2. align the native near-center `hasStickInput` threshold with the current web source
3. strengthen `PhysicsIntegrationTest.java` so it validates continuous multi-frame scenarios without resetting state each row
4. only after that, decide whether the remaining fixed-default tunables (`inputPow`, `stickRange`, `wMax*`, `brakeOnRelease`) should stay intentionally fixed for V1 or be fully wired through to match the web model

This keeps the port aligned with `PORT_SPEC.md`: verify the physics core first, then continue feature work on top of a documented baseline.

## Narrow Physics Correction / Parity-Harness Hardening Pass (2026-04-01)

### Scope and guardrails

Implemented only the three correction items from the latest physics checkpoint audit, in the required order:

1. removed non-source-backed native input smoothing
2. aligned near-center release threshold with web source
3. hardened `PhysicsIntegrationTest.java` to validate continuous trajectory behavior

No UI, rendering, controller mapping, or timing-architecture feature work was added in this pass.

### Files changed

- `app/src/main/java/com/l4dar/nativeapp/core/physics/L4PhysicsEngine.java`
- `app/src/androidTest/java/com/l4dar/nativeapp/PhysicsIntegrationTest.java`
- `PORT_LOG.md`

### Corrected parity items

| Corrected item | Change made | Source backing |
|---|---|---|
| Native 3-frame smoothing drift | Removed active rolling-average stick smoothing from `L4PhysicsEngine.performStep()` and now consume raw stick values directly (`jx = input.joyPixelsX`, `jy = input.joyPixelsY`) | Web `docs/js/modules/physics.js` currently defines `smoothInput()` but does not call it from `updatePhysics()` |
| Near-center release threshold drift | Changed native `hasStickInput` threshold from `eff >= 0.01f` to `eff >= 0.02f` | Web `docs/js/modules/physics.js` uses `const hasStickInput = eff >= 0.02;` |
| Harness weakness (single-step only) | `PhysicsIntegrationTest.validateScenario()` now resets state once per scenario, then steps through all CSV rows sequentially | Matches intended parity validation of integrated trajectory behavior across frames |

### Intentionally left unchanged

The following were intentionally not changed in this milestone to keep scope narrow:

- physics equations, PD gains, DAR cap ordering, and quaternion integration math
- render/timing architecture (`L4Renderer` variable-`dt` path and clamp policy)
- controller mappings, UI settings surface, and deferred dual-stick/controller features
- broader settings wiring (`inputPow`, `stickRange`, `wMax*`, `brakeOnRelease`) beyond the three audited correction items

### Parity harness status after this pass

- The harness is now a **real continuous-trajectory gate for fixed-step (1/60) CSV scenarios** because it validates integrated multi-frame progression without per-row reset.
- It is still **limited** for full runtime-certification:
  - it remains fixed-step only (`DT = 1/60f`), not variable-`dt`
  - it validates angular-velocity trajectory parity only within current tolerance policy

### Recommended next milestone

**Recommended next milestone:** narrow **physics settings-surface parity decision pass** only.

Safest order:
1. explicitly decide (and document) whether `inputPow`, `stickRange`, `wMax*`, and `brakeOnRelease` should remain intentionally fixed for V1 or be wired through to runtime settings parity
2. if kept fixed, mark this as an approved Android adaptation in the log
3. if not kept fixed, implement only that settings-to-physics wiring and re-run parity tests

## V1 Scope-Lock and Remaining Gaps Review (2026-04-01)

### Scope and method

Review-only pass. No runtime code was changed.

Inputs reviewed:
- `PORT_SPEC.md`
- `ANDROID_ARCHITECTURE.md`
- this `PORT_LOG.md` including the latest milestone sections
- current Android physics/input/UI/rendering/runtime files

Decision goal:
- identify only what remains for a coherent V1 baseline
- stop reopening areas already settled unless a concrete unresolved limitation is already logged

### V1 readiness by subsystem

| Subsystem | Classification | Evidence snapshot | V1 scope-lock decision |
|---|---|---|---|
| Physics core | **Blocked by unresolved parity question** | `L4PhysicsEngine.java` is now source-aligned on the recent smoothing + threshold corrections; core constants and PD/cap/quaternion path remain stable | Keep physics equations locked. Resolve one open policy question only: whether web tunables (`inputPow`, `stickRange`, `wMax*`, `brakeOnRelease`) stay intentionally fixed or must be wired for V1 parity |
| Timing/runtime behavior | **V1-ready** | Render-driven variable-`dt` loop in `L4Renderer.onDrawFrame()` matches current web architecture and was already validated in prior logged timing pass | Lock timing architecture for V1; no fixed-step redesign in baseline scope |
| Parity harness | **Needs one more implementation pass** | `PhysicsIntegrationTest` now validates continuous trajectories, but remains fixed-step only and soft-tolerance smoke style | Add one narrow hardening pass so the harness can gate the agreed baseline with stronger confidence |
| Touch controls | **V1-ready** | Touch joystick and touch DAR pipeline are in place and persisted settings are active (`touchDeadzone`, touch DAR direction/mode) | Keep touch path locked; no new touch feature expansion |
| Gamepad controls | **Needs one more implementation pass** | Core DAR/button semantics are aligned, but the Android UI still exposes gamepad deadzones as read-only and not editable | Implement only the missing V1-critical gamepad settings surface needed for baseline usability |
| DAR behavior | **V1-ready** | Source-aligned DAR states and hold/toggle behavior are implemented in current handlers; recent DAR parity fixes are already logged | Lock DAR behavior for V1; defer dual-stick DAR assignment path |
| Settings surface | **Needs one more implementation pass** | Settings persistence exists, but gamepad settings exposure is still incomplete vs baseline parity needs | Complete the narrow gamepad deadzone editing surface and keep broader controls menu deferred |
| UI structure | **V1-ready** | V1 cleanup already hid deferred Ring entry and placeholder-only view surface while preserving main baseline cards | Keep UI structure locked; avoid adding new sections during baseline closure |
| Rendering baseline | **V1-ready** | Native renderer, mesh loading, ground grid, camera, and day/night baseline are present and functional | Treat current renderer as baseline-complete for V1; defer visual polish |
| Offline / standalone readiness | **V1-ready** | Local assets are bundled under `app/src/main/assets`; no runtime web/network client usage found in `app/src/main`; native activity launch path is present | Lock offline architecture as complete for V1 baseline |

### Highest-value remaining implementation gaps (only)

1. Make gamepad left/right deadzones user-editable in Android UI (currently read-only despite being runtime settings).
2. Harden `PhysicsIntegrationTest` from smoke-style parity to stronger baseline gate coverage for the approved runtime envelope.
3. Close the open physics parity policy decision for web tunables (`inputPow`, `stickRange`, `wMax*`, `brakeOnRelease`) and implement only if the decision requires wiring.

### Next 3 milestones (ranked)

1. **Controller/settings surface closure pass**
  - implement editable `gpLeftStickDeadzone` and `gpRightStickDeadzone`
  - clean any gamepad card wording that implies unsupported controls
2. **Parity harness confidence pass**
  - strengthen `PhysicsIntegrationTest` to better certify baseline parity behavior (without changing runtime physics)
3. **Physics tunables policy closure pass**
  - finalize and log whether `inputPow`, `stickRange`, `wMax*`, `brakeOnRelease` are fixed-for-V1 or wired-for-parity
  - only implement wiring if explicitly chosen

### Deferred items (safe to defer after scope-lock)

- Full controller remapping UI (`⌨ Controls` parity surface)
- `dualStickMode` / `rightStickAssignment`
- gamepad sensitivity sliders and preset selector
- broader non-core controller actions (`boost`, `pause`, `openMenu`, `retry`, orbit actions, theme remap)
- View/HUD full control surface restoration
- Ring Mode implementation
- visual polish beyond current baseline renderer

### Excluded items (V1)

- Rhythm Mode
- Developer Mode

### Recommended next implementation pass

**Controller/settings surface closure pass only.**

Guardrails:
- no physics math changes
- no timing architecture changes
- no rendering/UI expansion outside gamepad deadzone editability and related copy cleanup
- keep all currently deferred/excluded items out of scope

## Controller/Settings Surface Closure Pass (2026-04-01)

### Scope and guardrails

Implemented only the V1-critical controller/settings surface items from the scope-lock review:

1. exposed runtime-backed gamepad deadzone controls in Android UI
2. aligned labels/grouping terminology with the web Gamepad section as closely as practical in current Android scope
3. wired controls through `MainActivity.java` -> `SettingsManager.java` -> existing runtime gamepad read path

No changes were made to physics math, DAR semantics, timing architecture, remapping UI, or parity harness.

### Files changed

- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/java/com/l4dar/nativeapp/MainActivity.java`
- `app/src/main/java/com/l4dar/nativeapp/core/settings/SettingsManager.java`
- `PORT_LOG.md`

### Settings surface gaps resolved

| Gap | Resolution | Evidence |
|---|---|---|
| `gpLeftStickDeadzone` exposed as read-only text only | Added editable `SeekBar` control in `Gamepad` card and bound it to persisted setting updates | `activity_main.xml`, `MainActivity.java`, `SettingsManager.java` |
| `gpRightStickDeadzone` exposed as read-only text only | Added editable `SeekBar` control in `Gamepad` card and bound it to persisted setting updates | `activity_main.xml`, `MainActivity.java`, `SettingsManager.java` |
| Status wording implied a missing enable toggle path | Updated card status wording to a neutral status label (`Status: Waiting for gamepad`) without implying an unsupported on/off control | `activity_main.xml`, `MainActivity.java` |

### Runtime wiring confirmation

- UI slider updates call:
  - `SettingsManager.setGpLeftStickDeadzone(...)`
  - `SettingsManager.setGpRightStickDeadzone(...)`
- Values persist via existing lifecycle save/load flow (`L4SurfaceView.savePersistentState()` / `restorePersistentState()` with `SettingsManager.save()` / `load()`).
- Runtime consumption remains unchanged and live via existing gamepad input path:
  - `GamepadInputHandler.getLeftStickDeadzone()` -> `settings.getGpLeftStickDeadzone()`
  - `GamepadInputHandler.getRightStickDeadzone()` -> `settings.getGpRightStickDeadzone()`

### Still intentionally read-only / deferred

- controller remapping UI (`⌨ Controls`) remains deferred
- `dualStickMode` / `rightStickAssignment` remains deferred
- gamepad sensitivity sliders and preset selector remain deferred
- broader non-core controller action remap surface remains deferred

### Recommended next milestone

**Parity harness confidence pass only.**

Keep controller/settings scope locked after this pass and harden parity gate confidence without changing runtime physics behavior.

## Parity Harness Confidence Pass (2026-04-01)

### Scope and guardrails

Implemented harness-only confidence improvements. Runtime app behavior was not changed.

No changes were made to:
- physics runtime equations
- controller mappings
- DAR semantics
- rendering
- UI

### Files changed

- `app/src/androidTest/java/com/l4dar/nativeapp/PhysicsIntegrationTest.java`
- `PORT_LOG.md`

### Harness improvements made

| Improvement | What changed | Confidence impact |
|---|---|---|
| CSV integrity gate | Added structural validation for frame continuity, positive/expected time deltas, and reference magnitude consistency before simulation comparison | Prevents false parity confidence from malformed reference assets |
| Stronger trajectory quality metrics | Added MAE/RMSE/max-abs tracking per angular-velocity axis and magnitude | Moves gate beyond simple per-frame pass/fail counting |
| Stronger divergence policy | Replaced prior 5% diverged-frame allowance with a tighter 2% limit and added max consecutive-divergence bound | Better catches sustained drift across trajectory segments |
| Magnitude-aware checking | Added explicit magnitude comparison and magnitude MAE gating in addition to per-axis checks | Verifies vector-level behavior, not just axis-wise tolerance independently |
| Final-state confidence check | Added end-of-scenario strict assertions on final `wx/wy/wz/magnitude` | Catches cumulative integration drift that could hide in average-only metrics |
| CSV-driven `dt` stepping | Harness now uses per-row `time` deltas (with validation) instead of blindly forcing one constant `dt` value | Better reflects reference trajectory timing semantics while remaining compatible with current fixed-step assets |

### Remaining confidence limitations

- Reference assets still represent fixed-step trajectories (current CSVs are 1/60 spacing only).
- Harness still validates angular-velocity trajectories only; no direct quaternion/orientation reference data is currently present in assets.
- Confidence still depends on the quality and scenario coverage breadth of the web-generated CSV set.

### V1 confidence assessment

Parity confidence is now **sufficient for V1 baseline confidence gating** for the approved fixed-step trajectory asset set, given:
- stronger statistical and structural gate checks
- stricter divergence limits
- explicit final-state assertions

Residual limitation remains documented: variable-`dt` parity confidence is indirect until variable-step reference assets are added.

### Recommended next milestone

**Physics settings-surface parity decision pass only** (already identified in prior scope-lock milestones):
- decide and document whether `inputPow`, `stickRange`, `wMax*`, and `brakeOnRelease` remain intentionally fixed for V1 or are wired through
- keep harness/runtime scope otherwise locked

## Physics Settings-Surface Parity Decision Pass (2026-04-01)

### Scope and guardrails

Decision + review only. No runtime physics behavior was changed.

No changes were made to:
- runtime physics equations
- controller mappings
- DAR semantics
- rendering
- feature scope

### 1) Current Android runtime-backed physics tunables inventory

Backed by persisted settings and consumed in runtime path:

- `maxAccelPitch`
- `maxAccelYaw`
- `maxAccelRoll`
- `damp`
- `dampDAR` (persisted/runtime-backed, but current behavior path does not meaningfully exercise DAR release damping)
- `touchDeadzone`
- `gameSpeed`

Fixed/default-only in runtime (not surfaced as editable Android physics settings):

- `inputPow` (fixed as `L4PhysicsDefaults.INPUT_CURVE` equivalent behavior path absent in native)
- `stickRange` (fixed as `L4PhysicsDefaults.STICK_RANGE` equivalent behavior path absent in native)
- `brakeOnRelease` (fixed `L4PhysicsDefaults.BRAKE_ON_RELEASE`)
- `wMax`
- `wMaxPitch`
- `wMaxYaw`
- `wMaxRoll`

### 2) Web vs Android settings-surface comparison (requested groups)

| Group | Web surface | Android current state | Decision summary |
|---|---|---|---|
| Accel values | Exposed in Dynamics surface (developer-oriented) | Runtime-backed and persisted; currently shown as read-only summary | Keep fixed/hidden for V1 clarity |
| Damping values | Exposed in Dynamics surface (developer-oriented) | Runtime-backed and persisted; shown as read-only summary | Keep fixed/hidden for V1 clarity |
| Input shaping (`inputPow`, `stickRange`) | Exposed in web Dynamics | Not runtime-backed in Android settings path | Intentional fixed for V1 |
| Max angular speed / caps (`wMax*`) | Exposed in web Dynamics | Fixed constants in Android runtime | Intentional fixed for V1 |
| Game speed related controls | Exposed in web Rotation card | Exposed and editable in Android Rotation card | V1 parity-critical already satisfied |

### 3) Tunable classification

| Tunable | Classification | Rationale |
|---|---|---|
| `gameSpeed` | **V1 parity-critical to expose** | Directly affects simulation pace and is part of baseline user control semantics in web and Android |
| `touchDeadzone` | **V1 parity-critical to expose** | Core baseline control-feel setting for touch input; already exposed |
| `maxAccelPitch` | **Intentionally fixed for Android V1** | Dynamics/developer-oriented tuning; not required for baseline use parity when source defaults already match |
| `maxAccelYaw` | **Intentionally fixed for Android V1** | Same as above |
| `maxAccelRoll` | **Intentionally fixed for Android V1** | Same as above |
| `damp` | **Intentionally fixed for Android V1** | Keep source-matched default for baseline behavior stability |
| `dampDAR` | **Intentionally fixed for Android V1** | Keep default alignment; avoid exposing a control whose runtime impact is limited in current behavior path |
| `inputPow` | **Safe defer** | Not currently runtime-wired in Android; source-backed default is stable for V1 baseline |
| `stickRange` | **Safe defer** | Not currently runtime-wired in Android; source-backed default is stable for V1 baseline |
| `wMax` | **Safe defer** | Fixed source-backed default sufficient for V1 baseline |
| `wMaxPitch` | **Safe defer** | Fixed source-backed default sufficient for V1 baseline |
| `wMaxYaw` | **Safe defer** | Fixed source-backed default sufficient for V1 baseline |
| `wMaxRoll` | **Safe defer** | Fixed source-backed default sufficient for V1 baseline |
| `brakeOnRelease` | **Safe defer** | Fixed default at `0.0` matches current source-backed baseline behavior |

### 4) Accidental drift decisions

| Item | Classification | Decision |
|---|---|---|
| Visible read-only `Dynamics` card summaries in Android menu | **Accidental drift** (surface clarity mismatch) | For V1 clarity, these should be hidden/de-emphasized rather than presented as active settings while remaining non-editable |
| `dampDAR` shown as if user-facing tuning control surface were active | **Accidental drift** (presentation mismatch) | Keep fixed and avoid presenting it as an active V1-adjustable setting |

### 5) Recommended V1 physics settings surface

Expose for V1 baseline:
- `gameSpeed`
- `touchDeadzone`

Keep intentionally fixed (source-default aligned) for V1:
- `maxAccelPitch`, `maxAccelYaw`, `maxAccelRoll`
- `damp`, `dampDAR`

Defer (do not surface in V1 UI):
- `inputPow`, `stickRange`
- `wMax`, `wMaxPitch`, `wMaxYaw`, `wMaxRoll`
- `brakeOnRelease`

### Recommended next implementation pass

**V1 physics settings-surface clarity cleanup pass only.**

Scope for that pass:
1. hide/de-emphasize `Dynamics` read-only surface for V1 clarity
2. keep `gameSpeed` and `touchDeadzone` as the only user-facing baseline physics controls
3. do not alter runtime physics behavior while applying this UI-surface clarity decision

## V1 Physics Settings-Surface Cleanup Pass (2026-04-01)

### Scope and guardrails

Implemented only the previously-decided V1 settings-surface cleanup.

No changes were made to:
- runtime physics behavior
- controller mappings
- DAR semantics
- timing
- rendering

### Files changed

- `app/src/main/res/layout/activity_main.xml`
- `PORT_LOG.md`

### V1 settings-surface cleanup completed

Completed outcomes:

1. Kept exposed for V1:
  - `gameSpeed`
  - `touchDeadzone`
2. Hidden accidental-drift `Dynamics` surface from active V1 menu UI for clarity.
3. Preserved intentionally fixed tunables as runtime-backed but not user-exposed.
4. Kept deferred tunables out of V1 UI.

### What remains intentionally fixed (runtime-backed, non-user-exposed)

- `maxAccelPitch`
- `maxAccelYaw`
- `maxAccelRoll`
- `damp`
- `dampDAR`

### What remains deferred (out of V1 UI)

- `inputPow`
- `stickRange`
- `wMax`
- `wMaxPitch`
- `wMaxYaw`
- `wMaxRoll`
- `brakeOnRelease`

### Recommended next milestone

**Physics tunables runtime-parity policy implementation pass only** (if scope is explicitly approved):
- decide whether any deferred/fixed tunables should be wired through for post-V1 parity expansion
- otherwise keep current fixed-default baseline intact

## V1 Device Bug-Bash and Lifecycle Validation Runbook Execution (2026-04-01)

### Scope and guardrails

Executed the approved V1 device bug-bash and lifecycle runbook in required order.

No feature work, redesign, or polish changes were made in this milestone.

### Test devices / profiles used

- Device: `SM-S938U` (ADB serial: `R5CY20ETGPL`)
- Android user/profile: `u0`
- App build/install path used: `:app:installDebug` from `android-native-l4`
- Orientation observed during most checks: landscape (`rotation=1` in UI dump)
- Input profile available during this run: touch + system keys (no active external gamepad device detected)

### Scenarios executed (required order)

| Order | Scenario | Result | Evidence summary |
|---|---|---|---|
| 1 | Startup and first-interaction checks | **PASS** | Cold launch via `am start -W` completed (`Status: ok`, `LaunchState: COLD`, `TotalTime: 334`). App remained top resumed. Initial touch interactions executed with no crash/ANR signature. |
| 2 | Pause/resume loop scenarios | **PASS** | 12x `HOME` -> relaunch loops executed. App remained recoverable and resumed as `com.l4dar.nativeapp/.MainActivity`. No `FATAL EXCEPTION` / ANR entries captured for app process. |
| 3 | Focus interruption scenarios | **PASS** | Repeated app switches to Android Settings + recents transitions, then return to app. Final explicit recovery check confirmed app resumed and interactive. No app crash/ANR signatures observed. |
| 4 | Gamepad connect/disconnect/reconnect scenarios | **BLOCKED (environment)** | `dumpsys input` showed no active attached gamepad/joystick device for this run profile, so connect/disconnect/reconnect lifecycle behavior could not be executed on hardware in this pass. |
| 5 | Settings persistence checks | **PASS** | Verified `darSettings.xml` persistence across force-stop/relaunch. Also validated UI-driven persistence using `Air Roll Mode` toggle with post-restart consistency (`Air Roll Mode: Toggle`, `airRollIsToggle=true`). |
| 6 | 15-minute soak / long-session drift check | **PASS** | 15-minute continuous run with periodic touch interaction completed. App stayed top resumed. `gfxinfo`: `Total frames rendered: 29934`, `Janky frames: 1 (0.00%)`, `50/90/95/99th: 5ms`. No crash/ANR signature observed. |

### Issues found and severity classification

| ID | Issue | Severity | Type | Notes |
|---|---|---|---|---|
| BB-001 | Gamepad connect/disconnect/reconnect scenario unvalidated in this run due no active external gamepad device attached | **Medium** | Validation coverage gap | This is a run-coverage limitation, not a confirmed runtime defect. V1 still requires one hardware gamepad lifecycle pass to close this gap. |

No Blocker or High severity runtime defects were found in this run.

### Immediate V1 fixes vs safe post-V1 defers

Immediate V1 actions:
1. **Validation-only follow-up (narrow):** run scenario 4 on the same app build with a physically connected controller and execute connect/disconnect/reconnect loops while monitoring app recovery/state.
2. Keep runtime code unchanged unless that hardware pass reveals a concrete defect.

Safe post-V1 defers:
1. Additional gamepad edge-case stress (multiple controller vendors, rapid reconnect storms, low-battery disconnect simulation).
2. Expanded interruption matrix beyond baseline V1 (incoming-call simulation, split-screen/PiP transitions, external display handoff).

### Milestone conclusion

- Runbook executed in required order.
- Startup, lifecycle, focus interruption, persistence, and long-session soak checks passed on the tested profile.
- One medium-severity validation gap remains: missing hardware gamepad reconnect validation in this specific run environment.

## Release-Candidate Checklist and Final V1 Defer List (2026-04-01)

### Scope and guardrails

Decision/checklist-only milestone.

No runtime behavior changes, no feature additions, and no scenario re-runs were performed in this pass.

### Final RC checklist by V1 subsystem

| Subsystem | Final RC readiness | Basis from completed milestones | RC gate status |
|---|---|---|---|
| Startup/lifecycle | **READY** | Bug-bash startup and pause/resume loops passed; app repeatedly resumed cleanly | Pass |
| Touch controls | **READY** | Touch control path previously stabilized and no regressions surfaced in bug-bash/soak | Pass |
| Gamepad controls | **READY (feature path)** | Core DAR/gamepad behavior and deadzone settings surface completed in prior milestones | Pass |
| Gamepad lifecycle validation | **NOT READY (validation pending)** | Scenario 4 blocked by missing attached gamepad in latest run (`BB-001`) | **Fail / open gate** |
| DAR behavior | **READY** | DAR semantics and hold/toggle behavior were aligned and remained stable through recent runs | Pass |
| Physics parity confidence | **READY FOR V1 BASELINE** | Hardened parity harness (integrity + MAE/RMSE/final-state checks) marked sufficient for baseline assets | Pass |
| Settings persistence | **READY** | `darSettings.xml` survived force-stop/restart; UI-visible air-roll mode persisted consistently | Pass |
| UI/settings surface | **READY (V1 scope)** | V1-critical exposed controls retained; deferred/non-V1 surfaces intentionally hidden/deferred | Pass |
| Long-session stability | **READY** | 15-minute soak passed with stable frame pacing and no crash/ANR signatures | Pass |

### BB-001 classification decision

`BB-001` is classified as: **required pre-RC validation**.

Rationale:
1. V1 explicitly includes controller/gamepad support in `PORT_SPEC.md`.
2. Lifecycle resilience is an acceptance criterion, and gamepad connect/disconnect/reconnect is part of lifecycle behavior for that input class.
3. Current state is a validation coverage gap, not a code defect, so the narrow required action is a hardware validation pass rather than feature work.

### Final V1 defer list (explicit IDs)

| Defer ID | Item | Classification | Rationale |
|---|---|---|---|
| DFR-V1-001 | Full controller remapping UI (`⌨ Controls`) | Final V1 defer | Baseline controller functionality exists; remapping UX expansion is non-blocking for baseline parity |
| DFR-V1-002 | `dualStickMode` / `rightStickAssignment` | Final V1 defer | Documented as safe defer in scope-lock; core V1 control path remains usable without this advanced mode |
| DFR-V1-003 | Gamepad sensitivity sliders and preset selector | Final V1 defer | Non-critical tuning surface; defaults are source-aligned for baseline behavior |
| DFR-V1-004 | Broader non-core controller action remap surface (`boost/openMenu/pause/retry/orbit/toggleTheme`) | Final V1 defer | Out of narrow V1 baseline closure scope; not required to certify core free-flight parity |
| DFR-V1-005 | View/HUD full control surface restoration | Final V1 defer | Intentionally hidden/deferred to avoid placeholder drift during V1 stabilization |
| DFR-V1-006 | Ring Mode implementation/surface | Final V1 defer | Explicitly deferred by spec until after baseline stability |
| DFR-V1-007 | Visual polish beyond current baseline renderer | Final V1 defer | Spec prioritizes parity and stability before polish |

### Remaining validation debt

| Debt ID | Item | Severity | RC policy decision |
|---|---|---|---|
| VD-001 (`BB-001`) | Missing hardware execution of gamepad connect/disconnect/reconnect lifecycle scenario | Medium | Must be closed pre-RC via narrow validation-only pass |

### RC exit criteria (final)

V1 RC is exit-ready only when all conditions below are true:

1. No Blocker/High issues remain open from milestone and bug-bash history.
2. Startup/lifecycle, touch controls, DAR behavior, settings persistence, and long-session stability remain in pass state.
3. Physics parity confidence remains at the currently accepted baseline-gate level (no new parity regressions introduced).
4. `VD-001`/`BB-001` is closed by successful on-device hardware gamepad connect/disconnect/reconnect validation on the RC build.
5. No new scope expansions are introduced while closing the validation debt.

### V1 RC go/no-go recommendation

Current recommendation: **NO-GO for RC cut yet**.

Reason:
- One required pre-RC validation gate is still open (`BB-001` / `VD-001`).

Narrow next step to reach GO:
1. Execute hardware gamepad lifecycle validation only (connect/disconnect/reconnect loops) on the current candidate build.
2. If pass with no Blocker/High findings, flip recommendation to **GO** without broadening scope.

## Hardware Gamepad Lifecycle Validation Only (2026-04-01)

### Scope and guardrails

Executed only the gamepad hardware lifecycle validation needed to close `BB-001` / `VD-001`.

No feature additions, controller redesign, remapping expansion, or unrelated system changes were made.

### Device profile and controller detection

- Phone/device profile: `SM-S938U` (`R5CY20ETGPL`, user `u0`)
- Controller (detected): `DualSense Wireless Controller`
- Detectable controller identifiers:
  - vendor: `0x054c`
  - product: `0x0ce6`
  - Android input node: `/dev/input/event13`
  - source classes: `KEYBOARD | GAMEPAD | JOYSTICK` (+ touchpad/motion companion nodes)

### Required scenario execution results (ordered)

| # | Repro steps | Observed result | Pass/Fail | Severity if failed |
|---|---|---|---|---|
| 1 | Launch app with gamepad already connected (`am start -W`) | App cold-launched and resumed as `com.l4dar.nativeapp/.MainActivity` while DualSense remained detected in `dumpsys input` | **PASS** | N/A |
| 2 | Verify controls from fresh launch (manual stick + DAR button check on connected controller) | Physical controller interaction confirmed normal control response; no stuck input reported | **PASS** | N/A |
| 3 | Disconnect gamepad during active control (manual) | Disconnect event executed during active session; controller removed from active connection state and later re-opened in logs | **PASS** | N/A |
| 4 | Reconnect gamepad in same session (manual) | Reconnect succeeded in same session; `InputReader`/`EventHub` showed DualSense add path and app remained usable | **PASS** | N/A |
| 5 | Background app and resume with gamepad still connected | `HOME` -> relaunch cycle returned app to top resumed with controller still present; manual control check after resume was normal | **PASS** | N/A |
| 6 | Background app, disconnect gamepad, reopen app, reconnect gamepad (manual) | Lifecycle sequence completed; reconnect successful and controller detected again by Android input stack | **PASS** | N/A |
| 7 | Verify no duplicated input, stuck DAR, lost axis, or required manual reset after reconnect/resume | Final foreground check with controller connected: no duplicated input, no stuck DAR, no axis loss, no manual reset required | **PASS** | N/A |

### Objective evidence snapshots

- App resumed verification: `topResumedActivity=...com.l4dar.nativeapp/.MainActivity`
- Controller detection: `Device 13: DualSense Wireless Controller`
- Lifecycle reconnection evidence in logs:
  - HID open/profile state changes (`btif_hh`, `Bluetooth` HID state transitions)
  - `EventHub: New device ... /dev/input/event13 ... DualSense Wireless Controller`
  - `InputReader: Device added ... name='DualSense Wireless Controller' ... sources=KEYBOARD | GAMEPAD | JOYSTICK`

### BB-001 / VD-001 closure decision

- `BB-001`: **CLOSED**
- `VD-001`: **CLOSED**

Closure rationale:
1. The previously missing hardware connect/disconnect/reconnect lifecycle validation was executed on real attached controller hardware.
2. All required ordered scenarios passed.
3. No blocker/high-severity defects were found in this validation pass.

### Narrow fix pass requirement

- **No fix pass required** from this milestone.

### Updated RC go/no-go recommendation

Updated recommendation: **GO for V1 RC**.

Reason:
1. The prior open pre-RC gate (`BB-001` / `VD-001`) is now closed.
2. No new blocker/high issues were discovered.
3. RC checklist conditions remain satisfied without scope expansion.

## V1 Release-Candidate Packaging Checklist Only (2026-04-01)

### Scope and guardrails

Checklist/documentation-only milestone for RC packaging readiness.

No runtime behavior changes, feature work, or subsystem reopening were performed.

### Inputs reviewed for packaging decision

- `PORT_SPEC.md`
- `PORT_LOG.md` (including latest GO decision and final V1 defers)
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle`
- `gradle.properties`
- In-repo docs inventory for release notes/changelog material (`README.md`, `BUILD_STATUS.md`, `COMPLETION_CHECKLIST.md`, `PORT_LOG.md`)

### Final RC packaging checklist

| Packaging item | Current state | Status | Packaging note |
|---|---|---|---|
| Version code / version name | `versionCode=1`, `versionName="0.1.0"` in `app/build.gradle` | **READY (candidate baseline)** | Keep as V1 RC baseline unless release management requests a pre-GA bump (e.g., `0.1.0-rc1`) |
| Release build variant | `assembleRelease` executed successfully on current branch | **READY** | Release artifact can be generated from current codebase |
| Signing readiness | No explicit `signingConfigs`/release keystore wiring present in `app/build.gradle` | **PENDING (packaging gate)** | Add/verify release signing material in secure CI/local secrets before external distribution |
| Manifest sanity | Launcher activity exported correctly; landscape policy declared; no unexpected runtime permissions added | **READY** | Manifest aligns with baseline simulator scope |
| App label / icon / orientation policy | Label set to `L4 Native`; orientation `sensorLandscape` on key activities; explicit icon entry not set in manifest | **READY FOR RC TESTING** | Orientation policy is intentional; confirm final launcher icon asset mapping in release packaging QA |
| minSdk / targetSdk sanity | `minSdk=29`, `targetSdk=35`, `compileSdk=35` | **READY** | Build warns AGP 8.5.2 tested up to 34, but build is successful; treat warning as non-blocking packaging note |
| Release notes draft | No standalone `CHANGELOG` file exists; release notes can be generated from milestone log | **READY (generated below)** | Use RC changelog summary in this section as release note draft source |
| Known defer list inclusion | Final defer IDs already defined (`DFR-V1-001..007`) | **READY** | Include unchanged defer list in RC package docs |

### Short tester-facing RC validation checklist

Use for controlled RC distribution testing:

1. Install RC build and verify app launches offline to `MainActivity`.
2. Verify touch free-flight control response and restart action.
3. Connect a controller (DualSense/XInput-equivalent) and verify baseline gamepad control.
4. Perform disconnect/reconnect during active session and confirm no stuck DAR/axis loss.
5. Perform background/resume with controller connected, then retest controls.
6. Confirm settings persistence survives force-stop/relaunch.
7. Run a 10-15 minute free-flight soak and check for crash/ANR or severe frame instability.
8. Confirm deferred features are not treated as regressions (`DFR-V1-*` list).

### Formal V1 RC changelog summary (from completed milestones)

`V1 RC` summary:

1. Physics parity hardening completed:
  - removed non-source-backed input smoothing
  - aligned near-center release threshold
  - strengthened continuous trajectory parity harness
2. Controller/settings V1-critical closure completed:
  - exposed editable gamepad left/right deadzones
  - aligned gamepad status wording for supported scope
3. Settings-surface scope cleanup completed:
  - retained V1-critical `gameSpeed` and `touchDeadzone`
  - kept non-V1 dynamics/tunable surfaces fixed/deferred by policy
4. Lifecycle and stability validation completed:
  - startup, pause/resume, focus interruption, persistence, and soak passes
5. Hardware controller lifecycle validation completed:
  - real DualSense connect/disconnect/reconnect and post-resume behavior passed
  - `BB-001` / `VD-001` closed
6. Final RC readiness decision updated to GO with defer list explicitly documented.

### Final known defer list for RC package

Unchanged final V1 defers:

- `DFR-V1-001`: full controller remapping UI (`⌨ Controls`)
- `DFR-V1-002`: `dualStickMode` / `rightStickAssignment`
- `DFR-V1-003`: gamepad sensitivity sliders and preset selector
- `DFR-V1-004`: broader non-core controller action remap surface
- `DFR-V1-005`: View/HUD full control surface restoration
- `DFR-V1-006`: Ring Mode implementation/surface
- `DFR-V1-007`: visual polish beyond baseline renderer

### RC distribution recommendation

Recommendation: **Proceed with controlled V1 RC distribution/testing**.

Distribution conditions:
1. Use release variant artifacts (`assembleRelease`) from current GO state.
2. Complete signing-material wiring/check before any external tester rollout.
3. Ship RC notes with this checklist and defer list so testers evaluate only in-scope V1 behavior.

## Release Signing and RC Handoff Checklist Only (2026-04-01)

### Scope and guardrails

Documentation/packaging-only milestone focused on release signing closure and RC handoff readiness.

No runtime feature changes, subsystem refactors, or scope expansion were performed.

### Current signing configuration state

Verified from project files:

1. `app/build.gradle` has no explicit `signingConfigs` block.
2. `release` build type exists and `assembleRelease` succeeds, but it is not wired to a project-managed release keystore config.
3. No `key.properties`, `.jks`, or `.keystore` files are currently present in `android-native-l4`.
4. Existing `.gitignore` does not yet include signing-secret patterns (`*.jks`, `*.keystore`, `key.properties`).

### Signing gap summary (what is still missing)

Remaining packaging gate for properly signed RC/release distribution:

1. A dedicated release keystore strategy (create/select keystore, alias, and ownership policy).
2. Secure secret material provisioning for build-time signing values (store password, key password, alias, keystore path).
3. Explicit Gradle wiring for release signing config so release artifacts are reproducibly signed.
4. Secret-handling hygiene updates (ignore patterns and local secret-file convention).
5. Signed-artifact verification step in packaging flow (`apksigner verify`/equivalent).

### Step-by-step signing readiness checklist (project-specific)

1. Decide signing owner and key custody policy for this project (single owner vs team escrow).
2. Generate or import the release keystore in a secure local/CI secret location (not committed to repo).
3. Create local `key.properties` (or CI secret variables) with:
  - `storeFile`
  - `storePassword`
  - `keyAlias`
  - `keyPassword`
4. Update local ignore rules before storing secrets:
  - add `*.jks`
  - add `*.keystore`
  - add `key.properties`
5. Add Gradle release signing wiring in `app/build.gradle` using `signingConfigs.release` and attach it to `buildTypes.release`.
6. Build signed release artifact via release variant (`assembleRelease`).
7. Verify signed artifact integrity with signer verification tooling (for example `apksigner verify --verbose --print-certs`).
8. Record signing fingerprint summary (SHA-256 cert digest) in internal release notes for traceability.
9. Confirm installability on at least one clean tester device from signed artifact.
10. Freeze signed RC artifact checksum and publish handoff bundle to testers.

### RC handoff checklist for controlled testers

#### Artifact type

1. Primary handoff artifact: signed release APK built from `assembleRelease`.
2. Include artifact hash/checksum in handoff note.

#### Install method

1. ADB install path for internal testers (`adb install -r <signed-apk>`), or approved internal distribution channel if available.
2. Require clean install once, then optional update install path for regression checks.

#### Tester validation focus

1. Launch/offline startup reliability.
2. Baseline touch free-flight behavior.
3. Controller lifecycle behavior (connect/disconnect/reconnect and post-resume control health).
4. Settings persistence across force-stop/relaunch.
5. 10-15 minute stability soak.

#### Known defer list (must not be filed as RC regressions)

1. `DFR-V1-001` full controller remapping UI.
2. `DFR-V1-002` dual-stick assignment features.
3. `DFR-V1-003` sensitivity sliders/preset selector.
4. `DFR-V1-004` broader non-core controller remap surface.
5. `DFR-V1-005` full View/HUD control restoration.
6. `DFR-V1-006` Ring Mode implementation/surface.
7. `DFR-V1-007` visual polish beyond current baseline.

#### Bug reporting expectations

1. Report only in-scope V1 behavior defects first (startup, control response, lifecycle, persistence, stability).
2. For each bug, provide:
  - device model + Android version
  - controller model/connection mode (if relevant)
  - exact repro steps
  - expected vs actual result
  - severity (Blocker/High/Medium/Low)
  - video/log evidence where possible
3. Label defer-list requests as post-V1 enhancement, not RC blocker.

### Final recommendation for controlled RC distribution (post-signing)

Recommendation:

1. **Proceed with controlled RC distribution immediately after signing checklist completion and signed artifact verification.**
2. Do not expand runtime scope during signing/handoff completion.
3. Keep RC gate focused on packaging integrity + in-scope validation only.

## Release Signing Implementation Pass Only (2026-04-02)

### Scope and guardrails

Implemented only release-signing project wiring and secret-exclusion updates.

No runtime feature behavior, input behavior, physics behavior, or UI behavior was changed.

### Files changed

1. `app/build.gradle`
2. `.gitignore`
3. `PORT_LOG.md`

### Signing wiring completed

Completed wiring:

1. Added release signing config support in `app/build.gradle`.
2. Added secret source support from either:
  - local `key.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`), or
  - environment variables:
    - `L4DAR_STORE_FILE`
    - `L4DAR_STORE_PASSWORD`
    - `L4DAR_KEY_ALIAS`
    - `L4DAR_KEY_PASSWORD`
3. Attached `signingConfigs.release` to `buildTypes.release`.
4. Added a release-task guard that fails fast with explicit instructions when signing material is missing.
5. Confirmed debug build remains unchanged (`assembleDebug` passes).

Secret hygiene updates:

1. Added to `.gitignore`:
  - `key.properties`
  - `*.jks`
  - `*.keystore`

### Validation results for this pass

1. `assembleDebug` -> **PASS** (debug build unaffected).
2. `assembleRelease` without signing secrets -> **expected fail** with explicit signing configuration error message.

### Remaining manual steps

1. Create/import release keystore in secure local/CI storage.
2. Provide signing values via `key.properties` or the documented `L4DAR_*` environment variables.
3. Re-run release build.
4. Verify signature and record fingerprint/checksum for RC handoff.

### Exact command/path to produce signed RC artifact

From project root folder `android-native-l4`:

1. Build signed RC artifact:
  - `./gradlew.bat assembleRelease`
2. Signed APK output path:
  - `app/build/outputs/apk/release/app-release.apk`

## Narrow Gamepad Status-Indicator Bug Audit Only (2026-04-02)

### Scope and guardrails

Audit-only pass focused strictly on Android gamepad status-indicator behavior.

No changes were made to:
- controller mappings
- physics
- DAR behavior
- rendering
- runtime input behavior

### Files reviewed for this audit

- `PORT_SPEC.md`
- `PORT_LOG.md`
- `app/build.gradle`
- `app/src/main/AndroidManifest.xml`
- `gradle.properties`
- gamepad/status path files:
  - `app/src/main/java/com/l4dar/nativeapp/MainActivity.java`
  - `app/src/main/java/com/l4dar/nativeapp/render/L4SurfaceView.java`
  - `app/src/main/java/com/l4dar/nativeapp/core/input/InputController.java`
  - `app/src/main/java/com/l4dar/nativeapp/core/input/GamepadInputHandler.java`
  - `app/src/main/res/layout/activity_main.xml`

### Android gamepad status update path (UI)

Observed status-indicator behavior:

1. `activity_main.xml` initializes the label as `Status: Waiting for gamepad`.
2. `MainActivity.setupMenuControls()` sets `gamepadStatusText` to `Status: Waiting for gamepad` again.
3. No subsequent code path updates `gamepadStatusText` based on runtime controller state.
4. No `InputManager.InputDeviceListener` registration (`registerInputDeviceListener`) was found in the Android app code.

Conclusion: the status text is currently static and not wired to the controller lifecycle.

### Actual working controller input path (runtime)

Verified functioning path:

1. `L4SurfaceView.onGenericMotionEvent/onKeyDown/onKeyUp` forwards controller events.
2. `InputController` accepts `SOURCE_GAMEPAD` / `SOURCE_JOYSTICK` events and sets `inputSource = GAMEPAD`.
3. `GamepadInputHandler` updates stick/throttle/DAR runtime state from motion/key events.
4. `L4Renderer.onDrawFrame()` consumes `InputController.getInputSnapshot()` each frame and applies it to physics.

Conclusion: the active controller-input path is implemented and independent of the menu status label.

### Root-cause classification

Classification decision for this bug:

- **Primary:** missing status-listener/update wiring (no attach/detach listener and no event-driven status-label update path)
- **User-visible symptom:** stale UI text (`Waiting for gamepad`) even when input is working
- **Not supported by this audit:** wrong source-detection logic in the active input path
- **Not supported by this audit:** broader controller lifecycle state defect affecting actual control behavior

In short: this is a status-indicator wiring gap that manifests as stale text, not a proven runtime control-lifecycle failure.

### Severity and fix timing recommendation

- **Severity recommendation:** Low (UI accuracy bug, misleading but non-blocking to control function)
- **Safe to defer?** Yes, safe to defer for narrow functionality testing where controller behavior is the focus
- **Should fix before wider testing?** Recommended yes before broader external QA/RC communication, because incorrect status messaging can create false bug reports and test confusion

### Implementation policy for this milestone

- No implementation done in this pass by design.
- If a follow-up is approved, keep it narrow to status plumbing only (device attach/detach listener + status text refresh), with no controller behavior changes.

## Free Air Roll Behavior Audit and Policy Decision Pass Only (2026-04-02)

### Scope and guardrails

Audit + decision only, focused on free/normal air roll behavior isolation.

No changes were made to:
- DAR behavior
- controller mappings
- rendering
- unrelated systems

### Files re-read for this pass

- `PORT_SPEC.md`
- `ANDROID_ARCHITECTURE.md`
- `PORT_LOG.md`
- `app/src/main/java/com/l4dar/nativeapp/core/input/GamepadInputHandler.java`
- `app/src/main/java/com/l4dar/nativeapp/core/input/InputController.java`
- `app/src/main/java/com/l4dar/nativeapp/core/physics/L4PhysicsEngine.java`

Requested web-source files were not present in this workspace path:
- `docs/js/modules/input/gamepadInput.js`
- `docs/js/modules/input/airRollController.js`
- `docs/js/modules/physics.js`

For web comparison in this pass, conclusions are based on:
1. source-parity notes already recorded earlier in this log
2. explicit source-port comments and behavior in current Android physics/input code

### 1) Isolated Android runtime path for free air roll only

Free-air-roll path (gamepad):

1. Button binding default is `gpBindRollFree = KEYCODE_BUTTON_R2` (`SettingsManager`).
2. `InputController.onKeyDown()` routes gamepad keys to `GamepadInputHandler`.
3. `GamepadInputHandler.onKeyDown()` for `gpBindRollFree` sets `freePressed = true`.
4. In hold mode, `applyHoldModeDarState()` resolves `darState = FREE` when free is active and left/right are not active.
5. In toggle mode, free toggles `darState` between `FREE` and `NONE`.
6. `InputController.getInputSnapshot()` exports:
  - `airRoll = FREE`
  - `airRollIntensity = 1.0` (digital)
  - `darOn = true`
7. `L4PhysicsEngine.performStep()` enters `isDARActive` branch and then special-cases `airRoll == FREE`.

### 2) Exact Android free-air-roll runtime behavior now

Current behavior in `L4PhysicsEngine` when `airRoll == FREE`:

- roll target (`wx_raw`) uses **left-stick X** and deadzone-shaped stick magnitude:
  - `wx_raw = W_MAX_ROLL * eff * (-jx)`
- yaw target is forced off:
  - `wy_raw = 0`
- pitch target still follows stick Y in DAR branch:
  - `wz_raw = pitchCapDar * eff * (-jy) * 0.997`
- `airRollIntensity` is not used in the FREE branch.

Practical result:
- pressing free-air-roll button alone with centered stick gives near-zero roll target
- free mode behaves like a stick-remapped roll/yaw mode, not a direct independent roll command

### 3) DAR behavior vs free-air-roll behavior (explicit separation)

Directional DAR (`LEFT` / `RIGHT`):
- roll is button-driven via `airRoll * airRollIntensity * DAR_ROLL_SPEED`
- yaw remains available from stick X
- pitch remains available from stick Y

Free air roll (`FREE`):
- roll is **not** button-driven; it is stick-X-driven
- yaw is explicitly suppressed
- pitch remains stick-Y-driven

So the free-air-roll path is behaviorally distinct from directional DAR and currently tied to stick shaping rather than button intensity.

### 4) Android vs web implementation and suspected web flaw

Android now:
- implements free air roll as the special `airRoll == FREE` branch above (stick-X controls roll, yaw suppressed).

Web now (as previously source-audited and reflected by the current source-port behavior):
- free-air-roll semantics route through `airRoll = 2` and the same effective physics concept now mirrored in Android.

Appears flawed in web behavior:
- free/normal air roll is not acting as a clean independent roll command when activated by button alone
- behavior is coupled to stick X and deadzone shaping in a way that can feel incorrect or inconsistent for users expecting button-driven free roll

### 5) V1 policy decision classification

Decision classification for next step:

- **defer correction and document as known limitation**

Rationale:
1. `PORT_SPEC.md` keeps web as source of truth for V1 parity decisions.
2. DAR behavior is currently acceptable in testing and should not be destabilized late.
3. Correcting free-air-roll now requires intentional behavior divergence from current web behavior and likely broader retuning/validation.
4. Safest V1 path is to ship parity-aligned known limitation, then run a targeted post-V1 correction pass with explicit behavior spec.

### 6) Recommended next implementation pass

**Recommended next milestone:** narrow free-air-roll correction design/spec pass only (no implementation in this audit).

Pass contents:
1. define the desired free-air-roll behavior contract explicitly (button-driven vs stick-assisted expectations)
2. decide whether to intentionally diverge from current web behavior or patch both platforms
3. create parity test cases focused on free-air-roll-only scenarios (centered stick, diagonal stick, hold/toggle transitions)
4. only then implement a narrow runtime change in one controlled pass

### Known limitation to carry forward (V1)

- Free/normal air roll may feel incorrect because current behavior is stick-coupled and yaw-suppressed rather than a pure independent roll command.
- DAR left/right behavior remains acceptable and out of scope for this correction.

## Free Air Roll Correction Design/Spec Pass Only (2026-04-02)

### Scope and guardrails

Design/spec-only pass for FAR (free air roll) correction planning.

No runtime behavior changes were made.

Explicitly out of scope in this pass:
- DAR behavior changes
- lifecycle changes
- rendering changes
- controller mapping changes

### Inputs reviewed

- `PORT_SPEC.md`
- `PORT_LOG.md`
- `app/src/main/java/com/l4dar/nativeapp/core/physics/L4PhysicsEngine.java`
- `app/src/main/java/com/l4dar/nativeapp/core/input/GamepadInputHandler.java`
- `app/src/main/java/com/l4dar/nativeapp/core/input/InputSnapshot.java`
- `app/src/main/java/com/l4dar/nativeapp/core/settings/SettingsManager.java`
- prior parity notes in this log related to `rollFree`
- signed RC observations in this log (controller lifecycle and RC-readiness sections)

### FAR intended behavior model (V1 target spec)

This section defines the intended FAR model precisely for a narrow correction pass.

1. FAR authority while held:
  - FAR should provide **constant roll authority while active**.
  - FAR should not require stick-X deflection to generate roll.
  - With centered stick and FAR active, roll target must remain non-zero and stable.

2. Stick behavior while FAR is active:
  - Left stick X/Y should remain the standard pitch+yaw control path.
  - Yaw must not be suppressed by FAR activation.
  - FAR should be an added roll-command mode, not a yaw-to-roll remap mode.

3. Axis-remap policy:
  - FAR should **not** remap yaw into roll-relative axes for V1.
  - Keep body-axis intent consistent with existing non-FAR handling to minimize regression surface.

4. Angular budget policy:
  - FAR should be treated as independent from directional DAR budget logic.
  - Do not force FAR into the current DAR-only yaw suppression and 50% pitch/yaw budget behavior.
  - Keep existing per-axis safety caps intact.
  - Do not alter directional DAR global-cap behavior in this milestone.

### Explicit FAR vs DAR separation

Directional DAR (`LEFT` / `RIGHT`) remains unchanged:
- button selects signed directional roll state
- DAR branch behavior, hold/toggle semantics, and current acceptable feel stay intact

FAR (`FREE`) intended behavior:
- activation enables free-roll command authority while preserving normal stick pitch+yaw
- FAR should not inherit directional DAR yaw suppression behavior
- FAR should not depend on stick-X magnitude to produce baseline roll

### Expected PS5 controller feel contract (design target)

Target feel contract for validation scenarios:

- `L1 = FAR` (hold):
  - car rolls continuously while held, even at centered left stick
  - left stick still controls pitch+yaw normally during hold
- `Square/Circle = DAR Left/Right`:
  - directional DAR behavior remains as currently accepted
  - no behavior drift from the existing DAR pass

Note:
- this feel contract defines behavior expectations for verification.
- mapping implementation remains out of scope in this pass.

### Classification decision

Classification for this issue after design pass:

- **V1 blocker correction** (narrow, FAR-only)

Rationale:
1. FAR is currently present but behaviorally misleading for expected controller use (centered-stick FAR yields weak/no roll target).
2. This affects core controller control intent, not cosmetic polish.
3. The correction can be scoped narrowly to FAR physics-target behavior while freezing DAR/lifecycle/rendering.

### Risk analysis

Primary risks:

1. DAR regression risk if FAR and DAR paths are not strictly separated in code.
2. Control-feel drift risk if FAR correction also changes stick shaping or per-axis caps globally.
3. Late-cycle scope creep risk if controller mapping/preset work is mixed into FAR behavior correction.

Mitigations for the implementation pass:

1. Restrict code changes to FAR branch logic only (primarily physics target construction for `airRoll == FREE`).
2. Add focused test matrix for FAR-only scenarios:
  - centered stick + FAR hold
  - stick X/Y + FAR hold
  - FAR release transitions
  - FAR vs DAR non-regression checks
3. Keep mappings frozen in that pass; verify behavior using existing bindings and a documented PS5 feel checklist.

### Exact next implementation scope (narrow)

Next pass title:
- **FAR narrow runtime correction pass only**

Allowed scope:
1. Update FAR-specific target behavior in `L4PhysicsEngine.performStep()` for `airRoll == FREE`.
2. Preserve all directional DAR branches and state machine behavior as-is.
3. Keep `GamepadInputHandler` and mapping defaults unchanged unless strictly required for FAR correctness (not expected).
4. Add/update log notes and targeted FAR verification evidence only.

Out-of-scope for that pass:
1. DAR redesign or remapping
2. controller lifecycle/status work
3. rendering/camera changes
4. broad controller preset/UI work

## Free Air Roll V1 Acceptance Decision Pass Only (2026-04-02)

### Scope and guardrails

Design/decision-only pass for V1 FAR acceptance criteria.

No runtime behavior changes were made.

Explicitly unchanged:
- DAR behavior
- lifecycle behavior
- rendering
- unrelated systems

### Inputs re-read for this pass

- `PORT_SPEC.md`
- `PORT_LOG.md`
- `app/src/main/java/com/l4dar/nativeapp/core/physics/L4PhysicsEngine.java`
- `app/src/main/java/com/l4dar/nativeapp/core/input/GamepadInputHandler.java`
- `app/src/main/java/com/l4dar/nativeapp/core/input/InputSnapshot.java`
- `app/src/main/java/com/l4dar/nativeapp/core/settings/SettingsManager.java`
- existing FAR audit findings in this log
- signed RC observations in this log

### 1) Current Android FAR path (isolated)

Current runtime FAR path:

1. `rollFree` input sets `darState = FREE` via `GamepadInputHandler` hold/toggle state machine.
2. `InputSnapshot` carries `airRoll = FREE`, `airRollIntensity = 1.0`, `darOn = true`.
3. `L4PhysicsEngine.performStep()` enters DAR-active branch and special-cases `airRoll == FREE`.
4. In that branch:
  - `wx_raw = W_MAX_ROLL * eff * (-jx)` (roll tied to stick X)
  - `wy_raw = 0` (yaw suppressed)
  - `wz_raw` remains stick-Y driven

### 2) Why current FAR feels wrong (practical control terms)

Practical control problems:

1. Holding FAR with centered stick gives weak/near-zero roll target, so FAR feels non-functional when first pressed.
2. Yaw suppression while FAR is active removes expected left-stick steering continuity.
3. FAR currently behaves like a stick remap mode, not a reliable free-roll command mode.
4. This conflicts with player expectation that the FAR button itself should create immediate roll authority.

### 3) Intended V1 FAR behavior (plain terms)

V1 intended behavior contract:

1. When FAR is held (`L1` in user acceptance profile):
  - roll authority should remain consistently active while held
  - roll should not depend on stick-X displacement to exist

2. Left stick while FAR is active:
  - stick X/Y should remain intuitive pitch+yaw control
  - yaw should remain available (not suppressed)

3. FAR authority model:
  - FAR should provide stable roll command authority while active
  - FAR should augment control, not replace yaw with roll remap logic

4. DAR separation:
  - `Square/Circle` directional DAR behavior remains unchanged and must not regress

### 4) V1 personal-acceptance impact decision

Acceptance decision for current project context:

- **FAR correction is required before V1 can be personally accepted by the current final acceptance authority (user).**

Reasoning:
1. User-reported acceptance standard explicitly identifies FAR as the currently wrong-feeling behavior.
2. DAR is acceptable now and should remain untouched.
3. The user is currently the sole tester and final acceptance gate, so practical control feel in FAR is release-critical for V1 acceptance.

This refines earlier wording that discussed general tester tolerance: for current V1 acceptance, FAR is not treated as a defer-only quality issue.

### 5) Exact next implementation scope (if correction proceeds)

Next pass title:
- **FAR narrow runtime correction pass for V1 acceptance**

Allowed scope:
1. FAR-only target-generation updates in `L4PhysicsEngine` under `airRoll == FREE`.
2. Preserve existing DAR left/right/toggle/hold behavior exactly.
3. Keep `GamepadInputHandler`, `InputSnapshot`, and bindings unchanged unless strictly needed for FAR-only correctness.
4. Add targeted FAR verification notes in `PORT_LOG.md` against the V1 feel contract above.

Out-of-scope:
1. DAR logic redesign
2. mapping/UI expansion
3. lifecycle/rendering/system changes
4. broad controller parity refactors

## FAR Narrow Runtime Correction Pass Only (2026-04-02)

### Scope and guardrails

Implemented only the approved narrow FAR runtime correction for V1 personal acceptance.

No changes were made to:
- DAR state machine behavior
- controller mappings
- lifecycle
- rendering
- unrelated systems

### Files changed

- `app/src/main/java/com/l4dar/nativeapp/core/physics/L4PhysicsEngine.java`
- `PORT_LOG.md`

### Exact FAR runtime change made

Changes were limited to FAR handling inside `L4PhysicsEngine.performStep()`:

1. Isolated FAR state from directional DAR using explicit booleans:
  - `isFreeAirRoll`
  - `isDirectionalDAR`
2. Updated only the `airRoll == FREE` target construction path:
  - roll target is now constant baseline authority while FAR is active:
    - `wx_raw = DAR_ROLL_SPEED * airRollIntensity`
  - yaw is no longer suppressed in FAR:
    - `wy_raw = W_MAX_YAW * eff * (-jx)`
  - pitch remains intuitive stick-Y-driven in FAR:
    - `wz_raw = W_MAX_PITCH * eff * (-jy) * 0.997`
3. Updated FAR-specific per-axis cap behavior so FAR no longer inherits directional DAR half-caps:
  - directional DAR (`LEFT`/`RIGHT`) still uses the existing 50%-of-`W_MAX` pitch/yaw cap policy
  - FAR now uses normal per-axis pitch/yaw caps

Behavioral impact of the correction:
- FAR now provides stable non-zero baseline roll authority while active (independent of stick X).
- Left-stick yaw/pitch continuity is preserved during FAR.

### Non-regression notes for DAR

Directional DAR behavior was intentionally kept unchanged:

1. `GamepadInputHandler` was not modified.
2. No DAR hold/toggle state-machine logic was changed.
3. Directional DAR target math path (`airRoll != FREE`) remains unchanged.
4. Directional DAR per-axis half-cap policy remains unchanged.

Build verification:
- `./gradlew.bat assembleDebug` -> **BUILD SUCCESSFUL**

### Remaining FAR limitations

Remaining limitations after this narrow pass:

1. FAR roll direction is fixed to the current baseline sign while active; no user-facing FAR direction setting was added.
2. FAR remains inside the broader DAR-active branch and still participates in existing DAR-era global vector normalization/cap flow where applicable.
3. No controller remapping/preset expansion was added in this pass by design.
4. On-device FAR feel verification matrix (centered-stick hold/release and mixed stick scenarios) is still required to fully close acceptance.

## On-Device FAR Acceptance Validation Only (2026-04-02)

### Scope and guardrails

Validation-only pass using the connected user device with no runtime code changes.

Unchanged in this pass:
- DAR logic
- controller mappings
- lifecycle/rendering/unrelated systems

### Validation setup

- Device connected via ADB: `R5CY20ETGPL`
- App launched on device: `com.l4dar.nativeapp/.MainActivity`
- Validation authority: user is current sole tester and final V1 acceptance gate

### Scenario results (required order)

1. FAR held with centered stick
  - Observed feel/result: no major forced-roll onset; behavior considered acceptable with only minor settle behavior
  - Pass/Fail: **PASS**
  - Meets personal V1 acceptance standard: **YES**

2. FAR held with stick up/down
  - Observed feel/result: pitch directionality felt acceptable, but user notes effective pitch rate feels lower than expected target authority
  - Pass/Fail: **PASS** (with concern)
  - Meets personal V1 acceptance standard: **PARTIAL / CONDITIONAL**

3. FAR held with stick left/right
  - Observed feel/result: left/right air-roll behavior still feels swapped / incorrect
  - Pass/Fail: **FAIL**
  - Meets personal V1 acceptance standard: **NO**

4. FAR held with diagonal stick
  - Observed feel/result: mixed pitch+yaw remained usable while roll continued
  - Pass/Fail: **PASS**
  - Meets personal V1 acceptance standard: **YES**

5. FAR release during motion
  - Observed feel/result: no stuck roll and no unusual transition observed
  - Pass/Fail: **PASS**
  - Meets personal V1 acceptance standard: **YES**

6. Directional DAR left/right non-regression check
  - Observed feel/result: directional DAR still feels unchanged/acceptable
  - Pass/Fail: **PASS**
  - Meets personal V1 acceptance standard: **YES**

### Important acceptance-standard correction

User clarified that FAR held with centered stick should **not** force obvious new roll onset by itself; the prior design wording that implied mandatory non-zero centered-stick roll authority was incorrect for user acceptance.

For acceptance going forward:
- FAR must preserve intuitive control behavior and correct directional response under stick input
- FAR must not introduce wrong-sign/swapped roll behavior

### Remaining FAR behavior issues from on-device validation

1. FAR left/right directional response still feels swapped/incorrect under stick-left/stick-right use.
2. FAR pitch/roll perceived authority may still feel below expected target (user reports lower-than-expected rad/s feel).

### V1 FAR acceptance decision after on-device validation

- **FAR is NOT yet accepted for V1** under the user’s personal acceptance standard.

Reason:
1. Required scenario 3 failed (left/right FAR behavior still incorrect).
2. Additional authority/feel concern remains on pitch/roll response.

### Recommended next milestone

**FAR directional/sign and authority calibration pass only** (narrow runtime fix):

1. Correct FAR left/right directional sign/axis behavior under FAR active control.
2. Recheck FAR target authority scaling against expected feel without changing DAR behavior.
3. Re-run the same on-device six-scenario acceptance checklist immediately after patch.

## FAR Directional/Sign and Authority Calibration Pass Only (2026-04-02)

### Scope and guardrails

Implemented only FAR directional/sign correction and FAR authority calibration identified by on-device acceptance failure.

No changes were made to:
- DAR behavior/state machine
- controller mappings
- lifecycle
- rendering
- unrelated systems

### Files changed

- `app/src/main/java/com/l4dar/nativeapp/core/physics/L4PhysicsEngine.java`
- `PORT_LOG.md`

### Exact FAR directional/sign correction

Within `L4PhysicsEngine.performStep()` FAR-only branch (`isFreeAirRoll`):

1. FAR roll sign was corrected to follow intuitive stick direction:
  - `stick right` -> positive roll command
  - `stick left` -> negative roll command
2. Implemented through:
  - `Math.signum(jx)` in FAR roll target construction
3. FAR yaw remains suppressed during FAR active state in this calibration pass (`wy_raw = 0f`) so left/right stick input maps cleanly to FAR roll rather than mixed yaw+roll.

### Exact FAR authority calibration change

1. Removed autonomous FAR roll onset at centered stick.
  - FAR roll now requires meaningful stick-X displacement.
2. Replaced previous FAR roll authority with deadzone-ramped stick-X-only authority:
  - `jxAbs = abs(jx)`
  - `farRollEff = (jxAbs - stickDeadzone) / (1 - stickDeadzone)` when outside deadzone
  - `wx_raw = DAR_ROLL_SPEED * farRollEff * sign(jx) * airRollIntensity`
3. This keeps FAR intentionally stick-driven and avoids baseline roll when centered.

### DAR non-regression note

Directional DAR path remains untouched:

1. `GamepadInputHandler.java` unchanged.
2. `InputSnapshot.java` unchanged.
3. Directional DAR target path in `L4PhysicsEngine` (`!isFreeAirRoll`) unchanged.
4. Directional DAR cap policy unchanged.

### Build verification

- `./gradlew.bat assembleDebug` -> **BUILD SUCCESSFUL**

### Remaining FAR limitations

1. FAR still uses the shared DAR-active global vector normalization/cap flow after raw target construction.
2. Final user acceptance still requires another on-device six-scenario FAR validation run after this calibration.

## Re-Run On-Device FAR Acceptance Validation Only (2026-04-02)

### Scope and guardrails

Validation-only milestone after FAR directional/sign and authority calibration.

No runtime code changes were made in this pass.

### Validation setup

- Device: `R5CY20ETGPL`
- App launched: `com.l4dar.nativeapp/.MainActivity`
- Acceptance authority: user personal V1 standard

### Scenario-by-scenario FAR results (required order)

1. Hold FAR with centered stick
  - Observed feel/result: no autonomous roll onset; nothing notable
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

2. FAR + stick left/right
  - Observed feel/result: left/right still does not produce expected opposite roll directions
  - Pass/Fail: **FAIL**
  - Meets personal acceptance standard: **NO**

3. FAR + stick up/down
  - Observed feel/result: prior issue still present in this scenario
  - Pass/Fail: **FAIL**
  - Meets personal acceptance standard: **NO**

4. FAR + diagonal stick input
  - Observed feel/result: currently usable; expected to improve further after left/right fix
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES (conditional)**

5. Release FAR during motion
  - Observed feel/result: transition feels clean; no stuck behavior noted
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

6. Verify directional DAR unchanged
  - Observed feel/result: DAR left/right still feels and works fine
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

### FAR V1 acceptance status after re-run

- **FAR is NOT accepted for V1 yet**.

Primary reason:
1. Scenario 2 remains failing (left/right FAR direction behavior not yet correct).
2. Scenario 3 remains failing and needs FAR-control feel correction.

### Remaining blocker issue

1. FAR directional behavior under left/right input is still incorrect.
2. Related FAR control issue also remains present for the on-screen analog stick path.

### Additional user observation (recorded, out of scope for this validation-only pass)

User reiterated preferred default control mapping expectations:
- air roll left on Square
- air roll right on Circle
- DAR toggle on X
- free air roll on L1

This pass did not modify mappings.

### Recommended next milestone

**FAR directional unification pass only (gamepad + on-screen analog consistency)**:

1. Correct FAR left/right directional behavior so opposite stick directions reliably produce opposite roll response.
2. Ensure the same directional correction applies to on-screen analog control path.
3. Keep DAR behavior and mappings unchanged during that correction.
4. Re-run the same six-scenario on-device acceptance checklist immediately after patch.

## FAR Directional Unification Pass Only (2026-04-02)

### Scope and guardrails

Implemented only FAR directional unification in shared FAR runtime path.

No changes were made to:
- DAR behavior
- controller mappings
- lifecycle
- rendering
- unrelated systems

### Files changed

- `app/src/main/java/com/l4dar/nativeapp/core/physics/L4PhysicsEngine.java`
- `PORT_LOG.md`

### Root cause of FAR directional issue

Root cause was in the shared FAR roll sign interpretation inside `L4PhysicsEngine.performStep()` `isFreeAirRoll` branch:

1. Both gamepad and on-screen analog stick feed `joyPixelsX` into the same FAR physics branch through `InputSnapshot`.
2. FAR roll sign used a convention that was opposite to the established accepted roll-axis feel (as validated by directional DAR behavior).
3. Because both input methods share this same branch/sign logic, the directional mismatch appeared on both gamepad and touch analog.

### Exact directional logic correction

In FAR branch only:

1. Changed FAR roll sign term from:
  - `Math.signum(jx)`
2. To:
  - `-Math.signum(jx)`
3. Full FAR roll term now:
  - `wx_raw = DAR_ROLL_SPEED * farRollEff * (-Math.signum(jx)) * airRollIntensity`

This preserves:
- stick-driven FAR authority ramp (`farRollEff`)
- centered-stick non-autonomous FAR behavior (no roll when `jx` remains in deadzone)

### DAR non-regression confirmation

DAR path stayed unchanged:

1. `GamepadInputHandler.java` unchanged.
2. `InputSnapshot.java` unchanged.
3. Directional DAR branch logic in `L4PhysicsEngine` (`!isFreeAirRoll`) unchanged.
4. Directional DAR cap policy unchanged.

### Build verification

- `./gradlew.bat assembleDebug` -> **BUILD SUCCESSFUL**

### Remaining FAR limitations

1. Final acceptance still requires on-device FAR checklist re-run after this directional sign unification.
2. FAR still follows the current FAR-mode design choices (including yaw suppression during FAR active state) from previous scoped passes.

## Final Re-Run On-Device FAR Acceptance Validation Only (2026-04-02)

### Scope and guardrails

Validation-only final re-run after FAR directional unification pass.

No runtime code changes were made in this milestone.

### Validation setup

- Device: `R5CY20ETGPL`
- App launched: `com.l4dar.nativeapp/.MainActivity`
- Acceptance authority: user personal V1 acceptance standard

### Scenario-by-scenario final FAR results (required order)

1. Hold FAR with centered stick
  - Observed feel/result: no autonomous roll onset observed
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

2. FAR + stick left/right
  - Observed feel/result: same incorrect directional behavior persists; no practical improvement felt
  - Pass/Fail: **FAIL**
  - Meets personal acceptance standard: **NO**

3. FAR + stick up/down
  - Observed feel/result: behavior unchanged and still unacceptable in FAR context
  - Pass/Fail: **FAIL**
  - Meets personal acceptance standard: **NO**

4. FAR + diagonal stick input
  - Observed feel/result: technically fails because resulting roll direction remains incorrect
  - Pass/Fail: **FAIL**
  - Meets personal acceptance standard: **NO**

5. Release FAR during motion
  - Observed feel/result: clean transition; no stuck behavior
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

6. Verify directional DAR unchanged
  - Observed feel/result: DAR still feels unchanged and acceptable
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

### Final FAR acceptance decision for V1

- **FAR is NOT accepted for V1**.

Remaining blocker:
1. FAR directional behavior still feels wrong/incorrect under active stick input.
2. Same issue persists from prior run and remains the current acceptance blocker.

### Updated overall V1/RC recommendation (acceptance-gated)

- For the current user-gated release process: **NO-GO** until FAR acceptance blocker is resolved.

Clarification:
1. DAR remains acceptable and non-regressed.
2. FAR remains the only active personal-acceptance blocker in this validation line.

### Recommended next milestone

**FAR directional response root-cause deep-dive pass only**:

1. instrument/log effective FAR roll target sign and resulting angular velocity sign under left/right and diagonal inputs
2. verify whether mismatch is in FAR target sign, axis-to-frame interpretation, or post-target normalization/cap interaction
3. apply only the minimal FAR-direction fix once root cause is explicitly confirmed
4. immediately re-run the same six-scenario acceptance checklist

## FAR Directional Root-Cause Deep-Dive Pass Only (2026-04-02)

### Scope and guardrails

Deep-dive analysis only for FAR directional acceptance failures.

No broad runtime changes were made in this pass.

### End-to-end FAR path trace

#### A) Gamepad path

1. `GamepadInputHandler.onGenericMotionEvent()` updates left stick axes and trigger throttle axes.
2. `GamepadInputHandler.onKeyDown()/onKeyUp()` updates DAR/FAR state (`darState`) from button key events.
3. `InputController.getInputSnapshot()` forwards gamepad `airRoll`, `airRollIntensity`, and stick channels.
4. `L4PhysicsEngine.performStep()` selects FAR behavior only when `isDARActive && airRoll == FREE`.

Critical observation:
- FAR activation for gamepad is currently key-event-driven (`KEYCODE_BUTTON_R2` binding path), while many controllers expose triggers primarily as analog motion axes.

#### B) On-screen analog path

1. Touch joystick writes `inputX/inputY` into `InputSnapshot`.
2. Touch DAR/FAR state comes from `DARButtonManager.getTouchDARState()`.
3. `DARButtonManager.onTouchButtonPressed()` maps touch mode:
  - `touchDarDirection == 0` -> `FREE`
  - otherwise -> directional DAR (`LEFT` or `RIGHT`)
4. Physics uses FAR branch only when touch path supplies `airRoll == FREE`.

Critical observation:
- Default touch mode in settings is `touchDarDirection = -1` (left), not free.

### Non-FAR vs directional DAR vs FAR comparison

1. Non-FAR path (`darOn=false`):
  - stick drives yaw/pitch only
2. Directional DAR path (`airRoll=LEFT/RIGHT`):
  - constant directional roll + stick-driven yaw/pitch (with DAR caps)
3. FAR path (`airRoll=FREE`):
  - stick-driven roll from stick-X deadzone ramp
  - yaw suppressed
  - pitch from stick-Y

### Root-cause analysis

Primary root-cause category:
- **wrong branch math is not the only issue; branch activation/path-selection is the dominant unresolved problem**.

Evidence-backed explanation:
1. Multiple FAR sign/authority adjustments in physics did not produce expected acceptance movement.
2. Gamepad FAR activation depends on button-key events for `rollFree`; trigger-as-axis hardware can bypass this path.
3. Touch path enters FAR only when touch DAR mode is explicitly set to `Free`; default mode is not free.
4. If FAR branch is not reliably entered, user will effectively experience non-FAR or directional-DAR behavior, which matches the repeated "no change" feedback pattern.

Secondary contributing factor:
- FAR branch math/sign still matters, but it cannot resolve acceptance if the runtime path is not consistently entering FAR state for the tested control method.

### Minimal-correction recommendation

Use the smallest correction that guarantees FAR branch activation consistency before further FAR math tuning:

1. Guarantee gamepad `rollFree` activation from both relevant input forms:
  - key event path (existing)
  - trigger-axis path when configured action corresponds to trigger control
2. Guarantee touch FAR validation path explicitly enters `FREE` mode during test flow (or document required precondition in UI/state).
3. After activation consistency is verified, perform any remaining FAR directional math tuning only if still needed.

### Exact next implementation scope (narrow)

**FAR activation consistency pass only**:

1. Keep DAR behavior and mappings unchanged.
2. Add narrow FAR activation bridging for trigger-axis driven controllers without altering overall control-map semantics.
3. Add explicit runtime/state verification signal (log or debug status note) to confirm `airRoll == FREE` is active during FAR tests.
4. Re-run the same six-scenario on-device acceptance checklist immediately.

## FAR Activation Consistency Pass Only (2026-04-02)

### Scope and guardrails

Implemented only FAR activation-consistency fixes identified by deep-dive analysis.

No changes were made to:
- DAR behavior semantics
- controller mapping defaults
- lifecycle
- rendering
- unrelated systems

### Files changed

- `app/src/main/java/com/l4dar/nativeapp/core/input/GamepadInputHandler.java`
- `app/src/main/java/com/l4dar/nativeapp/core/input/InputController.java`
- `PORT_LOG.md`

### Exact FAR activation root cause

Primary issue:
1. FAR gamepad activation depended on key events (`onKeyDown/onKeyUp` for `gpBindRollFree`) only.
2. On some controllers, trigger-style controls are surfaced primarily via `MotionEvent` axis values, so FREE state was not always entered reliably.

Related validation clarity gap:
3. There was no explicit runtime confirmation that `airRoll == FREE` was actually active during tests.

### Exact activation consistency fix

1. Added narrow trigger-axis FAR activation bridge in `GamepadInputHandler.onGenericMotionEvent()`:
  - when `gpBindRollFree` is trigger-bound (`KEYCODE_BUTTON_R2` or `KEYCODE_BUTTON_L2`), FREE state can now be driven from `AXIS_RTRIGGER`/`AXIS_LTRIGGER` motion values
  - hold mode: FREE state follows axis active/inactive state
  - toggle mode: FREE state toggles on axis rising-edge
2. Kept existing key-event FAR activation path unchanged and merged both via combined FREE-pressed state.
3. Added explicit runtime confirmation logging in `InputController.getInputSnapshot()` for DAR/FAR state changes, including `freeActive` boolean.

### Confirmation that DAR remained unchanged

DAR behavior path remained unchanged in semantics:

1. No directional DAR math or cap logic changes in physics.
2. No mapping-default changes in `SettingsManager`.
3. No DAR mode/state-machine redesign; only FREE activation source coverage was expanded.

### Build verification

- `./gradlew.bat assembleDebug` -> **BUILD SUCCESSFUL**

### Recommended next milestone

**On-device FAR acceptance re-run after activation-consistency fix**:

1. Re-run the same six-scenario checklist.
2. During validation, capture `logcat` entries for `InputController` DAR/FAR state transitions to confirm `freeActive=true` when FAR is expected.
3. If directional issues remain despite confirmed FREE activation, proceed to a strictly FAR-branch math/cap calibration pass.

## FAR Activation-Confirmed On-Device Acceptance Validation Only (2026-04-02)

### Scope and guardrails

Validation-only pass after FAR activation consistency fix.

No runtime code changes were made in this milestone.

### Activation-confirmation method

For each FAR scenario:
1. clear `logcat`
2. run scenario
3. verify `InputController` state logs include `freeActive=true` when FAR should be active

For DAR-only scenario:
1. clean rerun with `logcat` clear
2. verify `freeActive=true` is not present

### Scenario-by-scenario results (required order)

1. Centered FAR
  - FREE confirmed active: **YES**
  - Observed feel/result: works as intended; no autonomous roll
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

2. FAR + stick left/right
  - FREE confirmed active: **YES**
  - Observed feel/result: car still rolls in incorrect directions
  - Pass/Fail: **FAIL**
  - Meets personal acceptance standard: **NO**

3. FAR + stick up/down
  - FREE confirmed active: **YES**
  - Observed feel/result: pitch/roll acceleration feel is now acceptable
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

4. FAR + diagonal
  - FREE confirmed active: **YES**
  - Observed feel/result: feels improved but still uncertain until left/right FAR direction is fully correct
  - Pass/Fail: **INCONCLUSIVE (treat as fail for gate)**
  - Meets personal acceptance standard: **UNSURE**

5. FAR release during motion
  - FREE confirmed active: **YES** (plus release transition observed)
  - Observed feel/result: clean transition; feels fine
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

6. Directional DAR unchanged
  - FREE confirmed active: **NO** (clean DAR-only rerun)
  - Observed feel/result: DAR still fine / unchanged
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

### FAR acceptance decision for V1

- **FAR is NOT accepted for V1 yet**.

Reason:
1. FAR activation is now confirmed working, but left/right FAR directional behavior still fails acceptance.
2. Diagonal FAR remains acceptance-uncertain until left/right directional correctness is resolved.

### Remaining blocker

1. FAR directional correctness under stick left/right remains unresolved.
2. User also noted on-screen analog FAR correctness still needs verification/fix, but may be deferred based on scope decision.

### Recommended next milestone

**FAR directional correctness pass only (activation already confirmed)**:

1. Fix FAR left/right directional behavior in FAR branch math/axis interpretation.
2. Re-check diagonal FAR acceptance once left/right is corrected.
3. Keep DAR behavior and mappings unchanged.
4. Re-run the same activation-confirmed six-scenario checklist.

## FREE-Branch FAR Control-Model Deep-Dive and Correction Pass Only (2026-04-02)

### Scope and guardrails

This milestone applied a narrow correction inside `L4PhysicsEngine.performStep()` FREE branch only.

Unchanged by design:
1. directional DAR behavior/path
2. controller mappings
3. FAR activation plumbing
4. lifecycle/rendering/unrelated systems

### Deep-dive: exact FREE branch math path

Inside `isDARActive` + `isFreeAirRoll` branch:
1. `farRollEff` is computed from horizontal stick magnitude (`abs(jx)`) with deadzone ramp.
2. FREE roll target `wx_raw` is built from `DAR_ROLL_SPEED * farRollEff * sign(jx) * airRollIntensity` (after this pass).
3. FREE yaw target `wy_raw` is suppressed to `0f`.
4. FREE pitch target `wz_raw` remains stick-Y driven (`W_MAX_PITCH * eff * (-jy) * 0.997f`).

### Intended RL-style FREE behavior reference used for correction

Applied model for FREE branch (with centered-stick no-autonomous-roll retained):
1. left/right stick should control roll direction consistently
2. up/down stick should continue controlling pitch
3. diagonals should produce combined roll + pitch (not directionally inverted roll)

### Root cause identified

The FREE roll sign term was inverted relative to directional DAR roll-axis convention.

Specifically before correction:
1. directional DAR RIGHT path uses positive roll target (`airRoll=+1` -> positive `wx_raw`)
2. FREE branch used `(-Math.signum(jx))`, which made stick-right command negative roll
3. this created a control-model mismatch where FREE left/right felt directionally wrong even while FREE activation was confirmed active

### Exact FREE-branch correction applied

File changed:
1. `app/src/main/java/com/l4dar/nativeapp/core/physics/L4PhysicsEngine.java`

Change in FREE branch only:
1. replaced
  - `wx_raw = DAR_ROLL_SPEED * farRollEff * (-Math.signum(jx)) * airRollIntensity`
2. with
  - `wx_raw = DAR_ROLL_SPEED * farRollEff * Math.signum(jx) * airRollIntensity`

No other FREE math terms changed in this pass.

### DAR non-regression note

Directional DAR remained untouched:
1. `!isFreeAirRoll` branch logic unchanged
2. directional DAR caps unchanged
3. input/controller state machine code unchanged

### Build verification

1. `./gradlew.bat assembleDebug` -> **BUILD SUCCESSFUL**

### Remaining FAR limitations

1. On-device acceptance re-check is still required to confirm left/right and diagonal feel after this sign-alignment correction.
2. FREE branch still intentionally suppresses yaw while active (`wy_raw = 0f`) per current scoped model.
3. Any further FAR tuning (if needed) should remain inside FREE branch only.

## Post-Deep-Dive Final FREE-Branch FAR Acceptance Validation Only (2026-04-02)

### Scope and guardrails

Validation-only milestone after FREE-branch sign-alignment correction.

No runtime code changes were made in this milestone.

### Activation confirmation requirement

1. FREE activation remained visible in FAR scenarios via `InputController` logs (`freeActive=true`).
2. Directional DAR scenario was run clean with `freeActive=true` absent.

### Scenario-by-scenario final results (required order)

1. Centered FAR
  - FREE active confirmed: **YES**
  - Observed feel/result: no autonomous roll
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

2. FAR + left/right
  - FREE active confirmed: **YES**
  - Observed feel/result: roll direction remains opposite (left stick causes right roll, right stick causes left roll)
  - Pass/Fail: **FAIL**
  - Meets personal acceptance standard: **NO**

3. FAR + up/down
  - FREE active confirmed: **YES**
  - Observed feel/result: feels fine
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

4. FAR + diagonal
  - FREE active confirmed: **YES**
  - Observed feel/result: diagonal feel is generally fine but left/right roll direction issue remains present
  - Pass/Fail: **FAIL**
  - Meets personal acceptance standard: **UNSURE**

5. FAR release during motion
  - FREE active confirmed: **YES** (with release transition observed)
  - Observed feel/result: release behavior is fine
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

6. Directional DAR unchanged
  - FREE active confirmed: **NO** (clean DAR-only run)
  - Observed feel/result: everything works as intended and feels fine
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

### FAR acceptance decision for V1

- **FAR is NOT accepted for V1 yet**.

Reason:
1. FAR left/right remains directionally incorrect under confirmed FREE-active conditions.
2. Diagonal scenario remains impacted by the same unresolved directional mismatch.

### Remaining blocker

1. FREE-branch left/right roll-direction behavior is still reversed relative to expected feel.

### Final V1/RC recommendation (current acceptance gate)

1. **NO-GO for RC** while FAR remains unaccepted.
2. DAR remains acceptable and non-regressed.

## Final FREE Horizontal Roll-Direction Correction Pass Only (2026-04-02)

### Scope and guardrails

Applied only the remaining FREE-branch horizontal roll-direction fix.

Explicitly unchanged:
1. activation plumbing
2. controller mappings
3. directional DAR behavior
4. vertical FREE behavior
5. release/transition logic
6. unrelated systems

### Exact final blocker root cause

Only one confirmed blocker remained after activation-confirmed validation:
1. FREE branch horizontal roll direction was opposite to intended feel in left/right tests.
2. Diagonal failure was inherited from the same horizontal sign mismatch.

Root location:
1. `L4PhysicsEngine.performStep()` inside `isFreeAirRoll` horizontal roll target construction (`wx_raw`).

### Exact horizontal sign correction

File changed:
1. `app/src/main/java/com/l4dar/nativeapp/core/physics/L4PhysicsEngine.java`

FREE branch change (single-line sign correction only):
1. from:
  - `wx_raw = DAR_ROLL_SPEED * farRollEff * Math.signum(jx) * airRollIntensity`
2. to:
  - `wx_raw = DAR_ROLL_SPEED * farRollEff * (-Math.signum(jx)) * airRollIntensity`

No other FREE branch term changed.

### Explicit preservation of accepted behaviors

Preserved by scope and unchanged code paths:
1. centered-stick non-autonomous FREE behavior (still governed by `farRollEff` deadzone ramp)
2. accepted FREE up/down behavior (`wz_raw` unchanged)
3. FREE release transition behavior (input activation/release plumbing unchanged)
4. directional DAR behavior and caps (`!isFreeAirRoll` path unchanged)

### Build verification

1. `./gradlew.bat assembleDebug` -> **BUILD SUCCESSFUL**

### Expected validation outcome

After this isolated horizontal sign correction:
1. FREE + left/right should now match intended roll direction.
2. FREE + diagonal should improve accordingly because the inherited horizontal-direction defect is corrected.
3. centered FREE, up/down FREE, release transition, and directional DAR are expected to remain unchanged.

## Final Post-Correction FAR Acceptance Validation Only (2026-04-02)

### Scope and guardrails

Validation-only milestone after the final one-line FREE horizontal sign correction.

No runtime code changes were made in this milestone.

### Activation confirmation requirement

1. FREE activation remained visible during FAR scenarios via `InputController` logs (`freeActive=true`).
2. Directional DAR non-regression scenario remained clean with `freeActive=true` absent.

### Scenario-by-scenario results (required order)

1. Centered FAR
  - FREE active confirmed: **YES**
  - Observed feel/result: car does not do anything (expected centered behavior)
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

2. FAR + left/right
  - FREE active confirmed: **YES**
  - Observed feel/result: left/right still roll in opposite directions
  - Pass/Fail: **FAIL**
  - Meets personal acceptance standard: **NO**

3. FAR + up/down
  - FREE active confirmed: **YES**
  - Observed feel/result: feels fine, good result
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

4. FAR + diagonal
  - FREE active confirmed: **YES**
  - Observed feel/result: user perceived FAR not active in feel during test
  - Pass/Fail: **FAIL**
  - Meets personal acceptance standard: **NO**

5. FAR release during motion
  - FREE active confirmed: **YES** (activation and release transition observed in logs)
  - Observed feel/result: user perceived FREE not active in feel when pressing FAR
  - Pass/Fail: **FAIL**
  - Meets personal acceptance standard: **NO**

6. Directional DAR unchanged
  - FREE active confirmed: **NO** (clean DAR-only run)
  - Observed feel/result: unaffected and feels fine
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

### FAR acceptance decision for V1

- **FAR is NOT accepted for V1** after final post-correction validation.

Reason:
1. Required left/right scenario still fails acceptance.
2. Diagonal and release scenarios were not accepted in this run.

### RC/V1 recommendation update

1. Recommendation remains **NO-GO** for RC/V1 acceptance gate.
2. Recommendation did **not** change to GO in this milestone.

### Remaining blocker

1. User-observed FAR behavior remains unacceptable under active FAR usage despite `freeActive=true` log confirmation.
2. Horizontal directional feel is still reported as opposite in left/right scenario.

## FREE-Branch FAR Behavior Model Redesign Pass Only (2026-04-02)

### Scope and guardrails

Redesigned only the FREE branch target construction inside `L4PhysicsEngine.performStep()`.

Explicitly unchanged:
1. directional DAR behavior and caps
2. activation plumbing (GamepadInputHandler, InputController)
3. controller mappings
4. lifecycle / rendering / unrelated systems

### Old model problem

The previous FREE model suppressed yaw (`wy_raw = 0`) and routed horizontal stick entirely to roll:

```
wx_raw = DAR_ROLL_SPEED * farRollEff * sign(jx) * intensity   // roll from jx only
wy_raw = 0                                                     // yaw SUPPRESSED
wz_raw = W_MAX_PITCH * eff * (-jy) * 0.997                    // pitch from jy
```

Root cause of repeated "opposite direction" feel:
1. Yaw was suppressed, so pushing left/right stopped turning the car.
2. The car only rolled on an unexpected heading � not the heading the user anticipated.
3. Both sign polarities felt wrong because the underlying motion (no yaw, only roll) was disorienting regardless of roll direction.
4. Diagonal and release scenarios felt like "FAR not active" because the car was not yawing as expected, making the entire motion feel alien.

### New FREE behavior model

Control terms per input:
1. Horizontal stick (jx):  drives BOTH yaw (normal-flight formula restored) AND roll (FAR authority)
2. Vertical stick (jy):    drives pitch only (unchanged, already accepted)
3. Centered stick:         all targets zero � no autonomous roll (preserved)
4. Roll sign convention:   +jx (stick right) ? positive roll, matching DAR RIGHT acceptance reference

### Exact FREE branch replacement

File: `app/src/main/java/com/l4dar/nativeapp/core/physics/L4PhysicsEngine.java`

```
// Roll: horizontal X-axis deadzone ramp, sign matches DAR convention
wx_raw = DAR_ROLL_SPEED * farRollEff * Math.signum(jx) * airRollIntensity;
// Yaw: restored to normal-flight formula (was wy_raw = 0 in old model)
wy_raw = W_MAX_YAW * eff * (-jx);
// Pitch: unchanged
wz_raw = W_MAX_PITCH * eff * (-jy) * 0.997f;
```

Combined roll+yaw from jx means the combined raw vector can exceed W_MAX; the existing
pre-normalise step scales the combined vector down to W_MAX so neither axis is clipped hard.
Per-axis caps after PD control remain at full W_MAX values for FAR (not DAR-capped 50%).

### Preserved accepted behaviors

1. Centered-stick non-autonomous roll: farRollEff ramp requires jx deflection ? wx=0 at center ?
2. Accepted up/down pitch: wz_raw formula unchanged ?
3. DAR behavior: `!isFreeAirRoll` branch untouched ?
4. Activation plumbing: GamepadInputHandler / InputController unchanged ?

### Build verification

`./gradlew.bat assembleDebug` ? **BUILD SUCCESSFUL**

### Expected validation focus

1. FAR + left/right: car should now yaw AND roll simultaneously � direction should feel intuitive.
2. FAR + diagonal: three-axis motion (yaw + pitch + roll) should feel noticeably active.
3. FAR + centered: no autonomous roll (unchanged from accepted baseline).
4. FAR + up/down: pitch only, no roll or yaw change (unchanged).
5. FAR release: return to normal flight (activation plumbing unchanged).
6. Directional DAR: no regression expected.

## FREE-Branch FAR Behavior Model Redesign Pass Only (2026-04-02)

### Scope and guardrails

Redesigned only the FREE branch target construction inside `L4PhysicsEngine.performStep()`.

Explicitly unchanged:
1. directional DAR behavior and caps
2. activation plumbing (GamepadInputHandler, InputController)
3. controller mappings
4. lifecycle / rendering / unrelated systems

### Old model problem

The previous FREE model suppressed yaw (`wy_raw = 0`) and routed horizontal stick entirely to roll.

Root cause of repeated opposite-direction feel:
1. Yaw was suppressed, so pushing left/right stopped turning the car.
2. The car only rolled on an unexpected heading, not the heading the user anticipated.
3. Both sign polarities felt wrong because the underlying motion (no yaw, only roll) was disorienting regardless of roll direction.
4. Diagonal and release scenarios felt like FAR not active because the car was not yawing as expected, making the entire motion feel alien.

### New FREE behavior model

Control terms per input:
1. Horizontal stick (jx): drives BOTH yaw (normal-flight formula restored) AND roll (FAR authority).
2. Vertical stick (jy): drives pitch only (unchanged, already accepted).
3. Centered stick: all targets zero, no autonomous roll (preserved).
4. Roll sign convention: +jx (stick right) ? positive roll, matching DAR RIGHT acceptance reference.

### Exact FREE branch replacement

File: `app/src/main/java/com/l4dar/nativeapp/core/physics/L4PhysicsEngine.java`

1. Roll: `wx_raw = DAR_ROLL_SPEED * farRollEff * Math.signum(jx) * airRollIntensity`
2. Yaw restored: `wy_raw = W_MAX_YAW * eff * (-jx)` (was `wy_raw = 0` in old model)
3. Pitch unchanged: `wz_raw = W_MAX_PITCH * eff * (-jy) * 0.997f`

Combined roll+yaw from jx means the combined raw vector can exceed W_MAX when jx is deflected.
The existing pre-normalise step caps the combined vector to W_MAX so neither axis is hard-clipped.
Per-axis caps after PD control remain at full W_MAX values for FAR (not DAR-reduced 50%).

### Preserved accepted behaviors

1. Centered-stick non-autonomous roll: farRollEff ramp requires jx deflection, wx=0 at center.
2. Accepted up/down pitch: wz_raw formula unchanged.
3. DAR behavior: `!isFreeAirRoll` branch untouched.
4. Activation plumbing: GamepadInputHandler and InputController unchanged.

### Build verification

`./gradlew.bat assembleDebug` -> **BUILD SUCCESSFUL**

### Expected validation focus

1. FAR + left/right: car should now yaw AND roll simultaneously; direction should feel intuitive.
2. FAR + diagonal: three-axis motion (yaw + pitch + roll) should feel noticeably active.
3. FAR + centered: no autonomous roll (unchanged from accepted baseline).
4. FAR + up/down: pitch only, no roll or yaw change (unchanged).
5. FAR release: return to normal flight (activation plumbing unchanged).
6. Directional DAR: no regression expected.

## FREE-Branch Redesign Acceptance Validation Only (2026-04-02)

### Scope and guardrails

Validation-only pass after FREE behavior model redesign (yaw-restored model).

No runtime code changes were made in this milestone.

### Scenario-by-scenario results (required order)

1. Centered FAR
  - FREE active confirmed: **YES**
  - Observed feel/result: car behaved as expected, no autonomous roll
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

2. FAR + left/right
  - FREE active confirmed: **NO** (log gap; FAR behavior clearly experienced by user)
  - Observed feel/result: roll direction was correct, but car also yawed while rolling
  - Pass/Fail: **FAIL**
  - Meets personal acceptance standard: **NO**
  - Key insight captured: "the purpose of air roll is to swap left/right analog stick yaw for roll, not combine them"

3. FAR + up/down
  - FREE active confirmed: **YES**
  - Observed feel/result: feels fine, works as expected
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

4. FAR + diagonal
  - FREE active confirmed: **NO** (log gap; FAR behavior clearly experienced by user)
  - Observed feel/result: car yawed while rolling; same yaw-combined issue as S2
  - Pass/Fail: **FAIL**
  - Meets personal acceptance standard: **NO**

5. FAR release during motion
  - FREE active confirmed: **NO** (log gap; user experienced clean release)
  - Observed feel/result: feels fine
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

6. Directional DAR unchanged
  - FREE active confirmed: **NO** (clean DAR-only run, no FREE present)
  - Observed feel/result: feels fine, unaffected
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

### FAR acceptance decision for V1

- **FAR is NOT accepted for V1**.

### RC/V1 recommendation

- Remains **NO-GO**.

### Critical design signal captured from this validation

User explicitly confirmed the intended FAR control contract:

1. **Roll direction with `+Math.signum(jx)` is correct.** User confirmed in S2 that the roll direction felt right.
2. **Horizontal stick must REPLACE yaw, not combine with it.** FAR model should suppress yaw and remap horizontal stick to roll only.
3. **Yaw suppression is the correct model.** The redesign's yaw-restoration was the wrong direction.

Required FAR model (precisely stated by user):
- horizontal stick: roll only (yaw suppressed)
- vertical stick: pitch only (unchanged, already accepted)
- centered stick: no autonomous roll (unchanged, already accepted)
- roll sign: `+Math.signum(jx)` (confirmed correct in this validation)

### Remaining blocker

1. Revert yaw suppression (`wy_raw = 0`) while keeping the confirmed correct roll sign (`+Math.signum(jx)`).
2. Previously this combination was attempted but roll sign was `-Math.signum(jx)` — now we know the sign must be positive.

### Recommended next milestone

**FREE-branch final model correction pass only**:
1. Restore yaw suppression: `wy_raw = 0`.
2. Keep confirmed roll sign: `wx_raw = DAR_ROLL_SPEED * farRollEff * Math.signum(jx) * airRollIntensity`.
3. Keep pitch unchanged: `wz_raw = W_MAX_PITCH * eff * (-jy) * 0.997f`.
4. This is the correct Rocket League FAR model: horizontal stick swaps to roll, no yaw during FAR.

## Final FREE Yaw-Suppression + Correct-Roll-Sign Implementation (2026-04-02)

### Scope and guardrails

Implemented only the confirmed FREE control model correction inside `L4PhysicsEngine.performStep()` FREE branch.

Unchanged by design:
1. directional DAR branch behavior and caps
2. activation plumbing (`GamepadInputHandler`, `InputController`)
3. controller mappings
4. vertical FREE behavior
5. unrelated systems

### Final control model

FREE behavior now matches the confirmed intended contract:
1. horizontal stick replaces yaw with roll
2. yaw is fully suppressed during FREE
3. vertical stick continues to control pitch
4. centered stick remains non-autonomous
5. roll sign remains `+Math.signum(jx)` (confirmed correct)

### Exact change (FREE branch only)

File changed:
1. `app/src/main/java/com/l4dar/nativeapp/core/physics/L4PhysicsEngine.java`

Targets in `isFreeAirRoll` now:
1. `wx_raw = DAR_ROLL_SPEED * farRollEff * Math.signum(jx) * airRollIntensity` (kept)
2. `wy_raw = 0f` (restored yaw suppression)
3. `wz_raw = W_MAX_PITCH * eff * (-jy) * 0.997f` (kept)

### Preserved accepted behaviors

1. Centered-stick non-autonomous FREE remains preserved through `farRollEff` deadzone ramp.
2. Accepted FREE up/down pitch behavior is unchanged.
3. Directional DAR remains unchanged (`!isFreeAirRoll` branch untouched).
4. FREE activation/release plumbing remains unchanged (input layer untouched).

### Build verification

1. `./gradlew.bat assembleDebug` -> **BUILD SUCCESSFUL**

### Expected validation outcome

1. FAR + left/right should now retain the confirmed correct roll direction with no yaw coupling.
2. FAR + diagonal should no longer feel like unwanted yaw+roll combination.
3. FAR release and centered behavior should remain as previously accepted.
4. Directional DAR should remain unaffected.

## Final FREE Horizontal Behavior Fix + Validation Gate (2026-04-02)

### Applied change verification

Before validation, FREE branch was re-verified to match required final model:
1. `wx_raw = DAR_ROLL_SPEED * farRollEff * Math.signum(jx) * airRollIntensity` (kept)
2. `wy_raw = 0f` (yaw suppressed)
3. `wz_raw = W_MAX_PITCH * eff * (-jy) * 0.997f` (unchanged)

No additional code changes were required in this milestone because the final model was already present.

### Required gate validation (S2 then S4)

1. Scenario 2 (FAR + left/right)
  - FREE active confirmed: **YES**
  - Observed feel/result: good feel
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

2. Scenario 4 (FAR + diagonal)
  - FREE active confirmed: **YES**
  - Observed feel/result: good feel
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

Both required gate scenarios passed, so full checklist rerun was executed.

### Full six-scenario rerun results

1. Centered FAR
  - FREE active confirmed: **YES**
  - Observed feel/result: good feel
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

2. FAR + left/right
  - FREE active confirmed: **YES**
  - Observed feel/result: good feel
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

3. FAR + up/down
  - FREE active confirmed: **YES**
  - Observed feel/result: good feel
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

4. FAR + diagonal
  - FREE active confirmed: **YES**
  - Observed feel/result: good feel
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

5. FAR release during motion
  - FREE active confirmed: **YES** (activation + release transition observed)
  - Observed feel/result: good feel
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

6. Directional DAR unchanged
  - FREE active confirmed: **NO** (clean DAR-only run)
  - Observed feel/result: good feel
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

### Final acceptance and release recommendation

1. FAR acceptance for V1: **YES**
2. RC/V1 recommendation: **GO**

### Remaining blocker

None from this validation run.

## Post-Implementation Six-Scenario FAR Validation Re-Run (2026-04-02)

### Scope and method

Validation-only run after final FREE yaw-suppression + correct-roll-sign implementation.

For each scenario:
1. clear `logcat`
2. run scenario on device
3. capture user feel/pass/acceptance
4. check `InputController` logs for FREE-active evidence

### Scenario-by-scenario results (required order)

1. Centered FAR
  - FREE active confirmed: **YES**
  - Observed feel/result: feels good; expected centered behavior
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

2. FAR + left/right
  - FREE active confirmed: **YES**
  - Observed feel/result: felt wrong in this run; user reported yaw without expected roll
  - Pass/Fail: **FAIL**
  - Meets personal acceptance standard: **NO**

3. FAR + up/down
  - FREE active confirmed: **YES**
  - Observed feel/result: up/down behavior fine
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

4. FAR + diagonal
  - FREE active confirmed: **YES**
  - Observed feel/result: diagonals worked in this run
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

5. FAR release during motion
  - FREE active confirmed: **YES** (activation + release transition observed)
  - Observed feel/result: release felt fine; note raised that FAR can remain active in toggle mode
  - Pass/Fail: **PASS**
  - Meets personal acceptance standard: **YES**

6. Directional DAR unchanged
  - Initial run FREE check: **UNEXPECTED FREE PRESENT**
  - Clean rerun FREE check: **NO FREE PRESENT**
  - Observed feel/result: DAR unchanged and fine
  - Pass/Fail: **PASS** (based on clean rerun)
  - Meets personal acceptance standard: **YES**

### FAR acceptance status

Current user decision after this run:
1. **FAR acceptance = UNSURE** (user noted FAR unexpectedly stopped working during part of testing and wants more confidence).

### RC/V1 recommendation

User-selected policy for this milestone:
1. **GO (provisional)** even though FAR acceptance is currently unsure.

### Remaining blocker / follow-up note

1. Inconsistent FAR behavior perception remains (notably Scenario 2 result vs later Scenario 4/5 pass observations in same run).
2. Suggested follow-up: one additional short confirmation run focused on Scenario 2 repeatability under clean FREE-active logs.
