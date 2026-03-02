package com.deathrayresearch.forrester.model.def;

import java.util.List;

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
            points = List.copyOf(points);
        }
    }
}
