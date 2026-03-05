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
 * Uses a single flat layered (Sugiyama) graph with Direction RIGHT. Topology drives all
 * positioning — elements are placed where the dependency graph puts them, not segregated
 * by type. Port constraints enforce left-in/right-out on stocks. INTERACTIVE cycle breaking
 * with pre-computed material-flow-chain positions preserves natural edge directions.
 */
public final class AutoLayout {

    private AutoLayout() {
    }

    private static final double PADDING = 100;
    private static final int MATERIAL_FLOW_PRIORITY = 10;
    private static final int INFO_LINK_PRIORITY = 1;

    static {
        org.eclipse.elk.core.data.LayoutMetaDataService.getInstance()
                .registerLayoutMetaDataProviders(new LayeredMetaDataProvider());
    }

    /**
     * Generates a {@link ViewDef} with all elements placed using ELK's layered algorithm.
     */
    public static ViewDef layout(ModelDefinition def) {
        if (def.stocks().isEmpty() && def.flows().isEmpty() && def.auxiliaries().isEmpty()
                && def.constants().isEmpty() && def.lookupTables().isEmpty()
                && def.modules().isEmpty()) {
            return new ViewDef("Auto Layout", List.of(),
                    ConnectorGenerator.generate(def), List.of());
        }

        DependencyGraph depGraph = DependencyGraph.fromDefinition(def);

        ElkGraphFactory factory = ElkGraphFactory.eINSTANCE;

        // Single flat graph — topology drives all positioning
        ElkNode root = factory.createElkNode();
        root.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered");
        root.setProperty(CoreOptions.DIRECTION, Direction.RIGHT);
        root.setProperty(CoreOptions.PADDING, new ElkPadding(PADDING));
        root.setProperty(CoreOptions.SPACING_NODE_NODE, 60.0);
        root.setProperty(CoreOptions.SPACING_EDGE_NODE, 30.0);
        root.setProperty(LayeredOptions.CYCLE_BREAKING_STRATEGY,
                CycleBreakingStrategy.INTERACTIVE);

        // Create all element nodes
        Map<String, ElkNode> nodeMap = new LinkedHashMap<>();
        Map<String, ElementType> typeMap = new HashMap<>();
        Map<String, ElkPort> westPorts = new HashMap<>();
        Map<String, ElkPort> eastPorts = new HashMap<>();

        for (StockDef s : def.stocks()) {
            addNode(factory, root, nodeMap, typeMap, s.name(), ElementType.STOCK,
                    westPorts, eastPorts);
        }
        for (FlowDef f : def.flows()) {
            addNode(factory, root, nodeMap, typeMap, f.name(), ElementType.FLOW,
                    westPorts, eastPorts);
        }
        for (AuxDef a : def.auxiliaries()) {
            addNode(factory, root, nodeMap, typeMap, a.name(), ElementType.AUX,
                    westPorts, eastPorts);
        }
        for (ConstantDef c : def.constants()) {
            addNode(factory, root, nodeMap, typeMap, c.name(), ElementType.CONSTANT,
                    westPorts, eastPorts);
        }
        for (LookupTableDef t : def.lookupTables()) {
            addNode(factory, root, nodeMap, typeMap, t.name(), ElementType.LOOKUP,
                    westPorts, eastPorts);
        }
        for (ModuleInstanceDef m : def.modules()) {
            addNode(factory, root, nodeMap, typeMap, m.instanceName(), ElementType.MODULE,
                    westPorts, eastPorts);
        }

        // Set initial X positions from the material flow chain so INTERACTIVE
        // cycle breaking preserves the natural left-to-right stock-flow ordering.
        Map<String, Integer> chainOrder = computeMaterialFlowOrder(def);

        // Assign X positions to non-chain nodes (constants, auxiliaries) based on
        // their consumers' chain positions — place them just before their earliest consumer.
        assignNonChainPositions(chainOrder, depGraph, nodeMap);

        double xSpacing = 200;
        for (Map.Entry<String, ElkNode> entry : nodeMap.entrySet()) {
            Integer order = chainOrder.get(entry.getKey());
            if (order != null) {
                entry.getValue().setX(order * xSpacing);
            }
        }

        // Identify back-edges within SCCs using the material flow chain order.
        // An edge is a back-edge if it goes from a later chain position to an earlier one.
        Set<String> backEdgeKeys = identifyBackEdges(depGraph, def, chainOrder);

        // Add material flow edges (high priority)
        for (FlowDef f : def.flows()) {
            ElkNode flowNode = nodeMap.get(f.name());
            if (flowNode == null) {
                continue;
            }

            if (f.source() != null && nodeMap.containsKey(f.source())) {
                ElkPort sourcePort = eastPorts.get(f.source());
                ElkEdge edge = createEdge(factory, root,
                        sourcePort != null ? sourcePort : nodeMap.get(f.source()),
                        flowNode);
                edge.setProperty(CoreOptions.PRIORITY, MATERIAL_FLOW_PRIORITY);
                if (backEdgeKeys.contains(f.source() + "->" + f.name())) {
                    edge.setProperty(LayeredOptions.FEEDBACK_EDGES, true);
                }
            }

            if (f.sink() != null && nodeMap.containsKey(f.sink())) {
                ElkPort sinkPort = westPorts.get(f.sink());
                ElkEdge edge = createEdge(factory, root,
                        flowNode,
                        sinkPort != null ? sinkPort : nodeMap.get(f.sink()));
                edge.setProperty(CoreOptions.PRIORITY, MATERIAL_FLOW_PRIORITY);
                if (backEdgeKeys.contains(f.name() + "->" + f.sink())) {
                    edge.setProperty(LayeredOptions.FEEDBACK_EDGES, true);
                }
            }
        }

        // Add info link edges (low priority, excluding material edge duplicates)
        Set<String> materialEdges = buildMaterialEdgeKeys(def);
        for (String[] edge : depGraph.allEdges()) {
            String from = edge[0];
            String to = edge[1];
            String key = from + "->" + to;
            if (materialEdges.contains(key)) {
                continue;
            }

            ElkNode sourceNode = nodeMap.get(from);
            ElkNode targetNode = nodeMap.get(to);
            if (sourceNode == null || targetNode == null) {
                continue;
            }

            ElkEdge elkEdge = createEdge(factory, root, sourceNode, targetNode);
            elkEdge.setProperty(CoreOptions.PRIORITY, INFO_LINK_PRIORITY);
            if (backEdgeKeys.contains(key)) {
                elkEdge.setProperty(LayeredOptions.FEEDBACK_EDGES, true);
            }
        }

        // Run ELK layout
        RecursiveGraphLayoutEngine engine = new RecursiveGraphLayoutEngine();
        engine.layout(root, new NullElkProgressMonitor());

        // Extract center coordinates from ELK's top-left positions
        List<ElementPlacement> placements = new ArrayList<>();
        for (Map.Entry<String, ElkNode> entry : nodeMap.entrySet()) {
            String name = entry.getKey();
            ElkNode node = entry.getValue();
            ElementType type = typeMap.get(name);
            double cx = node.getX() + node.getWidth() / 2.0;
            double cy = node.getY() + node.getHeight() / 2.0;
            placements.add(new ElementPlacement(name, type, cx, cy));
        }

        alignFlowsWithStocks(placements, def);
        resolveOverlaps(placements);

        List<ConnectorRoute> connectors = ConnectorGenerator.generate(def);
        return new ViewDef("Auto Layout", placements, connectors, List.of());
    }

