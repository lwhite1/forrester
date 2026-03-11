package systems.courant.forrester.app.canvas;

import javafx.scene.canvas.GraphicsContext;

/**
 * Encapsulates pan offset and zoom scale for the canvas, providing
 * world-to-screen and screen-to-world coordinate transforms.
 */
public class Viewport {

    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 5.0;

    private double translateX;
    private double translateY;
    private double scale;

    public Viewport() {
        this.translateX = 0;
        this.translateY = 0;
        this.scale = 1.0;
    }

    /**
     * Converts a screen X coordinate to world X coordinate.
     */
    public double toWorldX(double screenX) {
        return (screenX - translateX) / scale;
    }

    /**
     * Converts a screen Y coordinate to world Y coordinate.
     */
    public double toWorldY(double screenY) {
        return (screenY - translateY) / scale;
    }

    /**
     * Converts a world X coordinate to screen X coordinate.
     */
    public double toScreenX(double worldX) {
        return worldX * scale + translateX;
    }

    /**
     * Converts a world Y coordinate to screen Y coordinate.
     */
    public double toScreenY(double worldY) {
        return worldY * scale + translateY;
    }

    /**
     * Pans the viewport by the given screen-space delta.
     */
    public void pan(double deltaScreenX, double deltaScreenY) {
        this.translateX += deltaScreenX;
        this.translateY += deltaScreenY;
    }

    /**
     * Zooms at the given screen-space pivot point by the given factor.
     * The world point under the pivot stays fixed on screen.
     * Scale is clamped to [0.1, 5.0].
     */
    public void zoomAt(double screenX, double screenY, double factor) {
        double worldX = toWorldX(screenX);
        double worldY = toWorldY(screenY);

        double newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, this.scale * factor));
        this.scale = newScale;

        // Recompute translate so that (worldX, worldY) maps back to (screenX, screenY)
        this.translateX = screenX - worldX * newScale;
        this.translateY = screenY - worldY * newScale;
    }

    /**
     * Applies the viewport transform to the given GraphicsContext.
     * Call between gc.save() and gc.restore().
     */
    public void applyTo(GraphicsContext gc) {
        gc.translate(translateX, translateY);
        gc.scale(scale, scale);
    }

    /**
     * Resets the viewport to identity transform (no pan, no zoom).
     */
    public void reset() {
        this.scale = 1.0;
        this.translateX = 0;
        this.translateY = 0;
    }

    /**
     * Restores the viewport to a previously saved state.
     */
    public void restoreState(double translateX, double translateY, double scale) {
        this.translateX = translateX;
        this.translateY = translateY;
        this.scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }

    public double getTranslateX() {
        return translateX;
    }

    public double getTranslateY() {
        return translateY;
    }

    public double getScale() {
        return scale;
    }
}
