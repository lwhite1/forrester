# Visual Editor Guide

The `courant-app` module provides a JavaFX canvas-based visual editor for creating and editing stock-and-flow diagrams and causal loop diagrams interactively. Models can be built, edited, saved, and simulated entirely through the GUI.

## Core Interactions

- **Element creation** — toolbar or keyboard shortcuts (2–6) to place stocks, flows, variables, modules, and lookup tables; shortcuts 7–8 to place CLD variables and draw causal links; shortcut 1 to return to Select tool
- **Flow connections** — two-click protocol: click source (stock or cloud), rubber-band follows cursor with stock hover highlight, click sink to create flow at midpoint
- **Inline editing** — double-click any element to rename; constants chain name→value editing; flows and auxiliaries chain name→equation editing. Rename propagates to flow references and equation tokens
- **Flow reattachment** — drag cloud endpoints onto stocks to reconnect, or drag connected endpoints off stocks to disconnect to cloud
- **Resize** — drag corner handles on selected elements to resize stocks, auxiliaries, constants, and modules
- **Hover highlighting** — subtle outline on mouse-over gives immediate visual feedback about which element will be acted on
- **Selection** — click to select, Shift+click to toggle, rubber-band marquee to select multiple elements, Ctrl+A to select all
- **Pan & zoom** — Space+drag or middle/right-drag to pan; scroll wheel to zoom at cursor; Ctrl+Plus/Minus/0 for keyboard zoom
- **Undo/redo** — Ctrl+Z / Ctrl+Shift+Z with a 100-level snapshot stack
- **File persistence** — New, Open, Save, Save As (JSON format with full view layout)
- **Copy/paste** — multi-window support with automatic name remapping via shared clipboard

## Properties Panel

The right-side panel shows editable fields for the selected element:
- Element type, name, value, equation, unit, negative-value policy
- Context toolbar for rename, delete, drill-into, and bindings actions
- For selected causal links: polarity dropdown and natural language explanation of the relationship

## Visual Language

The editor renders the Layered Flow Diagram notation with distinct shapes for each element type:
- Rounded-rectangle stocks
- Diamond flow indicators with material flow arrows
- Rounded-rectangle auxiliaries
- Dashed-border constants
- Thick-bordered module containers with "mod" badge
- Plain text CLD variables (auto-sized to text width)
- Dashed info link connectors
- Cloud symbols for disconnected flow endpoints
- Curved causal links with color-coded polarity labels (green "+", red "−", gray "?")
- Feedback loop labels (R1, B2, etc.) at loop centroids

## Simulation

Ctrl+R compiles the model definition, runs on a background thread, and displays results in a sortable table window.

## Analysis Dialogs

The Simulate menu provides dedicated dialogs for:

- **Parameter sweep** — single-parameter sweep with start/end/step configuration
- **Multi-parameter sweep** — dynamic parameter rows, live combination count, validation
- **Monte Carlo** — distribution configuration per parameter, run count, Latin Hypercube Sampling toggle
- **Optimization** — algorithm selection (Nelder-Mead, BOBYQA, CMA-ES), parameter bounds, objective function

## Dashboard Panel

Tabbed result display:
- **Simulation results** — table + chart
- **Sweep results** — summary + time series
- **Multi-sweep results** — combination table + per-run charts with series toggles
- **Monte Carlo results** — percentile envelope charts
- **Optimization results** — best-run summary + chart

All panes support right-click CSV export.

## Additional Features

- **Example models** — File → Open Example provides more than 100 bundled models across 18 categories
- **Context-sensitive help** — F1 shows documentation for the current tool or selected element type
- **Activity log** — timestamped event log for model creation, file operations, simulation runs, analysis executions, and validation checks
- **Equation autocomplete** — element names and built-in functions
- **Lookup table editing** — inline chart preview with interpolation mode selection
- **Feedback loop highlighting** — detected loops highlighted with colored edges and participant outlines
- **SVG export** — export diagrams to SVG format
- **Status bar** — shows active tool, selection state, element counts, and zoom level

## Key Classes

| Class | Purpose |
|---|---|
| `CourantApp` | JavaFX entry point and window management |
| `ModelWindow` | Per-model window with menus, toolbar, editor, dashboard, and activity log |
| `ModelCanvas` | Event handling and editing orchestration |
| `ModelEditor` | Mutable model editing layer with name index |
| `CanvasRenderer` | Rendering coordinator (connections, elements, overlays) |
| `CanvasState` | Mutable positions, types, draw order, selection |
| `PropertiesPanel` | Right-side panel for viewing/editing element properties |
| `DashboardPanel` | Tabbed result display for simulation, sweep, Monte Carlo, optimization |
| `SimulationRunner` | Compile + run + capture simulation results |
| `UndoManager` | Snapshot-based undo/redo stack |
