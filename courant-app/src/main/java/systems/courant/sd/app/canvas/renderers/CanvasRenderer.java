package systems.courant.sd.app.canvas.renderers;

import systems.courant.sd.model.def.ConnectorRoute;
import systems.courant.sd.model.def.ValidationIssue;
import systems.courant.sd.model.graph.CausalTraceAnalysis;
import systems.courant.sd.model.graph.FeedbackAnalysis;

import javafx.scene.canvas.GraphicsContext;

import java.util.List;
import java.util.Map;
import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.ColorPalette;
import systems.courant.sd.app.canvas.ConnectionId;
import systems.courant.sd.app.canvas.MaturityAnalysis;
import systems.courant.sd.app.canvas.ModelEditor;
import systems.courant.sd.app.canvas.Viewport;
import systems.courant.sd.app.canvas.controllers.CausalLinkCreationController;
import systems.courant.sd.app.canvas.controllers.FlowCreationController;
import systems.courant.sd.app.canvas.controllers.InfoLinkCreationController;

/**
 * Rendering coordinator for the model canvas.
 * Orchestrates a pipeline of {@link RenderPass} objects that draw connections,
 * elements, selection indicators, and rubber-band overlays.
 */
public class CanvasRenderer {

    /**
     * State for marquee (rubber-band) selection rendering.
     */
    public record MarqueeState(
            boolean active,
            double startX,
            double startY,
            double endX,
            double endY
    ) {
        public static final MarqueeState IDLE = new MarqueeState(false, 0, 0, 0, 0);
    }

    /**
     * State for connection reroute rubber-band rendering.
     */
    public record RerouteState(
            boolean active,
            double anchorX,
            double anchorY,
            double rubberBandX,
            double rubberBandY
    ) {
        public static final RerouteState IDLE = new RerouteState(false, 0, 0, 0, 0);
    }

    /**
     * State for reattachment rubber-band rendering.
     */
    public record ReattachState(
            boolean active,
            double diamondX,
            double diamondY,
            double rubberBandX,
            double rubberBandY
    ) {
        public static final ReattachState IDLE = new ReattachState(false, 0, 0, 0, 0);
    }

    /**
     * Pre-extracted sparkline data for stock elements.
     *
     * @param stockSeries map of stock name to time-series values
     * @param stale       true if the simulation results are stale (model changed since last run)
     */
    public record SparklineData(Map<String, double[]> stockSeries, boolean stale) {
    }

    public record RenderContext(
            ModelEditor editor,
            List<ConnectorRoute> connectors,
            FlowCreationController.State flowState,
            CausalLinkCreationController.State causalLinkState,
            InfoLinkCreationController.State infoLinkState,
            ReattachState reattachState,
            RerouteState rerouteState,
            MarqueeState marqueeState,
            FeedbackAnalysis loopAnalysis,
            CausalTraceAnalysis traceAnalysis,
            Map<String, ValidationIssue.Severity> elementIssues,
            SparklineData sparklineData,
            String hoveredElement,
            ConnectionId hoveredConnection,
            ConnectionId selectedConnection,
            boolean hideVariables,
            boolean showDelayBadges,
            boolean hideInfoLinks,
            MaturityAnalysis maturityAnalysis
    ) {}

    private final Viewport viewport;
    private final List<RenderPass> passes;

    public CanvasRenderer(CanvasState canvasState, Viewport viewport) {
        this.viewport = viewport;
        this.passes = List.of(
                new MaterialFlowPass(canvasState),
                new InfoLinkPass(canvasState),
                new CausalLinkPass(canvasState),
                new LoopHighlightPass(canvasState),
                new ElementPass(canvasState),
                new InteractionOverlayPass(canvasState));
    }

    /**
     * Renders the full canvas: background, connections, elements, selection, and overlays.
     */
    public void render(GraphicsContext gc, double width, double height, RenderContext ctx) {
        gc.clearRect(0, 0, width, height);
        gc.setFill(ColorPalette.BACKGROUND);
        gc.fillRect(0, 0, width, height);

        if (ctx.editor() == null) {
            return;
        }

        gc.save();
        viewport.applyTo(gc);

        for (RenderPass pass : passes) {
            pass.render(gc, ctx);
        }

        gc.restore();
    }
}
