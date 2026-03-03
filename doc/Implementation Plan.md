# Implementation Plan — Forrester Canvas Editor

Living document tracking what has been built, what's in progress, and what's ahead.

## Completed Phases

### Phase 1 — Static Model Rendering (commit 9a211c9)

Render a hardcoded SIR model onto a JavaFX Canvas.

| File | Purpose |
|------|---------|
| `ForresterApp.java` | JavaFX entry point, builds SIR ModelDefinition, creates canvas |
| `Launcher.java` | Main class (JavaFX module workaround) |
| `ColorPalette.java` | Color constants for the Layered Flow Diagram visual language |
| `LayoutMetrics.java` | Element dimensions, spacing, fonts |
| `ElementRenderer.java` | Draws stocks, flows, auxiliaries, constants |
| `ConnectionRenderer.java` | Draws material flow arrows and info link dashes |

**Result:** Static rendering of stocks, flows, auxiliaries, constants with material flow arrows and info link connectors. Auto-layout positions elements.

---

### Phase 2 — Pan, Zoom, Selection, Drag (commit bd83453)

Make the canvas interactive.

| File | Purpose |
|------|---------|
| `Viewport.java` | Pan/zoom transform, world↔screen coordinate conversion |
| `HitTester.java` | Click → element resolution (reverse draw order, rect + diamond shapes) |
| `SelectionRenderer.java` | Dashed blue outlines and corner handles on selected elements |
| `CanvasState.java` | Mutable element positions, types, draw order, selection set |
| `ModelCanvas.java` | Event handlers: scroll→zoom, drag→pan/move, click→select |

**Result:** Zoom at cursor, pan via Space+drag or middle/right-drag, click-to-select, shift-click multi-select, drag-to-move (multi-element).

---

### Phase 3 — Element Creation & Deletion (commit e7f56e8)

Add and remove model elements via a toolbar.

| File | Purpose |
|------|---------|
| `CanvasToolBar.java` | Toggle buttons: Select, Stock, Flow, Auxiliary, Constant |
| `ModelEditor.java` | Mutable model layer — add/remove elements, auto-naming, immutable snapshot rebuild |
| `ModelCanvas.java` | Placement mode (click to create), Delete/Backspace to remove |
| `CanvasState.java` | `addElement` / `removeElement` for canvas-side state |

**Result:** Toolbar with mutually exclusive tools. Click canvas in placement mode to create auto-named elements. Delete key removes selected elements. Stock deletion nullifies flow source/sink references. Connectors regenerated after mutations.

---

### Phase 4 — Flow Connections & Inline Editing (commits b3009d5, 466caaf)

Connect flows to stocks and edit element properties inline.

| File | Change |
|------|--------|
| `ModelEditor.java` | `addFlow(source, sink)`, `renameElement` with reference propagation, `setConstantValue`, `hasElement`, unmodifiable list getters |
| `CanvasState.java` | `renameElement` with collision check |
| `ModelCanvas.java` | Two-click flow protocol, rubber-band line, mouse-moved tracking, double-click inline editing, pending flow cancellation, direction-aware flow routing through diamond |
| `ConnectionRenderer.java` | `drawMaterialFlow` reworked: routes source→diamond→sink, direction-aware clouds, `drawCloudAt` public delegate |
| `ElementRenderer.java` | `formatValue` made package-private |
| `InlineEditor.java` | **New.** TextField overlay manager — open/close/commit/cancel lifecycle |
| `ForresterApp.java` | `canvas.setOverlayPane(canvasPane)` wiring |

**Result:** Click Flow → click source (stock or cloud) → rubber-band follows cursor with stock hover highlight → click sink → flow created at midpoint with proper connections. Double-click any element to rename; constants chain name→value editing. Rename propagates to flow refs and equation tokens. Material flows route through the diamond indicator with direction-aware attachment.

**Audit:** All 7 major bugs found in the Phase 4 audit have been fixed. See `doc/UI Audit — Phase 4.md` for remaining minor/nit findings.

---

### Phase 5 — Refactor & Flow Reattachment

Three-part refactoring and feature phase addressing code quality and usability.

