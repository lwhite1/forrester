# UI Audit — Phase 10 (Rubber-Band Marquee Selection)

Audit of the marquee selection feature in `ModelCanvas.java`, `CanvasRenderer.java`, and `CanvasState.java` against `UI Behaviors.md`.

## Scope

New feature: Rubber-band marquee selection initiated by left-dragging on empty canvas space in Select mode. Includes live selection updates, Shift+drag additive mode, Escape cancellation with selection restoration, cursor feedback, and rendering of the marquee rectangle.

---

## Verification Matrix

Every documented behavior verified against code:

| Documented Behavior | Code Match |
|---------------------|------------|
| Marquee starts on left-drag on empty canvas in Select mode | Yes — control flow prevents non-SELECT modes from reaching marquee code |
| Elements whose center is inside are selected | Yes — `updateMarqueeSelection` checks center (cx, cy) containment |
| Selection updates live during drag | Yes — `handleMouseDragged` calls `updateMarqueeSelection()` + `redraw()` |
| Shift+drag adds to existing selection | Yes — saves initial selection, only clears if `!isShiftDown()` |
| Escape cancels marquee and restores previous selection | Yes — `handleEscape` calls `cancelMarquee()` which restores saved selection |
| Mouse release finalizes selection | Yes — `handleMouseReleased` sets `marqueeActive = false` |
| Semi-transparent blue fill (#4A90D9 at 10%) | Yes — `Color.web("#4A90D9", 0.1)` |
| Dashed blue border (#4A90D9 at 60%, 6/3, 1px) | Yes — `RUBBER_BAND_COLOR`, `setLineWidth(1)`, `setLineDashes(6, 3)` |
| Cursor shows crosshair during marquee | Yes — `updateCursor` checks `marqueeActive` -> `CROSSHAIR` |
| Rendering order: marquee is layer 8 (topmost) | Yes — drawn last in `render()` |
| Escape priority: marquee > reattach > pending flow > tool reset > clear selection | Yes — `handleEscape` checks `marqueeActive` first |
| Click empty space (no drag) clears selection | Yes — zero-area marquee selects nothing |

---

## Regression Check

| Previous Finding | Status |
|---|---|
| All Phase 9 cursor states | **No regressions** — marquee cursor added cleanly |
| Escape priority chain | **No regressions** — marquee cancel inserted at top |
| All `updateCursor()` call sites | **No regressions** — new calls in marquee release path |
| BUG-14, BUG-16, BUG-17 | **Carried** — not affected |
| MINOR-1, MINOR-5, MINOR-6, MINOR-7 | **Carried** — not affected |
| CQ-11 | **Carried** — not affected |
| UX-7, UX-13–16 | **Carried** — not affected |

---

## Remaining Findings

### Minor

| ID | Description |
|----|-------------|
| MINOR-15 | Marquee cursor (CROSSHAIR) set on mouse press but may not visually update until first drag event — imperceptible in practice |
| MINOR-16 | Space pressed during active marquee does not cancel or pause the marquee — the marquee takes priority, which is consistent with the documented cursor priority order |
| MINOR-1 | (Carried) Equation editor overlaps flow name label |
| MINOR-5 | (Carried) Inline editor position assumes canvas at (0,0) |
| MINOR-6 | (Carried) No feedback clicking non-stock during flow creation |
| MINOR-7 | (Carried) No text clipping for equations inside aux rectangles |

### Code Quality

| ID | Description |
|----|-------------|
| CQ-13 | `updateMarqueeSelection` rebuilds selection from scratch on every drag event — acceptable for expected model sizes |
| CQ-11 | (Carried) `updateCursor` hit-tests on every mouse move in SELECT mode |

### Carried Bugs

| ID | Description | Severity |
|----|-------------|----------|
| BUG-14 | Flow equation/name text rendered outside hit area | Cosmetic |
| BUG-16 | Default "0" equation shown on new flows and auxes | Cosmetic |
| BUG-17 | SimulationResult `double[]` rows are mutable | Low-risk |

### UX

| ID | Description |
|----|-------------|
| UX-7 | (Carried) Right-click pans instead of context menu |
| UX-13 | (Carried) No progress indicator during simulation |
| UX-14 | (Carried) No way to export/copy simulation results |
| UX-15 | (Carried) Results window has no chart/graph view |
| UX-16 | (Carried) Multiple results windows accumulate |

---

## Summary

| Category | New | Carried | Open |
|----------|-----|---------|------|
| Major/Bug | 0 | 3 | 3 |
| Minor | 2 | 4 | 6 |
| Code Quality | 1 | 1 | 2 |
| UX | 0 | 5 | 5 |
| **Total** | **3** | **13** | **16** |

No bugs or doc-vs-code mismatches found for Phase 10. The implementation matches the documented behavior exactly. All new findings are low-severity and acceptable.
