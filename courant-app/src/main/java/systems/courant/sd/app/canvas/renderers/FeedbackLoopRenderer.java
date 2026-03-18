package systems.courant.sd.app.canvas.renderers;

import systems.courant.sd.model.graph.FeedbackAnalysis.LoopType;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.CausalLinkGeometry;
import systems.courant.sd.app.canvas.ColorPalette;
import systems.courant.sd.app.canvas.LayoutMetrics;

/**
 * Draws visual highlights for elements and edges that participate in feedback loops.
 * Follows the same static-method pattern as {@link SelectionRenderer}.
 */
public final class FeedbackLoopRenderer {

    private static final double GLOW_PADDING = LayoutMetrics.LOOP_GLOW_PADDING;
    private static final double GLOW_LINE_WIDTH = LayoutMetrics.LOOP_GLOW_LINE_WIDTH;
    private static final double EDGE_LINE_WIDTH = LayoutMetrics.LOOP_EDGE_LINE_WIDTH;
    private static final Font LOOP_LABEL_FONT = Font.font("System", FontWeight.BOLD,
            LayoutMetrics.LOOP_LABEL_FONT_SIZE);
    private static final double LABEL_PADDING = LayoutMetrics.LOOP_LABEL_PADDING;

    private FeedbackLoopRenderer() {
    }

    /**
     * Draws a colored glow/outline around a loop participant element.
     * Uses a solid border in the loop highlight color with a subtle fill.
     */
    public static void drawLoopHighlight(GraphicsContext gc, CanvasState state, String name) {
        gc.setLineDashes();
        OutlineGeometry.drawElementOutline(gc, state, name, GLOW_PADDING,
                ColorPalette.LOOP_FILL, ColorPalette.LOOP_HIGHLIGHT, GLOW_LINE_WIDTH);
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

        Text textNode = new Text(label);
        textNode.setFont(LOOP_LABEL_FONT);
        double textWidth = textNode.getLayoutBounds().getWidth();
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

        CausalLinkGeometry.strokeQuadCurve(gc, fromX, fromY, cpX, cpY, toX, toY, 1.0);
    }

    /**
     * Draws a cubic Bézier loop edge highlight for self-loop causal links.
     */
    public static void drawLoopEdgeCubic(GraphicsContext gc, double[] loopPts) {
        gc.setStroke(ColorPalette.LOOP_EDGE);
        gc.setLineWidth(EDGE_LINE_WIDTH);
        gc.setLineDashes();

        CausalLinkGeometry.strokeCubicCurve(gc,
                loopPts[0], loopPts[1], loopPts[2], loopPts[3],
                loopPts[4], loopPts[5], loopPts[6], loopPts[7], 1.0);
    }
}
