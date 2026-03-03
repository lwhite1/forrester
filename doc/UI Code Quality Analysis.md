# UI Code Quality Analysis

**Scope:** All 28 Java source files in `forrester-app/` (6,479 lines total)
**Date:** 2026-03-03

---

## Executive Summary

The UI codebase is well-structured and consistently written. It follows a clear decomposition pattern where ModelCanvas orchestrates interactions while delegating rendering, hit-testing, state management, and editing to focused helper classes. The code uses modern Java (records, sealed patterns, switch expressions) and maintains good separation between mutable editing state (ModelEditor, CanvasState) and immutable domain records. The main areas for improvement are: ModelCanvas is becoming a large God Class, there are several duplicated lookup patterns, test coverage is limited to non-JavaFX classes, and the PropertiesPanel has a stale-reference bug pattern.

**Overall rating: B+** — Solid, maintainable code with a few structural issues that will become painful as the editor grows.

---

## 1. Architecture & Decomposition

### Strengths

- **Clear layering.** The codebase separates concerns into well-defined roles: `CanvasState` (positions/selection), `ModelEditor` (model mutations), `CanvasRenderer` (draw orchestration), `ElementRenderer` (element drawing), `ConnectionRenderer` (line/arrow drawing), `HitTester` (click detection), `FlowEndpointCalculator` (endpoint geometry), `SelectionRenderer` / `FeedbackLoopRenderer` (overlay drawing), `Viewport` (coordinate transforms), and `InlineEditor` (text overlays).

- **Immutable domain records.** The engine-side records (`StockDef`, `FlowDef`, `AuxDef`, `ConstantDef`, `ModuleInstanceDef`) are immutable, and `ModelEditor` correctly replaces whole records when mutating. This avoids shared mutable state bugs.

- **Focused utility classes.** `LayoutMetrics`, `ColorPalette`, `HitTester`, and `ResizeHandle` are compact, stateless, and easy to test. They do one thing well.

- **Module navigation is well-designed.** `NavigationStack` cleanly captures/restores parent state with full fidelity (editor, view, viewport, undo manager, active tool). The write-back on `navigateBack()` is correctly ordered.

### Weaknesses

- **ModelCanvas is a God Class (1,520 lines).** It handles: mouse events (press, drag, release, move), keyboard events, inline editing orchestration, flow creation, reattachment, resize, marquee selection, undo/redo, module navigation, context menus, and public API surface for ForresterApp and PropertiesPanel. This is the single biggest quality concern. Over time, features added here will increasingly conflict.

- **ForresterApp mixes concerns.** It is both the JavaFX Application lifecycle manager and the controller wiring menus to actions. File I/O, simulation launching, and menu construction are all in one class (402 lines). Not critical yet, but trending toward needing decomposition.

---

## 2. Correctness & Robustness

### Strengths

- **Undo is consistent.** Every mutation path calls `saveUndoState()` before mutating. The snapshot-based approach (full model + view serialization) is simple and correct — no partial-state bugs possible.

- **Null safety throughout.** Hit-test methods return null for misses, and callers consistently null-check. Record lookups (e.g., `findConstant`, `findFlow`) return null rather than throwing. Canvas state methods handle missing elements gracefully (return NaN, false, or no-op).

- **Flow validation is thorough.** `FlowCreationController` rejects self-loops and cloud-to-cloud flows. `reconnectFlow` validates the stock exists and prevents self-loops. Flow endpoint hit-testing correctly prevents detaching when one end is already a cloud.

- **Equation reference maintenance.** `ModelEditor.renameElement()` and `removeElement()` both update equation tokens with word-boundary-aware replacement. This avoids the subtle bug of partial token replacement.

### Bugs & Risks

