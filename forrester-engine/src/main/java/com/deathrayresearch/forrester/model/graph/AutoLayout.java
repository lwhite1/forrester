package com.deathrayresearch.forrester.model.graph;

import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.ElementPlacement;
import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModuleInstanceDef;
import com.deathrayresearch.forrester.model.def.StockDef;
import com.deathrayresearch.forrester.model.def.ViewDef;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates a simple auto-layout for a model definition.
 * Stocks and flows are arranged in chains following source/sink relationships:
 * inflows appear to the left of their sink stock, outflows to the right of
 * their source stock, and transfer flows between the two stocks they connect.
 * Auxiliaries are placed above, constants below.
 */
public final class AutoLayout {

    private AutoLayout() {
    }

    private static final double X_START = 100;
    private static final double X_SPACING = 150;
    private static final double Y_STOCK = 200;
    private static final double Y_FLOW = 200;
    private static final double Y_AUX = 50;
    private static final double Y_CONSTANT = 350;
    private static final double Y_LOOKUP = 400;
    private static final double Y_MODULE = 450;

    /**
     * Generates a {@link ViewDef} with all elements placed in a simple layered layout.
     */
    public static ViewDef layout(ModelDefinition def) {
        List<ElementPlacement> placements = new ArrayList<>();

        // Classify flows by their relationship to stocks
        Map<String, List<FlowDef>> boundaryInflows = new LinkedHashMap<>();
        Map<String, List<FlowDef>> boundaryOutflows = new LinkedHashMap<>();
        Map<String, List<FlowDef>> transferOutflows = new LinkedHashMap<>();
        List<FlowDef> orphanFlows = new ArrayList<>();

        for (FlowDef f : def.flows()) {
            if (f.source() != null && f.sink() != null) {
                transferOutflows.computeIfAbsent(f.source(), k -> new ArrayList<>()).add(f);
            } else if (f.source() != null) {
                boundaryOutflows.computeIfAbsent(f.source(), k -> new ArrayList<>()).add(f);
            } else if (f.sink() != null) {
                boundaryInflows.computeIfAbsent(f.sink(), k -> new ArrayList<>()).add(f);
            } else {
                orphanFlows.add(f);
            }
        }

        // Order stocks: follow transfer flow chains so connected stocks are adjacent
        List<String> stockOrder = orderStocks(def.stocks(), transferOutflows);

        // Place stocks and their associated flows in chain order
        double x = X_START;
        Set<String> placedFlows = new HashSet<>();

        for (String stockName : stockOrder) {
            // Boundary inflows to the LEFT of the stock
            for (FlowDef f : boundaryInflows.getOrDefault(stockName, List.of())) {
                if (placedFlows.add(f.name())) {
                    placements.add(new ElementPlacement(f.name(), ElementType.FLOW, x, Y_FLOW));
                    x += X_SPACING;
                }
            }

            // The stock itself
            placements.add(new ElementPlacement(stockName, ElementType.STOCK, x, Y_STOCK));
            x += X_SPACING;

            // Boundary outflows to the RIGHT of the stock
            for (FlowDef f : boundaryOutflows.getOrDefault(stockName, List.of())) {
                if (placedFlows.add(f.name())) {
                    placements.add(new ElementPlacement(f.name(), ElementType.FLOW, x, Y_FLOW));
                    x += X_SPACING;
                }
            }

            // Transfer outflows between this stock and the next
            for (FlowDef f : transferOutflows.getOrDefault(stockName, List.of())) {
                if (placedFlows.add(f.name())) {
                    placements.add(new ElementPlacement(f.name(), ElementType.FLOW, x, Y_FLOW));
                    x += X_SPACING;
                }
            }
        }

        // Orphan flows (no source or sink)
        for (FlowDef f : orphanFlows) {
            placements.add(new ElementPlacement(f.name(), ElementType.FLOW, x, Y_FLOW));
            x += X_SPACING;
        }

        // Place auxiliaries above
        x = X_START;
        for (AuxDef a : def.auxiliaries()) {
            placements.add(new ElementPlacement(a.name(), ElementType.AUX, x, Y_AUX));
            x += X_SPACING;
        }

        // Place constants below their associated flows/elements
        placeConstantsBelowDependents(def, placements);

        // Place lookup tables at bottom
        x = X_START;
        for (LookupTableDef t : def.lookupTables()) {
            placements.add(new ElementPlacement(t.name(), ElementType.LOOKUP, x, Y_LOOKUP));
            x += X_SPACING;
        }

        // Place module instances
        x = X_START;
        for (ModuleInstanceDef m : def.modules()) {
            placements.add(new ElementPlacement(m.instanceName(), ElementType.MODULE, x, Y_MODULE));
            x += X_SPACING;
        }

        // Generate connectors from dependency graph
        List<ConnectorRoute> connectors = ConnectorGenerator.generate(def);

        return new ViewDef("Auto Layout", placements, connectors, List.of());
    }

