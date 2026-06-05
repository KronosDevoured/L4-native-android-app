# L4 Roadmap Companion: Source Anchor Sheet

## Purpose

This companion sheet maps each roadmap feature ID to:
1. The best known web source anchor.
2. Current Android implementation anchor.
3. Evidence location in local project docs.
4. Remaining anchor debt.

Primary roadmap: `PORT_ROADMAP_FULL_RECONSTRUCTION.md`

## Anchor Confidence

- A: Exact source anchor verified directly in local web files.
- B: Source anchor evidenced through audited references in `PORT_LOG.md`.
- C: Inferred anchor family only, still needs direct source confirmation.

Note: The web source tree (`../docs/...`) is not currently present in this workspace, so this sheet is currently B/C-only and should be upgraded to A after source sync.

## Core Mapping Table

| ID | Web source anchor (current best) | Android anchor | Evidence | Confidence | Next action |
|---|---|---|---|---|---|
| PHY-001 | `docs/js/modules/physics.js` `updatePhysics(...)` | `app/src/main/java/com/l4dar/nativeapp/core/physics/L4PhysicsEngine.java` | `PORT_LOG.md` lines 1099-1115 | B | Confirm exact web function/line anchors |
| PHY-002 | `docs/js/modules/constants.js` `PHYSICS_DEFAULTS`, `DAR_ROLL_SPEED` | `app/src/main/java/com/l4dar/nativeapp/core/physics/L4PhysicsDefaults.java` | `PORT_LOG.md` lines 408, 1100, 1111 | B | Capture exact constant keys + line anchors |
| PHY-003 | `docs/js/modules/settings.js` + `docs/js/modules/physics.js` tunable usage | `L4PhysicsEngine.java`, `SettingsManager.java` | `PORT_LOG.md` lines 1540, 1818-1888 | B | Build explicit tunable-by-tunable mapping |
| PHY-004 | `docs/js/modules/settings.js` game speed setting | `SettingsManager.java`, runtime step usage | `PORT_LOG.md` lines 410, 1818-1888 | B | Add direct read/write anchor in web code |
| TIM-001 | `docs/js/main.js` `tick()` + physics call chain | `render/L4Renderer.onDrawFrame()` | `PORT_LOG.md` lines 433, 1103 | B | Confirm exact frame delta clamp anchors |
| TIM-002 | `docs/js/main.js` lifecycle/resume behavior family | Activity + renderer lifecycle paths | `PORT_LOG.md` perf/lifecycle checkpoints | C | Add direct lifecycle anchor points |
| TIM-003 | Web runtime trajectory behavior via physics loop | `PhysicsIntegrationTest.java` + runtime loop | `PORT_LOG.md` lines 1549, 1598-1608 | B | Add variable-dt trajectory parity scenario anchors |
| INP-001 | `docs/js/modules/input.js` touch pipeline | `InputController.java` touch handling | `PORT_LOG.md` lines 268, 531, 653 | B | Add exact touch processing function anchors |
| INP-002 | `docs/js/modules/input/airRollController.js` DAR semantics | `DARButtonManager.java`, handlers | `PORT_LOG.md` lines 269, 531, 653 | B | Capture exact DAR state transition callsites |
| INP-003 | `docs/js/modules/settings.js` DAR mode/direction settings | `SettingsManager.java`, `MainActivity.java` | `PORT_LOG.md` lines 412, 488-536 | B | Map each setting key 1:1 |
| INP-004 | `docs/js/modules/input.js` + `airRollController.js` FAR interaction | touch + DAR integration paths | `PORT_LOG.md` FAR follow-up notes | C | Add focused FAR source trace and test matrix |
| GP-001 | `docs/js/modules/input/gamepadInput.js` core axes/buttons | `GamepadInputHandler.java`, `InputController.java` | `PORT_LOG.md` lines 270, 493-536 | B | Add exact mapping function anchors |
| GP-002 | `docs/js/modules/controlsMenu.js` remap flows | `MainActivity.java` binding UI | `PORT_LOG.md` lines 983, 1016-1046, 2132 | B | Build full action matrix parity sheet |
| GP-003 | `docs/js/modules/input.js` + `gamepadInput.js` dual-stick path | gamepad setting/runtime paths | `PORT_LOG.md` lines 262, 268-271, 1021, 2133 | B | Reconfirm current post-cleanup parity intent |
| GP-004 | `docs/index.html` gamepad deadzone UI + `gamepadInput.js` runtime usage | controller settings UI + `SettingsManager.java` | `PORT_LOG.md` lines 1039-1040 | B | Add exact slider key names and defaults |
| GP-005 | `docs/index.html` sensitivity sliders + `gamepadInput.js` usage | `SettingsManager.java` and controller pipeline | `PORT_LOG.md` lines 1023, 1043, 2134 and Sprint C U44 | B | Upgrade to A-confidence by pinning exact source line anchors when web tree is locally available |
| GP-006 | `docs/index.html` preset selector + `gamepadInput.js` preset logic | `SettingsManager` preset model state + model-specific DAR/openMenu/boost/pause/restart/retry/theme mappings + `MainActivity` selector/remap UI + menu/system routing + remap conflict resolution semantics + `GamepadInputHandler` boost runtime path + reset/profile paths | `PORT_LOG.md` lines 484-514, 1044, 2134 and Sprint C U35-U43 | B | Complete remaining broader controls surface parity (primarily deeper controls/remap UX scope) |
| GP-007 | `docs/js/modules/controlsMenu.js` profile/reset intents | `SettingsManager.java` profile save/load/reset/import/export semantics + `MainActivity` remap ownership/import-export UX + Robolectric roundtrip/clear-slot/cross-slot/import validation + schema anchor table | `PORT_LOG.md` controller/profile tickets (Sprint C U40-U43) | C | Expand parity checks for deeper controls UX polish and edge-case parity |
| UI-001 | `docs/index.html` section layout/labels + `settings.js` semantics | `activity_main.xml`, `MainActivity.java` | `PORT_LOG.md` lines 899-968 | B | Confirm each section item parity row-by-row |
| UI-002 | `docs/index.html` View and HUD controls | currently hidden View/HUD card path | `PORT_LOG.md` lines 917, 968, 2136 | B | Inventory full View/HUD controls for implementation phase |
| UI-003 | `docs/index.html` top shell actions (`#menuBtn`, `#themeBtn`) | top-row action controls | `PORT_LOG.md` line 907 | B | Verify behavior details for theme persistence/toggle |
| UI-004 | `docs/index.html` and `settings.js` terminology intent | strings and labels in Android UI | `PORT_LOG.md` language drift notes | C | Add terminology conformance checklist |
| MOD-001 | `docs/js/main.js` free-flight loop | native runtime baseline | `PORT_LOG.md` baseline parity sections | C | Attach exact mode entry/exit anchors |
| MOD-002 | `docs/index.html` Dynamics surface + settings semantics | dynamics summary/controls in Android | `PORT_LOG.md` lines 924, 950, 1650 | B | Decide full parity depth vs approved simplification |
| MOD-003 | `docs/index.html` ring mode surface | ring overlay paths | `PORT_LOG.md` lines 918, 924, 2137 | B | Implement only when defer lifted |
| MOD-004 | web Rhythm mode sources in app shell and modules | not currently surfaced in Android | `PORT_SPEC.md` + policy notes in `PORT_LOG.md` | C | Collect exact source anchors when policy opens |
| MOD-005 | web Developer mode sources | not currently surfaced in Android | `PORT_SPEC.md` + policy notes in `PORT_LOG.md` | C | Collect exact source anchors when policy opens |
| PER-001 | `docs/js/modules/settings.js` persistence keys/defaults | `SettingsManager.java` | `PORT_LOG.md` lines 409-412 | B | Produce key-by-key parity ledger |
| PER-002 | binding ownership flows in `controlsMenu.js` and input modules | explicit non-silent remap guardrails | `PORT_SPEC.md` + controller cleanup tickets in `PORT_LOG.md` | B | Add negative test cases for launch-time migration |
| PER-003 | legacy key behavior around web settings evolution | migration helpers in `SettingsManager.java` | `PORT_LOG.md` migration tickets U31/U34 | B | Track migration versioning policy document |
| REN-001 | `docs/js/main.js` render/bootstrap + scene parity intent | `L4Renderer`, `L4SurfaceView`, renderers | `PORT_LOG.md` render/perf sections | C | Add renderer object-level source map |
| REN-002 | camera control semantics in web shell/modules | camera settings/runtime in Android | `REN_002_CAMERA_EQUIVALENCE.md` + Sprint C U49 in `PORT_LOG.md` | B | Hold closure until user approves/defer decision for Android camera enhancements |
| REN-003 | HUD control nodes in `docs/index.html` | HUD/overlay control paths | `PORT_LOG.md` lines 917, 968, 2136 | B | Enumerate full HUD control inventory |
| REN-004 | visual treatment in web renderer/styles | native renderer polish track | defer list `DFR-V1-007` | B | Define post-V1 visual parity rubric |
| OPS-001 | web offline-ready behavior assumptions | packaged native assets/no network runtime | gates and architecture docs | C | Add explicit offline boot flow evidence links |
| OPS-002 | web-equivalent runtime stability expectations | perf harness + `scripts/perf-gates.json` | `PORTING_GATES.md`, perf baseline artifacts | B | Keep rolling perf baseline snapshots |
| OPS-003 | representative device/input coverage expectation | manual validation sheets + regression logs | `D2_MANUAL_CAMERA_VALIDATION_SHEET.md`, `PORT_LOG.md` | B | Close hardware gamepad lifecycle validation debt |

## Deferred IDs Crosswalk

The following roadmap IDs map directly to finalized V1 defer IDs already documented in `PORT_LOG.md` lines 2132-2138:

1. GP-002 -> DFR-V1-001
2. GP-003 -> DFR-V1-002
3. GP-005 + GP-006 -> DFR-V1-003
4. Additional non-core remap scope under GP-002/GP-007 -> DFR-V1-004
5. UI-002 + REN-003 -> DFR-V1-005
6. MOD-003 -> DFR-V1-006
7. REN-004 -> DFR-V1-007

## Next Upgrade Step (B/C to A)

To upgrade this sheet to A-grade anchors:
1. Sync the web source tree into this workspace or mount it read-only.
2. Add exact function or DOM anchors for each ID.
3. Append line-level references under a new column: Web line anchor.
4. Mark each row A only after direct verification.
