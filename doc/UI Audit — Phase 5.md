# UI Audit Report — Phase 5 (Refactor & Flow Reattachment)

Audit of all `forrester-app` canvas code after Phase 5 implementation.

## Status of Phase 4 Findings

| Phase 4 Finding | Status |
|---|---|
| BUG-7: Inline editor TextField width not scaled by zoom | **Fixed in Phase 6** |
| EDGE-1: Self-loop flow (same stock source/sink) | **Still open** (flow creation path) |
| EDGE-2: Clicking non-stock in flow mode = cloud | **Still open** |
| EDGE-3: No name validation for identifiers | **Still open** |
| EDGE-4: Rename to blank gives no feedback | **Still open** |
| EDGE-5: Equation references to deleted elements not cleaned | **Still open** |
| QUALITY-2: String literals for element types | **Fixed** — `ElementType` enum |
| QUALITY-3: ModelCanvas too many responsibilities | **Improved** — extracted 3 classes, ~760→560 lines |
| QUALITY-4: `double[]` for positions | **Still open** |
| QUALITY-5: Magic number cloud line width | **Still open** |
| UX-2: Escape in SELECT mode does nothing visible | **Still open** |
| UX-3: No cursor shape changes | **Still open** |
| UX-4: Inline editor assumes canvas at (0,0) | **Still open** |
| TEST-GAP-1: No tests for ModelCanvas event handling | **Still open** |
| TEST-GAP-2: No tests for InlineEditor | **Still open** |
| TEST-GAP-4: No tests for `formatValue` | **Still open** |
| TEST-GAP-5: No tests for `clipToBorder` | **Still open** |
| TEST-GAP-6: No test for `addFlow` with nonexistent stock names | **Still open** |

---

## Major / Bugs

### BUG-8 [NEW]: Duplicate CLOUD_OFFSET constant
**Files:** `ConnectionRenderer.java:15`, `FlowEndpointCalculator.java:14`

Both classes declare `private static final double CLOUD_OFFSET = 80`. If either is changed independently, cloud rendering and hit-testing will become misaligned — clouds drawn in one place but clickable in another.

**Fix:** Define `CLOUD_OFFSET` once in `LayoutMetrics` and reference from both.

### BUG-9 [NEW]: Cloud position logic duplicated between renderer and hit-tester
**Files:** `ConnectionRenderer.java:36-69`, `FlowEndpointCalculator.java:134-171`

`ConnectionRenderer.drawMaterialFlow` computes cloud positions inline using clipped border edge direction. `FlowEndpointCalculator.computeCloudPosition` uses stock center direction. For non-axis-aligned layouts, the direction vectors will differ, causing rendering/hit-testing mismatch.

**Fix:** Have `ConnectionRenderer` call `FlowEndpointCalculator.cloudPosition()` instead of recomputing inline.

### BUG-10 [NEW]: Reattachment can create self-loop or no-op reconnection
**Files:** `ModelCanvas.java:337-343`, `ModelEditor.java:262-278`

No check prevents dragging a source endpoint onto the flow's own sink stock (creating source==sink self-loop), or dropping on the same stock it was already connected to (unnecessary object rebuild).

**Fix:** In `completeReattachment`, check `stockHit` against the existing connection and the opposite endpoint.

---

## Minor

### BUG-7 [Carried]: Inline editor TextField width not scaled by zoom
**File:** `ModelCanvas.java:237`

Field width uses world-space units. At high zoom the TextField is too small; at low zoom it's too large.

### BUG-11 [NEW]: Constant value editor Y offset ignores zoom
**File:** `ModelCanvas.java:278`

The `screenY + 16` offset is a fixed pixel distance. At different zoom levels, the value editor appears in the wrong position relative to the element.

### BUG-12 [NEW]: `reconnectFlow` does not validate stockName exists
**File:** `ModelEditor.java:262-278`

Accepts any string as `stockName` without checking that a stock with that name exists.

### EDGE-1 [Carried]: Self-loop flow allowed
**File:** `FlowCreationController.java:55-80`

Clicking the same stock twice during PLACE_FLOW creates source==sink. Diamond overlaps stock.

### EDGE-2 [Carried]: No feedback when clicking non-stock during flow creation
**File:** `FlowCreationController.java:126`

### EDGE-3 [Carried]: No name validation for identifiers
**File:** `ModelCanvas.java:242-246`

### EDGE-4 [Carried]: Rename to blank gives no feedback
**File:** `ModelCanvas.java:243-244`

### EDGE-5 [Carried]: Equation references to deleted elements not cleaned
**File:** `ModelEditor.java:130`

### EDGE-6 [NEW]: Cloud-to-cloud flow creation allowed
**File:** `FlowCreationController.java:37-80`

Clicking empty space twice creates a flow with both ends disconnected. Such flows cannot be simulated.

### UX-4 [Carried]: Inline editor position assumes canvas at (0,0)
**File:** `ModelCanvas.java:235-236`

### UX-5 [NEW]: No cloud preview during reattachment drop on empty space
**Files:** `ModelCanvas.java:337-343`, `CanvasRenderer.java:237-249`

When dragging during reattachment, hovering over a stock shows a dashed highlight, but dropping on empty space (disconnect) has no visual preview.

### UX-6 [NEW]: No undo support
All editing operations are irreversible.

---

## Code Quality

### QUALITY-6 [NEW]: Misleading identical if/else in `computeCloudPosition`
**File:** `FlowEndpointCalculator.java:145-151`

