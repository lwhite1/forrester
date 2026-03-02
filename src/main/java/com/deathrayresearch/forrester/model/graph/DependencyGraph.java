package com.deathrayresearch.forrester.model.graph;

import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.StockDef;
import com.deathrayresearch.forrester.model.expr.ExprDependencies;
import com.deathrayresearch.forrester.model.expr.ExprParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Directed dependency graph extracted from a {@link ModelDefinition}.
 * Each edge represents a formula dependency: if element A's formula references element B,
 * then there is an edge from B → A (B influences A).
 */
public class DependencyGraph {

    private final Map<String, Set<String>> adjacency; // from → {to}
    private final Set<String> allNodes;

    private DependencyGraph(Map<String, Set<String>> adjacency, Set<String> allNodes) {
        this.adjacency = adjacency;
        this.allNodes = allNodes;
    }

    /**
     * Builds a dependency graph from a model definition by parsing all equations
     * and extracting references.
     */
    public static DependencyGraph fromDefinition(ModelDefinition def) {
        Map<String, Set<String>> adj = new LinkedHashMap<>();
        Set<String> allNodes = new LinkedHashSet<>();

        // Register all element names
        for (StockDef s : def.stocks()) {
            allNodes.add(s.name());
        }
        for (FlowDef f : def.flows()) {
            allNodes.add(f.name());
        }
        for (AuxDef a : def.auxiliaries()) {
            allNodes.add(a.name());
        }
        for (ConstantDef c : def.constants()) {
            allNodes.add(c.name());
        }
        for (LookupTableDef t : def.lookupTables()) {
            allNodes.add(t.name());
        }

        // Extract dependencies from flow equations
        for (FlowDef f : def.flows()) {
            Set<String> deps = ExprDependencies.extract(ExprParser.parse(f.equation()));
            for (String dep : deps) {
                if (allNodes.contains(dep) || allNodes.contains(dep.replace('_', ' '))) {
                    String resolvedDep = allNodes.contains(dep) ? dep : dep.replace('_', ' ');
                    adj.computeIfAbsent(resolvedDep, k -> new LinkedHashSet<>()).add(f.name());
                }
            }
            // Flow → stock connections (source and sink)
            if (f.source() != null) {
                adj.computeIfAbsent(f.name(), k -> new LinkedHashSet<>()).add(f.source());
            }
            if (f.sink() != null) {
                adj.computeIfAbsent(f.name(), k -> new LinkedHashSet<>()).add(f.sink());
            }
        }

        // Extract dependencies from auxiliary equations
        for (AuxDef a : def.auxiliaries()) {
            Set<String> deps = ExprDependencies.extract(ExprParser.parse(a.equation()));
            for (String dep : deps) {
                if (allNodes.contains(dep) || allNodes.contains(dep.replace('_', ' '))) {
                    String resolvedDep = allNodes.contains(dep) ? dep : dep.replace('_', ' ');
                    adj.computeIfAbsent(resolvedDep, k -> new LinkedHashSet<>()).add(a.name());
                }
            }
        }

        return new DependencyGraph(adj, allNodes);
    }

    /**
     * Returns the set of elements that the given element depends on (influences it).
     */
    public Set<String> dependenciesOf(String name) {
        Set<String> deps = new LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> entry : adjacency.entrySet()) {
            if (entry.getValue().contains(name)) {
                deps.add(entry.getKey());
            }
        }
        return deps;
    }

    /**
     * Returns the set of elements that the given element influences (depends on it).
     */
    public Set<String> dependentsOf(String name) {
        Set<String> deps = adjacency.get(name);
        if (deps == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(deps);
    }

    /**
     * Returns all directed edges as a list of [from, to] pairs.
     */
    public List<String[]> allEdges() {
        List<String[]> edges = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : adjacency.entrySet()) {
            for (String to : entry.getValue()) {
                edges.add(new String[]{entry.getKey(), to});
            }
        }
        return edges;
    }

    /**
     * Returns a topological sort of the nodes. If the graph has cycles,
     * some nodes may be omitted from the result.
     */
    public List<String> topologicalSort() {
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        for (String node : allNodes) {
            inDegree.put(node, 0);
        }
        for (Map.Entry<String, Set<String>> entry : adjacency.entrySet()) {
            for (String to : entry.getValue()) {
                inDegree.merge(to, 1, Integer::sum);
            }
        }

        List<String> result = new ArrayList<>();
        List<String> queue = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        while (!queue.isEmpty()) {
            String node = queue.remove(0);
            result.add(node);
            for (String dependent : adjacency.getOrDefault(node, Collections.emptySet())) {
                int newDegree = inDegree.get(dependent) - 1;
                inDegree.put(dependent, newDegree);
                if (newDegree == 0) {
                    queue.add(dependent);
                }
            }
        }

        return result;
    }

    /**
     * Returns true if the graph has any cycles.
     * Note: stock-flow loops (stock → flow → stock) are expected in SD models.
     */
    public boolean hasCycle() {
        return topologicalSort().size() < allNodes.size();
    }

    /**
     * Returns all node names in the graph.
     */
    public Set<String> allNodes() {
        return Collections.unmodifiableSet(allNodes);
    }
}