    /**
     * Orders stocks by following transfer flow chains so that connected stocks
     * are adjacent. Roots (stocks not the sink of any transfer) come first.
     */
    private static List<String> orderStocks(List<StockDef> stocks,
                                            Map<String, List<FlowDef>> transferOutflows) {
        // Find which stocks are sinks of transfer flows
        Set<String> hasPredecessor = new HashSet<>();
        for (List<FlowDef> flows : transferOutflows.values()) {
            for (FlowDef f : flows) {
                hasPredecessor.add(f.sink());
            }
        }

        List<String> order = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        // Start with roots (stocks not receiving any transfer flow)
        for (StockDef s : stocks) {
            if (!hasPredecessor.contains(s.name()) && visited.add(s.name())) {
                walkChain(s.name(), transferOutflows, visited, order);
            }
        }

        // Add remaining stocks (cycles or isolated)
        for (StockDef s : stocks) {
            if (visited.add(s.name())) {
                walkChain(s.name(), transferOutflows, visited, order);
            }
        }

        return order;
    }

    private static void walkChain(String stock, Map<String, List<FlowDef>> transferOutflows,
                                  Set<String> visited, List<String> order) {
        order.add(stock);
        for (FlowDef f : transferOutflows.getOrDefault(stock, List.of())) {
            if (visited.add(f.sink())) {
                walkChain(f.sink(), transferOutflows, visited, order);
            }
        }
    }

    /**
     * Places each constant below the flow or element it feeds into,
     * using the dependency graph to determine associations.
     * Falls back to sequential placement for constants with no placed dependents.
     */
    private static void placeConstantsBelowDependents(ModelDefinition def,
                                                       List<ElementPlacement> placements) {
        DependencyGraph graph = DependencyGraph.fromDefinition(def);

        // Build a lookup of already-placed element positions
        Map<String, Double> placedX = new LinkedHashMap<>();
        for (ElementPlacement p : placements) {
            placedX.put(p.name(), p.x());
        }

        // Track which x positions are already taken by constants to avoid overlap
        Set<Long> usedConstantX = new HashSet<>();
        double fallbackX = X_START;

        for (ConstantDef c : def.constants()) {
            Double targetX = null;

            // Find the first placed dependent (flow, aux, or stock this constant feeds)
            for (String dep : graph.dependentsOf(c.name())) {
                if (placedX.containsKey(dep)) {
                    targetX = placedX.get(dep);
                    break;
                }
            }

            double x;
            if (targetX != null) {
                x = targetX;
            } else {
                x = fallbackX;
            }

            // Nudge right if another constant already occupies this x
            while (!usedConstantX.add(Math.round(x))) {
                x += X_SPACING;
            }

            placements.add(new ElementPlacement(c.name(), ElementType.CONSTANT, x, Y_CONSTANT));
            fallbackX += X_SPACING;
        }
    }
}
