package systems.courant.shrewd.model.def;

import systems.courant.shrewd.model.def.ValidationIssue.Severity;
import systems.courant.shrewd.model.expr.Expr;
import systems.courant.shrewd.model.expr.ExprDependencies;
import systems.courant.shrewd.model.expr.ExprParser;
import systems.courant.shrewd.model.expr.ParseException;
import systems.courant.shrewd.model.graph.DependencyGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates a {@link ModelDefinition} for completeness and structural correctness.
 * Returns a {@link ValidationResult} containing errors and warnings.
 *
 * <p>Checks performed:
 * <ol>
 *   <li>Delegates to {@link DefinitionValidator} — wraps each error as an ERROR issue</li>
 *   <li>Disconnected flows — flows where both source and sink are null</li>
 *   <li>Missing units — stocks and auxiliaries with null/blank unit</li>
 *   <li>Algebraic loops — cycle groups containing no stocks</li>
 *   <li>Unused elements — parameters and lookup tables not referenced by any equation</li>
 *   <li>Isolated stocks — stocks with no inflows or outflows</li>
 *   <li>Dangling connectors — connector arrows whose source is not referenced
 *       in the target element's equation</li>
 *   <li>Unconnected modules — modules with no ports and no bindings</li>
 * </ol>
 */
public final class ModelValidator {

    private static final Logger log = LoggerFactory.getLogger(ModelValidator.class);

    private static final Pattern ELEMENT_NAME_PATTERN = Pattern.compile("'([^']+)'");

    private ModelValidator() {
    }

    /**
     * Validates the given model definition and returns all issues found.
     */
    public static ValidationResult validate(ModelDefinition def) {
        List<ValidationIssue> issues = new ArrayList<>();

        // 1. Delegate to DefinitionValidator and wrap errors
        List<String> defErrors = DefinitionValidator.validate(def);
        for (String error : defErrors) {
            String elementName = extractElementName(error);
            issues.add(new ValidationIssue(Severity.ERROR, elementName, error));
        }

        // 2. Disconnected flows
        checkDisconnectedFlows(def, issues);

        // 3. Missing units
        checkMissingUnits(def, issues);

        // 4. Algebraic loops (only if equations parse — avoid crashes on invalid models)
        checkAlgebraicLoops(def, issues);

        // 5. Unused elements
        checkUnusedElements(def, issues);

        // 6. Isolated stocks
        checkIsolatedStocks(def, issues);

        // 7. Dangling connectors
        checkDanglingConnectors(def, issues);

        // 8. Unconnected modules
        checkUnconnectedModules(def, issues);

        // 9. CLD checks
        checkOrphanedCldVariables(def, issues);
        checkCausalLinkEndpoints(def, issues);
        checkUnknownPolarity(def, issues);

        return new ValidationResult(issues);
    }

