# UI Code Quality Audit

*Audited: 2025-03-05*
*Scope: All Java source files in `forrester-app/src/main/java/.../app/` and `app/canvas/`*
*Total files audited: 44 | Total LOC: ~8,500*

---

## Executive Summary

The UI codebase is functional and well-organized at the individual-file level, but suffers from three systemic problems:

1. **God classes** — `ModelWindow` (1,153 LOC, ~15 responsibilities) and `ModelCanvas` (1,484 LOC, ~10 responsibilities) carry too much weight
2. **Rendering duplication** — `SvgExporter` (639 LOC) is a near-complete parallel reimplementation of the canvas rendering pipeline
3. **Dialog/pane boilerplate** — Identical patterns (sidebar checkboxes, CSV export, parameter rows, background task execution) are copy-pasted across 5+ files

There are no critical security vulnerabilities. There are a handful of genuine bugs (detailed below) and numerous medium-severity code quality issues that increase maintenance cost.

---

## Bugs

These are issues that produce incorrect behavior under specific conditions.

| ID | File | Lines | Description | Severity |
|----|------|-------|-------------|----------|
| B1 | `SvgExporter.java` | (public API) | **No empty-diagram guard.** `SvgExporter.export()` calls `ExportBounds.compute()` without checking for an empty diagram. `ExportBounds` returns degenerate bounds (`MAX_VALUE`/`-MAX_VALUE`) when there are no elements, producing an SVG with a nonsensical `viewBox`. `DiagramExporter` guards against this, but `SvgExporter.export()` is public and can be called directly. | High |
| B2 | `StatusBar.java` | 150 | **Duplicate CSS property.** `updateValidation()` concatenates `Styles.STATUS_LABEL` (which already sets `-fx-text-fill: #555`) with another `-fx-text-fill` override. Relies on JavaFX CSS parser taking the last value, which works in practice but is fragile and produces a confusing style string. | Medium |
| B3 | `ChartUtils.java` | 36-38 | **`formatNumber` integer detection overflows.** `value == Math.floor(value)` returns true for huge doubles like `1e20`, but the `(long)` cast on line 38 silently overflows, producing incorrect output. | Medium |
| B4 | `MultiParameterSweepDialog.java` | 93-96 | **OK button binding doesn't observe field text changes.** The disable binding only triggers when rows are added/removed, not when field contents change. The OK button can appear enabled when inputs are invalid. | Medium |
| B5 | `DiagramExporter.java` | 141 | **JPEG export loses transparency.** `SwingFXUtils.fromFXImage` returns an alpha-capable image, but JPEG format renders transparent regions as black. The canvas background fill usually masks this, but any element transparency renders incorrectly. | Medium |
| B6 | `OptimizerDialog.java` | 136-139 | **OK button disable binding doesn't observe param rows.** When all param rows are removed, the button may still appear enabled. A click-time check catches this with a warning, but the UX is misleading. | Medium |
| B7 | `DiagramExporter.java` | 82 | **Locale-dependent `toLowerCase()`** for extension checking. On a Turkish locale, `.PNG` lowercases to `.png` correctly, but `.JPG` could produce unexpected results. Should use `Locale.ROOT`. | Low |
| B8 | `CanvasState.java` | 260 | **Hardcoded view name "Main"** in `toViewDef()`. Original view names are silently lost during round-tripping. | Low |

---

## Architecture Issues

### 1. ModelWindow God Class (Critical)

**File:** `ModelWindow.java` (1,153 LOC)

Handles UI construction, menu bar, file I/O, simulation execution, parameter sweep execution, multi-parameter sweep execution, Monte Carlo execution, optimization execution, validation, model info dialog, breadcrumb navigation, status bar updates, undo/redo, activity logging, and examples loading.

The five `run*` methods (`runSimulation`, `runParameterSweep`, `runMultiParameterSweep`, `runMonteCarlo`, `runOptimization`) account for ~350 lines of near-identical boilerplate: `ensureSettings()` → show dialog → create background `Task` → wire `onSucceeded`/`onFailed` → start daemon thread.

**Recommendation:** Extract an `AnalysisRunner` helper that encapsulates the background-task pattern, and a `FileOperations` helper for open/save/import/export.

### 2. ModelCanvas God Class (High)

**File:** `ModelCanvas.java` (1,484 LOC, ~85 methods)

Central orchestrator for element creation, deletion, copy/paste, undo/redo, mouse handling (~400 lines), keyboard handling, module navigation, property mutations, tooltip management, and loop analysis lifecycle. Contains 15 nearly identical `applyXxx()` methods (save undo → mutate → redraw).

**Recommendation:** Extract `applyMutation(Runnable)` to replace 15 boilerplate methods. Extract keyboard handling into a `KeyBindingController`.

