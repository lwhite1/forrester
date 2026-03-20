package systems.courant.sd.model.graph;

import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.CldVariableDef;
import systems.courant.sd.model.def.ElementPlacement;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ViewDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CLD-specific layout algorithm. Detects whether the causal link graph
 * is cyclic or a DAG and applies the appropriate placement strategy:
 * <ul>
 *   <li><b>Cyclic</b>: Seeds nodes on an ellipse and runs force-directed
 *       relaxation so feedback loops form natural circular shapes.</li>
 *   <li><b>DAG</b>: Longest-path rank assignment with evenly spaced
 *       nodes per rank (layered left-to-right).</li>
 * </ul>
 * All placements are centered on the canvas centroid (600, 400).
 */
public final class CldLayout {

    private static final double CENTER_X = 600;
    private static final double CENTER_Y = 400;

    /** Spring constant for edge attractions in force-directed layout. */
    private static final double K_SPRING = 0.35;

    /** Repulsion constant (Coulomb-style). */
    private static final double K_REPULSION = 50_000;

    /** Centripetal pull toward center (prevents drift). */
    private static final double K_CENTER = 0.002;

    /** Number of force-directed iterations. */
    private static final int ITERATIONS = 75;

    /** Damping factor to improve convergence. */
    private static final double DAMPING = 0.9;

    /** Maximum displacement per iteration to avoid explosion. */
    private static final double MAX_DISPLACEMENT = 80;

    /** Horizontal spacing between ranks for DAG layout. */
    private static final double RANK_SPACING = 200;

    /** Vertical spacing between nodes within a rank. */
    private static final double NODE_SPACING = 120;

    private CldLayout() {
    }

    /**
     * Computes a layout for a pure CLD model.
     *
     * @param def the model definition (must have CLD variables and causal links)
     * @return a ViewDef with element placements
     */
    public static ViewDef layout(ModelDefinition def) {
        List<CldVariableDef> vars = def.cldVariables();
        List<CausalLinkDef> links = def.causalLinks();

        if (vars.isEmpty()) {
            return new ViewDef("Main", List.of(), List.of(), List.of());
        }

        // Build adjacency
        Set<String> nodes = new LinkedHashSet<>();
        Map<String, Set<String>> adj = new LinkedHashMap<>();
        for (CldVariableDef v : vars) {
            nodes.add(v.name());
            adj.putIfAbsent(v.name(), new LinkedHashSet<>());
        }
        for (CausalLinkDef link : links) {
            adj.computeIfAbsent(link.from(), k -> new LinkedHashSet<>()).add(link.to());
        }

        // Single node — place at center
        if (nodes.size() == 1) {
            String name = nodes.iterator().next();
            List<ElementPlacement> placements = List.of(
                    new ElementPlacement(name, ElementType.CLD_VARIABLE, CENTER_X, CENTER_Y));
            return new ViewDef("Main", placements, List.of(), List.of());
        }

        // Detect cycles
        List<Set<String>> sccs = TarjanSCC.findNonTrivial(nodes, adj);
        boolean hasCycle = !sccs.isEmpty();

        Map<String, double[]> positions;
        if (hasCycle) {
            positions = forceDirectedLayout(nodes, adj);
        } else {
            positions = dagLayout(nodes, adj);
        }

        // Enforce minimum inter-node distance
        enforceMinDistance(positions, nodes, adj);

        // Center on canvas
        centerOnCanvas(positions);

        List<ElementPlacement> placements = new ArrayList<>();
        for (String name : nodes) {
            double[] pos = positions.get(name);
            if (pos != null) {
                placements.add(new ElementPlacement(name, ElementType.CLD_VARIABLE,
                        pos[0], pos[1]));
            }
        }
        return new ViewDef("Main", placements, List.of(), List.of());
    }

