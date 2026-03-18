package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.FlowDef;

/**
 * Resolved source and sink endpoints for a material flow pipe. Encapsulates
 * the endpoint-resolution logic that was previously duplicated between
 * {@link systems.courant.sd.app.canvas.renderers.MaterialFlowPass} and
 * {@link SvgExporter}.
 *
 * <p>For each endpoint (source and sink), the position is either clipped to
 * the connected stock's border or placed at the cloud position computed by
 * {@link FlowEndpointCalculator}.
 *
 * @param sourceX       resolved source endpoint x
 * @param sourceY       resolved source endpoint y
 * @param midX          flow indicator (diamond) center x
 * @param midY          flow indicator (diamond) center y
 * @param sinkX         resolved sink endpoint x
 * @param sinkY         resolved sink endpoint y
 * @param sourceIsCloud true if the source endpoint is a cloud (disconnected)
 * @param sinkIsCloud   true if the sink endpoint is a cloud (disconnected)
 */
public record MaterialFlowEndpoints(
        double sourceX, double sourceY,
        double midX, double midY,
        double sinkX, double sinkY,
        boolean sourceIsCloud, boolean sinkIsCloud
) {

    /**
     * Resolves the endpoints for a material flow.
     *
     * @param flow  the flow definition
     * @param state canvas state for position lookups
     * @return the resolved endpoints, or {@code null} if the flow element is not on the canvas
     */
    public static MaterialFlowEndpoints resolve(FlowDef flow, CanvasState state) {
        if (!state.hasElement(flow.name())) {
            return null;
        }

        double midX = state.getX(flow.name());
        double midY = state.getY(flow.name());

        double sourceX;
        double sourceY;
        boolean sourceIsCloud;

        if (flow.source() != null && state.hasElement(flow.source())) {
            FlowGeometry.Point2D edge = FlowGeometry.clipToElement(state, flow.source(), midX, midY);
            sourceX = edge.x();
            sourceY = edge.y();
            sourceIsCloud = false;
        } else {
            FlowGeometry.Point2D cloud = FlowEndpointCalculator.cloudPosition(
                    FlowEndpointCalculator.FlowEnd.SOURCE, flow, state);
            sourceX = cloud != null ? cloud.x() : midX - LayoutMetrics.CLOUD_OFFSET;
            sourceY = cloud != null ? cloud.y() : midY;
            sourceIsCloud = true;
        }

        double sinkX;
        double sinkY;
        boolean sinkIsCloud;

        if (flow.sink() != null && state.hasElement(flow.sink())) {
            FlowGeometry.Point2D edge = FlowGeometry.clipToElement(state, flow.sink(), midX, midY);
            sinkX = edge.x();
            sinkY = edge.y();
            sinkIsCloud = false;
        } else {
            FlowGeometry.Point2D cloud = FlowEndpointCalculator.cloudPosition(
                    FlowEndpointCalculator.FlowEnd.SINK, flow, state);
            sinkX = cloud != null ? cloud.x() : midX + LayoutMetrics.CLOUD_OFFSET;
            sinkY = cloud != null ? cloud.y() : midY;
            sinkIsCloud = true;
        }

        return new MaterialFlowEndpoints(sourceX, sourceY, midX, midY, sinkX, sinkY,
                sourceIsCloud, sinkIsCloud);
    }
}
