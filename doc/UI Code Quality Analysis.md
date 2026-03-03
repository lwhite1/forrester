# UI Code Quality Analysis

**Scope:** All 43 Java source files in `forrester-app/` canvas package (8,562 lines total), plus `ForresterApp.java` (409 lines)
**Date:** 2026-03-03 (updated after refactoring and three new features)

---

## Executive Summary

The UI codebase has improved significantly since the previous analysis. All five medium-priority recommendations from the prior review have been addressed: token replacement is deduplicated (ModelEditor exposes a static `replaceToken`), bounding box computation is shared via `ExportBounds`, the mutation protocol is encapsulated in `applyXxx` methods on ModelCanvas, ModelCanvas has been decomposed from 1,587 to 1,345 lines (extracting `CopyPasteController`, `InlineEditController`, `ModuleNavigationController`, and `ConnectionRerouteController`), and new tests cover copy/paste and export bounds (269 tests, up from 242). Three new features — connection rerouting, cut (Ctrl+X), and lookup table editing — follow existing patterns cleanly. ModelEditor grew to 977 lines (reroute + lookup methods) and PropertiesPanel to 736 lines (lookup form), both warranting attention in the next round of refactoring.

**Overall rating: A-** — The codebase is well-structured, consistently written, and improving iteration over iteration. The remaining issues are ModelEditor size, PropertiesPanel rebuild cost, and test gaps for the newest features.

---

## 1. Architecture & Decomposition

### Strengths

- **Clear layering.** The codebase separates concerns into well-defined roles: `CanvasState` (positions/selection), `ModelEditor` (model mutations), `CanvasRenderer` (draw orchestration), `ElementRenderer` (element drawing), `ConnectionRenderer` (line/arrow drawing), `HitTester` (click detection), `FlowEndpointCalculator` (endpoint geometry), `SelectionRenderer` / `FeedbackLoopRenderer` (overlay drawing), `Viewport` (coordinate transforms), `InlineEditor` (text overlays), `Clipboard` (copy/paste data), `SvgExporter` (vector export), `DiagramExporter` (raster/SVG export dispatch), and `ExportBounds` (shared bounding box computation).

- **Immutable domain records.** The engine-side records (`StockDef`, `FlowDef`, `AuxDef`, `ConstantDef`, `ModuleInstanceDef`, `LookupTableDef`) are immutable, and `ModelEditor` correctly replaces whole records when mutating. `LookupTableDef` uses defensive `clone()` in both its compact constructor and accessors, preventing shared mutable array state.

- **Focused utility classes.** `LayoutMetrics`, `ColorPalette`, `HitTester`, `FlowGeometry`, `ResizeHandle`, `Clipboard`, `ExportBounds`, and `SvgExporter` are compact, stateless or self-contained, and easy to test.

- **Extracted interaction controllers.** `DragController`, `MarqueeController`, `ResizeController`, `ReattachController`, `ConnectionRerouteController`, `CopyPasteController`, `InlineEditController`, and `ModuleNavigationController` each own their state and expose clean lifecycle methods. ModelCanvas now delegates to 8 specialized controllers rather than inlining all interaction logic.

- **Shared geometry through FlowGeometry.** The `FlowGeometry` utility centralizes coordinate clipping with a type-safe `Point2D` record. All callers (CanvasRenderer, FlowEndpointCalculator, HitTester, DiagramExporter, SvgExporter, ConnectionRerouteController) use the same geometry code.

- **Shared bounding box via ExportBounds.** Both `DiagramExporter` and `SvgExporter` delegate to `ExportBounds.compute()`, eliminating the previous duplication. The `Bounds` record provides a clean data transfer type.

- **Centralized token replacement.** `ModelEditor.replaceToken()` is now `static` and package-private, used by both `ModelEditor` itself and `CopyPasteController.remapEquationTokens()`. The previous duplication between ModelEditor and ModelCanvas is gone.

