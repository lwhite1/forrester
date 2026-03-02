package com.deathrayresearch.forrester.app.canvas;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Draws selection indicators (dashed outlines and corner handles) around selected elements.
 */
public final class SelectionRenderer {

    private static final Color SELECTION_COLOR = Color.web("#4A90D9", 0.8);
    private static final double SELECTION_PADDING = 4;
    private static final double SELECTION_LINE_WIDTH = 1.5;
    private static final double SELECTION_DASH_LENGTH = 5;
    private static final double SELECTION_DASH_GAP = 3;
    private static final double HANDLE_SIZE = 6;

    private SelectionRenderer() {
    }

    /**
     * Draws a selection indicator around the named element.
     * Uses a dashed rectangle for stock/aux/constant and a dashed diamond for flow.
     */
    public static void drawSelectionIndicator(GraphicsContext gc, CanvasState state, String name) {
        String type = state.getType(name);
        double cx = state.getX(name);
        double cy = state.getY(name);

        if (type == null || Double.isNaN(cx) || Double.isNaN(cy)) {
            return;
        }

        gc.setStroke(SELECTION_COLOR);
        gc.setLineWidth(SELECTION_LINE_WIDTH);
        gc.setLineDashes(SELECTION_DASH_LENGTH, SELECTION_DASH_GAP);

        if ("flow".equals(type)) {
            drawDiamondIndicator(gc, cx, cy);
        } else {
            double halfW = LayoutMetrics.widthFor(type) / 2 + SELECTION_PADDING;
            double halfH = LayoutMetrics.heightFor(type) / 2 + SELECTION_PADDING;
            drawRectIndicator(gc, cx, cy, halfW, halfH);
        }

        gc.setLineDashes();
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
