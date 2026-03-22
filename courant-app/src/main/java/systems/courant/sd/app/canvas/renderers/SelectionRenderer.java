package systems.courant.sd.app.canvas.renderers;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.CausalLinkGeometry;
import systems.courant.sd.app.canvas.ColorPalette;

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
        OutlineGeometry.Shape shape = OutlineGeometry.resolve(state, name, SELECTION_PADDING);
        if (shape == null) {
            return;
        }

        gc.setStroke(SELECTION_COLOR);
        gc.setLineWidth(SELECTION_LINE_WIDTH);
        gc.setLineDashes(SELECTION_DASH_LENGTH, SELECTION_DASH_GAP);

        switch (shape) {
            case OutlineGeometry.Diamond d -> drawDiamondIndicator(gc, d);
            case OutlineGeometry.Rect r -> drawRectIndicator(gc, r);
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
     * Draws a cubic Bézier hover highlight for a self-loop causal link.
     */
    public static void drawConnectionHoverCubic(GraphicsContext gc, double[] pts) {
        gc.setStroke(ColorPalette.HOVER);
        gc.setLineWidth(3.0);
        gc.setLineDashes();
        CausalLinkGeometry.strokeCubicCurve(gc,
                pts[0], pts[1], pts[2], pts[3],
                pts[4], pts[5], pts[6], pts[7], 1.0);
    }

    /**
     * Draws a cubic Bézier selection highlight for a self-loop causal link.
     */
    public static void drawConnectionSelectionCubic(GraphicsContext gc, double[] pts) {
        gc.setStroke(SELECTION_COLOR);
        gc.setLineWidth(3.0);
        gc.setLineDashes(SELECTION_DASH_LENGTH, SELECTION_DASH_GAP);
        CausalLinkGeometry.strokeCubicCurve(gc,
                pts[0], pts[1], pts[2], pts[3],
                pts[4], pts[5], pts[6], pts[7], 1.0);
        gc.setLineDashes();
    }

    /**
     * Draws a hover indicator around the named element.
     * Uses a solid outline (no dashes, no handles) to distinguish from selection.
     */
    public static void drawHoverIndicator(GraphicsContext gc, CanvasState state, String name) {
        OutlineGeometry.strokeElementOutline(gc, state, name, SELECTION_PADDING,
                ColorPalette.HOVER, HOVER_LINE_WIDTH);
    }

    private static void drawRectIndicator(GraphicsContext gc, OutlineGeometry.Rect r) {
        gc.strokeRect(r.x(), r.y(), r.w(), r.h());

        // Corner handles
        gc.setFill(SELECTION_COLOR);
        drawHandle(gc, r.x(), r.y());
        drawHandle(gc, r.x() + r.w(), r.y());
        drawHandle(gc, r.x(), r.y() + r.h());
        drawHandle(gc, r.x() + r.w(), r.y() + r.h());
    }

    private static void drawDiamondIndicator(GraphicsContext gc, OutlineGeometry.Diamond d) {
        gc.strokePolygon(d.xPoints(), d.yPoints(), 4);

        // Corner handles at diamond tips
        gc.setFill(SELECTION_COLOR);
        drawHandle(gc, d.cx(), d.cy() - d.half());
        drawHandle(gc, d.cx() + d.half(), d.cy());
        drawHandle(gc, d.cx(), d.cy() + d.half());
        drawHandle(gc, d.cx() - d.half(), d.cy());
    }

    /**
     * Draws a circular drag handle for adjusting causal link curvature.
     */
    public static void drawCurveHandle(GraphicsContext gc, double x, double y, double radius) {
        gc.setFill(Color.WHITE);
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        gc.setStroke(SELECTION_COLOR);
        gc.setLineWidth(1.5);
        gc.setLineDashes();
        gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);
    }

    private static void drawHandle(GraphicsContext gc, double x, double y) {
        gc.fillRect(x - HANDLE_SIZE / 2, y - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);
    }
}