    /**
     * Force-directed layout for cyclic graphs. Seeds nodes on an ellipse,
     * then iteratively applies repulsion, attraction, and centripetal forces.
     */
    static Map<String, double[]> forceDirectedLayout(Set<String> nodes,
                                                      Map<String, Set<String>> adj) {
        int n = nodes.size();
        double radius = Math.max(200, n * 40);
        Map<String, double[]> positions = new LinkedHashMap<>();

        // Seed on ellipse
        int i = 0;
        for (String node : nodes) {
            double angle = 2 * Math.PI * i / n;
            positions.put(node, new double[]{
                    CENTER_X + radius * Math.cos(angle),
                    CENTER_Y + radius * 0.7 * Math.sin(angle)
            });
            i++;
        }

        List<String> nodeList = new ArrayList<>(nodes);
        Map<String, double[]> displacements = new HashMap<>();

        for (int iter = 0; iter < ITERATIONS; iter++) {
            // Reset displacements
            for (String node : nodeList) {
                displacements.put(node, new double[]{0, 0});
            }

            // Repulsion between all pairs
            for (int a = 0; a < nodeList.size(); a++) {
                for (int b = a + 1; b < nodeList.size(); b++) {
                    String na = nodeList.get(a);
                    String nb = nodeList.get(b);
                    double[] pa = positions.get(na);
                    double[] pb = positions.get(nb);
                    double dx = pa[0] - pb[0];
                    double dy = pa[1] - pb[1];
                    double dist2 = dx * dx + dy * dy;
                    if (dist2 < 1) {
                        dist2 = 1;
                    }
                    double dist = Math.sqrt(dist2);
                    double force = K_REPULSION / dist2;
                    double fx = force * dx / dist;
                    double fy = force * dy / dist;

                    displacements.get(na)[0] += fx;
                    displacements.get(na)[1] += fy;
                    displacements.get(nb)[0] -= fx;
                    displacements.get(nb)[1] -= fy;
                }
            }

            // Edge spring attraction
            for (Map.Entry<String, Set<String>> entry : adj.entrySet()) {
                String from = entry.getKey();
                double[] pf = positions.get(from);
                if (pf == null) {
                    continue;
                }
                for (String to : entry.getValue()) {
                    double[] pt = positions.get(to);
                    if (pt == null) {
                        continue;
                    }
                    double dx = pt[0] - pf[0];
                    double dy = pt[1] - pf[1];
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist < 1) {
                        continue;
                    }
                    double force = K_SPRING * dist;
                    double fx = force * dx / dist;
                    double fy = force * dy / dist;

                    displacements.get(from)[0] += fx;
                    displacements.get(from)[1] += fy;
                    displacements.get(to)[0] -= fx;
                    displacements.get(to)[1] -= fy;
                }
            }

            // Centripetal pull toward center
            for (String node : nodeList) {
                double[] p = positions.get(node);
                double dx = CENTER_X - p[0];
                double dy = CENTER_Y - p[1];
                displacements.get(node)[0] += K_CENTER * dx;
                displacements.get(node)[1] += K_CENTER * dy;
            }

            // Apply displacements with damping and max displacement cap
            double damp = DAMPING * (1.0 - (double) iter / ITERATIONS);
            for (String node : nodeList) {
                double[] d = displacements.get(node);
                double mag = Math.sqrt(d[0] * d[0] + d[1] * d[1]);
                if (mag > MAX_DISPLACEMENT) {
                    d[0] *= MAX_DISPLACEMENT / mag;
                    d[1] *= MAX_DISPLACEMENT / mag;
                }
                double[] p = positions.get(node);
                p[0] += d[0] * damp;
                p[1] += d[1] * damp;
            }
        }