#### Part 1: ElementType Enum

Replaced all `"stock"`, `"flow"`, `"aux"`, `"constant"` string literals with a proper `ElementType` enum for type safety.

| File | Change |
|------|--------|
| `ElementType.java` | **New.** Enum with `label()` / `fromLabel()` for serialization compat |
| `ElementPlacement.java` | `String type` → `ElementType type`, removed `VALID_TYPES` set |
| `AutoLayout.java` | String literals → `ElementType.STOCK` etc. |
| `ModelDefinitionSerializer.java` | `type.label()` / `ElementType.fromLabel()` |
| `XmileViewParser.java` | `ElementType.fromLabel()` at parse boundary |
| `XmileViewWriter.java` | `type.label()` at write boundary |
| `SketchParser.java` | `ElementType.fromLabel()` at parse boundary |
| `CanvasState.java` | `Map<String, ElementType>`, typed `getType()` / `addElement()` |
| `LayoutMetrics.java` | `widthFor(ElementType)` / `heightFor(ElementType)` |
| `HitTester.java` | Enum comparison instead of `.equals()` |
| `SelectionRenderer.java` | Enum comparison instead of `.equals()` |
| `ModelCanvas.java` | All type switches use enum |
| Engine + app tests (6 files) | Updated to use `ElementType` constants |

#### Part 2: ModelCanvas Decomposition

Extracted two focused classes, reducing ModelCanvas from ~760 to ~560 lines.

| File | Purpose |
|------|---------|
| `FlowCreationController.java` | **New.** Two-click flow creation state machine with `State` record |
| `FlowCreationControllerTest.java` | **New.** 8 tests for state machine (no JavaFX dependency) |
| `CanvasRenderer.java` | **New.** Rendering coordinator — draws connections, elements, selection, overlays |
| `ModelCanvas.java` | Delegates to `FlowCreationController` and `CanvasRenderer` |

#### Part 3: Flow Reattachment

Drag cloud endpoints onto stocks to reconnect flows, or drag connected endpoints off stocks to disconnect.

| File | Purpose |
|------|---------|
| `FlowEndpointCalculator.java` | **New.** Cloud position computation, cloud hit testing, connected endpoint hit testing |
| `FlowEndpointCalculatorTest.java` | **New.** 9 tests for cloud positions, hit testing |
| `ModelEditor.java` | Added `reconnectFlow(flowName, end, stockName)` |
| `ModelEditorTest.java` | Added 5 reconnect flow tests |
| `ModelCanvas.java` | Reattachment drag state, mouse event handling for drag-to-reconnect, Escape to cancel |
| `CanvasRenderer.java` | `ReattachState` record, rubber-band + stock hover highlight during reattachment |

**Result:** ElementType enum eliminates type-string bugs. ModelCanvas decomposed into three focused classes. Flow endpoints can be dragged onto stocks (reattach) or off stocks (disconnect to cloud). Rubber-band line with stock hover highlight during reattachment drag. Escape cancels reattachment.

---

### Phase 6 — File Save/Load & Bug Fixes (commits 2a7d54f, 763a93a, 099719e)

Two-part phase: added file persistence and fixed audit findings from Phase 5.

#### Part 1: File Save/Load & Phase 5 Bug Fixes

| File | Change |
|------|--------|
| `ForresterApp.java` | File menu with New, Open, Save, Save As (Ctrl+N/O/S/Shift+S); `ModelDefinitionSerializer` wiring; title bar shows filename |
| `ModelEditor.java` | `loadFrom(ModelDefinition)`, `toModelDefinition(ViewDef)` for round-trip serialization |
| `CanvasState.java` | `toViewDef()` to convert canvas state back to immutable `ViewDef` for serialization |
| `ModelCanvas.java` | `setModel(editor, view)` and `toModelDefinition()` public API for load/save |
| `ConnectionRenderer.java` | Consolidated duplicate `CLOUD_OFFSET` constant |
| `FlowEndpointCalculator.java` | Uses `ConnectionRenderer.CLOUD_OFFSET` instead of own constant |
| `ElementRenderer.java` | Fixed `formatValue` precision |
| `CanvasStateTest.java` | Added `toViewDef` round-trip tests |
| `ModelEditorTest.java` | Added `toModelDefinition` round-trip and rename propagation tests |

