package systems.courant.sd.app.canvas.controllers;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.CausalLinkGeometry;
import systems.courant.sd.app.canvas.ConnectionId;
import systems.courant.sd.app.canvas.FlowGeometry;
import systems.courant.sd.model.def.CausalLinkDef;

import java.util.List;

/**
 * Controller for dragging the curvature handle on a selected causal link.
 * The handle appears at the midpoint (t=0.5) of the quadratic Bézier curve.
 * Dragging it adjusts the link's strength value by projecting the drag
 * position onto the same direction axis the renderer uses.
 */
public final class CausalLinkDragController {

    /** Pixel radius for hit-testing the curve handle. */
    public static final double HANDLE_HIT_RADIUS = 8;

    /** Visual radius of the drawn handle. */
    public static final double HANDLE_RADIUS = 5;

    private boolean active;
    private String fromName;
    private String toName;

    // Direction unit vector and chord midpoint, captured at drag start
    private double dirX;
    private double dirY;
    private double midX;
    private double midY;

    /**
     * Computes the position of the drag handle for a selected causal link.
     * Returns the point at t=0.5 on the Bézier curve, or null if the link
     * is a self-loop or endpoints are missing.
     */
    public static double[] handlePosition(ConnectionId connection,
                                           CanvasState state,
                                           List<CausalLinkDef> allLinks,
                                           CausalLinkGeometry.LoopContext loopCtx) {
        String from = connection.from();
        String to = connection.to();
        if (from.equals(to) || !state.hasElement(from) || !state.hasElement(to)) {
            return null;
        }

        double fromX = state.getX(from);
        double fromY = state.getY(from);
        double toX = state.getX(to);
        double toY = state.getY(to);

        CausalLinkGeometry.ControlPoint cp = CausalLinkGeometry.controlPoint(
                fromX, fromY, toX, toY, from, to, allLinks, loopCtx);

        FlowGeometry.Point2D clippedFrom = FlowGeometry.clipToElement(state, from, cp.x(), cp.y());
        FlowGeometry.Point2D clippedTo = FlowGeometry.clipToElement(state, to, cp.x(), cp.y());

        return CausalLinkGeometry.evaluate(
                clippedFrom.x(), clippedFrom.y(),
                cp.x(), cp.y(),
                clippedTo.x(), clippedTo.y(), 0.5);
    }

    /**
     * Tests if a world-coordinate point hits the drag handle of the selected causal link.
     *
     * @return true if the point is within {@link #HANDLE_HIT_RADIUS} of the handle
     */
    public static boolean hitTestHandle(double worldX, double worldY,
                                        ConnectionId connection,
                                        CanvasState state,
                                        List<CausalLinkDef> allLinks,
                                        CausalLinkGeometry.LoopContext loopCtx) {
        double[] pos = handlePosition(connection, state, allLinks, loopCtx);
        if (pos == null) {
            return false;
        }
        double dx = worldX - pos[0];
        double dy = worldY - pos[1];
        return dx * dx + dy * dy <= HANDLE_HIT_RADIUS * HANDLE_HIT_RADIUS;
    }

    /**
     * Begins a handle drag for the given causal link.
     * Derives the projection axis from the auto-computed control point
     * (the same algorithm the renderer uses), so the drag direction
     * matches what the user sees on screen.
     */
    public void start(ConnectionId connection, CanvasState state,
                      List<CausalLinkDef> allLinks,
                      CausalLinkGeometry.LoopContext loopCtx) {
        this.fromName = connection.from();
        this.toName = connection.to();
        this.active = true;

        double fromX = state.getX(fromName);
        double fromY = state.getY(fromName);
        double toX = state.getX(toName);
        double toY = state.getY(toName);

        this.midX = (fromX + toX) / 2;
        this.midY = (fromY + toY) / 2;

        // Compute the auto control point using the same algorithm as the renderer,
        // temporarily ignoring any existing strength override so we get the
        // natural direction vector.
        List<CausalLinkDef> autoLinks = stripStrength(fromName, toName, allLinks);
        CausalLinkGeometry.ControlPoint autoCP = CausalLinkGeometry.controlPoint(
                fromX, fromY, toX, toY, fromName, toName, autoLinks, loopCtx);

        double ax = autoCP.x() - midX;
        double ay = autoCP.y() - midY;
        double aDist = Math.sqrt(ax * ax + ay * ay);

        if (aDist < 0.001) {
            // Degenerate — fall back to simple perpendicular
            double dx = toX - fromX;
            double dy = toY - fromY;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < 1) {
                this.dirX = 0;
                this.dirY = -1;
            } else {
                this.dirX = -dy / dist;
                this.dirY = dx / dist;
            }
        } else {
            this.dirX = ax / aDist;
            this.dirY = ay / aDist;
        }
    }

    /**
     * Updates the strength based on the current drag position.
     * Projects the drag point onto the direction axis derived at start.
     * Negative values reverse the curve direction.
     *
     * @return the computed strength value
     */
    public double drag(double worldX, double worldY) {
        double dx = worldX - midX;
        double dy = worldY - midY;
        return dx * dirX + dy * dirY;
    }

    /**
     * Ends the drag interaction.
     */
    public void cancel() {
        this.active = false;
        this.fromName = null;
        this.toName = null;
    }

    public boolean isActive() {
        return active;
    }

    public String getFromName() {
        return fromName;
    }

    public String getToName() {
        return toName;
    }

    /**
     * Returns a copy of the link list with the strength stripped from the
     * specific link, so we can compute the auto control point direction.
     */
    private static List<CausalLinkDef> stripStrength(String from, String to,
                                                      List<CausalLinkDef> allLinks) {
        return allLinks.stream()
                .map(link -> {
                    if (link.from().equals(from) && link.to().equals(to)
                            && link.hasStrength()) {
                        return new CausalLinkDef(link.from(), link.to(),
                                link.polarity(), link.comment());
                    }
                    return link;
                })
                .toList();
    }
}
