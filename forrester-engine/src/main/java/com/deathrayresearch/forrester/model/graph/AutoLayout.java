package com.deathrayresearch.forrester.model.graph;

import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.ElementPlacement;
import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModuleInstanceDef;
import com.deathrayresearch.forrester.model.def.StockDef;
import com.deathrayresearch.forrester.model.def.ViewDef;

import org.eclipse.elk.alg.layered.options.CycleBreakingStrategy;
import org.eclipse.elk.alg.layered.options.LayerConstraint;
import org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider;
import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.math.ElkPadding;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.Direction;
import org.eclipse.elk.core.options.PortConstraints;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.core.util.NullElkProgressMonitor;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkGraphFactory;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates an auto-layout for a model definition using the Eclipse Layout Kernel (ELK).
 * Uses a compound node (hierarchical) approach with three bands:
 * <ul>
 *   <li>{@code band-aux}: auxiliaries not in any SCC (top)</li>
 *   <li>{@code band-main}: all stocks, flows, and SCC members of any type (middle)</li>
 *   <li>{@code band-lower}: constants, lookups, and modules not in any SCC (bottom)</li>
 * </ul>
 * The root uses Direction DOWN to stack bands vertically; each band uses Direction RIGHT
 * internally. Port constraints enforce left-in/right-out on stocks. Back-edges within SCCs
 * are marked with {@code feedbackEdge: true}.
 */
public final class AutoLayout {

    private AutoLayout() {
    }

    private static final double PADDING = 100;
    private static final double BAND_PADDING = 20;
    private static final int MATERIAL_FLOW_PRIORITY = 10;
    private static final int INFO_LINK_PRIORITY = 1;

    static {
        org.eclipse.elk.core.data.LayoutMetaDataService.getInstance()
                .registerLayoutMetaDataProviders(new LayeredMetaDataProvider());
    }

