package systems.courant.sd.model.graph;

import systems.courant.sd.model.NameResolver;
import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.StockDef;
import systems.courant.sd.model.expr.ExprDependencies;
import systems.courant.sd.model.expr.ExprParser;
import systems.courant.sd.model.expr.ParseException;

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

    /**
     * Compact constructor that defensively copies all mutable collection arguments.
     */
    public FeedbackAnalysis {
        loopParticipants = loopParticipants.isEmpty()
                ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(loopParticipants));
        loopGroups = loopGroups.isEmpty()
                ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(loopGroups));
        loopEdges = loopEdges.isEmpty()
                ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(loopEdges));
        causalLoops = causalLoops.isEmpty()
                ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(causalLoops));
    }

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
     * @param path       ordered list of variable names forming the cycle
     *                   (edge from path[i] to path[(i+1) % size])
     * @param polarities polarity of each edge (same order as path; polarity[i] is from path[i] to path[i+1 mod size])
     * @param type       loop classification
     * @param label      display label (e.g. "R1", "B2", "?1")
     */
    public record CausalLoop(
            List<String> path,
            List<CausalLinkDef.Polarity> polarities,
            LoopType type,
            String label
    ) {
        public CausalLoop {
            path = List.copyOf(path);
            polarities = List.copyOf(polarities);
        }
    }


    /**
     * Analyzes a model definition to find feedback loops in both the
     * stock-and-flow structure and the causal link graph.
     *
     * <p><b>Stock-and-flow algorithm:</b>
     * <ol>
     *   <li>Build a stock-to-stock causal graph: an edge from stock X to stock Y
     *       exists when a flow that affects Y has an equation that depends on X
     *       (directly or transitively through variables), and X &ne; Y.</li>
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
        Set<Edge> sfEdges = new LinkedHashSet<>();

        List<CausalLoop> sfCycles = analyzeSF(def, sfParticipants, sfEdges);

        // ---- CLD causal link analysis ----
        List<CausalLoop> cldLoops = analyzeCausalLinks(def);

        // ---- Merge results ----
        List<CausalLoop> allLoops = new ArrayList<>(sfCycles);
        allLoops.addAll(cldLoops);

        Set<String> allParticipants = new LinkedHashSet<>(sfParticipants);
        Set<Edge> allEdges = new LinkedHashSet<>(sfEdges);

        for (CausalLoop loop : allLoops) {
            allParticipants.addAll(loop.path());
            List<String> path = loop.path();
            for (int i = 0; i < path.size(); i++) {
                allEdges.add(new Edge(path.get(i), path.get((i + 1) % path.size())));
            }
        }

        if (allParticipants.isEmpty() && allLoops.isEmpty()) {
            return new FeedbackAnalysis(
                    Collections.emptySet(),
                    Collections.emptyList(),
                    Collections.emptySet(),
                    Collections.emptyList());
        }

        return new FeedbackAnalysis(
                Collections.unmodifiableSet(allParticipants),
                Collections.emptyList(),
                Collections.unmodifiableSet(allEdges),
                Collections.unmodifiableList(allLoops));
    }

    /**
     * Returns the total number of detected loops (SF groups + CLD cycles).
     */
    public int loopCount() {
        return causalLoops.size();
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
        CausalLoop loop = causalLoops.get(index);
        List<String> path = loop.path();
        String narrative = buildBehavioralNarrative(loop);
        return Optional.of(new LoopInfo(loop.label(), loop.type(),
                path, narrative));
    }

    /**
     * Builds a behavioral narrative for a causal loop, describing how each variable
     * in the chain affects the next. For example:
     * "As Population rises, Births increase, further raising Population (reinforcing)"
     */
    static String buildBehavioralNarrative(CausalLoop loop) {
        List<String> path = loop.path();
        List<CausalLinkDef.Polarity> polarities = loop.polarities();

        if (path.isEmpty() || polarities.isEmpty()
                || polarities.stream().anyMatch(p -> p == CausalLinkDef.Polarity.UNKNOWN)) {
            // For S&F feedback groups, list participating stocks
            if (loop.type() == LoopType.INDETERMINATE && path.size() > 2) {
                return buildGroupNarrative(path);
            }
            // Fall back to simple chain for small indeterminate loops
            return buildSimpleChain(path);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("As ").append(path.getFirst()).append(" rises");

        // Track cumulative effect: starts positive (rise), each link may flip it
        boolean currentlyRising = true;
        for (int i = 0; i < path.size(); i++) {
            String target = path.get((i + 1) % path.size());
            CausalLinkDef.Polarity polarity = polarities.get(i);
            boolean nextRises = (polarity == CausalLinkDef.Polarity.POSITIVE) == currentlyRising;

            if (i < path.size() - 1) {
                sb.append(", ").append(target);
                sb.append(nextRises ? " increases" : " decreases");
            } else {
                // Closing the loop back to the first variable
                String verb = nextRises ? "raising" : "lowering";
                sb.append(", further ").append(verb).append(" ").append(target);
            }
            currentlyRising = nextRises;
        }

        String typeDesc = switch (loop.type()) {
            case REINFORCING -> "reinforcing";
            case BALANCING -> "balancing";
            case INDETERMINATE -> "indeterminate";
        };
        sb.append(" (").append(typeDesc).append(")");
        return sb.toString();
    }

    private static String buildSimpleChain(List<String> path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                sb.append(" \u2192 ");
            }
            sb.append(path.get(i));
        }
        if (!path.isEmpty()) {
            sb.append(" \u2192 ").append(path.getFirst());
        }
        return sb.toString();
    }

    private static String buildGroupNarrative(List<String> members) {
        StringBuilder sb = new StringBuilder();
        sb.append(members.size()).append(" stocks in mutual feedback:\n");
        for (int i = 0; i < members.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(members.get(i));
        }
        sb.append("\n\nThese stocks influence each other's rates of change ");
        sb.append("through flows and intermediary variables.");
        return sb.toString();
    }

    /**
     * Returns the loop type for the loop at the given index, or null for S&amp;F groups.
     */
    public LoopType loopType(int index) {
        if (index < 0 || index >= loopCount()) {
            return null;
        }
        return causalLoops.get(index).type();
    }

    /**
     * Returns the indices of all loops matching the given type filter.
     * A null filter matches all loops. S&amp;F groups (type=null) only match a null filter.
     */
    public List<Integer> filteredIndices(LoopType filter) {
        List<Integer> result = new ArrayList<>();
        int total = loopCount();
        for (int i = 0; i < total; i++) {
            if (filter == null || filter.equals(loopType(i))) {
                result.add(i);
            }
        }
        return result;
    }

    /**
     * Creates a filtered {@code FeedbackAnalysis} containing only loops of the given type.
     * A null filter returns the full analysis unchanged.
     */
    public FeedbackAnalysis filterByType(LoopType filter) {
        if (filter == null) {
            return this;
        }
        List<CausalLoop> filtered = new ArrayList<>();
        for (CausalLoop loop : causalLoops) {
            if (loop.type() == filter) {
                filtered.add(loop);
            }
        }
        Set<String> participants = new LinkedHashSet<>();
        Set<Edge> edges = new LinkedHashSet<>();
        for (CausalLoop loop : filtered) {
            participants.addAll(loop.path());
            List<String> path = loop.path();
            for (int i = 0; i < path.size(); i++) {
                edges.add(new Edge(path.get(i), path.get((i + 1) % path.size())));
            }
        }
        // Include S&F flow edges that mediate between cycle stocks
        addMediatingFlowEdges(participants, edges, loopEdges);
        return new FeedbackAnalysis(
                Collections.unmodifiableSet(participants),
                Collections.emptyList(),
                Collections.unmodifiableSet(edges),
                Collections.unmodifiableList(filtered));
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

        CausalLoop loop = causalLoops.get(index);
        Set<String> participants = new LinkedHashSet<>(loop.path());
        Set<Edge> edges = new LinkedHashSet<>();
        List<String> path = loop.path();
        for (int i = 0; i < path.size(); i++) {
            edges.add(new Edge(path.get(i), path.get((i + 1) % path.size())));
        }
        // Include S&F flow edges that mediate between cycle stocks
        addMediatingFlowEdges(participants, edges, loopEdges);
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

    /**
     * Adds flow nodes and their edges when they mediate between stocks
     * already in the participants set. This ensures that when filtering
     * to a single S&F cycle, the connecting flows are also highlighted.
     */
    private static void addMediatingFlowEdges(Set<String> participants,
            Set<Edge> edges, Set<Edge> allEdges) {
        Set<String> stockSet = Set.copyOf(participants);
        for (Edge e : allEdges) {
            if (stockSet.contains(e.from()) && !stockSet.contains(e.to())) {
                // e.from() is a stock, e.to() is a flow — check if flow leads to another stock in set
                for (Edge e2 : allEdges) {
                    if (e2.from().equals(e.to()) && stockSet.contains(e2.to())) {
                        participants.add(e.to());
                        edges.add(e);
                        edges.add(e2);
                    }
                }
            }
        }
    }

    // ---- Stock-and-Flow analysis ----

    private static List<CausalLoop> analyzeSF(ModelDefinition def,
            Set<String> participants, Set<Edge> edges) {
        // Collect stock names
        Set<String> stockNames = new LinkedHashSet<>();
        for (StockDef s : def.stocks()) {
            stockNames.add(s.name());
        }

        if (stockNames.isEmpty()) {
            return Collections.emptyList();
        }

        // Collect variable equations for transitive resolution
        Map<String, String> auxEquations = new LinkedHashMap<>();
        for (VariableDef a : def.variables()) {
            auxEquations.put(a.name(), a.equation());
        }

        // Build name resolution set (all element names)
        Set<String> allNames = new LinkedHashSet<>(stockNames);
        allNames.addAll(auxEquations.keySet());
        for (FlowDef f : def.flows()) {
            allNames.add(f.name());
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
        List<Set<String>> sccs = TarjanSCC.findAll(stockNames, stockGraph);
        List<Set<String>> loopSCCs = new ArrayList<>();
        for (Set<String> scc : sccs) {
            if (scc.size() >= 2) {
                loopSCCs.add(scc);
            }
        }

        if (loopSCCs.isEmpty()) {
            return Collections.emptyList();
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

        // Find participating flows and build edges (stock→flow→stock)
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

        // Report each SCC as a single feedback group rather than enumerating
        // individual elementary cycles. Dense stock-to-stock graphs (common when
        // variables like "total_population" reference all stocks) can produce
        // hundreds of cycles that are unhelpful for users. One group per SCC
        // provides a clearer summary of feedback structure.
        List<CausalLoop> sfGroups = new ArrayList<>();

        for (int gi = 0; gi < loopSCCs.size(); gi++) {
            Set<String> scc = loopSCCs.get(gi);
            List<String> sortedMembers = new ArrayList<>(scc);
            Collections.sort(sortedMembers);
            List<CausalLinkDef.Polarity> polarities = Collections.nCopies(
                    sortedMembers.size(), CausalLinkDef.Polarity.UNKNOWN);
            String label = loopSCCs.size() == 1
                    ? "Feedback Group"
                    : "Feedback Group " + (gi + 1);
            sfGroups.add(new CausalLoop(
                    Collections.unmodifiableList(sortedMembers),
                    polarities,
                    LoopType.INDETERMINATE,
                    label));
        }

        return sfGroups;
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
        List<Set<String>> sccs = TarjanSCC.findAll(nodes, graph);

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
                List<CausalLinkDef.Polarity> cyclePolarities = collectPolarities(cycle, edgePolarities);
                LoopType type = classifyCycle(cycle, edgePolarities);
                String label = switch (type) {
                    case REINFORCING -> "R" + (++rCount);
                    case BALANCING -> "B" + (++bCount);
                    case INDETERMINATE -> "?" + (++iCount);
                };
                loops.add(new CausalLoop(Collections.unmodifiableList(cycle),
                        Collections.unmodifiableList(cyclePolarities), type, label));
            }
        }

        return loops;
    }

    /**
     * Collects the polarity of each edge in the cycle.
     */
    private static List<CausalLinkDef.Polarity> collectPolarities(List<String> cycle,
            Map<Edge, CausalLinkDef.Polarity> edgePolarities) {
        List<CausalLinkDef.Polarity> result = new ArrayList<>();
        for (int i = 0; i < cycle.size(); i++) {
            String from = cycle.get(i);
            String to = cycle.get((i + 1) % cycle.size());
            CausalLinkDef.Polarity p = edgePolarities.get(new Edge(from, to));
            result.add(p != null ? p : CausalLinkDef.Polarity.UNKNOWN);
        }
        return result;
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
     * Finds an independent cycle basis within an SCC using a BFS spanning tree.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Build a BFS spanning tree from the lexicographically first node.</li>
     *   <li>For each non-tree edge (u, v), find the shortest path from v back to u
     *       in the full graph using BFS.</li>
     *   <li>The fundamental cycle is: u → v → (shortest path back to u).</li>
     * </ol>
     *
     * <p>This produces exactly E − V + 1 independent cycles (the circuit rank),
     * which matches the minimal set SD practitioners expect. Cycles are sorted
     * by length so shorter, more intuitive loops appear first.
     */
    private static List<List<String>> findElementaryCycles(Set<String> sccNodes,
            Map<String, Set<String>> graph) {
        if (sccNodes.size() < 2) {
            return Collections.emptyList();
        }

        // Deterministic root: lexicographically first node
        List<String> sortedNodes = new ArrayList<>(sccNodes);
        Collections.sort(sortedNodes);
        String root = sortedNodes.getFirst();

        // BFS spanning tree
        Set<Edge> treeEdges = new LinkedHashSet<>();
        Set<String> visited = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(root);
        visited.add(root);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String next : graph.getOrDefault(current, Collections.emptySet())) {
                if (sccNodes.contains(next) && visited.add(next)) {
                    treeEdges.add(new Edge(current, next));
                    queue.add(next);
                }
            }
        }

        // For each non-tree edge, find the shortest cycle through it
        Set<List<String>> seen = new LinkedHashSet<>();
        List<List<String>> cycles = new ArrayList<>();

        for (String from : sortedNodes) {
            for (String to : graph.getOrDefault(from, Collections.emptySet())) {
                if (!sccNodes.contains(to) || treeEdges.contains(new Edge(from, to))) {
                    continue;
                }
                // Non-tree edge from → to: find shortest path to → from in full graph
                List<String> returnPath = bfsShortestPath(to, from, graph, sccNodes);
                if (returnPath == null) {
                    continue;
                }
                // Cycle is: from → to → ... → from
                List<String> cycle = new ArrayList<>(returnPath.size() + 1);
                cycle.add(from);
                cycle.addAll(returnPath.subList(0, returnPath.size() - 1));

                // Normalize: rotate to start at lexicographically smallest node
                List<String> normalized = normalizeCycle(cycle);
                if (seen.add(normalized)) {
                    cycles.add(normalized);
                }
            }
        }

        // Sort by length (shorter = more intuitive)
        cycles.sort((a, b) -> Integer.compare(a.size(), b.size()));
        return cycles;
    }

    /**
     * Finds the shortest directed path from start to end within the allowed nodes.
     */
    private static List<String> bfsShortestPath(String start, String end,
            Map<String, Set<String>> graph, Set<String> allowedNodes) {
        if (start.equals(end)) {
            return List.of(start);
        }
        Map<String, String> parent = new LinkedHashMap<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(start);
        parent.put(start, null);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String next : graph.getOrDefault(current, Collections.emptySet())) {
                if (!allowedNodes.contains(next) || parent.containsKey(next)) {
                    continue;
                }
                parent.put(next, current);
                if (next.equals(end)) {
                    List<String> path = new ArrayList<>();
                    String node = end;
                    while (node != null) {
                        path.add(node);
                        node = parent.get(node);
                    }
                    Collections.reverse(path);
                    return path;
                }
                queue.add(next);
            }
        }
        return null;
    }

    /**
     * Normalizes a cycle by rotating it to start at the lexicographically smallest node.
     * This allows deduplication of cycles that represent the same loop from different
     * starting points.
     */
    private static List<String> normalizeCycle(List<String> cycle) {
        int minIdx = 0;
        for (int i = 1; i < cycle.size(); i++) {
            if (cycle.get(i).compareTo(cycle.get(minIdx)) < 0) {
                minIdx = i;
            }
        }
        if (minIdx == 0) {
            return cycle;
        }
        List<String> rotated = new ArrayList<>(cycle.size());
        for (int i = 0; i < cycle.size(); i++) {
            rotated.add(cycle.get((minIdx + i) % cycle.size()));
        }
        return rotated;
    }

    // ---- Shared utilities ----

    /**
     * Resolves an equation's references to find all stocks that transitively
     * influence it (following variable chains but stopping at stocks).
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
                    log.debug("Skip unparseable variable '{}': {}", resolved, ex.getMessage(), ex);
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
        return NameResolver.resolveInSet(ref, allNames);
    }

}
