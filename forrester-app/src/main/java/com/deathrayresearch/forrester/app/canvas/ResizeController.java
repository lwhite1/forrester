package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ElementType;

import javafx.scene.Cursor;

/**
 * Manages element resize interactions on the canvas.
 * Tracks the resize target, anchor corner, and applies size/position
 * changes during the drag.
 */
final class ResizeController {

    private boolean active;
    private String target;
    private ResizeHandle handle;
    private double anchorX;
    private double anchorY;
    private boolean undoSaved;

    boolean isActive() {
        return active;
    }

    ResizeHandle getHandle() {
        return handle;
    }

    /**
     * Begins a resize operation from the given handle hit.
     * The anchor is set to the corner opposite the grabbed handle.
     */
    void start(ResizeHandle.HandleHit hit, CanvasState state) {
        active = true;
        target = hit.elementName();
        handle = hit.handle();
        undoSaved = false;

        double cx = state.getX(target);
        double cy = state.getY(target);
        double halfW = LayoutMetrics.effectiveWidth(state, target) / 2
                + SelectionRenderer.SELECTION_PADDING;
        double halfH = LayoutMetrics.effectiveHeight(state, target) / 2
                + SelectionRenderer.SELECTION_PADDING;

        switch (handle) {
            case TOP_LEFT -> { anchorX = cx + halfW; anchorY = cy + halfH; }
            case TOP_RIGHT -> { anchorX = cx - halfW; anchorY = cy + halfH; }
            case BOTTOM_LEFT -> { anchorX = cx + halfW; anchorY = cy - halfH; }
            case BOTTOM_RIGHT -> { anchorX = cx - halfW; anchorY = cy - halfH; }
        }
    }

    /**
     * Updates element size and position during a resize drag.
     * Saves undo state on the first move. Enforces minimum sizes.
     */
    void drag(double worldX, double worldY, CanvasState state, Runnable saveUndo) {
        if (!active) {
            return;
        }
        if (!undoSaved) {
            saveUndo.run();
            undoSaved = true;
        }

        ElementType type = state.getType(target).orElse(null);
        double minW = LayoutMetrics.minWidthFor(type);
        double minH = LayoutMetrics.minHeightFor(type);
        double pad = SelectionRenderer.SELECTION_PADDING;

        double rawW = Math.abs(worldX - anchorX) - pad;
        double rawH = Math.abs(worldY - anchorY) - pad;
        double newW = Math.max(minW, rawW);
        double newH = Math.max(minH, rawH);

        double edgeX = anchorX + Math.signum(worldX - anchorX) * (newW / 2 + pad);
        double edgeY = anchorY + Math.signum(worldY - anchorY) * (newH / 2 + pad);
        double newCx = (anchorX + edgeX) / 2;
        double newCy = (anchorY + edgeY) / 2;

        state.setSize(target, newW, newH);
        state.setPosition(target, newCx, newCy);
    }

    /**
     * Ends the resize operation normally.
     */
    void end() {
        active = false;
        target = null;
        handle = null;
    }

    /**
     * Cancels the resize operation, reverting via undo if changes were made.
     */
    void cancel(Runnable performUndo) {
        if (undoSaved) {
            performUndo.run();
        }
        active = false;
        target = null;
        handle = null;
    }

    /**
     * Returns the appropriate resize cursor for the given handle position.
     */
    static Cursor cursorFor(ResizeHandle handle) {
        return switch (handle) {
            case TOP_LEFT, BOTTOM_RIGHT -> Cursor.NW_RESIZE;
            case TOP_RIGHT, BOTTOM_LEFT -> Cursor.NE_RESIZE;
        };
    }
}
