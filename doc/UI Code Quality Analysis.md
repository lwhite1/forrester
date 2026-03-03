# UI Code Quality Analysis

**Scope:** All 50 Java source files in `forrester-app/` canvas package (9,268 lines total), plus `ForresterApp.java` (444 lines)
**Date:** 2026-03-03 (updated after addressing all four medium-priority recommendations from previous review)

---

## Executive Summary

All four medium-priority recommendations from the prior review have been addressed: (1) PropertiesPanel decomposed from 878 to 310 lines by extracting per-type `ElementForm` implementations (`StockForm`, `FlowForm`, `AuxForm`, `ConstantForm`, `LookupForm`) and shared `FormContext`; (2) SvgExporter now has 13 tests covering `svgColor`, `svgOpacity`, and `escapeXml`; (3) `ModelCanvas.selectElement` now pans the viewport to center the selected element, so validation dialog clicks scroll to off-screen elements; (4) `validateModel()` now runs on a background thread using `javafx.concurrent.Task`, matching the `runSimulation()` pattern. ForresterApp grew from 426 to 444 lines (background thread wiring). Total test count is 322 (up from 309 UI tests), plus 45 engine def tests — all passing.

**Overall rating: A** — The codebase has reached a high quality level. Every medium-priority recommendation from two successive reviews has been addressed. PropertiesPanel is no longer oversized (310 lines, down from 878). The remaining issues are ForresterApp concern mixing, EquationAutoComplete test gaps, and JavaFX-dependent UI components that require TestFX for testing.

---

## 1. Architecture & Decomposition

### Strengths

- **Clear layering.** The codebase separates concerns into well-defined roles: `CanvasState` (positions/selection), `ModelEditor` (model mutations), `CanvasRenderer` (draw orchestration), `ElementRenderer` (element drawing), `ConnectionRenderer` (line/arrow drawing), `HitTester` (click detection), `FlowEndpointCalculator` (endpoint geometry), `SelectionRenderer` / `FeedbackLoopRenderer` (overlay drawing), `Viewport` (coordinate transforms), `InlineEditor` (text overlays), `Clipboard` (copy/paste data), `SvgExporter` (vector export), `DiagramExporter` (raster/SVG export dispatch), and `ExportBounds` (shared bounding box computation).

- **Immutable domain records.** The engine-side records (`StockDef`, `FlowDef`, `AuxDef`, `ConstantDef`, `ModuleInstanceDef`, `LookupTableDef`) are immutable, and `ModelEditor` correctly replaces whole records when mutating. The new engine-side records (`ValidationIssue`, `ValidationResult`) follow the same conventions — compact constructors with validation, `List.copyOf` for defensive copying.

- **Focused utility classes.** `LayoutMetrics`, `ColorPalette`, `HitTester`, `FlowGeometry`, `ResizeHandle`, `Clipboard`, `ExportBounds`, `SvgExporter`, and the new `EquationAutoComplete` are compact, stateless or self-contained, and easy to test.

- **Extracted interaction controllers.** `DragController`, `MarqueeController`, `ResizeController`, `ReattachController`, `ConnectionRerouteController`, `CopyPasteController`, `InlineEditController`, and `ModuleNavigationController` each own their state and expose clean lifecycle methods. ModelCanvas delegates to 8 specialized controllers rather than inlining all interaction logic.

- **Engine-layer validation is cleanly separated.** `ModelValidator` is a static utility class in the engine `def` package (same pattern as `DefinitionValidator`). It delegates to `DefinitionValidator` for structural errors, then adds its own higher-level checks (disconnected flows, missing units, algebraic loops, unused elements). The UI layer (`ValidationDialog`, toolbar, menu) only depends on `ValidationResult` — no engine internals leak into the app.

- **Model validation uses the existing graph infrastructure.** `ModelValidator.checkAlgebraicLoops` reuses `DependencyGraph.fromDefinition` and `FeedbackAnalysis.analyze` rather than reimplementing cycle detection. The `equationsParseable` guard prevents cascading errors when equations are already invalid.

