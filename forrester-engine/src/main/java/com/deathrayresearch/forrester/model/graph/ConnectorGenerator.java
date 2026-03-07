package com.deathrayresearch.forrester.model.graph;

import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.ModelDefinition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates influence connectors (arrows) from a model definition's dependency graph.
 * Used when a model has no explicit {@link com.deathrayresearch.forrester.model.def.ViewDef}.
 */
public final class ConnectorGenerator {

    private ConnectorGenerator() {
    }

    /**
     * Generates connector routes from the dependency graph of the given model definition.
     * Each edge in the dependency graph becomes a connector arrow.
     * Duplicate connectors are eliminated.
     *
     * @param def the model definition whose dependencies are to be visualized
     * @return a deduplicated list of connector routes
     */
    public static List<ConnectorRoute> generate(ModelDefinition def) {
        DependencyGraph graph = DependencyGraph.fromDefinition(def);
        Set<String> seen = new LinkedHashSet<>();
        List<ConnectorRoute> connectors = new ArrayList<>();

        for (String[] edge : graph.allEdges()) {
            String key = edge[0] + " -> " + edge[1];
            if (seen.add(key)) {
                connectors.add(new ConnectorRoute(edge[0], edge[1]));
            }
        }

        return connectors;
    }
}