    /**
     * Generates a {@link ViewDef} with all elements placed using ELK's layered algorithm
     * in a compound node layout.
     */
    public static ViewDef layout(ModelDefinition def) {
        if (def.stocks().isEmpty() && def.flows().isEmpty() && def.auxiliaries().isEmpty()
                && def.constants().isEmpty() && def.lookupTables().isEmpty()
                && def.modules().isEmpty()) {
            return new ViewDef("Auto Layout", List.of(),
                    ConnectorGenerator.generate(def), List.of());
        }

        // Step 1: Detect SCCs in the full dependency graph
        DependencyGraph depGraph = DependencyGraph.fromDefinition(def);
        Set<String> sccMembers = depGraph.findSccMembers();

        // Step 2: Classify elements into bands
        Map<String, ElementType> typeMap = new HashMap<>();
        Map<String, Band> bandMap = new LinkedHashMap<>();

        for (StockDef s : def.stocks()) {
            typeMap.put(s.name(), ElementType.STOCK);
            bandMap.put(s.name(), Band.MAIN);
        }
        for (FlowDef f : def.flows()) {
            typeMap.put(f.name(), ElementType.FLOW);
            bandMap.put(f.name(), Band.MAIN);
        }
        for (AuxDef a : def.auxiliaries()) {
            typeMap.put(a.name(), ElementType.AUX);
            bandMap.put(a.name(), sccMembers.contains(a.name()) ? Band.MAIN : Band.AUX);
        }
        for (ConstantDef c : def.constants()) {
            typeMap.put(c.name(), ElementType.CONSTANT);
            bandMap.put(c.name(), sccMembers.contains(c.name()) ? Band.MAIN : Band.LOWER);
        }
        for (LookupTableDef t : def.lookupTables()) {
            typeMap.put(t.name(), ElementType.LOOKUP);
            bandMap.put(t.name(), sccMembers.contains(t.name()) ? Band.MAIN : Band.LOWER);
        }
        for (ModuleInstanceDef m : def.modules()) {
            typeMap.put(m.instanceName(), ElementType.MODULE);
            bandMap.put(m.instanceName(),
                    sccMembers.contains(m.instanceName()) ? Band.MAIN : Band.LOWER);
        }

        // Check which bands have content
        boolean hasAux = bandMap.values().stream().anyMatch(b -> b == Band.AUX);
        boolean hasMain = bandMap.values().stream().anyMatch(b -> b == Band.MAIN);
        boolean hasLower = bandMap.values().stream().anyMatch(b -> b == Band.LOWER);

        ElkGraphFactory factory = ElkGraphFactory.eINSTANCE;

        // Root graph node: Direction DOWN to stack bands vertically
        ElkNode root = factory.createElkNode();
        root.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered");
        root.setProperty(CoreOptions.DIRECTION, Direction.DOWN);
        root.setProperty(CoreOptions.PADDING, new ElkPadding(PADDING));
        root.setProperty(CoreOptions.SPACING_NODE_NODE, 40.0);

        // Create compound band nodes with ordering edges to enforce vertical stacking.
        // band-aux (top) → band-main (middle) → band-lower (bottom)
        ElkNode bandAux = null;
        ElkNode bandMain = null;
        ElkNode bandLower = null;

        if (hasAux) {
            bandAux = createBandNode(factory, root, "band-aux", LayerConstraint.FIRST);
        }
        if (hasMain) {
            bandMain = createBandNode(factory, root, "band-main", LayerConstraint.NONE);
            bandMain.setProperty(LayeredOptions.CYCLE_BREAKING_STRATEGY,
                    CycleBreakingStrategy.INTERACTIVE);
        }
        if (hasLower) {
            bandLower = createBandNode(factory, root, "band-lower", LayerConstraint.LAST);
        }

        // Add ordering edges between bands to guarantee vertical stacking
        if (bandAux != null && bandMain != null) {
            ElkEdge orderEdge = factory.createElkEdge();
            orderEdge.setContainingNode(root);
            orderEdge.getSources().add(bandAux);
            orderEdge.getTargets().add(bandMain);
        }
        if (bandMain != null && bandLower != null) {
            ElkEdge orderEdge = factory.createElkEdge();
            orderEdge.setContainingNode(root);
            orderEdge.getSources().add(bandMain);
            orderEdge.getTargets().add(bandLower);
        }
        if (bandAux != null && bandLower != null && bandMain == null) {
            ElkEdge orderEdge = factory.createElkEdge();
            orderEdge.setContainingNode(root);
            orderEdge.getSources().add(bandAux);
            orderEdge.getTargets().add(bandLower);
        }

        // Add element nodes to their respective bands
        Map<String, ElkNode> nodeMap = new LinkedHashMap<>();
        // Stock ports: stockName -> {WEST: port, EAST: port}
        Map<String, ElkPort> westPorts = new HashMap<>();
        Map<String, ElkPort> eastPorts = new HashMap<>();

        for (Map.Entry<String, Band> entry : bandMap.entrySet()) {
            String name = entry.getKey();
            Band band = entry.getValue();
            ElementType type = typeMap.get(name);

            ElkNode parent = switch (band) {
                case AUX -> bandAux;
                case MAIN -> bandMain;
                case LOWER -> bandLower;
            };

            ElkNode node = factory.createElkNode();
            node.setParent(parent);
            node.setIdentifier(name);
            ElementSizes size = ElementSizes.forType(type);
            node.setWidth(size.width());
            node.setHeight(size.height());
            nodeMap.put(name, node);

            // Port constraints on stocks: WEST for inflows, EAST for outflows
            if (type == ElementType.STOCK) {
                node.setProperty(CoreOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_SIDE);

                ElkPort westPort = factory.createElkPort();
                westPort.setParent(node);
                westPort.setIdentifier(name + "_west");
                westPort.setProperty(CoreOptions.PORT_SIDE, PortSide.WEST);
                westPorts.put(name, westPort);

                ElkPort eastPort = factory.createElkPort();
                eastPort.setParent(node);
                eastPort.setIdentifier(name + "_east");
                eastPort.setProperty(CoreOptions.PORT_SIDE, PortSide.EAST);
                eastPorts.put(name, eastPort);
            }
        }

        // Set initial X positions from the material flow chain so INTERACTIVE
        // cycle breaking preserves the natural left-to-right stock-flow ordering.
        Map<String, Integer> chainOrder = computeMaterialFlowOrder(def);
        double xSpacing = 200;
        for (Map.Entry<String, ElkNode> entry : nodeMap.entrySet()) {
            Integer order = chainOrder.get(entry.getKey());
            if (order != null) {
                entry.getValue().setX(order * xSpacing);
            }
        }

        // Step 3: Identify back-edges within SCCs
        Set<String> backEdgeKeys = identifyBackEdges(depGraph, def);

        // Step 4: Add material flow edges (within band-main)
        for (FlowDef f : def.flows()) {
            ElkNode flowNode = nodeMap.get(f.name());
            if (flowNode == null) {
                continue;
            }

            if (f.source() != null && nodeMap.containsKey(f.source())) {
                // source stock (EAST port) -> flow node
                ElkPort sourcePort = eastPorts.get(f.source());
                ElkEdge edge = factory.createElkEdge();
                edge.setContainingNode(bandMain);
                if (sourcePort != null) {
                    edge.getSources().add(sourcePort);
                } else {
                    edge.getSources().add(nodeMap.get(f.source()));
                }
                edge.getTargets().add(flowNode);
                edge.setProperty(CoreOptions.PRIORITY, MATERIAL_FLOW_PRIORITY);

                String edgeKey = f.source() + "->" + f.name();
                if (backEdgeKeys.contains(edgeKey)) {
                    edge.setProperty(LayeredOptions.FEEDBACK_EDGES, true);
                }
            }

            if (f.sink() != null && nodeMap.containsKey(f.sink())) {
                // flow node -> sink stock (WEST port)
                ElkPort sinkPort = westPorts.get(f.sink());
                ElkEdge edge = factory.createElkEdge();
                edge.setContainingNode(bandMain);
                edge.getSources().add(flowNode);
                if (sinkPort != null) {
                    edge.getTargets().add(sinkPort);
                } else {
                    edge.getTargets().add(nodeMap.get(f.sink()));
                }
                edge.setProperty(CoreOptions.PRIORITY, MATERIAL_FLOW_PRIORITY);

                String edgeKey = f.name() + "->" + f.sink();
                if (backEdgeKeys.contains(edgeKey)) {
                    edge.setProperty(LayeredOptions.FEEDBACK_EDGES, true);
                }
            }
        }

        // Step 5: Add within-band info link edges
        Set<String> materialEdges = buildMaterialEdgeKeys(def);
        addWithinBandInfoLinks(factory, nodeMap, bandMap, depGraph, materialEdges,
                backEdgeKeys, bandAux, bandMain, bandLower);

        // Run ELK layout
        RecursiveGraphLayoutEngine engine = new RecursiveGraphLayoutEngine();
        engine.layout(root, new NullElkProgressMonitor());

        // Extract positions: compound node offset + element offset → center coordinates
        List<ElementPlacement> placements = new ArrayList<>();
        for (Map.Entry<String, ElkNode> entry : nodeMap.entrySet()) {
            String name = entry.getKey();
            ElkNode node = entry.getValue();
            ElementType type = typeMap.get(name);

            // Parent band's position within root
            ElkNode parentBand = node.getParent();
            double bandX = parentBand.getX();
            double bandY = parentBand.getY();

            // Element center within the global coordinate system
            double cx = bandX + node.getX() + node.getWidth() / 2.0;
            double cy = bandY + node.getY() + node.getHeight() / 2.0;

            placements.add(new ElementPlacement(name, type, cx, cy));
        }

        // Resolve any remaining overlaps
        resolveOverlaps(placements);

        List<ConnectorRoute> connectors = ConnectorGenerator.generate(def);
        return new ViewDef("Auto Layout", placements, connectors, List.of());
    }

