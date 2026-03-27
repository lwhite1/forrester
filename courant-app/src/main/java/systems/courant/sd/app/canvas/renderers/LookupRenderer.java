package systems.courant.sd.app.canvas.renderers;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.LayoutMetrics;
import systems.courant.sd.app.canvas.ModelEditor;

/**
 * Renders lookup table elements: subtle-fill rectangle with "Table" badge and name.
 */
final class LookupRenderer implements ElementTypeRenderer {

    @Override
    public void render(GraphicsContext gc, String name, double cx, double cy,
                       CanvasState canvasState, ModelEditor editor, boolean showDelay) {
        double w = LayoutMetrics.effectiveWidth(canvasState, name);
        double h = LayoutMetrics.effectiveHeight(canvasState, name);
        Color customColor = canvasState.getColor(name).map(Color::web).orElse(null);
        ElementRenderer.drawLookup(gc, name, cx - w / 2, cy - h / 2, w, h, false, customColor);
    }
}
