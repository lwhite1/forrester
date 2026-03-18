package systems.courant.sd.app.canvas.renderers;

import javafx.scene.canvas.GraphicsContext;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.graph.CausalTraceAnalysis;

/**
 * A discrete rendering phase within the canvas render pipeline.
 * Each pass draws one logical layer (connections, elements, overlays, etc.).
 */
@FunctionalInterface
interface RenderPass {

    void render(GraphicsContext gc, CanvasRenderer.RenderContext ctx);

    /**
     * Returns true if the named element is a hidden auxiliary variable.
     */
    static boolean isHiddenAux(String name, boolean hideVariables, CanvasState canvasState) {
        return hideVariables
                && canvasState.getType(name).orElse(null) == ElementType.AUX;
    }

    /**
     * Returns true if a connection should be dimmed based on hover or trace state.
     */
    static boolean shouldDimConnection(String fromName, String toName,
                                        String hoveredElement,
                                        CausalTraceAnalysis traceAnalysis) {
        if (traceAnalysis != null) {
            return !traceAnalysis.isTraceEdge(fromName, toName)
                    && !traceAnalysis.isTraceEdge(toName, fromName);
        }
        if (hoveredElement != null) {
            return !fromName.equals(hoveredElement) && !toName.equals(hoveredElement);
        }
        return false;
    }
}
