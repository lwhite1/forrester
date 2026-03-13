package systems.courant.sd.model.def;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Route of an influence connector (arrow) between two elements in a view.
 *
 * @param from the source element name
 * @param to the target element name
 * @param controlPoints optional intermediate control points for curved connectors
 */
public record ConnectorRoute(
        String from,
        String to,
        List<double[]> controlPoints
) {

    public ConnectorRoute {
        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException("Connector 'from' must not be blank");
        }
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Connector 'to' must not be blank");
        }
        if (controlPoints == null) {
            controlPoints = List.of();
        } else {
            List<double[]> cloned = new ArrayList<>(controlPoints.size());
            for (double[] point : controlPoints) {
                cloned.add(point.clone());
            }
            controlPoints = List.copyOf(cloned);
        }
    }

    /**
     * Returns a defensive copy of the control points, cloning each array.
     */
    @Override
    public List<double[]> controlPoints() {
        List<double[]> cloned = new ArrayList<>(controlPoints.size());
        for (double[] point : controlPoints) {
            cloned.add(point.clone());
        }
        return List.copyOf(cloned);
    }

    /**
     * Creates a straight connector with no intermediate control points.
     *
     * @param from the source element name
     * @param to   the target element name
     */
    public ConnectorRoute(String from, String to) {
        this(from, to, List.of());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConnectorRoute that)) {
            return false;
        }
        return Objects.equals(from, that.from)
                && Objects.equals(to, that.to)
                && PointListUtil.pointListEquals(controlPoints, that.controlPoints);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(from, to);
        for (double[] point : controlPoints) {
            result = 31 * result + Arrays.hashCode(point);
        }
        return result;
    }
}
