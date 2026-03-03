# UI Behaviors Reference

Catalog of all interactive behaviors in the Forrester canvas application.
This document serves as the source of truth for user-facing documentation and help text.

## Application Window

- **Initial size:** 1200x800 pixels
- **Title bar:** "Forrester — [filename]" or "Forrester — Untitled" when no file is loaded
- **Layout:** Menu bar and toolbar at top, canvas in center, status bar at bottom

### Status Bar

Displayed at the bottom of the window with a light gray background (#E8EAED) and top border (#BDC3C7). Shows four sections:

| Section | Content | Example |
|---------|---------|---------|
| Active tool | Current tool name | "Select", "Place Stock", "Place Flow", "Place Auxiliary", "Place Constant" |
| Selection count | Number of selected elements | "No selection", "1 selected", "3 selected" |
| Element count | Breakdown of all elements | "5 elements (2 stocks, 1 flows, 1 aux, 1 const)" or "Empty model" |
| Zoom level | Current zoom as percentage | "100%", "150%" |

All sections update in real time as the user interacts with the canvas.

## Menu Bar

### File Menu

| Item | Accelerator | Behavior |
|------|-------------|----------|
| New | Ctrl+N | Replaces current model with an empty "Untitled" model. Clears undo history. |
| Open... | Ctrl+O | Opens a file chooser (*.json filter). Loads the model definition and its saved view. If the file has no saved view, applies automatic layout. Clears undo history. |
| Save | Ctrl+S | Saves to the current file path. If no file has been set yet, behaves like Save As. |
| Save As... | Ctrl+Shift+S | Opens a save file chooser. Remembers the directory and filename of the current file. |
| Close | Ctrl+W | Creates a new empty model (same as New). |
| Exit | — | Closes the application window. |

### Edit Menu

| Item | Accelerator | Behavior |
|------|-------------|----------|
| Undo | Ctrl+Z | Restores the previous model and view snapshot. Disabled when undo stack is empty. |
| Redo | Ctrl+Shift+Z | Re-applies the last undone snapshot. Disabled when redo stack is empty. |
| Select All | Ctrl+A | Selects all elements on the canvas. |

### Simulate Menu

| Item | Accelerator | Behavior |
|------|-------------|----------|
| Simulation Settings... | — | Opens a modal dialog to configure time step, duration, and duration unit. |
| Run Simulation | Ctrl+R | Runs the simulation in a background thread. If no settings have been configured, prompts for them first. Shows results dialog on completion, or error alert on failure. |

## Canvas Navigation

| Action | Input | Notes |
|--------|-------|-------|
| Zoom in/out | Scroll wheel / trackpad two-finger scroll | Zooms toward cursor position. Scale range: 10%–500% |
| Zoom in | Ctrl+Plus or Ctrl+Equals | Zooms in at canvas center |
| Zoom out | Ctrl+Minus | Zooms out at canvas center |
| Reset zoom | Ctrl+0 | Resets zoom to 100% and clears pan offset |
| Pan | Space + left-drag | Hold spacebar, then click and drag to pan the canvas |
| Pan (alt) | Middle-drag or right-drag | For users with a multi-button mouse |

## Element Selection

| Action | Input | Notes |
|--------|-------|-------|
| Select element | Left-click on element | Clears previous selection, selects clicked element |
| Add/remove from selection | Shift + left-click on element | Toggles the clicked element without affecting others |
| Marquee select | Left-drag on empty canvas (in Select mode) | Draws a selection rectangle; all elements whose center falls inside are selected |
| Marquee add to selection | Shift + left-drag on empty canvas | Adds elements inside the marquee to the existing selection |
| Cancel marquee | Escape (while marquee is active) | Cancels the marquee and restores the previous selection |
| Select all | Ctrl+A | Selects all elements on the canvas. Also available via Edit > Select All |
| Clear selection | Left-click on empty canvas (no drag) | Clears all selected elements (Shift+click preserves selection) |
| Clear selection (keyboard) | Escape (when on Select tool with no pending operation) | Deselects all elements |

### Marquee Visual Feedback

- Semi-transparent blue fill (#4A90D9 at 10% opacity) with dashed blue border (#4A90D9 at 60% opacity, 6px dash / 3px gap, 1px width)
- Selection updates live as the marquee is resized
- Cursor shows crosshair during marquee drag
- On mouse release the marquee rectangle disappears and the selection is finalized

### Selection Visual Feedback

- Selected elements show a **dashed blue outline** (#4A90D9 at 80% opacity) with 4px padding
- Small solid square **corner handles** appear at each corner of the selection outline
- Stocks, auxiliaries, and constants get a rectangular indicator
- Flow indicators get a diamond-shaped indicator matching their shape

## Element Creation

| Action | Input | Notes |
|--------|-------|-------|
| Enter placement mode | Click a toolbar button (Stock, Flow, Auxiliary, Constant), or press 2–5 | Button stays toggled; cursor is ready to place elements |
| Place stock/aux/constant | Left-click on empty canvas (in placement mode) | Creates element at click position with auto-generated name |
| Exit placement mode | Press Escape, or press 1, or click the Select button | Returns to select/drag mode |

- Elements are auto-named with incrementing numbers: "Stock 1", "Stock 2", "Flow 3", etc.
- After placement, the new element is automatically selected
- Connectors (info links) are regenerated after each creation

### Flow Connection (Two-Click Protocol)

| Action | Input | Notes |
|--------|-------|-------|
| Start flow (from stock) | Click Flow button, then click a stock | Sets the stock as the flow source |
| Start flow (from cloud) | Click Flow button, then click empty space | Sets a cloud as the flow source |
| Complete flow (to stock) | Second click on a stock | Creates flow connected source > sink; diamond placed at midpoint |
| Complete flow (to cloud) | Second click on empty space | Creates flow with cloud sink; diamond placed at midpoint |
| Cancel pending flow | Press Escape during pending flow | Discards the pending flow, no element created |

- A **rubber-band line** (blue dashed, #4A90D9 at 60% opacity, 2px width) follows the cursor from the source to the current mouse position during the pending state
- When hovering over a stock during pending flow, a **blue dashed highlight** rectangle appears around the stock (6px dash/3px gap, 4px padding)
- If the source is a cloud, a cloud symbol is drawn at the source position
- Switching tools while a flow is pending cancels the pending flow
- **Self-loops are prevented** — source and sink cannot be the same stock
- **Cloud-to-cloud flows are prevented** — at least one end must be a stock
- Flow creation only snaps to stocks (not auxiliaries, constants, or other flows)

### Flow Endpoint Reattachment

| Action | Input | Notes |
|--------|-------|-------|
| Start reattachment | Left-drag on a cloud endpoint or connected stock endpoint of a flow (in Select mode) | Begins rubber-band drag from the flow diamond |
| Complete reattachment (to stock) | Release on a stock | Reconnects the flow endpoint to that stock |
| Complete reattachment (to cloud) | Release on empty space | Disconnects the endpoint to a cloud |
| Cancel reattachment | Press Escape | Returns to previous state without changes |

- Cloud hit radius: 18px (world space); connected endpoint hit radius: 14px
- Clouds appear at 80px offset from the flow diamond center, in the direction away from the opposite connected stock
- A rubber-band line (same blue dashed style) connects the diamond center to the current mouse position during the drag
- When hovering over a stock during reattachment, the stock gets the same blue dashed highlight as during flow creation

## Element Deletion

| Action | Input | Notes |
|--------|-------|-------|
| Delete selected | Delete or Backspace key | Removes all selected elements from the model |

- If a deleted stock is referenced as a flow's source or sink, that connection becomes a cloud
- Formula references to deleted elements become invalid (user must fix manually)
- No cascade deletion — only the selected elements are removed
- Connectors (info links) are regenerated after deletion

## Inline Editing

| Action | Input | Notes |
|--------|-------|-------|
| Edit element | Double-click on any element (in Select mode) | Opens a TextField overlay at the element position |
| Commit edit | Enter key or click away (focus loss) | Applies the new name/value/equation |
| Cancel edit | Escape key | Closes the editor without changes |

### Editing Chains by Element Type

| Element type | First editor | Second editor |
|-------------|-------------|---------------|
| Stock | Name | — |
| Flow | Name | Equation (positioned below diamond + name label) |
| Auxiliary | Name | Equation (positioned below name inside rectangle) |
| Constant | Name | Value (positioned below name; must be a valid number) |

- The name editor opens first; on commit, the second editor opens automatically (for types that have one)
- The current text is pre-selected for immediate overwriting
- Rename propagation: renaming an element updates flow source/sink references and equation tokens automatically
- Equation editors use a wider field (minimum 100px width)
- Invalid constant values (non-numeric) are silently ignored
- While the inline editor is open, all canvas keyboard shortcuts and mouse interactions are suppressed

## Element Manipulation

| Action | Input | Notes |
|--------|-------|-------|
| Move element | Left-drag on selected element | Drag to reposition; connections follow automatically |
| Move multiple | Select multiple, then drag any selected element | All selected elements move together, maintaining relative positions |

- The first drag movement saves an undo snapshot; subsequent movements in the same drag do not create additional snapshots

## Undo / Redo

- **Mechanism:** Snapshot-based — stores immutable pairs of (model definition + canvas view) on each mutation
- **Stack depth:** Maximum 100 undo levels; oldest entries are discarded when the limit is exceeded
- **Redo stack:** Cleared whenever a new mutation occurs (branching history discards the redo stack)
- **Undo/redo menu items** are enabled/disabled dynamically based on stack state
- **History is cleared** when a new file is opened or a new model is created

Operations that save undo state:
- Element creation (stock, flow, auxiliary, constant)
- Element deletion
- Element drag/move (one snapshot per drag, not per pixel)
- Inline name edits (rename)
- Inline value edits (constant value change)
- Inline equation edits (flow/auxiliary equation change)
- Flow endpoint reattachment

## Simulation

### Settings Dialog

A modal dialog with three fields:

| Field | Control | Default | Options |
|-------|---------|---------|---------|
| Time Step | ComboBox | Day | Day, Week, Month, Year, Hour, Minute, Second |
| Duration | TextField | 100 | Any positive number (integer or decimal) |
| Duration Unit | ComboBox | Day | Day, Week, Month, Year, Hour, Minute, Second |

- OK button is disabled if the duration field is empty or contains a non-positive / non-numeric value
- If the model already has settings, the dialog shows the existing values

### Results Dialog

- Opens as a **separate, non-modal window** (800x500 pixels)
- Displays a **table** with one column per simulation variable, plus a "Step" column
- Values are formatted as integers when whole numbers, or with 4 decimal places otherwise
- User can scroll, resize columns, and close the window independently

## Element Visual Styling

All elements have a white (#FFFFFF) fill color.

### Stock
- **Shape:** Rounded rectangle (8px corner radius)
- **Border:** 3px solid line
- **Label:** Bold 13pt font, centered
- **Badge:** Unit text in 9pt gray, bottom-right corner

### Flow
- **Shape:** Diamond (rotated square, 30px size)
- **Border:** 1.5px solid line
- **Label:** 11pt font, below diamond (4px gap)
- **Equation:** 9pt gray font below label; hidden if null, blank, or "0"

### Auxiliary
- **Shape:** Rounded rectangle (6px corner radius)
- **Border:** 1.5px solid line
- **Badge:** "fx" in 9pt gray, top-left corner
- **Label:** 12pt font, slightly above center
- **Equation:** 9pt gray font below label; hidden if null, blank, or "0"

### Constant
- **Shape:** Rounded rectangle (4px corner radius)
- **Border:** 1px dashed line (6px dash / 4px gap)
- **Badge:** "pin" in 9pt gray, top-left corner
- **Label:** 11pt font, slightly above center
- **Value:** 9pt gray font below label; integers without decimal, floats as-is

### Connectors

- **Material flow lines:** Thick solid arrows with arrowheads, clipped to element borders
- **Info link lines:** Thin dashed arrows, clipped to element borders

### Rendering Order (bottom to top)

1. Background fill (#F8F9FA)
2. Material flow lines
3. Info link lines
4. Element shapes
5. Selection indicators
6. Pending flow rubber-band line
7. Reattachment rubber-band line
8. Marquee selection rectangle

## Cursor Feedback

The cursor changes shape to reflect the current interaction state.

| State | Cursor | Condition |
|-------|--------|-----------|
| Default / idle | Default arrow | No special state active |
| Hovering element (Select mode) | Open hand | Mouse over an element in Select tool mode |
| Hovering cloud or connected endpoint | Pointing hand | Mouse over a flow's cloud endpoint or connected stock endpoint |
| Dragging element | Closed hand | Element drag in progress |
| Marquee selection | Crosshair | Rubber-band selection drag in progress |
| Space held (pan ready) | Move (four-way arrow) | Spacebar held down, ready to pan |
| Panning | Closed hand | Pan drag in progress (Space+drag, middle-drag, or right-drag) |
| Placement mode | Crosshair | Any placement tool active (Stock, Flow, Auxiliary, Constant) |
| Flow pending (rubber-band) | Crosshair | Waiting for second click during flow creation |
| Reattaching endpoint | Closed hand | Dragging a flow endpoint to reattach |
| Inline editor active | No change (TextField shows I-beam within its own bounds) | Canvas cursor unchanged; cursor updates resume when editor closes |

Priority order (highest to lowest): reattaching / panning / dragging > marquee > space held > flow pending / placement mode > cloud/endpoint hover > element hover > default.

## Keyboard Shortcuts

| Key | Action |
|-----|--------|
| Delete / Backspace | Delete selected elements |
| Escape | Cancel marquee; cancel reattachment; cancel pending flow; reset tool to Select; clear selection; cancel inline edit (in priority order) |
| Space (hold) | Enable pan mode while held |
| Enter | Commit inline edit (when editor is open) |
| 1 | Switch to Select tool |
| 2 | Switch to Stock tool |
| 3 | Switch to Flow tool |
| 4 | Switch to Auxiliary tool |
| 5 | Switch to Constant tool |
| Ctrl+A | Select all elements |
| Ctrl+Z | Undo |
| Ctrl+Shift+Z | Redo |
| Ctrl+N | New model |
| Ctrl+O | Open file |
| Ctrl+S | Save |
| Ctrl+Shift+S | Save As |
| Ctrl+W | Close (new model) |
| Ctrl+R | Run simulation |
| Ctrl+Plus / Ctrl+Equals | Zoom in at canvas center |
| Ctrl+Minus | Zoom out at canvas center |
| Ctrl+0 | Reset zoom to 100% |

## Not Yet Implemented

- Context toolbar near selection
- Functional resize handles
- Hover highlighting / feedback loop highlighting
