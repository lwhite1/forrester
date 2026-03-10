package systems.courant.forrester.model.def;

import systems.courant.forrester.model.def.ValidationIssue.Severity;
import systems.courant.forrester.model.expr.Expr;
import systems.courant.forrester.model.expr.ExprDependencies;
import systems.courant.forrester.model.expr.ExprParser;
import systems.courant.forrester.model.expr.ParseException;
import systems.courant.forrester.model.graph.DependencyGraph;

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

        // 6. CLD checks
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
                        "Flow '" + flow.name() + "' is disconnected (no source or sink stock)"));
            }
        }
    }

    private static void checkMissingUnits(ModelDefinition def, List<ValidationIssue> issues) {
        for (StockDef stock : def.stocks()) {
            if (stock.unit() == null || stock.unit().isBlank()) {
                issues.add(new ValidationIssue(Severity.WARNING, stock.name(),
                        "Stock '" + stock.name() + "' has no unit specified"));
            }
        }
        for (AuxDef aux : def.auxiliaries()) {
            if (aux.unit() == null || aux.unit().isBlank()) {
                issues.add(new ValidationIssue(Severity.WARNING, aux.name(),
                        "Auxiliary '" + aux.name() + "' has no unit specified"));
            }
        }
    }

    private static void checkAlgebraicLoops(ModelDefinition def, List<ValidationIssue> issues) {
        // Only attempt graph analysis if equations are parseable
        if (!equationsParseable(def)) {
            return;
        }

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
                                    + "} — circular dependency without a stock to break the loop"));
                }
            }
        }
    }

    private static boolean equationsParseable(ModelDefinition def) {
        for (FlowDef flow : def.flows()) {
            try {
                ExprParser.parse(flow.equation());
            } catch (ParseException e) {
                return false;
            }
        }
        for (AuxDef aux : def.auxiliaries()) {
            try {
                ExprParser.parse(aux.equation());
            } catch (ParseException e) {
                return false;
            }
        }
        return true;
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
                        "Parameter '" + aux.name() + "' is not referenced by any equation"));
            }
        }
        // Check lookup tables
        for (LookupTableDef table : def.lookupTables()) {
            if (!isReferenced(table.name(), referencedNames)) {
                issues.add(new ValidationIssue(Severity.WARNING, table.name(),
                        "Lookup table '" + table.name() + "' is not referenced by any equation"));
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
                                + "' is not connected by any causal link"));
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
                        "Causal link references non-existent source '" + link.from() + "'"));
            }
            if (!allNames.contains(link.to())) {
                issues.add(new ValidationIssue(Severity.ERROR, null,
                        "Causal link references non-existent target '" + link.to() + "'"));
            }
        }
    }

    private static void checkUnknownPolarity(ModelDefinition def,
            List<ValidationIssue> issues) {
        for (CausalLinkDef link : def.causalLinks()) {
            if (link.polarity() == CausalLinkDef.Polarity.UNKNOWN) {
                issues.add(new ValidationIssue(Severity.WARNING, link.from(),
                        "Causal link from '" + link.from() + "' to '" + link.to()
                                + "' has unknown polarity"));
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
        return referencedNames.contains(underscored);
    }
}
