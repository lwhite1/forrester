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

## Current State

### Source files (forrester-app)

```
src/main/java/com/deathrayresearch/forrester/app/
├── ForresterApp.java              — JavaFX entry point
├── Launcher.java                  — Main class
└── canvas/
    ├── CanvasRenderer.java        — Rendering coordinator
    ├── CanvasState.java           — Mutable positions, types, draw order, selection
    ├── CanvasToolBar.java         — Tool toggle buttons
    ├── ColorPalette.java          — Color constants
    ├── ConnectionRenderer.java    — Material flows and info links
    ├── ElementRenderer.java       — Stock, flow, aux, constant shapes
    ├── FlowCreationController.java — Two-click flow state machine
    ├── FlowEndpointCalculator.java — Cloud positions and endpoint hit testing
    ├── HitTester.java             — Click → element resolution
    ├── InlineEditor.java          — TextField overlay for inline editing
    ├── LayoutMetrics.java         — Dimensions, fonts
    ├── ModelCanvas.java           — Event handling + editing orchestration (~560 lines)
    ├── ModelEditor.java           — Mutable model editing layer
    ├── SelectionRenderer.java     — Selection indicators
    └── Viewport.java              — Pan/zoom transforms
```

### Test coverage

- 116 tests, all passing
- `ViewportTest` — coordinate transforms, zoom, pan (11 tests)
- `HitTesterTest` — rect and diamond hit testing
- `CanvasStateTest` — load, position, selection, add/remove/rename
- `ModelEditorTest` — load, add, remove, rename, setConstantValue, reconnectFlow, round-trip
- `FlowCreationControllerTest` — two-click state machine (8 tests)
- `FlowEndpointCalculatorTest` — cloud positions, cloud hit testing, connected endpoint hit testing (10 tests)
- No tests for `ModelCanvas`, `InlineEditor`, renderers (JavaFX dependency)

---

## Not Yet Implemented

### Features
- Full equation editor (currently only name/value editing)
- Rubber-band (marquee) selection
- Context toolbar near selection
- Functional resize handles
- Hover highlighting / feedback loop highlighting
- Cursor shape changes (hand for pan, move for drag, etc.)
- Additional keyboard shortcuts (Ctrl+A to select all, etc.)
- Undo/redo
- Model save/load to disk

### Known issues (from Phase 5 audit)

**Major:**
- Duplicate `CLOUD_OFFSET` constant in `ConnectionRenderer` and `FlowEndpointCalculator`
- Cloud position computed differently in renderer vs hit-tester (clipped edge vs center direction)
- Reattachment can create self-loop (source==sink) or no-op reconnection

**Minor:**
- Inline editor TextField width not scaled by zoom
- Constant value editor Y offset ignores zoom
- `reconnectFlow` does not validate stockName exists
- Self-loop flows (same stock as source/sink) place diamond on top of stock
- No name validation for identifiers
- No user feedback when rename is rejected
- Equation references to deleted elements not cleaned up
- No cloud preview when dropping reattachment on empty space
- No undo support

See `doc/UI Audit — Phase 5.md` for the full list with file locations and suggested fixes.
