# UI Audit Report — Phase 4 (Flow Connections & Inline Editing)

Audit of all forrester-app UI/canvas code after Phase 4 implementation.

## 1. BUGS

### BUG-1: `drawMaterialFlows` hardcodes source/sink attachment to left/right stock edges [Major]
**File:** `ModelCanvas.java`, lines ~226-244

Material flow drawing always attaches to the right edge of the source stock (`cx + STOCK_WIDTH/2`) and left edge of the sink stock (`cx - STOCK_WIDTH/2`). This assumes flows always go left-to-right. If a user places a sink stock to the LEFT of a source stock, the arrow draws backwards. The cloud fallback offsets (`-80` / `+80`) also assume left-to-right layout.

**Suggested fix:** Use center-to-center direction to dynamically determine attachment points based on actual relative positions.

---

### BUG-2: `ConnectionRenderer.drawMaterialFlow` produces NaN coordinates for cloud-to-cloud flows [Major]
**File:** `ConnectionRenderer.java`, lines 27-29

When both source and sink are `NaN` (both null/cloud), the fallback:
```java
double startX = hasSource ? sourceX : sinkX - 80;  // NaN - 80 = NaN
double endX = hasSink ? sinkX : sourceX + 80;       // NaN + 80 = NaN
```
All coordinates become NaN, producing invisible/undefined drawing. Cloud-to-cloud is the default `addFlow()` state.

**Suggested fix:** Use the flow indicator's canvas position as anchor, or skip drawing for unconnected flows.

---

### BUG-3: `CanvasState.renameElement` does not check if `newName` already exists [Major]
**File:** `CanvasState.java`, lines ~151-172

Renaming "A" to "B" when "B" already exists silently overwrites B's data and creates duplicate entries in `drawOrder`. Same issue exists in `ModelEditor.renameElement`.

**Suggested fix:** Both methods should check for name collisions and return false if the target name is already in use.

---

### BUG-4: `applyRename` does not check return values [Minor]
**File:** `ModelCanvas.java`, `applyRename` method

If `editor.renameElement()` fails (returns false), `canvasState.renameElement()` is still called, leaving model and canvas out of sync.

**Suggested fix:** Only proceed with canvas rename if model rename succeeds.

---

### BUG-5: Double-click on stock in PLACE_FLOW mode creates inconsistent state [Major]
**File:** `ModelCanvas.java`, `handleMousePressed`

Event sequence on double-click:
1. First press (`clickCount == 1`) enters `handleFlowClick` — starts pending flow with stock as source
2. Second press (`clickCount == 2`) hits the double-click check BEFORE the PLACE_FLOW check — opens inline editor on the stock

This leaves a dangling pending flow and opens the inline editor simultaneously.

**Suggested fix:** Add a `pendingFlow` guard before the double-click check, or check `activeTool == SELECT` before allowing inline edit.

---

### BUG-6: Material flow routing ignores flow indicator position [Major]
**File:** `ModelCanvas.java`, `drawMaterialFlows`

Material flows draw from source stock directly to sink stock, completely bypassing the flow indicator (diamond) position. If the user drags the flow indicator elsewhere, the diamond appears disconnected from its flow line.

**Suggested fix:** Route the material flow line through the flow indicator's current position (source -> diamond -> sink).

---

### BUG-7: Inline editor TextField width not scaled by zoom [Minor]
**File:** `ModelCanvas.java`, `startInlineEdit`

The field width uses `LayoutMetrics.widthFor(type) + 20` which is in world-space units. At high zoom the element appears large but the TextField stays small; at low zoom the TextField may be larger than the element.

**Suggested fix:** Scale field width by `viewport.getScale()`.

---

## 2. MISSING EDGE CASES

### EDGE-1: Creating a flow from a stock to the same stock is allowed [Minor]
**File:** `ModelCanvas.java`, `handleFlowClick`

Clicking the same stock twice creates a self-loop flow where `source == sink`. The midpoint calculation places the diamond directly on top of the stock.

**Suggested fix:** Disallow same-stock source/sink, or offset the flow indicator.

---

### EDGE-2: Clicking a non-stock element during flow creation treats it as empty space [Minor]
**File:** `ModelCanvas.java`, `handleFlowClick` / `hitTestStockOnly`

Clicking an aux, constant, or flow indicator during PLACE_FLOW creates a cloud at that position. This is technically correct per the doc but may confuse users who expect the click to be ignored or to show feedback.

---

### EDGE-3: No validation that element names are valid model identifiers [Minor]
**File:** `ModelCanvas.java`, inline edit commit

Names with special characters (`+`, `*`, `(`) are accepted, which could corrupt equation parsing since the underscore token convention would produce invalid tokens.

**Suggested fix:** Validate names contain only alphanumeric characters and spaces.

---

