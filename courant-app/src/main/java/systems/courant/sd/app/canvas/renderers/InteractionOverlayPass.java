package systems.courant.sd.app.canvas.renderers;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.CausalLinkGeometry;
import systems.courant.sd.app.canvas.ColorPalette;
import systems.courant.sd.app.canvas.FlowGeometry;
import systems.courant.sd.app.canvas.HitTester;
import systems.courant.sd.app.canvas.LayoutMetrics;
import systems.courant.sd.app.canvas.MaturityAnalysis;
import systems.courant.sd.app.canvas.MaturityIndicatorRenderer;
import systems.courant.sd.app.canvas.ModelEditor;
import systems.courant.sd.app.canvas.controllers.CausalLinkCreationController;
import systems.courant.sd.app.canvas.controllers.FlowCreationController;
import systems.courant.sd.app.canvas.controllers.InfoLinkCreationController;
import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.ConnectorRoute;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.ValidationIssue;
import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.expr.DelayDetector;
import systems.courant.sd.model.graph.CausalTraceAnalysis;
import systems.courant.sd.model.graph.FeedbackAnalysis;
import systems.courant.sd.model.graph.FeedbackAnalysis.CausalLoop;

import systems.courant.sd.app.canvas.ConnectionId;

import java.util.List;
import java.util.Map;

/**
 * Draws all interactive overlays above elements: sparklines, validation indicators,
 * maturity indicators, analysis highlights, connection/hover/selection highlights,
 * rubber bands, and marquee.
 */
final class InteractionOverlayPass implements RenderPass {

    private static final Color RUBBER_BAND_COLOR = ColorPalette.RUBBER_BAND;
    private static final Color STOCK_HOVER_COLOR = ColorPalette.HOVER;
    private static final double RUBBER_BAND_DASH = 8;
    private static final double RUBBER_BAND_GAP = 4;
    private static final Color PORT_HOVER_COLOR = ColorPalette.PORT_HOVER;
    private static final double PORT_HOVER_RADIUS = 7.0;

    private final CanvasState canvasState;

    InteractionOverlayPass(CanvasState canvasState) {
        this.canvasState = canvasState;
    }

