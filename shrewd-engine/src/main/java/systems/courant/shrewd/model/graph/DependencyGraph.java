package systems.courant.shrewd.model.graph;

import systems.courant.shrewd.model.def.VariableDef;
import systems.courant.shrewd.model.def.FlowDef;
import systems.courant.shrewd.model.def.LookupTableDef;
import systems.courant.shrewd.model.def.ModelDefinition;
import systems.courant.shrewd.model.def.StockDef;
import systems.courant.shrewd.model.expr.ExprDependencies;
import systems.courant.shrewd.model.expr.ExprParser;
import systems.courant.shrewd.model.expr.ParseException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
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

    /** Maximum recursion depth for graph traversal, matching ExprParser.MAX_DEPTH. */
    private static final int MAX_DEPTH = 200;

    private final Map<String, Set<String>> adjacency; // from → {to}
    private final Set<String> allNodes;

    private DependencyGraph(Map<String, Set<String>> adjacency, Set<String> allNodes) {
        this.adjacency = adjacency;
        this.allNodes = allNodes;
    }

    /**
     * Builds a dependency graph from a model definition by parsing all equations
     * and extracting references.
     *
     * @param def the model definition to analyze
     * @return a dependency graph with edges from influencing elements to dependent elements
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
        for (VariableDef a : def.variables()) {
            allNodes.add(a.name());
        }
        for (LookupTableDef t : def.lookupTables()) {
            allNodes.add(t.name());
        }

        // Extract dependencies from flow equations
        for (FlowDef f : def.flows()) {
            try {
                Set<String> deps = ExprDependencies.extract(ExprParser.parse(f.equation()));
                for (String dep : deps) {
                    if (allNodes.contains(dep) || allNodes.contains(dep.replace('_', ' '))) {
                        String resolvedDep = allNodes.contains(dep) ? dep : dep.replace('_', ' ');
                        adj.computeIfAbsent(resolvedDep, k -> new LinkedHashSet<>()).add(f.name());
                    }
                }
            } catch (ParseException ignored) {
                // Skip unparseable equations — cycle detection proceeds with parseable ones
            }
            // Flow → stock connections (source and sink)
            if (f.source() != null) {
                adj.computeIfAbsent(f.name(), k -> new LinkedHashSet<>()).add(f.source());
            }
            if (f.sink() != null) {
                adj.computeIfAbsent(f.name(), k -> new LinkedHashSet<>()).add(f.sink());
            }
        }

        // Extract dependencies from variable equations
        for (VariableDef a : def.variables()) {
            try {
                Set<String> deps = ExprDependencies.extract(ExprParser.parse(a.equation()));
                for (String dep : deps) {
                    if (allNodes.contains(dep) || allNodes.contains(dep.replace('_', ' '))) {
                        String resolvedDep = allNodes.contains(dep) ? dep : dep.replace('_', ' ');
                        adj.computeIfAbsent(resolvedDep, k -> new LinkedHashSet<>()).add(a.name());
                    }
                }
            } catch (ParseException ignored) {
                // Skip unparseable equations — cycle detection proceeds with parseable ones
            }
        }

        return new DependencyGraph(adj, allNodes);
    }

    /**
     * Returns the set of elements that the given element depends on (influences it).
     *
     * @param name the element name
     * @return the set of element names that influence the given element
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
     *
     * @param name the element name
     * @return the set of element names that depend on the given element
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
        Deque<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        while (!queue.isEmpty()) {
            String node = queue.poll();
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
     * Returns all elements transitively upstream of the given element (what influences it),
     * organized by BFS depth. Depth 0 is the element itself.
     *
     * @param name the element to trace from
     * @return map of element name to BFS depth
     */
    public Map<String, Integer> transitiveUpstream(String name) {
        Map<String, Integer> depthMap = new LinkedHashMap<>();
        depthMap.put(name, 0);
        Deque<String> queue = new ArrayDeque<>();
        queue.add(name);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            int nextDepth = depthMap.get(current) + 1;
            for (String dep : dependenciesOf(current)) {
                if (!depthMap.containsKey(dep)) {
                    depthMap.put(dep, nextDepth);
                    queue.add(dep);
                }
            }
        }
        return depthMap;
    }

    /**
     * Returns all elements transitively downstream of the given element (what it influences),
     * organized by BFS depth. Depth 0 is the element itself.
     *
     * @param name the element to trace from
     * @return map of element name to BFS depth
     */
    public Map<String, Integer> transitiveDownstream(String name) {
        Map<String, Integer> depthMap = new LinkedHashMap<>();
        depthMap.put(name, 0);
        Deque<String> queue = new ArrayDeque<>();
        queue.add(name);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            int nextDepth = depthMap.get(current) + 1;
            for (String dep : dependentsOf(current)) {
                if (!depthMap.containsKey(dep)) {
                    depthMap.put(dep, nextDepth);
                    queue.add(dep);
                }
            }
        }
        return depthMap;
    }

    /**
     * Returns all node names in the graph.
     */
    public Set<String> allNodes() {
        return Collections.unmodifiableSet(allNodes);
    }

    /**
     * Returns an unmodifiable view of the adjacency map (from → {to}).
     */
    public Map<String, Set<String>> adjacencyMap() {
        return Collections.unmodifiableMap(adjacency);
    }

    /**
     * Finds all strongly connected components of size >= 2 using Tarjan's algorithm.
     * Returns a list of SCCs, where each SCC is a set of node names.
     */
    public List<Set<String>> findSCCs() {
        int[] index = {0};
        Map<String, Integer> nodeIndex = new LinkedHashMap<>();
        Map<String, Integer> lowlink = new LinkedHashMap<>();
        Set<String> onStack = new LinkedHashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        List<Set<String>> result = new ArrayList<>();

        for (String node : allNodes) {
            if (!nodeIndex.containsKey(node)) {
                tarjanStrongconnect(node, index, nodeIndex, lowlink, onStack, stack, result, 0);
            }
        }

        List<Set<String>> nonTrivial = new ArrayList<>();
        for (Set<String> scc : result) {
            if (scc.size() >= 2) {
                nonTrivial.add(scc);
            }
        }
        return nonTrivial;
    }

    /**
     * Returns the set of all nodes that belong to any SCC of size >= 2.
     */
    public Set<String> findSccMembers() {
        Set<String> members = new LinkedHashSet<>();
        for (Set<String> scc : findSCCs()) {
            members.addAll(scc);
        }
        return members;
    }

    private void tarjanStrongconnect(String v, int[] index,
            Map<String, Integer> nodeIndex, Map<String, Integer> lowlink,
            Set<String> onStack, Deque<String> stack, List<Set<String>> result, int depth) {
        if (depth > MAX_DEPTH) {
            // Register node so callers can safely read index/lowlink,
            // but don't push onto stack or recurse — treat as a dead end.
            nodeIndex.put(v, index[0]);
            lowlink.put(v, index[0]);
            index[0]++;
            return;
        }
        nodeIndex.put(v, index[0]);
        lowlink.put(v, index[0]);
        index[0]++;
        stack.push(v);
        onStack.add(v);

        for (String w : adjacency.getOrDefault(v, Collections.emptySet())) {
            if (!allNodes.contains(w)) {
                continue;
            }
            if (!nodeIndex.containsKey(w)) {
                tarjanStrongconnect(w, index, nodeIndex, lowlink, onStack, stack, result, depth + 1);
                lowlink.put(v, Math.min(lowlink.get(v), lowlink.get(w)));
            } else if (onStack.contains(w)) {
                lowlink.put(v, Math.min(lowlink.get(v), nodeIndex.get(w)));
            }
        }

        if (lowlink.get(v).equals(nodeIndex.get(v))) {
            Set<String> scc = new LinkedHashSet<>();
            String w;
            do {
                w = stack.pop();
                onStack.remove(w);
                scc.add(w);
            } while (!w.equals(v));
            result.add(scc);
        }
    }
}
