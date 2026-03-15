package systems.courant.sd.app.canvas.renderers;

import javafx.scene.canvas.GraphicsContext;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.LayoutMetrics;
import systems.courant.sd.app.canvas.ModelEditor;

/**
 * Renders lookup table elements: dot-dash bordered rectangle with data point count.
 */
final class LookupRenderer implements ElementTypeRenderer {

    @Override
    public void render(GraphicsContext gc, String name, double cx, double cy,
                       CanvasState canvasState, ModelEditor editor, boolean showDelay) {
        double w = LayoutMetrics.effectiveWidth(canvasState, name);
        double h = LayoutMetrics.effectiveHeight(canvasState, name);
        int pts = editor.getLookupTableByName(name)
                .map(lt -> lt.xValues().length).orElse(0);
        ElementRenderer.drawLookup(gc, name, pts, cx - w / 2, cy - h / 2, w, h);
    }
}
