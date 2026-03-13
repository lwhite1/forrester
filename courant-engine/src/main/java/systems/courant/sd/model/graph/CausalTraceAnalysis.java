package systems.courant.sd.model.graph;

import systems.courant.sd.model.def.ModelDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Result of a causal trace operation: all elements transitively upstream or
 * downstream of an origin element, with BFS depth information for progressive
 * opacity fading.
 *
 * @param origin    the element the trace started from
 * @param direction whether the trace went upstream or downstream
 * @param depthMap  element name to BFS depth (0 = origin)
 * @param traceEdges all directed edges in the trace subgraph
 * @param maxDepth  maximum depth reached
 */
public record CausalTraceAnalysis(
        String origin,
        TraceDirection direction,
        Map<String, Integer> depthMap,
        Set<FeedbackAnalysis.Edge> traceEdges,
        int maxDepth
) {

    /**
     * Direction of causal tracing.
     */
    public enum TraceDirection {
        /** Trace what influences the origin (upstream). */
        UPSTREAM,
        /** Trace what the origin influences (downstream). */
        DOWNSTREAM
    }

    /**
     * Returns true if the given element is part of this trace.
     */
    public boolean isTraced(String elementName) {
        return depthMap.containsKey(elementName);
    }

    /**
     * Returns true if the given directed edge is part of this trace.
     */
    public boolean isTraceEdge(String from, String to) {
        return traceEdges.contains(new FeedbackAnalysis.Edge(from, to));
    }

    /**
     * Returns the BFS depth of the element, or -1 if not in the trace.
     */
    public int depthOf(String elementName) {
        return depthMap.getOrDefault(elementName, -1);
    }

    /**
     * Returns an opacity value for the given depth level.
     * Depth 0 (origin) returns 1.0. Deeper levels fade toward 0.25.
     */
    public double opacityForDepth(int depth) {
        if (depth <= 0 || maxDepth <= 0) {
            return 1.0;
        }
        double fadeStep = 0.75 / maxDepth;
        return Math.max(0.25, 1.0 - depth * fadeStep);
    }

    /**
     * Builds a causal trace from a model definition.
     *
     * @param origin    the element to start tracing from
     * @param direction upstream or downstream
     * @param def       the model definition
     * @return the trace analysis
     */
    public static CausalTraceAnalysis trace(String origin, TraceDirection direction,
                                             ModelDefinition def) {
        DependencyGraph graph = DependencyGraph.fromDefinition(def);

        Map<String, Integer> depthMap = direction == TraceDirection.UPSTREAM
                ? graph.transitiveUpstream(origin)
                : graph.transitiveDownstream(origin);

        // Build edge set from the depth map
        Set<FeedbackAnalysis.Edge> edges = new LinkedHashSet<>();
        Map<String, Set<String>> adjacency = graph.adjacencyMap();

        for (String element : depthMap.keySet()) {
            Set<String> targets = adjacency.getOrDefault(element, Collections.emptySet());
            for (String target : targets) {
                if (depthMap.containsKey(target)) {
                    edges.add(new FeedbackAnalysis.Edge(element, target));
                }
            }
        }

        int maxDepth = 0;
        for (int d : depthMap.values()) {
            if (d > maxDepth) {
                maxDepth = d;
            }
        }

        return new CausalTraceAnalysis(
                origin, direction,
                Collections.unmodifiableMap(new LinkedHashMap<>(depthMap)),
                Collections.unmodifiableSet(edges),
                maxDepth);
    }
}