- **Centralized CSS constants.** The `Styles` class provides named constants for all inline CSS strings, making theme changes straightforward and preventing style drift.

- **SVG export mirrors canvas rendering.** `SvgExporter` follows the same layer order as `CanvasRenderer` (background → material flows → info links → loop edges → elements → loop highlights), uses the same shared utilities, and includes lookup table rendering with "tbl" badge.

- **Clipboard uses centroid-relative positioning.** The `Clipboard` class stores element positions relative to the selection centroid, ensuring correct paste positioning regardless of original location. All element types including lookups are supported.

- **Connection reroute follows the controller pattern.** `ConnectionRerouteController` uses the same prepare → drag → complete/cancel lifecycle as `ReattachController`, with a `RerouteHit` record capturing the hit test result and `RerouteEnd` enum distinguishing source vs target rerouting.

- **InlineEditController cleanly encapsulates edit chaining.** The name → value/equation edit sequence for constants, flows, and auxiliaries is captured in a single class with a `Callbacks` interface for communicating results back to ModelCanvas. This removed ~80 lines of chaining logic from ModelCanvas.

- **ModuleNavigationController wraps NavigationStack.** Module drill-in/out, context menu, and bindings dialog are captured in a single controller (128 lines), with delegate methods for navigation state queries. This removed ~100 lines from ModelCanvas.

### Weaknesses

- **ModelEditor is the second-largest class (977 lines).** It has grown with the addition of reroute methods (`rerouteConnectionSource`, `rerouteConnectionTarget`, `addConnectionReference` — ~80 lines) and lookup table methods (`addLookup`, `addLookupFrom`, `getLookupTableByName`, `setLookupTable` — ~50 lines). The `addXxx`/`addXxxFrom` pairs (5 pairs, 10 methods) remain duplicative, and the per-type `getXxxByName` methods (6 methods) all follow the same linear scan pattern. A unified element registry or generic lookup map would reduce boilerplate.

- **PropertiesPanel is the third-largest class (736 lines).** The addition of `buildLookupForm()` (~120 lines for inline table editing, add/remove rows, and interpolation mode) accounts for most of the growth. The panel still rebuilds its entire UI on every selection change rather than caching forms by element type.

- **ForresterApp mixes concerns.** It remains both the JavaFX Application lifecycle manager and the controller wiring menus to actions. File I/O, simulation launching, and menu construction are all in one class (409 lines). Not critical yet, but trending toward needing decomposition.

---

## 2. Correctness & Robustness

### Strengths

- **Undo is consistent.** Every mutation path calls `saveUndoState()` before mutating — the new `applyLookupTable`, connection reroute, cut, and paste operations all follow the protocol. The `applyXxx` pattern on ModelCanvas encapsulates undo → mutate → regenerate in single methods, reducing the risk of protocol violations.

- **Connection rerouting correctly validates targets.** `ConnectionRerouteController.complete()` rejects rerouting to the same element (prevents self-loops) and defers undo saving to a callback, ensuring undo state is only saved when a reroute actually succeeds.

- **Connection deletion correctly targets only the relevant equation.** `ModelEditor.removeConnectionReference()` replaces the source token with "0" in only the target element's equation, not globally.

- **Copy/paste correctly handles all element types.** The `CopyPasteController` two-pass algorithm creates elements (building a name mapping), then reconnects flows and remaps equations. The LOOKUP case is handled in both Clipboard capture and paste.

- **Lookup table validation is thorough.** `LookupTableDef`'s compact constructor validates: same-length arrays, minimum 2 points, finite values, strictly increasing x values, and LINEAR or SPLINE interpolation. `commitLookupDataPoint` in PropertiesPanel respects all these constraints.

- **Lookup table data is safely cloned.** `LookupTableDef.xValues()` and `yValues()` return `clone()` defensive copies. PropertiesPanel's `commitLookupDataPoint` works on these clones, mutates them, then constructs a new `LookupTableDef` — correct and safe.

