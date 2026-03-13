package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.FlowDef;

/**
 * Computes cloud positions for disconnected flow endpoints and hit-tests them.
 * Used by both the renderer (to draw clouds) and the interaction layer (to start reattachment drags).
 */
public final class FlowEndpointCalculator {

    private FlowEndpointCalculator() {
    }

    private static final double CLOUD_HIT_RADIUS = 18;
    private static final double ENDPOINT_HIT_RADIUS = 14;

    /**
     * Which end of a flow: source or sink.
     */
    public enum FlowEnd {
        SOURCE, SINK
    }

    /**
     * Result of a cloud endpoint hit test.
     */
    public record CloudHit(String flowName, FlowEnd end, double cloudX, double cloudY) {
    }

    /**
     * Returns the cloud position for a flow's disconnected endpoint, or null if connected.
     *
     * @param end the endpoint to check
     * @param flow the flow definition
     * @param canvasState canvas state for position lookups
     * @return cloud position, or null if the endpoint is connected
     */
    public static FlowGeometry.Point2D cloudPosition(FlowEnd end, FlowDef flow, CanvasState canvasState) {
        if (!canvasState.hasElement(flow.name())) {
            return null;
        }
        double midX = canvasState.getX(flow.name());
        double midY = canvasState.getY(flow.name());

        if (end == FlowEnd.SOURCE) {
            if (flow.source() != null && canvasState.hasElement(flow.source())) {
                return null; // connected
            }
            return computeCloudPosition(midX, midY, flow.sink(), canvasState, true);
        } else {
            if (flow.sink() != null && canvasState.hasElement(flow.sink())) {
                return null; // connected
            }
            return computeCloudPosition(midX, midY, flow.source(), canvasState, false);
        }
    }

    /**
     * Hit-tests all cloud (disconnected) endpoints across all flows.
     * Returns the first hit, or null.
     */
    public static CloudHit hitTestClouds(double worldX, double worldY,
                                         CanvasState canvasState, ModelEditor editor) {
        for (FlowDef flow : editor.getFlows()) {
            FlowGeometry.Point2D sourceCloud = cloudPosition(FlowEnd.SOURCE, flow, canvasState);
            if (sourceCloud != null) {
                double dist = Math.hypot(worldX - sourceCloud.x(), worldY - sourceCloud.y());
                if (dist <= CLOUD_HIT_RADIUS) {
                    return new CloudHit(flow.name(), FlowEnd.SOURCE,
                            sourceCloud.x(), sourceCloud.y());
                }
            }

            FlowGeometry.Point2D sinkCloud = cloudPosition(FlowEnd.SINK, flow, canvasState);
            if (sinkCloud != null) {
                double dist = Math.hypot(worldX - sinkCloud.x(), worldY - sinkCloud.y());
                if (dist <= CLOUD_HIT_RADIUS) {
                    return new CloudHit(flow.name(), FlowEnd.SINK,
                            sinkCloud.x(), sinkCloud.y());
                }
            }
        }
        return null;
    }

    /**
     * Hit-tests connected flow endpoints (where the pipe meets a stock border).
     * Returns a CloudHit describing the flow and end, or null.
     */
    public static CloudHit hitTestConnectedEndpoints(double worldX, double worldY,
                                                     CanvasState canvasState, ModelEditor editor) {
        for (FlowDef flow : editor.getFlows()) {
            if (!canvasState.hasElement(flow.name())) {
                continue;
            }

            // Only allow detachment when both ends are connected — if one end is
            // already a cloud, the user should reattach that cloud (via hitTestClouds)
            // rather than accidentally detaching the remaining connection.
            boolean sourceConnected = flow.source() != null && canvasState.hasElement(flow.source());
            boolean sinkConnected = flow.sink() != null && canvasState.hasElement(flow.sink());
            if (!sourceConnected || !sinkConnected) {
                continue;
            }

            double midX = canvasState.getX(flow.name());
            double midY = canvasState.getY(flow.name());

            // Check source endpoint
            if (flow.source() != null && canvasState.hasElement(flow.source())) {
                FlowGeometry.Point2D edge = FlowGeometry.clipToElement(
                        canvasState, flow.source(), midX, midY);
                double dist = Math.hypot(worldX - edge.x(), worldY - edge.y());
                if (dist <= ENDPOINT_HIT_RADIUS) {
                    return new CloudHit(flow.name(), FlowEnd.SOURCE, edge.x(), edge.y());
                }
            }

            // Check sink endpoint
            if (flow.sink() != null && canvasState.hasElement(flow.sink())) {
                FlowGeometry.Point2D edge = FlowGeometry.clipToElement(
                        canvasState, flow.sink(), midX, midY);
                double dist = Math.hypot(worldX - edge.x(), worldY - edge.y());
                if (dist <= ENDPOINT_HIT_RADIUS) {
                    return new CloudHit(flow.name(), FlowEnd.SINK, edge.x(), edge.y());
                }
            }
        }
        return null;
    }

    /**
     * Computes the cloud position for a disconnected endpoint.
     * The cloud is placed at LayoutMetrics.CLOUD_OFFSET distance from the diamond center,
     * in the direction away from the opposite endpoint (if connected).
     */
    private static FlowGeometry.Point2D computeCloudPosition(double midX, double midY,
                                                              String oppositeStock,
                                                              CanvasState canvasState,
                                                              boolean isSource) {
        double dx;
        double dy;

        if (oppositeStock != null && canvasState.hasElement(oppositeStock)) {
            double oppX = canvasState.getX(oppositeStock);
            double oppY = canvasState.getY(oppositeStock);
            // Direction from opposite stock toward diamond — already points away
            // from the connected stock, which is correct for both source and sink clouds
            dx = midX - oppX;
            dy = midY - oppY;
        } else {
            // No opposite stock: default left for source, right for sink
            dx = isSource ? -LayoutMetrics.CLOUD_OFFSET : LayoutMetrics.CLOUD_OFFSET;
            dy = 0;
        }

        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 1) {
            dx = isSource ? -LayoutMetrics.CLOUD_OFFSET : LayoutMetrics.CLOUD_OFFSET;
            dy = 0;
            dist = LayoutMetrics.CLOUD_OFFSET;
        }

        return new FlowGeometry.Point2D(
                midX + dx / dist * LayoutMetrics.CLOUD_OFFSET,
                midY + dy / dist * LayoutMetrics.CLOUD_OFFSET);
    }
}