### 3. SvgExporter Rendering Duplication (High)

**File:** `SvgExporter.java` (639 LOC)

Near-complete reimplementation of `CanvasRenderer` + `ElementRenderer` + `ConnectionRenderer` + `FeedbackLoopRenderer` in SVG format. Flow endpoint resolution, clipping, arrowhead geometry, element shapes, and label rendering are all duplicated. Font sizes and layout offsets are hardcoded as string literals rather than derived from `LayoutMetrics` constants.

Any visual change must be applied in two places. The SVG font sizes will drift from canvas rendering over time.

**Recommendation:** Extract a renderer-agnostic geometry model that computes positions, polygons, and lines. Have both Canvas and SVG consumers translate that geometry to their output format.

### 4. Dialog Parameter Row Duplication (High)

**Files:** `MultiParameterSweepDialog.java`, `MonteCarloDialog.java`, `OptimizerDialog.java`

All three contain nearly identical inner classes (`ParameterRow` / `ParamRow`) with: ComboBox for parameter name, numeric TextFields, remove button, `isValid()`, `toConfig()`, `getPane()`. Total duplication: ~200 lines.

**Recommendation:** Extract a shared `ParameterRowComponent` class.

### 5. Result Pane Duplication (High)

**Files:** `SimulationResultPane`, `SweepResultPane`, `MultiSweepResultPane`, `OptimizationResultPane`, `MonteCarloResultPane`

Three patterns are duplicated across these five files:

| Pattern | Occurrences | ~Lines each |
|---------|-------------|-------------|
| Sidebar checkbox block (VBox + colored CheckBoxes + visibility wiring) | 3 | 20 |
| FileChooser CSV export boilerplate | 8 | 15 |
| Chart construction (NumberAxis + LineChart + stock/variable series iteration) | 4 | 30 |

**Recommendation:** Extract `ChartUtils.buildSeriesSidebar()`, `ExportUtils.showCsvSaveDialog()`, and `ChartUtils.buildTimeSeries()`.

---

## Code Quality Issues by Category

### Scattered Constants and Magic Numbers

| Location | Issue | Severity |
|----------|-------|----------|
| `CanvasRenderer.java:20-21` | `RUBBER_BAND_COLOR`, `STOCK_HOVER_COLOR` defined locally instead of in `ColorPalette` | Low |
| `CanvasRenderer.java:495` | Marquee fill `Color.web("#4A90D9", 0.1)` inlined | Low |
| `SelectionRenderer.java:13` | `SELECTION_COLOR` duplicates `#4A90D9` from `CanvasRenderer` | Low |
| `SvgExporter.java:278,298,308,539` | Magic numbers `2.5` and `6` duplicated from `FeedbackLoopRenderer` private constants | Medium |
| `SvgExporter.java:340,372,406...` | Font sizes hardcoded as string literals instead of derived from `LayoutMetrics` | Medium |
| `ActivityLogPanel.java:29,32,36` | Inline CSS styles not in `Styles.java` | Low |

### Null Returns Instead of Optional

Per project guidelines (`CLAUDE.md`), methods should return `Optional` for absent values.

| File | Methods returning null |
|------|----------------------|
| `ModelEditor.java` | `getLookupTableByName`, `getStockByName`, `getFlowByName`, `getAuxByName`, `getModuleByName`, `getConstantByName`, `findByName` (~8 methods) |
| `CanvasState.java` | `getType()` |
| `UndoManager.java` | `undo()`, `redo()` |

### Exception Handling

| Location | Issue | Severity |
|----------|-------|----------|
| `SweepResultPane.java:157,177` | Catches `RuntimeException` instead of `IOException` — hides NPEs and other programming errors as "export failed" | High |
| `MonteCarloResultPane.java:91` | Same broad `RuntimeException` catch | High |
| `MultiSweepResultPane.java:245,265` | Same broad `RuntimeException` catch (×2) | High |
| `ModelWindow.java:496,517` | Catches `Exception` (overly broad) in example loading | Low |

### Thread Safety

| Location | Issue | Severity |
|----------|-------|----------|
| `ModelEditor.java` | Mutable state with no synchronization or documented thread-confinement contract | High |
| `ModelEditor.java:42` | Listener list is a plain `ArrayList` — `ConcurrentModificationException` risk if listener callback modifies the list | Medium |
| `ModelWindow.java:643-954` | Background threads created with `new Thread()` instead of an executor — repeated clicks can pile up threads | Medium |
| `CanvasState.java` | No thread safety documentation | Low |
| `Viewport.java` | Mutable fields, no synchronization or documentation | Low |

### API Design

