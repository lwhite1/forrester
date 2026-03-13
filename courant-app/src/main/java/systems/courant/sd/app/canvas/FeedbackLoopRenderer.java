package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.graph.FeedbackAnalysis.LoopType;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Draws visual highlights for elements and edges that participate in feedback loops.
 * Follows the same static-method pattern as {@link SelectionRenderer}.
 */
public final class FeedbackLoopRenderer {

    private static final double GLOW_PADDING = 6;
    private static final double GLOW_LINE_WIDTH = 2.5;
    private static final double EDGE_LINE_WIDTH = 2.5;
    private static final Font LOOP_LABEL_FONT = Font.font("System", FontWeight.BOLD, 14);
    private static final double LABEL_PADDING = 6;

    private FeedbackLoopRenderer() {
    }

    /**
     * Draws a colored glow/outline around a loop participant element.
     * Uses a solid border in the loop highlight color with a subtle fill.
     */
    public static void drawLoopHighlight(GraphicsContext gc, CanvasState state, String name) {
        ElementType type = state.getType(name).orElse(null);
        double cx = state.getX(name);
        double cy = state.getY(name);

        if (type == null || Double.isNaN(cx) || Double.isNaN(cy)) {
            return;
        }

        gc.setStroke(ColorPalette.LOOP_HIGHLIGHT);
        gc.setLineWidth(GLOW_LINE_WIDTH);
        gc.setLineDashes();

        if (type == ElementType.FLOW) {
            double half = LayoutMetrics.FLOW_INDICATOR_SIZE / 2 + GLOW_PADDING;
            double[] xPoints = {cx, cx + half, cx, cx - half};
            double[] yPoints = {cy - half, cy, cy + half, cy};

            gc.setFill(ColorPalette.LOOP_FILL);
            gc.fillPolygon(xPoints, yPoints, 4);
            gc.strokePolygon(xPoints, yPoints, 4);
        } else {
            double halfW = LayoutMetrics.effectiveWidth(state, name) / 2 + GLOW_PADDING;
            double halfH = LayoutMetrics.effectiveHeight(state, name) / 2 + GLOW_PADDING;
            double x = cx - halfW;
            double y = cy - halfH;
            double w = halfW * 2;
            double h = halfH * 2;

            gc.setFill(ColorPalette.LOOP_FILL);
            gc.fillRect(x, y, w, h);
            gc.strokeRect(x, y, w, h);
        }
    }

    /**
     * Draws a loop classification label (e.g. "R1", "B2") at the given position.
     * Uses a rounded rectangle badge with the loop type's color.
     */
    public static void drawLoopLabel(GraphicsContext gc, String label, LoopType type,
                                     double cx, double cy) {
        Color color = switch (type) {
            case REINFORCING -> ColorPalette.LOOP_REINFORCING;
            case BALANCING -> ColorPalette.LOOP_BALANCING;
            case INDETERMINATE -> ColorPalette.LOOP_INDETERMINATE;
        };

        double textWidth = label.length() * 9;
        double badgeW = textWidth + LABEL_PADDING * 2;
        double badgeH = 20;
        double x = cx - badgeW / 2;
        double y = cy - badgeH / 2;

        // Badge background
        gc.setFill(Color.web("#FFFFFF", 0.9));
        gc.fillRoundRect(x, y, badgeW, badgeH, 8, 8);

        // Badge border
        gc.setStroke(color);
        gc.setLineWidth(1.5);
        gc.setLineDashes();
        gc.strokeRoundRect(x, y, badgeW, badgeH, 8, 8);

        // Label text
        gc.setFill(color);
        gc.setFont(LOOP_LABEL_FONT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(label, cx, cy);
    }

    /**
     * Draws a thicker, colored version of an info link for edges within feedback loops.
     * Should be drawn before normal info links so it appears as a glow behind them.
     */
    public static void drawLoopEdge(GraphicsContext gc,
                                    double fromX, double fromY,
                                    double toX, double toY) {
        gc.setStroke(ColorPalette.LOOP_EDGE);
        gc.setLineWidth(EDGE_LINE_WIDTH);
        gc.setLineDashes();
        gc.strokeLine(fromX, fromY, toX, toY);
    }

    /**
     * Draws a curved loop edge highlight for causal links (quadratic Bézier).
     */
    public static void drawLoopEdgeCurved(GraphicsContext gc,
                                           double fromX, double fromY,
                                           double cpX, double cpY,
                                           double toX, double toY) {
        gc.setStroke(ColorPalette.LOOP_EDGE);
        gc.setLineWidth(EDGE_LINE_WIDTH);
        gc.setLineDashes();

        gc.beginPath();
        gc.moveTo(fromX, fromY);
        int segments = 30;
        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            double[] pt = CausalLinkGeometry.evaluate(fromX, fromY, cpX, cpY, toX, toY, t);
            gc.lineTo(pt[0], pt[1]);
        }
        gc.stroke();
    }

    /**
     * Draws a cubic Bézier loop edge highlight for self-loop causal links.
     */
    public static void drawLoopEdgeCubic(GraphicsContext gc, double[] loopPts) {
        gc.setStroke(ColorPalette.LOOP_EDGE);
        gc.setLineWidth(EDGE_LINE_WIDTH);
        gc.setLineDashes();

        gc.beginPath();
        gc.moveTo(loopPts[0], loopPts[1]);
        int segments = 30;
        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            double[] pt = CausalLinkGeometry.evaluateCubic(
                    loopPts[0], loopPts[1], loopPts[2], loopPts[3],
                    loopPts[4], loopPts[5], loopPts[6], loopPts[7], t);
            gc.lineTo(pt[0], pt[1]);
        }
        gc.stroke();
    }
}