- **Generic helpers reduce ModelEditor boilerplate.** The new `findByName(List<T>, String, Function<T, String>)` method replaces 6 per-type linear scan methods with a single generic implementation. Similarly, `updateEquationByName` unifies the pattern of finding a flow/aux by name and transforming its equation. ModelEditor dropped from 977 to 924 lines.

- **PropertiesPanel decomposed into per-type form builders.** The `ElementForm` interface defines `build(int startRow)`, `updateValues()`, and `dispose()`. Five implementations (`StockForm`, `FlowForm`, `AuxForm`, `ConstantForm`, `LookupForm`) each own their form construction and commit logic. `FormContext` holds shared state (canvas, editor, property grid) and common UI helpers (name field with rename commit, field rows, read-only rows). PropertiesPanel dropped from 878 to 310 lines — no longer the second-largest class. Form caching by `cachedFormType` avoids full rebuilds on same-type selection changes.

- **Shared geometry through FlowGeometry.** The `FlowGeometry` utility centralizes coordinate clipping with a type-safe `Point2D` record. All callers (CanvasRenderer, FlowEndpointCalculator, HitTester, DiagramExporter, SvgExporter, ConnectionRerouteController) use the same geometry code.

- **Centralized token replacement, bounding box, and CSS constants.** `ModelEditor.replaceToken()` (static, package-private), `ExportBounds.compute()`, and the `Styles` class all serve as single sources of truth for their respective concerns.

- **Connection reroute follows the controller pattern.** `ConnectionRerouteController` uses the same prepare → drag → complete/cancel lifecycle as `ReattachController`, with a `RerouteHit` record and `RerouteEnd` enum. Now fully tested with 11 tests.

### Weaknesses

- **ForresterApp continues to mix concerns (444 lines).** It handles JavaFX lifecycle, menu construction, file I/O, simulation launching, and validation wiring. Both `validateModel()` and `runSimulation()` now use background threads, adding task setup boilerplate. The class is trending toward needing decomposition into a separate `MenuBarController` or `AppController`.

- **EquationAutoComplete is a significant new class (305 lines).** It handles popup lifecycle, text parsing for cursor context, prefix matching, keyboard navigation, and completion insertion. It is well-structured with private helper methods and named constants (`MAX_VISIBLE_ROWS`, `MIN_PREFIX_LENGTH`), but has no tests and depends on JavaFX components (`Popup`, `ListView`, `TextField`).

---

## 2. Correctness & Robustness

### Strengths

- **Undo is consistent.** Every mutation path calls `saveUndoState()` before mutating. The `applyXxx` pattern on ModelCanvas encapsulates undo → mutate → regenerate in single methods.

- **Validation correctly delegates and wraps.** `ModelValidator.validate()` calls `DefinitionValidator.validate()` first, wraps each error string as a `Severity.ERROR` issue with element name extracted via regex, then runs its own WARNING-level checks. This avoids duplicating structural validation logic.

