package systems.courant.sd.app.canvas.renderers;

import javafx.scene.canvas.GraphicsContext;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.ModelEditor;

/**
 * Strategy interface for rendering a specific element type onto the canvas.
 * Each element type (stock, flow, auxiliary, module, etc.) has its own implementation
 * that encapsulates both data extraction and drawing.
 */
public interface ElementTypeRenderer {

    /**
     * Renders the named element at the given center position.
     *
     * @param gc         the graphics context to draw on
     * @param name       element name
     * @param cx         center X in world coordinates
     * @param cy         center Y in world coordinates
     * @param canvasState canvas state for element dimensions
     * @param editor     model editor for element data
     * @param showDelay  whether to show delay badges
     */
    void render(GraphicsContext gc, String name, double cx, double cy,
                CanvasState canvasState, ModelEditor editor, boolean showDelay);
}
