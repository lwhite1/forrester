package systems.courant.forrester.model.compile;

import systems.courant.forrester.model.def.AuxDef;
import systems.courant.forrester.model.def.FlowDef;
import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.StockDef;
import systems.courant.forrester.model.def.SubscriptDef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands subscripted model elements into multiple scalar elements before compilation.
 *
 * <p>For each subscripted stock/flow/aux, the expander creates one scalar copy per label
 * in the subscript dimension. Element names become {@code "name[label]"}, and equations
 * are rewritten so that references to other subscripted elements also use the bracket notation.
 *
 * <p>Example: given {@code StockDef("Population", subscripts=["Region"])} with
 * {@code SubscriptDef("Region", ["North", "South"])}, produces two scalar stocks:
 * {@code StockDef("Population[North]")} and {@code StockDef("Population[South]")}.
 */
public class SubscriptExpander {

    /**
     * Returns a new ModelDefinition with all subscripted elements expanded into scalar elements.
     * If the definition has no subscripted elements, returns the original unchanged.
     */
    public static ModelDefinition expand(ModelDefinition def) {
        if (def.subscripts().isEmpty()) {
            return def;
        }

        // Build subscript dimension lookup: dimension name → labels
        Map<String, List<String>> dimensionLabels = new HashMap<>();
        for (SubscriptDef sub : def.subscripts()) {
            dimensionLabels.put(sub.name(), sub.labels());
        }

        // Identify which element names are subscripted (to rewrite equations)
        Set<String> subscriptedNames = new HashSet<>();
        for (StockDef s : def.stocks()) {
            if (!s.subscripts().isEmpty()) {
                subscriptedNames.add(s.name());
            }
        }
        for (FlowDef f : def.flows()) {
            if (!f.subscripts().isEmpty()) {
                subscriptedNames.add(f.name());
            }
        }
        for (AuxDef a : def.auxiliaries()) {
            if (!a.subscripts().isEmpty()) {
                subscriptedNames.add(a.name());
            }
        }

        if (subscriptedNames.isEmpty()) {
            return def;
        }

        // Build regex pattern to match subscripted element names as whole words
        Pattern namePattern = buildNamePattern(subscriptedNames);

        // Expand stocks
        List<StockDef> expandedStocks = new ArrayList<>();
        for (StockDef s : def.stocks()) {
            if (s.subscripts().isEmpty()) {
                expandedStocks.add(s);
            } else {
                for (String dimName : s.subscripts()) {
                    List<String> labels = dimensionLabels.get(dimName);
                    if (labels == null) {
                        throw new CompilationException(
                                "Stock '" + s.name() + "' references unknown subscript: " + dimName,
                                s.name());
                    }
                    for (String label : labels) {
                        expandedStocks.add(new StockDef(
                                s.name() + "[" + label + "]",
                                s.comment(),
                                s.initialValue(),
                                s.initialExpression(),
                                s.unit(),
                                s.negativeValuePolicy(),
                                List.of()));
                    }
                }
            }
        }

        // Expand flows
        List<FlowDef> expandedFlows = new ArrayList<>();
        for (FlowDef f : def.flows()) {
            if (f.subscripts().isEmpty()) {
                expandedFlows.add(f);
            } else {
                for (String dimName : f.subscripts()) {
                    List<String> labels = dimensionLabels.get(dimName);
                    if (labels == null) {
                        throw new CompilationException(
                                "Flow '" + f.name() + "' references unknown subscript: " + dimName,
                                f.name());
                    }
                    for (String label : labels) {
                        String expandedEq = rewriteEquation(
                                f.equation(), label, subscriptedNames, namePattern);
                        String expandedSource = expandReference(
                                f.source(), label, subscriptedNames);
                        String expandedSink = expandReference(
                                f.sink(), label, subscriptedNames);
                        expandedFlows.add(new FlowDef(
                                f.name() + "[" + label + "]",
                                f.comment(),
                                expandedEq,
                                f.timeUnit(),
                                expandedSource,
                                expandedSink));
                    }
                }
            }
        }

        // Expand auxiliaries
        List<AuxDef> expandedAuxes = new ArrayList<>();
        for (AuxDef a : def.auxiliaries()) {
            if (a.subscripts().isEmpty()) {
                expandedAuxes.add(a);
            } else {
                for (String dimName : a.subscripts()) {
                    List<String> labels = dimensionLabels.get(dimName);
                    if (labels == null) {
                        throw new CompilationException(
                                "Auxiliary '" + a.name() + "' references unknown subscript: " + dimName,
                                a.name());
                    }
                    for (String label : labels) {
                        String expandedEq = rewriteEquation(
                                a.equation(), label, subscriptedNames, namePattern);
                        expandedAuxes.add(new AuxDef(
                                a.name() + "[" + label + "]",
                                a.comment(),
                                expandedEq,
                                a.unit()));
                    }
                }
            }
        }

        return new ModelDefinition(
                def.name(), def.comment(), def.moduleInterface(),
                expandedStocks, expandedFlows, expandedAuxes,
                def.constants(), def.lookupTables(), def.modules(),
                def.subscripts(), def.cldVariables(), def.causalLinks(),
                def.views(), def.defaultSimulation());
    }

    /**
     * Rewrites an equation string, replacing references to subscripted elements with
     * their bracketed form for the given label.
     */
    static String rewriteEquation(String equation, String label,
                                  Set<String> subscriptedNames, Pattern namePattern) {
        Matcher matcher = namePattern.matcher(equation);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String matchedName = matcher.group();
            if (subscriptedNames.contains(matchedName)) {
                // Check if already has a bracket suffix (don't double-expand)
                int afterMatch = matcher.end();
                if (afterMatch < equation.length() && equation.charAt(afterMatch) == '[') {
                    matcher.appendReplacement(result, Matcher.quoteReplacement(matchedName));
                } else {
                    matcher.appendReplacement(result,
                            Matcher.quoteReplacement(matchedName + "[" + label + "]"));
                }
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matchedName));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * If a source/sink reference names a subscripted element, expand it with the label.
     */
    private static String expandReference(String ref, String label, Set<String> subscriptedNames) {
        if (ref == null) {
            return null;
        }
        if (subscriptedNames.contains(ref)) {
            return ref + "[" + label + "]";
        }
        return ref;
    }

    /**
     * Builds a regex pattern that matches any of the given names as whole identifiers.
     * Uses word-boundary-like logic that respects identifier characters.
     */
    private static Pattern buildNamePattern(Set<String> names) {
        // Sort by length descending to match longer names first (avoid partial matches)
        List<String> sorted = new ArrayList<>(names);
        sorted.sort((a, b) -> Integer.compare(b.length(), a.length()));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) {
                sb.append('|');
            }
            // Use lookahead/lookbehind for identifier boundaries
            sb.append("(?<![\\w])");
            sb.append(Pattern.quote(sorted.get(i)));
            sb.append("(?![\\w])");
        }
        return Pattern.compile(sb.toString());
    }
}
