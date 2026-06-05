# PORT_LOG UI/REN Audit Extract (Pass 1)

Source: selective extraction from dirty-workspace PORT_LOG.md for quarantine-safe docs preservation.
Scope: UI/REN audit content only (U49, U47, U46).

## U49 - REN-002 Camera Equivalence Review (2026-06-04)

Classification summary:
1. REN-002 was not closed as strict source parity in this review.
2. Android camera preset/FOV/distance/sensitivity controls were treated as approved Android enhancements.
3. Those enhancements did not by themselves close strict web parity for REN-002.

Source-vs-Android conclusion:
1. Web View and HUD contract includes Zoom, Show Arrow, Show Circle, Arrow Size, Stick Size, Minimal UI.
2. Android had additional camera controls (presets/FOV/distance/sensitivity) that were accepted as enhancements.
3. Remaining parity items were explicitly deferred pending scope decision.

## U47 - UI-004 Terminology Parity Closure (2026-06-04)

Classification summary:
1. UI-004 recorded as a confirmed parity gap and closure ticket.

Terminology contract summary:
1. Use Gamepad for controller settings surface wording.
2. Use View & HUD wording.
3. Preserve DAR/FAR wording where already clear.

Guardrail summary:
1. IDs unchanged.
2. Persistence keys unchanged.
3. Binding behavior unchanged.
4. Reset behavior unchanged (wording only).
5. FAR/DAR semantics unchanged.

## U46 - UI-001 Web-Parity Menu Card Regrouping (2026-06-04) [Trimmed]

Classification summary:
1. UI-001 recorded as a confirmed parity gap.

Mapping summary (trimmed):
1. Android card grouping was adjusted to align with web-style structure: Rotation, Gamepad, Dynamics, Car, View and HUD.
2. Controls were regrouped by card intent without changing underlying control identity.

Source-vs-Android conclusion:
1. Regrouping intent was parity-oriented for menu structure.
2. Existing control IDs and persisted settings were preserved.

Behavior guardrails:
1. Persistence keys/defaults preserved.
2. Reset handlers preserved.
3. FAR/DAR semantics unchanged.
4. Input semantics/controller mapping behavior unchanged.
5. Camera runtime behavior unchanged.

---

Pass-1 exclusions:
1. U45 excluded by explicit decision for this pass.
2. U50/U51/U52 excluded as already preserved in clean branches.
3. Runtime-status assertions and mixed Sprint A/B/C chains excluded.
4. Perf recovery, controller/profile chains, and template scaffolding excluded.
