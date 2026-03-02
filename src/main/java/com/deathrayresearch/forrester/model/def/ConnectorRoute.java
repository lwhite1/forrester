package com.deathrayresearch.forrester.model.def;

import java.util.List;

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
            controlPoints = List.copyOf(controlPoints);
        }
    }

    public ConnectorRoute(String from, String to) {
        this(from, to, List.of());
    }
}
