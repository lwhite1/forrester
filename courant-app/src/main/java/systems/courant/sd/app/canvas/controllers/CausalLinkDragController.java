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
 * Dragging it adjusts both the link's strength (perpendicular offset) and
 * bias (parallel offset along the chord), giving the user full 2D control
 * over the curve shape.
 */
public final class CausalLinkDragController {

    /** Pixel radius for hit-testing the curve handle. */
    public static final double HANDLE_HIT_RADIUS = 8;

    /** Visual radius of the drawn handle. */
    public static final double HANDLE_RADIUS = 5;

    /**
     * Result of a drag operation: perpendicular strength and parallel bias.
     */
    public record DragResult(double strength, double bias) {
    }

    private boolean active;
    private String fromName;
    private String toName;

    // Direction unit vector (perpendicular) and chord midpoint, captured at drag start
    private double dirX;
    private double dirY;
    // Chord unit vector (parallel), captured at drag start
    private double chordUnitX;
    private double chordUnitY;
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

        return CausalLinkGeometry.evaluateAsArc(
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
     * Derives the perpendicular projection axis from the auto-computed control
     * point, and the parallel axis from the chord direction.
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

        // Chord unit vector (raw direction from source to target)
        double cdx = toX - fromX;
        double cdy = toY - fromY;
        double chordLen = Math.sqrt(cdx * cdx + cdy * cdy);
        if (chordLen >= 1) {
            this.chordUnitX = cdx / chordLen;
            this.chordUnitY = cdy / chordLen;
        } else {
            this.chordUnitX = 1;
            this.chordUnitY = 0;
        }

        // Compute the auto control point using the same algorithm as the renderer,
        // temporarily ignoring any existing user overrides so we get the
        // natural direction vector.
        List<CausalLinkDef> autoLinks = stripUserOverrides(fromName, toName, allLinks);
        CausalLinkGeometry.ControlPoint autoCP = CausalLinkGeometry.controlPoint(
                fromX, fromY, toX, toY, fromName, toName, autoLinks, loopCtx);

        double ax = autoCP.x() - midX;
        double ay = autoCP.y() - midY;
        double aDist = Math.sqrt(ax * ax + ay * ay);

        if (aDist < 0.001) {
            // Degenerate — fall back to simple perpendicular
            if (chordLen < 1) {
                this.dirX = 0;
                this.dirY = -1;
            } else {
                this.dirX = -cdy / chordLen;
                this.dirY = cdx / chordLen;
            }
        } else {
            this.dirX = ax / aDist;
            this.dirY = ay / aDist;
        }
    }

    /**
     * Updates strength and bias based on the current drag position.
     * Decomposes the drag displacement into perpendicular (strength) and
     * parallel (bias) components.
     * <p>
     * The 2× multiplier compensates for the Bézier midpoint formula:
     * B(0.5) = (mid + CP) / 2, so the control point must be set at twice
     * the desired offset for the handle (at t=0.5) to track the mouse.
     *
     * @return the computed strength and bias values
     */
    public DragResult drag(double worldX, double worldY) {
        double dx = worldX - midX;
        double dy = worldY - midY;
        double strength = 2 * (dx * dirX + dy * dirY);
        double bias = 2 * (dx * chordUnitX + dy * chordUnitY);
        return new DragResult(strength, bias);
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
     * Returns a copy of the link list with user overrides (strength and bias)
     * stripped from the specific link, so we can compute the auto control
     * point direction.
     */
    private static List<CausalLinkDef> stripUserOverrides(String from, String to,
                                                            List<CausalLinkDef> allLinks) {
        return allLinks.stream()
                .map(link -> {
                    if (link.from().equals(from) && link.to().equals(to)
                            && (link.hasStrength() || link.hasBias())) {
                        return new CausalLinkDef(link.from(), link.to(),
                                link.polarity(), link.comment());
                    }
                    return link;
                })
                .toList();
    }
}