- **PropertiesPanel stale name capture.** The `buildStockForm`, `buildFlowForm`, etc. methods capture the element `name` parameter in closures for commit handlers. If the user renames the element via the name field, the commit handlers for other fields (Initial Value, Unit, Equation) still reference the *old* name. Example: select a stock named "S1", rename it to "S2" in the name field (commits), then edit the initial value — `commitStockInitialValue` calls `editor.setStockInitialValue("S1", ...)` which silently fails because "S1" no longer exists. The panel should either refresh after a rename or capture the name in a mutable holder.

- **PropertiesPanel double-commit on focus loss.** When the user presses Enter in a field, `setOnAction` fires the commit, then focus transfers to the next field (or elsewhere), triggering the `focusedProperty` listener which commits again. The second commit is harmless for idempotent operations (setting the same value) but pushes a redundant undo state. This wastes undo stack slots and confuses undo behavior (user has to undo twice to revert one edit).

- **Undo snapshot cost.** Each `saveUndoState()` serializes the entire model + view to immutable records. For large models with hundreds of elements, this creates GC pressure on every drag frame, resize frame, or property edit. Currently acceptable, but will not scale. A command-based undo system would be more efficient.

- **PropertiesPanel `propertyGrid.getRowCount()` call on line 116 is a no-op.** The return value is discarded. This appears to be a leftover from debugging or a mistaken attempt to reset row count. Harmless but confusing.

- **`updatingFields` flag is declared but never set to true.** The flag exists in PropertiesPanel to guard against focus-loss commits during programmatic updates, but no code ever sets it. This means the guard is ineffective — if `updateSelection()` is called while a field has focus, the focus-loss listener will fire and attempt to commit the stale value.

---

## 3. Code Duplication

### Element lookup pattern (ModelEditor)

The following pattern appears 15+ times across ModelEditor:
```java
for (int i = 0; i < stocks.size(); i++) {
    if (stocks.get(i).name().equals(name)) {
        StockDef s = stocks.get(i);
        stocks.set(i, new StockDef(...));
        return true;
    }
}
return false;
```

Each setter (`setStockInitialValue`, `setStockUnit`, `setStockNegativeValuePolicy`, `setFlowEquation`, `setFlowTimeUnit`, `setAuxEquation`, `setAuxUnit`, `setConstantValue`, `setConstantUnit`) duplicates this structure. A generic `updateInList` helper — similar to the existing `renameInList` — would eliminate this repetition:

```java
private <T> boolean updateInList(List<T> list, String name,
                                  Function<T, String> nameGetter,
                                  UnaryOperator<T> updater) {
    for (int i = 0; i < list.size(); i++) {
        if (nameGetter.apply(list.get(i)).equals(name)) {
            list.set(i, updater.apply(list.get(i)));
            return true;
        }
    }
    return false;
}
```

### Element-by-name lookup pattern (ModelEditor)

`getStockByName`, `getFlowByName`, `getAuxByName`, `getConstantByName`, `getModuleByName` all follow the same linear scan pattern. A single generic `findByName` or a unified `Map<String, Object>` index would be cleaner, though the current approach is perfectly functional at expected model sizes.

### CanvasRenderer effective-width/height boilerplate

Every element branch in the render loop calls `LayoutMetrics.effectiveWidth(canvasState, name)` and `effectiveHeight(canvasState, name)` then computes `cx - w/2, cy - h/2`. This could be extracted into a `BoundingBox` record computed once per element.

### PropertiesPanel commit handler boilerplate

Each `buildXxxForm` method repeats the pattern: create TextField, set onAction to commit, add focusedProperty listener to commit on focus loss. A helper like `createCommittingField(initialValue, commitAction)` would halve the form-building code.

### FlowEndpointCalculator + CanvasRenderer duplication

Both `drawMaterialFlows` in CanvasRenderer and `hitTestConnectedEndpoints` in FlowEndpointCalculator compute flow endpoint positions using the same `clipToBorder` + `effectiveWidth/effectiveHeight` logic. If the endpoint computation is ever adjusted (e.g., for curved flows), both places must change in sync. Extracting a shared `FlowGeometry.computeEndpoints(flow, canvasState)` method would eliminate this.

