package com.deathrayresearch.forrester.model.def;

import com.deathrayresearch.forrester.model.expr.Expr;
import com.deathrayresearch.forrester.model.expr.ExprDependencies;
import com.deathrayresearch.forrester.model.expr.ExprParser;
import com.deathrayresearch.forrester.model.expr.ParseException;
import com.deathrayresearch.forrester.model.graph.ViewValidator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates a {@link ModelDefinition} for structural correctness.
 * Returns a list of error messages; an empty list means the definition is valid.
 */
public final class DefinitionValidator {

    private DefinitionValidator() {
    }

    /**
     * Validates the given model definition and returns all errors found.
     */
    public static List<String> validate(ModelDefinition def) {
        List<String> errors = new ArrayList<>();
        Set<String> allNames = new HashSet<>();

        // Collect all element names and check for duplicates
        for (StockDef stock : def.stocks()) {
            if (!allNames.add(stock.name())) {
                errors.add("Duplicate element name: " + stock.name());
            }
        }
        for (FlowDef flow : def.flows()) {
            if (!allNames.add(flow.name())) {
                errors.add("Duplicate element name: " + flow.name());
            }
        }
        for (AuxDef aux : def.auxiliaries()) {
            if (!allNames.add(aux.name())) {
                errors.add("Duplicate element name: " + aux.name());
            }
        }
        for (ConstantDef constant : def.constants()) {
            if (!allNames.add(constant.name())) {
                errors.add("Duplicate element name: " + constant.name());
            }
        }
        for (LookupTableDef table : def.lookupTables()) {
            if (!allNames.add(table.name())) {
                errors.add("Duplicate element name: " + table.name());
            }
        }
        for (ModuleInstanceDef module : def.modules()) {
            if (!allNames.add(module.instanceName())) {
                errors.add("Duplicate element name: " + module.instanceName());
            }
        }

        // Build set of stock names for flow source/sink validation
        Set<String> stockNames = new HashSet<>();
        for (StockDef stock : def.stocks()) {
            stockNames.add(stock.name());
        }

        // Validate flow source/sink references
        for (FlowDef flow : def.flows()) {
            if (flow.source() != null && !stockNames.contains(flow.source())) {
                errors.add("Flow '" + flow.name() + "' references non-existent source stock: "
                        + flow.source());
            }
            if (flow.sink() != null && !stockNames.contains(flow.sink())) {
                errors.add("Flow '" + flow.name() + "' references non-existent sink stock: "
                        + flow.sink());
            }
        }

        // Validate flow equations parse
        for (FlowDef flow : def.flows()) {
            try {
                ExprParser.parse(flow.equation());
            } catch (ParseException e) {
                errors.add("Flow '" + flow.name() + "' has invalid equation: " + e.getMessage());
            }
        }

        // Validate auxiliary equations parse
        for (AuxDef aux : def.auxiliaries()) {
            try {
                ExprParser.parse(aux.equation());
            } catch (ParseException e) {
                errors.add("Auxiliary '" + aux.name() + "' has invalid equation: "
                        + e.getMessage());
            }
        }

        // Validate that formula references resolve to known element names
        Set<String> knownNames = new HashSet<>(allNames);
        // Add module output port aliases as known names
        for (ModuleInstanceDef module : def.modules()) {
            knownNames.addAll(module.outputBindings().values());
        }
        validateFormulaReferences(def, knownNames, errors);

        // Validate module interface bindings
        for (ModuleInstanceDef module : def.modules()) {
            ModuleInterface iface = module.definition().moduleInterface();
            if (iface != null) {
                Set<String> inputPortNames = new HashSet<>();
                for (PortDef port : iface.inputs()) {
                    inputPortNames.add(port.name());
                }
                for (String bindingPort : module.inputBindings().keySet()) {
                    if (!inputPortNames.contains(bindingPort)) {
                        errors.add("Module '" + module.instanceName()
                                + "' binds non-existent input port: " + bindingPort);
                    }
                }
                Set<String> outputPortNames = new HashSet<>();
                for (PortDef port : iface.outputs()) {
                    outputPortNames.add(port.name());
                }
                for (String bindingPort : module.outputBindings().keySet()) {
                    if (!outputPortNames.contains(bindingPort)) {
                        errors.add("Module '" + module.instanceName()
                                + "' binds non-existent output port: " + bindingPort);
                    }
                }
                // Check that all required input ports are bound
                for (String inputPort : inputPortNames) {
                    if (!module.inputBindings().containsKey(inputPort)) {
                        errors.add("Module '" + module.instanceName()
                                + "' is missing binding for required input port: " + inputPort);
                    }
                }
            }
        }

        // Check for circular module references
        Set<String> rootPath = new HashSet<>();
        rootPath.add(def.name());
        for (ModuleInstanceDef module : def.modules()) {
            if (hasCircularReference(module.definition(), new HashSet<>(rootPath))) {
                errors.add("Circular module reference detected involving module: "
                        + module.instanceName());
            }
        }

        // Validate views if present
        for (ViewDef view : def.views()) {
            errors.addAll(ViewValidator.validate(view, def));
        }

        return errors;
    }

    private static final Set<String> BUILTIN_NAMES = Set.of(
            "TIME", "DT", "Pi", "PI", "E");

    private static void validateFormulaReferences(ModelDefinition def,
                                                   Set<String> knownNames,
                                                   List<String> errors) {
        for (FlowDef flow : def.flows()) {
            try {
                Expr expr = ExprParser.parse(flow.equation());
                Set<String> refs = ExprDependencies.extract(expr);
                for (String ref : refs) {
                    String resolved = ref.replace('_', ' ');
                    if (!knownNames.contains(ref) && !knownNames.contains(resolved)
                            && !BUILTIN_NAMES.contains(ref)) {
                        errors.add("Flow '" + flow.name()
                                + "' references unknown element: " + ref);
                    }
                }
            } catch (ParseException ignored) {
                // Already reported as a parse error above
            }
        }
        for (AuxDef aux : def.auxiliaries()) {
            try {
                Expr expr = ExprParser.parse(aux.equation());
                Set<String> refs = ExprDependencies.extract(expr);
                for (String ref : refs) {
                    String resolved = ref.replace('_', ' ');
                    if (!knownNames.contains(ref) && !knownNames.contains(resolved)
                            && !BUILTIN_NAMES.contains(ref)) {
                        errors.add("Auxiliary '" + aux.name()
                                + "' references unknown element: " + ref);
                    }
                }
            } catch (ParseException ignored) {
                // Already reported as a parse error above
            }
        }
    }

    private static boolean hasCircularReference(ModelDefinition def, Set<String> pathNames) {
        if (!pathNames.add(def.name())) {
            return true; // this definition name already appears on the current path
        }
        for (ModuleInstanceDef module : def.modules()) {
            if (hasCircularReference(module.definition(), new HashSet<>(pathNames))) {
                return true;
            }
        }
        return false;
    }
}
