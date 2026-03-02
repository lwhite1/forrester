package com.deathrayresearch.forrester.model.graph;

import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.ElementPlacement;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModuleInstanceDef;
import com.deathrayresearch.forrester.model.def.StockDef;
import com.deathrayresearch.forrester.model.def.ViewDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates a simple auto-layout for a model definition.
 * Stocks are placed in the center row, flows between their source/sink,
 * auxiliaries above, and constants below.
 */
public final class AutoLayout {

    private AutoLayout() {
    }

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
        double x = 100;

        // Place stocks in center row
        for (StockDef s : def.stocks()) {
            placements.add(new ElementPlacement(s.name(), "stock", x, Y_STOCK));
            x += X_SPACING;
        }

        // Place flows at the same y as stocks, offset between them
        x = 100 + X_SPACING / 2;
        for (FlowDef f : def.flows()) {
            placements.add(new ElementPlacement(f.name(), "flow", x, Y_FLOW));
            x += X_SPACING;
        }

        // Place auxiliaries above
        x = 100;
        for (AuxDef a : def.auxiliaries()) {
            placements.add(new ElementPlacement(a.name(), "aux", x, Y_AUX));
            x += X_SPACING;
        }

        // Place constants below
        x = 100;
        for (ConstantDef c : def.constants()) {
            placements.add(new ElementPlacement(c.name(), "constant", x, Y_CONSTANT));
            x += X_SPACING;
        }

        // Place lookup tables at bottom
        x = 100;
        for (LookupTableDef t : def.lookupTables()) {
            placements.add(new ElementPlacement(t.name(), "lookup", x, Y_LOOKUP));
            x += X_SPACING;
        }

        // Place module instances
        x = 100;
        for (ModuleInstanceDef m : def.modules()) {
            placements.add(new ElementPlacement(m.instanceName(), "module", x, Y_MODULE));
            x += X_SPACING;
        }

        // Generate connectors from dependency graph
        List<ConnectorRoute> connectors = ConnectorGenerator.generate(def);

        return new ViewDef("Auto Layout", placements, connectors, List.of());
    }
}
