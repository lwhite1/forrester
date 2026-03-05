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

import org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.math.ElkPadding;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.Direction;
import org.eclipse.elk.core.util.NullElkProgressMonitor;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkGraphFactory;
import org.eclipse.elk.graph.ElkNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates an auto-layout for a model definition using the Eclipse Layout Kernel (ELK).
 * Uses the layered (Sugiyama) algorithm with left-to-right direction for horizontal positioning.
 * Vertical positions follow SD conventions: auxiliaries above, stocks/flows in the middle,
 * constants and lookups below, modules at the bottom.
 */
public final class AutoLayout {

    private AutoLayout() {
    }

    private static final double PADDING = 100;
    private static final double Y_AUX = 80;
    private static final double Y_STOCK_FLOW = 220;
    private static final double Y_CONSTANT = 380;
    private static final double Y_LOOKUP = 450;
    private static final double Y_MODULE = 520;
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

        ElkGraphFactory factory = ElkGraphFactory.eINSTANCE;

        // Root graph node
        ElkNode root = factory.createElkNode();
        root.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered");
        root.setProperty(CoreOptions.DIRECTION, Direction.RIGHT);
        root.setProperty(CoreOptions.PADDING, new ElkPadding(PADDING));
        root.setProperty(CoreOptions.SPACING_NODE_NODE, 60.0);
        root.setProperty(CoreOptions.SPACING_EDGE_NODE, 30.0);

        // Map element names to ELK nodes and types
        Map<String, ElkNode> nodeMap = new LinkedHashMap<>();
        Map<String, ElementType> typeMap = new HashMap<>();

        for (StockDef s : def.stocks()) {
            addNode(factory, root, nodeMap, typeMap, s.name(), ElementType.STOCK);
        }
        for (FlowDef f : def.flows()) {
            addNode(factory, root, nodeMap, typeMap, f.name(), ElementType.FLOW);
        }
        for (AuxDef a : def.auxiliaries()) {
            addNode(factory, root, nodeMap, typeMap, a.name(), ElementType.AUX);
        }
        for (ConstantDef c : def.constants()) {
            addNode(factory, root, nodeMap, typeMap, c.name(), ElementType.CONSTANT);
        }
        for (LookupTableDef t : def.lookupTables()) {
            addNode(factory, root, nodeMap, typeMap, t.name(), ElementType.LOOKUP);
        }
        for (ModuleInstanceDef m : def.modules()) {
            addNode(factory, root, nodeMap, typeMap, m.instanceName(), ElementType.MODULE);
        }

        // Material flow edges (high priority): stock ↔ flow connections
        for (FlowDef f : def.flows()) {
            ElkNode flowNode = nodeMap.get(f.name());
            if (f.source() != null && nodeMap.containsKey(f.source())) {
                ElkNode sourceStock = nodeMap.get(f.source());
                ElkEdge edge = createEdge(factory, root, sourceStock, flowNode);
                edge.setProperty(CoreOptions.PRIORITY, MATERIAL_FLOW_PRIORITY);
            }
            if (f.sink() != null && nodeMap.containsKey(f.sink())) {
                ElkNode sinkStock = nodeMap.get(f.sink());
                ElkEdge edge = createEdge(factory, root, flowNode, sinkStock);
                edge.setProperty(CoreOptions.PRIORITY, MATERIAL_FLOW_PRIORITY);
            }
        }

        // Info link edges (low priority): formula dependencies
        addInfoLinkEdges(factory, root, nodeMap, def);

        // Run ELK layout
        RecursiveGraphLayoutEngine engine = new RecursiveGraphLayoutEngine();
        engine.layout(root, new NullElkProgressMonitor());

        // Extract X from ELK (center coordinate), assign Y based on element type
        List<ElementPlacement> placements = new ArrayList<>();
        for (Map.Entry<String, ElkNode> entry : nodeMap.entrySet()) {
            String name = entry.getKey();
            ElkNode node = entry.getValue();
            ElementType type = typeMap.get(name);
            double cx = node.getX() + node.getWidth() / 2.0;
            double cy = yBandFor(type);
            placements.add(new ElementPlacement(name, type, cx, cy));
        }

        // Resolve overlaps within each Y band
        resolveOverlaps(placements);

        List<ConnectorRoute> connectors = ConnectorGenerator.generate(def);
        return new ViewDef("Auto Layout", placements, connectors, List.of());
    }

    private static double yBandFor(ElementType type) {
        return switch (type) {
            case AUX -> Y_AUX;
            case STOCK, FLOW -> Y_STOCK_FLOW;
            case CONSTANT -> Y_CONSTANT;
            case LOOKUP -> Y_LOOKUP;
            case MODULE -> Y_MODULE;
        };
    }

    /**
     * Resolves overlaps within each Y band by nudging elements rightward.
     * Elements are kept in their assigned band to avoid cross-band overlaps.
     */
    private static void resolveOverlaps(List<ElementPlacement> placements) {
        // Group indices by Y band
        Map<Double, List<Integer>> byY = new LinkedHashMap<>();
        for (int i = 0; i < placements.size(); i++) {
            byY.computeIfAbsent(placements.get(i).y(), k -> new ArrayList<>()).add(i);
        }

        double minGap = 20;
        for (List<Integer> indices : byY.values()) {
            if (indices.size() <= 1) {
                continue;
            }
            // Sort by X within the band
            indices.sort((a, b) -> Double.compare(placements.get(a).x(), placements.get(b).x()));

            for (int i = 1; i < indices.size(); i++) {
                ElementPlacement prev = placements.get(indices.get(i - 1));
                ElementPlacement curr = placements.get(indices.get(i));
                ElementSizes sp = ElementSizes.forType(prev.type());
                ElementSizes sc = ElementSizes.forType(curr.type());

                double prevRight = prev.x() + sp.width() / 2.0;
                double currLeft = curr.x() - sc.width() / 2.0;

                if (currLeft < prevRight + minGap) {
                    double newX = prevRight + minGap + sc.width() / 2.0;
                    placements.set(indices.get(i),
                            new ElementPlacement(curr.name(), curr.type(), newX, curr.y()));
                }
            }
        }
    }

    private static void addNode(ElkGraphFactory factory, ElkNode root,
                                Map<String, ElkNode> nodeMap,
                                Map<String, ElementType> typeMap,
                                String name, ElementType type) {
        ElkNode node = factory.createElkNode();
        node.setParent(root);
        node.setIdentifier(name);
        ElementSizes size = ElementSizes.forType(type);
        node.setWidth(size.width());
        node.setHeight(size.height());
        nodeMap.put(name, node);
        typeMap.put(name, type);
    }

    private static ElkEdge createEdge(ElkGraphFactory factory, ElkNode root,
                                      ElkNode source, ElkNode target) {
        ElkEdge edge = factory.createElkEdge();
        edge.setContainingNode(root);
        edge.getSources().add(source);
        edge.getTargets().add(target);
        return edge;
    }

    /**
     * Adds info link edges from the dependency graph, excluding stock-flow material connections.
     */
    private static void addInfoLinkEdges(ElkGraphFactory factory, ElkNode root,
                                         Map<String, ElkNode> nodeMap,
                                         ModelDefinition def) {
        DependencyGraph graph = DependencyGraph.fromDefinition(def);
        Set<String> materialEdges = buildMaterialEdgeKeys(def);

        for (String[] edge : graph.allEdges()) {
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
}
