package systems.courant.sd.app.canvas.controllers;

import systems.courant.sd.model.def.ElementType;

import javafx.scene.Cursor;
import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.LayoutMetrics;
import systems.courant.sd.app.canvas.ResizeHandle;
import systems.courant.sd.app.canvas.renderers.SelectionRenderer;

/**
 * Manages element resize interactions on the canvas.
 * Tracks the resize target, anchor corner, and applies size/position
 * changes during the drag.
 */
public final class ResizeController {

    private boolean active;
    private String target;
    private ResizeHandle handle;
    private double anchorX;
    private double anchorY;
    private double lastSignX = 1;
    private double lastSignY = 1;
    private boolean undoSaved;

    public boolean isActive() {
        return active;
    }

    public ResizeHandle getHandle() {
        return handle;
    }

    public String getTarget() {
        return target;
    }

    /**
     * Begins a resize operation from the given handle hit.
     * The anchor is set to the corner opposite the grabbed handle.
     */
    public void start(ResizeHandle.HandleHit hit, CanvasState state) {
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
            case TOP_LEFT -> { anchorX = cx + halfW; anchorY = cy + halfH; lastSignX = -1; lastSignY = -1; }
            case TOP_RIGHT -> { anchorX = cx - halfW; anchorY = cy + halfH; lastSignX = 1; lastSignY = -1; }
            case BOTTOM_LEFT -> { anchorX = cx + halfW; anchorY = cy - halfH; lastSignX = -1; lastSignY = 1; }
            case BOTTOM_RIGHT -> { anchorX = cx - halfW; anchorY = cy - halfH; lastSignX = 1; lastSignY = 1; }
            default -> throw new IllegalArgumentException("Unknown handle: " + handle);
        }
    }

    /**
     * Updates element size and position during a resize drag.
     * Saves undo state on the first move. Enforces minimum sizes.
     */
    public void drag(double worldX, double worldY, CanvasState state, Runnable saveUndo) {
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

        double doublePad = 2 * pad;
        double rawW = Math.abs(worldX - anchorX) - doublePad;
        double rawH = Math.abs(worldY - anchorY) - doublePad;
        double newW = Math.max(minW, rawW);
        double newH = Math.max(minH, rawH);

        double signX = Math.signum(worldX - anchorX);
        double signY = Math.signum(worldY - anchorY);
        // When the cursor is exactly on the anchor axis, signum returns 0 which
        // would snap the element to the anchor — use the last non-zero direction.
        if (signX != 0) { lastSignX = signX; } else { signX = lastSignX; }
        if (signY != 0) { lastSignY = signY; } else { signY = lastSignY; }

        double newCx = anchorX + signX * (newW / 2 + pad);
        double newCy = anchorY + signY * (newH / 2 + pad);

        state.setSize(target, newW, newH);
        state.setPosition(target, newCx, newCy);
    }

    /**
     * Ends the resize operation normally.
     */
    public void end() {
        active = false;
        target = null;
        handle = null;
    }

    /**
     * Cancels the resize operation, reverting via undo if changes were made.
     */
    public void cancel(Runnable performUndo) {
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
    public static Cursor cursorFor(ResizeHandle handle) {
        return switch (handle) {
            case TOP_LEFT, BOTTOM_RIGHT -> Cursor.NW_RESIZE;
            case TOP_RIGHT, BOTTOM_LEFT -> Cursor.NE_RESIZE;
        };
    }
}
