# L4 Native Android (Standalone)

This module is the native Android port baseline for L4.

## Scope Baseline
- Keep: Free Flight and Dynamics settings parity.
- Remove: Developer Mode.
- Exclude for now: Rhythm Mode.
- Defer: Ring Mode integration until core native parity is stable.

## Runtime Policy
- Android SDK/NDK only.
- No WebView runtime.
- No Cordova/Capacitor runtime.
- No third-party game/render/audio runtime libraries.
- App must be fully operational offline after install.

## Source of Truth
Behavior, visuals, and tuning values must match web L4 source unless user-approved changes are made.

Primary references:
- ../docs/js/modules/constants.js
- ../docs/js/modules/physics.js
- ../docs/js/modules/settings.js
- ../docs/js/main.js

## Product Guardrail: Binding Ownership

Input binding ownership belongs to the user after configuration.

1. Any gameplay action may be bound to any supported gamepad button, trigger, or axis.
2. Runtime systems must not silently remap or migrate user bindings during normal launch.
3. Automatic setting rewrites are allowed only for explicit user-driven actions (for example: pressing reset, applying a preset, importing a profile).
4. Before introducing a convenience feature, evaluate whether it can fundamentally change final product behavior; reject or redesign if it can override user intent.

## Project Layout
- app/src/main/java/com/l4dar/nativeapp/core/math: Native vector/quaternion primitives.
- app/src/main/java/com/l4dar/nativeapp/core/physics: Physics constants and engine modules.
- app/src/main/java/com/l4dar/nativeapp/core/settings: Settings persistence mapped from darSettings keys.
- app/src/main/java/com/l4dar/nativeapp/render: Native OpenGL surface and renderer bootstrap.

## Build Prerequisites
- Android Studio Iguana+ or command-line Android SDK tools.
- Android SDK Platform 35 installed.
- Java 17.
- Gradle available locally to generate wrapper on first setup.

## First-time Setup
From this directory, generate wrapper scripts:

```powershell
gradle wrapper --gradle-version 8.9
```

Then build debug APK:

```powershell
.\gradlew.bat assembleDebug
```

## Current Implementation Status
- Native app module scaffolded with API 29+ baseline.
- MainActivity + GLSurfaceView + OpenGL renderer bootstrap added.
- Physics defaults mirrored from web constants.
- SettingsManager created with SharedPreferences key parity for initial core keys.
- Math primitives (Vec3/Quat) added for upcoming physics integration.