    private static void addNode(ElkGraphFactory factory, ElkNode root,
                                Map<String, ElkNode> nodeMap,
                                Map<String, ElementType> typeMap,
                                String name, ElementType type,
                                Map<String, ElkPort> westPorts,
                                Map<String, ElkPort> eastPorts) {
        ElkNode node = factory.createElkNode();
        node.setParent(root);
        node.setIdentifier(name);
        ElementSizes size = ElementSizes.forType(type);
        node.setWidth(size.width());
        node.setHeight(size.height());
        nodeMap.put(name, node);
        typeMap.put(name, type);

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

    private static ElkEdge createEdge(ElkGraphFactory factory, ElkNode root,
                                      org.eclipse.elk.graph.ElkConnectableShape source,
                                      org.eclipse.elk.graph.ElkConnectableShape target) {
        ElkEdge edge = factory.createElkEdge();
        edge.setContainingNode(root);
        edge.getSources().add(source);
        edge.getTargets().add(target);
        return edge;
    }

    /**
     * Computes a left-to-right ordering for all elements reachable via material flow edges.
     * Uses BFS along the material flow chain (source stock → flow → sink stock).
     */
    private static Map<String, Integer> computeMaterialFlowOrder(ModelDefinition def) {
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

        Deque<String> queue = new ArrayDeque<>();
        for (String node : allChainNodes) {
            if (!rev.containsKey(node)) {
                queue.add(node);
            }
        }
        if (queue.isEmpty()) {
            for (StockDef s : def.stocks()) {
                if (allChainNodes.contains(s.name())) {
                    queue.add(s.name());
                    break;
                }
            }
        }

        Map<String, Integer> order = new LinkedHashMap<>();
        while (!queue.isEmpty()) {
            String node = queue.poll();
            if (order.containsKey(node)) {
                continue;
            }
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

        for (String node : allChainNodes) {
            if (!order.containsKey(node)) {
                order.put(node, 0);
            }
        }

        return order;
    }

    /**
     * Aligns each flow's Y coordinate with its connected stock(s) so that
     * flow pipes run horizontally. For transfer flows (source + sink),
     * uses the Y of whichever stock is closest in the X direction.
     */
    private static final double BACK_FLOW_Y_OFFSET = 120;

    private static void alignFlowsWithStocks(List<ElementPlacement> placements,
                                              ModelDefinition def) {
        Map<String, ElementPlacement> map = new HashMap<>();
        for (ElementPlacement p : placements) {
            map.put(p.name(), p);
        }

        // Find the maximum Y across all stocks for back-flow positioning
        double maxStockY = Double.NEGATIVE_INFINITY;
        for (ElementPlacement p : placements) {
            if (p.type() == ElementType.STOCK) {
                maxStockY = Math.max(maxStockY, p.y());
            }
        }
        if (maxStockY == Double.NEGATIVE_INFINITY) {
            maxStockY = 200;
        }

        for (int i = 0; i < placements.size(); i++) {
            ElementPlacement fp = placements.get(i);
            if (fp.type() != ElementType.FLOW) {
                continue;
            }

            // Find the matching FlowDef
            FlowDef flowDef = null;
            for (FlowDef f : def.flows()) {
                if (f.name().equals(fp.name())) {
                    flowDef = f;
                    break;
                }
            }
            if (flowDef == null) {
                continue;
            }

            ElementPlacement source = flowDef.source() != null ? map.get(flowDef.source()) : null;
            ElementPlacement sink = flowDef.sink() != null ? map.get(flowDef.sink()) : null;

            if (source != null && sink != null && sink.x() < source.x()) {
                // Back-flow: sink is to the left of source — place below the chain,
                // centered between source and sink for a visual loop
                double backX = (source.x() + sink.x()) / 2;
                double backY = maxStockY + BACK_FLOW_Y_OFFSET;
                placements.set(i, new ElementPlacement(fp.name(), fp.type(), backX, backY));
            } else {
                double targetY;
                if (source != null && sink != null) {
                    // Transfer flow: align with the closer stock
                    double distToSource = Math.abs(fp.x() - source.x());
                    double distToSink = Math.abs(fp.x() - sink.x());
                    targetY = distToSource <= distToSink ? source.y() : sink.y();
                } else if (source != null) {
                    targetY = source.y();
                } else if (sink != null) {
                    targetY = sink.y();
                } else {
                    continue;
                }
                placements.set(i, new ElementPlacement(fp.name(), fp.type(), fp.x(), targetY));
            }
        }
    }

    /**
     * Assigns chain order positions to nodes not in the material flow chain
     * (constants, auxiliaries, lookup tables). Each non-chain node gets placed
     * one step before its earliest consumer in the chain.
     */
    private static void assignNonChainPositions(Map<String, Integer> chainOrder,
                                                 DependencyGraph depGraph,
                                                 Map<String, ElkNode> nodeMap) {
        for (String name : nodeMap.keySet()) {
            if (chainOrder.containsKey(name)) {
                continue;
            }
            int minConsumerOrder = Integer.MAX_VALUE;
            for (String dependent : depGraph.dependentsOf(name)) {
                Integer order = chainOrder.get(dependent);
                if (order != null) {
                    minConsumerOrder = Math.min(minConsumerOrder, order);
                }
            }
            if (minConsumerOrder == Integer.MAX_VALUE) {
                // No consumers in the chain — place at 0
                chainOrder.put(name, 0);
            } else {
                // Place one step before the earliest consumer (may be negative)
                chainOrder.put(name, minConsumerOrder - 1);
            }
        }
    }

    /**
     * Identifies back-edges within SCCs by comparing the material flow chain order.
     * An edge going from a higher chain position to a lower one is a back-edge
     * (it goes against the natural left-to-right material flow direction).
     */
    private static Set<String> identifyBackEdges(DependencyGraph depGraph,
                                                  ModelDefinition def,
                                                  Map<String, Integer> chainOrder) {
        Set<String> backEdges = new HashSet<>();

        for (Set<String> scc : depGraph.findSCCs()) {
            for (String node : scc) {
                for (String neighbor : depGraph.dependentsOf(node)) {
                    if (!scc.contains(neighbor)) {
                        continue;
                    }
                    Integer fromOrder = chainOrder.get(node);
                    Integer toOrder = chainOrder.get(neighbor);
                    if (fromOrder != null && toOrder != null && fromOrder > toOrder) {
                        backEdges.add(node + "->" + neighbor);
                    }
                }
            }
        }

        return backEdges;
    }

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
     * Resolves overlaps by checking all element pairs and nudging apart any that overlap.
     */
    private static void resolveOverlaps(List<ElementPlacement> placements) {
        double minGap = 20;
        for (int pass = 0; pass < 3; pass++) {
            boolean changed = false;
            for (int i = 0; i < placements.size(); i++) {
                for (int j = i + 1; j < placements.size(); j++) {
                    ElementPlacement a = placements.get(i);
                    ElementPlacement b = placements.get(j);
                    double aw = ElementSizes.forType(a.type()).width() / 2.0;
                    double ah = ElementSizes.forType(a.type()).height() / 2.0;
                    double bw = ElementSizes.forType(b.type()).width() / 2.0;
                    double bh = ElementSizes.forType(b.type()).height() / 2.0;

                    double overlapX = (aw + bw + minGap) - Math.abs(a.x() - b.x());
                    double overlapY = (ah + bh + minGap) - Math.abs(a.y() - b.y());

                    if (overlapX > 0 && overlapY > 0) {
                        // Push apart along the axis with less overlap
                        if (overlapX < overlapY) {
                            double nudge = overlapX / 2.0;
                            if (a.x() <= b.x()) {
                                placements.set(i, new ElementPlacement(a.name(), a.type(),
                                        a.x() - nudge, a.y()));
                                placements.set(j, new ElementPlacement(b.name(), b.type(),
                                        b.x() + nudge, b.y()));
                            } else {
                                placements.set(i, new ElementPlacement(a.name(), a.type(),
                                        a.x() + nudge, a.y()));
                                placements.set(j, new ElementPlacement(b.name(), b.type(),
                                        b.x() - nudge, b.y()));
                            }
                        } else {
                            double nudge = overlapY / 2.0;
                            if (a.y() <= b.y()) {
                                placements.set(i, new ElementPlacement(a.name(), a.type(),
                                        a.x(), a.y() - nudge));
                                placements.set(j, new ElementPlacement(b.name(), b.type(),
                                        b.x(), b.y() + nudge));
                            } else {
                                placements.set(i, new ElementPlacement(a.name(), a.type(),
                                        a.x(), a.y() + nudge));
                                placements.set(j, new ElementPlacement(b.name(), b.type(),
                                        b.x(), b.y() - nudge));
                            }
                        }
                        changed = true;
                    }
                }
            }
            if (!changed) {
                break;
            }
        }
    }
}