- **Equation token remapping during paste is correct.** `CopyPasteController.remapEquationTokens` delegates to `ModelEditor.replaceToken()`, which uses word-boundary-aware replacement, preventing partial token matches.

- **SVG export handles XML escaping.** `SvgExporter.escapeXml()` properly escapes `&`, `<`, `>`, `"`, and `'` in element names and equations, preventing malformed SVG output.

- **PropertiesPanel correctly handles renames.** A mutable `currentElementName` field is updated after renames, and the `updatingFields` guard prevents spurious focus-loss commits during programmatic updates.

- **Null safety throughout.** Hit-test methods return null for misses, and callers consistently null-check. ConnectionRerouteController's `hitTestEndpoint` checks element existence before computing clipped endpoints.

### Risks

- **Undo snapshot cost.** Each `saveUndoState()` serializes the entire model + view to immutable records. For large models, this creates GC pressure on every drag frame. Currently acceptable; a command-based undo system would be more efficient for very large models.

- **PropertiesPanel rebuilds entire UI on every selection change.** `updateSelection` clears all children and rebuilds from scratch. The lookup form with per-row editing fields is the most expensive case. Caching the current form and updating field values in-place when the element type hasn't changed would be smoother.

- **Clipboard.Entry uses Object for elementDef.** The `elementDef` field is typed as `Object` and cast at paste time. A sealed interface or typed union would provide compile-time safety.

---

## 3. Code Duplication

### Resolved since previous analysis

- **Token replacement** — `ModelEditor.replaceToken()` is now static and package-private; `CopyPasteController` calls it directly. No duplication.
- **Bounding box computation** — Shared via `ExportBounds.compute()`. No duplication.
- **ModelCanvas decomposition** — Copy/paste, inline editing, module navigation, and connection rerouting are in separate controllers. Token replacement and equation remapping live in their respective homes.

### Remaining duplication

- **Element-by-name lookup pattern (ModelEditor).** `getStockByName`, `getFlowByName`, `getAuxByName`, `getConstantByName`, `getModuleByName`, `getLookupTableByName` all follow the same linear scan pattern. A single generic `findByName` or a unified `Map<String, Object>` index would be cleaner, though the current approach is perfectly functional at expected model sizes.

- **addXxx / addXxxFrom pattern (ModelEditor).** Six element types now have paired creation methods (12 methods total). Each pair shares name-generation and registration logic. Unifying each pair (e.g., optional template parameter) would reduce boilerplate but the current approach is clear and explicit.

- **CanvasRenderer effective-width/height boilerplate.** Every element branch in the render loop calls `LayoutMetrics.effectiveWidth(canvasState, name)` and `effectiveHeight(canvasState, name)` then computes `cx - w/2, cy - h/2`. Extracting a bounds record computed once per element would clean this up.

- **PropertiesPanel commit pattern.** Each commit handler (stock, flow, aux, constant, lookup) follows the same pattern: get current def → compare with new value → if changed, call canvas.applyXxx. A generic commit helper could reduce repetition, though each type has slightly different fields.

---

## 4. Test Coverage

### What's tested (14 test classes, 269 tests)

