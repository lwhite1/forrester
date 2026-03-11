package systems.courant.shrewd.app.canvas;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages element drag-to-move interactions on the canvas.
 * Tracks the drag target, start positions of all selected elements,
 * and applies world-space deltas during the drag.
 */
final class DragController {

    private boolean dragging;
    private String dragTarget;
    private boolean undoSaved;
    private double startScreenX;
    private double startScreenY;
    private final Map<String, CanvasState.Position> startPositions = new HashMap<>();

    boolean isDragging() {
        return dragging;
    }

    String getDragTarget() {
        return dragTarget;
    }

    /**
     * Begins a drag operation for the given target element.
     * Captures the starting positions of all currently selected elements.
     */
    void start(String target, double screenX, double screenY, CanvasState state) {
        dragging = true;
        dragTarget = target;
        undoSaved = false;
        startScreenX = screenX;
        startScreenY = screenY;
        startPositions.clear();
        for (String name : state.getSelection()) {
            startPositions.put(name,
                    new CanvasState.Position(state.getX(name), state.getY(name)));
        }
    }

    /**
     * Updates element positions during a drag, converting screen-space delta to world-space.
     * Saves undo state on the first move.
     */
    void drag(double screenX, double screenY, CanvasState state,
              Viewport viewport, Runnable saveUndo) {
        if (!dragging || dragTarget == null) {
            return;
        }
        if (!undoSaved) {
            saveUndo.run();
            undoSaved = true;
        }
        double worldDx = (screenX - startScreenX) / viewport.getScale();
        double worldDy = (screenY - startScreenY) / viewport.getScale();
        for (Map.Entry<String, CanvasState.Position> entry : startPositions.entrySet()) {
            CanvasState.Position startPos = entry.getValue();
            state.setPosition(entry.getKey(),
                    startPos.x() + worldDx, startPos.y() + worldDy);
        }
    }

    /**
     * Ends the drag operation and clears tracked state.
     */
    void end() {
        dragging = false;
        dragTarget = null;
        startPositions.clear();
    }
}