    private enum Band {
        AUX, MAIN, LOWER
    }

    private static ElkNode createBandNode(ElkGraphFactory factory, ElkNode root,
                                           String id, LayerConstraint constraint) {
        ElkNode band = factory.createElkNode();
        band.setParent(root);
        band.setIdentifier(id);
        band.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered");
        band.setProperty(CoreOptions.DIRECTION, Direction.RIGHT);
        band.setProperty(CoreOptions.PADDING, new ElkPadding(BAND_PADDING));
        band.setProperty(CoreOptions.SPACING_NODE_NODE, 60.0);
        band.setProperty(CoreOptions.SPACING_EDGE_NODE, 30.0);
        if (constraint != LayerConstraint.NONE) {
            band.setProperty(LayeredOptions.LAYERING_LAYER_CONSTRAINT, constraint);
        }
        return band;
    }

    /**
     * Computes a left-to-right ordering for all elements reachable via material flow edges.
     * Uses BFS along the material flow chain (source stock → flow → sink stock).
     * Elements not in any material flow chain get no entry (will use default position).
     */
    private static Map<String, Integer> computeMaterialFlowOrder(ModelDefinition def) {
        // Build adjacency: source_stock → flow, flow → sink_stock
        Map<String, Set<String>> fwd = new LinkedHashMap<>();
        Map<String, Set<String>> rev = new LinkedHashMap<>();
        Set<String> allChainNodes = new LinkedHashSet<>();

        for (FlowDef f : def.flows()) {
            allChainNodes.add(f.name());
            if (f.source() != null) {
                allChainNodes.add(f.source());
                fwd.computeIfAbsent(f.source(), k -> new LinkedHashSet<>()).add(f.name());
                rev.computeIfAbsent(f.name(), k -> new LinkedHashSet<>()).add(f.source());
            }
            if (f.sink() != null) {
                allChainNodes.add(f.sink());
                fwd.computeIfAbsent(f.name(), k -> new LinkedHashSet<>()).add(f.sink());
                rev.computeIfAbsent(f.sink(), k -> new LinkedHashSet<>()).add(f.name());
            }
        }

        // Find roots: nodes with no incoming material flow edge
        Deque<String> queue = new ArrayDeque<>();
        for (String node : allChainNodes) {
            if (!rev.containsKey(node)) {
                queue.add(node);
            }
        }
        // If everything is in a cycle (no roots), start from the first stock
        if (queue.isEmpty()) {
            for (StockDef s : def.stocks()) {
                if (allChainNodes.contains(s.name())) {
                    queue.add(s.name());
                    break;
                }
            }
        }

        // BFS to assign layer indices
        Map<String, Integer> order = new LinkedHashMap<>();
        while (!queue.isEmpty()) {
            String node = queue.poll();
            if (order.containsKey(node)) {
                continue;
            }
            // Layer = max of predecessors + 1
            int layer = 0;
            for (String pred : rev.getOrDefault(node, Set.of())) {
                if (order.containsKey(pred)) {
                    layer = Math.max(layer, order.get(pred) + 1);
                }
            }
            order.put(node, layer);
            for (String succ : fwd.getOrDefault(node, Set.of())) {
                if (!order.containsKey(succ)) {
                    queue.add(succ);
                }
            }
        }

        // Assign remaining chain nodes that weren't reached (isolated cycles)
        for (String node : allChainNodes) {
            if (!order.containsKey(node)) {
                order.put(node, 0);
            }
        }

        return order;
    }

