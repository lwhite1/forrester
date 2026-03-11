package systems.courant.shrewd.model.def;

import java.util.List;

/**
 * Definition of a graphical view of a model (or a page within a multi-page view).
 *
 * @param name the view name
 * @param elements element placements (positions of stocks, flows, etc.)
 * @param connectors influence connectors (dependency arrows)
 * @param flowRoutes flow pipe routes
 */
public record ViewDef(
        String name,
        List<ElementPlacement> elements,
        List<ConnectorRoute> connectors,
        List<FlowRoute> flowRoutes
) {

    public ViewDef {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("View name must not be blank");
        }
        elements = elements == null ? List.of() : List.copyOf(elements);
        connectors = connectors == null ? List.of() : List.copyOf(connectors);
        flowRoutes = flowRoutes == null ? List.of() : List.copyOf(flowRoutes);
    }
}
