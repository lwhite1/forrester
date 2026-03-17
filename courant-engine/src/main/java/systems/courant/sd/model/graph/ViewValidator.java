package systems.courant.sd.model.graph;

import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.def.CldVariableDef;
import systems.courant.sd.model.def.CommentDef;
import systems.courant.sd.model.def.ConnectorRoute;
import systems.courant.sd.model.def.ElementPlacement;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.FlowRoute;
import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModuleInstanceDef;
import systems.courant.sd.model.def.StockDef;
import systems.courant.sd.model.def.TimeSeriesDef;
import systems.courant.sd.model.def.ViewDef;

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
     * Checks that all element placements, connector endpoints, and flow route references
     * correspond to elements that exist in the model.
     *
     * @param view the view definition to validate
     * @param def  the model definition to validate against
     * @return a list of error messages; an empty list means the view is valid
     */
    public static List<String> validate(ViewDef view, ModelDefinition def) {
        List<String> errors = new ArrayList<>();
        Set<String> elementNames = collectElementNames(def);

        // Validate element placements
        for (ElementPlacement placement : view.elements()) {
            if (!matchesName(elementNames, placement.name())) {
                errors.add("View '" + view.name() + "' places non-existent element: "
                        + placement.name());
            }
        }

        // Validate connector endpoints
        for (ConnectorRoute connector : view.connectors()) {
            if (!matchesName(elementNames, connector.from())) {
                errors.add("View '" + view.name() + "' connector references non-existent element: "
                        + connector.from());
            }
            if (!matchesName(elementNames, connector.to())) {
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
            if (!matchesName(flowNames, flowRoute.flowName())) {
                errors.add("View '" + view.name() + "' flow route references non-existent flow: "
                        + flowRoute.flowName());
            }
        }

        return errors;
    }

    private static boolean matchesName(Set<String> names, String name) {
        if (names.contains(name)) {
            return true;
        }
        if (name.contains("_")) {
            return names.contains(name.replace('_', ' '));
        }
        if (name.contains(" ")) {
            return names.contains(name.replace(' ', '_'));
        }
        return false;
    }

    private static Set<String> collectElementNames(ModelDefinition def) {
        Set<String> names = new HashSet<>();
        for (StockDef s : def.stocks()) {
            names.add(s.name());
        }
        for (FlowDef f : def.flows()) {
            names.add(f.name());
        }
        for (VariableDef a : def.variables()) {
            names.add(a.name());
        }
        for (LookupTableDef t : def.lookupTables()) {
            names.add(t.name());
        }
        for (ModuleInstanceDef m : def.modules()) {
            names.add(m.instanceName());
        }
        for (CldVariableDef v : def.cldVariables()) {
            names.add(v.name());
        }
        for (CommentDef c : def.comments()) {
            names.add(c.name());
        }
        for (TimeSeriesDef ts : def.timeSeries()) {
            names.add(ts.name());
        }
        return names;
    }
}
