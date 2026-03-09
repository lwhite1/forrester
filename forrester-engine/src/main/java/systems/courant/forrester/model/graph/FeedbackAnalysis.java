package systems.courant.forrester.model.graph;

import systems.courant.forrester.model.def.AuxDef;
import systems.courant.forrester.model.def.CausalLinkDef;
import systems.courant.forrester.model.def.ConstantDef;
import systems.courant.forrester.model.def.FlowDef;
import systems.courant.forrester.model.def.LookupTableDef;
import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.StockDef;
import systems.courant.forrester.model.expr.ExprDependencies;
import systems.courant.forrester.model.expr.ExprParser;
import systems.courant.forrester.model.expr.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private static final Logger log = LoggerFactory.getLogger(FeedbackAnalysis.class);

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

        /**
         * Returns the single-character label for this loop type ({@code "R"}, {@code "B"}, or {@code "?"}).
         */
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

    /** Maximum recursion depth for graph traversal, matching ExprParser.MAX_DEPTH. */
    private static final int MAX_DEPTH = 200;

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
     *
     * @param def the model definition to analyze
     * @return a FeedbackAnalysis containing loop participants, groups, edges, and classified causal loops
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
     * Summary information about a single feedback loop, usable for both
     * stock-and-flow SCC groups and CLD elementary cycles.
     *
     * @param label     display label (e.g. "R1", "B2", "Loop 1")
     * @param type      loop classification (null for S&amp;F groups which lack polarity info)
     * @param path      ordered element names forming the loop (empty for S&amp;F groups)
     * @param narrative human-readable description (e.g. "Population → Births → Population")
     */
    public record LoopInfo(String label, LoopType type, List<String> path, String narrative) {
    }

    /**
     * Returns summary information for the loop at the given index.
     * Indices {@code 0..loopGroups.size()-1} refer to stock-and-flow SCC groups;
     * indices {@code loopGroups.size()..loopCount()-1} refer to CLD causal loops.
     *
     * @param index zero-based loop index
     * @return loop info, or empty if index is out of range
     */
    public Optional<LoopInfo> loopInfo(int index) {
        if (index < 0 || index >= loopCount()) {
            return Optional.empty();
        }
        if (index < loopGroups.size()) {
            Set<String> group = loopGroups.get(index);
            String label = "Loop " + (index + 1);
            String narrative = String.join(" \u2192 ", group);
            return Optional.of(new LoopInfo(label, null,
                    List.copyOf(group), narrative));
        }
        int cldIndex = index - loopGroups.size();
        CausalLoop loop = causalLoops.get(cldIndex);
        List<String> path = loop.path();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                sb.append(" \u2192 ");
            }
            sb.append(path.get(i));
        }
        sb.append(" \u2192 ").append(path.getFirst());
        return Optional.of(new LoopInfo(loop.label(), loop.type(),
                path, sb.toString()));
    }

    /**
     * Creates a filtered {@code FeedbackAnalysis} containing only the single loop
     * at the given index. Returns the full analysis if index is out of range.
     *
     * @param index zero-based loop index
     * @return filtered analysis for a single loop
     */
    public FeedbackAnalysis filterToLoop(int index) {
        if (index < 0 || index >= loopCount()) {
            return this;
        }

        if (index < loopGroups.size()) {
            Set<String> group = loopGroups.get(index);
            Set<Edge> filteredEdges = new LinkedHashSet<>();
            for (Edge e : loopEdges) {
                if (group.contains(e.from()) && group.contains(e.to())) {
                    filteredEdges.add(e);
                }
            }
            return new FeedbackAnalysis(
                    Collections.unmodifiableSet(new LinkedHashSet<>(group)),
                    List.of(group),
                    Collections.unmodifiableSet(filteredEdges),
                    Collections.emptyList());
        }

        int cldIndex = index - loopGroups.size();
        CausalLoop loop = causalLoops.get(cldIndex);
        Set<String> participants = new LinkedHashSet<>(loop.path());
        Set<Edge> edges = new LinkedHashSet<>();
        List<String> path = loop.path();
        for (int i = 0; i < path.size(); i++) {
            edges.add(new Edge(path.get(i), path.get((i + 1) % path.size())));
        }
        return new FeedbackAnalysis(
                Collections.unmodifiableSet(participants),
                Collections.emptyList(),
                Collections.unmodifiableSet(edges),
                List.of(loop));
    }

    /**
     * Returns true if the named element participates in at least one feedback loop.
     *
     * @param elementName the element name to check
     * @return {@code true} if the element is part of a feedback loop
     */
    public boolean isInLoop(String elementName) {
        return loopParticipants.contains(elementName);
    }

    /**
     * Returns true if the edge from &rarr; to is part of a feedback loop.
     *
     * @param from the source element name
     * @param to   the target element name
     * @return {@code true} if this directed edge participates in a feedback loop
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
            dfsCycles(start, start, path, visited, allowedNodes, graph, cycles, 0);
        }
        return cycles;
    }

    private static void dfsCycles(String current, String start,
            List<String> path, Set<String> visited,
            Set<String> allowedNodes, Map<String, Set<String>> graph,
            List<List<String>> cycles, int depth) {
        if (cycles.size() >= MAX_CYCLES || depth > MAX_DEPTH) {
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
                dfsCycles(next, start, path, visited, allowedNodes, graph, cycles, depth + 1);
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
                } catch (ParseException ex) {
                    log.debug("Skip unparseable auxiliary '{}': {}", resolved, ex.getMessage(), ex);
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
                        onStack, stack, result, 0);
            }
        }
        return result;
    }

    private static void strongconnect(String v, Map<String, Set<String>> graph,
            int[] index, Map<String, Integer> nodeIndex,
            Map<String, Integer> lowlink, Set<String> onStack,
            Deque<String> stack, List<Set<String>> result, int depth) {
        if (depth > MAX_DEPTH) {
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
