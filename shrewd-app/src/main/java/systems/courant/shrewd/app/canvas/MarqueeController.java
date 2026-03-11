package systems.courant.shrewd.app.canvas;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Manages rubber-band (marquee) selection on the canvas.
 * Tracks the marquee rectangle in world coordinates and updates
 * the selection based on elements inside the rectangle.
 */
final class MarqueeController {

    private boolean active;
    private double startWorldX;
    private double startWorldY;
    private double endWorldX;
    private double endWorldY;
    private Set<String> initialSelection;

    boolean isActive() {
        return active;
    }

    /**
     * Returns the renderer state for the current marquee.
     */
    CanvasRenderer.MarqueeState toRenderState() {
        if (!active) {
            return CanvasRenderer.MarqueeState.IDLE;
        }
        return new CanvasRenderer.MarqueeState(true,
                startWorldX, startWorldY, endWorldX, endWorldY);
    }

    /**
     * Begins a marquee selection from the given world coordinates.
     * If Shift is held, preserves the existing selection as a base.
     */
    void start(double worldX, double worldY, CanvasState state, boolean shiftDown) {
        active = true;
        startWorldX = worldX;
        startWorldY = worldY;
        endWorldX = worldX;
        endWorldY = worldY;
        initialSelection = new LinkedHashSet<>(state.getSelection());
        if (!shiftDown) {
            state.clearSelection();
        }
    }

    /**
     * Updates the marquee rectangle endpoint and refreshes the selection.
     */
    void drag(double worldX, double worldY, CanvasState state) {
        endWorldX = worldX;
        endWorldY = worldY;
        updateSelection(state);
    }

    /**
     * Ends the marquee selection, keeping the current selection.
     */
    void end() {
        active = false;
        initialSelection = null;
    }

    /**
     * Cancels the marquee selection, restoring the pre-marquee selection state.
     */
    void cancel(CanvasState state) {
        state.clearSelection();
        if (initialSelection != null) {
            for (String name : initialSelection) {
                state.addToSelection(name);
            }
        }
        active = false;
        initialSelection = null;
    }

    /**
     * Updates the selection based on elements inside the marquee rectangle.
     * Restores initial selection first (for Shift+marquee behavior).
     */
    private void updateSelection(CanvasState state) {
        double minX = Math.min(startWorldX, endWorldX);
        double maxX = Math.max(startWorldX, endWorldX);
        double minY = Math.min(startWorldY, endWorldY);
        double maxY = Math.max(startWorldY, endWorldY);

        state.clearSelection();
        if (initialSelection != null) {
            for (String name : initialSelection) {
                state.addToSelection(name);
            }
        }
        for (String name : state.getDrawOrder()) {
            double cx = state.getX(name);
            double cy = state.getY(name);
            if (cx >= minX && cx <= maxX && cy >= minY && cy <= maxY) {
                state.addToSelection(name);
            }
        }
    }
}