| Test class | Tests | What it covers |
|---|---|---|
| ModelEditorTest | 103 | Add/remove/rename elements, equation references, flow reconnection, setters, updateInList, removeConnectionReference, addXxxFrom (all types), reroute, lookup CRUD |
| CanvasStateTest | 37 | Position, selection, draw order, rename, load/save ViewDef |
| NavigationStackTest | 15 | Push/pop/peek, breadcrumb path, depth |
| UndoManagerTest | 14 | Push/undo/redo, max depth, clear |
| HitTesterTest | 14 | Rectangular hit testing, draw order priority, connection hit testing |
| ViewportTest | 13 | Coordinate transforms, zoom, pan, reset |
| FlowCreationControllerTest | 12 | Two-click flow creation, self-loop rejection, cloud-to-cloud rejection |
| ConnectionHitTestTest | 12 | Point-to-segment distance, info link hit testing |
| ElementRendererTest | 11 | formatValue, isDisplayableEquation |
| FlowEndpointCalculatorTest | 10 | Cloud positions, hit-test clouds and connected endpoints |
| CopyPasteControllerTest | 10 | Copy/paste lifecycle, equation remapping, flow reconnection, name mapping |
| SimulationRunnerTest | 9 | Basic compile-and-run |
| CanvasRendererTest | 6 | clipToBorder geometry (via FlowGeometry) |
| ExportBoundsTest | 3 | Single element bounds, cloud inclusion, padding |

### What's not tested

- **ConnectionRerouteController.** The `hitTestEndpoint`, `prepare`, `drag`, `complete`, and `cancel` methods are pure Java (no JavaFX dependency beyond `CanvasState` which is testable). This is the highest-priority untested new code — test scenarios include: hit near from-end, hit near to-end, miss tolerance, complete reroute to valid target, reject reroute to self, cancel without drag.

- **Lookup table operations in ModelEditor.** While `addLookup` and `getLookupTableByName` are exercised transitively through CopyPasteControllerTest, `setLookupTable` and the lookup rename/remove paths lack direct tests.

- **SvgExporter helper methods.** `svgColor()`, `svgOpacity()`, and `escapeXml()` are easily testable static methods. The lookup SVG rendering (`writeLookup`) is new and untested.

- **All JavaFX UI components.** `PropertiesPanel`, `ForresterApp`, `CanvasToolBar`, `StatusBar`, `BreadcrumbBar`, `InlineEditor`, `BindingConfigDialog`, `SimulationSettingsDialog`, `SimulationResultsDialog`. These require a running JavaFX toolkit (TestFX or similar) and are currently untested.

- **ModelCanvas interaction logic.** The event handler / coordination layer has no tests. Mouse press → drag → release sequences, marquee selection, keyboard shortcuts, context menu behavior — all untested. This is the highest-risk untested code, though the extraction of logic into testable controllers reduces the risk surface.

- **Extracted controllers without tests.** `InlineEditController` and `ModuleNavigationController` depend on JavaFX components (InlineEditor, ContextMenu, BindingConfigDialog) and are not easily unit-testable without TestFX. `DragController`, `MarqueeController`, `ResizeController`, and `ReattachController` depend on CanvasState for state queries and could be tested with mocked state but are not yet tested.

### Recommendation

The non-UI logic has excellent coverage — 269 tests, all passing. The biggest gap is `ConnectionRerouteController`, which is pure Java and straightforward to test. Adding ~8 tests for reroute hit testing, completion, and cancellation would bring it up to the quality bar set by `FlowCreationControllerTest` and `CopyPasteControllerTest`.

---

## 5. API Design & Encapsulation

### Strengths

- **ModelEditor returns unmodifiable lists.** `getStocks()`, `getFlows()`, etc. wrap with `Collections.unmodifiableList()`, preventing accidental mutation by callers.

- **CanvasState returns unmodifiable views.** `getSelection()` and `getDrawOrder()` return unmodifiable wrappers.

- **Record types for data transfer.** `FlowCreationController.State`, `FlowCreationController.FlowResult`, `CanvasRenderer.ReattachState`, `CanvasRenderer.MarqueeState`, `CanvasRenderer.RerouteState`, `NavigationStack.Frame`, `UndoManager.Snapshot`, `FlowEndpointCalculator.CloudHit`, `ResizeHandle.HandleHit`, `FlowGeometry.Point2D`, `ConnectionId`, `ConnectionRerouteController.RerouteHit`, `Clipboard.Entry`, `ExportBounds.Bounds` — all immutable records that cleanly pass state between components.

