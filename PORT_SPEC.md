# Native Port Specification

## Document Role

This file defines the product-level constraints for the Android port and is the first reference for implementation decisions.

## Required Workflow References

Use these docs together when making changes:

1. `PORT_SPEC.md` (this file) for intent and constraints
2. `PORTING_GATES.md` for pass/fail criteria
3. `PORT_LOG.md` for active execution state and next implementation steps

## Product Guardrail: End-Product Integrity

Every feature/system change must be evaluated against the final product behavior, not short-term convenience.

Required checks:

1. Could this change silently alter how the product behaves for existing users?
2. Could this change undermine user control of configured bindings/settings?
3. Could this change conflict with parity intent without explicit approval?

If any answer is yes, redesign before merge.

## Product Guardrail: Binding Ownership

1. Any gameplay action must remain bindable to any supported gamepad button, trigger, or axis.
2. Launch-time auto-migration or silent rewrite of user bindings is disallowed.
3. Binding changes may occur only through explicit user actions (bind capture, reset actions, preset apply, profile load/import).

# Native Port Gates

## Gate A: Dependency Integrity
- No WebView runtime classes in production variant.
- No Cordova/Capacitor runtime in production variant.
- No third-party runtime game/render/audio libs.
- No remote URL references in app code/assets for production path.

## Gate B: Offline Integrity
- Fresh install on airplane mode starts successfully.
- Core free-flight loop runs without network.
- Required assets are packaged and load from local app storage.

## Gate C: Parity Integrity
- Physics constants match web source.
- Deterministic fixed-step update loop produces parity-tolerant outputs.
- Input mappings and DAR behavior match web implementation.

## Gate D: Persistence Integrity
- Settings survive app restart.
- Settings survive process death and recreation.

## Gate E: Performance Integrity
- Stable frame pacing on API 29+ target device.
- No severe GC stalls in baseline free-flight flow.