    /**
     * Identifies back-edges within SCCs using DFS.
     * Prefers info-link back-edges over material-flow back-edges.
     */
    private static Set<String> identifyBackEdges(DependencyGraph depGraph, ModelDefinition def) {
        Set<String> backEdges = new HashSet<>();

        for (Set<String> scc : depGraph.findSCCs()) {
            String root = pickDfsRoot(scc, depGraph, def);

            Set<String> visited = new HashSet<>();
            Set<String> inStack = new HashSet<>();
            Set<String> sccBackEdges = new HashSet<>();
            dfsMarkBackEdges(root, scc, depGraph, visited, inStack, sccBackEdges);

            backEdges.addAll(sccBackEdges);
        }

        return backEdges;
    }

    private static String pickDfsRoot(Set<String> scc, DependencyGraph depGraph,
                                       ModelDefinition def) {
        Set<String> stockNames = new HashSet<>();
        for (StockDef s : def.stocks()) {
            stockNames.add(s.name());
        }

        String bestStock = null;
        int bestStockDegree = -1;
        String bestOther = null;
        int bestOtherDegree = -1;

        for (String node : scc) {
            int degree = depGraph.dependenciesOf(node).size() + depGraph.dependentsOf(node).size();
            if (stockNames.contains(node)) {
                if (degree > bestStockDegree) {
                    bestStockDegree = degree;
                    bestStock = node;
                }
            } else {
                if (degree > bestOtherDegree) {
                    bestOtherDegree = degree;
                    bestOther = node;
                }
            }
        }
        return bestStock != null ? bestStock : bestOther;
    }

    private static void dfsMarkBackEdges(String node, Set<String> scc,
                                          DependencyGraph depGraph,
                                          Set<String> visited, Set<String> inStack,
                                          Set<String> backEdges) {
        visited.add(node);
        inStack.add(node);

        for (String neighbor : depGraph.dependentsOf(node)) {
            if (!scc.contains(neighbor)) {
                continue;
            }
            String edgeKey = node + "->" + neighbor;
            if (inStack.contains(neighbor)) {
                // Back-edge found
                backEdges.add(edgeKey);
            } else if (!visited.contains(neighbor)) {
                dfsMarkBackEdges(neighbor, scc, depGraph, visited, inStack, backEdges);
            }
        }

        inStack.remove(node);
    }

