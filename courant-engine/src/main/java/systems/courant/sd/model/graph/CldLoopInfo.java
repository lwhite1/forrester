package systems.courant.sd.model.graph;

import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.CldVariableDef;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loop membership data for CLD graphs. Identifies which nodes belong to
 * which strongly connected components (loops) so that layout and edge
 * routing can treat intra-loop and cross-loop edges differently.
 *
 * @param loops       non-trivial SCCs (size &ge; 2), each a set of node names
 * @param nodeToLoops mapping from node name to the indices into {@code loops}
 */
public record CldLoopInfo(
        List<Set<String>> loops,
        Map<String, Set<Integer>> nodeToLoops
) {

    /** Empty instance for graphs with no feedback loops. */
    public static final CldLoopInfo EMPTY = new CldLoopInfo(List.of(), Map.of());

    /**
     * Computes loop info from CLD variable and causal link definitions.
     */
    public static CldLoopInfo compute(List<CldVariableDef> vars, List<CausalLinkDef> links) {
        if (vars.isEmpty()) {
            return EMPTY;
        }
        Set<String> nodes = new LinkedHashSet<>();
        Map<String, Set<String>> adj = new LinkedHashMap<>();
        for (CldVariableDef v : vars) {
            nodes.add(v.name());
            adj.putIfAbsent(v.name(), new LinkedHashSet<>());
        }
        for (CausalLinkDef link : links) {
            adj.computeIfAbsent(link.from(), k -> new LinkedHashSet<>()).add(link.to());
        }
        return fromAdjacency(nodes, adj);
    }

    /**
     * Computes loop info from a pre-built adjacency graph.
     */
    static CldLoopInfo fromAdjacency(Set<String> nodes, Map<String, Set<String>> adj) {
        List<Set<String>> sccs = TarjanSCC.findNonTrivial(nodes, adj);
        if (sccs.isEmpty()) {
            return EMPTY;
        }
        Map<String, Set<Integer>> ntl = new LinkedHashMap<>();
        for (int i = 0; i < sccs.size(); i++) {
            for (String node : sccs.get(i)) {
                ntl.computeIfAbsent(node, k -> new LinkedHashSet<>()).add(i);
            }
        }
        return new CldLoopInfo(List.copyOf(sccs), Collections.unmodifiableMap(ntl));
    }

    /**
     * Returns true if both nodes share at least one common loop.
     */
    public boolean inSameLoop(String a, String b) {
        return commonLoopIndex(a, b) >= 0;
    }

    /**
     * Returns the index of a common loop, or -1 if the nodes share no loop.
     */
    public int commonLoopIndex(String a, String b) {
        Set<Integer> la = nodeToLoops.get(a);
        Set<Integer> lb = nodeToLoops.get(b);
        if (la == null || lb == null) {
            return -1;
        }
        for (int idx : la) {
            if (lb.contains(idx)) {
                return idx;
            }
        }
        return -1;
    }

    /**
     * Returns true if the node belongs to more than one loop.
     */
    public boolean isSharedNode(String name) {
        Set<Integer> set = nodeToLoops.get(name);
        return set != null && set.size() > 1;
    }

    /**
     * Returns the set of loop indices the node belongs to, or empty set.
     */
    public Set<Integer> loopsOf(String name) {
        return nodeToLoops.getOrDefault(name, Set.of());
    }

    /**
     * Returns true if there are no feedback loops.
     */
    public boolean isEmpty() {
        return loops.isEmpty();
    }
}
