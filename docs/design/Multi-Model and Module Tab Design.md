# Multi-Model and Module Tab Design

## Overview

This document describes the design for two related UI capabilities:

1. **Multiple models open simultaneously** via separate windows (IntelliJ-style)
2. **Multiple modules visible simultaneously** via tabs within a single model window

These are independent features that share a common motivation: system dynamics models grow complex enough that users need to reference or edit multiple contexts at once. The design separates the concerns cleanly — windows isolate models, tabs organize modules within a model.

## Current Architecture

### Single model, single view

The application today is a single-window, single-model design. All state in `ShrewdApp` is singular:

```java
private Stage stage;
private ModelCanvas canvas;
private ModelEditor editor;
private Path currentFile;
private final UndoManager undoManager = new UndoManager();
```

Opening or creating a model destroys the previous one. There are no tabs, no secondary content windows, and no MDI.

### Module drill-down

Module navigation uses a stack-based drill-down. Entering a module pushes the parent's `(ModelEditor, ViewDef, UndoManager, viewport state)` onto a `NavigationStack` and replaces the canvas contents with the child module's editor. A breadcrumb bar shows the current path. Navigating back pops the stack and writes the child's `ModelDefinition` snapshot back into the parent.

This means only one level of the model hierarchy is visible at a time. To check a parent variable while editing a module, the user must navigate back, look, and drill in again.

### No global singletons

There are no static singletons or application-scope registries on `ModelEditor`, `ModelDefinition`, or `ModelCanvas`. The single-model restriction is purely structural — instance fields in `ShrewdApp`. Multiple `ModelEditor` instances can coexist (the navigation stack already does this).

---

## Feature 1: Multiple Models via Separate Windows

### Design

Each open model gets its own top-level `Stage` with fully independent state. This mirrors how IntelliJ IDEA handles multiple projects — each project is a separate window with its own toolbars, editor, and lifecycle.

### Why separate windows, not tabs

- **Full isolation.** Each window is a complete copy of the current `ShrewdApp` architecture. No shared UI state, no re-wiring on switch, no risk of cross-model state leaking.
- **Simplest implementation.** The existing `ShrewdApp` code barely changes. The change is in *lifecycle management* (how windows are created and closed), not in the window's internal architecture.
- **Natural OS-level window management.** Users can tile models side by side, move them to different monitors, or Alt-Tab between them using familiar OS conventions.
- **Independent undo, save, simulation.** Each window has its own undo history, file path, and simulation state. No ambiguity about "which model does Ctrl+Z affect?"

### Implementation approach

Factor the window construction out of `ShrewdApp.start()` into a reusable method:

```java
public class ShrewdApp extends Application {

    private final List<ModelWindow> openWindows = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        openWindow(primaryStage, null);  // empty model
    }

    public ModelWindow openWindow(Stage stage, Path file) {
        ModelWindow window = new ModelWindow(stage, this);
        if (file != null) window.loadFile(file);
        openWindows.add(window);
        stage.setOnHidden(e -> openWindows.remove(window));
        return window;
    }
}
```

`ModelWindow` encapsulates everything that is currently in `ShrewdApp`'s instance fields — the `ModelCanvas`, `ModelEditor`, `UndoManager`, `Path currentFile`, menu bar, toolbar, status bar, and all event wiring. Each `ModelWindow` is fully self-contained.

### Window lifecycle

- **File > New Window** (or Ctrl+Shift+N): creates a new `Stage`, calls `openWindow()` with an empty model.
- **File > Open** when no model is loaded: opens in the current window. When a model is already loaded: opens in a new window. (Or always opens in a new window — user preference.)
- **File > Close Window**: closes the current `Stage`. Prompts to save if dirty. If it's the last window, exits the application (or leaves a blank window, matching IntelliJ behavior).
- **Window menu**: lists all open model windows by filename for quick switching.

### What changes in existing code

| Component | Change |
|---|---|
| `ShrewdApp` | Extract window construction into `ModelWindow` class. `ShrewdApp` becomes a thin Application lifecycle manager. |
| Menu bar | Add "New Window", "Close Window", "Window" menu with open model list. |
| File open/save | Move into `ModelWindow`. Each window tracks its own `currentFile` and dirty state. |
| Title bar | Each window shows its own filename: `"ModelName.forr — Shrewd"` or `"Untitled — Shrewd"`. |
| Everything else | Unchanged. `ModelCanvas`, `ModelEditor`, `UndoManager`, toolbar, status bar, properties panel — all remain exactly as they are, just owned by `ModelWindow` instead of `ShrewdApp`. |

---

## Feature 2: Module Tabs Within a Model

### Design

Within a single model window, a tab bar allows multiple modules (and the root model) to be open simultaneously. Each tab displays a different level of the model's module hierarchy on its own canvas.

### Phase 1: Read-only reference tabs

The first implementation supports **read-only reference tabs**. Only one tab is editable (the "active" tab); other tabs show a frozen snapshot for reference. This avoids the synchronization complexity of multiple live editors mutating the same model hierarchy.

#### User workflow

