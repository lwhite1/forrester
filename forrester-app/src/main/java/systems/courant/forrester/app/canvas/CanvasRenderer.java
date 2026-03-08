package systems.courant.forrester.app.canvas;

import systems.courant.forrester.model.def.CausalLinkDef;
import systems.courant.forrester.model.def.ConnectorRoute;
import systems.courant.forrester.model.def.ConstantDef;
import systems.courant.forrester.model.def.ElementType;
import systems.courant.forrester.model.def.FlowDef;
import systems.courant.forrester.model.def.ValidationIssue;
import systems.courant.forrester.model.graph.FeedbackAnalysis;
import systems.courant.forrester.model.graph.FeedbackAnalysis.CausalLoop;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

/**
 * Rendering coordinator for the model canvas.
 * Draws connections, elements, selection indicators, and rubber-band overlays.
 */
public class CanvasRenderer {

    private static final Color RUBBER_BAND_COLOR = Color.web("#4A90D9", 0.6);
    private static final Color STOCK_HOVER_COLOR = Color.web("#4A90D9", 0.4);
    private static final double RUBBER_BAND_DASH = 8;
    private static final double RUBBER_BAND_GAP = 4;

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
        static final MarqueeState IDLE = new MarqueeState(false, 0, 0, 0, 0);
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
        static final RerouteState IDLE = new RerouteState(false, 0, 0, 0, 0);
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
        static final ReattachState IDLE = new ReattachState(false, 0, 0, 0, 0);
    }

    /**
     * Groups the per-frame interactive state passed to {@link #render}.
     */
    public record RenderContext(
            ModelEditor editor,
            List<ConnectorRoute> connectors,
            FlowCreationController.State flowState,
            CausalLinkCreationController.State causalLinkState,
            ReattachState reattachState,
            RerouteState rerouteState,
            MarqueeState marqueeState,
            FeedbackAnalysis loopAnalysis,
            Map<String, ValidationIssue.Severity> elementIssues,
            String hoveredElement,
            ConnectionId hoveredConnection,
            ConnectionId selectedConnection
    ) {}

    private final CanvasState canvasState;
    private final Viewport viewport;

    public CanvasRenderer(CanvasState canvasState, Viewport viewport) {
        this.canvasState = canvasState;
        this.viewport = viewport;
    }

    /**
     * Renders the full canvas: background, connections, elements, selection, and overlays.
     */
    public void render(GraphicsContext gc, double width, double height, RenderContext ctx) {
        // Background in screen space
        gc.clearRect(0, 0, width, height);
        gc.setFill(ColorPalette.BACKGROUND);
        gc.fillRect(0, 0, width, height);

        ModelEditor editor = ctx.editor();
        List<ConnectorRoute> connectors = ctx.connectors();
        FlowCreationController.State flowState = ctx.flowState();
        CausalLinkCreationController.State causalLinkState = ctx.causalLinkState();
        ReattachState reattachState = ctx.reattachState();
        RerouteState rerouteState = ctx.rerouteState();
        MarqueeState marqueeState = ctx.marqueeState();
        FeedbackAnalysis loopAnalysis = ctx.loopAnalysis();
        Map<String, ValidationIssue.Severity> elementIssues = ctx.elementIssues();
        String hoveredElement = ctx.hoveredElement();
        ConnectionId hoveredConnection = ctx.hoveredConnection();
        ConnectionId selectedConnection = ctx.selectedConnection();

        if (editor == null) {
            return;
        }

        // Apply viewport transform for world-space rendering
        gc.save();
        viewport.applyTo(gc);

        // 1. Draw connections first (behind elements)
        drawMaterialFlows(gc, editor);
        drawInfoLinks(gc, connectors);
        drawCausalLinks(gc, editor);

        // 1b. Draw loop edge highlights (above normal connections, behind elements)
        if (loopAnalysis != null) {
            drawLoopEdges(gc, connectors, editor, loopAnalysis);
        }

        // 2. Draw elements on top
        for (String name : canvasState.getDrawOrder()) {
            ElementType type = canvasState.getType(name).orElse(null);
            double cx = canvasState.getX(name);
            double cy = canvasState.getY(name);

            if (type == null) {
                continue;
            }

            switch (type) {
                case STOCK -> {
                    double w = LayoutMetrics.effectiveWidth(canvasState, name);
                    double h = LayoutMetrics.effectiveHeight(canvasState, name);
                    String unit = editor.getStockUnit(name).orElse(null);
                    ElementRenderer.drawStock(gc, name, unit,
                            cx - w / 2, cy - h / 2, w, h);
                }
                case FLOW -> {
                    ElementRenderer.drawFlow(gc, name,
                            cx - LayoutMetrics.FLOW_INDICATOR_SIZE / 2,
                            cy - LayoutMetrics.FLOW_INDICATOR_SIZE / 2,
                            LayoutMetrics.FLOW_INDICATOR_SIZE, LayoutMetrics.FLOW_INDICATOR_SIZE);
                }
                case AUX -> {
                    double w = LayoutMetrics.effectiveWidth(canvasState, name);
                    double h = LayoutMetrics.effectiveHeight(canvasState, name);
                    ElementRenderer.drawAux(gc, name,
                            cx - w / 2, cy - h / 2, w, h);
                }
                case CONSTANT -> {
                    double w = LayoutMetrics.effectiveWidth(canvasState, name);
                    double h = LayoutMetrics.effectiveHeight(canvasState, name);
                    double value = editor.getConstantByName(name)
                            .map(ConstantDef::value).orElse(0.0);
                    ElementRenderer.drawConstant(gc, name, value,
                            cx - w / 2, cy - h / 2, w, h);
                }
                case MODULE -> {
                    double w = LayoutMetrics.effectiveWidth(canvasState, name);
                    double h = LayoutMetrics.effectiveHeight(canvasState, name);
                    ElementRenderer.drawModule(gc, name, cx - w / 2, cy - h / 2, w, h);
                }
                case LOOKUP -> {
                    double w = LayoutMetrics.effectiveWidth(canvasState, name);
                    double h = LayoutMetrics.effectiveHeight(canvasState, name);
                    int pts = editor.getLookupTableByName(name)
                            .map(lt -> lt.xValues().length).orElse(0);
                    ElementRenderer.drawLookup(gc, name, pts,
                            cx - w / 2, cy - h / 2, w, h);
                }
                case CLD_VARIABLE -> {
                    double w = LayoutMetrics.effectiveWidth(canvasState, name);
                    double h = LayoutMetrics.effectiveHeight(canvasState, name);
                    ElementRenderer.drawCldVariable(gc, name,
                            cx - w / 2, cy - h / 2, w, h);
                }
                default -> { }
            }
        }

        // 2a. Draw error/warning indicators on elements with validation issues
        if (elementIssues != null) {
            for (Map.Entry<String, ValidationIssue.Severity> entry : elementIssues.entrySet()) {
                if (canvasState.hasElement(entry.getKey())) {
                    ErrorIndicatorRenderer.drawIndicator(
                            gc, canvasState, entry.getKey(), entry.getValue());
                }
            }
        }

        // 2b. Draw loop participant highlights around elements in loops
        if (loopAnalysis != null) {
            for (String name : loopAnalysis.loopParticipants()) {
                if (canvasState.hasElement(name)) {
                    FeedbackLoopRenderer.drawLoopHighlight(gc, canvasState, name);
                }
            }
        }

        // 2b2. Draw causal loop type labels (R1, B1, etc.) at loop centroids
        if (loopAnalysis != null) {
            for (CausalLoop loop : loopAnalysis.causalLoops()) {
                drawCausalLoopLabel(gc, loop);
            }
        }

        // 2c. Draw connection highlights (above loops, below element hover)
        if (selectedConnection != null) {
            drawConnectionHighlight(gc, connectors, selectedConnection, false);
        }
        if (hoveredConnection != null
                && !hoveredConnection.equals(selectedConnection)) {
            drawConnectionHighlight(gc, connectors, hoveredConnection, true);
        }

        // 2d. Draw hover indicator (above loops, below selection)
        if (hoveredElement != null && !canvasState.getSelection().contains(hoveredElement)) {
            SelectionRenderer.drawHoverIndicator(gc, canvasState, hoveredElement);
        }

        // 3. Draw selection indicators on top of everything
        for (String name : canvasState.getSelection()) {
            SelectionRenderer.drawSelectionIndicator(gc, canvasState, name);
        }

        // 4. Draw rubber-band line during pending flow creation
        if (flowState.pending()) {
            drawFlowRubberBand(gc, flowState);
        }

        // 4b. Draw rubber-band line during pending causal link creation
        if (causalLinkState.pending()) {
            drawCausalLinkRubberBand(gc, causalLinkState);
        }

        // 5. Draw reattachment rubber-band
        if (reattachState.active()) {
            drawReattachRubberBand(gc, reattachState);
        }

        // 5b. Draw connection reroute rubber-band
        if (rerouteState.active()) {
            drawRerouteRubberBand(gc, rerouteState);
        }

        // 6. Draw marquee selection rectangle
        if (marqueeState.active()) {
            drawMarquee(gc, marqueeState);
        }

        gc.restore();
    }

    /**
     * Draws material flow arrows routed through the flow indicator (diamond).
     * Cloud positions are computed via {@link FlowEndpointCalculator#cloudPosition}
     * so that rendering and hit-testing use the same logic.
     */
    private void drawMaterialFlows(GraphicsContext gc, ModelEditor editor) {
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

            ConnectionRenderer.drawMaterialFlow(gc, sourceX, sourceY, midX, midY,
                    sinkX, sinkY, sourceIsCloud, sinkIsCloud);
        }
    }

    /**
     * Draws info link dashed arrows based on cached connector routes.
     */
    private void drawInfoLinks(GraphicsContext gc, List<ConnectorRoute> connectors) {
        for (ConnectorRoute route : connectors) {
            String fromName = route.from();
            String toName = route.to();

            if (!canvasState.hasElement(fromName) || !canvasState.hasElement(toName)) {
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

            ConnectionRenderer.drawInfoLink(gc, clippedFrom.x(), clippedFrom.y(),
                    clippedTo.x(), clippedTo.y());
        }
    }

    /**
     * Draws causal links between CLD variables (and potentially S&F elements)
     * as curved arcs using quadratic Bézier curves.
     */
    private void drawCausalLinks(GraphicsContext gc, ModelEditor editor) {
        List<CausalLinkDef> allLinks = editor.getCausalLinks();

        for (CausalLinkDef link : allLinks) {
            String fromName = link.from();
            String toName = link.to();

            if (!canvasState.hasElement(fromName) || !canvasState.hasElement(toName)) {
                continue;
            }

            double fromX = canvasState.getX(fromName);
            double fromY = canvasState.getY(fromName);

            // Self-loop: use cubic Bézier loop above the element
            if (fromName.equals(toName)) {
                double halfW = LayoutMetrics.effectiveWidth(canvasState, fromName) / 2;
                double halfH = LayoutMetrics.effectiveHeight(canvasState, fromName) / 2;
                double[] loopPts = CausalLinkGeometry.selfLoopPoints(fromX, fromY, halfW, halfH);
                ConnectionRenderer.drawCausalLinkSelfLoop(gc, loopPts, link.polarity());
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
        }
    }

    /**
     * Draws highlighted edges for info links and material flows that are part of feedback loops.
     */
    private void drawLoopEdges(GraphicsContext gc, List<ConnectorRoute> connectors,
                               ModelEditor editor, FeedbackAnalysis loopAnalysis) {
        // Highlight info link edges
        for (ConnectorRoute route : connectors) {
            String fromName = route.from();
            String toName = route.to();

            if (!canvasState.hasElement(fromName) || !canvasState.hasElement(toName)) {
                continue;
            }
            if (!loopAnalysis.isLoopEdge(fromName, toName)) {
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

            FeedbackLoopRenderer.drawLoopEdge(gc, clippedFrom.x(), clippedFrom.y(),
                    clippedTo.x(), clippedTo.y());
        }

        // Highlight material flow edges (flow <-> stock connections)
        for (FlowDef flow : editor.getFlows()) {
            if (!canvasState.hasElement(flow.name())) {
                continue;
            }
            double midX = canvasState.getX(flow.name());
            double midY = canvasState.getY(flow.name());

            if (flow.source() != null && canvasState.hasElement(flow.source())
                    && loopAnalysis.isLoopEdge(flow.name(), flow.source())) {
                FlowGeometry.Point2D edge = FlowGeometry.clipToElement(
                        canvasState, flow.source(), midX, midY);
                FeedbackLoopRenderer.drawLoopEdge(gc, midX, midY, edge.x(), edge.y());
            }

            if (flow.sink() != null && canvasState.hasElement(flow.sink())
                    && loopAnalysis.isLoopEdge(flow.name(), flow.sink())) {
                FlowGeometry.Point2D edge = FlowGeometry.clipToElement(
                        canvasState, flow.sink(), midX, midY);
                FeedbackLoopRenderer.drawLoopEdge(gc, midX, midY, edge.x(), edge.y());
            }
        }

        // Highlight causal link edges (curved)
        List<CausalLinkDef> allLinks = editor.getCausalLinks();
        for (CausalLinkDef link : allLinks) {
            String fromName = link.from();
            String toName = link.to();

            if (!canvasState.hasElement(fromName) || !canvasState.hasElement(toName)) {
                continue;
            }
            if (!loopAnalysis.isLoopEdge(fromName, toName)) {
                continue;
            }

            double fromX = canvasState.getX(fromName);
            double fromY = canvasState.getY(fromName);

            if (fromName.equals(toName)) {
                double halfW = LayoutMetrics.effectiveWidth(canvasState, fromName) / 2;
                double halfH = LayoutMetrics.effectiveHeight(canvasState, fromName) / 2;
                double[] loopPts = CausalLinkGeometry.selfLoopPoints(fromX, fromY, halfW, halfH);
                FeedbackLoopRenderer.drawLoopEdgeCubic(gc, loopPts);
                continue;
            }

            double toX = canvasState.getX(toName);
            double toY = canvasState.getY(toName);

            CausalLinkGeometry.ControlPoint cp = CausalLinkGeometry.controlPoint(
                    fromX, fromY, toX, toY, fromName, toName, allLinks);

            FlowGeometry.Point2D clippedFrom = FlowGeometry.clipToElement(
                    canvasState, fromName, cp.x(), cp.y());
            FlowGeometry.Point2D clippedTo = FlowGeometry.clipToElement(
                    canvasState, toName, cp.x(), cp.y());

            FeedbackLoopRenderer.drawLoopEdgeCurved(gc,
                    clippedFrom.x(), clippedFrom.y(),
                    cp.x(), cp.y(),
                    clippedTo.x(), clippedTo.y());
        }
    }

    /**
     * Draws a rubber-band line for flow creation.
     */
    private void drawFlowRubberBand(GraphicsContext gc, FlowCreationController.State state) {
        double startX = state.sourceX();
        double startY = state.sourceY();

        if (state.source() == null) {
            ConnectionRenderer.drawCloud(gc, startX, startY);
        }

        gc.setStroke(RUBBER_BAND_COLOR);
        gc.setLineWidth(2);
        gc.setLineDashes(RUBBER_BAND_DASH, RUBBER_BAND_GAP);
        gc.strokeLine(startX, startY, state.rubberBandEndX(), state.rubberBandEndY());
        gc.setLineDashes();

        String hoverStock = FlowCreationController.hitTestStockOnly(
                state.rubberBandEndX(), state.rubberBandEndY(), canvasState);
        if (hoverStock != null) {
            drawStockHoverHighlight(gc, hoverStock);
        }
    }

    /**
     * Draws a rubber-band line for causal link creation.
     */
    private void drawCausalLinkRubberBand(GraphicsContext gc,
                                          CausalLinkCreationController.State state) {
        gc.setStroke(RUBBER_BAND_COLOR);
        gc.setLineWidth(2);
        gc.setLineDashes(RUBBER_BAND_DASH, RUBBER_BAND_GAP);
        gc.strokeLine(state.sourceX(), state.sourceY(),
                state.rubberBandEndX(), state.rubberBandEndY());
        gc.setLineDashes();

        String hitElement = HitTester.hitTest(canvasState,
                state.rubberBandEndX(), state.rubberBandEndY());
        if (hitElement != null && !hitElement.equals(state.source())) {
            drawElementHoverHighlight(gc, hitElement);
        }
    }

    /**
     * Draws a rubber-band line for flow endpoint reattachment.
     * Shows a stock highlight when hovering a stock, or a cloud preview on empty space.
     */
    private void drawReattachRubberBand(GraphicsContext gc, ReattachState state) {
        gc.setStroke(RUBBER_BAND_COLOR);
        gc.setLineWidth(2);
        gc.setLineDashes(RUBBER_BAND_DASH, RUBBER_BAND_GAP);
        gc.strokeLine(state.diamondX(), state.diamondY(), state.rubberBandX(), state.rubberBandY());
        gc.setLineDashes();

        String hoverStock = FlowCreationController.hitTestStockOnly(
                state.rubberBandX(), state.rubberBandY(), canvasState);
        if (hoverStock != null) {
            drawStockHoverHighlight(gc, hoverStock);
        } else {
            ConnectionRenderer.drawCloud(gc, state.rubberBandX(), state.rubberBandY());
        }
    }

    /**
     * Draws a rubber-band line for connection rerouting.
     * Shows an element highlight when hovering a valid target.
     */
    private void drawRerouteRubberBand(GraphicsContext gc, RerouteState state) {
        gc.setStroke(RUBBER_BAND_COLOR);
        gc.setLineWidth(2);
        gc.setLineDashes(RUBBER_BAND_DASH, RUBBER_BAND_GAP);
        gc.strokeLine(state.anchorX(), state.anchorY(),
                state.rubberBandX(), state.rubberBandY());
        gc.setLineDashes();

        String hitElement = HitTester.hitTest(canvasState,
                state.rubberBandX(), state.rubberBandY());
        if (hitElement != null) {
            drawElementHoverHighlight(gc, hitElement);
        }
    }

    /**
     * Draws a loop type label (e.g. "R1", "B2") at the centroid of the loop's variables.
     */
    private void drawCausalLoopLabel(GraphicsContext gc, CausalLoop loop) {
        double sumX = 0;
        double sumY = 0;
        int count = 0;

        for (String name : loop.path()) {
            if (canvasState.hasElement(name)) {
                sumX += canvasState.getX(name);
                sumY += canvasState.getY(name);
                count++;
            }
        }

        if (count == 0) {
            return;
        }

        FeedbackLoopRenderer.drawLoopLabel(gc, loop.label(), loop.type(),
                sumX / count, sumY / count);
    }

    /**
     * Draws a dashed highlight rectangle around any element.
     */
    private void drawElementHoverHighlight(GraphicsContext gc, String elementName) {
        double ex = canvasState.getX(elementName);
        double ey = canvasState.getY(elementName);
        double halfW = LayoutMetrics.effectiveWidth(canvasState, elementName) / 2 + 4;
        double halfH = LayoutMetrics.effectiveHeight(canvasState, elementName) / 2 + 4;

        gc.setStroke(STOCK_HOVER_COLOR);
        gc.setLineWidth(2.5);
        gc.setLineDashes(6, 3);
        gc.strokeRect(ex - halfW, ey - halfH, halfW * 2, halfH * 2);
        gc.setLineDashes();
    }

    /**
     * Draws a dashed highlight rectangle around a stock element.
     */
    private void drawStockHoverHighlight(GraphicsContext gc, String stockName) {
        double sx = canvasState.getX(stockName);
        double sy = canvasState.getY(stockName);
        double halfW = LayoutMetrics.effectiveWidth(canvasState, stockName) / 2 + 4;
        double halfH = LayoutMetrics.effectiveHeight(canvasState, stockName) / 2 + 4;

        gc.setStroke(STOCK_HOVER_COLOR);
        gc.setLineWidth(2.5);
        gc.setLineDashes(6, 3);
        gc.strokeRect(sx - halfW, sy - halfH, halfW * 2, halfH * 2);
        gc.setLineDashes();
    }

    /**
     * Draws a hover or selection highlight for the given connection.
     */
    private void drawConnectionHighlight(GraphicsContext gc, List<ConnectorRoute> connectors,
                                         ConnectionId connectionId, boolean isHover) {
        // Try info links first (straight line highlight)
        for (ConnectorRoute route : connectors) {
            if (route.from().equals(connectionId.from())
                    && route.to().equals(connectionId.to())) {
                drawClippedHighlight(gc, connectionId.from(), connectionId.to(), isHover, false);
                return;
            }
        }
        // Fall back to causal links (curved highlight)
        if (canvasState.hasElement(connectionId.from())
                && canvasState.hasElement(connectionId.to())) {
            drawClippedHighlight(gc, connectionId.from(), connectionId.to(), isHover, true);
        }
    }

    private void drawClippedHighlight(GraphicsContext gc, String fromName, String toName,
                                      boolean isHover, boolean isCausalLink) {
        if (!canvasState.hasElement(fromName) || !canvasState.hasElement(toName)) {
            return;
        }

        double fromX = canvasState.getX(fromName);
        double fromY = canvasState.getY(fromName);
        double toX = canvasState.getX(toName);
        double toY = canvasState.getY(toName);

        if (isCausalLink) {
            // Curved highlight for causal links
            // Note: cast needed to get the editor — but we only need positions here
            // so we pass null-safe defaults
            if (fromName.equals(toName)) {
                // Self-loop highlight — skip for now, straight highlight is fine
                FlowGeometry.Point2D cf = FlowGeometry.clipToElement(canvasState, fromName, toX, toY);
                FlowGeometry.Point2D ct = FlowGeometry.clipToElement(canvasState, toName, fromX, fromY);
                if (isHover) {
                    SelectionRenderer.drawConnectionHover(gc, cf.x(), cf.y(), ct.x(), ct.y());
                } else {
                    SelectionRenderer.drawConnectionSelection(gc, cf.x(), cf.y(), ct.x(), ct.y());
                }
                return;
            }

            CausalLinkGeometry.ControlPoint cp = CausalLinkGeometry.controlPoint(
                    fromX, fromY, toX, toY, fromName, toName, List.of());

            FlowGeometry.Point2D clippedFrom = FlowGeometry.clipToElement(
                    canvasState, fromName, cp.x(), cp.y());
            FlowGeometry.Point2D clippedTo = FlowGeometry.clipToElement(
                    canvasState, toName, cp.x(), cp.y());

            if (isHover) {
                SelectionRenderer.drawConnectionHoverCurved(gc,
                        clippedFrom.x(), clippedFrom.y(),
                        cp.x(), cp.y(),
                        clippedTo.x(), clippedTo.y());
            } else {
                SelectionRenderer.drawConnectionSelectionCurved(gc,
                        clippedFrom.x(), clippedFrom.y(),
                        cp.x(), cp.y(),
                        clippedTo.x(), clippedTo.y());
            }
        } else {
            FlowGeometry.Point2D clippedFrom = FlowGeometry.clipToElement(
                    canvasState, fromName, toX, toY);
            FlowGeometry.Point2D clippedTo = FlowGeometry.clipToElement(
                    canvasState, toName, fromX, fromY);

            if (isHover) {
                SelectionRenderer.drawConnectionHover(gc,
                        clippedFrom.x(), clippedFrom.y(),
                        clippedTo.x(), clippedTo.y());
            } else {
                SelectionRenderer.drawConnectionSelection(gc,
                        clippedFrom.x(), clippedFrom.y(),
                        clippedTo.x(), clippedTo.y());
            }
        }
    }

    /**
     * Draws the marquee (rubber-band) selection rectangle.
     */
    private void drawMarquee(GraphicsContext gc, MarqueeState state) {
        double x = Math.min(state.startX(), state.endX());
        double y = Math.min(state.startY(), state.endY());
        double w = Math.abs(state.endX() - state.startX());
        double h = Math.abs(state.endY() - state.startY());

        gc.setFill(Color.web("#4A90D9", 0.1));
        gc.fillRect(x, y, w, h);

        gc.setStroke(RUBBER_BAND_COLOR);
        gc.setLineWidth(1);
        gc.setLineDashes(6, 3);
        gc.strokeRect(x, y, w, h);
        gc.setLineDashes();
    }

}