    /**
     * Extracts an element name from error messages that use the 'name' quoting pattern.
     */
    private static String extractElementName(String error) {
        Matcher matcher = ELEMENT_NAME_PATTERN.matcher(error);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static void checkDisconnectedFlows(ModelDefinition def, List<ValidationIssue> issues) {
        for (FlowDef flow : def.flows()) {
            if (flow.source() == null && flow.sink() == null) {
                issues.add(new ValidationIssue(Severity.WARNING, flow.name(),
                        "Flow '" + flow.name()
                                + "' is disconnected (no source or sink stock)."
                                + " Connect it to a stock by dragging from a stock to this flow,"
                                + " or from this flow to a stock."));
            }
        }
    }

    private static void checkMissingUnits(ModelDefinition def, List<ValidationIssue> issues) {
        for (StockDef stock : def.stocks()) {
            if (stock.unit() == null || stock.unit().isBlank()) {
                issues.add(new ValidationIssue(Severity.WARNING, stock.name(),
                        "Stock '" + stock.name() + "' has no unit specified."
                                + " Select the stock and set its unit in the Properties panel."));
            }
        }
        for (AuxDef aux : def.auxiliaries()) {
            if (aux.unit() == null || aux.unit().isBlank()) {
                issues.add(new ValidationIssue(Severity.WARNING, aux.name(),
                        "Variable '" + aux.name() + "' has no unit specified."
                                + " Select it and set the unit in the Properties panel."));
            }
        }
    }

    private static void checkAlgebraicLoops(ModelDefinition def, List<ValidationIssue> issues) {
        DependencyGraph graph = DependencyGraph.fromDefinition(def);
        List<String> sorted = graph.topologicalSort();

        if (sorted.size() == graph.allNodes().size()) {
            return; // No cycles
        }

        Set<String> sortedSet = new HashSet<>(sorted);
        Set<String> cycleNodes = new LinkedHashSet<>();
        for (String node : graph.allNodes()) {
            if (!sortedSet.contains(node)) {
                cycleNodes.add(node);
            }
        }

        Set<String> stockNames = new HashSet<>();
        for (StockDef stock : def.stocks()) {
            stockNames.add(stock.name());
        }

        // Group cycle participants into connected components
        Map<String, Set<String>> adjacency = graph.adjacencyMap();
        Map<String, Set<String>> undirected = new LinkedHashMap<>();
        for (String node : cycleNodes) {
            undirected.put(node, new HashSet<>());
        }
        for (String from : cycleNodes) {
            Set<String> targets = adjacency.get(from);
            if (targets == null) {
                continue;
            }
            for (String to : targets) {
                if (cycleNodes.contains(to)) {
                    undirected.get(from).add(to);
                    undirected.get(to).add(from);
                }
            }
        }

        Set<String> visited = new HashSet<>();
        for (String start : cycleNodes) {
            if (visited.contains(start)) {
                continue;
            }
            Set<String> component = new LinkedHashSet<>();
            Deque<String> queue = new ArrayDeque<>();
            queue.add(start);
            while (!queue.isEmpty()) {
                String current = queue.poll();
                if (!visited.add(current)) {
                    continue;
                }
                component.add(current);
                for (String neighbor : undirected.getOrDefault(current, Set.of())) {
                    if (!visited.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }

            boolean hasStock = false;
            for (String member : component) {
                if (stockNames.contains(member)) {
                    hasStock = true;
                    break;
                }
            }

            if (!hasStock) {
                String members = String.join(", ", component);
                for (String member : component) {
                    issues.add(new ValidationIssue(Severity.WARNING, member,
                            "Algebraic loop detected: {" + members
                                    + "} — circular dependency without a stock to break the loop."
                                    + " Add a stock to break the cycle,"
                                    + " or restructure the equations to remove the circular reference."));
                }
            }
        }
    }

    private static void checkUnusedElements(ModelDefinition def, List<ValidationIssue> issues) {
        // Collect all references from flow and auxiliary equations
        Set<String> referencedNames = new HashSet<>();

        for (FlowDef flow : def.flows()) {
            collectReferences(flow.equation(), referencedNames);
            // Flows also reference their source/sink stocks
            if (flow.source() != null) {
                referencedNames.add(flow.source());
            }
            if (flow.sink() != null) {
                referencedNames.add(flow.sink());
            }
        }
        for (AuxDef aux : def.auxiliaries()) {
            collectReferences(aux.equation(), referencedNames);
        }

        // Check literal-valued auxiliaries (parameters)
        for (AuxDef aux : def.auxiliaries()) {
            if (aux.isLiteral() && !isReferenced(aux.name(), referencedNames)) {
                issues.add(new ValidationIssue(Severity.WARNING, aux.name(),
                        "Parameter '" + aux.name()
                                + "' is not referenced by any equation."
                                + " Use it in a flow or auxiliary equation, or remove it if unneeded."));
            }
        }
        // Check lookup tables
        for (LookupTableDef table : def.lookupTables()) {
            if (!isReferenced(table.name(), referencedNames)) {
                issues.add(new ValidationIssue(Severity.WARNING, table.name(),
                        "Lookup table '" + table.name()
                                + "' is not referenced by any equation."
                                + " Reference it in an equation using its name,"
                                + " or remove it if unneeded."));
            }
        }
    }

    private static void checkIsolatedStocks(ModelDefinition def, List<ValidationIssue> issues) {
        for (StockDef stock : def.stocks()) {
            boolean hasFlow = false;
            for (FlowDef flow : def.flows()) {
                if (stock.name().equals(flow.source()) || stock.name().equals(flow.sink())) {
                    hasFlow = true;
                    break;
                }
            }
            if (!hasFlow) {
                issues.add(new ValidationIssue(Severity.WARNING, stock.name(),
                        "Stock '" + stock.name()
                                + "' has no inflows or outflows."
                                + " Connect a flow to this stock,"
                                + " or remove it if unneeded."));
            }
        }
    }

    private static void checkDanglingConnectors(ModelDefinition def, List<ValidationIssue> issues) {
        // Build a map of element name → equation for flows and auxiliaries
        Map<String, String> equations = new LinkedHashMap<>();
        for (FlowDef flow : def.flows()) {
            equations.put(flow.name(), flow.equation());
        }
        for (AuxDef aux : def.auxiliaries()) {
            equations.put(aux.name(), aux.equation());
        }

        for (ViewDef view : def.views()) {
            for (ConnectorRoute connector : view.connectors()) {
                String targetEquation = equations.get(connector.to());
                if (targetEquation == null) {
                    continue; // target is a stock or non-equation element — skip
                }
                Set<String> refs = new HashSet<>();
                collectReferences(targetEquation, refs);
                if (!isReferenced(connector.from(), refs)) {
                    issues.add(new ValidationIssue(Severity.WARNING, connector.to(),
                            "Connector from '" + connector.from() + "' to '"
                                    + connector.to()
                                    + "' does not match the equation — '"
                                    + connector.to()
                                    + "' does not reference '" + connector.from()
                                    + "'. Remove the connector or update the equation."));
                }
            }
        }
    }

    private static void checkUnconnectedModules(ModelDefinition def,
            List<ValidationIssue> issues) {
        for (ModuleInstanceDef module : def.modules()) {
            ModuleInterface iface = module.definition().moduleInterface();
            boolean hasNoPorts = iface == null
                    || (iface.inputs().isEmpty() && iface.outputs().isEmpty());
            boolean hasNoBindings = module.inputBindings().isEmpty()
                    && module.outputBindings().isEmpty();
            if (hasNoPorts && hasNoBindings) {
                issues.add(new ValidationIssue(Severity.WARNING, module.instanceName(),
                        "Module '" + module.instanceName()
                                + "' has no ports defined and no bindings."
                                + " Right-click the module and use 'Define Ports...'"
                                + " to declare its inputs and outputs."));
            }
        }
    }

    private static void checkOrphanedCldVariables(ModelDefinition def,
            List<ValidationIssue> issues) {
        Set<String> linkedNames = new HashSet<>();
        for (CausalLinkDef link : def.causalLinks()) {
            linkedNames.add(link.from());
            linkedNames.add(link.to());
        }
        for (CldVariableDef v : def.cldVariables()) {
            if (!linkedNames.contains(v.name())) {
                issues.add(new ValidationIssue(Severity.WARNING, v.name(),
                        "CLD variable '" + v.name()
                                + "' is not connected by any causal link."
                                + " Draw a causal link to or from it,"
                                + " or remove it if unneeded."));
            }
        }
    }

    private static void checkCausalLinkEndpoints(ModelDefinition def,
            List<ValidationIssue> issues) {
        Set<String> allNames = new HashSet<>();
        for (StockDef s : def.stocks()) {
            allNames.add(s.name());
        }
        for (FlowDef f : def.flows()) {
            allNames.add(f.name());
        }
        for (AuxDef a : def.auxiliaries()) {
            allNames.add(a.name());
        }
        for (CldVariableDef v : def.cldVariables()) {
            allNames.add(v.name());
        }
        for (LookupTableDef t : def.lookupTables()) {
            allNames.add(t.name());
        }

        for (CausalLinkDef link : def.causalLinks()) {
            if (!allNames.contains(link.from())) {
                issues.add(new ValidationIssue(Severity.ERROR, null,
                        "Causal link references non-existent source '" + link.from()
                                + "'. The element may have been renamed or deleted."));
            }
            if (!allNames.contains(link.to())) {
                issues.add(new ValidationIssue(Severity.ERROR, null,
                        "Causal link references non-existent target '" + link.to()
                                + "'. The element may have been renamed or deleted."));
            }
        }
    }

    private static void checkUnknownPolarity(ModelDefinition def,
            List<ValidationIssue> issues) {
        for (CausalLinkDef link : def.causalLinks()) {
            if (link.polarity() == CausalLinkDef.Polarity.UNKNOWN) {
                issues.add(new ValidationIssue(Severity.WARNING, link.from(),
                        "Causal link from '" + link.from() + "' to '" + link.to()
                                + "' has unknown polarity."
                                + " Set it to '+' (reinforcing) or '−' (balancing)"
                                + " by selecting the link and editing its polarity."));
            }
        }
    }

    private static void collectReferences(String equation, Set<String> refs) {
        try {
            Expr expr = ExprParser.parse(equation);
            refs.addAll(ExprDependencies.extract(expr));
        } catch (ParseException ex) {
            log.debug("Invalid equation already reported by DefinitionValidator: '{}'", equation, ex);
        }
    }

    /**
     * Checks if a name is referenced, accounting for the underscore/space name resolution.
     */
    private static boolean isReferenced(String name, Set<String> referencedNames) {
        if (referencedNames.contains(name)) {
            return true;
        }
        // Check underscore variant (equations use underscores for names with spaces)
        String underscored = name.replace(' ', '_');
        if (referencedNames.contains(underscored)) {
            return true;
        }
        // Check space variant (element named with underscores, equation uses spaces)
        String spaced = name.replace('_', ' ');
        return referencedNames.contains(spaced);
    }
}