- **Generic helper in ModelEditor.** The `updateInList` method eliminates duplicated scan-and-replace patterns with a single parameterized method, and `renameInList` delegates to it.

- **Static replaceToken is reusable.** `ModelEditor.replaceToken()` is package-private and static, callable from `CopyPasteController` and any future code that needs word-boundary-aware token replacement.

- **Callback interface for InlineEditController.** The `Callbacks` interface (`applyRename`, `saveAndSetConstantValue`, `saveAndSetFlowEquation`, `saveAndSetAuxEquation`, `postEdit`) provides a clean contract between the controller and ModelCanvas, without exposing the full canvas API.

- **Clipboard and CopyPasteController are package-private.** Only `ModelCanvas` accesses clipboard internals. The controller's `paste()` method returns a list of pasted names, allowing ModelCanvas to handle selection and status updates.

- **SvgExporter has a clean public API.** A single static `export()` method with clear parameters. Color/opacity helpers are package-private for testability.

### Weaknesses

- **ModelCanvas still exposes a wide surface.** Public methods include: selection queries, element deletion, element rename, drill-into, binding config, undo state, regeneration, diagram export accessors, loop analysis, status callbacks, and the new `applyXxx` mutation methods. An interface (e.g., `CanvasActions`) that PropertiesPanel depends on would narrow the coupling.

- **Callback wiring is ad-hoc.** ForresterApp sets callbacks via `setOnStatusChanged`, `setOnNavigationChanged`, `setOnToolChanged`, `setOnLoopToggleChanged`. There's no consistent pattern — some use `Runnable`, some use `Consumer<T>`, some use `IntConsumer`.

- **Clipboard.Entry uses Object for elementDef.** The `elementDef` field is typed as `Object` and cast to `StockDef`, `FlowDef`, `LookupTableDef`, etc. at paste time. A sealed interface would provide compile-time safety.

---

## 6. Performance Considerations

- **Full redraw on every mouse move during hover.** `handleMouseMoved` calls `redraw()` whenever the hovered element changes. Each `redraw()` clears and repaints the entire canvas. For models with 100+ elements, this may lag.

- **Connector generation on every structural change.** `editor.generateConnectors()` rebuilds the full dependency graph on every element add, remove, rename, equation edit, flow reconnect, connection deletion, reroute, and paste. For large models, caching with invalidation would be more efficient.

- **UndoManager stores full snapshots.** With MAX_UNDO=100 and each snapshot containing a complete `ModelDefinition` + `ViewDef`, memory usage scales as O(undoDepth × modelSize). For very large models, this could consume significant memory.

- **PropertiesPanel lookup form rebuild.** The lookup table form creates per-row text fields, buttons, and listeners. For lookup tables with many points, rebuilding on every selection or edit is expensive. Incremental updates (modify existing rows rather than recreating) would help.

---

## 7. Style & Consistency

### Strengths

- **Consistent naming.** Classes follow clear patterns: `*Renderer` for drawing, `*Controller` for state machines and interaction logic, `*Dialog` for dialogs, `*Bar` for toolbars/status bars, `*Exporter` for export utilities.

- **Javadoc on all public classes and methods.** The new classes (`ConnectionRerouteController`, `CopyPasteController`, `InlineEditController`, `ModuleNavigationController`, `ExportBounds`) all have class-level and method-level Javadoc.

- **Modern Java features used well.** Records, switch expressions, pattern matching (`instanceof TextField tf`), sealed types, and `List.copyOf()` are used consistently. The new controllers follow the same conventions.

- **No wildcard imports.** All imports are explicit across all 43 files.

- **No System.out.println.** All output goes through proper UI channels.

- **CSS centralized in Styles.** All inline CSS strings have been extracted to named constants.

- **SVG output is locale-safe.** `SvgExporter` uses `Locale.US` for all `printf` calls, preventing comma-decimal-separator issues.

