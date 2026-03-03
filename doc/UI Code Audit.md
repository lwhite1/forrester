# UI Code Audit — Visual Editor & Chart Viewer

Audit of the `forrester-app` (visual editor) and `forrester-ui` (chart viewers) modules. Covers bugs, design issues, javadoc mismatches, dead code, and test coverage gaps.

## 1. Scope

### forrester-app (25 classes)

| Package | Classes |
|---------|---------|
| `com.deathrayresearch.forrester.app` | ForresterApp, Launcher |
| `com.deathrayresearch.forrester.app.canvas` | BindingConfigDialog, BreadcrumbBar, CanvasRenderer, CanvasState, CanvasToolBar, ColorPalette, ConnectionRenderer, ElementRenderer, FlowCreationController, FlowEndpointCalculator, HitTester, InlineEditor, LayoutMetrics, ModelCanvas, ModelEditor, NavigationStack, SelectionRenderer, SimulationResultsDialog, SimulationRunner, SimulationSettingsDialog, StatusBar, UndoManager, Viewport |

### forrester-ui (5 classes)

| Package | Classes |
|---------|---------|
| `com.deathrayresearch.forrester.ui` | ChartViewerApplication, FanChart, FlowChartViewer, StockLevelChartViewer, package-info |

---

## 2. Bugs

### ~~BUG-1 — NPE: `currentFile.getParent()` can return null (High)~~ **FIXED**
**File:** `ForresterApp.java:245`

Added a null check for `getParent()` before calling `toFile()`.

### ~~BUG-2 — `restoreState()` doesn't validate scale bounds (Medium)~~ **FIXED**
**File:** `Viewport.java:98–102`

Scale is now clamped to `[MIN_SCALE, MAX_SCALE]` in `restoreState()`, consistent with `zoomAt()`.

### ~~BUG-3 — No explicit NaN guard in `formatValue()` (Low)~~ **FIXED**
**File:** `ElementRenderer.java:217–222`

Added explicit `Double.isNaN()` check at the top of `formatValue()`.

---

## 3. Design Issues

### ~~DESIGN-1 — Duplicate rename logic across 5 element types (Medium)~~ **FIXED**
**File:** `ModelEditor.java:227–285`

Extracted a generic `renameInList()` helper that replaces five identical find-by-name / reconstruct-record blocks with a single parameterized method.

### ~~DESIGN-2 — Rebuilds 4 lookup maps on every render call (Medium)~~ **FIXED**
**File:** `CanvasRenderer.java:84–105`

Replaced per-frame `HashMap` construction with direct lookup methods on `ModelEditor` (`getConstantByName`, `getStockUnit`, `getFlowEquation`, `getAuxEquation`). No maps are built during rendering.

### DESIGN-3 — Extensive mutable state in ModelCanvas (Medium)
**File:** `ModelCanvas.java:44–103`

Approximately 30 mutable fields track drag, pan, marquee, flow-creation, reattachment, inline-editing, and undo state. Acceptable for JavaFX's single-thread model but makes the class difficult to reason about. State groups could be extracted into focused state objects (as was done with `FlowCreationController`).

### ~~DESIGN-4 — Silent validation failures with no user feedback (High)~~ **FIXED**
**File:** `FlowCreationController.java:60–66`

Introduced a `FlowResult` record that distinguishes pending, created, and rejected outcomes. Rejected validations now cancel the pending state (clearing the rubber-band) and return a human-readable rejection reason.

### DESIGN-5 — Static mutable state prevents multi-window use (Medium)
**File:** `ChartViewerApplication.java:40–47`

Eight `static` mutable fields (`scene`, `series`, `width`, `height`, `lineChart`, `formatter`, `title`, `xAxisLabel`) mean only one chart configuration can exist per JVM. Calling `setSimulation()` or `addSeries()` between launches silently corrupts shared state. Combined with the JavaFX single-`launch()` limitation, the class cannot support multiple chart windows.

---

## 4. Javadoc Mismatches

### ~~JAVADOC-1 — `getPath()` accepts unused `currentName` parameter~~ **FIXED**
**File:** `NavigationStack.java:83–107`

Removed the unused `currentName` parameter from `getPath()` and cleaned up the stream-of-consciousness comments. Updated all call sites and tests.

---

## 5. Dead Code & Unused Imports

### ~~DEAD-1 — `hitTestDiamond()` never called~~ **FIXED**
**File:** `HitTester.java:65–69`

Removed the unused `hitTestDiamond()` method.

### ~~DEAD-2 — Unused `javafx.geometry.Side` import~~ **FIXED**
**File:** `SimulationResultsDialog.java:7`

Removed unused import (fixed during initial audit).

