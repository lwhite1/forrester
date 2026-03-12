package systems.courant.shrewd.app.canvas;

/**
 * Shared port position calculations for module input/output ports.
 * Used by both the renderer ({@link ElementRenderer}) and hit-tester ({@link HitTester}).
 */
public final class PortGeometry {

    /** Visual radius of port indicator circles. */
    public static final double PORT_RADIUS = 3.0;

    /** Hit-test radius — generous tolerance for clicking small port circles. */
    public static final double PORT_HIT_RADIUS = 8.0;

    private PortGeometry() {
    }

    /**
     * Computes the Y coordinate of a port indicator.
     *
     * @param moduleTopY top Y of the module rectangle
     * @param moduleHeight height of the module rectangle
     * @param portIndex zero-based index of the port
     * @param portCount total number of ports on this side
     * @return the Y coordinate of the port center
     */
    public static double portY(double moduleTopY, double moduleHeight,
                               int portIndex, int portCount) {
        double spacing = moduleHeight / (portCount + 1);
        return moduleTopY + spacing * (portIndex + 1);
    }

    /**
     * Returns the X coordinate for input ports (left edge of the module).
     */
    public static double inputPortX(double moduleCenterX, double moduleHalfWidth) {
        return moduleCenterX - moduleHalfWidth;
    }

    /**
     * Returns the X coordinate for output ports (right edge of the module).
     */
    public static double outputPortX(double moduleCenterX, double moduleHalfWidth) {
        return moduleCenterX + moduleHalfWidth;
    }
}
