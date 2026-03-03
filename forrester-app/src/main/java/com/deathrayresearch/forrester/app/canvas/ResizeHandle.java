package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ElementType;

/**
 * Corner resize handles for selected elements on the canvas.
 */
public enum ResizeHandle {

    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT;

    /**
     * Result of a resize handle hit test.
     */
    public record HandleHit(String elementName, ResizeHandle handle) {}

    private static final double HIT_RADIUS = 5;

    /**
     * Hit-tests all selected non-flow elements' corner handle positions.
     * Returns the first handle hit, or null if no handle is under the cursor.
     */
    public static HandleHit hitTest(CanvasState state, double worldX, double worldY) {
        for (String name : state.getSelection()) {
            ElementType type = state.getType(name);
            if (type == null || type == ElementType.FLOW) {
                continue;
            }

            double cx = state.getX(name);
            double cy = state.getY(name);
            if (Double.isNaN(cx) || Double.isNaN(cy)) {
                continue;
            }

            double halfW = LayoutMetrics.effectiveWidth(state, name) / 2
                    + SelectionRenderer.SELECTION_PADDING;
            double halfH = LayoutMetrics.effectiveHeight(state, name) / 2
                    + SelectionRenderer.SELECTION_PADDING;

            // Check each corner
            if (isNear(worldX, worldY, cx - halfW, cy - halfH)) {
                return new HandleHit(name, TOP_LEFT);
            }
            if (isNear(worldX, worldY, cx + halfW, cy - halfH)) {
                return new HandleHit(name, TOP_RIGHT);
            }
            if (isNear(worldX, worldY, cx - halfW, cy + halfH)) {
                return new HandleHit(name, BOTTOM_LEFT);
            }
            if (isNear(worldX, worldY, cx + halfW, cy + halfH)) {
                return new HandleHit(name, BOTTOM_RIGHT);
            }
        }
        return null;
    }

    private static boolean isNear(double wx, double wy, double hx, double hy) {
        return Math.abs(wx - hx) <= HIT_RADIUS && Math.abs(wy - hy) <= HIT_RADIUS;
    }
}