    @Override
    public void render(GraphicsContext gc, CanvasRenderer.RenderContext ctx) {
        List<ConnectorRoute> connectors = ctx.connectors();
        List<CausalLinkDef> allCausalLinks = ctx.editor().getCausalLinks();
        boolean hideAux = ctx.hideVariables();

        // Sparklines inside stock elements
        if (ctx.sparklineData() != null) {
            SparklineRenderer.drawAll(gc, canvasState,
                    ctx.sparklineData().stockSeries(), ctx.sparklineData().stale());
        }

        // Error/warning indicators on elements with validation issues
        Map<String, ValidationIssue.Severity> elementIssues = ctx.elementIssues();
        if (elementIssues != null) {
            for (Map.Entry<String, ValidationIssue.Severity> entry : elementIssues.entrySet()) {
                if (canvasState.hasElement(entry.getKey())
                        && !RenderPass.isHiddenAux(entry.getKey(), hideAux, canvasState)) {
                    ErrorIndicatorRenderer.drawIndicator(
                            gc, canvasState, entry.getKey(), entry.getValue());
                }
            }
        }

        // Maturity indicators (missing equation accent, missing unit badge)
        MaturityAnalysis maturity = ctx.maturityAnalysis();
        if (maturity != null) {
            for (String name : maturity.missingEquation()) {
                if (canvasState.hasElement(name)
                        && !RenderPass.isHiddenAux(name, hideAux, canvasState)) {
                    MaturityIndicatorRenderer.drawMissingEquationAccent(gc, canvasState, name);
                }
            }
            for (String name : maturity.missingUnit()) {
                if (canvasState.hasElement(name)
                        && !RenderPass.isHiddenAux(name, hideAux, canvasState)) {
                    MaturityIndicatorRenderer.drawMissingUnitBadge(gc, canvasState, name);
                }
            }
        }

        // Loop participant highlights around elements in loops
        FeedbackAnalysis loopAnalysis = ctx.loopAnalysis();
        if (loopAnalysis != null) {
            for (String name : loopAnalysis.loopParticipants()) {
                if (canvasState.hasElement(name)
                        && !RenderPass.isHiddenAux(name, hideAux, canvasState)) {
                    FeedbackLoopRenderer.drawLoopHighlight(gc, canvasState, name);
                }
            }
        }

        // Trace element highlights
        CausalTraceAnalysis traceAnalysis = ctx.traceAnalysis();
        if (traceAnalysis != null) {
            for (String name : traceAnalysis.depthMap().keySet()) {
                if (canvasState.hasElement(name)
                        && !RenderPass.isHiddenAux(name, hideAux, canvasState)) {
                    CausalTraceRenderer.drawTraceHighlight(gc, canvasState, name, traceAnalysis);
                }
            }
        }

        // Causal loop type labels (R1, B1, etc.) at loop centroids
        if (loopAnalysis != null) {
            for (CausalLoop loop : loopAnalysis.causalLoops()) {
                if (loop.type() != FeedbackAnalysis.LoopType.INDETERMINATE) {
                    drawCausalLoopLabel(gc, loop);
                }
            }
        }

        // Connection highlights (above loops, below element hover)
        ConnectionId selectedConnection = ctx.selectedConnection();
        ConnectionId hoveredConnection = ctx.hoveredConnection();
        if (selectedConnection != null) {
            drawConnectionHighlight(gc, connectors, allCausalLinks, selectedConnection, false);
        }
        if (hoveredConnection != null
                && !hoveredConnection.equals(selectedConnection)) {
            drawConnectionHighlight(gc, connectors, allCausalLinks, hoveredConnection, true);
        }

        // Hover indicator (above loops, below selection)
        String hoveredElement = ctx.hoveredElement();
        if (hoveredElement != null && !canvasState.getSelection().contains(hoveredElement)
                && !RenderPass.isHiddenAux(hoveredElement, hideAux, canvasState)) {
            drawHoverHighlight(gc, ctx, hoveredElement);
        }

        // Selection indicators on top of everything
        for (String name : canvasState.getSelection()) {
            if (!RenderPass.isHiddenAux(name, hideAux, canvasState)) {
                SelectionRenderer.drawSelectionIndicator(gc, canvasState, name);
            }
        }

        // Rubber-band lines for pending creations
        FlowCreationController.State flowState = ctx.flowState();
        if (flowState.pending()) {
            drawFlowRubberBand(gc, flowState);
        }

        CausalLinkCreationController.State causalLinkState = ctx.causalLinkState();
        if (causalLinkState.pending()) {
            drawCausalLinkRubberBand(gc, causalLinkState);
        }

        InfoLinkCreationController.State infoLinkState = ctx.infoLinkState();
        if (infoLinkState.pending()) {
            drawInfoLinkRubberBand(gc, infoLinkState);
        }

        // Port hover highlight during info link tool use
        if (infoLinkState.hoveredPort() != null) {
            drawPortHoverHighlight(gc, infoLinkState.hoveredPort());
        }

        // Reattachment rubber-band
        CanvasRenderer.ReattachState reattachState = ctx.reattachState();
        if (reattachState.active()) {
            drawReattachRubberBand(gc, reattachState);
        }

        // Connection reroute rubber-band
        CanvasRenderer.RerouteState rerouteState = ctx.rerouteState();
        if (rerouteState.active()) {
            drawRerouteRubberBand(gc, rerouteState);
        }

        // Marquee selection rectangle
        CanvasRenderer.MarqueeState marqueeState = ctx.marqueeState();
        if (marqueeState.active()) {
            drawMarquee(gc, marqueeState);
        }
    }

    // --- Hover highlighting ---

    private void drawHoverHighlight(GraphicsContext gc, CanvasRenderer.RenderContext ctx,
                                    String hoveredElement) {
        ElementType hoverType = canvasState.getType(hoveredElement).orElse(null);
        if (hoverType == ElementType.AUX) {
            ModelEditor ed = ctx.editor();
            double hw = LayoutMetrics.effectiveWidth(canvasState, hoveredElement);
            double hh = LayoutMetrics.effectiveHeight(canvasState, hoveredElement);
            double hx = canvasState.getX(hoveredElement) - hw / 2;
            double hy = canvasState.getY(hoveredElement) - hh / 2;
            boolean isLit = ed.getVariableByName(hoveredElement)
                    .map(VariableDef::isLiteral).orElse(false);
            String eq = ed.getVariableEquation(hoveredElement).orElse(null);
            boolean hasDel = ctx.showDelayBadges()
                    && DelayDetector.equationContainsDelay(eq);
            ElementRenderer.drawAux(gc, hoveredElement, isLit, eq, hasDel,
                    hx, hy, hw, hh, true);
        } else if (hoverType == ElementType.LOOKUP) {
            double hw = LayoutMetrics.effectiveWidth(canvasState, hoveredElement);
            double hh = LayoutMetrics.effectiveHeight(canvasState, hoveredElement);
            double hx = canvasState.getX(hoveredElement) - hw / 2;
            double hy = canvasState.getY(hoveredElement) - hh / 2;
            ElementRenderer.drawLookup(gc, hoveredElement, hx, hy, hw, hh, true);
        } else {
            SelectionRenderer.drawHoverIndicator(gc, canvasState, hoveredElement);
        }
    }

