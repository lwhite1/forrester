package systems.courant.sd.app.canvas.renderers;

import javafx.scene.canvas.GraphicsContext;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.FlowGeometry;
import systems.courant.sd.model.def.ConnectorRoute;
import systems.courant.sd.model.graph.CausalTraceAnalysis;

import java.util.List;

/**
 * Draws info link dashed arrows based on cached connector routes.
 * When hovering an element, dims links not connected to it.
 * When a trace is active, dims links not part of the trace.
 */
final class InfoLinkPass implements RenderPass {

    private final CanvasState canvasState;

    InfoLinkPass(CanvasState canvasState) {
        this.canvasState = canvasState;
    }

    @Override
    public void render(GraphicsContext gc, CanvasRenderer.RenderContext ctx) {
        if (ctx.hideInfoLinks()) {
            return;
        }

        List<ConnectorRoute> connectors = ctx.connectors();
        boolean hideAux = ctx.hideVariables();
        String hoveredElement = ctx.hoveredElement();
        CausalTraceAnalysis traceAnalysis = ctx.traceAnalysis();

        for (ConnectorRoute route : connectors) {
            String fromName = route.from();
            String toName = route.to();

            if (!canvasState.hasElement(fromName) || !canvasState.hasElement(toName)) {
                continue;
            }
            if (RenderPass.isHiddenAux(fromName, hideAux, canvasState)
                    || RenderPass.isHiddenAux(toName, hideAux, canvasState)) {
                continue;
            }

            double fromX = canvasState.getX(fromName);
            double fromY = canvasState.getY(fromName);
            double toX = canvasState.getX(toName);
            double toY = canvasState.getY(toName);

            FlowGeometry.Point2D clippedFrom = FlowGeometry.clipToElement(
                    canvasState, fromName, toX, toY);
            FlowGeometry.Point2D clippedTo = FlowGeometry.clipToElement(
                    canvasState, toName, fromX, fromY);

            boolean dim = RenderPass.shouldDimConnection(
                    fromName, toName, hoveredElement, traceAnalysis);
            if (dim) {
                gc.save();
                gc.setGlobalAlpha(0.15);
            }
            ConnectionRenderer.drawInfoLink(gc, clippedFrom.x(), clippedFrom.y(),
                    clippedTo.x(), clippedTo.y());
            if (dim) {
                gc.restore();
            }
        }
    }
}
