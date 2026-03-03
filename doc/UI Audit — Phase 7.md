# UI Audit Report — Phase 7 (Inline Equation Editing)

Audit of all `forrester-app` canvas code after the inline equation editing feature was added to flows and auxiliaries.

## Scope

New feature: inline equation editing for flows and auxiliaries. Changes across `ModelEditor.java`, `ModelCanvas.java`, `ElementRenderer.java`, `CanvasRenderer.java`, and `ModelEditorTest.java`.

---

## Regression Check — Previously-Fixed Findings

| Previous Finding | Status |
|---|---|
| BUG-7: Inline editor width not scaled by zoom | **Still fixed** |
| BUG-8: Duplicate CLOUD_OFFSET | **Still fixed** |
| BUG-9: Cloud position logic duplication | **Still fixed** |
| BUG-10: Reattachment self-loop | **Still fixed** |
| BUG-11: Constant value editor Y offset ignores zoom | **Still fixed** |
| BUG-12: reconnectFlow doesn't validate stockName | **Still fixed** |
| EDGE-1: Self-loop flow creation | **Still fixed** |
| EDGE-3: Name validation | **Still fixed** |
| EDGE-5: Equation references on delete | **Still fixed** |
| EDGE-6: Cloud-to-cloud | **Still fixed** |
| QUALITY-10: drawFlow coord convention | **Still fixed** |
| UX-5: No cloud preview during reattachment | **Still fixed** |
| UX-9: No status bar | **Fixed** — StatusBar exists |

**No regressions detected.** All previously-fixed findings remain fixed.

---

## Major / Bug

### BUG-14 [NEW]: Flow equation/name text rendered outside hit area
**File:** `ElementRenderer.java:80,88`

The flow diamond is only 30×30 pixels (`FLOW_INDICATOR_SIZE`). Name text is drawn below the diamond at `cy + half + 4` and the equation at `cy + half + 18`. Both are outside the diamond's hit-test area, so clicking on the name/equation text does not trigger inline editing — only clicking the small diamond does.

Additionally, the fixed 14-pixel gap between name and equation means long names can overlap with equation text.

### BUG-16 [NEW]: Default "0" equation shown on all new flows and auxes
**File:** `ElementRenderer.java:83,125`

When a flow or aux is created, its default equation is `"0"`. The renderer shows the equation whenever it's non-null and non-blank. Since `"0"` passes both checks, every new flow/aux displays `"0"` as its equation, adding visual noise.

---

## Minor

### MINOR-1 [NEW]: Equation editor overlaps flow name label
**File:** `ModelCanvas.java:383,422`

The equation editor position uses `screenY + 16 * viewport.getScale()`, placing it 16 world-units below element center. For flows, the name label starts at `cy + half + 4` (19 world-pixels below center). At normal zoom, the equation editor and name label will overlap.

### MINOR-4 [NEW]: Flow equation editor field is too narrow (50px at 100% zoom)
**File:** `ModelCanvas.java:294`

`fieldWidth = (LayoutMetrics.widthFor(type) + 20) * viewport.getScale()`. For flows, `widthFor(FLOW) = 30`, giving a 50px-wide field at 100% zoom — too narrow for equations like `Stock_1 * Contact_Rate`.

### MINOR-7 [NEW]: No text clipping for equations inside aux rectangles
**File:** `ElementRenderer.java:117-131`

Long equations overflow the aux rectangle (100×55px) horizontally. Pre-existing issue for names, now extended to equations.

### MINOR-3 [NEW]: `drawAux` Javadoc not updated for new `equation` parameter
**File:** `ElementRenderer.java:96`

### MINOR-5 [CARRIED]: UX-4 — Inline editor position assumes canvas at (0,0)
Non-issue in current layout, but would break if layout changes.

### MINOR-6 [CARRIED]: EDGE-2 — No feedback clicking non-stock during flow creation

---

## Code Quality

### CQ-1 [NEW]: Duplication across name-then-chain edit methods
**File:** `ModelCanvas.java:316-329, 360-372, 398-410`

`startConstantNameEdit`, `startFlowNameEdit`, and `startAuxNameEdit` follow the exact same pattern. Could be extracted into a shared helper.

