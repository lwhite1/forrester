package com.deathrayresearch.forrester.model.graph;

import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.ElementPlacement;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.FlowRoute;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModuleInstanceDef;
import com.deathrayresearch.forrester.model.def.StockDef;
import com.deathrayresearch.forrester.model.def.ViewDef;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates a {@link ViewDef} against a {@link ModelDefinition}.
 * Checks that all element placements and connector endpoints reference existing model elements.
 */
public final class ViewValidator {

    private ViewValidator() {
    }

    /**
     * Validates the given view definition against the model definition.
     * Returns a list of error messages; an empty list means the view is valid.
     */
    public static List<String> validate(ViewDef view, ModelDefinition def) {
        List<String> errors = new ArrayList<>();
        Set<String> elementNames = collectElementNames(def);

        // Validate element placements
        for (ElementPlacement placement : view.elements()) {
            if (!elementNames.contains(placement.name())) {
                errors.add("View '" + view.name() + "' places non-existent element: "
                        + placement.name());
            }
        }

        // Validate connector endpoints
        for (ConnectorRoute connector : view.connectors()) {
            if (!elementNames.contains(connector.from())) {
                errors.add("View '" + view.name() + "' connector references non-existent element: "
                        + connector.from());
            }
            if (!elementNames.contains(connector.to())) {
                errors.add("View '" + view.name() + "' connector references non-existent element: "
                        + connector.to());
            }
        }

        // Validate flow route references
        Set<String> flowNames = new HashSet<>();
        for (FlowDef f : def.flows()) {
            flowNames.add(f.name());
        }
        for (FlowRoute flowRoute : view.flowRoutes()) {
            if (!flowNames.contains(flowRoute.flowName())) {
                errors.add("View '" + view.name() + "' flow route references non-existent flow: "
                        + flowRoute.flowName());
            }
        }

        return errors;
    }

    private static Set<String> collectElementNames(ModelDefinition def) {
        Set<String> names = new HashSet<>();
        for (StockDef s : def.stocks()) {
            names.add(s.name());
        }
        for (FlowDef f : def.flows()) {
            names.add(f.name());
        }
        for (AuxDef a : def.auxiliaries()) {
            names.add(a.name());
        }
        for (ConstantDef c : def.constants()) {
            names.add(c.name());
        }
        for (LookupTableDef t : def.lookupTables()) {
            names.add(t.name());
        }
        for (ModuleInstanceDef m : def.modules()) {
            names.add(m.instanceName());
        }
        return names;
    }
}
