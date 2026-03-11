package systems.courant.shrewd.model.def;

import systems.courant.shrewd.model.expr.Expr;
import systems.courant.shrewd.model.expr.ExprDependencies;
import systems.courant.shrewd.model.expr.ExprParser;
import systems.courant.shrewd.model.expr.ParseException;
import systems.courant.shrewd.model.graph.ViewValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Validates a {@link ModelDefinition} for structural correctness.
 * Returns a list of error messages; an empty list means the definition is valid.
 */
public final class DefinitionValidator {

    private static final Logger log = LoggerFactory.getLogger(DefinitionValidator.class);

    private DefinitionValidator() {
    }

    /**
     * Validates the given model definition and returns all errors found.
     */
    public static List<String> validate(ModelDefinition def) {
        List<String> errors = new ArrayList<>();
        Set<String> allNames = new HashSet<>();
        // Case-insensitive duplicate detection: maps lowercase → first-seen original name
        Map<String, String> lowerToOriginal = new HashMap<>();

        // Collect all element names and check for duplicates (case-insensitive)
        for (StockDef stock : def.stocks()) {
            checkDuplicateName(stock.name(), allNames, lowerToOriginal, errors);
        }
        for (FlowDef flow : def.flows()) {
            checkDuplicateName(flow.name(), allNames, lowerToOriginal, errors);
        }
        for (AuxDef aux : def.auxiliaries()) {
            checkDuplicateName(aux.name(), allNames, lowerToOriginal, errors);
        }
        for (LookupTableDef table : def.lookupTables()) {
            checkDuplicateName(table.name(), allNames, lowerToOriginal, errors);
        }
        for (ModuleInstanceDef module : def.modules()) {
            checkDuplicateName(module.instanceName(), allNames, lowerToOriginal, errors);
        }
        for (CldVariableDef v : def.cldVariables()) {
            checkDuplicateName(v.name(), allNames, lowerToOriginal, errors);
        }

        // Build set of stock names for flow source/sink validation
        Set<String> stockNames = new HashSet<>();
        for (StockDef stock : def.stocks()) {
            stockNames.add(stock.name());
        }

        // Validate flow source/sink references
        for (FlowDef flow : def.flows()) {
            if (flow.source() != null && !stockNames.contains(flow.source())) {
                errors.add("Flow '" + flow.name() + "' references non-existent source stock: '"
                        + flow.source() + "'. The stock may have been renamed or deleted.");
            }
            if (flow.sink() != null && !stockNames.contains(flow.sink())) {
                errors.add("Flow '" + flow.name() + "' references non-existent sink stock: '"
                        + flow.sink() + "'. The stock may have been renamed or deleted.");
            }
        }

        // Validate flow equations parse
        for (FlowDef flow : def.flows()) {
            try {
                ExprParser.parse(flow.equation());
            } catch (ParseException e) {
                errors.add("Flow '" + flow.name() + "' has invalid equation: " + e.getMessage()
                        + ". Double-click the flow to edit its equation.");
            }
        }

        // Validate auxiliary equations parse
        for (AuxDef aux : def.auxiliaries()) {
            try {
                ExprParser.parse(aux.equation());
            } catch (ParseException e) {
                errors.add("Variable '" + aux.name() + "' has invalid equation: "
                        + e.getMessage()
                        + ". Double-click the variable to edit its equation.");
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
                                + "' binds non-existent input port: '" + bindingPort
                                + "'. Check the module definition for valid port names.");
                    }
                }
                Set<String> outputPortNames = new HashSet<>();
                for (PortDef port : iface.outputs()) {
                    outputPortNames.add(port.name());
                }
                for (String bindingPort : module.outputBindings().keySet()) {
                    if (!outputPortNames.contains(bindingPort)) {
                        errors.add("Module '" + module.instanceName()
                                + "' binds non-existent output port: '" + bindingPort
                                + "'. Check the module definition for valid port names.");
                    }
                }
                // Check that all required input ports are bound
                for (String inputPort : inputPortNames) {
                    if (!module.inputBindings().containsKey(inputPort)) {
                        errors.add("Module '" + module.instanceName()
                                + "' is missing binding for required input port: '" + inputPort
                                + "'. Bind it to an element in the parent model.");
                    }
                }
            }
        }

        // Check for circular module references
        Set<String> rootPath = new HashSet<>();
        rootPath.add(def.name());
        for (ModuleInstanceDef module : def.modules()) {
            if (hasCircularReference(module.definition(), new HashSet<>(rootPath))) {
                errors.add("Circular module reference detected involving module: '"
                        + module.instanceName()
                        + "'. A module cannot contain itself, directly or indirectly.");
            }
        }

        // Validate causal links for self-loops and duplicates
        Set<String> seenLinks = new HashSet<>();
        for (CausalLinkDef link : def.causalLinks()) {
            if (link.from().equals(link.to())) {
                errors.add("Causal link from '" + link.from()
                        + "' to itself is not allowed. Remove the self-loop.");
            }
            String key = link.from() + " -> " + link.to();
            if (!seenLinks.add(key)) {
                errors.add("Duplicate causal link: " + key
                        + ". Remove the extra link.");
            }
        }

        // Validate views if present
        for (ViewDef view : def.views()) {
            errors.addAll(ViewValidator.validate(view, def));
        }

        return errors;
    }

    /**
     * Validates a model definition for structural correctness, excluding view validation.
     * Use this for compilation where views are non-functional and may reference
     * import-generated names that differ from model element names.
     */
    public static List<String> validateStructure(ModelDefinition def) {
        List<String> all = validate(def);
        all.removeIf(e -> e.startsWith("View "));
        return all;
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
                                + "' references unknown element: '" + ref
                                + "'. Check the spelling or add the missing element to the model.");
                    }
                }
            } catch (ParseException ex) {
                log.debug("Already reported parse error in flow '{}': {}", flow.name(), ex.getMessage(), ex);
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
                        errors.add("Variable '" + aux.name()
                                + "' references unknown element: '" + ref
                                + "'. Check the spelling or add the missing element to the model.");
                    }
                }
            } catch (ParseException ex) {
                log.debug("Already reported parse error in auxiliary '{}': {}", aux.name(), ex.getMessage(), ex);
            }
        }
    }

    private static void checkDuplicateName(String name, Set<String> allNames,
                                              Map<String, String> lowerToOriginal,
                                              List<String> errors) {
        allNames.add(name);
        String lower = name.toLowerCase(Locale.ROOT);
        String existing = lowerToOriginal.putIfAbsent(lower, name);
        if (existing != null) {
            if (existing.equals(name)) {
                errors.add("Duplicate element name: '" + name
                        + "'. Rename one of the elements to make names unique.");
            } else {
                errors.add("Duplicate element name (case-insensitive): '" + name
                        + "' conflicts with '" + existing
                        + "'. Rename one of the elements to make names unique.");
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