### CQ-2 [NEW]: `findFlow`/`findAux`/`findConstant` are linear scans
**File:** `ModelCanvas.java:446-470`

Linear list scans to find elements by name, versus `HashMap` lookups used in `CanvasRenderer`. Fine for typical model sizes but inconsistent.

### CQ-3 [NEW]: No equation syntax validation in `setFlowEquation`/`setAuxEquation`
**File:** `ModelEditor.java:322-354`

Any non-blank string is accepted. Invalid equations are stored silently and only fail at simulation time. Acceptable for now since `ExprParser` validates at compile time.

### CQ-4 [NEW]: Magic numbers for text positioning in ElementRenderer
**File:** `ElementRenderer.java:80, 88, 122, 130`

The values `4`, `18`, `-6`, `8` should be extracted into named constants in `LayoutMetrics`.

### CQ-5 [NEW]: `startInlineEdit` growing if/else chain
**File:** `ModelCanvas.java:296-310`

Now four branches (CONSTANT, FLOW, AUX, default STOCK). Consider a strategy pattern or method map.

### QUALITY-4 [CARRIED]: `double[]` for positions instead of record

---

## Test Coverage Gaps

| Gap | Description |
|-----|-------------|
| TEST-GAP-10 [NEW] | No test for equation editing chaining (name → equation) — requires JavaFX |
| TEST-GAP-11 [NEW] | No test for Escape cancellation during chained editing |
| TEST-GAP-12 [NEW] | No test for equation rendering in `ElementRenderer` — requires GraphicsContext |
| TEST-GAP-13 [NEW] | No test for `setFlowEquation`/`setAuxEquation` interaction with rename (e.g., rename aux referenced in flow equation) |
| TEST-GAP-14 [NEW] | No test for equation cleanup in aux equations when elements are deleted (only flow equations tested) |
| TEST-GAP-6 [CARRIED] | No test for `addFlow` with nonexistent stock names |
| TEST-GAP-1 [CARRIED] | No tests for ModelCanvas event handling (requires JavaFX) |
| TEST-GAP-2 [CARRIED] | No tests for InlineEditor (requires JavaFX) |
| TEST-GAP-7 [CARRIED] | No tests for ConnectionRenderer/SelectionRenderer (requires JavaFX) |
| TEST-GAP-8 [CARRIED] | No tests for CanvasToolBar (requires JavaFX) |

---

## UX

| Finding | Description |
|---------|-------------|
| UX-10 [NEW] | Default "0" equation displayed on all new flows/auxes — visual noise |
| UX-11 [NEW] | Subtle distinction between name and equation text on flows (2pt font, color shift only) |
| UX-12 [NEW] | Long equations not truncated — overflow element bounds |
| UX-2 [CARRIED] | Escape in SELECT mode does nothing visible |
| UX-3 [CARRIED] | No cursor shape changes |
| UX-6 [CARRIED] | No undo support |
| UX-7 [CARRIED] | Right-click pans instead of context menu |
| UX-8 [CARRIED] | No keyboard shortcuts for tool modes |

---

## Summary

| Severity | New | Carried | Total |
|----------|-----|---------|-------|
| Critical | 0 | 0 | 0 |
| Major/Bug | 2 | 0 | 2 |
| Minor | 4 | 2 | 6 |
| Code Quality | 5 | 1 | 6 |
| Test Gaps | 5 | 5 | 10 |
| UX | 3 | 5 | 8 |
| **Total** | **19** | **13** | **32** |

### Priority Recommendations

1. **BUG-14**: Enlarge flow hit area to encompass name/equation text, or add separate click targets for text labels
2. **BUG-16 / UX-10**: Suppress display of default `"0"` equation (check `equation.equals("0")` and skip)
3. **MINOR-4**: Increase minimum width for equation text fields (150-200px minimum)
4. **CQ-1**: Extract duplicated name-edit-then-chain pattern into a shared helper
5. **TEST-GAP-14**: Add tests for equation cleanup when deleting auxes referenced in other equations

### Test count: 156 passing (up from 146)
