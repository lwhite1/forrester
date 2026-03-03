# UI Code Quality Analysis

**Scope:** All 34 Java source files in `forrester-app/` canvas package (6,658 lines total), plus `ForresterApp.java` (409 lines)
**Date:** 2026-03-03

---

## Executive Summary

The UI codebase is well-structured and consistently written. It follows a clear decomposition pattern where ModelCanvas orchestrates interactions while delegating rendering, hit-testing, state management, and interaction state machines to focused helper classes. The code uses modern Java (records, switch expressions, pattern matching) and maintains good separation between mutable editing state (ModelEditor, CanvasState) and immutable domain records. The main areas for improvement are: ModelCanvas remains the largest class despite recent extraction work, ForresterApp mixes concerns, test coverage is limited to non-JavaFX classes, and PropertiesPanel reaches through canvas to perform mutations that should be encapsulated.

**Overall rating: A-** — Clean, well-decomposed code with consistent patterns. The remaining issues are architectural scaling concerns rather than bugs.

---

## 1. Architecture & Decomposition

### Strengths

- **Clear layering.** The codebase separates concerns into well-defined roles: `CanvasState` (positions/selection), `ModelEditor` (model mutations), `CanvasRenderer` (draw orchestration), `ElementRenderer` (element drawing), `ConnectionRenderer` (line/arrow drawing), `HitTester` (click detection), `FlowEndpointCalculator` (endpoint geometry), `SelectionRenderer` / `FeedbackLoopRenderer` (overlay drawing), `Viewport` (coordinate transforms), and `InlineEditor` (text overlays).

- **Immutable domain records.** The engine-side records (`StockDef`, `FlowDef`, `AuxDef`, `ConstantDef`, `ModuleInstanceDef`) are immutable, and `ModelEditor` correctly replaces whole records when mutating. This avoids shared mutable state bugs.

- **Focused utility classes.** `LayoutMetrics`, `ColorPalette`, `HitTester`, `FlowGeometry`, and `ResizeHandle` are compact, stateless, and easy to test. They do one thing well.

- **Module navigation is well-designed.** `NavigationStack` cleanly captures/restores parent state with full fidelity (editor, view, viewport, undo manager, active tool). The write-back on `navigateBack()` is correctly ordered.

- **Extracted interaction controllers.** `DragController`, `MarqueeController`, `ResizeController`, and `ReattachController` each own their state fields and expose clean start/drag/end/cancel lifecycle methods. This reduced ModelCanvas from ~1,520 to 1,370 lines and removed scattered state fields.

- **Shared geometry through FlowGeometry.** The `FlowGeometry` utility centralizes coordinate clipping with a type-safe `Point2D` record. All callers (CanvasRenderer, FlowEndpointCalculator, HitTester, DiagramExporter) use the same geometry code, eliminating the previous duplication and `double[]` returns.

- **Centralized CSS constants.** The `Styles` class provides named constants for all inline CSS strings, making theme changes straightforward and preventing style drift.

### Weaknesses

- **ModelCanvas is still the largest class (1,370 lines).** Despite the controller extractions, it still handles: mouse/keyboard event dispatch, inline editing orchestration, flow creation coordination, undo/redo, module navigation, context menus, element creation/deletion, and the public API surface for ForresterApp and PropertiesPanel. Further decomposition (e.g., extracting module navigation or inline editing orchestration) would continue to reduce its size.

- **ForresterApp mixes concerns.** It is both the JavaFX Application lifecycle manager and the controller wiring menus to actions. File I/O, simulation launching, and menu construction are all in one class (409 lines). Not critical yet, but trending toward needing decomposition.

---

## 2. Correctness & Robustness

### Strengths

- **Undo is consistent.** Every mutation path calls `saveUndoState()` before mutating. The snapshot-based approach (full model + view serialization) is simple and correct — no partial-state bugs possible.

- **PropertiesPanel correctly handles renames.** A mutable `currentElementName` field is updated after renames, so all subsequent commit handlers reference the correct name. The `updatingFields` guard prevents spurious focus-loss commits during programmatic updates, and each commit handler checks if the value actually changed before pushing undo state.

- **Null safety throughout.** Hit-test methods return null for misses, and callers consistently null-check. Record lookups (e.g., `findConstant`, `findFlow`) return null rather than throwing. Canvas state methods handle missing elements gracefully (return NaN, false, or no-op).