---

## 4. Test Coverage

### What's tested (11 test classes)

| Test class | Lines | What it covers |
|---|---|---|
| ModelEditorTest | ~600 | Add/remove/rename elements, equation references, flow reconnection, setters |
| CanvasStateTest | ~200 | Position, selection, draw order, rename, load/save ViewDef |
| FlowCreationControllerTest | ~150 | Two-click flow creation, self-loop rejection, cloud-to-cloud rejection |
| FlowEndpointCalculatorTest | ~100 | Cloud positions, hit-test clouds and endpoints |
| CanvasRendererTest | ~80 | clipToBorder geometry |
| HitTesterTest | ~60 | Rectangular hit testing, draw order priority |
| UndoManagerTest | ~80 | Push/undo/redo, max depth, clear |
| ViewportTest | ~60 | Coordinate transforms, zoom, pan, reset |
| NavigationStackTest | ~80 | Push/pop/peek, breadcrumb path, depth |
| ElementRendererTest | ~40 | formatValue, isDisplayableEquation |
| SimulationRunnerTest | ~30 | Basic compile-and-run |

### What's not tested

- **All JavaFX UI components**: `PropertiesPanel`, `ForresterApp`, `CanvasToolBar`, `StatusBar`, `BreadcrumbBar`, `InlineEditor`, `BindingConfigDialog`, `SimulationSettingsDialog`, `SimulationResultsDialog`. These require a running JavaFX toolkit (TestFX or similar) and are currently untested.

- **ModelCanvas interaction logic**: The 1,520-line event handler / coordination layer has no tests. Mouse press → drag → release sequences, marquee selection, keyboard shortcuts, context menu behavior, resize logic — all untested. This is the highest-risk untested code.

- **PropertiesPanel commit logic**: The commit handlers that bridge property edits to ModelEditor mutations have no test coverage. The stale-name and double-commit bugs described in Section 2 would be caught by tests.

- **Integration between InlineEditor and ModelCanvas**: The chained edit sequence (name → equation) and the editor lifecycle (open → commit/cancel → close) have no test coverage.

### Recommendation

The non-UI logic (ModelEditor, CanvasState, FlowCreationController, etc.) has excellent test coverage — 230 tests, all passing. The untested area is exclusively JavaFX-dependent code. Adding TestFX would unlock testing for PropertiesPanel commit logic, toolbar interactions, and keyboard shortcuts. Alternatively, extracting the commit/validation logic from PropertiesPanel into a testable non-UI class would allow testing without a JavaFX runtime.

---

## 5. API Design & Encapsulation

### Strengths

- **ModelEditor returns unmodifiable lists.** `getStocks()`, `getFlows()`, etc. wrap with `Collections.unmodifiableList()`, preventing accidental mutation by callers.

- **CanvasState returns unmodifiable views.** `getSelection()` and `getDrawOrder()` return unmodifiable wrappers.

- **Record types for data transfer.** `FlowCreationController.State`, `FlowCreationController.FlowResult`, `CanvasRenderer.ReattachState`, `CanvasRenderer.MarqueeState`, `NavigationStack.Frame`, `UndoManager.Snapshot`, `FlowEndpointCalculator.CloudHit`, `ResizeHandle.HandleHit` — all immutable records that cleanly pass state between components without coupling.

### Weaknesses

- **ModelCanvas exposes too much.** After the PropertiesPanel addition, ModelCanvas now has public methods for: selection queries, element deletion, element rename, drill-into, binding config, undo state, and regeneration. Combined with the existing public API (setModel, setActiveTool, setOverlayPane, setOnStatusChanged, etc.), the class has become a wide-surface-area facade. Consider an interface (e.g., `CanvasActions`) that PropertiesPanel depends on, rather than the full ModelCanvas.

