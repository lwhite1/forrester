package systems.courant.sd.app.canvas.renderers;

import javafx.scene.canvas.GraphicsContext;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.CausalLinkGeometry;
import systems.courant.sd.app.canvas.FlowGeometry;
import systems.courant.sd.app.canvas.LayoutMetrics;
import systems.courant.sd.app.canvas.ModelEditor;
import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.ConnectorRoute;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.graph.CausalTraceAnalysis;
import systems.courant.sd.model.graph.FeedbackAnalysis;

import java.util.List;

/**
 * Draws highlighted edges for feedback loops and causal traces.
 * Rendered above normal connections but behind elements.
 */
final class LoopHighlightPass implements RenderPass {

    private final CanvasState canvasState;

    LoopHighlightPass(CanvasState canvasState) {
        this.canvasState = canvasState;
    }

    @Override
    public void render(GraphicsContext gc, CanvasRenderer.RenderContext ctx) {
        ModelEditor editor = ctx.editor();
        boolean hideAux = ctx.hideVariables();

        FeedbackAnalysis loopAnalysis = ctx.loopAnalysis();
        if (loopAnalysis != null) {
            drawLoopEdges(gc, ctx.connectors(), editor, loopAnalysis, hideAux);
        }

        CausalTraceAnalysis traceAnalysis = ctx.traceAnalysis();
        if (traceAnalysis != null) {
            drawTraceEdges(gc, ctx.connectors(), editor, traceAnalysis, hideAux);
        }
    }

