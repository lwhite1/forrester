package systems.courant.sd.app.canvas.renderers;

import javafx.scene.canvas.GraphicsContext;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.LayoutMetrics;
import systems.courant.sd.app.canvas.ModelEditor;

/**
 * Renders CLD (Causal Loop Diagram) variables: plain text label without a border,
 * following standard CLD notation.
 */
final class CldVariableRenderer implements ElementTypeRenderer {

    @Override
    public void render(GraphicsContext gc, String name, double cx, double cy,
                       CanvasState canvasState, ModelEditor editor, boolean showDelay) {
        double w = LayoutMetrics.effectiveWidth(canvasState, name);
        double h = LayoutMetrics.effectiveHeight(canvasState, name);
        ElementRenderer.drawCldVariable(gc, name, cx - w / 2, cy - h / 2, w, h);
    }
}