### EDGE-4: Renaming to blank/whitespace gives no user feedback [Minor]
**File:** `ModelCanvas.java`, inline edit commit

The blank check silently closes the editor with no change. User gets no indication why the rename didn't apply.

---

### EDGE-5: Equation references to deleted elements are not cleaned up [Minor]
**File:** `ModelEditor.java`, `removeElement`

Flow source/sink references are nullified on stock deletion, but equation strings referencing any deleted element remain unchanged. Documented in UI Behaviors.md as a known limitation.

---

## 3. TEST COVERAGE GAPS

### TEST-GAP-1: No tests for `ModelCanvas` event handling [Major]
No test file exists for ModelCanvas. All event handler logic (mouse, keyboard, scroll), flow creation protocol, inline editing, and redraw logic are untested.

**Suggested fix:** Extract state machine logic into a testable non-JavaFX controller class.

---

### TEST-GAP-2: No tests for `InlineEditor` [Minor]
Commit/cancel behavior, focus loss handling, and chained constant editing are untested. Depends on JavaFX runtime.

---

### TEST-GAP-3: No tests for rename-to-existing-name scenario [Major]
**Files:** `ModelEditorTest.java`, `CanvasStateTest.java`

No test verifies what happens when renaming to a name already in use. Related to BUG-3.

---

### TEST-GAP-4: No tests for `ElementRenderer.formatValue` [Minor]
This is a pure function now with package-private visibility but has no direct tests.

---

### TEST-GAP-5: No tests for `clipToBorder` [Minor]
Private static pure function with geometric logic but no test coverage.

---

### TEST-GAP-6: No test for `addFlow` with nonexistent stock names [Minor]
`ModelEditor.addFlow("Ghost", "AlsoGhost")` creates a flow referencing nonexistent stocks without validation.

---

## 4. CODE QUALITY

### QUALITY-1: Mutable internal lists exposed via ModelEditor getters [Major]
**File:** `ModelEditor.java`, lines ~325-343

`getStocks()`, `getFlows()`, etc. return direct references to internal `ArrayList` instances. External code can modify them, bypassing editor mutation methods.

**Suggested fix:** Return `Collections.unmodifiableList()` from each getter.

---

### QUALITY-2: String literals for element types instead of enum [Minor]
**Files:** Throughout `CanvasState`, `HitTester`, `ModelCanvas`, `LayoutMetrics`, `SelectionRenderer`

Element types ("stock", "flow", "aux", "constant") are raw strings. A typo would silently fail.

**Suggested fix:** Define an `ElementType` enum.

---

### QUALITY-3: `ModelCanvas` has too many responsibilities [Minor]
**File:** `ModelCanvas.java` (~740 lines)

Handles rendering, event handling, flow creation state machine, inline editing orchestration, and coordinate clipping.

**Suggested fix:** Extract interaction controller, dedicated renderer, and flow-creation state machine.

---

### QUALITY-4: `CanvasState` uses `double[]` for positions [Nit]
A `record Point(double x, double y)` would be cleaner and align with the project's immutability preference.

---

### QUALITY-5: Magic numbers in `ConnectionRenderer` [Nit]
The cloud offset `80` and cloud line width `1.5` are undeclared magic numbers.

---

## 5. UX ISSUES

### UX-1: Double-click in placement mode creates element AND opens inline editor [Minor]
**File:** `ModelCanvas.java`, `handleMousePressed`

First click creates the element (placement), second click (double-click) hits the just-created element and opens inline edit. Could be a useful shortcut but is undocumented and may surprise users.

---

### UX-2: Escape in SELECT mode with no pending flow does nothing visible [Nit]
Resets toolbar to SELECT (already active). Could clear selection instead.

---

### UX-3: No cursor shape changes for different modes [Nit]
Documented in "Not Yet Implemented" section.

---

### UX-4: Inline editor position assumes canvas is at (0,0) in overlay pane [Minor]
Uses `viewport.toScreenX/Y` directly as overlay pane coordinates. Works currently but would break if canvas had a non-zero offset within the pane.

**Suggested fix:** Use `canvas.localToParent()` for coordinate conversion.

---

## Summary

| Severity | Count |
|----------|-------|
| Major    | 7     |
| Minor    | 13    |
| Nit      | 5     |

### Top Priority Fixes
1. **BUG-5**: Double-click in flow mode creates dangling state — guard inline edit behind `!pendingFlow` and `activeTool == SELECT`
2. **BUG-3**: Rename-to-existing-name corruption — add collision checks in both `ModelEditor` and `CanvasState`
3. **BUG-2**: Cloud-to-cloud flow rendering NaN — skip drawing or anchor to flow indicator
4. **BUG-6**: Flow routing bypasses diamond — route through flow indicator position
5. **QUALITY-1**: Exposed mutable lists — return unmodifiable views
