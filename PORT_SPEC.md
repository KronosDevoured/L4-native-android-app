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
