# UI Code Quality Analysis

**Scope:** All 38 Java source files in `forrester-app/` canvas package (8,137 lines total), plus `ForresterApp.java` (409 lines)
**Date:** 2026-03-03

---

## Executive Summary

The UI codebase is well-structured and consistently written. It follows a clear decomposition pattern where ModelCanvas orchestrates interactions while delegating rendering, hit-testing, state management, and interaction state machines to focused helper classes. The code uses modern Java (records, switch expressions, pattern matching) and maintains good separation between mutable editing state (ModelEditor, CanvasState) and immutable domain records. Recent additions — connection deletion, copy/paste, and SVG export — follow existing patterns cleanly. The main areas for improvement are: ModelCanvas has grown to 1,587 lines (copy/paste and connection deletion added ~220 lines), the token replacement logic is duplicated between ModelEditor and ModelCanvas, bounding box computation is duplicated across DiagramExporter and SvgExporter, and test coverage does not yet cover the new features.

**Overall rating: A-** — Clean, well-decomposed code with consistent patterns. The remaining issues are architectural scaling concerns and minor duplication rather than bugs.

---

## 1. Architecture & Decomposition

### Strengths

- **Clear layering.** The codebase separates concerns into well-defined roles: `CanvasState` (positions/selection), `ModelEditor` (model mutations), `CanvasRenderer` (draw orchestration), `ElementRenderer` (element drawing), `ConnectionRenderer` (line/arrow drawing), `HitTester` (click detection), `FlowEndpointCalculator` (endpoint geometry), `SelectionRenderer` / `FeedbackLoopRenderer` (overlay drawing), `Viewport` (coordinate transforms), `InlineEditor` (text overlays), `Clipboard` (copy/paste data), `SvgExporter` (vector export), and `DiagramExporter` (raster/SVG export dispatch).

- **Immutable domain records.** The engine-side records (`StockDef`, `FlowDef`, `AuxDef`, `ConstantDef`, `ModuleInstanceDef`) are immutable, and `ModelEditor` correctly replaces whole records when mutating. This avoids shared mutable state bugs.

- **Focused utility classes.** `LayoutMetrics`, `ColorPalette`, `HitTester`, `FlowGeometry`, `ResizeHandle`, `Clipboard`, and `SvgExporter` are compact, stateless or self-contained, and easy to test. They do one thing well.

- **Module navigation is well-designed.** `NavigationStack` cleanly captures/restores parent state with full fidelity (editor, view, viewport, undo manager, active tool). The write-back on `navigateBack()` is correctly ordered.

- **Extracted interaction controllers.** `DragController`, `MarqueeController`, `ResizeController`, and `ReattachController` each own their state fields and expose clean start/drag/end/cancel lifecycle methods.

- **Shared geometry through FlowGeometry.** The `FlowGeometry` utility centralizes coordinate clipping with a type-safe `Point2D` record. All callers (CanvasRenderer, FlowEndpointCalculator, HitTester, DiagramExporter, SvgExporter) use the same geometry code.

- **Centralized CSS constants.** The `Styles` class provides named constants for all inline CSS strings, making theme changes straightforward and preventing style drift.

- **SVG export mirrors canvas rendering.** `SvgExporter` follows the same layer order as `CanvasRenderer` (background → material flows → info links → loop edges → elements → loop highlights), uses the same shared utilities (`FlowGeometry`, `FlowEndpointCalculator`, `LayoutMetrics`, `ColorPalette`), and produces output that matches the canvas visually. This makes it easy to verify correctness and maintain both in sync.

- **Clipboard uses centroid-relative positioning.** The `Clipboard` class stores element positions relative to the selection centroid, which means paste positioning works correctly regardless of where the original elements were located. The two-pass paste algorithm (create elements → reconnect flows and remap equations) correctly handles the ordering dependency.

### Weaknesses

- **ModelCanvas is the largest class (1,587 lines).** Despite the controller extractions, it has grown with the addition of copy/paste (~120 lines for `pasteClipboard`, `copySelection`, `remapEquationTokens`, `replaceTokenInEquation`, `isTokenChar`) and connection deletion (~20 lines for `deleteSelectedConnection`). It still handles: mouse/keyboard event dispatch, inline editing orchestration, flow creation coordination, undo/redo, module navigation, context menus, element creation/deletion, copy/paste orchestration, connection deletion, and the public API surface. Further decomposition would help.