    /**
     * Draws highlighted edges for info links and material flows that are part of feedback loops.
     */
    private void drawLoopEdges(GraphicsContext gc, List<ConnectorRoute> connectors,
                               ModelEditor editor, FeedbackAnalysis loopAnalysis,
                               boolean hideAux) {
        // Highlight info link edges
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
        CausalLinkGeometry.LoopContext loopCtx = CausalLinkGeometry.loopContext(
                canvasState, editor.getCldVariables());
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
            if (!loopAnalysis.isLoopEdge(fromName, toName)) {
                continue;
            }

            double fromX = canvasState.getX(fromName);
            double fromY = canvasState.getY(fromName);

            if (fromName.equals(toName)) {
                double halfW = LayoutMetrics.effectiveWidth(canvasState, fromName) / 2;
                double halfH = LayoutMetrics.effectiveHeight(canvasState, fromName) / 2;
                double[] loopPts = CausalLinkGeometry.selfLoopPoints(
                        fromX, fromY, halfW, halfH, loopCtx, fromName);
                FeedbackLoopRenderer.drawLoopEdgeCubic(gc, loopPts);
                continue;
            }

            double toX = canvasState.getX(toName);
            double toY = canvasState.getY(toName);

            CausalLinkGeometry.ControlPoint cp = CausalLinkGeometry.controlPoint(
                    fromX, fromY, toX, toY, fromName, toName, allLinks, loopCtx);

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
     * Draws highlighted edges for trace connections with depth-based opacity.
     */
    private void drawTraceEdges(GraphicsContext gc, List<ConnectorRoute> connectors,
                                 ModelEditor editor, CausalTraceAnalysis traceAnalysis,
                                 boolean hideAux) {
        // Highlight info link edges in the trace
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
            if (!traceAnalysis.isTraceEdge(fromName, toName)
                    && !traceAnalysis.isTraceEdge(toName, fromName)) {
                continue;
            }

            int fromDepth = traceAnalysis.depthOf(fromName);
            int toDepth = traceAnalysis.depthOf(toName);
            int edgeDepth = Math.min(
                    fromDepth >= 0 ? fromDepth : Integer.MAX_VALUE,
                    toDepth >= 0 ? toDepth : Integer.MAX_VALUE);
            double opacity = traceAnalysis.opacityForDepth(edgeDepth);

            double fromX = canvasState.getX(fromName);
            double fromY = canvasState.getY(fromName);
            double toX = canvasState.getX(toName);
            double toY = canvasState.getY(toName);

            FlowGeometry.Point2D clippedFrom = FlowGeometry.clipToElement(
                    canvasState, fromName, toX, toY);
            FlowGeometry.Point2D clippedTo = FlowGeometry.clipToElement(
                    canvasState, toName, fromX, fromY);

            CausalTraceRenderer.drawTraceEdge(gc, clippedFrom.x(), clippedFrom.y(),
                    clippedTo.x(), clippedTo.y(), opacity, traceAnalysis.direction());
        }

        // Highlight material flow edges in the trace
        for (FlowDef flow : editor.getFlows()) {
            if (!canvasState.hasElement(flow.name())) {
                continue;
            }
            double midX = canvasState.getX(flow.name());
            double midY = canvasState.getY(flow.name());

            if (flow.source() != null && canvasState.hasElement(flow.source())
                    && (traceAnalysis.isTraceEdge(flow.name(), flow.source())
                        || traceAnalysis.isTraceEdge(flow.source(), flow.name()))) {
                int depth = Math.min(
                        traceAnalysis.depthOf(flow.name()) >= 0
                                ? traceAnalysis.depthOf(flow.name()) : Integer.MAX_VALUE,
                        traceAnalysis.depthOf(flow.source()) >= 0
                                ? traceAnalysis.depthOf(flow.source()) : Integer.MAX_VALUE);
                double opacity = traceAnalysis.opacityForDepth(depth);
                FlowGeometry.Point2D edge = FlowGeometry.clipToElement(
                        canvasState, flow.source(), midX, midY);
                CausalTraceRenderer.drawTraceEdge(gc, midX, midY, edge.x(), edge.y(),
                        opacity, traceAnalysis.direction());
            }

            if (flow.sink() != null && canvasState.hasElement(flow.sink())
                    && (traceAnalysis.isTraceEdge(flow.name(), flow.sink())
                        || traceAnalysis.isTraceEdge(flow.sink(), flow.name()))) {
                int depth = Math.min(
                        traceAnalysis.depthOf(flow.name()) >= 0
                                ? traceAnalysis.depthOf(flow.name()) : Integer.MAX_VALUE,
                        traceAnalysis.depthOf(flow.sink()) >= 0
                                ? traceAnalysis.depthOf(flow.sink()) : Integer.MAX_VALUE);
                double opacity = traceAnalysis.opacityForDepth(depth);
                FlowGeometry.Point2D edge = FlowGeometry.clipToElement(
                        canvasState, flow.sink(), midX, midY);
                CausalTraceRenderer.drawTraceEdge(gc, midX, midY, edge.x(), edge.y(),
                        opacity, traceAnalysis.direction());
            }
        }

        // Highlight causal link edges in the trace (curved)
        List<CausalLinkDef> allLinks = editor.getCausalLinks();
        CausalLinkGeometry.LoopContext traceLoopCtx = CausalLinkGeometry.loopContext(
                canvasState, editor.getCldVariables());
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
            if (!traceAnalysis.isTraceEdge(fromName, toName)
                    && !traceAnalysis.isTraceEdge(toName, fromName)) {
                continue;
            }

            int fromDepth = traceAnalysis.depthOf(fromName);
            int toDepth = traceAnalysis.depthOf(toName);
            int edgeDepth = Math.min(
                    fromDepth >= 0 ? fromDepth : Integer.MAX_VALUE,
                    toDepth >= 0 ? toDepth : Integer.MAX_VALUE);
            double opacity = traceAnalysis.opacityForDepth(edgeDepth);

            if (fromName.equals(toName)) {
                continue; // self-loops not relevant for tracing
            }

            double fromX = canvasState.getX(fromName);
            double fromY = canvasState.getY(fromName);
            double toX = canvasState.getX(toName);
            double toY = canvasState.getY(toName);

            CausalLinkGeometry.ControlPoint cp = CausalLinkGeometry.controlPoint(
                    fromX, fromY, toX, toY, fromName, toName, allLinks, traceLoopCtx);

            FlowGeometry.Point2D clippedFrom = FlowGeometry.clipToElement(
                    canvasState, fromName, cp.x(), cp.y());
            FlowGeometry.Point2D clippedTo = FlowGeometry.clipToElement(
                    canvasState, toName, cp.x(), cp.y());

            CausalTraceRenderer.drawTraceEdgeCurved(gc,
                    clippedFrom.x(), clippedFrom.y(),
                    cp.x(), cp.y(),
                    clippedTo.x(), clippedTo.y(),
                    opacity, traceAnalysis.direction());
        }
    }
}