#### Part 2: Remaining Audit Fixes

| File | Change |
|------|--------|
| `ForresterApp.java` | Added Close and Exit menu items |
| `ModelEditor.java` | Self-loop guard in `reconnectFlow`, stock existence validation, identifier validation |
| `FlowCreationController.java` | Self-loop prevention (source == sink rejected) |
| `CanvasRenderer.java` | Cloud preview when dropping reattachment on empty space |
| `CanvasRendererTest.java` | **New.** Tests for rendering coordinator |
| `ElementRendererTest.java` | **New.** Tests for element formatting |
| `FlowCreationControllerTest.java` | Added self-loop rejection tests |
| `ModelEditorTest.java` | Added validation and reconnect guard tests |

**Result:** Models can be saved to and loaded from JSON files. All major Phase 5 audit bugs resolved: unified cloud offset, self-loop prevention, reconnect validation. File menu has full lifecycle (New, Open, Save, Save As, Close, Exit).

**Audit:** See `doc/UI Audit — Phase 5.md` for the full list with resolution status.

---

### Phase 7 — Status Bar, Equation Editing & Undo (commits a2d7aad, 92bb4b1, cf18a6c)

Added status bar, inline equation editing for flows and auxiliaries, and undo/redo support.

| File | Change |
|------|--------|
| `StatusBar.java` | **New.** Shows active tool, selection count, element counts (stocks/flows/aux/constants), zoom level |
| `UndoManager.java` | **New.** Snapshot-based undo/redo stack with `Snapshot(ModelDefinition, ViewDef)` pairs |
| `UndoManagerTest.java` | **New.** 13 tests for undo/redo lifecycle, capacity limits, clear |
| `ForresterApp.java` | Undo/Redo menu items (Ctrl+Z / Ctrl+Shift+Z), status bar wiring, undo manager integration |
| `ModelCanvas.java` | Undo snapshots before mutations, `performUndo()` / `performRedo()` methods |
| `ModelEditor.java` | `setFlowEquation()` / `setAuxEquation()` / `getFlowEquation()` / `getAuxEquation()` for equation editing |
| `CanvasRenderer.java` | Renders equation text below flow diamonds and auxiliary circles |
| `ElementRenderer.java` | Equation text rendering for flows and auxiliaries |
| `LayoutMetrics.java` | Equation text font and positioning constants |
| `HitTester.java` | Updated hit areas for elements with equation text |
| `ElementRendererTest.java` | Added equation formatting tests |
| `ModelEditorTest.java` | Added equation get/set, round-trip, and rename propagation tests |

**Result:** Status bar shows tool, selection, element counts, and zoom. Double-click flows or auxiliaries to edit their equations inline. Undo/redo via Edit menu or keyboard shortcuts. Snapshots capture full model + view state before each mutation.

**Audit:** See `doc/UI Audit — Phase 7.md` for findings and resolution status.

---

### Phase 8 — Simulation Running (commits 5c4c92a, ea56674)

Wire the existing compilation and simulation pipeline to the UI with a Simulate menu.

| File | Change |
|------|--------|
| `SimulationRunner.java` | **New.** Non-UI class: compiles `ModelDefinition`, runs `Simulation`, captures time-series via `DataCaptureHandler` |
| `SimulationSettingsDialog.java` | **New.** Dialog for time step unit, duration, and duration unit configuration |
| `SimulationResultsDialog.java` | **New.** Separate window with `TableView` showing simulation results |
| `SimulationRunnerTest.java` | **New.** 9 tests: column names, row count, initial values, variable capture, error handling, empty model |
| `ModelEditor.java` | Added `simulationSettings` field with getter/setter, round-trip through `toModelDefinition` |
| `ModelEditorTest.java` | Added `SimulationSettings` round-trip tests (non-null and null) |
| `ForresterApp.java` | Simulate menu: Simulation Settings dialog + Run Simulation (Ctrl+R) on background thread |
| `ModelCanvas.java` | `setUndoManager()` wiring for undo integration |
| `CanvasState.java` | Replaced mutable `double[]` positions with immutable `Position` record |

