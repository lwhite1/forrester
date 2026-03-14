package systems.courant.sd.app.canvas.renderers;

import systems.courant.sd.model.def.ElementType;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Map;
import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.LayoutMetrics;

/**
 * Draws small inline sparklines (trajectory previews) inside stock elements.
 * Sparklines show shape (growth, oscillation, plateau) rather than precise values,
 * occupying the lower portion of the stock rectangle.
 */
public final class SparklineRenderer {

    private static final double SPARKLINE_MARGIN_LEFT = 8;
    private static final double SPARKLINE_MARGIN_RIGHT = 8;
    private static final double SPARKLINE_MARGIN_BOTTOM = 6;
    private static final double SPARKLINE_HEIGHT = 16;
    private static final double LINE_WIDTH = 1.2;
    private static final Color SPARKLINE_COLOR = Color.web("#4A90D9", 0.7);
    private static final Color SPARKLINE_COLOR_STALE = Color.web("#4A90D9", 0.25);

    private SparklineRenderer() {
    }

    /**
     * Draws sparklines for all stock elements that have time-series data.
     *
     * @param gc           the graphics context
     * @param canvasState  canvas state for element positions/sizes
     * @param sparklines   map of stock name to time-series values
     * @param stale        if true, render at reduced opacity
     */
    public static void drawAll(GraphicsContext gc, CanvasState canvasState,
                               Map<String, double[]> sparklines, boolean stale) {
        if (sparklines == null || sparklines.isEmpty()) {
            return;
        }
        for (Map.Entry<String, double[]> entry : sparklines.entrySet()) {
            String name = entry.getKey();
            double[] values = entry.getValue();
            if (!canvasState.hasElement(name) || values.length < 2) {
                continue;
            }
            // Only draw sparklines on stock elements
            if (canvasState.getType(name).orElse(null) != ElementType.STOCK) {
                continue;
            }
            double cx = canvasState.getX(name);
            double cy = canvasState.getY(name);
            double w = LayoutMetrics.effectiveWidth(canvasState, name);
            double h = LayoutMetrics.effectiveHeight(canvasState, name);
            double x = cx - w / 2;
            double y = cy - h / 2;

            drawSparkline(gc, values, x, y, w, h, stale);
        }
    }

    /**
     * Draws a single sparkline inside the stock rectangle.
     */
    private static void drawSparkline(GraphicsContext gc, double[] values,
                                      double boxX, double boxY,
                                      double boxW, double boxH,
                                      boolean stale) {
        double sparkX = boxX + SPARKLINE_MARGIN_LEFT;
        double sparkW = boxW - SPARKLINE_MARGIN_LEFT - SPARKLINE_MARGIN_RIGHT;
        double sparkY = boxY + boxH - SPARKLINE_MARGIN_BOTTOM - SPARKLINE_HEIGHT;
        double sparkH = SPARKLINE_HEIGHT;

        if (sparkW < 10 || sparkH < 4) {
            return;
        }

        // Find min/max for scaling
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double v : values) {
            if (Double.isFinite(v)) {
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
        }
        if (min == Double.MAX_VALUE) {
            return; // all NaN/Infinity
        }

        double range = max - min;
        if (range < 1e-15) {
            // Flat line — draw at midpoint
            gc.setStroke(stale ? SPARKLINE_COLOR_STALE : SPARKLINE_COLOR);
            gc.setLineWidth(LINE_WIDTH);
            gc.setLineDashes();
            gc.strokeLine(sparkX, sparkY + sparkH / 2, sparkX + sparkW, sparkY + sparkH / 2);
            return;
        }

        // Draw polyline
        gc.setStroke(stale ? SPARKLINE_COLOR_STALE : SPARKLINE_COLOR);
        gc.setLineWidth(LINE_WIDTH);
        gc.setLineDashes();

        gc.beginPath();
        boolean started = false;
        for (int i = 0; i < values.length; i++) {
            if (!Double.isFinite(values[i])) {
                continue;
            }
            double px = sparkX + (double) i / (values.length - 1) * sparkW;
            double py = sparkY + sparkH - (values[i] - min) / range * sparkH;
            if (!started) {
                gc.moveTo(px, py);
                started = true;
            } else {
                gc.lineTo(px, py);
            }
        }
        gc.stroke();
    }
}
