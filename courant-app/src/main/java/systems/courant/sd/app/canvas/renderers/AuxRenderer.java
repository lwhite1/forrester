package systems.courant.sd.app.canvas.renderers;

import javafx.scene.canvas.GraphicsContext;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.LayoutMetrics;
import systems.courant.sd.app.canvas.ModelEditor;
import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.expr.DelayDetector;

/**
 * Renders auxiliary/constant elements: rounded rectangle with badge, name, and
 * optional delay indicator.
 */
final class AuxRenderer implements ElementTypeRenderer {

    @Override
    public void render(GraphicsContext gc, String name, double cx, double cy,
                       CanvasState canvasState, ModelEditor editor, boolean showDelay) {
        double w = LayoutMetrics.effectiveWidth(canvasState, name);
        double h = LayoutMetrics.effectiveHeight(canvasState, name);
        boolean isLiteral = editor.getVariableByName(name)
                .map(VariableDef::isLiteral).orElse(false);
        String equation = editor.getVariableEquation(name).orElse(null);
        boolean hasDelay = showDelay
                && DelayDetector.equationContainsDelay(equation);
        ElementRenderer.drawAux(gc, name, isLiteral, equation, hasDelay,
                cx - w / 2, cy - h / 2, w, h);
    }
}