1. User is editing the root model. Double-clicks a module to drill in — this opens the module in a **new tab** (instead of replacing the canvas).
2. The module tab becomes the active (editable) tab. The root model tab remains visible but is now read-only, showing the state at the moment the module was opened.
3. User can click back to the root tab to reference variable names, check structure, or review bindings. The root tab is grayed or otherwise marked as read-only.
4. User can open additional module tabs. Sibling modules can be open for reference simultaneously.
5. Closing a module tab writes its changes back to the parent (same as today's `navigateBack()`). The parent tab refreshes to reflect the updated module definition.

#### Tab state

Each tab holds:

```java
record ModuleTab(
    Tab tab,
    ModelCanvas canvas,
    ModelEditor editor,
    UndoManager undoManager,
    String modulePath,     // e.g. "Root > Production > Labor"
    boolean editable       // only one tab is editable at a time
)
```

#### Tab bar behavior

- The tab label shows the module name (or "Root" / the model name for the top level).
- A breadcrumb path tooltip shows the full hierarchy path.
- The active (editable) tab is visually distinct — other tabs are dimmed or marked with a lock icon.
- Closing tabs follows right-to-left precedence: closing a child tab writes back to the parent and activates the parent tab. Closing the root tab is equivalent to closing the model.
- Tabs can be reordered by dragging.

#### What changes for Phase 1

| Component | Change |
|---|---|
| `ModelWindow` center area | Replace `SplitPane(canvas, properties)` with `TabPane` where each tab contains its own `SplitPane(canvas, properties)`. |
| `ModelCanvas.drillInto()` | Instead of pushing to `NavigationStack` and replacing the canvas, create a new `ModuleTab` and add it to the `TabPane`. |
| `NavigationStack` | Becomes per-tab or is replaced by the tab hierarchy itself. The parent-child relationship is tracked by which tab spawned which. |
| `BreadcrumbBar` | May become redundant for navigation (tabs replace it) but could remain as a read-only path indicator within each tab. |
| Toolbar / Properties panel | Either shared (re-wired on tab switch, disabled for read-only tabs) or per-tab. Shared is simpler. |
| Undo/Redo | Per-tab. Only functional on the active editable tab. |

### Phase 2: Live editing across module tabs (future)

Phase 2 upgrades all open tabs to be simultaneously editable with real-time synchronization.

#### Additional requirements for Phase 2

- **Change notification system.** `ModelEditor` needs to emit change events (element added, renamed, deleted, equation changed) so that sibling and parent tabs can react.
- **Incremental write-back.** Changes propagate from child to parent continuously, not just on tab close. When a module editor changes, its `toModelDefinition()` snapshot is written into the parent editor's `ModuleInstanceDef` automatically.
- **Conflict resolution.** If the parent tab renames a variable that a child module binds to, the child tab needs to update or flag the broken binding. Similarly, deleting a module in the parent while its tab is open needs graceful handling (close the orphaned tab with a warning).
- **Structural changes in parent scope.** Adding or removing ports from a module's interface while the module tab and parent tab are both live requires bidirectional updates.

These problems are tractable but non-trivial. Phase 1 avoids all of them by freezing non-active tabs.

---

## Feature 3: Cross-Model and Cross-Module Copy/Paste

### Cross-model copy/paste (with separate windows)

Since models live in separate windows, copy/paste uses the **system clipboard**. This works naturally across windows, even across separate application instances.

#### Clipboard format

Selected elements are serialized to a self-contained clipboard payload:

```java
record ClipboardPayload(
    List<StockDef> stocks,
    List<FlowDef> flows,
    List<AuxiliaryDef> auxiliaries,
    List<ConstantDef> constants,
    List<ModuleInstanceDef> modules,
    List<ViewElement> layout,        // canvas positions for pasted elements
    List<ConnectorDef> connectors    // internal connectors between selected elements
)
```

This is essentially a subset of `ModelDefinition` — only the selected elements and the connectors between them. It serializes to the same JSON format used for `.forr` files.

#### Paste behavior

- Pasted elements are placed at the cursor position (or offset from their original positions if no cursor target).
- **Name conflicts** are resolved by appending a numeric suffix: `Stock 1` becomes `Stock 1 (2)`.
- **Dangling references** — if a pasted flow's equation references a variable that doesn't exist in the target model, the reference is preserved as-is but flagged (the equation will show an error until the user resolves it). This is preferable to silently dropping references.
- **Connectors** between pasted elements are included. Connectors to elements outside the selection are dropped (they reference variables that may not exist in the target model).

### Cross-module copy/paste (future, with live editing)

Paste into a different module within the same model raises the same dangling-reference problem. A pasted element's equation may reference variables from its source module that don't exist in the target module. Resolution options:

1. **Paste with warnings.** Same as cross-model — paste the elements, flag broken references, let the user fix equations manually.
2. **Offer to create bindings.** If the target module has a `moduleInterface`, offer to create input port bindings for the missing variables. This is more helpful but more complex to implement.

Option 1 is sufficient for initial implementation. Option 2 is a natural enhancement alongside Phase 2 live editing.

---

## Implementation Order

1. **Separate windows for multiple models.** Extract `ModelWindow` from `ShrewdApp`. Low risk, no architectural change to the canvas or editor layer.
2. **Cross-model copy/paste.** Define the clipboard format, implement serialize/deserialize, handle name conflicts and dangling references on paste.
3. **Read-only module tabs (Phase 1).** Replace drill-down navigation with tab creation. Read-only reference tabs for non-active modules.
4. **Live module tabs (Phase 2).** Add change notification to `ModelEditor`, implement incremental write-back, handle structural conflicts.
5. **Cross-module copy/paste.** Paste with warnings initially, binding-aware paste as an enhancement.

---

## Relationship to Design 4 Spec

The Design 4 Spec describes a single-window application with a conversation panel, canvas, and behavior dashboard. This design is additive:

- **Separate windows** do not conflict with Design 4. Each window is a complete Design 4 interface for one model.
- **Module tabs** extend the canvas area of Design 4. The conversation panel and behavior dashboard remain per-window (shared across module tabs within that window), which is correct — the conversation is about the model, not a specific module.
- **The maturity system** (Design 4, section 4) operates on the root `ModelDefinition`. With module tabs, maturity signals should still be computed on the full model (including all modules), not per-tab. A module tab might display a subset of relevant signals (e.g., structural completeness of just that module) but the authoritative maturity assessment is model-wide.