- **Controller naming is consistent.** All interaction controllers follow the `prepare/start → drag → complete/cancel` lifecycle. `ConnectionRerouteController`, `ReattachController`, `FlowCreationController`, `DragController`, `MarqueeController`, and `ResizeController` all share this pattern.

### Minor Issues

- **Magic numbers in rendering.** Constants like `4` (padding in cloud endpoint), `6` (glow padding), `12` (flow label area in ExportBounds), `30` (paste offset), `20` (reroute endpoint tolerance) appear as literals. Most are documented or self-evident in context, but extracting them to named constants in `LayoutMetrics` would improve readability. The `ENDPOINT_TOLERANCE = 20.0` in `ConnectionRerouteController` is correctly named.

---

## 8. File-by-File Summary

| File | Lines | Rating | Key observation |
|---|---|---|---|
| ModelCanvas.java | 1,345 | B+ | Down from 1,587; delegates to 8 controllers; still the largest class but focused on event dispatch and coordination |
| ModelEditor.java | 977 | B+ | Up from 828; reroute and lookup methods are well-targeted; addXxx/addXxxFrom duplication and linear scans are the main code smells |
| PropertiesPanel.java | 736 | B+ | Up from 598; lookup form is functional and thorough; still rebuilds UI on every selection change |
| SvgExporter.java | 627 | A- | Clean SVG generation; lookup rendering added; uses shared ExportBounds |
| CanvasRenderer.java | 505 | A- | Up from 443; RerouteState overlay and LOOKUP rendering added; clean orchestration |
| ForresterApp.java | 409 | B | Works well; mixing lifecycle + controller wiring |
| CanvasState.java | 273 | A | Clean state management with good encapsulation |
| ElementRenderer.java | 268 | A | Up from 226; drawLookup added with dot-dash border and "tbl" badge |
| SimulationResultsDialog.java | 205 | B+ | Functional; checkbox series toggle is well done |
| ConnectionRerouteController.java | 179 | A | New; clean state machine with RerouteHit record; needs tests |
| LayoutMetrics.java | 174 | A | Up from 163; LOOKUP dimensions added consistently |
| FlowEndpointCalculator.java | 172 | A- | Uses FlowGeometry.Point2D throughout |
| FlowCreationController.java | 170 | A | Clean state machine with good validation |
| CopyPasteController.java | 170 | A | New; clean two-pass paste algorithm; uses ModelEditor.replaceToken; well-tested |
| InlineEditController.java | 160 | A- | New; clean separation with Callbacks interface; depends on JavaFX InlineEditor |
| DiagramExporter.java | 148 | A | Clean export dispatch; delegates bounding box to ExportBounds |
| SelectionRenderer.java | 141 | A | Compact, correct, well-separated |
| BindingConfigDialog.java | 137 | A- | Clean dialog; uses Styles constants |
| HitTester.java | 129 | A | Minimal, correct; uses FlowGeometry.clipToElement |
| ModuleNavigationController.java | 128 | A- | New; wraps NavigationStack with context menu and bindings dialog |
| ConnectionRenderer.java | 126 | A | Focused rendering utilities |
| CanvasToolBar.java | 125 | A- | Clean; PLACE_LOOKUP added; toggle group prevents deselection |
| SimulationRunner.java | 116 | A- | Clean bridge between definition and simulation engine |
| ResizeController.java | 116 | A | Clean extracted controller with proper undo integration |
| Viewport.java | 115 | A | Simple, correct, well-tested |
| StatusBar.java | 115 | A | Good use of label binding; PLACE_LOOKUP case added |
| NavigationStack.java | 114 | A | Clean stack with good record usage |
| MarqueeController.java | 106 | A | Clean extracted controller with Shift+marquee support |
| SimulationSettingsDialog.java | 103 | A- | Good validation binding on OK button |
| InlineEditor.java | 98 | B+ | Functional; commit-on-focus-loss can interfere with chaining |
| Clipboard.java | 97 | A- | Clean centroid-relative capture; LOOKUP case added; Object-typed elementDef |
| UndoManager.java | 91 | A | Simple, correct, well-tested |
| BreadcrumbBar.java | 84 | A | Clean, auto-hides at root; uses Styles constants |
| ExportBounds.java | 80 | A | New; shared bounding box computation; well-tested |
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