    /**
     * Adds info link edges within each band (excluding cross-band connections).
     */
    private static void addWithinBandInfoLinks(ElkGraphFactory factory,
                                                Map<String, ElkNode> nodeMap,
                                                Map<String, Band> bandMap,
                                                DependencyGraph depGraph,
                                                Set<String> materialEdges,
                                                Set<String> backEdgeKeys,
                                                ElkNode bandAux,
                                                ElkNode bandMain,
                                                ElkNode bandLower) {
        for (String[] edge : depGraph.allEdges()) {
            String from = edge[0];
            String to = edge[1];

            // Skip material edges (already added as high-priority flow edges)
            String key = from + "->" + to;
            if (materialEdges.contains(key)) {
                continue;
            }

            ElkNode sourceNode = nodeMap.get(from);
            ElkNode targetNode = nodeMap.get(to);
            if (sourceNode == null || targetNode == null) {
                continue;
            }

            // Only add within-band edges; cross-band info links are rendered directly
            Band fromBand = bandMap.get(from);
            Band toBand = bandMap.get(to);
            if (fromBand != toBand) {
                continue;
            }

            ElkNode containingBand = switch (fromBand) {
                case AUX -> bandAux;
                case MAIN -> bandMain;
                case LOWER -> bandLower;
            };

            if (containingBand == null) {
                continue;
            }

            ElkEdge elkEdge = factory.createElkEdge();
            elkEdge.setContainingNode(containingBand);
            elkEdge.getSources().add(sourceNode);
            elkEdge.getTargets().add(targetNode);
            elkEdge.setProperty(CoreOptions.PRIORITY, INFO_LINK_PRIORITY);

            if (backEdgeKeys.contains(key)) {
                elkEdge.setProperty(LayeredOptions.FEEDBACK_EDGES, true);
            }
        }
    }

    /**
     * Builds a set of edge keys (both directions) for stock-flow material connections.
     */
    private static Set<String> buildMaterialEdgeKeys(ModelDefinition def) {
        Set<String> keys = new HashSet<>();
        for (FlowDef f : def.flows()) {
            if (f.source() != null) {
                keys.add(f.source() + "->" + f.name());
                keys.add(f.name() + "->" + f.source());
            }
            if (f.sink() != null) {
                keys.add(f.name() + "->" + f.sink());
                keys.add(f.sink() + "->" + f.name());
            }
        }
        return keys;
    }

    /**
     * Resolves overlaps within each Y band by redistributing elements symmetrically
     * around their centroid, preserving order and maintaining a minimum gap.
     */
    private static void resolveOverlaps(List<ElementPlacement> placements) {
        Map<Double, List<Integer>> byY = new LinkedHashMap<>();
        for (int i = 0; i < placements.size(); i++) {
            byY.computeIfAbsent(placements.get(i).y(), k -> new ArrayList<>()).add(i);
        }

        double minGap = 20;
        for (List<Integer> indices : byY.values()) {
            if (indices.size() <= 1) {
                continue;
            }
            indices.sort((a, b) -> Double.compare(placements.get(a).x(), placements.get(b).x()));

            boolean hasOverlap = false;
            for (int i = 1; i < indices.size(); i++) {
                ElementPlacement prev = placements.get(indices.get(i - 1));
                ElementPlacement curr = placements.get(indices.get(i));
                double prevRight = prev.x() + ElementSizes.forType(prev.type()).width() / 2.0;
                double currLeft = curr.x() - ElementSizes.forType(curr.type()).width() / 2.0;
                if (currLeft < prevRight + minGap) {
                    hasOverlap = true;
                    break;
                }
            }

            if (!hasOverlap) {
                continue;
            }

            double centroid = 0;
            for (int idx : indices) {
                centroid += placements.get(idx).x();
            }
            centroid /= indices.size();

            double totalSpan = 0;
            for (int idx : indices) {
                totalSpan += ElementSizes.forType(placements.get(idx).type()).width();
            }
            totalSpan += minGap * (indices.size() - 1);

            double cursor = centroid - totalSpan / 2.0;
            for (int idx : indices) {
                ElementPlacement ep = placements.get(idx);
                double w = ElementSizes.forType(ep.type()).width();
                double newX = cursor + w / 2.0;
                placements.set(idx, new ElementPlacement(ep.name(), ep.type(), newX, ep.y()));
                cursor += w + minGap;
            }
        }
    }
}
