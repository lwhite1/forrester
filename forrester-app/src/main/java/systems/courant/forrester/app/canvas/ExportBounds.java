package systems.courant.forrester.app.canvas;

import systems.courant.forrester.model.def.ElementType;
import systems.courant.forrester.model.def.FlowDef;

/**
 * Computes the world-space bounding box of a diagram for export.
 * Shared by {@link DiagramExporter} and {@link SvgExporter} to avoid duplication.
 */
final class ExportBounds {

    private static final double PADDING = 50;

    /**
     * Padded world-space bounding box of a diagram.
     */
    record Bounds(double minX, double minY, double width, double height) {}

    private ExportBounds() {
    }

    /**
     * Computes the padded bounding box encompassing all elements and cloud positions.
     * Includes flow label areas below diamond indicators.
     */
    static Bounds compute(CanvasState canvasState, ModelEditor editor) {
        if (canvasState.getDrawOrder().isEmpty() && editor.getFlows().isEmpty()) {
            return new Bounds(0, 0, 2 * PADDING, 2 * PADDING);
        }

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (String name : canvasState.getDrawOrder()) {
            double cx = canvasState.getX(name);
            double cy = canvasState.getY(name);
            double halfW = LayoutMetrics.effectiveWidth(canvasState, name) / 2;
            double halfH = LayoutMetrics.effectiveHeight(canvasState, name) / 2;

            // For flows, include the label area below the diamond
            if (canvasState.getType(name).orElse(null) == ElementType.FLOW) {
                halfH = LayoutMetrics.FLOW_INDICATOR_SIZE / 2
                        + LayoutMetrics.FLOW_EQUATION_GAP + 12;
            }

            minX = Math.min(minX, cx - halfW);
            minY = Math.min(minY, cy - halfH);
            maxX = Math.max(maxX, cx + halfW);
            maxY = Math.max(maxY, cy + halfH);
        }

        // Include cloud positions
        double cloudR = LayoutMetrics.CLOUD_OFFSET / 4;
        for (FlowDef flow : editor.getFlows()) {
            FlowGeometry.Point2D sourceCloud = FlowEndpointCalculator.cloudPosition(
                    FlowEndpointCalculator.FlowEnd.SOURCE, flow, canvasState);
            if (sourceCloud != null) {
                minX = Math.min(minX, sourceCloud.x() - cloudR);
                minY = Math.min(minY, sourceCloud.y() - cloudR);
                maxX = Math.max(maxX, sourceCloud.x() + cloudR);
                maxY = Math.max(maxY, sourceCloud.y() + cloudR);
            }

            FlowGeometry.Point2D sinkCloud = FlowEndpointCalculator.cloudPosition(
                    FlowEndpointCalculator.FlowEnd.SINK, flow, canvasState);
            if (sinkCloud != null) {
                minX = Math.min(minX, sinkCloud.x() - cloudR);
                minY = Math.min(minY, sinkCloud.y() - cloudR);
                maxX = Math.max(maxX, sinkCloud.x() + cloudR);
                maxY = Math.max(maxY, sinkCloud.y() + cloudR);
            }
        }

        // Add padding
        minX -= PADDING;
        minY -= PADDING;
        maxX += PADDING;
        maxY += PADDING;

        return new Bounds(minX, minY, maxX - minX, maxY - minY);
    }
}
