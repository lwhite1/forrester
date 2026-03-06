package com.deathrayresearch.forrester.model.graph;

import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.CausalLinkDef;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.StockDef;
import com.deathrayresearch.forrester.model.expr.ExprDependencies;
import com.deathrayresearch.forrester.model.expr.ExprParser;
import com.deathrayresearch.forrester.model.expr.ParseException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Feedback loop analysis for system dynamics models and causal loop diagrams.
 *
 * <p>For stock-and-flow models, identifies causal loops where a stock's value,
 * through a chain of flows and intermediary elements, ultimately influences a
 * different stock that feeds back to the original. Only multi-stock cycles
 * (two or more distinct stocks) are reported as feedback loops.
 *
 * <p>For causal loop diagrams (CLDs), finds all elementary cycles in the causal
 * link graph and classifies each as reinforcing (R), balancing (B), or
 * indeterminate (?). A cycle is reinforcing when the number of negative links
 * is even; balancing when odd; indeterminate when any link has unknown polarity.
 */
public record FeedbackAnalysis(
        Set<String> loopParticipants,
        List<Set<String>> loopGroups,
        Set<Edge> loopEdges,
        List<CausalLoop> causalLoops
) {

    /**
     * A directed edge between two elements.
     */
    public record Edge(String from, String to) {
    }

    /**
     * Classification of a feedback loop's behavior.
     */
    public enum LoopType {
        /** Even number of negative links — self-reinforcing behavior. */
        REINFORCING("R"),
        /** Odd number of negative links — goal-seeking/oscillating behavior. */
        BALANCING("B"),
        /** Contains links with unknown polarity — cannot classify. */
        INDETERMINATE("?");

        private final String label;

        LoopType(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /**
     * An individual causal feedback loop (elementary cycle) with its classification.
     *
     * @param path  ordered list of variable names forming the cycle
     *              (edge from path[i] to path[(i+1) % size])
     * @param type  loop classification
     * @param label display label (e.g. "R1", "B2", "?1")
     */
    public record CausalLoop(
            List<String> path,
            LoopType type,
            String label
    ) {
    }

    /** Maximum number of elementary cycles to enumerate per analysis. */
    private static final int MAX_CYCLES = 100;

    /**
     * Analyzes a model definition to find feedback loops in both the
     * stock-and-flow structure and the causal link graph.
     *
     * <p><b>Stock-and-flow algorithm:</b>
     * <ol>
     *   <li>Build a stock-to-stock causal graph: an edge from stock X to stock Y
     *       exists when a flow that affects Y has an equation that depends on X
     *       (directly or transitively through auxiliaries), and X &ne; Y.</li>
     *   <li>Find strongly connected components (SCCs) of size &ge; 2 using
     *       Tarjan's algorithm.</li>
     *   <li>Identify participating flows and build the full participant/edge sets.</li>
     * </ol>
     *
     * <p><b>Causal link algorithm:</b>
     * <ol>
     *   <li>Build a directed graph from causal links.</li>
     *   <li>Find SCCs of size &ge; 2.</li>
     *   <li>Enumerate elementary cycles within each SCC.</li>
     *   <li>Classify each cycle by its polarity product.</li>
     * </ol>
     */
    public static FeedbackAnalysis analyze(ModelDefinition def) {
        // ---- Stock-and-Flow analysis ----
        Set<String> sfParticipants = new LinkedHashSet<>();
        List<Set<String>> sfGroups = new ArrayList<>();
        Set<Edge> sfEdges = new LinkedHashSet<>();

        analyzeSF(def, sfParticipants, sfGroups, sfEdges);

        // ---- CLD causal link analysis ----
        List<CausalLoop> causalLoops = analyzeCausalLinks(def);

        // ---- Merge results ----
        Set<String> allParticipants = new LinkedHashSet<>(sfParticipants);
        Set<Edge> allEdges = new LinkedHashSet<>(sfEdges);

        for (CausalLoop loop : causalLoops) {
            allParticipants.addAll(loop.path());
            List<String> path = loop.path();
            for (int i = 0; i < path.size(); i++) {
                allEdges.add(new Edge(path.get(i), path.get((i + 1) % path.size())));
            }
        }

        if (allParticipants.isEmpty() && causalLoops.isEmpty()) {
            return new FeedbackAnalysis(
                    Collections.emptySet(),
                    Collections.emptyList(),
                    Collections.emptySet(),
                    Collections.emptyList());
        }

        return new FeedbackAnalysis(
                Collections.unmodifiableSet(allParticipants),
                Collections.unmodifiableList(sfGroups),
                Collections.unmodifiableSet(allEdges),
                Collections.unmodifiableList(causalLoops));
    }

    /**
     * Returns the total number of detected loops (SF groups + CLD cycles).
     */
    public int loopCount() {
        return loopGroups.size() + causalLoops.size();
    }

    /**
     * Returns true if the named element participates in at least one feedback loop.
     */
    public boolean isInLoop(String elementName) {
        return loopParticipants.contains(elementName);
    }

    /**
     * Returns true if the edge from &rarr; to is part of a feedback loop.
     */
    public boolean isLoopEdge(String from, String to) {
        return loopEdges.contains(new Edge(from, to));
    }

    // ---- Stock-and-Flow analysis ----

    private static void analyzeSF(ModelDefinition def,
            Set<String> participants, List<Set<String>> groups, Set<Edge> edges) {
        // Collect stock names
        Set<String> stockNames = new LinkedHashSet<>();
        for (StockDef s : def.stocks()) {
            stockNames.add(s.name());
        }

        if (stockNames.isEmpty()) {
            return;
        }

        // Collect auxiliary equations for transitive resolution
        Map<String, String> auxEquations = new LinkedHashMap<>();
        for (AuxDef a : def.auxiliaries()) {
            auxEquations.put(a.name(), a.equation());
        }

        // Build name resolution set (all element names)
        Set<String> allNames = new LinkedHashSet<>(stockNames);
        allNames.addAll(auxEquations.keySet());
        for (FlowDef f : def.flows()) {
            allNames.add(f.name());
        }
        for (ConstantDef c : def.constants()) {
            allNames.add(c.name());
        }
        for (LookupTableDef t : def.lookupTables()) {
            allNames.add(t.name());
        }

        // Pre-compute stock dependencies and affected stocks for each flow
        Map<String, Set<String>> flowInfluencing = new LinkedHashMap<>();
        Map<String, Set<String>> flowAffected = new LinkedHashMap<>();
        for (FlowDef flow : def.flows()) {
            Set<String> influencing = resolveStockDeps(
                    flow.equation(), stockNames, auxEquations, allNames);
            Set<String> affected = new LinkedHashSet<>();
            if (flow.source() != null && stockNames.contains(flow.source())) {
                affected.add(flow.source());
            }
            if (flow.sink() != null && stockNames.contains(flow.sink())) {
                affected.add(flow.sink());
            }
            flowInfluencing.put(flow.name(), influencing);
            flowAffected.put(flow.name(), affected);
        }

        // Build stock-to-stock causal graph (excluding self-loops)
        Map<String, Set<String>> stockGraph = new LinkedHashMap<>();
        for (String stock : stockNames) {
            stockGraph.put(stock, new LinkedHashSet<>());
        }
        for (FlowDef flow : def.flows()) {
            Set<String> influencing = flowInfluencing.get(flow.name());
            Set<String> affected = flowAffected.get(flow.name());
            for (String inf : influencing) {
                for (String aff : affected) {
                    if (!inf.equals(aff)) {
                        stockGraph.computeIfAbsent(inf, k -> new LinkedHashSet<>()).add(aff);
                    }
                }
            }
        }

        // Find SCCs of size >= 2
        List<Set<String>> sccs = tarjanSCC(stockNames, stockGraph);
        List<Set<String>> loopSCCs = new ArrayList<>();
        for (Set<String> scc : sccs) {
            if (scc.size() >= 2) {
                loopSCCs.add(scc);
            }
        }

        if (loopSCCs.isEmpty()) {
            return;
        }

        // Map each loop stock to its SCC index
        Set<String> loopStocks = new LinkedHashSet<>();
        Map<String, Integer> stockToSCC = new LinkedHashMap<>();
        for (int i = 0; i < loopSCCs.size(); i++) {
            for (String s : loopSCCs.get(i)) {
                loopStocks.add(s);
                stockToSCC.put(s, i);
            }
        }

        // Find participating flows and build edges
        participants.addAll(loopStocks);

        for (FlowDef flow : def.flows()) {
            Set<String> influencing = flowInfluencing.get(flow.name());
            Set<String> affected = flowAffected.get(flow.name());

            boolean flowInLoop = false;
            for (String inf : influencing) {
                for (String aff : affected) {
                    if (!inf.equals(aff)
                            && loopStocks.contains(inf)
                            && loopStocks.contains(aff)
                            && stockToSCC.get(inf).equals(stockToSCC.get(aff))) {
                        flowInLoop = true;
                        break;
                    }
                }
                if (flowInLoop) {
                    break;
                }
            }

            if (flowInLoop) {
                participants.add(flow.name());
                for (String inf : influencing) {
                    if (loopStocks.contains(inf)) {
                        edges.add(new Edge(inf, flow.name()));
                    }
                }
                for (String aff : affected) {
                    if (loopStocks.contains(aff)) {
                        edges.add(new Edge(flow.name(), aff));
                    }
                }
            }
        }

        // Build groups: each SCC's stocks + their participating flows
        for (int i = 0; i < loopSCCs.size(); i++) {
            Set<String> scc = loopSCCs.get(i);
            Set<String> group = new LinkedHashSet<>(scc);

            for (FlowDef flow : def.flows()) {
                if (!participants.contains(flow.name())) {
                    continue;
                }
                Set<String> influencing = flowInfluencing.get(flow.name());
                Set<String> affected = flowAffected.get(flow.name());

                boolean belongsToSCC = false;
                for (String inf : influencing) {
                    for (String aff : affected) {
                        if (!inf.equals(aff) && scc.contains(inf) && scc.contains(aff)) {
                            belongsToSCC = true;
                            break;
                        }
                    }
                    if (belongsToSCC) {
                        break;
                    }
                }
                if (belongsToSCC) {
                    group.add(flow.name());
                }
            }

            groups.add(Collections.unmodifiableSet(group));
        }
    }

    // ---- CLD causal link analysis ----

    /**
     * Detects and classifies feedback loops in the causal link graph.
     */
    private static List<CausalLoop> analyzeCausalLinks(ModelDefinition def) {
        List<CausalLinkDef> links = def.causalLinks();
        if (links.isEmpty()) {
            return Collections.emptyList();
        }

        // Build directed graph from causal links
        Set<String> nodes = new LinkedHashSet<>();
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        Map<Edge, CausalLinkDef.Polarity> edgePolarities = new LinkedHashMap<>();

        for (CausalLinkDef link : links) {
            nodes.add(link.from());
            nodes.add(link.to());
            graph.computeIfAbsent(link.from(), k -> new LinkedHashSet<>()).add(link.to());
            edgePolarities.put(new Edge(link.from(), link.to()), link.polarity());
        }

        // Find SCCs of size >= 2
        List<Set<String>> sccs = tarjanSCC(nodes, graph);

        // Enumerate elementary cycles within each SCC
        List<CausalLoop> loops = new ArrayList<>();
        int rCount = 0;
        int bCount = 0;
        int iCount = 0;

        for (Set<String> scc : sccs) {
            if (scc.size() < 2) {
                continue;
            }

            // Restrict graph to SCC nodes
            Map<String, Set<String>> subGraph = new LinkedHashMap<>();
            for (String node : scc) {
                Set<String> successors = new LinkedHashSet<>();
                for (String succ : graph.getOrDefault(node, Collections.emptySet())) {
                    if (scc.contains(succ)) {
                        successors.add(succ);
                    }
                }
                subGraph.put(node, successors);
            }

            List<List<String>> cycles = findElementaryCycles(scc, subGraph);
            for (List<String> cycle : cycles) {
                LoopType type = classifyCycle(cycle, edgePolarities);
                String label = switch (type) {
                    case REINFORCING -> "R" + (++rCount);
                    case BALANCING -> "B" + (++bCount);
                    case INDETERMINATE -> "?" + (++iCount);
                };
                loops.add(new CausalLoop(Collections.unmodifiableList(cycle), type, label));
            }
        }

        return loops;
    }

    /**
     * Classifies a cycle by counting the negative links.
     * Even negatives = reinforcing, odd = balancing, any unknown = indeterminate.
     */
    private static LoopType classifyCycle(List<String> cycle,
            Map<Edge, CausalLinkDef.Polarity> edgePolarities) {
        int negativeCount = 0;

        for (int i = 0; i < cycle.size(); i++) {
            String from = cycle.get(i);
            String to = cycle.get((i + 1) % cycle.size());
            CausalLinkDef.Polarity polarity = edgePolarities.get(new Edge(from, to));
            if (polarity == null || polarity == CausalLinkDef.Polarity.UNKNOWN) {
                return LoopType.INDETERMINATE;
            }
            if (polarity == CausalLinkDef.Polarity.NEGATIVE) {
                negativeCount++;
            }
        }

        return negativeCount % 2 == 0 ? LoopType.REINFORCING : LoopType.BALANCING;
    }

    /**
     * Finds all elementary cycles within an SCC using DFS backtracking.
     * To avoid reporting duplicate cycles, only explores cycles starting
     * from the lexicographically smallest node.
     */
    private static List<List<String>> findElementaryCycles(Set<String> sccNodes,
            Map<String, Set<String>> graph) {
        List<List<String>> cycles = new ArrayList<>();
        List<String> sortedNodes = new ArrayList<>(sccNodes);
        Collections.sort(sortedNodes);

        for (int i = 0; i < sortedNodes.size(); i++) {
            if (cycles.size() >= MAX_CYCLES) {
                break;
            }
            String start = sortedNodes.get(i);
            // Only search through nodes at index >= i to avoid duplicate cycles
            Set<String> allowedNodes = new LinkedHashSet<>(
                    sortedNodes.subList(i, sortedNodes.size()));
            List<String> path = new ArrayList<>();
            path.add(start);
            Set<String> visited = new HashSet<>();
            visited.add(start);
            dfsCycles(start, start, path, visited, allowedNodes, graph, cycles);
        }
        return cycles;
    }

    private static void dfsCycles(String current, String start,
            List<String> path, Set<String> visited,
            Set<String> allowedNodes, Map<String, Set<String>> graph,
            List<List<String>> cycles) {
        if (cycles.size() >= MAX_CYCLES) {
            return;
        }
        for (String next : graph.getOrDefault(current, Collections.emptySet())) {
            if (!allowedNodes.contains(next)) {
                continue;
            }
            if (next.equals(start) && path.size() > 1) {
                cycles.add(new ArrayList<>(path));
                continue;
            }
            if (!visited.contains(next)) {
                visited.add(next);
                path.add(next);
                dfsCycles(next, start, path, visited, allowedNodes, graph, cycles);
                path.removeLast();
                visited.remove(next);
            }
        }
    }

    // ---- Shared utilities ----

    /**
     * Resolves an equation's references to find all stocks that transitively
     * influence it (following auxiliary chains but stopping at stocks).
     */
    private static Set<String> resolveStockDeps(String equation,
            Set<String> stockNames, Map<String, String> auxEquations,
            Set<String> allNames) {
        if (equation == null || equation.isBlank()) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();

        try {
            Set<String> directRefs = ExprDependencies.extract(ExprParser.parse(equation));
            queue.addAll(directRefs);
        } catch (ParseException e) {
            return Collections.emptySet();
        }

        while (!queue.isEmpty()) {
            String ref = queue.poll();
            String resolved = resolveName(ref, allNames);
            if (resolved == null) {
                continue;
            }

            if (stockNames.contains(resolved)) {
                result.add(resolved);
            } else if (auxEquations.containsKey(resolved) && visited.add(resolved)) {
                try {
                    Set<String> auxRefs = ExprDependencies.extract(
                            ExprParser.parse(auxEquations.get(resolved)));
                    queue.addAll(auxRefs);
                } catch (ParseException e) {
                    // skip unparseable auxiliaries
                }
            }
        }

        return result;
    }

    /**
     * Resolves a reference name to its canonical form, handling the
     * underscore-to-space mapping used in equations.
     */
    private static String resolveName(String ref, Set<String> allNames) {
        if (allNames.contains(ref)) {
            return ref;
        }
        String spaced = ref.replace('_', ' ');
        if (allNames.contains(spaced)) {
            return spaced;
        }
        return null;
    }

    // ---- Tarjan's SCC algorithm ----

    /**
     * Finds all strongly connected components using Tarjan's algorithm.
     */
    private static List<Set<String>> tarjanSCC(Set<String> nodes,
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
                        onStack, stack, result);
            }
        }
        return result;
    }

    private static void strongconnect(String v, Map<String, Set<String>> graph,
            int[] index, Map<String, Integer> nodeIndex,
            Map<String, Integer> lowlink, Set<String> onStack,
            Deque<String> stack, List<Set<String>> result) {
        nodeIndex.put(v, index[0]);
        lowlink.put(v, index[0]);
        index[0]++;
        stack.push(v);
        onStack.add(v);

        for (String w : graph.getOrDefault(v, Collections.emptySet())) {
            if (!nodeIndex.containsKey(w)) {
                strongconnect(w, graph, index, nodeIndex, lowlink,
                        onStack, stack, result);
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
