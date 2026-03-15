package systems.courant.sd.app.canvas.renderers;

import javafx.scene.canvas.GraphicsContext;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.LayoutMetrics;
import systems.courant.sd.app.canvas.ModelEditor;

/**
 * Renders stock elements: heavy bordered rectangle with name and unit badge.
 */
final class StockRenderer implements ElementTypeRenderer {

    @Override
    public void render(GraphicsContext gc, String name, double cx, double cy,
                       CanvasState canvasState, ModelEditor editor, boolean showDelay) {
        double w = LayoutMetrics.effectiveWidth(canvasState, name);
        double h = LayoutMetrics.effectiveHeight(canvasState, name);
        String unit = editor.getStockUnit(name).orElse(null);
        ElementRenderer.drawStock(gc, name, unit, cx - w / 2, cy - h / 2, w, h);
    }
}
