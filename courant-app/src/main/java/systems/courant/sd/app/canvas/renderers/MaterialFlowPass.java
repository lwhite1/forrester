package systems.courant.sd.app.canvas.renderers;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.ColorPalette;
import systems.courant.sd.app.canvas.MaterialFlowEndpoints;
import systems.courant.sd.app.canvas.MaturityAnalysis;
import systems.courant.sd.app.canvas.ModelEditor;
import systems.courant.sd.model.def.FlowDef;

/**
 * Draws material flow arrows routed through the flow indicator (diamond).
 * Endpoint resolution is delegated to {@link MaterialFlowEndpoints} so that
 * rendering and SVG export use the same logic.
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
            MaterialFlowEndpoints endpoints = MaterialFlowEndpoints.resolve(flow, canvasState);
            if (endpoints == null) {
                continue;
            }

            boolean mismatch = maturity != null
                    && maturity.unitMismatchFlows().contains(flow.name());
            Color flowColor;
            if (mismatch) {
                flowColor = ColorPalette.UNIT_MISMATCH;
            } else {
                flowColor = canvasState.getColor(flow.name())
                        .map(Color::web).orElse(ColorPalette.MATERIAL_FLOW);
            }
            ConnectionRenderer.drawMaterialFlow(gc,
                    endpoints.sourceX(), endpoints.sourceY(),
                    endpoints.midX(), endpoints.midY(),
                    endpoints.sinkX(), endpoints.sinkY(),
                    endpoints.sourceIsCloud(), endpoints.sinkIsCloud(),
                    flowColor);
        }
    }
}