    // --- Rubber bands ---

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
            drawElementHoverHighlight(gc, hoverStock);
        }
    }

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

    private void drawInfoLinkRubberBand(GraphicsContext gc,
                                        InfoLinkCreationController.State state) {
        gc.setStroke(RUBBER_BAND_COLOR);
        gc.setLineWidth(2);
        gc.setLineDashes(RUBBER_BAND_DASH, RUBBER_BAND_GAP);
        gc.strokeLine(state.sourceX(), state.sourceY(),
                state.rubberBandEndX(), state.rubberBandEndY());
        gc.setLineDashes();

        String hitElement = HitTester.hitTest(canvasState,
                state.rubberBandEndX(), state.rubberBandEndY());
        if (hitElement != null && !hitElement.equals(state.sourceName())) {
            drawElementHoverHighlight(gc, hitElement);
        }
    }

    private void drawPortHoverHighlight(GraphicsContext gc, HitTester.PortHit port) {
        gc.setFill(PORT_HOVER_COLOR);
        gc.fillOval(port.portX() - PORT_HOVER_RADIUS, port.portY() - PORT_HOVER_RADIUS,
                PORT_HOVER_RADIUS * 2, PORT_HOVER_RADIUS * 2);
    }

    private void drawReattachRubberBand(GraphicsContext gc, CanvasRenderer.ReattachState state) {
        gc.setStroke(RUBBER_BAND_COLOR);
        gc.setLineWidth(2);
        gc.setLineDashes(RUBBER_BAND_DASH, RUBBER_BAND_GAP);
        gc.strokeLine(state.diamondX(), state.diamondY(),
                state.rubberBandX(), state.rubberBandY());
        gc.setLineDashes();

        String hoverStock = FlowCreationController.hitTestStockOnly(
                state.rubberBandX(), state.rubberBandY(), canvasState);
        if (hoverStock != null) {
            drawElementHoverHighlight(gc, hoverStock);
        } else {
            ConnectionRenderer.drawCloud(gc, state.rubberBandX(), state.rubberBandY());
        }
    }

    private void drawRerouteRubberBand(GraphicsContext gc, CanvasRenderer.RerouteState state) {
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

    // --- Element and connection highlights ---

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

    private void drawConnectionHighlight(GraphicsContext gc, List<ConnectorRoute> connectors,
                                         List<CausalLinkDef> allLinks,
                                         ConnectionId connectionId, boolean isHover) {
        for (ConnectorRoute route : connectors) {
            if (route.from().equals(connectionId.from())
                    && route.to().equals(connectionId.to())) {
                drawClippedHighlight(gc, connectionId.from(), connectionId.to(),
                        isHover, false, allLinks);
                return;
            }
        }
        if (canvasState.hasElement(connectionId.from())
                && canvasState.hasElement(connectionId.to())) {
            drawClippedHighlight(gc, connectionId.from(), connectionId.to(),
                    isHover, true, allLinks);
        }
    }

    private void drawClippedHighlight(GraphicsContext gc, String fromName, String toName,
                                      boolean isHover, boolean isCausalLink,
                                      List<CausalLinkDef> allLinks) {
        if (!canvasState.hasElement(fromName) || !canvasState.hasElement(toName)) {
            return;
        }

        double fromX = canvasState.getX(fromName);
        double fromY = canvasState.getY(fromName);
        double toX = canvasState.getX(toName);
        double toY = canvasState.getY(toName);

        if (isCausalLink) {
            if (fromName.equals(toName)) {
                FlowGeometry.Point2D cf = FlowGeometry.clipToElement(
                        canvasState, fromName, toX, toY);
                FlowGeometry.Point2D ct = FlowGeometry.clipToElement(
                        canvasState, toName, fromX, fromY);
                if (isHover) {
                    SelectionRenderer.drawConnectionHover(gc,
                            cf.x(), cf.y(), ct.x(), ct.y());
                } else {
                    SelectionRenderer.drawConnectionSelection(gc,
                            cf.x(), cf.y(), ct.x(), ct.y());
                }
                return;
            }

            CausalLinkGeometry.ControlPoint cp = CausalLinkGeometry.controlPoint(
                    fromX, fromY, toX, toY, fromName, toName, allLinks);

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

    // --- Analysis labels ---

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

    // --- Marquee ---

    private void drawMarquee(GraphicsContext gc, CanvasRenderer.MarqueeState state) {
        double x = Math.min(state.startX(), state.endX());
        double y = Math.min(state.startY(), state.endY());
        double w = Math.abs(state.endX() - state.startX());
        double h = Math.abs(state.endY() - state.startY());

        gc.setFill(ColorPalette.MARQUEE_FILL);
        gc.fillRect(x, y, w, h);

        gc.setStroke(RUBBER_BAND_COLOR);
        gc.setLineWidth(1);
        gc.setLineDashes(6, 3);
        gc.strokeRect(x, y, w, h);
        gc.setLineDashes();
    }
}
