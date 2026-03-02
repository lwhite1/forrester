# UI Behaviors Reference

Catalog of all interactive behaviors in the Forrester canvas application.
This document serves as the source of truth for user-facing documentation and help text.

## Canvas Navigation

| Action | Input | Notes |
|--------|-------|-------|
| Zoom in/out | Scroll wheel / trackpad two-finger scroll | Zooms toward cursor position. Scale range: 10%–500% |
| Pan | Space + left-drag | Hold spacebar, then click and drag to pan the canvas |
| Pan (alt) | Middle-drag or right-drag | For users with a multi-button mouse |

## Element Selection

| Action | Input | Notes |
|--------|-------|-------|
| Select element | Left-click on element | Clears previous selection, selects clicked element |
| Add/remove from selection | Shift + left-click on element | Toggles the clicked element without affecting others |
| Clear selection | Left-click on empty canvas | Clears all selected elements |

### Selection Visual Feedback

- Selected elements show a **dashed blue outline** (#4A90D9 at 80% opacity) with 4px padding
- Small solid square **corner handles** appear at each corner of the selection outline
- Stocks, auxiliaries, and constants get a rectangular indicator
- Flow indicators get a diamond-shaped indicator matching their shape

## Element Creation

| Action | Input | Notes |
|--------|-------|-------|
| Enter placement mode | Click a toolbar button (Stock, Flow, Auxiliary, Constant) | Button stays toggled; cursor is ready to place elements |
| Place element | Left-click on empty canvas (in placement mode) | Creates element at click position with auto-generated name |
| Exit placement mode | Press Escape, or click the Select button | Returns to select/drag mode |

- Elements are auto-named with incrementing numbers: "Stock 1", "Stock 2", "Flow 3", etc.
- Newly created flows start as cloud-to-cloud (no stock connections)
- After placement, the new element is automatically selected
- Connectors (info links) are regenerated after each creation

## Element Deletion

| Action | Input | Notes |
|--------|-------|-------|
| Delete selected | Delete or Backspace key | Removes all selected elements from the model |

- If a deleted stock is referenced as a flow's source or sink, that connection becomes a cloud (null)
- Formula references to deleted elements become invalid (user must fix manually)
- No cascade deletion — only the selected elements are removed
- Connectors (info links) are regenerated after deletion

## Element Manipulation

| Action | Input | Notes |
|--------|-------|-------|
| Move element | Left-drag on selected element | Drag to reposition; connections follow automatically |
| Move multiple | Select multiple, then drag any selected element | All selected elements move together, maintaining relative positions |

## Window

| Action | Input | Notes |
|--------|-------|-------|
| Resize | Drag window border | Canvas redraws to fill available space |

## Keyboard Shortcuts

| Key | Action |
|-----|--------|
| Delete / Backspace | Delete selected elements |
| Escape | Reset tool to Select mode |
| Space (hold) | Enable pan mode while held |

## Not Yet Implemented

- Port-based drag to connect flows to stocks
- Double-click inline equation editing
- Element renaming
- Rubber-band (marquee) selection
- Context toolbar near selection
- Functional resize handles
- Hover highlighting / feedback loop highlighting
- Cursor shape changes (hand for pan, move for drag, etc.)
- Additional keyboard shortcuts (Ctrl+A to select all, etc.)
- Undo/redo
- Model save/load to disk
