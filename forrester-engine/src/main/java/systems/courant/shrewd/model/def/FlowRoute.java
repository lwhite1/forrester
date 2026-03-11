package systems.courant.shrewd.model.def;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Route of a flow pipe in a graphical view.
 *
 * @param flowName the flow name
 * @param points the points describing the pipe path
 */
public record FlowRoute(
        String flowName,
        List<double[]> points
) {

    public FlowRoute {
        if (flowName == null || flowName.isBlank()) {
            throw new IllegalArgumentException("Flow name must not be blank");
        }
        if (points == null) {
            points = List.of();
        } else {
            List<double[]> cloned = new ArrayList<>(points.size());
            for (double[] point : points) {
                cloned.add(point.clone());
            }
            points = List.copyOf(cloned);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FlowRoute that)) {
            return false;
        }
        return Objects.equals(flowName, that.flowName)
                && PointListUtil.pointListEquals(points, that.points);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(flowName);
        for (double[] point : points) {
            result = 31 * result + Arrays.hashCode(point);
        }
        return result;
    }
}
