package systems.courant.sd.app.canvas.controllers;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.CausalLinkGeometry;
import systems.courant.sd.app.canvas.ConnectionId;
import systems.courant.sd.app.canvas.FlowGeometry;
import systems.courant.sd.app.canvas.LayoutMetrics;
import systems.courant.sd.model.def.CausalLinkDef;

import java.util.List;

/**
 * Controller for dragging the curvature handle on a selected causal link.
 * The handle appears at the midpoint (t=0.5) of the quadratic Bézier curve.
 * Dragging it perpendicular to the chord adjusts the link's strength value.
 */
public final class CausalLinkDragController {

    /** Pixel radius for hit-testing the curve handle. */
    public static final double HANDLE_HIT_RADIUS = 8;

    /** Visual radius of the drawn handle. */
    public static final double HANDLE_RADIUS = 5;

    private boolean active;
    private String fromName;
    private String toName;

    // Perpendicular unit vector and chord midpoint, captured at drag start
    private double perpX;
    private double perpY;
    private double midX;
    private double midY;
    private int direction;

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
     * Captures the chord geometry for projecting drag positions.
     */
    public void start(ConnectionId connection, CanvasState state,
                      List<CausalLinkDef> allLinks) {
        this.fromName = connection.from();
        this.toName = connection.to();
        this.active = true;

        double fromX = state.getX(fromName);
        double fromY = state.getY(fromName);
        double toX = state.getX(toName);
        double toY = state.getY(toName);

        double dx = toX - fromX;
        double dy = toY - fromY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        this.midX = (fromX + toX) / 2;
        this.midY = (fromY + toY) / 2;

        if (dist < 1) {
            this.perpX = 0;
            this.perpY = -1;
        } else {
            boolean canonical = fromName.compareTo(toName) < 0;
            double cdx = canonical ? dx : -dx;
            double cdy = canonical ? dy : -dy;
            this.perpX = -cdy / dist;
            this.perpY = cdx / dist;
        }

        this.direction = curveDirection(fromName, toName, allLinks);
    }

    /**
     * Updates the strength based on the current drag position.
     * Projects the drag point onto the perpendicular axis to compute strength.
     *
     * @return the computed strength value
     */
    public double drag(double worldX, double worldY) {
        double dx = worldX - midX;
        double dy = worldY - midY;
        double projection = (dx * perpX + dy * perpY) * direction;
        return Math.max(0, projection);
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

    private static int curveDirection(String fromName, String toName,
                                       List<CausalLinkDef> allLinks) {
        boolean hasReciprocal = false;
        for (CausalLinkDef link : allLinks) {
            if (link.from().equals(toName) && link.to().equals(fromName)) {
                hasReciprocal = true;
                break;
            }
        }
        if (!hasReciprocal) {
            return 1;
        }
        return fromName.compareTo(toName) < 0 ? 1 : -1;
    }
}