**Result:** Ctrl+R compiles the model and runs a simulation on a background thread. If no settings exist, prompts first. Results displayed in a sortable table window. Simulation errors shown in an alert dialog. Settings saved/loaded with the model JSON.

**Audit:** See `doc/UI Audit — Phase 8.md` for findings and resolution status.

---

### Phase 9 — Keyboard Shortcuts & Cursor Feedback

Added number-key tool switching, keyboard zoom controls, and context-sensitive cursor shapes.

| File | Change |
|------|--------|
| `ModelCanvas.java` | Number keys 1–5 switch tools, Ctrl+Plus/Minus/0 zoom, Escape priority chain, `updateCursor()` with `lastMouseX`/`lastMouseY` tracking |
| `CanvasToolBar.java` | `selectTool()` and `resetToSelect()` methods for programmatic tool switching |
| `ForresterApp.java` | Edit menu Select All (Ctrl+A) |

**Keyboard shortcuts added:**
- 1–5: Switch tool (Select, Stock, Flow, Auxiliary, Constant)
- Ctrl+Plus/Equals: Zoom in at center
- Ctrl+Minus: Zoom out at center
- Ctrl+0: Reset zoom to 100%
- Ctrl+A: Select all elements

**Cursor shapes added:**

| State | Cursor |
|-------|--------|
| Default / idle | Default arrow |
| Hovering element (Select mode) | Open hand |
| Hovering cloud/connected endpoint | Pointing hand |
| Dragging element | Closed hand |
| Space held (pan ready) | Move (four-way arrow) |
| Panning | Closed hand |
| Placement mode | Crosshair |
| Flow pending (rubber-band) | Crosshair |
| Reattaching endpoint | Closed hand |

**Result:** All tool modes accessible via keyboard. Cursor provides visual feedback for every interaction state. Priority chain ensures the most relevant cursor is always shown.

---

### Phase 10 — Rubber-Band (Marquee) Selection

Added marquee selection: drag on empty canvas to select multiple elements at once.

| File | Change |
|------|--------|
| `ModelCanvas.java` | Marquee state fields, `updateMarqueeSelection()`, `cancelMarquee()`, marquee in `handleMousePressed` / `handleMouseDragged` / `handleMouseReleased` / `handleEscape`, crosshair cursor during marquee |
| `CanvasRenderer.java` | `MarqueeState` record, `drawMarquee()` renders semi-transparent blue rect with dashed border |
| `CanvasState.java` | `addToSelection(name)` method for non-clearing selection additions |

**Behavior:**
- Left-drag on empty space in Select mode draws a selection rectangle
- Elements whose center falls inside the rectangle are selected live during drag
- Shift+drag adds to existing selection instead of replacing it
- Escape cancels the marquee and restores the pre-marquee selection
- Mouse release finalizes the selection and removes the marquee rectangle
- Cursor shows crosshair during marquee drag

**Result:** Users can select multiple elements by dragging a rectangle on the canvas. Shift+marquee adds to the existing selection. Escape cancels without changing the original selection.

**Audit:** See `doc/UI Audit — Phase 10.md` for findings and resolution status.

---

### Phase 11 — Module/Submodel Support (current)

Added module as the 5th interactive element type. Modules are opaque container boxes — a thick-bordered rectangle with a "mod" badge. Click to place, double-click to rename, drag/select/delete/undo all work. No drill-down or binding configuration in this phase.

