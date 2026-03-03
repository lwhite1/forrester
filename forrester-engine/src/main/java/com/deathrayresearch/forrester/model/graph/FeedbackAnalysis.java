package com.deathrayresearch.forrester.model.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Feedback loop analysis for a dependency graph.
 * Identifies all nodes and edges that participate in at least one cycle (feedback loop).
 * Loop groups represent connected sets of loop participants — each group corresponds
 * to one feedback structure in the model.
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
     * Analyzes a dependency graph to find all feedback loop participants, groups, and edges.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Run topological sort — nodes omitted from the result are cycle participants</li>
     *   <li>Build a sub-graph of just participants and their mutual edges</li>
     *   <li>Find connected components within the sub-graph = loop groups</li>
     *   <li>Collect all edges between participants within the same group = loop edges</li>
     * </ol>
     */
    public static FeedbackAnalysis analyze(DependencyGraph graph) {
        // Step 1: find loop participants via topological sort
        List<String> sorted = graph.topologicalSort();
        Set<String> sortedSet = new HashSet<>(sorted);
        Set<String> participants = new LinkedHashSet<>();
        for (String node : graph.allNodes()) {
            if (!sortedSet.contains(node)) {
                participants.add(node);
            }
        }

        if (participants.isEmpty()) {
            return new FeedbackAnalysis(
                    Collections.emptySet(),
                    Collections.emptyList(),
                    Collections.emptySet());
        }

        // Step 2: collect edges within the participant sub-graph
        Map<String, Set<String>> adjacency = graph.adjacencyMap();
        Set<Edge> edges = new LinkedHashSet<>();
        for (String from : participants) {
            Set<String> targets = adjacency.get(from);
            if (targets == null) {
                continue;
            }
            for (String to : targets) {
                if (participants.contains(to)) {
                    edges.add(new Edge(from, to));
                }
            }
        }

        // Step 3: find connected components (undirected) among participants
        List<Set<String>> groups = findConnectedComponents(participants, edges);

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
     * Returns true if the edge from → to is part of a feedback loop.
     */
    public boolean isLoopEdge(String from, String to) {
        return loopEdges.contains(new Edge(from, to));
    }

    /**
     * Finds connected components treating edges as undirected.
     */
    private static List<Set<String>> findConnectedComponents(
            Set<String> nodes, Set<Edge> edges) {

        // Build undirected adjacency
        java.util.Map<String, Set<String>> undirected = new java.util.LinkedHashMap<>();
        for (String node : nodes) {
            undirected.put(node, new LinkedHashSet<>());
        }
        for (Edge edge : edges) {
            undirected.get(edge.from()).add(edge.to());
            undirected.get(edge.to()).add(edge.from());
        }

        Set<String> visited = new HashSet<>();
        List<Set<String>> components = new ArrayList<>();

        for (String node : nodes) {
            if (visited.contains(node)) {
                continue;
            }
            Set<String> component = new LinkedHashSet<>();
            Deque<String> stack = new ArrayDeque<>();
            stack.push(node);
            while (!stack.isEmpty()) {
                String current = stack.pop();
                if (!visited.add(current)) {
                    continue;
                }
                component.add(current);
                for (String neighbor : undirected.getOrDefault(current, Collections.emptySet())) {
                    if (!visited.contains(neighbor)) {
                        stack.push(neighbor);
                    }
                }
            }
            components.add(Collections.unmodifiableSet(component));
        }

        return components;
    }
}
