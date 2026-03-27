package systems.courant.sd.app.canvas.renderers;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.LayoutMetrics;
import systems.courant.sd.app.canvas.ModelEditor;
import systems.courant.sd.model.expr.DelayDetector;

import java.util.List;

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
        List<String> subscripts = editor.getElementSubscripts(name);
        double size = LayoutMetrics.FLOW_INDICATOR_SIZE;
        Color customColor = canvasState.getColor(name).map(Color::web).orElse(null);
        ElementRenderer.drawFlow(gc, name, hasDelay, subscripts,
                cx - size / 2, cy - size / 2, size, size, customColor);
    }
}
