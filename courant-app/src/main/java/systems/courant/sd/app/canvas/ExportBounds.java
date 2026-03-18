package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.FlowDef;

import java.util.List;

/**
 * Computes the world-space bounding box of a diagram for export.
 * Shared by {@link DiagramExporter} and {@link SvgExporter} to avoid duplication.
 */
final class ExportBounds {

    private static final double PADDING = 50;
    private static final double POLARITY_LABEL_T = 0.8;
    private static final double POLARITY_PERP_OFFSET = 12;
    private static final double POLARITY_LABEL_RADIUS = 10;
    private static final double SELF_LOOP_LABEL_Y_OFFSET = 10;

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
        if (canvasState.getDrawOrder().isEmpty() && editor.getFlows().isEmpty()
                && editor.getCausalLinks().isEmpty()) {
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

        // Include causal link polarity label positions
        List<CausalLinkDef> allLinks = editor.getCausalLinks();
        for (CausalLinkDef link : allLinks) {
            String fromName = link.from();
            String toName = link.to();
            if (!canvasState.hasElement(fromName) || !canvasState.hasElement(toName)) {
                continue;
            }

            double fromX = canvasState.getX(fromName);
            double fromY = canvasState.getY(fromName);

            if (fromName.equals(toName)) {
                // Self-loop: label at cubic midpoint with y offset
                double halfW = LayoutMetrics.effectiveWidth(canvasState, fromName) / 2;
                double halfH = LayoutMetrics.effectiveHeight(canvasState, fromName) / 2;
                double[] loopPts = CausalLinkGeometry.selfLoopPoints(fromX, fromY, halfW, halfH);
                double[] midPt = CausalLinkGeometry.evaluateCubic(
                        loopPts[0], loopPts[1], loopPts[2], loopPts[3],
                        loopPts[4], loopPts[5], loopPts[6], loopPts[7], 0.5);
                double labelX = midPt[0];
                double labelY = midPt[1] - SELF_LOOP_LABEL_Y_OFFSET;
                minX = Math.min(minX, labelX - POLARITY_LABEL_RADIUS);
                minY = Math.min(minY, labelY - POLARITY_LABEL_RADIUS);
                maxX = Math.max(maxX, labelX + POLARITY_LABEL_RADIUS);
                maxY = Math.max(maxY, labelY + POLARITY_LABEL_RADIUS);
                continue;
            }

            double toX = canvasState.getX(toName);
            double toY = canvasState.getY(toName);

            double dx = toX - fromX;
            double dy = toY - fromY;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist <= 1) {
                continue;
            }

            CausalLinkGeometry.ControlPoint cp = CausalLinkGeometry.controlPoint(
                    fromX, fromY, toX, toY, fromName, toName, allLinks);
            double[] labelPt = CausalLinkGeometry.evaluate(
                    fromX, fromY, cp.x(), cp.y(), toX, toY, POLARITY_LABEL_T);
            double[] labelTan = CausalLinkGeometry.tangent(
                    fromX, fromY, cp.x(), cp.y(), toX, toY, POLARITY_LABEL_T);
            double tanDist = Math.sqrt(labelTan[0] * labelTan[0] + labelTan[1] * labelTan[1]);
            if (tanDist > 0) {
                double perpX = -labelTan[1] / tanDist;
                double perpY = labelTan[0] / tanDist;
                double labelX = labelPt[0] + perpX * POLARITY_PERP_OFFSET;
                double labelY = labelPt[1] + perpY * POLARITY_PERP_OFFSET;
                minX = Math.min(minX, labelX - POLARITY_LABEL_RADIUS);
                minY = Math.min(minY, labelY - POLARITY_LABEL_RADIUS);
                maxX = Math.max(maxX, labelX + POLARITY_LABEL_RADIUS);
                maxY = Math.max(maxY, labelY + POLARITY_LABEL_RADIUS);
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