- **ForresterApp mixes concerns.** It is both the JavaFX Application lifecycle manager and the controller wiring menus to actions. File I/O, simulation launching, and menu construction are all in one class (409 lines). Not critical yet, but trending toward needing decomposition.

---

## 2. Correctness & Robustness

### Strengths

- **Undo is consistent.** Every mutation path calls `saveUndoState()` before mutating — including the new `deleteSelectedConnection()` and `pasteClipboard()`. The snapshot-based approach (full model + view serialization) is simple and correct — no partial-state bugs possible.

- **Connection deletion correctly targets only the relevant equation.** `ModelEditor.removeConnectionReference()` replaces the source token with "0" in only the target element's equation, not globally. This preserves other references to the same element from different targets.

- **Copy/paste correctly handles flow reconnection.** The two-pass algorithm in `pasteClipboard()` first creates all elements (building a name mapping), then reconnects flows only when both source and sink were in the copied set. External references (flows connected to non-copied stocks) correctly become clouds.

- **Equation token remapping during paste is correct.** `remapEquationTokens` iterates all name mappings and applies word-boundary-aware replacement, preventing partial token matches (e.g., "Stock_1" won't match inside "Stock_10").

- **SVG export handles XML escaping.** `SvgExporter.escapeXml()` properly escapes `&`, `<`, `>`, `"`, and `'` in element names and equations, preventing malformed SVG output.

- **PropertiesPanel correctly handles renames.** A mutable `currentElementName` field is updated after renames, so all subsequent commit handlers reference the correct name. The `updatingFields` guard prevents spurious focus-loss commits during programmatic updates.

- **Null safety throughout.** Hit-test methods return null for misses, and callers consistently null-check. Record lookups return null rather than throwing. Canvas state methods handle missing elements gracefully (return NaN, false, or no-op).

- **Flow validation is thorough.** `FlowCreationController` rejects self-loops and cloud-to-cloud flows. `reconnectFlow` validates the stock exists and prevents self-loops. Flow endpoint hit-testing correctly prevents detaching when one end is already a cloud.

### Risks

- **Undo snapshot cost.** Each `saveUndoState()` serializes the entire model + view to immutable records. For large models with hundreds of elements, this creates GC pressure on every drag frame, resize frame, or property edit. Currently acceptable, but will not scale. A command-based undo system would be more efficient.

- **PropertiesPanel rebuilds entire UI on every selection change.** `updateSelection` clears all children and rebuilds from scratch. For rapid selection changes (e.g., clicking through elements quickly), this creates unnecessary garbage. Caching the current form and updating field values in-place when the element type hasn't changed would be smoother.

- **Clipboard stores references to immutable records.** The `Clipboard.Entry.elementDef` field stores a reference to the original `StockDef`, `FlowDef`, etc. Since these are immutable records, this is safe — but the `Object` type loses type safety. A sealed interface or typed union would be cleaner.

---

## 3. Code Duplication

### Token replacement logic (ModelEditor ↔ ModelCanvas)

`ModelCanvas.replaceTokenInEquation()` and `ModelCanvas.isTokenChar()` are exact duplicates of `ModelEditor.replaceToken()` and `ModelEditor.isTokenChar()`. The logic should live in one place — either exposed as package-private on `ModelEditor` or extracted to a shared utility. Currently, a fix to the token replacement algorithm would need to be applied in two places.

### Bounding box computation (DiagramExporter ↔ SvgExporter)

Both `DiagramExporter.exportDiagram()` and `SvgExporter.export()` contain identical bounding box computation code (~30 lines each): iterating draw order for element bounds, iterating flows for cloud positions, and adding padding. This should be extracted to a shared utility method (e.g., `BoundingBox computeBoundingBox(CanvasState, ModelEditor)`).

### Element-by-name lookup pattern (ModelEditor)

`getStockByName`, `getFlowByName`, `getAuxByName`, `getConstantByName`, `getModuleByName` all follow the same linear scan pattern. A single generic `findByName` or a unified `Map<String, Object>` index would be cleaner, though the current approach is perfectly functional at expected model sizes.

### addXxx / addXxxFrom pattern (ModelEditor)

The `addStock()` / `addStockFrom()`, `addFlow()` / `addFlowFrom()`, etc. pairs share the same name-generation and registration logic. Each pair could be unified (e.g., `addStock` taking an optional template parameter), reducing the 10 creation methods to 5. Minor duplication — the current approach is clear and explicit.

### CanvasRenderer effective-width/height boilerplate

Every element branch in the render loop calls `LayoutMetrics.effectiveWidth(canvasState, name)` and `effectiveHeight(canvasState, name)` then computes `cx - w/2, cy - h/2`. This could be extracted into a `BoundingBox` record computed once per element.

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

- **New features**: `removeConnectionReference()`, `addStockFrom()/addFlowFrom()/addAuxFrom()/addConstantFrom()/addModuleFrom()` in ModelEditor, `Clipboard` capture/isEmpty/getEntries, `SvgExporter.export()`, `SvgExporter.svgColor()/svgOpacity()`, and the `pasteClipboard()/copySelection()/deleteSelectedConnection()` methods in ModelCanvas all lack unit tests. The ModelEditor additions are straightforward to test without JavaFX; the Clipboard class is also pure Java. SvgExporter's color/opacity helpers and XML escaping are easily testable.

- **All JavaFX UI components**: `PropertiesPanel`, `ForresterApp`, `CanvasToolBar`, `StatusBar`, `BreadcrumbBar`, `InlineEditor`, `BindingConfigDialog`, `SimulationSettingsDialog`, `SimulationResultsDialog`. These require a running JavaFX toolkit (TestFX or similar) and are currently untested.

- **ModelCanvas interaction logic**: The event handler / coordination layer has no tests. Mouse press → drag → release sequences, marquee selection, keyboard shortcuts, context menu behavior — all untested. This is the highest-risk untested code.

- **Interaction controllers**: `DragController`, `MarqueeController`, `ResizeController`, and `ReattachController` are separate classes with clean interfaces, but still depend on JavaFX CanvasState for state queries, making them testable with mocked state but not yet tested.

### Recommendation

The non-UI logic (ModelEditor, CanvasState, FlowCreationController, etc.) has excellent test coverage — 242 tests, all passing. Adding tests for the new ModelEditor methods (`removeConnectionReference`, `addXxxFrom`) and for `Clipboard` would be straightforward and would cover the new feature logic. `SvgExporter`'s helper methods (`svgColor`, `svgOpacity`, `escapeXml`) are also easily unit-testable. The extracted interaction controllers remain the highest-priority untested area.

---

## 5. API Design & Encapsulation

### Strengths

- **ModelEditor returns unmodifiable lists.** `getStocks()`, `getFlows()`, etc. wrap with `Collections.unmodifiableList()`, preventing accidental mutation by callers.

- **CanvasState returns unmodifiable views.** `getSelection()` and `getDrawOrder()` return unmodifiable wrappers.

- **Record types for data transfer.** `FlowCreationController.State`, `FlowCreationController.FlowResult`, `CanvasRenderer.ReattachState`, `CanvasRenderer.MarqueeState`, `NavigationStack.Frame`, `UndoManager.Snapshot`, `FlowEndpointCalculator.CloudHit`, `ResizeHandle.HandleHit`, `FlowGeometry.Point2D`, `ConnectionId`, `Clipboard.Entry` — all immutable records that cleanly pass state between components without coupling.

- **Generic helper in ModelEditor.** The `updateInList` method eliminates 9 duplicated scan-and-replace patterns with a single parameterized method, and `renameInList` delegates to it.

- **Clipboard is package-private.** The `Clipboard` class has package-private visibility, preventing external callers from depending on clipboard internals. Only `ModelCanvas` accesses it.

- **SvgExporter has a clean public API.** A single static `export()` method with clear parameters. Color/opacity helpers are package-private for testability without polluting the public API.

### Weaknesses

- **ModelCanvas exposes a wide surface.** It has public methods for: selection queries, element deletion, element rename, drill-into, binding config, undo state, regeneration, diagram export accessors, loop analysis, and status callbacks. Consider an interface (e.g., `CanvasActions`) that PropertiesPanel depends on, rather than the full ModelCanvas.

- **Callback wiring is ad-hoc.** ForresterApp sets callbacks via `setOnStatusChanged`, `setOnNavigationChanged`, `setOnToolChanged`, `setOnLoopToggleChanged`. There's no consistent pattern — some use `Runnable`, some use `Consumer<T>`, some use `IntConsumer`. A small event bus or listener interface would unify this.

- **PropertiesPanel directly calls both `canvas` and `editor`.** The panel reaches through canvas to call `pushUndoState()` and `regenerateAndRedraw()`, then separately calls editor setters. This means the panel needs to know the correct mutation protocol (save undo → mutate → regenerate). If the protocol changes, the panel breaks. Better: ModelCanvas should expose high-level mutation methods that handle undo/regeneration internally.

- **Clipboard.Entry uses Object for elementDef.** The `elementDef` field is typed as `Object` and cast to `StockDef`, `FlowDef`, etc. at paste time. A sealed interface or generic type parameter would provide compile-time safety.

---

## 6. Performance Considerations

- **Full redraw on every mouse move during hover.** `handleMouseMoved` calls `redraw()` whenever the hovered element changes. Each `redraw()` clears and repaints the entire canvas. For models with 100+ elements, this may lag. A dirty-region approach (only repaint the old and new hover areas) would help, but is complex with Canvas API.

- **Connector generation on every structural change.** `editor.generateConnectors()` rebuilds the full dependency graph. This is called on every element add, remove, rename, equation edit, flow reconnect, connection deletion, and paste. For large models, caching with invalidation would be more efficient.

- **UndoManager stores full snapshots.** With MAX_UNDO=100 and each snapshot containing a complete `ModelDefinition` + `ViewDef`, memory usage scales as O(undoDepth × modelSize). For very large models, this could consume significant memory. Incremental undo (storing diffs or commands) would be more memory-efficient.

- **SVG export writes the full document in memory.** `SvgExporter` uses `PrintWriter` which buffers the entire output. For extremely large diagrams this is fine (SVG is text and typically small), but the bounding box computation duplicates DiagramExporter's work unnecessarily.

---

## 7. Style & Consistency

### Strengths

- **Consistent naming.** Classes follow clear patterns: `*Renderer` for drawing, `*Controller` for state machines, `*Dialog` for dialogs, `*Bar` for toolbars/status bars, `*Exporter` for export utilities.

- **Javadoc on all public methods.** Every public method has a meaningful Javadoc comment. Private methods with non-obvious behavior are also documented. The new methods (`removeConnectionReference`, `addStockFrom`, `pasteClipboard`, `copySelection`, `SvgExporter.export`) all have Javadoc.

- **Modern Java features used well.** Records, switch expressions, pattern matching (`instanceof TextField tf`), and `List.copyOf()` are used consistently. The new code follows the same conventions (switch expressions in Clipboard.capture, Locale.US in SvgExporter printf calls).

- **No wildcard imports.** All imports are explicit across all 38 files.

- **No System.out.println.** All output goes through proper UI channels.

- **CSS centralized in Styles.** All inline CSS strings have been extracted to named constants, making theme changes straightforward.

- **SVG output is locale-safe.** `SvgExporter` uses `Locale.US` for all `printf` calls, preventing comma-decimal-separator issues in locales like de_DE or fr_FR.

### Minor Issues

- **Magic numbers in rendering.** Constants like `4` (padding in cloud endpoint), `6` (glow padding), `12` (flow label area in SvgExporter bounding box), `30` (paste offset) appear as literals. Most are documented or self-evident in context, but extracting them to named constants in `LayoutMetrics` would improve readability.

---

## 8. File-by-File Summary

| File | Lines | Rating | Key observation |
|---|---|---|---|
| ModelCanvas.java | 1,587 | B | Largest class; grew ~220 lines with copy/paste and connection deletion; duplicates token replacement logic from ModelEditor |
| ModelEditor.java | 828 | A- | Clean with generic updateInList helper; addXxx/addXxxFrom pairs have minor duplication; new removeConnectionReference is well-targeted |
| SvgExporter.java | 628 | A- | New; clean SVG generation matching canvas layer order; bounding box duplicated from DiagramExporter; locale-safe printf |
| PropertiesPanel.java | 598 | B+ | Stale-name and double-commit bugs fixed; still rebuilds UI on every selection change |
| CanvasRenderer.java | 443 | A- | Clean orchestration using FlowGeometry for all coordinate clipping |
| ForresterApp.java | 409 | B | Works well; mixing lifecycle + controller wiring |
| CanvasState.java | 273 | A | Clean state management with good encapsulation |
| ElementRenderer.java | 226 | A | Clean, focused, well-structured |
| SimulationResultsDialog.java | 205 | B+ | Functional; checkbox series toggle is well done |
| DiagramExporter.java | 195 | A- | Clean export dispatch; SVG delegation is straightforward; bounding box should be shared with SvgExporter |
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
| Viewport.java | 115 | A | Simple, correct, well-tested |
| StatusBar.java | 114 | A | Good use of label binding; uses Styles constants |
| NavigationStack.java | 114 | A | Clean stack with good record usage |
| MarqueeController.java | 106 | A | Clean extracted controller with Shift+marquee support |
| SimulationSettingsDialog.java | 103 | A- | Good validation binding on OK button |
| InlineEditor.java | 98 | B+ | Functional; commit-on-focus-loss can interfere with chaining |
| Clipboard.java | 95 | A- | New; clean centroid-relative capture; Object-typed elementDef loses type safety |
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

1. **Extract shared token replacement.** `ModelCanvas.replaceTokenInEquation()` / `isTokenChar()` duplicate `ModelEditor.replaceToken()` / `isTokenChar()`. Make the ModelEditor methods package-private (or extract to a shared `TokenReplacer` utility) and have ModelCanvas call them. This eliminates the duplication and ensures any future fixes apply to both code paths.

2. **Extract bounding box computation.** `DiagramExporter` and `SvgExporter` both compute the same world-space bounding box with cloud positions and padding. Extract to a shared static method (e.g., `ExportBounds.compute(CanvasState, ModelEditor)`) returning a record `(minX, minY, maxX, maxY)`.

3. **Encapsulate mutation protocol in ModelCanvas.** PropertiesPanel currently calls `pushUndoState()` → editor setter → `regenerateAndRedraw()` directly. ModelCanvas should expose high-level mutation methods (e.g., `setStockInitialValue(name, value)`) that handle the undo/mutate/regenerate sequence internally.

4. **Further decompose ModelCanvas.** Extract module navigation (drillInto, navigateBack, navigateToDepth, breadcrumb path) into a `NavigationController`. Extract inline editing orchestration (startInlineEdit, startNameEditThenChain, chain methods) into an `InlineEditController`. Extract copy/paste orchestration (copySelection, pasteClipboard, remapEquationTokens) into a `CopyPasteController`. This would bring ModelCanvas closer to ~900 lines.

5. **Add tests for new features.** `removeConnectionReference()`, `addXxxFrom()`, `Clipboard.capture()`, and `SvgExporter` helper methods (`svgColor`, `svgOpacity`, `escapeXml`) are all pure Java and easily testable without JavaFX. These would bring test coverage in line with the existing quality bar.

### Low Priority

6. **Type-safe clipboard entries.** Replace `Object elementDef` in `Clipboard.Entry` with a sealed interface or use separate typed lists per element type. This would catch type mismatches at compile time rather than relying on runtime casts.

7. **Unify callback patterns.** Replace ad-hoc `Runnable`/`Consumer`/`IntConsumer` callbacks with a consistent listener interface or lightweight event bus.

8. **Cache PropertiesPanel forms.** Instead of rebuilding the entire UI on every selection change, cache the current form type and update field values in-place when the element type hasn't changed.

9. **Consider incremental undo.** Replace full-snapshot undo with command-based undo for better memory efficiency as models grow larger. Current snapshot approach is correct and works well at current model sizes.

10. **Extract remaining magic numbers.** Move inline numeric literals (paste offset `30`, flow label area `12`, glow padding `6`) into `LayoutMetrics` constants.
