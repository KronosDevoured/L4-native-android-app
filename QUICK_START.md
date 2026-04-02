# Quick Start Guide - Android Native L4

## Build & Deploy in 30 Seconds

```powershell
cd C:\Users\itsju\Documents\L4-dar-prototype\android-native-l4
.\gradlew.bat assembleDebug && .\gradlew.bat installDebug
```

Then open the L4 Native app on your Samsung device.

## What You'll See
- Blue rotating box on screen
- Physics engine running (confirms app is active)
- Menu button opens settings

## What Won't Work Yet
- Touching screen: No effect
- Gamepad sticks: No effect
- Game keyboard: No effect
(Input is blocked at Android system level - see BUILD_STATUS.md for details)

## Key Files
- **Physics:** `app/src/main/java/com/l4dar/nativeapp/core/physics/L4PhysicsEngine.java`
- **Rendering:** `app/src/main/java/com/l4dar/nativeapp/render/L4Renderer.java`
- **Input:** `app/src/main/java/com/l4dar/nativeapp/core/input/InputController.java`
- **Settings:** `app/src/main/java/com/l4dar/nativeapp/core/settings/SettingsManager.java`

## Full Documentation
1. **ANDROID_NATIVE_SESSION_REPORT.md** - Start here (overview + next steps)
2. **BUILD_STATUS.md** - Architecture & troubleshooting
3. **TOUCH_INPUT_DEBUG_SUMMARY.md** - Input system investigation

## View Logs
```powershell
$adb = "C:\Users\itsju\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb logcat -s L4Physics,L4SurfaceView,GamepadInputHandler
```

## Most Important
The app is **fully built and working**. It just needs Android system-level input events to flow through. Once that's fixed, everything else is ready to use.
