package systems.courant.sd.model.compile;

import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.StockDef;
import systems.courant.sd.model.def.SubscriptDef;
import systems.courant.sd.model.expr.BuiltinFunctions;

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

        Map<String, Integer> dimensionCounts = collectDimensionCounts(def);
        Pattern namePattern = buildNamePattern(subscriptedNames);

        return def.toBuilder()
                .clearStocks().clearFlows().clearVariables()
                .stocks(expandStocks(def, dimensionLabels, subscriptedNames, namePattern, dimensionCounts))
                .flows(expandFlows(def, dimensionLabels, subscriptedNames, namePattern, dimensionCounts))
                .variables(expandVariables(def, dimensionLabels, subscriptedNames, namePattern, dimensionCounts))
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

    private static Map<String, Integer> collectDimensionCounts(ModelDefinition def) {
        Map<String, Integer> counts = new HashMap<>();
        for (StockDef s : def.stocks()) {
            if (!s.subscripts().isEmpty()) {
                counts.put(s.name(), s.subscripts().size());
            }
        }
        for (FlowDef f : def.flows()) {
            if (!f.subscripts().isEmpty()) {
                counts.put(f.name(), f.subscripts().size());
            }
        }
        for (VariableDef a : def.variables()) {
            if (!a.subscripts().isEmpty()) {
                counts.put(a.name(), a.subscripts().size());
            }
        }
        return counts;
    }

    private static List<StockDef> expandStocks(ModelDefinition def,
                                                Map<String, List<String>> dimensionLabels,
                                                Set<String> subscriptedNames,
                                                Pattern namePattern,
                                                Map<String, Integer> dimensionCounts) {
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
                                              Pattern namePattern,
                                              Map<String, Integer> dimensionCounts) {
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
                            f.equation(), suffix, subscriptedNames, namePattern, dimensionCounts);
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
                                                      Pattern namePattern,
                                                      Map<String, Integer> dimensionCounts) {
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
                            a.equation(), suffix, subscriptedNames, namePattern, dimensionCounts);
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
     * their bracketed form for the given label. Partial subscript references (fewer
     * dimensions than defined) are completed with trailing dimensions from the current label.
     */
    static String rewriteEquation(String equation, String label,
                                  Set<String> subscriptedNames, Pattern namePattern,
                                  Map<String, Integer> dimensionCounts) {
        Matcher matcher = namePattern.matcher(equation);
        StringBuilder result = new StringBuilder();
        int cursor = 0;
        while (matcher.find()) {
            // Append text between previous match end and this match start
            result.append(equation, cursor, matcher.start());

            String matchedText = matcher.group();
            String rawName;
            boolean wasQuoted;
            if (matchedText.startsWith("`") && matchedText.endsWith("`")) {
                rawName = matchedText.substring(1, matchedText.length() - 1);
                wasQuoted = true;
            } else {
                rawName = matchedText;
                wasQuoted = false;
            }

            if (!subscriptedNames.contains(rawName)) {
                result.append(matchedText);
                cursor = matcher.end();
                continue;
            }

            int afterMatch = matcher.end();

            // If the name is followed by '(' and matches a built-in function,
            // treat it as a function call rather than a subscripted variable.
            if (afterMatch < equation.length()
                    && equation.charAt(afterMatch) == '('
                    && BuiltinFunctions.NAMES.contains(rawName.toUpperCase())) {
                result.append(matchedText);
                cursor = matcher.end();
                continue;
            }
            if (afterMatch < equation.length() && equation.charAt(afterMatch) == '[') {
                int closeBracket = equation.indexOf(']', afterMatch);
                if (closeBracket < 0) {
                    // Malformed bracket — leave as-is
                    result.append(matchedText);
                    cursor = matcher.end();
                } else {
                    String existing = equation.substring(afterMatch + 1, closeBracket);
                    int existingDims = countDimensions(existing);
                    int expectedDims = dimensionCounts.getOrDefault(rawName, existingDims);
                    if (existingDims < expectedDims) {
                        // Partial subscript — append trailing dimensions from current label
                        String[] labelParts = label.split(",");
                        StringBuilder fullSubscript = new StringBuilder(existing);
                        for (int i = existingDims; i < expectedDims && i < labelParts.length; i++) {
                            fullSubscript.append(',').append(labelParts[i]);
                        }
                        String prefix = wasQuoted ? "`" + rawName + "`" : rawName;
                        result.append(prefix).append('[').append(fullSubscript).append(']');
                    } else {
                        // Full subscript — leave name + brackets as-is
                        result.append(equation, matcher.start(), closeBracket + 1);
                    }
                    cursor = closeBracket + 1;
                }
            } else {
                // No bracket — expand with full label
                String prefix = wasQuoted ? "`" + rawName + "`" : rawName;
                result.append(prefix).append('[').append(label).append(']');
                cursor = matcher.end();
            }
        }
        // Append remaining text after last match
        result.append(equation, cursor, equation.length());
        return result.toString();
    }

    private static int countDimensions(String bracketContent) {
        if (bracketContent.isEmpty()) {
            return 0;
        }
        int count = 1;
        for (int i = 0; i < bracketContent.length(); i++) {
            if (bracketContent.charAt(i) == ',') {
                count++;
            }
        }
        return count;
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
            String quoted = Pattern.quote(sorted.get(i));
            // Match backtick-quoted form first, then unquoted with word boundaries
            sb.append("`").append(quoted).append("`");
            sb.append("|");
            sb.append("(?<![\\w])");
            sb.append(quoted);
            sb.append("(?![\\w])");
        }
        return Pattern.compile(sb.toString());
    }
}