        return positions;
    }

    /**
     * DAG layout using longest-path rank assignment.
     * Nodes are placed left-to-right by rank, evenly spaced vertically per rank.
     */
    static Map<String, double[]> dagLayout(Set<String> nodes,
                                            Map<String, Set<String>> adj) {
        // Build in-degree and reverse adjacency
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        Map<String, Set<String>> reverseAdj = new LinkedHashMap<>();
        for (String n : nodes) {
            inDegree.put(n, 0);
            reverseAdj.put(n, new LinkedHashSet<>());
        }
        for (Map.Entry<String, Set<String>> entry : adj.entrySet()) {
            for (String to : entry.getValue()) {
                if (nodes.contains(to)) {
                    inDegree.merge(to, 1, Integer::sum);
                    reverseAdj.computeIfAbsent(to, k -> new LinkedHashSet<>())
                            .add(entry.getKey());
                }
            }
        }

        // Find roots (in-degree 0)
        List<String> roots = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                roots.add(entry.getKey());
            }
        }

        // If no roots (shouldn't happen for a DAG, but handle disconnected nodes),
        // pick the first node
        if (roots.isEmpty()) {
            roots.add(nodes.iterator().next());
        }

        // Longest-path rank assignment via BFS
        Map<String, Integer> ranks = new LinkedHashMap<>();
        List<String> queue = new ArrayList<>(roots);
        for (String root : roots) {
            ranks.put(root, 0);
        }

        int head = 0;
        while (head < queue.size()) {
            String current = queue.get(head++);
            int currentRank = ranks.get(current);
            for (String neighbor : adj.getOrDefault(current, Collections.emptySet())) {
                if (!nodes.contains(neighbor)) {
                    continue;
                }
                int newRank = currentRank + 1;
                if (!ranks.containsKey(neighbor) || ranks.get(neighbor) < newRank) {
                    ranks.put(neighbor, newRank);
                    queue.add(neighbor);
                }
            }
        }

        // Place any disconnected nodes not yet ranked
        for (String n : nodes) {
            ranks.putIfAbsent(n, 0);
        }

        // Group by rank
        Map<Integer, List<String>> rankGroups = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : ranks.entrySet()) {
            rankGroups.computeIfAbsent(entry.getValue(), k -> new ArrayList<>())
                    .add(entry.getKey());
        }

        // Assign positions
        Map<String, double[]> positions = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<String>> entry : rankGroups.entrySet()) {
            int rank = entry.getKey();
            List<String> group = entry.getValue();
            double x = rank * RANK_SPACING;
            double totalHeight = (group.size() - 1) * NODE_SPACING;
            double startY = -totalHeight / 2;
            for (int j = 0; j < group.size(); j++) {
                positions.put(group.get(j), new double[]{x, startY + j * NODE_SPACING});
            }
        }

        return positions;
    }

    /**
     * Enforces minimum inter-node distance by nudging overlapping nodes apart.
     * Minimum distance is 2.5 * k * average edge length.
     */
    static void enforceMinDistance(Map<String, double[]> positions,
                                   Set<String> nodes,
                                   Map<String, Set<String>> adj) {
        // Compute average edge length
        double totalLen = 0;
        int edgeCount = 0;
        for (Map.Entry<String, Set<String>> entry : adj.entrySet()) {
            double[] pf = positions.get(entry.getKey());
            if (pf == null) {
                continue;
            }
            for (String to : entry.getValue()) {
                double[] pt = positions.get(to);
                if (pt == null) {
                    continue;
                }
                double dx = pt[0] - pf[0];
                double dy = pt[1] - pf[1];
                totalLen += Math.sqrt(dx * dx + dy * dy);
                edgeCount++;
            }
        }

        if (edgeCount == 0) {
            return;
        }

        double avgEdgeLen = totalLen / edgeCount;
        double minDist = 2.5 * K_SPRING * avgEdgeLen;
        // Ensure a reasonable minimum
        minDist = Math.max(minDist, 80);

        List<String> nodeList = new ArrayList<>(nodes);
        // Simple iterative nudging
        for (int iter = 0; iter < 20; iter++) {
            boolean anyMoved = false;
            for (int a = 0; a < nodeList.size(); a++) {
                for (int b = a + 1; b < nodeList.size(); b++) {
                    double[] pa = positions.get(nodeList.get(a));
                    double[] pb = positions.get(nodeList.get(b));
                    if (pa == null || pb == null) {
                        continue;
                    }
                    double dx = pb[0] - pa[0];
                    double dy = pb[1] - pa[1];
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist < minDist && dist > 0.01) {
                        double push = (minDist - dist) / 2;
                        double nx = dx / dist;
                        double ny = dy / dist;
                        pa[0] -= nx * push;
                        pa[1] -= ny * push;
                        pb[0] += nx * push;
                        pb[1] += ny * push;
                        anyMoved = true;
                    } else if (dist <= 0.01) {
                        // Coincident nodes — nudge apart
                        pa[0] -= minDist / 2;
                        pb[0] += minDist / 2;
                        anyMoved = true;
                    }
                }
            }
            if (!anyMoved) {
                break;
            }
        }
    }

    /**
     * Centers all positions on the canvas centroid.
     */
    private static void centerOnCanvas(Map<String, double[]> positions) {
        if (positions.isEmpty()) {
            return;
        }
        double sumX = 0, sumY = 0;
        for (double[] p : positions.values()) {
            sumX += p[0];
            sumY += p[1];
        }
        double n = positions.size();
        double offsetX = CENTER_X - sumX / n;
        double offsetY = CENTER_Y - sumY / n;
        for (double[] p : positions.values()) {
            p[0] += offsetX;
            p[1] += offsetY;
        }
    }
}