- **Flow validation is thorough.** `FlowCreationController` rejects self-loops and cloud-to-cloud flows. `reconnectFlow` validates the stock exists and prevents self-loops. Flow endpoint hit-testing correctly prevents detaching when one end is already a cloud.

- **Equation reference maintenance.** `ModelEditor.renameElement()` and `removeElement()` both update equation tokens with word-boundary-aware replacement. This avoids the subtle bug of partial token replacement.

### Risks

- **Undo snapshot cost.** Each `saveUndoState()` serializes the entire model + view to immutable records. For large models with hundreds of elements, this creates GC pressure on every drag frame, resize frame, or property edit. Currently acceptable, but will not scale. A command-based undo system would be more efficient.

- **PropertiesPanel rebuilds entire UI on every selection change.** `updateSelection` clears all children and rebuilds from scratch. For rapid selection changes (e.g., clicking through elements quickly), this creates unnecessary garbage. Caching the current form and updating field values in-place when the element type hasn't changed would be smoother.

---

## 3. Code Duplication

### Element-by-name lookup pattern (ModelEditor)

`getStockByName`, `getFlowByName`, `getAuxByName`, `getConstantByName`, `getModuleByName` all follow the same linear scan pattern. A single generic `findByName` or a unified `Map<String, Object>` index would be cleaner, though the current approach is perfectly functional at expected model sizes.

### CanvasRenderer effective-width/height boilerplate

Every element branch in the render loop calls `LayoutMetrics.effectiveWidth(canvasState, name)` and `effectiveHeight(canvasState, name)` then computes `cx - w/2, cy - h/2`. This could be extracted into a `BoundingBox` record computed once per element.

### PropertiesPanel form-building boilerplate

Each `buildXxxForm` method repeats the pattern: create TextField, add commit handlers, add to grid. The `addCommitHandlers` helper reduces some repetition, but the form-building methods themselves are still verbose (~30-40 lines each). A declarative form-builder pattern could further reduce this.

---

## 4. Test Coverage

### What's tested (12 test classes, 242 tests)

| Test class | Tests | What it covers |
|---|---|---|
| ModelEditorTest | 89 | Add/remove/rename elements, equation references, flow reconnection, setters, updateInList |
| CanvasStateTest | 37 | Position, selection, draw order, rename, load/save ViewDef |
| NavigationStackTest | 15 | Push/pop/peek, breadcrumb path, depth |
| UndoManagerTest | 14 | Push/undo/redo, max depth, clear |
| HitTesterTest | 14 | Rectangular hit testing, draw order priority, connection hit testing |
| ViewportTest | 13 | Coordinate transforms, zoom, pan, reset |
| FlowCreationControllerTest | 12 | Two-click flow creation, self-loop rejection, cloud-to-cloud rejection |
| ConnectionHitTestTest | 12 | Point-to-segment distance, info link hit testing |
| ElementRendererTest | 11 | formatValue, isDisplayableEquation |
| FlowEndpointCalculatorTest | 10 | Cloud positions, hit-test clouds and connected endpoints |
| SimulationRunnerTest | 9 | Basic compile-and-run |
| CanvasRendererTest | 6 | clipToBorder geometry (via FlowGeometry) |

### What's not tested

- **All JavaFX UI components**: `PropertiesPanel`, `ForresterApp`, `CanvasToolBar`, `StatusBar`, `BreadcrumbBar`, `InlineEditor`, `BindingConfigDialog`, `SimulationSettingsDialog`, `SimulationResultsDialog`. These require a running JavaFX toolkit (TestFX or similar) and are currently untested.

- **ModelCanvas interaction logic**: The event handler / coordination layer has no tests. Mouse press → drag → release sequences, marquee selection, keyboard shortcuts, context menu behavior — all untested. This is the highest-risk untested code.

- **Interaction controllers**: `DragController`, `MarqueeController`, `ResizeController`, and `ReattachController` are now separate classes with clean interfaces, but they still depend on JavaFX CanvasState for state queries, making them testable with mocked state but not yet tested.

- **Integration between InlineEditor and ModelCanvas**: The chained edit sequence (name → equation) and the editor lifecycle (open → commit/cancel → close) have no test coverage.

### Recommendation

The non-UI logic (ModelEditor, CanvasState, FlowCreationController, etc.) has excellent test coverage — 242 tests, all passing. The untested area is exclusively JavaFX-dependent code. The extracted interaction controllers (DragController, etc.) are now clean enough to test with mocked CanvasState objects — adding tests for these would significantly improve coverage of the interaction layer without requiring TestFX.

---

## 5. API Design & Encapsulation

