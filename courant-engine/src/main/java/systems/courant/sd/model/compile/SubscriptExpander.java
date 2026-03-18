package systems.courant.sd.model.compile;

import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.StockDef;
import systems.courant.sd.model.def.SubscriptDef;

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

        Map<String, List<String>> dimensionLabels = buildDimensionLabels(def);
        Set<String> subscriptedNames = collectSubscriptedNames(def);

        if (subscriptedNames.isEmpty()) {
            return def;
        }

        Pattern namePattern = buildNamePattern(subscriptedNames);

        return def.toBuilder()
                .clearStocks().clearFlows().clearVariables()
                .stocks(expandStocks(def, dimensionLabels, subscriptedNames, namePattern))
                .flows(expandFlows(def, dimensionLabels, subscriptedNames, namePattern))
                .variables(expandVariables(def, dimensionLabels, subscriptedNames, namePattern))
                .build();
    }

    private static Map<String, List<String>> buildDimensionLabels(ModelDefinition def) {
        Map<String, List<String>> dimensionLabels = new HashMap<>();
        for (SubscriptDef sub : def.subscripts()) {
            dimensionLabels.put(sub.name(), sub.labels());
        }
        return dimensionLabels;
    }

    private static Set<String> collectSubscriptedNames(ModelDefinition def) {
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
        for (VariableDef a : def.variables()) {
            if (!a.subscripts().isEmpty()) {
                subscriptedNames.add(a.name());
            }
        }
        return subscriptedNames;
    }

    private static List<StockDef> expandStocks(ModelDefinition def,
                                                Map<String, List<String>> dimensionLabels,
                                                Set<String> subscriptedNames,
                                                Pattern namePattern) {
        List<StockDef> expanded = new ArrayList<>();
        for (StockDef s : def.stocks()) {
            if (s.subscripts().isEmpty()) {
                expanded.add(s);
            } else {
                List<List<String>> labelLists = resolveLabels(
                        s.subscripts(), dimensionLabels, "Stock", s.name());
                for (List<String> combo : cartesianProduct(labelLists)) {
                    String suffix = joinLabels(combo);
                    expanded.add(new StockDef(
                            s.name() + "[" + suffix + "]",
                            s.comment(),
                            s.initialValue(),
                            s.initialExpression(),
                            s.unit(),
                            s.negativeValuePolicy(),
                            List.of()));
                }
            }
        }
        return expanded;
    }

    private static List<FlowDef> expandFlows(ModelDefinition def,
                                              Map<String, List<String>> dimensionLabels,
                                              Set<String> subscriptedNames,
                                              Pattern namePattern) {
        List<FlowDef> expanded = new ArrayList<>();
        for (FlowDef f : def.flows()) {
            if (f.subscripts().isEmpty()) {
                expanded.add(f);
            } else {
                List<List<String>> labelLists = resolveLabels(
                        f.subscripts(), dimensionLabels, "Flow", f.name());
                for (List<String> combo : cartesianProduct(labelLists)) {
                    String suffix = joinLabels(combo);
                    String expandedEq = rewriteEquation(
                            f.equation(), suffix, subscriptedNames, namePattern);
                    String expandedSource = expandReference(
                            f.source(), suffix, subscriptedNames);
                    String expandedSink = expandReference(
                            f.sink(), suffix, subscriptedNames);
                    expanded.add(new FlowDef(
                            f.name() + "[" + suffix + "]",
                            f.comment(),
                            expandedEq,
                            f.timeUnit(),
                            f.materialUnit(),
                            expandedSource,
                            expandedSink,
                            List.of()));
                }
            }
        }
        return expanded;
    }

    private static List<VariableDef> expandVariables(ModelDefinition def,
                                                      Map<String, List<String>> dimensionLabels,
                                                      Set<String> subscriptedNames,
                                                      Pattern namePattern) {
        List<VariableDef> expanded = new ArrayList<>();
        for (VariableDef a : def.variables()) {
            if (a.subscripts().isEmpty()) {
                expanded.add(a);
            } else {
                List<List<String>> labelLists = resolveLabels(
                        a.subscripts(), dimensionLabels, "Auxiliary", a.name());
                for (List<String> combo : cartesianProduct(labelLists)) {
                    String suffix = joinLabels(combo);
                    String expandedEq = rewriteEquation(
                            a.equation(), suffix, subscriptedNames, namePattern);
                    expanded.add(new VariableDef(
                            a.name() + "[" + suffix + "]",
                            a.comment(),
                            expandedEq,
                            a.unit()));
                }
            }
        }
        return expanded;
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
     * Resolves the label lists for each subscript dimension of an element.
     */
    private static List<List<String>> resolveLabels(List<String> subscripts,
                                                    Map<String, List<String>> dimensionLabels,
                                                    String elementType, String elementName) {
        List<List<String>> labelLists = new ArrayList<>();
        for (String dimName : subscripts) {
            List<String> labels = dimensionLabels.get(dimName);
            if (labels == null) {
                throw new CompilationException(
                        elementType + " '" + elementName + "' references unknown subscript: " + dimName,
                        elementName);
            }
            labelLists.add(labels);
        }
        return labelLists;
    }

    /**
     * Computes the Cartesian product of the given label lists.
     * For example, given [["A","B"], ["X","Y"]], returns
     * [["A","X"], ["A","Y"], ["B","X"], ["B","Y"]].
     */
    static List<List<String>> cartesianProduct(List<List<String>> labelLists) {
        List<List<String>> result = new ArrayList<>();
        result.add(List.of());
        for (List<String> labels : labelLists) {
            List<List<String>> next = new ArrayList<>();
            for (List<String> partial : result) {
                for (String label : labels) {
                    List<String> extended = new ArrayList<>(partial);
                    extended.add(label);
                    next.add(extended);
                }
            }
            result = next;
        }
        return result;
    }

    private static String joinLabels(List<String> combo) {
        return String.join(",", combo);
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
