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

## Element Manipulation

| Action | Input | Notes |
|--------|-------|-------|
| Move element | Left-drag on selected element | Drag to reposition; connections follow automatically |
| Move multiple | Select multiple, then drag any selected element | All selected elements move together, maintaining relative positions |

## Window

| Action | Input | Notes |
|--------|-------|-------|
| Resize | Drag window border | Canvas redraws to fill available space |

## Not Yet Implemented

- Rubber-band (marquee) selection
- Context toolbar near selection
- Drag from port to create flow/connector
- Double-click inline equation editing
- Functional resize handles
- Hover highlighting / feedback loop highlighting
- Cursor shape changes (hand for pan, move for drag, etc.)
- Keyboard shortcuts (Delete to remove, Ctrl+A to select all, etc.)
- Undo/redo