- **Algebraic loop detection is sound.** The check uses `FeedbackAnalysis.analyze()` to find cycle groups, then filters out groups containing stocks (which are normal SD feedback loops). Only stock-free circular dependencies (which can't be resolved by integration) are flagged as warnings.

- **Unused element detection accounts for name normalization.** `ModelValidator.isReferenced()` checks both the literal name and the underscore-to-space variant, matching the same normalization used in `DependencyGraph.fromDefinition()`.

- **ValidationDialog is null-safe.** The row-selection listener guards against both null selection and null element name before invoking the callback. The canvas `selectElement` method correctly clears prior selection before selecting the new element.

- **Connection rerouting correctly validates targets.** `ConnectionRerouteController.complete()` rejects rerouting to the same element and defers undo saving to a callback.

- **PropertiesPanel form lifecycle is safe.** `ElementForm.dispose()` is called before switching forms, detaching autocomplete listeners. The `cachedFormType` is cleared on rename and when the selection becomes empty, preventing stale form state. `FormContext.updatingFields` guard prevents spurious focus-loss commits during field population.

- **Null safety in startup sequence.** The `updateBreadcrumb` method now guards against null canvas editor, fixing a startup NPE where `clearNavigation` fired the navigation callback before `setModel` initialized the editor.

### Risks

- **Undo snapshot cost.** Each `saveUndoState()` serializes the entire model + view to immutable records. Currently acceptable; a command-based undo system would be more efficient for very large models.

- **StatusBar validation color uses style concatenation.** `updateValidation()` concatenates `-fx-text-fill` onto `Styles.STATUS_LABEL`. If `Styles.STATUS_LABEL` changes format, this could produce invalid CSS. Using `Label.setTextFill()` would be more robust.

- **Clipboard.Entry uses Object for elementDef.** The `elementDef` field is typed as `Object` and cast at paste time. A sealed interface or typed union would provide compile-time safety.

- **ValidationDialog now scrolls to off-screen elements.** `ModelCanvas.selectElement` pans the viewport to center the element, so validation clicks always bring the element into view.

---

## 3. Code Duplication

### Resolved since previous analysis

- **Element-by-name lookup pattern** — `ModelEditor.findByName(List<T>, String, Function<T, String>)` replaces 6 per-type methods with a single generic implementation. The per-type public methods (`getStockByName`, etc.) are now one-line delegates.
- **Equation update pattern** — `ModelEditor.updateEquationByName()` unifies the scan-and-transform pattern for flow and auxiliary equations.
- **PropertiesPanel rebuild cost** — Form caching by `cachedFormType` avoids full UI rebuilds on same-type selection changes.
- **ConnectionRerouteController tests** — 11 tests covering hit testing, reroute completion, self-reroute rejection, and cancellation.
- **Lookup table tests** — ModelEditorTest expanded to 113 tests (up from 103), covering lookup CRUD, rename, and copy/paste.

### Remaining duplication

- **addXxx / addXxxFrom pattern (ModelEditor).** Six element types have paired creation methods (12 methods total). Each pair shares name-generation and registration logic. Unifying each pair (e.g., optional template parameter) would reduce boilerplate but the current approach is clear and explicit.

- **CanvasRenderer effective-width/height boilerplate.** Every element branch in the render loop calls `LayoutMetrics.effectiveWidth(canvasState, name)` and `effectiveHeight(canvasState, name)` then computes `cx - w/2, cy - h/2`. Extracting a bounds record computed once per element would clean this up.

- **ElementForm commit pattern.** Each form's commit handler follows the same pattern: get current def → compare with new value → if changed, call canvas.applyXxx. A generic commit helper in `FormContext` could reduce repetition, though each type has slightly different fields.

---

## 4. Test Coverage

### What's tested (17 UI test classes, 322 UI tests; 45 engine model/def tests)

| Test class | Tests | What it covers |
|---|---|---|
| ModelEditorTest | 113 | Add/remove/rename elements, equation references, flow reconnection, setters, updateInList, removeConnectionReference, addXxxFrom (all types), reroute, lookup CRUD, findByName, updateEquationByName |
| CanvasStateTest | 37 | Position, selection, draw order, rename, load/save ViewDef |
| EquationAutoCompleteTest | 19 | Prefix matching, cursor context extraction, completion insertion |
| NavigationStackTest | 15 | Push/pop/peek, breadcrumb path, depth |
| UndoManagerTest | 14 | Push/undo/redo, max depth, clear |
| HitTesterTest | 14 | Rectangular hit testing, draw order priority, connection hit testing |
| ViewportTest | 13 | Coordinate transforms, zoom, pan, reset |
| FlowCreationControllerTest | 12 | Two-click flow creation, self-loop rejection, cloud-to-cloud rejection |
| ConnectionHitTestTest | 12 | Point-to-segment distance, info link hit testing |
| ConnectionRerouteControllerTest | 11 | Hit testing near from/to ends, miss tolerance, reroute completion, reject self-reroute, cancel |
| ElementRendererTest | 11 | formatValue, isDisplayableEquation |
| FlowEndpointCalculatorTest | 10 | Cloud positions, hit-test clouds and connected endpoints |
| CopyPasteControllerTest | 10 | Copy/paste lifecycle, equation remapping, flow reconnection, name mapping |
| SimulationRunnerTest | 9 | Basic compile-and-run |
| CanvasRendererTest | 6 | clipToBorder geometry (via FlowGeometry) |
| SvgExporterTest | 13 | svgColor hex conversion (4), svgOpacity extraction (3), escapeXml special characters (6) |
| ExportBoundsTest | 3 | Single element bounds, cloud inclusion, padding |

Engine `model/def` tests (separate module):

| Test class | Tests | What it covers |
|---|---|---|
| ModelValidatorTest | 14 | Clean model, DefinitionValidator error wrapping, disconnected flows, missing units, algebraic loops, unused elements |
| ModelDefinitionTest | 14 | Record construction, compact constructor validation, defensive copies |
| DefinitionValidatorTest | 11 | Duplicate names, dangling refs, invalid formulas, circular modules, unbound ports |
| ModelDefinitionBuilderTest | 6 | Builder fluent API, defaults |

### What's not tested

- **All JavaFX UI components.** `PropertiesPanel`, `ForresterApp`, `CanvasToolBar`, `StatusBar`, `BreadcrumbBar`, `InlineEditor`, `BindingConfigDialog`, `SimulationSettingsDialog`, `SimulationResultsDialog`, `ValidationDialog`. These require a running JavaFX toolkit (TestFX or similar) and are currently untested.

- **ModelCanvas interaction logic.** The event handler / coordination layer has no tests. Mouse press → drag → release sequences, marquee selection, keyboard shortcuts, context menu behavior — all untested. This is the highest-risk untested code, though the extraction of logic into testable controllers reduces the risk surface.

- **Extracted controllers without tests.** `InlineEditController` and `ModuleNavigationController` depend on JavaFX components and are not easily unit-testable without TestFX. `DragController`, `MarqueeController`, `ResizeController`, and `ReattachController` depend on CanvasState and could be tested but are not yet.

- **EquationAutoComplete internals.** The test class covers prefix matching and completion logic (19 tests), but popup lifecycle and keyboard navigation are JavaFX-dependent and untested.

### Recommendation

Test coverage is strong — 322 UI tests + 45 engine def tests (367 total), all passing. SvgExporter helpers now have 13 tests. The biggest remaining gaps are the JavaFX-dependent UI components (require TestFX investment) and `EquationAutoComplete` popup/keyboard behavior. The engine-layer `ModelValidator` is well-tested with 14 tests covering all 5 check categories.

---

## 5. API Design & Encapsulation

### Strengths

- **ModelEditor returns unmodifiable lists.** `getStocks()`, `getFlows()`, etc. wrap with `Collections.unmodifiableList()`, preventing accidental mutation by callers.

- **CanvasState returns unmodifiable views.** `getSelection()` and `getDrawOrder()` return unmodifiable wrappers.

- **Record types for data transfer.** `FlowCreationController.State`, `FlowCreationController.FlowResult`, `CanvasRenderer.ReattachState`, `CanvasRenderer.MarqueeState`, `CanvasRenderer.RerouteState`, `NavigationStack.Frame`, `UndoManager.Snapshot`, `FlowEndpointCalculator.CloudHit`, `ResizeHandle.HandleHit`, `FlowGeometry.Point2D`, `ConnectionId`, `ConnectionRerouteController.RerouteHit`, `Clipboard.Entry`, `ExportBounds.Bounds`, and the new `ValidationIssue` / `ValidationResult` — all immutable records that cleanly pass state between components.

- **Generic helpers in ModelEditor.** `findByName` and `updateInList` eliminate duplicated scan-and-replace patterns. `updateEquationByName` provides a clean abstraction for equation transformations.

- **Validation has a clean layered API.** The engine exposes `ModelValidator.validate(ModelDefinition) → ValidationResult`. The UI calls this single method and passes the result to `ValidationDialog`. The dialog communicates back via `Consumer<String>` callback — no coupling to canvas internals.

- **ModelCanvas.selectElement is a focused public method.** It provides exactly the API the validation dialog needs (select by name, redraw) without exposing selection internals.

- **Callback interface for InlineEditController.** The `Callbacks` interface provides a clean contract between the controller and ModelCanvas, without exposing the full canvas API.

- **Clipboard and CopyPasteController are package-private.** Only `ModelCanvas` accesses clipboard internals.

### Weaknesses

- **ModelCanvas still exposes a wide surface.** Public methods include: selection queries, element deletion, element rename, drill-into, binding config, undo state, regeneration, diagram export accessors, loop analysis, status callbacks, the `applyXxx` mutation methods, and now `selectElement`. An interface (e.g., `CanvasActions`) would narrow the coupling.

- **Callback wiring is ad-hoc.** ForresterApp sets callbacks via `setOnStatusChanged`, `setOnNavigationChanged`, `setOnToolChanged`, `setOnLoopToggleChanged`, and now `setOnValidateClicked`. There's no consistent pattern — some use `Runnable`, some use `Consumer<T>`.

- **Clipboard.Entry uses Object for elementDef.** The `elementDef` field is typed as `Object` and cast at paste time. A sealed interface would provide compile-time safety.

---

## 6. Performance Considerations

- **Full redraw on every mouse move during hover.** `handleMouseMoved` calls `redraw()` whenever the hovered element changes. Each `redraw()` clears and repaints the entire canvas. For models with 100+ elements, this may lag.

- **Connector generation on every structural change.** `editor.generateConnectors()` rebuilds the full dependency graph on every element add, remove, rename, equation edit, flow reconnect, connection deletion, reroute, and paste. For large models, caching with invalidation would be more efficient.

- **UndoManager stores full snapshots.** With MAX_UNDO=100 and each snapshot containing a complete `ModelDefinition` + `ViewDef`, memory usage scales as O(undoDepth × modelSize).

- **Validation runs on a background thread.** `validateModel()` now uses `javafx.concurrent.Task` with `setOnSucceeded`/`setOnFailed`, matching the `runSimulation()` pattern. The UI thread is not blocked during validation.

- **PropertiesPanel form builders mitigate rebuild cost.** Same-type selection changes update field values in-place via `ElementForm.updateValues()`. The lookup form with per-row editing is rebuilt when switching element types, but this is unavoidable and only occurs on type transitions.

---

## 7. Style & Consistency

### Strengths

- **Consistent naming.** Classes follow clear patterns: `*Renderer` for drawing, `*Controller` for state machines and interaction logic, `*Dialog` for dialogs, `*Bar` for toolbars/status bars, `*Exporter` for export utilities, `*Validator` for validation logic.

- **Javadoc on all public classes and methods.** The new classes (`ModelValidator`, `ValidationIssue`, `ValidationResult`, `ValidationDialog`, `EquationAutoComplete`) all have class-level and method-level Javadoc.

- **Modern Java features used well.** Records, switch expressions, pattern matching, sealed types, `List.copyOf()`, and generic methods (`findByName`) are used consistently.

- **No wildcard imports.** All imports are explicit across all 44 files.

- **No System.out.println.** All output goes through proper UI channels.

- **CSS centralized in Styles.** All inline CSS strings have been extracted to named constants. (Exception: `StatusBar.updateValidation` concatenates inline color styles — see Risks.)

- **Controller naming is consistent.** All interaction controllers follow the `prepare/start → drag → complete/cancel` lifecycle.

- **Engine validation follows the established utility class pattern.** `ModelValidator` mirrors `DefinitionValidator`: final class, private constructor, single static `validate` method returning an immutable result type.

### Minor Issues

- **Magic numbers in rendering.** Constants like `4` (padding in cloud endpoint), `6` (glow padding), `12` (flow label area in ExportBounds), `30` (paste offset) appear as literals. Most are documented or self-evident in context.

- **StatusBar inline color strings.** `updateValidation()` uses hardcoded hex colors (`#d62728` for red, `#ff7f0e` for orange) concatenated to the style string rather than using `Styles` constants or `Label.setTextFill()`.

---

## 8. File-by-File Summary

| File | Lines | Rating | Key observation |
|---|---|---|---|
| ModelCanvas.java | 1,370 | B+ | Delegates to 8 controllers; selectElement now pans viewport to center element; still the largest class |
| ModelEditor.java | 924 | A- | Down from 977; findByName and updateEquationByName eliminate per-type boilerplate; clean and improving |
| PropertiesPanel.java | 310 | A | Down from 878; delegates to ElementForm implementations; form caching; clean and focused |
| LookupForm.java | 229 | A- | New; extracted from PropertiesPanel; lookup table editing with per-row data points |
| StockForm.java | 113 | A | New; extracted from PropertiesPanel; stock property editing |
| FlowForm.java | 105 | A | New; extracted from PropertiesPanel; flow property editing with autocomplete |
| FormContext.java | 92 | A | New; shared mutable context + UI helpers for form builders |
| AuxForm.java | 87 | A | New; extracted from PropertiesPanel; auxiliary property editing with autocomplete |
| ConstantForm.java | 83 | A | New; extracted from PropertiesPanel; constant property editing |
| ElementForm.java | 26 | A | New; interface defining build, updateValues, dispose lifecycle |
| SvgExporter.java | 627 | A- | Clean SVG generation; lookup rendering; uses shared ExportBounds |
| CanvasRenderer.java | 505 | A- | RerouteState overlay and LOOKUP rendering; clean orchestration |
| ForresterApp.java | 444 | B | Up from 426; validation now on background thread; still mixing lifecycle + controller wiring |
| EquationAutoComplete.java | 305 | B+ | New; well-structured with named constants; popup/keyboard logic is clean; needs more testing |
| CanvasState.java | 273 | A | Clean state management with good encapsulation |
| ElementRenderer.java | 268 | A | drawLookup with dot-dash border and "tbl" badge |
| SimulationResultsDialog.java | 205 | B+ | Functional; checkbox series toggle is well done |
| ConnectionRerouteController.java | 179 | A | Clean state machine with RerouteHit record; now fully tested (11 tests) |
| LayoutMetrics.java | 174 | A | LOOKUP dimensions added consistently |
| FlowEndpointCalculator.java | 172 | A- | Uses FlowGeometry.Point2D throughout |
| FlowCreationController.java | 170 | A | Clean state machine with good validation |
| CopyPasteController.java | 170 | A | Clean two-pass paste algorithm; uses ModelEditor.replaceToken; well-tested |
| InlineEditController.java | 160 | A- | Clean separation with Callbacks interface; depends on JavaFX InlineEditor |
| DiagramExporter.java | 148 | A | Clean export dispatch; delegates bounding box to ExportBounds |
| StatusBar.java | 146 | A- | Up from 115; validation label with visibility binding added; inline color styles |
| CanvasToolBar.java | 143 | A- | Up from 125; Validate button added as plain Button (correct — one-shot action vs toggle) |
| SelectionRenderer.java | 141 | A | Compact, correct, well-separated |
| BindingConfigDialog.java | 137 | A- | Clean dialog; uses Styles constants |
| HitTester.java | 129 | A | Minimal, correct; uses FlowGeometry.clipToElement |
| ModuleNavigationController.java | 128 | A- | Wraps NavigationStack with context menu and bindings dialog |
| ConnectionRenderer.java | 126 | A | Focused rendering utilities |
| ResizeController.java | 116 | A | Clean extracted controller with proper undo integration |
| SimulationRunner.java | 116 | A- | Clean bridge between definition and simulation engine |
| Viewport.java | 115 | A | Simple, correct, well-tested |
| NavigationStack.java | 114 | A | Clean stack with good record usage |
| MarqueeController.java | 106 | A | Clean extracted controller with Shift+marquee support |
| SimulationSettingsDialog.java | 103 | A- | Good validation binding on OK button |
| InlineEditor.java | 98 | B+ | Functional; commit-on-focus-loss can interfere with chaining |
| Clipboard.java | 97 | A- | Clean centroid-relative capture; LOOKUP case added; Object-typed elementDef |
| UndoManager.java | 91 | A | Simple, correct, well-tested |
| BreadcrumbBar.java | 84 | A | Clean, auto-hides at root; uses Styles constants |
| ExportBounds.java | 80 | A | Shared bounding box computation; well-tested |
| ValidationDialog.java | 77 | A | Clean Stage + TableView pattern; row click → element selection with viewport pan |
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

1. **Add EquationAutoComplete tests for popup behavior.** The 19 existing tests cover prefix matching and completion logic, but popup lifecycle (show/hide on focus), keyboard navigation (up/down/enter/escape), and cursor-context parsing are untested. These depend on JavaFX but could be tested with TestFX.

2. **Extract ForresterApp controller logic.** At 444 lines, ForresterApp mixes lifecycle, menu construction, file I/O, and simulation/validation wiring. Extract a `MenuBarController` or similar to own menu actions and reduce the class to pure lifecycle setup.

3. **Type-safe clipboard entries.** Replace `Object elementDef` in `Clipboard.Entry` with a sealed interface or use separate typed lists per element type.

4. **Move StatusBar validation colors to Styles.** Replace inline hex color strings in `updateValidation()` with named constants in `Styles` or use `Label.setTextFill()`.

### Low Priority

5. **Unify callback patterns.** Replace ad-hoc `Runnable`/`Consumer`/`IntConsumer` callbacks with a consistent listener interface or lightweight event bus.

6. **Narrow ModelCanvas public API.** Extract an interface (e.g., `CanvasActions`) that PropertiesPanel depends on, rather than the full `ModelCanvas` class.

7. **Consider incremental undo.** Replace full-snapshot undo with command-based undo for better memory efficiency as models grow larger.

8. **Extract remaining magic numbers.** Move inline numeric literals (paste offset `30`, flow label area `12`, glow padding `6`) into `LayoutMetrics` constants.

---

## 10. Progress Since Previous Analysis

All four medium-priority recommendations from the previous analysis have been addressed:

| # | Previous Recommendation | Status | How Resolved |
|---|---|---|---|
| 1 | Reduce PropertiesPanel size | Done | Extracted `ElementForm` interface + 5 implementations (`StockForm`, `FlowForm`, `AuxForm`, `ConstantForm`, `LookupForm`) + `FormContext`. PropertiesPanel dropped from 878 to 310 lines. |
| 2 | Add SvgExporter tests | Done | `SvgExporterTest` with 13 tests: `svgColor` (4), `svgOpacity` (3), `escapeXml` (6). Made helpers package-private for testability. |
| 3 | Add scroll-to-element on validation click | Done | `ModelCanvas.selectElement` now computes viewport translation to center the element using world→screen coordinate transforms and calls `viewport.restoreState`. |
| 4 | Run validation on background thread | Done | `ForresterApp.validateModel()` now uses `javafx.concurrent.Task` with `setOnSucceeded`/`setOnFailed`, matching the existing `runSimulation()` pattern. |

Cumulative progress from the two prior analysis rounds:

| # | Prior Round Recommendation | Status | How Resolved |
|---|---|---|---|
| 1 | Add ConnectionRerouteController tests | Done | 11 tests covering hit testing, completion, self-rejection, and cancellation |
| 2 | Reduce ModelEditor size | Done | Generic `findByName` and `updateEquationByName`; down from 977 to 924 lines |
| 3 | Cache PropertiesPanel forms | Done | `cachedFormType` tracks current form; superseded by full ElementForm extraction |
| 4 | Add lookup table tests | Done | ModelEditorTest expanded to 113 tests with lookup CRUD, rename, and copy/paste |
