package systems.courant.sd.model.def;

import java.util.List;
import java.util.Map;

/**
 * Definition of a graphical view of a model (or a page within a multi-page view).
 *
 * @param name the view name
 * @param elements element placements (positions of stocks, flows, etc.)
 * @param connectors influence connectors (dependency arrows)
 * @param flowRoutes flow pipe routes
 * @param loopNames user-assigned names for feedback loops, keyed by auto-generated label (e.g., "R1")
 */
public record ViewDef(
        String name,
        List<ElementPlacement> elements,
        List<ConnectorRoute> connectors,
        List<FlowRoute> flowRoutes,
        Map<String, String> loopNames
) {

    /** Backward-compatible constructor without loopNames. */
    public ViewDef(String name, List<ElementPlacement> elements,
                   List<ConnectorRoute> connectors, List<FlowRoute> flowRoutes) {
        this(name, elements, connectors, flowRoutes, Map.of());
    }

    public ViewDef {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("View name must not be blank");
        }
        elements = elements == null ? List.of() : List.copyOf(elements);
        connectors = connectors == null ? List.of() : List.copyOf(connectors);
        flowRoutes = flowRoutes == null ? List.of() : List.copyOf(flowRoutes);
        loopNames = loopNames == null ? Map.of() : Map.copyOf(loopNames);
    }
}
