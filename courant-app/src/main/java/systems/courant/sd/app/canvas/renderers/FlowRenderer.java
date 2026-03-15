package systems.courant.sd.app.canvas.renderers;

import javafx.scene.canvas.GraphicsContext;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.LayoutMetrics;
import systems.courant.sd.app.canvas.ModelEditor;
import systems.courant.sd.model.expr.DelayDetector;

/**
 * Renders flow elements: diamond indicator with name label and optional delay badge.
 */
final class FlowRenderer implements ElementTypeRenderer {

    @Override
    public void render(GraphicsContext gc, String name, double cx, double cy,
                       CanvasState canvasState, ModelEditor editor, boolean showDelay) {
        boolean hasDelay = showDelay
                && editor.getFlowEquation(name)
                        .map(DelayDetector::equationContainsDelay).orElse(false);
        double size = LayoutMetrics.FLOW_INDICATOR_SIZE;
        ElementRenderer.drawFlow(gc, name, hasDelay,
                cx - size / 2, cy - size / 2, size, size);
    }
}
