package systems.courant.sd.app.canvas;

import javafx.scene.input.MouseButton;

/**
 * Manages pan state and logic for the canvas. Handles middle-drag,
 * right-drag, and Space+left-drag panning interactions.
 */
final class PanController {

    private boolean panning;
    private boolean panMoved;
    private boolean spaceDown;
    private double panStartX;
    private double panStartY;

    boolean shouldStartPan(MouseButton button) {
        return button == MouseButton.MIDDLE
                || button == MouseButton.SECONDARY
                || (button == MouseButton.PRIMARY && spaceDown);
    }

    void startPan(double screenX, double screenY) {
        panning = true;
        panMoved = false;
        panStartX = screenX;
        panStartY = screenY;
    }

    boolean handleDrag(double screenX, double screenY, Viewport viewport) {
        if (!panning) {
            return false;
        }
        panMoved = true;
        double dx = screenX - panStartX;
        double dy = screenY - panStartY;
        viewport.pan(dx, dy);
        panStartX = screenX;
        panStartY = screenY;
        return true;
    }

    void endPan() {
        panning = false;
        panMoved = false;
    }

    void onSpacePressed() {
        spaceDown = true;
    }

    void onSpaceReleased() {
        spaceDown = false;
    }

    boolean isPanning() {
        return panning;
    }

    boolean hasPanMoved() {
        return panMoved;
    }

    boolean isSpaceDown() {
        return spaceDown;
    }
}
