package systems.courant.sd.io.vensim;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Expands subscripted Vensim variables into per-label scalar variables.
 *
 * <p>Handles both single-dimension subscripts (e.g., {@code x[Region]}) and
 * multi-dimensional subscripts (e.g., {@code y[Region,Product]}). For each
 * combination of labels, a synthetic {@link MdlEquation} is created and
 * delegated back to the importer's {@code classifyAndBuild} pipeline.
 *
 * <p>This class was extracted from {@link VensimImporter} to isolate the
 * ~250 lines of subscript expansion logic into a single-responsibility unit.
 */
final class VensimSubscriptExpander {

    private VensimSubscriptExpander() {
    }

    /**
     * Expands a subscripted variable into per-label scalar variables.
     * For comma-separated values, assigns each value to the corresponding label.
     * For formulas, creates copies with the dimension reference replaced by each label.
     *
     * @param eq               the original subscripted equation
     * @param baseName         the raw base name (before brackets)
     * @param dimName          the normalized dimension name
     * @param dimNameRaw       the raw dimension name (inside brackets)
     * @param normalizedLabels the normalized labels for this dimension
     * @param displayLabels    the display labels for this dimension
     * @param unit             the cleaned unit string
     * @param ctx              the shared import context
     * @param classifyCallback callback to classify each expanded equation
     */
    static void expandSubscriptedVariable(MdlEquation eq, String baseName,
                                           String dimName, String dimNameRaw,
                                           List<String> normalizedLabels,
                                           List<String> displayLabels,
                                           String unit, ImportContext ctx,
                                           ClassifyCallback classifyCallback) {
        String operator = eq.operator();
        String expression = eq.expression();
        String normalizedBase = VensimExprTranslator.normalizeName(baseName);
        String displayBase = VensimExprTranslator.normalizeDisplayName(baseName);

        List<String> perLabelValues = splitSubscriptValues(expression, normalizedLabels.size());

        for (int i = 0; i < normalizedLabels.size(); i++) {
            String label = normalizedLabels.get(i);
            String displayLabel = displayLabels.get(i);
            String expandedEqName = normalizedBase + "_" + label;
            String expandedDisplayName = displayBase + " " + displayLabel;
            String comment = eq.comment().isBlank() ? eq.name().strip() : eq.comment();

            String labelExpression;
            if (perLabelValues != null) {
                labelExpression = perLabelValues.get(i).strip();
            } else {
                labelExpression = replaceDimInSubscripts(expression, dimNameRaw, label);

                for (var entry : ctx.subscriptMappings().entrySet()) {
                    VensimImporter.SubscriptMapping mapping = entry.getValue();
                    if (mapping.targetDimension().equals(dimName)
                            && i < mapping.rawLabels().size()) {
                        String mappedLabel = mapping.rawLabels().get(i);
                        labelExpression = replaceDimInSubscripts(
                                labelExpression, mapping.rawDimName(), mappedLabel);
                    }
                }
            }

            MdlEquation labelEq = new MdlEquation(
                    expandedDisplayName, operator, labelExpression,
                    eq.units(), comment, eq.group());
            classifyCallback.classifyAndBuild(labelEq, expandedDisplayName,
                    expandedEqName, unit, comment);
        }
    }

    /**
     * Expands a variable with multi-dimensional subscripts (comma-separated).
     * Generates cross-product of dimension labels and creates per-combination variables.
     *
     * @param eq               the original multi-dimensional subscripted equation
     * @param baseName         the raw base name (before brackets)
     * @param dimNameRaw       the raw comma-separated dimension specifier
     * @param unit             the cleaned unit string
     * @param ctx              the shared import context
     * @param classifyCallback callback to classify each expanded equation
     */
    static void expandMultiDimSubscriptedVariable(MdlEquation eq, String baseName,
                                                   String dimNameRaw, String unit,
                                                   ImportContext ctx,
                                                   ClassifyCallback classifyCallback) {
        List<List<String>> combos = resolveMultiDimLabels(
                dimNameRaw, ctx.subscriptDimensions());

        String[] rawSubs = dimNameRaw.split(",");
        List<List<String>> displayPerDim = new ArrayList<>();
        for (String sub : rawSubs) {
            String key = VensimExprTranslator.normalizeName(sub.strip());
            List<String> dimDisplayLabels = ctx.subscriptDisplayLabels().get(key);
            if (dimDisplayLabels != null) {
                displayPerDim.add(dimDisplayLabels);
            } else {
                displayPerDim.add(List.of(
                        VensimExprTranslator.normalizeDisplayName(sub.strip())));
            }
        }
        List<List<String>> displayCombos = crossProduct(displayPerDim);

        String normalizedBase = VensimExprTranslator.normalizeName(baseName);
        String displayBase = VensimExprTranslator.normalizeDisplayName(baseName);

        List<String> perLabelValues = splitSubscriptValues(
                eq.expression(), combos.size());

        for (int ci = 0; ci < combos.size(); ci++) {
            List<String> combo = combos.get(ci);
            List<String> displayCombo = displayCombos.get(ci);

            String expandedEqName = normalizedBase + "_" + String.join("_", combo);
            String expandedDisplayName = displayBase + " " + String.join(" ", displayCombo);
            String comment = eq.comment().isBlank() ? eq.name().strip() : eq.comment();

            String labelExpression;
            if (perLabelValues != null) {
                labelExpression = perLabelValues.get(ci).strip();
            } else {
                labelExpression = eq.expression();
                String[] subs = dimNameRaw.split(",");
                for (int si = 0; si < subs.length; si++) {
                    String sub = subs[si].strip();
                    String subKey = VensimExprTranslator.normalizeName(sub);
                    List<String> dimLabels = ctx.subscriptDimensions().get(subKey);
                    if (dimLabels != null) {
                        String label = combo.get(si);
                        labelExpression = replaceDimInSubscripts(
                                labelExpression, sub, label);
                    }
                }
            }

            MdlEquation labelEq = new MdlEquation(
                    expandedDisplayName, eq.operator(), labelExpression,
                    eq.units(), comment, eq.group());
            classifyCallback.classifyAndBuild(labelEq, expandedDisplayName,
                    expandedEqName, unit, comment);
        }
    }