Both branches compute `dx = midX - oppX; dy = midY - oppY;` identically. The negation for sink direction is on lines 153-156. The if/else is misleading.

**Fix:** Remove the if/else; compute `dx`/`dy` once, then negate for sink.

### QUALITY-4 [Carried]: `double[]` for positions instead of record
### QUALITY-5 [Carried]: Magic numbers in renderers

### QUALITY-7 [NEW]: `drawCloudAt` is trivial delegate
**File:** `ConnectionRenderer.java:150-152`

### QUALITY-10 [NEW]: `ElementRenderer` draw methods inconsistent: `drawFlow` takes center coords, all others take top-left
**File:** `ElementRenderer.java`

### QUALITY-11 [NEW]: `ForresterApp` Javadoc says "Phase 3"
**File:** `ForresterApp.java:20`

---

## Test Coverage Gaps

| Gap | Description |
|-----|-------------|
| TEST-GAP-1 [Carried] | No tests for `ModelCanvas` event handling |
| TEST-GAP-2 [Carried] | No tests for `InlineEditor` |
| TEST-GAP-4 [Carried] | No tests for `formatValue` |
| TEST-GAP-5 [Carried] | No tests for `clipToBorder` |
| TEST-GAP-6 [Carried] | No test for `addFlow` with nonexistent stock names |
| TEST-GAP-7 [NEW] | No tests for `ConnectionRenderer` or `SelectionRenderer` |
| TEST-GAP-8 [NEW] | No tests for `CanvasToolBar` |
| TEST-GAP-9 [NEW] | No test for `reconnectFlow` creating a self-loop |

---

## UX Nits

| Finding | Description |
|---------|-------------|
| UX-2 [Carried] | Escape in SELECT mode with nothing pending does nothing visible |
| UX-3 [Carried] | No cursor shape changes for different modes |
| UX-7 [NEW] | Right-click pans instead of showing a context menu |
| UX-8 [NEW] | No keyboard shortcuts for tool modes (S, F, A, C) |
| UX-9 [NEW] | No status bar or mode indicator outside toolbar |

---

## Summary

| Severity | New | Carried | Total |
|----------|-----|---------|-------|
| Critical | 0 | 0 | 0 |
| Major | 3 | 0 | 3 |
| Minor | 7 | 8 | 15 |
| Nit/Quality | 8 | 6 | 14 |
| Test Gaps | 3 | 5 | 8 |
| **Total** | **21** | **19** | **40** |

### Priority Recommendations

1. **BUG-8 + BUG-9**: Consolidate cloud offset and position logic into one place
2. **BUG-10**: Prevent self-loop via reattachment
3. **QUALITY-6**: Clean up misleading identical if/else
4. **BUG-7 + BUG-11**: Fix zoom-scaling for inline editor

---

## Phase 6 Resolution Status

Phase 6 addressed all 3 major bugs, 3 minor bugs, and 3 code quality findings from above, plus added file save/load.

| Finding | Resolution |
|---|---|
| **BUG-8**: Duplicate CLOUD_OFFSET | **Fixed** — consolidated into `LayoutMetrics.CLOUD_OFFSET`, removed copies from `ConnectionRenderer` and `FlowEndpointCalculator` |
| **BUG-9**: Divergent cloud position logic | **Fixed** — `CanvasRenderer.drawMaterialFlows()` now calls `FlowEndpointCalculator.cloudPosition()` for disconnected endpoints and passes concrete coords + boolean flags to `ConnectionRenderer` (NaN sentinels eliminated) |
| **BUG-10**: Reattachment self-loop | **Fixed** — `ModelEditor.reconnectFlow()` rejects `stockName` equal to the opposite endpoint |
| **BUG-7**: Inline editor width not scaled by zoom | **Fixed** — `fieldWidth` now multiplied by `viewport.getScale()` |
| **BUG-11**: Constant value editor Y offset ignores zoom | **Fixed** — offset now scaled by `viewport.getScale()` |
| **BUG-12**: `reconnectFlow` doesn't validate stockName | **Fixed** — rejects nonexistent stock names with `hasElement()` guard |
| **QUALITY-6**: Identical if/else in `computeCloudPosition` | **Fixed** — removed redundant branches, compute `dx`/`dy` once then negate for sink |
| **QUALITY-10**: `drawFlow` takes center coords | **Fixed** — standardized to `drawFlow(gc, name, x, y, width, height)` taking top-left like all other draw methods |
| **QUALITY-11**: ForresterApp javadoc says "Phase 3" | **Fixed** — updated to describe current capabilities |
| **TEST-GAP-9**: No test for self-loop via reconnect | **Fixed** — added `shouldRejectSelfLoop` and `shouldRejectNonexistentStockName` tests |

### New functionality added in Phase 6

- **File menu**: New (Ctrl+N), Open (Ctrl+O), Save (Ctrl+S), Save As (Ctrl+Shift+S)
- **`CanvasState.toViewDef()`**: converts canvas positions back to `ViewDef` for serialization
- **`ModelEditor.toModelDefinition(ViewDef)`**: includes view layout in serialized model
- **`ModelCanvas.toModelDefinition()`**: convenience method wiring editor + canvas state
- **Empty canvas on startup**: app no longer loads hardcoded SIR model
- **Window title**: shows current filename or "Untitled"

### Test count: 123 passing (up from ~116)
