# V1 Master Stabilization Checkpoint - 2026-06-05 (Post-PR9 Final)

## Source state

- Base: origin/master (post-PR9)
- Head: 1b8ab2c
- Final validation worktree: C:/Users/itsju/Documents/L4-native-android-app__wt_v1_master_post_pr9_checkpoint
- Final validation branch: stabilize/v1-master-post-pr9-checkpoint-20260605
- Device: R5CY20ETGPL

## Context

PR #9 (Fix gamepad input ownership and Back exit flow) was merged into master before final checkpoint validation.
Validated master includes the gamepad input ownership and Back exit flow fix.
This document reflects the final post-PR9 validated state.

## Build/install

- Command: `cmd /c gradlew.bat :app:assembleDebug`
- Build: Pass
- Install: Pass
- Device: R5CY20ETGPL

## Validation matrix

| Check | Result | Notes |
|---|---|---|
| app launches | Pass | Automated |
| app remains foreground after launch | Pass | Automated |
| car renders | Pass | Manual |
| touch joystick works | Pass | Manual |
| normal touch pitch/yaw correct | Pass | Manual |
| normal gamepad left-stick pitch/yaw works | Pass | Manual |
| right stick does not steer/pitch/yaw the car unless car-control actions are bound to right-stick axes | Pass | Manual |
| right stick camera/look behavior matches current camera-axis bindings | Pass | Manual |
| FAR / Free Air Roll on L1/LB works | Pass | Manual |
| Air Roll Left on Square/X works | Pass | Manual |
| Air Roll Right on Circle/B works during gameplay | Pass | Manual |
| Toggle DAR on X/A works | Pass | Manual |
| DAR with stick up/down works | Pass | Manual |
| FAR centered stability unchanged | Pass | Manual |
| Start opens menu when closed | Pass | Automated |
| Start closes menu when open | Pass | Automated |
| Share toggles day/night theme | Pass | Manual |
| Share does not open menu | Pass | Automated |
| Share does not close menu | Pass | Manual |
| Circle/B closes menu when menu is open | Pass | Automated |
| Circle/B does not exit app during gameplay | Pass | Automated |
| Circle/B does not trigger Back exit popup | Pass | Automated |
| Android Back first press shows "Press Back again to exit" and does not exit | Pass | Automated |
| Android Back second press before timeout shows Leave app? popup | Pass | Automated |
| delayed Android Back after timeout behaves like first Back again | Pass | Automated |
| Leave app? popup No dismisses and stays in app | Pass | Automated |
| Leave app? popup Yes exits app | Pass | Automated |
| pause/resume does not crash | Pass | Automated |
| Stick Size setting exists and changes HUD stick size | Pass | Manual |
| View & HUD settings open | Pass | Automated |

## Confirmations

- Dirty v2-experiments was not modified.
- Parked audit branches were not touched.
- No runtime/source files were changed in PR #8 update.
- No generated/build artifacts included.