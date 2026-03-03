package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.FlowDef;

/**
 * Computes cloud positions for disconnected flow endpoints and hit-tests them.
 * Used by both the renderer (to draw clouds) and the interaction layer (to start reattachment drags).
 */
public final class FlowEndpointCalculator {

    private FlowEndpointCalculator() {
    }

    private static final double CLOUD_OFFSET = 80;
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
     * @return {x, y} of the cloud, or null if the endpoint is connected
     */
    public static double[] cloudPosition(FlowEnd end, FlowDef flow, CanvasState canvasState) {
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
            double[] sourceCloud = cloudPosition(FlowEnd.SOURCE, flow, canvasState);
            if (sourceCloud != null) {
                double dist = Math.hypot(worldX - sourceCloud[0], worldY - sourceCloud[1]);
                if (dist <= CLOUD_HIT_RADIUS) {
                    return new CloudHit(flow.name(), FlowEnd.SOURCE,
                            sourceCloud[0], sourceCloud[1]);
                }
            }

            double[] sinkCloud = cloudPosition(FlowEnd.SINK, flow, canvasState);
            if (sinkCloud != null) {
                double dist = Math.hypot(worldX - sinkCloud[0], worldY - sinkCloud[1]);
                if (dist <= CLOUD_HIT_RADIUS) {
                    return new CloudHit(flow.name(), FlowEnd.SINK,
                            sinkCloud[0], sinkCloud[1]);
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
                double scx = canvasState.getX(flow.source());
                double scy = canvasState.getY(flow.source());
                double[] edge = CanvasRenderer.clipToBorder(scx, scy,
                        LayoutMetrics.STOCK_WIDTH / 2, LayoutMetrics.STOCK_HEIGHT / 2,
                        midX, midY);
                double dist = Math.hypot(worldX - edge[0], worldY - edge[1]);
                if (dist <= ENDPOINT_HIT_RADIUS) {
                    return new CloudHit(flow.name(), FlowEnd.SOURCE, edge[0], edge[1]);
                }
            }

            // Check sink endpoint
            if (flow.sink() != null && canvasState.hasElement(flow.sink())) {
                double scx = canvasState.getX(flow.sink());
                double scy = canvasState.getY(flow.sink());
                double[] edge = CanvasRenderer.clipToBorder(scx, scy,
                        LayoutMetrics.STOCK_WIDTH / 2, LayoutMetrics.STOCK_HEIGHT / 2,
                        midX, midY);
                double dist = Math.hypot(worldX - edge[0], worldY - edge[1]);
                if (dist <= ENDPOINT_HIT_RADIUS) {
                    return new CloudHit(flow.name(), FlowEnd.SINK, edge[0], edge[1]);
                }
            }
        }
        return null;
    }

    /**
     * Computes the cloud position for a disconnected endpoint.
     * The cloud is placed at CLOUD_OFFSET distance from the diamond center,
     * in the direction away from the opposite endpoint (if connected).
     */
    private static double[] computeCloudPosition(double midX, double midY,
                                                  String oppositeStock,
                                                  CanvasState canvasState,
                                                  boolean isSource) {
        double dx;
        double dy;

        if (oppositeStock != null && canvasState.hasElement(oppositeStock)) {
            double oppX = canvasState.getX(oppositeStock);
            double oppY = canvasState.getY(oppositeStock);
            // Direction away from the opposite endpoint
            if (isSource) {
                dx = midX - oppX;
                dy = midY - oppY;
            } else {
                dx = midX - oppX;
                dy = midY - oppY;
            }
            // For sink, we want the opposite direction (toward the sink side)
            if (!isSource) {
                dx = -dx;
                dy = -dy;
            }
        } else {
            // No opposite stock: default left for source, right for sink
            dx = isSource ? -CLOUD_OFFSET : CLOUD_OFFSET;
            dy = 0;
        }

        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 1) {
            dx = isSource ? -CLOUD_OFFSET : CLOUD_OFFSET;
            dy = 0;
            dist = CLOUD_OFFSET;
        }

        return new double[]{midX + dx / dist * CLOUD_OFFSET, midY + dy / dist * CLOUD_OFFSET};
    }
}
