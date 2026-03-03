package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ElementType;

import java.util.List;

/**
 * Performs hit testing against canvas elements in world coordinates.
 * Tests in reverse draw order so that topmost-drawn elements are hit first.
 */
public final class HitTester {

    private HitTester() {
    }

    /**
     * Returns the name of the element at the given world coordinates,
     * or null if no element is hit. Tests in reverse draw order.
     */
    public static String hitTest(CanvasState state, double worldX, double worldY) {
        List<String> drawOrder = state.getDrawOrder();

        for (int i = drawOrder.size() - 1; i >= 0; i--) {
            String name = drawOrder.get(i);
            ElementType type = state.getType(name);
            double cx = state.getX(name);
            double cy = state.getY(name);

            if (type == null || Double.isNaN(cx) || Double.isNaN(cy)) {
                continue;
            }

            if (type == ElementType.FLOW) {
                if (hitTestRect(worldX, worldY, cx, cy,
                        LayoutMetrics.FLOW_HIT_HALF_WIDTH,
                        LayoutMetrics.FLOW_HIT_HALF_HEIGHT)) {
                    return name;
                }
            } else {
                double halfW = LayoutMetrics.widthFor(type) / 2;
                double halfH = LayoutMetrics.heightFor(type) / 2;
                if (hitTestRect(worldX, worldY, cx, cy, halfW, halfH)) {
                    return name;
                }
            }
        }

        return null;
    }

    /**
     * Rectangular AABB hit test: checks if (worldX, worldY) is within
     * a rectangle centered at (cx, cy) with given half-width and half-height.
     */
    static boolean hitTestRect(double worldX, double worldY,
                               double cx, double cy,
                               double halfW, double halfH) {
        return Math.abs(worldX - cx) <= halfW && Math.abs(worldY - cy) <= halfH;
    }

    /**
     * Diamond (Manhattan distance) hit test: checks if (worldX, worldY) is within
     * a diamond centered at (cx, cy) with the given half-size.
     */
    static boolean hitTestDiamond(double worldX, double worldY,
                                  double cx, double cy,
                                  double half) {
        return Math.abs(worldX - cx) + Math.abs(worldY - cy) <= half;
    }
}