### Strengths

- **ModelEditor returns unmodifiable lists.** `getStocks()`, `getFlows()`, etc. wrap with `Collections.unmodifiableList()`, preventing accidental mutation by callers.

- **CanvasState returns unmodifiable views.** `getSelection()` and `getDrawOrder()` return unmodifiable wrappers.

- **Record types for data transfer.** `FlowCreationController.State`, `FlowCreationController.FlowResult`, `CanvasRenderer.ReattachState`, `CanvasRenderer.MarqueeState`, `NavigationStack.Frame`, `UndoManager.Snapshot`, `FlowEndpointCalculator.CloudHit`, `ResizeHandle.HandleHit`, `FlowGeometry.Point2D`, `ConnectionId` — all immutable records that cleanly pass state between components without coupling.

- **Generic helper in ModelEditor.** The `updateInList` method eliminates 9 duplicated scan-and-replace patterns with a single parameterized method, and `renameInList` delegates to it.

### Weaknesses

- **ModelCanvas exposes a wide surface.** It has public methods for: selection queries, element deletion, element rename, drill-into, binding config, undo state, regeneration, diagram export accessors, loop analysis, and status callbacks. Consider an interface (e.g., `CanvasActions`) that PropertiesPanel depends on, rather than the full ModelCanvas.

- **Callback wiring is ad-hoc.** ForresterApp sets callbacks via `setOnStatusChanged`, `setOnNavigationChanged`, `setOnToolChanged`, `setOnLoopToggleChanged`. There's no consistent pattern — some use `Runnable`, some use `Consumer<T>`, some use `IntConsumer`. A small event bus or listener interface would unify this.

- **PropertiesPanel directly calls both `canvas` and `editor`.** The panel reaches through canvas to call `pushUndoState()` and `regenerateAndRedraw()`, then separately calls editor setters. This means the panel needs to know the correct mutation protocol (save undo → mutate → regenerate). If the protocol changes, the panel breaks. Better: ModelCanvas should expose high-level mutation methods (e.g., `setStockProperty(name, property, value)`) that handle undo/regeneration internally.

---

## 6. Performance Considerations

- **Full redraw on every mouse move during hover.** `handleMouseMoved` calls `redraw()` whenever the hovered element changes. Each `redraw()` clears and repaints the entire canvas. For models with 100+ elements, this may lag. A dirty-region approach (only repaint the old and new hover areas) would help, but is complex with Canvas API.

- **Connector generation on every structural change.** `editor.generateConnectors()` rebuilds the full dependency graph. This is called on every element add, remove, rename, equation edit, and flow reconnect. For large models, caching with invalidation would be more efficient.

- **UndoManager stores full snapshots.** With MAX_UNDO=100 and each snapshot containing a complete `ModelDefinition` + `ViewDef`, memory usage scales as O(undoDepth × modelSize). For very large models, this could consume significant memory. Incremental undo (storing diffs or commands) would be more memory-efficient.

---

## 7. Style & Consistency

### Strengths

- **Consistent naming.** Classes follow clear patterns: `*Renderer` for drawing, `*Controller` for state machines, `*Dialog` for dialogs, `*Bar` for toolbars/status bars.

- **Javadoc on all public methods.** Every public method has a meaningful Javadoc comment. Private methods with non-obvious behavior are also documented.

- **Modern Java features used well.** Records, switch expressions, pattern matching (`instanceof TextField tf`), and `List.copyOf()` are used consistently.

- **No wildcard imports.** All imports are explicit.

- **No System.out.println.** All output goes through proper UI channels.

- **CSS centralized in Styles.** All inline CSS strings have been extracted to named constants, making theme changes straightforward.

### Minor Issues

- **Magic numbers in rendering.** Constants like `4` (padding in cloud endpoint), `6` (glow padding), `20` (text offset) appear as literals in `ElementRenderer` and `ConnectionRenderer`. Most are documented in `LayoutMetrics`, but a few inline values remain.

---

## 8. File-by-File Summary