| Location | Issue | Severity |
|----------|-------|----------|
| `CanvasRenderer.java:78-87` | `render()` method has 13 parameters — needs a parameter object | Medium |
| `Clipboard.java:30` | `Entry.elementDef` typed as `Object` — loses type safety, requires casting | Medium |
| `ModelDefinition` (record) | 12-argument constructor used in ~6-8 call sites across 4 files — fragile, hard to read | Medium |
| `ChartUtils.java:12-15` | `SERIES_COLORS` is a package-visible mutable `String[]` — any caller can corrupt the palette | High |
| `ElementRenderer.java:62-86` | `drawFlow` and `drawAux` accept `equation` parameter but never use it — dead parameter | Medium |

### Performance

| Location | Issue | Severity |
|----------|-------|----------|
| `ElementRenderer.java:263-278` | `truncate()` allocates a new `Text` node on every call — creates GC pressure during rendering | Medium |
| `CanvasState.java:206,232,270` | `drawOrder` list operations (`contains`, `indexOf`, `remove`) are O(n) — could be slow for large models | Low |
| `UndoManager.java` | Stores full `ModelDefinition` + `ViewDef` snapshots ×100 levels — high memory cost for large models | Medium |

### Style and Consistency

| Issue | Locations | Severity |
|-------|-----------|----------|
| `default` branch in exhaustive enum switch masks future additions | `LayoutMetrics:99-108,113-123`, `Clipboard:83`, `PropertiesPanel:247-254` | Medium |
| All styles are inline Java strings — no CSS files for theming/dark mode | `Styles.java`, all UI files | Medium |
| `Styles.java` incomplete — `ActivityLogPanel` uses its own inline styles | `ActivityLogPanel:29,32,36` | Low |
| `FORCE_RELAYOUT` hardcoded `true` with dead else branch | `ModelWindow:399-400` | Medium |
| Inconsistent validation: `ParameterSweepDialog` requires `start < end`, `MultiParameterSweepDialog` allows `start == end` | Cross-dialog | Medium |
| `OptimizationResultPane` uses built-in legend; all other panes hide legend and use checkbox sidebar | Cross-pane | Medium |
| Only `SimulationResultPane` offers PNG export; other chart panes do not | Cross-pane | Medium |

---

## Metrics Summary

### By Severity

| Severity | Count |
|----------|-------|
| Critical | 1 (ModelWindow god class) |
| High | 14 |
| Medium | 33 |
| Low | 30 |
| **Total** | **78** |

### By Category

| Category | Count |
|----------|-------|
| Architecture / God class | 2 |
| Code duplication | 8 |
| Bugs (incorrect behavior) | 8 |
| Exception handling | 7 |
| Thread safety | 5 |
| API design | 5 |
| Constants / magic numbers | 6 |
| Null vs Optional | 3 |
| Performance | 3 |
| Style / consistency | 8 |
| Dead code / dead parameters | 3 |

### Largest Files (LOC)

| File | LOC | Issues |
|------|-----|--------|
| ModelCanvas.java | 1,484 | 9 |
| ModelWindow.java | 1,153 | 11 |
| ModelEditor.java | 1,059 | 8 |
| SvgExporter.java | 639 | 10 |
| CanvasRenderer.java | 505 | 5 |
| PropertiesPanel.java | 327 | 3 |
| CanvasState.java | 273 | 5 |
| MultiSweepResultPane.java | 270 | 7 |
| OptimizerDialog.java | 270 | 4 |
| MultiParameterSweepDialog.java | 254 | 3 |

---

## Recommended Priorities

### Immediate (fix bugs)

1. Add empty-diagram guard to `SvgExporter.export()` (B1)
2. Fix `StatusBar` CSS concatenation (B2)
3. Fix `ChartUtils.formatNumber` overflow (B3)
4. Fix dialog OK-button bindings (B4, B6)
5. Change `RuntimeException` catches to specific exception types (6 occurrences)
6. Make `ChartUtils.SERIES_COLORS` unmodifiable

### Short-term (reduce duplication)

7. Extract `AnalysisRunner` from `ModelWindow` (~350 lines saved)
8. Extract `ChartUtils.buildSeriesSidebar()` (~60 lines saved across 3 files)
9. Extract `ExportUtils.showCsvSaveDialog()` (~120 lines saved across 5 files)
10. Extract shared `ParameterRowComponent` from 3 dialog classes (~200 lines saved)

### Medium-term (architecture)

11. Extract renderer-agnostic geometry model to eliminate `SvgExporter` duplication
12. Add `toBuilder()` or `with*()` methods to `ModelDefinition` record
13. Replace null returns with `Optional` in `ModelEditor` (8 methods)
14. Introduce a reusable `Text` node in `ElementRenderer.truncate()` for performance
15. Move from inline CSS strings to `.css` stylesheet files
