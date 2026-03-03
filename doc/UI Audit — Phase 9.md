# UI Audit — Phase 9 (Keyboard Shortcuts & Cursor Feedback)

Audit of cursor feedback and keyboard shortcut changes in `ModelCanvas.java` against `UI Behaviors.md`.

## Scope

New features: (1) Keyboard shortcuts for tool switching (1-5), zoom (Ctrl+Plus/Minus/0), and Escape priority chain. (2) Cursor feedback system (`updateCursor` in `ModelCanvas`) providing visual feedback for all interaction states.

---

## Fixed During Audit

| Finding | Description | Resolution |
|---------|-------------|------------|
| BUG-20 | Cursor not updated after scroll (zoom) — element may scroll under cursor without cursor changing | Added `updateCursor()` to `handleScroll` |
| BUG-21 | Cursor not updated after element deletion — OPEN_HAND persists over empty space | Added `updateCursor()` to `deleteSelected` |
| EDGE-10 | Keyboard zoom (Ctrl+Plus/Minus) doesn't update cursor | Added `updateCursor()` after keyboard zoom handlers |
| EDGE-11 | Ctrl+0 (reset zoom) doesn't update cursor | Added `updateCursor()` after `viewport.reset()` |
| MINOR-12 | Cursor not restored after inline editor closes until next mouse move | Added `updateCursor()` after `requestFocus()` in all 4 editor close callbacks |
| DOC-1 | Doc says inline editor cursor is "Text cursor (I-beam)" but canvas does not set it | Updated doc to clarify TextField handles its own cursor within its bounds |
| BUG-19 | `switchTool` calls `setActiveTool` twice via toolbar callback loop | `switchTool` now delegates to `toolBar.selectTool` (which triggers callback) and only calls `setActiveTool` directly when toolbar is null |
| CQ-10 | `switchTool` fires status update redundantly due to callback loop | Fixed alongside BUG-19 |
| MINOR-14 | Escape with modifier keys duplicates logic with subtly different behavior (missing "clear selection") | Extracted `handleEscape()` method called from a single top-level Escape check; modifier+Escape now includes clear-selection step |
| CQ-9 | `handleKeyPressed` has Escape handling split across two locations | Unified into single `handleEscape()` method; Escape checked before modifier-gated keys |

---

## Regression Check

| Previous Finding | Status |
|---|---|
| BUG-18: Simulation runs on FX thread | **Resolved** — uses background thread |
| UX-3: No cursor shape changes | **Resolved** — full cursor feedback system |
| UX-8: No keyboard shortcuts for tool modes | **Resolved** — 1-5 keys switch tools |
| All other carried findings from Phase 8 | **No regressions** |

---

## Remaining Findings

### Major / Bug

| ID | Description | Severity |
|----|-------------|----------|
| BUG-14 | (Carried) Flow equation/name text rendered outside hit area | Cosmetic |
| BUG-16 | (Carried) Default "0" equation shown on new flows and auxes | Cosmetic |
| BUG-17 | (Carried) SimulationResult `double[]` rows are mutable despite record contract | Low-risk |

### Minor

| ID | Description |
|----|-------------|
| MINOR-1 | (Carried) Equation editor overlaps flow name label |
| MINOR-5 | (Carried) Inline editor position assumes canvas at (0,0) |
| MINOR-6 | (Carried) No feedback clicking non-stock during flow creation |
| MINOR-7 | (Carried) No text clipping for equations inside aux rectangles |

### Code Quality

| ID | Description |
|----|-------------|
| CQ-11 | `updateCursor` performs hit-testing on every mouse move in SELECT mode — acceptable for expected model sizes, only runs in idle state |

### UX

| ID | Description |
|----|-------------|
| UX-7 | (Carried) Right-click pans instead of context menu |
| UX-13 | (Carried) No progress indicator during simulation |
| UX-14 | (Carried) No way to export/copy simulation results |
| UX-15 | (Carried) Results window has no chart/graph view |
| UX-16 | (Carried) Multiple results windows accumulate |

---

## Cursor State Verification

All 10 documented cursor states verified against `updateCursor()`:

| State | Documented Cursor | Code Cursor | Match |
|-------|------------------|-------------|-------|
| Default / idle | Default arrow | `Cursor.DEFAULT` | Yes |
| Hovering element | Open hand | `Cursor.OPEN_HAND` | Yes |
| Hovering cloud/endpoint | Pointing hand | `Cursor.HAND` | Yes |
| Dragging element | Closed hand | `Cursor.CLOSED_HAND` | Yes |
| Space held | Move (four-way) | `Cursor.MOVE` | Yes |
| Panning | Closed hand | `Cursor.CLOSED_HAND` | Yes |
| Placement mode | Crosshair | `Cursor.CROSSHAIR` | Yes |
| Flow pending | Crosshair | `Cursor.CROSSHAIR` | Yes |
| Reattaching endpoint | Closed hand | `Cursor.CLOSED_HAND` | Yes |
| Inline editor active | No change | Early return (no-op) | Yes |

Priority order matches: reattaching/panning/dragging > spaceDown > flowPending/placement > cloud hover > element hover > default.

## `updateCursor()` Call Sites

| Location | Present |
|----------|---------|
| `handleScroll` | Yes |
| `handleMouseMoved` | Yes |
| `handleMousePressed` (6 exit paths) | Yes |
| `handleMouseReleased` (2 paths) | Yes |
| `handleEscape` | Yes |
| `handleKeyPressed` (space, zoom x3) | Yes |
| `handleKeyReleased` (space) | Yes |
| `setActiveTool` | Yes |
| `deleteSelected` | Yes |
| Inline editor close callbacks (4 locations) | Yes |

---

## Summary

| Category | New | Fixed | Carried | Open |
|----------|-----|-------|---------|------|
| Major/Bug | 0 | 3 | 3 | 3 |
| Minor | 0 | 2 | 4 | 4 |
| Code Quality | 1 | 2 | 0 | 1 |
| UX | 0 | 0 | 5 | 5 |
| **Total** | **1** | **7** | **12** | **13** |

All new Phase 9 findings were fixed during the audit. The one remaining code quality note (CQ-11) is accepted as a known trade-off. No critical or high-severity issues remain.
