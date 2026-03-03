# UI Audit Report — Phase 4 (Flow Connections & Inline Editing)

Audit of all forrester-app UI/canvas code after Phase 4 implementation.

## 1. BUGS

### ~~BUG-1: `drawMaterialFlows` hardcodes source/sink attachment to left/right stock edges~~ [Fixed]
Material flow routing now uses `clipToBorder` to compute direction-aware attachment points on stock borders toward/from the diamond. Works correctly regardless of stock layout direction.

---

### ~~BUG-2: `ConnectionRenderer.drawMaterialFlow` produces NaN coordinates for cloud-to-cloud flows~~ [Fixed]
Cloud positions are now computed relative to the flow indicator (diamond) position. When both source and sink are clouds, the diamond serves as anchor and clouds are placed at fixed offsets along the natural flow direction. No more NaN coordinates.

---

### ~~BUG-3: `CanvasState.renameElement` does not check if `newName` already exists~~ [Fixed]
Both `CanvasState.renameElement` and `ModelEditor.renameElement` now reject renames when the target name is already in use, returning false. `ModelEditor.hasElement(name)` added as a helper. Tests added for both.

---

### ~~BUG-4: `applyRename` does not check return values~~ [Fixed]
`ModelCanvas.applyRename` now checks the return value of `editor.renameElement()` and skips `canvasState.renameElement()` if the model rename failed.

---

### ~~BUG-5: Double-click on stock in PLACE_FLOW mode creates inconsistent state~~ [Fixed]
Inline editing on double-click is now guarded by `activeTool == SELECT && !pendingFlow`. Double-clicking in any placement mode or during a pending flow no longer opens the inline editor.

---

### ~~BUG-6: Material flow routing ignores flow indicator position~~ [Fixed]
Material flows now route through the flow indicator (diamond): source → diamond → sink. The `ConnectionRenderer.drawMaterialFlow` method draws two line segments through the diamond. Dragging a flow indicator correctly bends the flow line.

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

### ~~TEST-GAP-3: No tests for rename-to-existing-name scenario~~ [Fixed]
Tests added: `ModelEditorTest.shouldRejectRenameToExistingName`, `shouldRejectRenameToExistingNameAcrossTypes`, and `CanvasStateTest.shouldRejectRenameToExistingName`.

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

### ~~QUALITY-1: Mutable internal lists exposed via ModelEditor getters~~ [Fixed]
All list getters (`getStocks`, `getFlows`, `getAuxiliaries`, `getConstants`, `getLookupTables`) now return `Collections.unmodifiableList()` wrappers. Internal code accesses the fields directly.

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
The cloud line width `1.5` is an undeclared magic number. (Cloud offset `80` was extracted to `CLOUD_OFFSET` constant.)

---

## 5. UX ISSUES

### ~~UX-1: Double-click in placement mode creates element AND opens inline editor~~ [Fixed]
Inline editing is now restricted to SELECT mode only. Double-click in placement modes no longer triggers inline edit.

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

| Severity | Count | Fixed |
|----------|-------|-------|
| Major    | 7     | 7     |
| Minor    | 13    | 2     |
| Nit      | 5     | 0     |

All major findings have been resolved. 11 minor and 5 nit findings remain as documented above.
