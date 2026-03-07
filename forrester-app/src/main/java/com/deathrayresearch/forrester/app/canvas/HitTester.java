package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.CausalLinkDef;
import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.ElementType;

import java.util.List;

/**
 * Performs hit testing against canvas elements and connections in world coordinates.
 * Tests in reverse draw order so that topmost-drawn elements are hit first.
 */
public final class HitTester {

    private static final double CONNECTION_HIT_TOLERANCE = 6.0;

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
            ElementType type = state.getType(name).orElse(null);
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
                double halfW = LayoutMetrics.effectiveWidth(state, name) / 2;
                double halfH = LayoutMetrics.effectiveHeight(state, name) / 2;
                if (hitTestRect(worldX, worldY, cx, cy, halfW, halfH)) {
                    return name;
                }
            }
        }

        return null;
    }

    /**
     * Returns the ConnectionId of the info link at the given world coordinates,
     * or null if no connection is hit. Tests in reverse order (last-drawn first).
     * Clips endpoints to element borders before computing distance.
     */
    public static ConnectionId hitTestInfoLink(CanvasState state,
                                               List<ConnectorRoute> connectors,
                                               double worldX, double worldY) {
        for (int i = connectors.size() - 1; i >= 0; i--) {
            ConnectorRoute route = connectors.get(i);
            String fromName = route.from();
            String toName = route.to();

            if (!state.hasElement(fromName) || !state.hasElement(toName)) {
                continue;
            }

            double fromX = state.getX(fromName);
            double fromY = state.getY(fromName);
            double toX = state.getX(toName);
            double toY = state.getY(toName);

            FlowGeometry.Point2D clippedFrom = FlowGeometry.clipToElement(state, fromName, toX, toY);
            FlowGeometry.Point2D clippedTo = FlowGeometry.clipToElement(state, toName, fromX, fromY);

            double dist = pointToSegmentDistance(worldX, worldY,
                    clippedFrom.x(), clippedFrom.y(), clippedTo.x(), clippedTo.y());

            if (dist <= CONNECTION_HIT_TOLERANCE) {
                return new ConnectionId(fromName, toName);
            }
        }

        return null;
    }

    /**
     * Returns the ConnectionId of the causal link at the given world coordinates,
     * or null if no causal link is hit. Tests in reverse order (last-drawn first).
     * Uses curved (quadratic Bézier) distance for hit-testing.
     */
    public static ConnectionId hitTestCausalLink(CanvasState state,
                                                  List<CausalLinkDef> causalLinks,
                                                  double worldX, double worldY) {
        for (int i = causalLinks.size() - 1; i >= 0; i--) {
            CausalLinkDef link = causalLinks.get(i);
            String fromName = link.from();
            String toName = link.to();

            if (!state.hasElement(fromName) || !state.hasElement(toName)) {
                continue;
            }

            double fromX = state.getX(fromName);
            double fromY = state.getY(fromName);

            double dist;

            if (fromName.equals(toName)) {
                // Self-loop: hit-test against cubic Bézier
                double halfW = LayoutMetrics.effectiveWidth(state, fromName) / 2;
                double halfH = LayoutMetrics.effectiveHeight(state, fromName) / 2;
                double[] lp = CausalLinkGeometry.selfLoopPoints(fromX, fromY, halfW, halfH);
                dist = CausalLinkGeometry.pointToCubicDistance(worldX, worldY,
                        lp[0], lp[1], lp[2], lp[3], lp[4], lp[5], lp[6], lp[7]);
            } else {
                double toX = state.getX(toName);
                double toY = state.getY(toName);

                CausalLinkGeometry.ControlPoint cp = CausalLinkGeometry.controlPoint(
                        fromX, fromY, toX, toY, fromName, toName, causalLinks);

                FlowGeometry.Point2D clippedFrom = FlowGeometry.clipToElement(
                        state, fromName, cp.x(), cp.y());
                FlowGeometry.Point2D clippedTo = FlowGeometry.clipToElement(
                        state, toName, cp.x(), cp.y());

                dist = CausalLinkGeometry.pointToCurveDistance(worldX, worldY,
                        clippedFrom.x(), clippedFrom.y(),
                        cp.x(), cp.y(),
                        clippedTo.x(), clippedTo.y());
            }

            if (dist <= CONNECTION_HIT_TOLERANCE) {
                return new ConnectionId(fromName, toName);
            }
        }

        return null;
    }

    /**
     * Computes the minimum distance from point (px, py) to the line segment
     * from (x1, y1) to (x2, y2) using projection clamping.
     */
    static double pointToSegmentDistance(double px, double py,
                                         double x1, double y1,
                                         double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSq = dx * dx + dy * dy;

        if (lengthSq == 0) {
            // Degenerate segment (zero length)
            double ddx = px - x1;
            double ddy = py - y1;
            return Math.sqrt(ddx * ddx + ddy * ddy);
        }

        // Project point onto the line, clamped to [0, 1]
        double t = ((px - x1) * dx + (py - y1) * dy) / lengthSq;
        t = Math.max(0, Math.min(1, t));

        double closestX = x1 + t * dx;
        double closestY = y1 + t * dy;

        double distX = px - closestX;
        double distY = py - closestY;
        return Math.sqrt(distX * distX + distY * distY);
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
}