---

## 6. Test Coverage

### forrester-app test summary

| Metric | Value |
|--------|-------|
| Test classes | 11 |
| Test methods (`@Test`) | 230 |
| Source classes with tests | 11 / 25 (44%) |
| Source classes without tests | 14 / 25 (56%) |

### Tested classes

| Class | Test Class |
|-------|-----------|
| CanvasRenderer | CanvasRendererTest |
| CanvasState | CanvasStateTest |
| ElementRenderer | ElementRendererTest |
| FlowCreationController | FlowCreationControllerTest |
| FlowEndpointCalculator | FlowEndpointCalculatorTest |
| HitTester | HitTesterTest |
| ModelEditor | ModelEditorTest |
| NavigationStack | NavigationStackTest |
| SimulationRunner | SimulationRunnerTest |
| UndoManager | UndoManagerTest |
| Viewport | ViewportTest |

### Untested classes (forrester-app)

| Class | Category | Risk |
|-------|----------|------|
| ForresterApp | Application shell | Low — JavaFX lifecycle |
| Launcher | Main entry point | Low — trivial |
| BindingConfigDialog | Dialog UI | Low — visual |
| BreadcrumbBar | Navigation UI | Low — visual |
| CanvasToolBar | Toolbar UI | Low — visual |
| ColorPalette | Constants | Low — static values |
| ConnectionRenderer | Rendering | Low — visual |
| InlineEditor | Inline editing UI | Medium — user interaction |
| LayoutMetrics | Layout constants | Low — static values |
| ModelCanvas | Canvas orchestration | Medium — complex state |
| SelectionRenderer | Rendering | Low — visual |
| SimulationResultsDialog | Dialog UI | Low — visual |
| SimulationSettingsDialog | Dialog UI | Low — visual |
| StatusBar | Status display | Low — visual |

### Untested classes (forrester-ui)

| Class | Category | Risk |
|-------|----------|------|
| ChartViewerApplication | Chart viewer app | Low — visual |
| FanChart | Fan chart | Low — visual |
| FlowChartViewer | Flow chart viewer | Low — visual |
| StockLevelChartViewer | Stock chart viewer | Low — visual |

Most untested classes are JavaFX UI components (dialogs, renderers, toolbars) that require a running JavaFX toolkit for testing. The data-layer and logic classes are well covered.

---

## 7. forrester-ui Module

### Static mutable state (see DESIGN-5)

All of `ChartViewerApplication`'s fields are `static` and mutable:

| Field | Type | Line |
|-------|------|------|
| `scene` | Scene | 40 |
| `series` | ArrayList\<Series\> | 41 |
| `width` | double | 42 |
| `height` | double | 43 |
| `lineChart` | LineChart | 44 |
| `formatter` | DateTimeFormatter | 45 |
| `title` | String | 46 |
| `xAxisLabel` | String | 47 |

### Single-launch limitation

`Application.launch()` is a JavaFX constraint (can only be called once per JVM). Second simulation run with chart viewer fails. Already documented in `SystemAudit.md` §5.

### No test coverage

Zero test classes exist in `forrester-ui`. All 5 source classes (4 viewers + package-info) are untested. Risk is low since these are pure visualization classes.

---

## 8. Summary

| Category | Count | Fixed | Remaining |
|----------|-------|-------|-----------|
| Bugs | 3 | 3 | 0 |
| Design issues | 5 | 3 | 2 (DESIGN-3, DESIGN-5 — observations, not actionable bugs) |
| Javadoc mismatches | 1 | 1 | 0 |
| Dead code / unused imports | 2 | 2 | 0 |
| Untested classes | 18 | — | 18 (low-risk JavaFX UI classes) |
| **Total actionable** | **11** | **9** | **2** |

### Fixes applied

- **BUG-1**: Added null guard for `Path.getParent()` in `ForresterApp.saveAs()`
- **BUG-2**: Clamped scale to `[MIN_SCALE, MAX_SCALE]` in `Viewport.restoreState()`
- **BUG-3**: Added explicit `NaN` guard in `ElementRenderer.formatValue()`
- **DESIGN-1**: Extracted generic `renameInList()` helper in `ModelEditor`
- **DESIGN-2**: Replaced per-frame map construction with direct `ModelEditor` lookup methods
- **DESIGN-4**: Introduced `FlowResult` record; rejections now cancel pending state and return reasons
- **JAVADOC-1**: Removed unused `currentName` parameter from `NavigationStack.getPath()`
- **DEAD-1**: Removed unused `hitTestDiamond()` from `HitTester`
- **DEAD-2**: Removed unused `Side` import from `SimulationResultsDialog`
