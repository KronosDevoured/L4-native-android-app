# V1 Master Stabilization Checkpoint - 2026-06-05

## Source state

- Base: origin/master
- Head: 34cc684
- Worktree: C:/Users/itsju/Documents/L4-native-android-app__wt_v1_master_checkpoint
- Branch: stabilize/v1-master-checkpoint-20260605

## Build/install

- `cmd /c gradlew.bat :app:assembleDebug` passed
- Debug APK installed successfully
- Device: R5CY20ETGPL

## Manual smoke test

| Check | Result | Notes |
|---|---|---|
| app launches | Pass | Verified earlier via adb launch |
| car renders | Pass | |
| touch joystick works | Pass | |
| normal pitch/yaw correct | Pass | |
| FAR centered stable | Pass | |
| FAR left/right/up/down works | Pass | |
| DAR left/right works | Pass | |
| DAR with stick up/down works | Pass | |
| Stick Size setting exists and changes HUD stick size | Pass | |
| View & HUD settings open | Pass | |
| pause/resume does not crash | Pass | Includes post-interaction pause/resume |

## Confirmations

- Dirty v2-experiments was not modified.
- Parked audit branches were not touched.
- No runtime code changes.
- No build/config changes.
- No generated artifacts included.