1. **Add ConnectionRerouteController tests.** The controller is pure Java and testable without JavaFX. Test scenarios: hitTestEndpoint near from/to ends, outside tolerance, prepare → drag → complete to valid target, reject reroute to self/same element, cancel without drag. This is the biggest test gap for new functionality.

2. **Reduce ModelEditor size.** At 977 lines, it is the second-largest class. Two opportunities: (a) Extract a generic `ElementRegistry` that replaces the six `getXxxByName` linear scans with a `Map<String, ElementRecord>` index; (b) Unify `addXxx`/`addXxxFrom` pairs into single methods with optional template parameters. Either change would remove ~100-150 lines of boilerplate.

3. **Cache PropertiesPanel forms.** Instead of rebuilding the entire UI on every selection change, cache the current form type and update field values in-place when the element type hasn't changed. The lookup form with per-row editing is the most expensive rebuild.

4. **Add lookup table tests.** `setLookupTable`, lookup rename/remove paths, and `commitLookupDataPoint` validation logic (strictly increasing x, minimum 2 points) are untested. The validation constraints are critical for model correctness.

### Low Priority

5. **Type-safe clipboard entries.** Replace `Object elementDef` in `Clipboard.Entry` with a sealed interface or use separate typed lists per element type.

6. **Unify callback patterns.** Replace ad-hoc `Runnable`/`Consumer`/`IntConsumer` callbacks with a consistent listener interface or lightweight event bus.

7. **Narrow ModelCanvas public API.** Extract an interface (e.g., `CanvasActions`) that PropertiesPanel depends on, rather than the full `ModelCanvas` class. This would decouple the panel from canvas internals.

8. **Consider incremental undo.** Replace full-snapshot undo with command-based undo for better memory efficiency as models grow larger.

9. **Extract remaining magic numbers.** Move inline numeric literals (paste offset `30`, flow label area `12`, glow padding `6`) into `LayoutMetrics` constants.

---

## 10. Progress Since Previous Analysis

All five medium-priority recommendations from the previous analysis have been addressed:

| # | Previous Recommendation | Status | How Resolved |
|---|---|---|---|
| 1 | Extract shared token replacement | Done | `ModelEditor.replaceToken()` is static package-private; `CopyPasteController` calls it |
| 2 | Extract bounding box computation | Done | `ExportBounds.compute()` shared by DiagramExporter and SvgExporter; 3 tests |
| 3 | Encapsulate mutation protocol | Done | `applyXxx()` methods on ModelCanvas handle undo → mutate → regenerate |
| 4 | Further decompose ModelCanvas | Done | Extracted CopyPasteController (170 lines), InlineEditController (160 lines), ModuleNavigationController (128 lines), ConnectionRerouteController (179 lines); ModelCanvas down from 1,587 to 1,345 lines |
| 5 | Add tests for new features | Done | CopyPasteControllerTest (10 tests), ExportBoundsTest (3 tests), ModelEditorTest expanded to 103 tests; total 269 tests |

Three new features were added cleanly following existing patterns:
- **Cut (Ctrl+X)** — Copy + delete, integrated with undo
- **Connection rerouting** — New controller with RerouteEnd enum, RerouteHit record, CanvasRenderer.RerouteState for rubber-band overlay
- **Lookup table editor** — Full lifecycle across 11 files: ModelEditor CRUD, toolbar/keys, rendering, properties panel with inline table editing, copy/paste, SVG export, status bar
