package systems.courant.sd.app.canvas.controllers;

import systems.courant.sd.model.def.ConnectorRoute;

import java.util.List;
import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.ConnectionId;
import systems.courant.sd.app.canvas.FlowGeometry;
import systems.courant.sd.app.canvas.HitTester;
import systems.courant.sd.app.canvas.ModelEditor;

/**
 * Manages info link connection rerouting via click-and-drag.
 * When the user clicks on an info link and drags, this controller
 * determines which end (source or target) was closer to the click
 * and tracks the rubber-band state for rerouting.
 */
public final class ConnectionRerouteController {

    private static final double ENDPOINT_TOLERANCE = 20.0;

    public enum RerouteEnd { FROM, TO }

    public record RerouteHit(String from, String to, RerouteEnd end,
                      double anchorX, double anchorY) {}

    private boolean active;
    private String fromName;
    private String toName;
    private RerouteEnd end;
    private double anchorX;
    private double anchorY;
    private double rubberBandX;
    private double rubberBandY;
    private boolean dragStarted;

    public boolean isActive() {
        return active;
    }

    public boolean isDragStarted() {
        return dragStarted;
    }

    public RerouteEnd getEnd() {
        return end;
    }

    public String getFromName() {
        return fromName;
    }

    public String getToName() {
        return toName;
    }

    /**
     * Tests if a click near a selected connection's endpoint should start a reroute.
     * Returns the hit info if the click is near the from or to endpoint.
     */
    public static RerouteHit hitTestEndpoint(ConnectionId connection, CanvasState state,
                                      List<ConnectorRoute> connectors,
                                      double worldX, double worldY) {
        if (connection == null) {
            return null;
        }

        String fromName = connection.from();
        String toName = connection.to();

        if (!state.hasElement(fromName) || !state.hasElement(toName)) {
            return null;
        }

        double fromCX = state.getX(fromName);
        double fromCY = state.getY(fromName);
        double toCX = state.getX(toName);
        double toCY = state.getY(toName);

        // Compute clipped endpoints (where the line meets element borders)
        FlowGeometry.Point2D clippedFrom = FlowGeometry.clipToElement(
                state, fromName, toCX, toCY);
        FlowGeometry.Point2D clippedTo = FlowGeometry.clipToElement(
                state, toName, fromCX, fromCY);

        double distToFrom = distance(worldX, worldY, clippedFrom.x(), clippedFrom.y());
        double distToTo = distance(worldX, worldY, clippedTo.x(), clippedTo.y());

        // Check if click is near an endpoint, prefer the closer one
        if (distToFrom <= ENDPOINT_TOLERANCE && distToFrom <= distToTo) {
            // Near the "from" end — anchor at "to" center
            return new RerouteHit(fromName, toName, RerouteEnd.FROM, toCX, toCY);
        } else if (distToTo <= ENDPOINT_TOLERANCE) {
            // Near the "to" end — anchor at "from" center
            return new RerouteHit(fromName, toName, RerouteEnd.TO, fromCX, fromCY);
        }

        return null;
    }

    /**
     * Prepares for a potential reroute. The actual drag starts on mouseDragged.
     */
    public void prepare(RerouteHit hit) {
        active = true;
        dragStarted = false;
        fromName = hit.from();
        toName = hit.to();
        end = hit.end();
        anchorX = hit.anchorX();
        anchorY = hit.anchorY();
        rubberBandX = anchorX;
        rubberBandY = anchorY;
    }

    /**
     * Activates the rubber band and updates its endpoint.
     */
    public void drag(double worldX, double worldY) {
        dragStarted = true;
        rubberBandX = worldX;
        rubberBandY = worldY;
    }

    /**
     * Completes the reroute: reconnects the info link to the element under the cursor.
     * Returns true if a reroute was actually performed.
     */
    public boolean complete(double worldX, double worldY, CanvasState state,
                     ModelEditor editor, Runnable saveUndo) {
        if (!dragStarted) {
            cancel();
            return false;
        }

        String hitElement = HitTester.hitTest(state, worldX, worldY);

        if (end == RerouteEnd.FROM) {
            // Rerouting the source: replace old source token with new source token
            // in the target's equation
            if (hitElement != null && !hitElement.equals(fromName)
                    && !hitElement.equals(toName)) {
                saveUndo.run();
                boolean ok = editor.rerouteConnectionSource(fromName, hitElement, toName);
                cancel();
                return ok;
            }
        } else {
            // Rerouting the target: move the reference from old target to new target
            if (hitElement != null && !hitElement.equals(toName)
                    && !hitElement.equals(fromName)) {
                saveUndo.run();
                boolean ok = editor.rerouteConnectionTarget(fromName, toName, hitElement);
                cancel();
                return ok;
            }
        }

        cancel();
        return false;
    }

    public void cancel() {
        active = false;
        dragStarted = false;
        fromName = null;
        toName = null;
        end = null;
    }

    public double getAnchorX() {
        return anchorX;
    }

    public double getAnchorY() {
        return anchorY;
    }

    public double getRubberBandX() {
        return rubberBandX;
    }

    public double getRubberBandY() {
        return rubberBandY;
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