    /**
     * Splits a subscript expression into per-label values if it's comma-separated.
     * Returns null if the expression is a formula (not comma-separated values).
     */
    static List<String> splitSubscriptValues(String expression, int expectedCount) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                parts.add(expression.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(expression.substring(start));

        if (parts.size() == expectedCount) {
            return parts;
        }
        return null;
    }

    /**
     * Replaces a dimension name within bracket subscripts in an expression.
     * Handles both single and comma-separated subscripts.
     * For example, with dimName="task", replacement="design":
     * "x[task]" becomes "x[design]" and "y[task,prereqtask]" becomes "y[design,prereqtask]"
     */
    static String replaceDimInSubscripts(String expr, String dimName, String replacement) {
        StringBuilder result = new StringBuilder();
        int pos = 0;
        while (pos < expr.length()) {
            int bracketStart = expr.indexOf('[', pos);
            if (bracketStart < 0) {
                result.append(expr, pos, expr.length());
                break;
            }
            int bracketEnd = findMatchingBracket(expr, bracketStart);
            if (bracketEnd < 0) {
                result.append(expr, pos, expr.length());
                break;
            }
            result.append(expr, pos, bracketStart + 1);
            String content = expr.substring(bracketStart + 1, bracketEnd);

            String[] parts = content.split(",", -1);
            for (int j = 0; j < parts.length; j++) {
                if (parts[j].strip().equals(dimName)) {
                    parts[j] = parts[j].replace(dimName, replacement);
                }
            }
            result.append(String.join(",", parts));
            result.append(']');
            pos = bracketEnd + 1;
        }
        return result.toString();
    }

    /**
     * Finds the closing ']' that matches the opening '[' at the given position,
     * accounting for nested brackets and skipping content inside double quotes.
     */
    static int findMatchingBracket(String expr, int openPos) {
        int depth = 0;
        boolean inQuote = false;
        for (int i = openPos; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (!inQuote) {
                if (c == '[') {
                    depth++;
                } else if (c == ']') {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Resolves multi-dimensional subscript labels into a cross-product list.
     * Each sub is checked against subscriptDimensions; dimensions expand to labels,
     * specific labels become singletons.
     */
    static List<List<String>> resolveMultiDimLabels(String dimNameRaw,
                                                     Map<String, List<String>> subscriptDimensions) {
        String[] subs = dimNameRaw.split(",");
        List<List<String>> perDimLabels = new ArrayList<>();
        for (String sub : subs) {
            String key = VensimExprTranslator.normalizeName(sub.strip());
            List<String> dimLabels = subscriptDimensions.get(key);
            if (dimLabels != null) {
                perDimLabels.add(dimLabels);
            } else {
                perDimLabels.add(List.of(key));
            }
        }
        return crossProduct(perDimLabels);
    }

    /**
     * Computes the Cartesian cross-product of multiple label lists.
     */
    static List<List<String>> crossProduct(List<List<String>> lists) {
        List<List<String>> result = new ArrayList<>();
        result.add(new ArrayList<>());
        for (List<String> list : lists) {
            List<List<String>> newResult = new ArrayList<>();
            for (List<String> existing : result) {
                for (String item : list) {
                    List<String> combo = new ArrayList<>(existing);
                    combo.add(item);
                    newResult.add(combo);
                }
            }
            result = newResult;
        }
        return result;
    }

    /**
     * Callback interface for delegating classification of expanded equations
     * back to the importer.
     */
    @FunctionalInterface
    interface ClassifyCallback {
        void classifyAndBuild(MdlEquation eq, String displayName,
                              String eqName, String unit, String comment);
    }
}