| File | Lines | Rating | Key observation |
|---|---|---|---|
| ModelCanvas.java | 1,370 | B | Still the largest class; interaction controllers extracted but inline editing, navigation, and event dispatch remain |
| ModelEditor.java | 728 | A- | Clean with generic updateInList helper; getXxxByName still duplicated |
| PropertiesPanel.java | 598 | B+ | Stale-name and double-commit bugs fixed; still rebuilds UI on every selection change |
| CanvasRenderer.java | 443 | A- | Clean orchestration using FlowGeometry for all coordinate clipping |
| ForresterApp.java | 409 | B | Works well; mixing lifecycle + controller wiring |
| CanvasState.java | 273 | A | Clean state management with good encapsulation |
| ElementRenderer.java | 226 | A | Clean, focused, well-structured |
| SimulationResultsDialog.java | 205 | B+ | Functional; checkbox series toggle is well done |
| DiagramExporter.java | 178 | A | Clean export utility with proper bounding box computation |
| FlowEndpointCalculator.java | 172 | A- | Uses FlowGeometry.Point2D throughout; no more double[] returns |
| FlowCreationController.java | 170 | A | Clean state machine with good validation |
| LayoutMetrics.java | 163 | A | Well-organized constants and sizing logic |
| SelectionRenderer.java | 141 | A | Compact, correct, well-separated |
| BindingConfigDialog.java | 137 | A- | Clean dialog; uses Styles constants |
| HitTester.java | 129 | A | Minimal, correct; uses FlowGeometry.clipToElement |
| ConnectionRenderer.java | 126 | A | Focused rendering utilities |
| CanvasToolBar.java | 123 | A- | Clean; toggle group prevents deselection correctly |
| ResizeController.java | 116 | A | Clean extracted controller with proper undo integration |
| SimulationRunner.java | 116 | A- | Clean bridge between definition and simulation engine |
| StatusBar.java | 114 | A | Good use of label binding; uses Styles constants |
| Viewport.java | 115 | A | Simple, correct, well-tested |
| NavigationStack.java | 114 | A | Clean stack with good record usage |
| MarqueeController.java | 106 | A | Clean extracted controller with Shift+marquee support |
| SimulationSettingsDialog.java | 103 | A- | Good validation binding on OK button |
| InlineEditor.java | 98 | B+ | Functional; commit-on-focus-loss can interfere with chaining |
| UndoManager.java | 91 | A | Simple, correct, well-tested |
| BreadcrumbBar.java | 84 | A | Clean, auto-hides at root; uses Styles constants |
| DragController.java | 75 | A | Clean extracted controller with lazy undo save |
| ReattachController.java | 74 | A | Clean extracted controller with proper lifecycle |
| FeedbackLoopRenderer.java | 71 | A | Compact, consistent with SelectionRenderer style |
| ResizeHandle.java | 64 | A | Clean enum with built-in hit-testing |
| FlowGeometry.java | 49 | A | Shared Point2D record and clipping utilities |
| Styles.java | 40 | A | Centralized CSS constants |
| ColorPalette.java | 31 | A | Well-organized color constants |
| ConnectionId.java | 15 | A | Simple record for connection identification |
| Launcher.java | 13 | A | Correct JavaFX launcher indirection |

---

## 9. Prioritized Recommendations

### Medium Priority

1. **Encapsulate mutation protocol in ModelCanvas.** PropertiesPanel currently calls `pushUndoState()` → editor setter → `regenerateAndRedraw()` directly. ModelCanvas should expose high-level mutation methods (e.g., `setStockInitialValue(name, value)`) that handle the undo/mutate/regenerate sequence internally. This reduces coupling and prevents protocol drift.

2. **Further decompose ModelCanvas.** Extract module navigation (drillInto, navigateBack, navigateToDepth, breadcrumb path) into a `NavigationController`. Extract inline editing orchestration (startInlineEdit, startNameEditThenChain, chain methods) into an `InlineEditController`. This would bring ModelCanvas closer to ~800 lines.

3. **Add tests for extracted interaction controllers.** DragController, MarqueeController, ResizeController, and ReattachController now have clean interfaces that can be tested with mocked CanvasState objects. This would cover the highest-risk interaction logic without requiring TestFX.

### Low Priority

4. **Unify callback patterns.** Replace ad-hoc `Runnable`/`Consumer`/`IntConsumer` callbacks with a consistent listener interface or lightweight event bus.

5. **Cache PropertiesPanel forms.** Instead of rebuilding the entire UI on every selection change, cache the current form type and update field values in-place when the element type hasn't changed.

6. **Consider incremental undo.** Replace full-snapshot undo with command-based undo for better memory efficiency as models grow larger. Current snapshot approach is correct and works well at current model sizes.

7. **Extract generic findByName in ModelEditor.** Replace the 5 duplicated `getXxxByName` methods with a single parameterized lookup.

8. **Extract remaining magic numbers.** Move inline numeric literals from ElementRenderer and ConnectionRenderer into LayoutMetrics constants.
