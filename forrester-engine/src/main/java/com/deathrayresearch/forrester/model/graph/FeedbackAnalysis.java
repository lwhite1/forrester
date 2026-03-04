package com.deathrayresearch.forrester.model.graph;

import com.deathrayresearch.forrester.model.def.AuxDef;
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
 * Feedback loop analysis for system dynamics models.
 * Identifies causal loops where a stock's value, through a chain of flows
 * and intermediary elements, ultimately influences a different stock that
 * feeds back to the original. Only multi-stock cycles (two or more distinct
 * stocks) are reported as feedback loops; single-stock drain/growth loops
 * are not flagged.
 */
public record FeedbackAnalysis(
        Set<String> loopParticipants,
        List<Set<String>> loopGroups,
        Set<Edge> loopEdges
) {

    /**
     * A directed edge between two elements.
     */
    public record Edge(String from, String to) {
    }

    /**
     * Analyzes a model definition to find stock-level feedback loops.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Build a stock-to-stock causal graph: an edge from stock X to stock Y
     *       exists when a flow that affects Y (source or sink) has an equation
     *       that depends on X (directly or transitively through auxiliaries),
     *       and X &ne; Y.</li>
     *   <li>Find strongly connected components (SCCs) of size &ge; 2 using
     *       Tarjan's algorithm &mdash; these are the feedback loops.</li>
     *   <li>Identify participating flows (those that create causal edges between
     *       stocks in the same SCC) and build the full participant/edge sets
     *       for rendering.</li>
     * </ol>
     */
    public static FeedbackAnalysis analyze(ModelDefinition def) {
        // Collect stock names
        Set<String> stockNames = new LinkedHashSet<>();
        for (StockDef s : def.stocks()) {
            stockNames.add(s.name());
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
            return new FeedbackAnalysis(
                    Collections.emptySet(),
                    Collections.emptyList(),
                    Collections.emptySet());
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
        Set<String> participants = new LinkedHashSet<>(loopStocks);
        Set<Edge> edges = new LinkedHashSet<>();

        for (FlowDef flow : def.flows()) {
            Set<String> influencing = flowInfluencing.get(flow.name());
            Set<String> affected = flowAffected.get(flow.name());

            // A flow participates if it creates a causal edge between two
            // different stocks in the same SCC
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
                // Formula dependency edges: each influencing loop stock → flow
                for (String inf : influencing) {
                    if (loopStocks.contains(inf)) {
                        edges.add(new Edge(inf, flow.name()));
                    }
                }
                // Material flow edges: flow → each affected loop stock
                for (String aff : affected) {
                    if (loopStocks.contains(aff)) {
                        edges.add(new Edge(flow.name(), aff));
                    }
                }
            }
        }

        // Build groups: each SCC's stocks + their participating flows
        List<Set<String>> groups = new ArrayList<>();
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

        return new FeedbackAnalysis(
                Collections.unmodifiableSet(participants),
                Collections.unmodifiableList(groups),
                Collections.unmodifiableSet(edges));
    }

    /**
     * Returns the number of loop groups (feedback structures) in the model.
     */
    public int loopCount() {
        return loopGroups.size();
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