| File | Change |
|------|--------|
| `LayoutMetrics.java` | `MODULE_WIDTH` (120), `MODULE_HEIGHT` (70), `MODULE_BORDER_WIDTH` (2.0), `MODULE_CORNER_RADIUS` (6), `MODULE_NAME_FONT` (bold 13pt), `MODULE` cases in `widthFor()` / `heightFor()` |
| `ElementRenderer.java` | `drawModule()` — white fill, 2px #2C3E50 border, 6px corners, "mod" badge top-left, bold centered name |
| `ModelEditor.java` | `modules` list, `ModuleInstanceDef` import, `loadFrom` clears/loads/indexes modules, `addModule()` auto-naming, `removeElement` handles modules, `renameElement` handles modules, `toModelDefinition` includes `List.copyOf(modules)`, `getModules()` getter |
| `CanvasToolBar.java` | `PLACE_MODULE` in `Tool` enum, "Module" toggle button |
| `StatusBar.java` | `PLACE_MODULE` → "Place Module", `updateElements` accepts `int modules` parameter, shows "X mod" when > 0 |
| `CanvasRenderer.java` | `case MODULE` rendering block calling `ElementRenderer.drawModule()` |
| `ModelCanvas.java` | `PLACE_MODULE` in `createElementAt()`, `DIGIT6` keyboard shortcut, MODULE uses default inline edit branch (name-only) |
| `ForresterApp.java` | `editor.getModules().size()` in `updateStatusBar()` |
| `ModelEditorTest.java` | `@Nested Modules` class with 9 tests: auto-naming, remove, rename, reject duplicate, loadFrom, toModelDefinition, round-trip, clear on reload, continue numbering |

**Result:** Module is the 5th element type in the visual editor. Users can create modules via toolbar button or key 6, place on canvas, rename via double-click, drag/select/delete, undo/redo, and save/load with full JSON round-trip. Status bar shows module counts. Engine-level `ModuleInstanceDef` records are preserved through the editor layer.

---

## Current State

### Source files (forrester-app)

```
src/main/java/com/deathrayresearch/forrester/app/
├── ForresterApp.java              — JavaFX entry point, menus, undo/simulation wiring
├── Launcher.java                  — Main class
└── canvas/
    ├── CanvasRenderer.java        — Rendering coordinator (connections, elements, overlays)
    ├── CanvasState.java           — Mutable positions (Position record), types, draw order, selection
    ├── CanvasToolBar.java         — Tool toggle buttons
    ├── ColorPalette.java          — Color constants
    ├── ConnectionRenderer.java    — Material flows and info links
    ├── ElementRenderer.java       — Stock, flow, aux, constant, module shapes + equation text
    ├── FlowCreationController.java — Two-click flow state machine
    ├── FlowEndpointCalculator.java — Cloud positions and endpoint hit testing
    ├── HitTester.java             — Click → element resolution
    ├── InlineEditor.java          — TextField overlay for inline editing
    ├── LayoutMetrics.java         — Dimensions, fonts, equation text metrics
    ├── ModelCanvas.java           — Event handling + editing orchestration
    ├── ModelEditor.java           — Mutable model editing layer + simulation settings + modules
    ├── SelectionRenderer.java     — Selection indicators
    ├── SimulationResultsDialog.java — Results table window
    ├── SimulationRunner.java      — Compile + run + capture simulation results
    ├── SimulationSettingsDialog.java — Settings input dialog
    ├── StatusBar.java             — Tool, selection, element counts, zoom display
    ├── UndoManager.java           — Snapshot-based undo/redo stack
    └── Viewport.java              — Pan/zoom transforms
```

### Test coverage

- 204 tests, all passing
- `ViewportTest` — coordinate transforms, zoom, pan (11 tests)
- `HitTesterTest` — rect and diamond hit testing
- `CanvasStateTest` — load, position, selection, add/remove/rename, toViewDef
- `CanvasRendererTest` — rendering coordinator
- `ElementRendererTest` — element formatting, equation text
- `ModelEditorTest` — load, add, remove, rename, equations, reconnect, simulation settings, modules, round-trip
- `FlowCreationControllerTest` — two-click state machine, self-loop prevention
- `FlowEndpointCalculatorTest` — cloud positions, cloud hit testing, connected endpoint hit testing
- `SimulationRunnerTest` — column names, row count, initial values, variable capture, error handling
- `UndoManagerTest` — undo/redo lifecycle, capacity limits, clear
- No tests for `ModelCanvas`, `InlineEditor` (JavaFX dependency)

---

## Not Yet Implemented

### Features
- Context toolbar near selection
- Functional resize handles
- Hover highlighting / feedback loop highlighting
- Simulation results charting/graphing
- Module drill-down / binding configuration in UI
