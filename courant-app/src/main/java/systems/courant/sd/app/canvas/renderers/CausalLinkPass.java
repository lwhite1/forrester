package systems.courant.sd.app.canvas.renderers;

import javafx.scene.canvas.GraphicsContext;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.CausalLinkGeometry;
import systems.courant.sd.app.canvas.FlowGeometry;
import systems.courant.sd.app.canvas.LayoutMetrics;
import systems.courant.sd.app.canvas.ModelEditor;
import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.graph.CausalTraceAnalysis;

import java.util.List;

/**
 * Draws causal links between CLD variables (and potentially S&amp;F elements)
 * as curved arcs using quadratic Bézier curves.
 */
final class CausalLinkPass implements RenderPass {

    private final CanvasState canvasState;

    CausalLinkPass(CanvasState canvasState) {
        this.canvasState = canvasState;
    }

    @Override
    public void render(GraphicsContext gc, CanvasRenderer.RenderContext ctx) {
        ModelEditor editor = ctx.editor();
        boolean hideAux = ctx.hideVariables();
        String hoveredElement = ctx.hoveredElement();
        CausalTraceAnalysis traceAnalysis = ctx.traceAnalysis();
        List<CausalLinkDef> allLinks = editor.getCausalLinks();

        for (CausalLinkDef link : allLinks) {
            String fromName = link.from();
            String toName = link.to();

            if (!canvasState.hasElement(fromName) || !canvasState.hasElement(toName)) {
                continue;
            }
            if (RenderPass.isHiddenAux(fromName, hideAux, canvasState)
                    || RenderPass.isHiddenAux(toName, hideAux, canvasState)) {
                continue;
            }

            boolean dim = RenderPass.shouldDimConnection(
                    fromName, toName, hoveredElement, traceAnalysis);
            if (dim) {
                gc.save();
                gc.setGlobalAlpha(0.15);
            }

            double fromX = canvasState.getX(fromName);
            double fromY = canvasState.getY(fromName);

            // Self-loop: use cubic Bézier loop above the element
            if (fromName.equals(toName)) {
                double halfW = LayoutMetrics.effectiveWidth(canvasState, fromName) / 2;
                double halfH = LayoutMetrics.effectiveHeight(canvasState, fromName) / 2;
                double[] loopPts = CausalLinkGeometry.selfLoopPoints(fromX, fromY, halfW, halfH);
                ConnectionRenderer.drawCausalLinkSelfLoop(gc, loopPts, link.polarity());
                if (dim) {
                    gc.restore();
                }
                continue;
            }

            double toX = canvasState.getX(toName);
            double toY = canvasState.getY(toName);

            // Compute control point for the curve
            CausalLinkGeometry.ControlPoint cp = CausalLinkGeometry.controlPoint(
                    fromX, fromY, toX, toY, fromName, toName, allLinks);

            // Clip endpoints to element borders, aiming at the control point
            // for a more natural exit angle from the element
            FlowGeometry.Point2D clippedFrom = FlowGeometry.clipToElement(
                    canvasState, fromName, cp.x(), cp.y());
            FlowGeometry.Point2D clippedTo = FlowGeometry.clipToElement(
                    canvasState, toName, cp.x(), cp.y());

            ConnectionRenderer.drawCausalLink(gc, clippedFrom.x(), clippedFrom.y(),
                    clippedTo.x(), clippedTo.y(), cp, link.polarity());
            if (dim) {
                gc.restore();
            }
        }
    }
}