- **Callback wiring is ad-hoc.** ForresterApp sets callbacks via `setOnStatusChanged`, `setOnNavigationChanged`, `setOnToolChanged`, `setOnLoopToggleChanged`. There's no consistent pattern — some use `Runnable`, some use `Consumer<T>`, some use `IntConsumer`. A small event bus or listener interface would unify this.

- **PropertiesPanel directly calls both `canvas` and `editor`.** The panel reaches through canvas to call `pushUndoState()` and `regenerateAndRedraw()`, then separately calls editor setters. This means the panel needs to know the correct mutation protocol (save undo → mutate → regenerate). If the protocol changes, the panel breaks. Better: ModelCanvas should expose high-level mutation methods (e.g., `setStockProperty(name, property, value)`) that handle undo/regeneration internally.

---

## 6. Performance Considerations

- **Full redraw on every mouse move during hover.** `handleMouseMoved` calls `redraw()` whenever the hovered element changes. Each `redraw()` clears and repaints the entire canvas. For models with 100+ elements, this may lag. A dirty-region approach (only repaint the old and new hover areas) would help, but is complex with Canvas API.

- **Connector generation on every structural change.** `editor.generateConnectors()` rebuilds the full dependency graph. This is called on every element add, remove, rename, equation edit, and flow reconnect. For large models, caching with invalidation would be more efficient.

- **UndoManager stores full snapshots.** With MAX_UNDO=100 and each snapshot containing a complete `ModelDefinition` + `ViewDef`, memory usage scales as O(undoDepth * modelSize). For very large models, this could consume significant memory. Incremental undo (storing diffs or commands) would be more memory-efficient.

- **PropertiesPanel rebuilds entire UI on every selection change.** `updateSelection` clears all children and rebuilds from scratch. For rapid selection changes (e.g., clicking through elements quickly), this creates unnecessary garbage. Caching the current form and updating field values in-place when the element type hasn't changed would be smoother.

---

## 7. Style & Consistency

### Strengths

- **Consistent naming.** Classes follow clear patterns: `*Renderer` for drawing, `*Controller` for state machines, `*Dialog` for dialogs, `*Bar` for toolbars/status bars.

- **Javadoc on all public methods.** Every public method has a meaningful Javadoc comment. Private methods with non-obvious behavior are also documented.

- **Modern Java features used well.** Records, switch expressions, pattern matching (`instanceof TextField tf`), and `List.copyOf()` are used consistently.

- **No wildcard imports.** All imports are explicit.

### Minor Issues

- **Inline CSS strings.** Styles like `"-fx-font-weight: bold; -fx-font-size: 11px;"` appear as string literals throughout `StatusBar`, `BreadcrumbBar`, `PropertiesPanel`, and `BindingConfigDialog`. Extracting these into constants (or a shared stylesheet) would improve consistency and make theme changes easier.

- **Magic numbers in rendering.** Constants like `4` (padding in cloud endpoint), `6` (glow padding), `20` (text offset) appear as literals in `ElementRenderer` and `ConnectionRenderer`. Most are documented in `LayoutMetrics`, but a few inline values remain.

- **Mixed use of `double[]` for coordinates.** `clipToBorder` and `cloudPosition` return `double[]` arrays. A `Point2D` record (or reuse of `CanvasState.Position`) would be more type-safe and self-documenting.

---

## 8. File-by-File Summary

