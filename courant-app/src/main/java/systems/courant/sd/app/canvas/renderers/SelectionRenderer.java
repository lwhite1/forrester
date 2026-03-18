package systems.courant.sd.app.canvas.renderers;

import systems.courant.sd.model.def.ElementType;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.CausalLinkGeometry;
import systems.courant.sd.app.canvas.ColorPalette;
import systems.courant.sd.app.canvas.LayoutMetrics;

/**
 * Draws selection indicators (dashed outlines and corner handles) around selected elements.
 */
public final class SelectionRenderer {

    private static final Color SELECTION_COLOR = ColorPalette.SELECTION;
    public static final double SELECTION_PADDING = 4;
    private static final double SELECTION_LINE_WIDTH = 1.5;
    private static final double SELECTION_DASH_LENGTH = 5;
    private static final double SELECTION_DASH_GAP = 3;
    public static final double HANDLE_SIZE = 6;

    private static final double HOVER_LINE_WIDTH = 1.0;

    private SelectionRenderer() {
    }

    /**
     * Draws a selection indicator around the named element.
     * Uses a dashed rectangle for stock/aux/constant and a dashed diamond for flow.
     */
    public static void drawSelectionIndicator(GraphicsContext gc, CanvasState state, String name) {
        ElementType type = state.getType(name).orElse(null);
        double cx = state.getX(name);
        double cy = state.getY(name);

        if (type == null || Double.isNaN(cx) || Double.isNaN(cy)) {
            return;
        }

        gc.setStroke(SELECTION_COLOR);
        gc.setLineWidth(SELECTION_LINE_WIDTH);
        gc.setLineDashes(SELECTION_DASH_LENGTH, SELECTION_DASH_GAP);

        if (type == ElementType.FLOW) {
            drawDiamondIndicator(gc, cx, cy);
        } else {
            double halfW = LayoutMetrics.effectiveWidth(state, name) / 2 + SELECTION_PADDING;
            double halfH = LayoutMetrics.effectiveHeight(state, name) / 2 + SELECTION_PADDING;
            drawRectIndicator(gc, cx, cy, halfW, halfH);
        }

        gc.setLineDashes();
    }

    /**
     * Draws a hover highlight on a connection line (solid, thicker, hover color).
     */
    public static void drawConnectionHover(GraphicsContext gc,
                                           double fromX, double fromY,
                                           double toX, double toY) {
        gc.setStroke(ColorPalette.HOVER);
        gc.setLineWidth(3.0);
        gc.setLineDashes();
        gc.strokeLine(fromX, fromY, toX, toY);
    }

    /**
     * Draws a selection indicator on a connection line (dashed, thicker, selection color).
     */
    public static void drawConnectionSelection(GraphicsContext gc,
                                               double fromX, double fromY,
                                               double toX, double toY) {
        gc.setStroke(SELECTION_COLOR);
        gc.setLineWidth(3.0);
        gc.setLineDashes(SELECTION_DASH_LENGTH, SELECTION_DASH_GAP);
        gc.strokeLine(fromX, fromY, toX, toY);
        gc.setLineDashes();
    }

    /**
     * Draws a curved hover highlight for a causal link (quadratic Bézier).
     */
    public static void drawConnectionHoverCurved(GraphicsContext gc,
                                                  double fromX, double fromY,
                                                  double cpX, double cpY,
                                                  double toX, double toY) {
        gc.setStroke(ColorPalette.HOVER);
        gc.setLineWidth(3.0);
        gc.setLineDashes();
        CausalLinkGeometry.strokeQuadCurve(gc, fromX, fromY, cpX, cpY, toX, toY, 1.0);
    }

    /**
     * Draws a curved selection highlight for a causal link (quadratic Bézier).
     */
    public static void drawConnectionSelectionCurved(GraphicsContext gc,
                                                      double fromX, double fromY,
                                                      double cpX, double cpY,
                                                      double toX, double toY) {
        gc.setStroke(SELECTION_COLOR);
        gc.setLineWidth(3.0);
        gc.setLineDashes(SELECTION_DASH_LENGTH, SELECTION_DASH_GAP);
        CausalLinkGeometry.strokeQuadCurve(gc, fromX, fromY, cpX, cpY, toX, toY, 1.0);
        gc.setLineDashes();
    }

    /**
     * Draws a hover indicator around the named element.
     * Uses a solid outline (no dashes, no handles) to distinguish from selection.
     */
    public static void drawHoverIndicator(GraphicsContext gc, CanvasState state, String name) {
        ElementType type = state.getType(name).orElse(null);
        double cx = state.getX(name);
        double cy = state.getY(name);

        if (type == null || Double.isNaN(cx) || Double.isNaN(cy)) {
            return;
        }

        gc.setStroke(ColorPalette.HOVER);
        gc.setLineWidth(HOVER_LINE_WIDTH);

        if (type == ElementType.FLOW) {
            double half = LayoutMetrics.FLOW_INDICATOR_SIZE / 2 + SELECTION_PADDING;
            double[] xPoints = {cx, cx + half, cx, cx - half};
            double[] yPoints = {cy - half, cy, cy + half, cy};
            gc.strokePolygon(xPoints, yPoints, 4);
        } else {
            double halfW = LayoutMetrics.effectiveWidth(state, name) / 2 + SELECTION_PADDING;
            double halfH = LayoutMetrics.effectiveHeight(state, name) / 2 + SELECTION_PADDING;
            gc.strokeRect(cx - halfW, cy - halfH, halfW * 2, halfH * 2);
        }
    }

    private static void drawRectIndicator(GraphicsContext gc, double cx, double cy,
                                          double halfW, double halfH) {
        double x = cx - halfW;
        double y = cy - halfH;
        double w = halfW * 2;
        double h = halfH * 2;

        gc.strokeRect(x, y, w, h);

        // Corner handles
        gc.setFill(SELECTION_COLOR);
        drawHandle(gc, x, y);
        drawHandle(gc, x + w, y);
        drawHandle(gc, x, y + h);
        drawHandle(gc, x + w, y + h);
    }

    private static void drawDiamondIndicator(GraphicsContext gc, double cx, double cy) {
        double half = LayoutMetrics.FLOW_INDICATOR_SIZE / 2 + SELECTION_PADDING;

        double[] xPoints = {cx, cx + half, cx, cx - half};
        double[] yPoints = {cy - half, cy, cy + half, cy};
        gc.strokePolygon(xPoints, yPoints, 4);

        // Corner handles at diamond tips
        gc.setFill(SELECTION_COLOR);
        drawHandle(gc, cx, cy - half);
        drawHandle(gc, cx + half, cy);
        drawHandle(gc, cx, cy + half);
        drawHandle(gc, cx - half, cy);
    }

    private static void drawHandle(GraphicsContext gc, double x, double y) {
        gc.fillRect(x - HANDLE_SIZE / 2, y - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);
    }
}
