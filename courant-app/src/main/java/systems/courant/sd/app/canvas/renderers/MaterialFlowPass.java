package systems.courant.sd.app.canvas.renderers;

import javafx.scene.canvas.GraphicsContext;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.ColorPalette;
import systems.courant.sd.app.canvas.FlowEndpointCalculator;
import systems.courant.sd.app.canvas.FlowGeometry;
import systems.courant.sd.app.canvas.LayoutMetrics;
import systems.courant.sd.app.canvas.MaturityAnalysis;
import systems.courant.sd.app.canvas.ModelEditor;
import systems.courant.sd.model.def.FlowDef;

/**
 * Draws material flow arrows routed through the flow indicator (diamond).
 * Cloud positions are computed via {@link FlowEndpointCalculator#cloudPosition}
 * so that rendering and hit-testing use the same logic.
 * Flows with unit mismatches are drawn in red.
 */
final class MaterialFlowPass implements RenderPass {

    private final CanvasState canvasState;

    MaterialFlowPass(CanvasState canvasState) {
        this.canvasState = canvasState;
    }

    @Override
    public void render(GraphicsContext gc, CanvasRenderer.RenderContext ctx) {
        ModelEditor editor = ctx.editor();
        MaturityAnalysis maturity = ctx.maturityAnalysis();

        for (FlowDef flow : editor.getFlows()) {
            if (!canvasState.hasElement(flow.name())) {
                continue;
            }
            double midX = canvasState.getX(flow.name());
            double midY = canvasState.getY(flow.name());

            double sourceX;
            double sourceY;
            boolean sourceIsCloud;

            if (flow.source() != null && canvasState.hasElement(flow.source())) {
                FlowGeometry.Point2D edge = FlowGeometry.clipToElement(
                        canvasState, flow.source(), midX, midY);
                sourceX = edge.x();
                sourceY = edge.y();
                sourceIsCloud = false;
            } else {
                FlowGeometry.Point2D cloud = FlowEndpointCalculator.cloudPosition(
                        FlowEndpointCalculator.FlowEnd.SOURCE, flow, canvasState);
                sourceX = cloud != null ? cloud.x() : midX - LayoutMetrics.CLOUD_OFFSET;
                sourceY = cloud != null ? cloud.y() : midY;
                sourceIsCloud = true;
            }

            double sinkX;
            double sinkY;
            boolean sinkIsCloud;

            if (flow.sink() != null && canvasState.hasElement(flow.sink())) {
                FlowGeometry.Point2D edge = FlowGeometry.clipToElement(
                        canvasState, flow.sink(), midX, midY);
                sinkX = edge.x();
                sinkY = edge.y();
                sinkIsCloud = false;
            } else {
                FlowGeometry.Point2D cloud = FlowEndpointCalculator.cloudPosition(
                        FlowEndpointCalculator.FlowEnd.SINK, flow, canvasState);
                sinkX = cloud != null ? cloud.x() : midX + LayoutMetrics.CLOUD_OFFSET;
                sinkY = cloud != null ? cloud.y() : midY;
                sinkIsCloud = true;
            }

            boolean mismatch = maturity != null
                    && maturity.unitMismatchFlows().contains(flow.name());
            if (mismatch) {
                ConnectionRenderer.drawMaterialFlow(gc, sourceX, sourceY, midX, midY,
                        sinkX, sinkY, sourceIsCloud, sinkIsCloud,
                        ColorPalette.UNIT_MISMATCH);
            } else {
                ConnectionRenderer.drawMaterialFlow(gc, sourceX, sourceY, midX, midY,
                        sinkX, sinkY, sourceIsCloud, sinkIsCloud);
            }
        }
    }
}