| File | Lines | Rating | Key observation |
|---|---|---|---|
| ModelCanvas.java | 1,520 | C+ | God Class — needs decomposition into interaction controllers |
| ModelEditor.java | 771 | B+ | Solid but repetitive setter pattern; consider generic helper |
| PropertiesPanel.java | 567 | B- | Stale-name bug, double-commit, `updatingFields` never set |
| CanvasRenderer.java | 439 | A- | Clean orchestration; minor duplication with FlowEndpointCalculator |
| ForresterApp.java | 402 | B | Works well; mixing lifecycle + controller wiring |
| ElementRenderer.java | 226 | A | Clean, focused, well-structured |
| SimulationResultsDialog.java | 205 | B+ | Functional; checkbox series toggle is well done |
| CanvasState.java | 273 | A | Clean state management with good encapsulation |
| FlowEndpointCalculator.java | 178 | B+ | Correct but duplicates some CanvasRenderer geometry |
| FlowCreationController.java | 170 | A | Clean state machine with good validation |
| LayoutMetrics.java | 163 | A | Well-organized constants and sizing logic |
| BindingConfigDialog.java | 137 | B+ | Functional dialog; grid construction is clean |
| ConnectionRenderer.java | 126 | A | Focused rendering utilities |
| CanvasToolBar.java | 123 | A- | Clean; toggle group prevents deselection correctly |
| SelectionRenderer.java | 116 | A | Compact, correct, well-separated |
| SimulationRunner.java | 116 | A- | Clean bridge between definition and simulation engine |
| StatusBar.java | 115 | A- | Good use of label binding for loop separator |
| Viewport.java | 115 | A | Simple, correct, well-tested |
| NavigationStack.java | 114 | A | Clean stack with good record usage |
| SimulationSettingsDialog.java | 103 | A- | Good validation binding on OK button |
| InlineEditor.java | 98 | B+ | Functional; commit-on-focus-loss can interfere with chaining |
| UndoManager.java | 91 | A | Simple, correct, well-tested |
| BreadcrumbBar.java | 85 | A | Clean, auto-hides at root, correct depth navigation |
| FeedbackLoopRenderer.java | 71 | A | Compact, consistent with SelectionRenderer style |
| ResizeHandle.java | 64 | A | Clean enum with built-in hit-testing |
| HitTester.java | 60 | A | Minimal, correct reverse-draw-order testing |
| ColorPalette.java | 31 | A | Well-organized color constants |
| Launcher.java | 13 | A | Correct JavaFX launcher indirection |

---

## 9. Prioritized Recommendations

### High Priority

1. **Fix PropertiesPanel stale-name bug.** After a rename commit, refresh the entire panel (or at minimum update the captured name in all closures). This is a functional bug that will cause silent data loss.

2. **Set `updatingFields` flag in PropertiesPanel.** Wrap `updateSelection()` body in `updatingFields = true` / `finally { updatingFields = false; }` to prevent spurious focus-loss commits when the panel refreshes.

3. **Fix double-commit on Enter.** In the commit handlers, check whether the value actually changed before pushing undo state. Or use a committed flag per field to prevent the focus-loss listener from re-committing.

### Medium Priority

4. **Extract interaction controllers from ModelCanvas.** Split into: `DragController` (element drag, pan), `MarqueeController` (rubber-band selection), `ResizeController` (element resize), `ReattachController` (flow endpoint reattachment). Each controller would own its state fields and handle its mouse event phases. ModelCanvas would delegate to the active controller.

5. **Add generic `updateInList` helper to ModelEditor.** Replace the 9 duplicated setter loops with a single parameterized method.

6. **Extract PropertiesPanel commit logic into a testable class.** Something like `PropertyCommitter` that takes a ModelEditor and performs validated mutations, without any JavaFX dependency. This enables unit testing without TestFX.

### Low Priority

7. **Unify coordinate return types.** Replace `double[]` returns in `clipToBorder` and `cloudPosition` with a `Point2D` record.

8. **Extract shared flow endpoint geometry.** Create `FlowGeometry` with methods used by both CanvasRenderer and FlowEndpointCalculator.

9. **Move inline CSS to a stylesheet or constants class.** Create a `Styles` utility or use an actual `.css` resource.

10. **Consider incremental undo.** Replace full-snapshot undo with command-based undo for better memory efficiency as models grow larger.
