package systems.courant.sd.model.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Tarjan's algorithm for finding strongly connected components (SCCs) in a directed graph.
 *
 * <p>Shared utility used by both {@link DependencyGraph} and {@link FeedbackAnalysis}
 * to avoid duplicating the algorithm.
 */
public final class TarjanSCC {

    private static final Logger log = LoggerFactory.getLogger(TarjanSCC.class);

    /** Maximum recursion depth for graph traversal, matching ExprParser.MAX_DEPTH. */
    private static final int MAX_DEPTH = 200;

    private TarjanSCC() {
    }

    /**
     * Finds all strongly connected components using Tarjan's algorithm.
     *
     * @param nodes the set of all node names
     * @param graph the adjacency map (from → set of to)
     * @return list of all SCCs (including trivial single-node ones)
     */
    public static List<Set<String>> findAll(Set<String> nodes,
            Map<String, Set<String>> graph) {
        int[] index = {0};
        Map<String, Integer> nodeIndex = new LinkedHashMap<>();
        Map<String, Integer> lowlink = new LinkedHashMap<>();
        Set<String> onStack = new LinkedHashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        List<Set<String>> result = new ArrayList<>();

        for (String node : nodes) {
            if (!nodeIndex.containsKey(node)) {
                strongconnect(node, graph, index, nodeIndex, lowlink,
                        onStack, stack, result, 0);
            }
        }
        return result;
    }

    /**
     * Finds all SCCs of size &ge; 2 (non-trivial cycles).
     *
     * @param nodes the set of all node names
     * @param graph the adjacency map (from → set of to)
     * @return list of SCCs with at least 2 nodes
     */
    public static List<Set<String>> findNonTrivial(Set<String> nodes,
            Map<String, Set<String>> graph) {
        List<Set<String>> all = findAll(nodes, graph);
        List<Set<String>> nonTrivial = new ArrayList<>();
        for (Set<String> scc : all) {
            if (scc.size() >= 2) {
                nonTrivial.add(scc);
            }
        }
        return nonTrivial;
    }

    private static void strongconnect(String v, Map<String, Set<String>> graph,
            int[] index, Map<String, Integer> nodeIndex,
            Map<String, Integer> lowlink, Set<String> onStack,
            Deque<String> stack, List<Set<String>> result, int depth) {
        if (depth > MAX_DEPTH) {
            log.warn("SCC traversal truncated at depth {} for node '{}' — "
                    + "feedback loops in deep dependency chains may go undetected", depth, v);
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

        for (String w : graph.getOrDefault(v, Collections.emptySet())) {
            if (!nodeIndex.containsKey(w)) {
                strongconnect(w, graph, index, nodeIndex, lowlink,
                        onStack, stack, result, depth + 1);
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
