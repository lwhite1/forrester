package systems.courant.sd.io.vensim;

import systems.courant.sd.io.AbstractModelImporter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pre-classifies Vensim equations into stocks, flows, lookups, and constants
 * before the main model-building pass. Separated from {@link VensimImporter}
 * so that classification logic can be tested and modified independently.
 */
final class ElementClassifier {

    private static final Pattern INTEG_PATTERN = Pattern.compile(
            "(?i)^INTEG\\s*\\(");
    private static final Pattern SUBSCRIPT_NAME_PATTERN = Pattern.compile(
            "^(.+?)\\[(.+?)\\]$");
    private static final Pattern A_FUNCTION_OF_PATTERN = Pattern.compile(
            "(?i)^A\\s+FUNCTION\\s+OF\\s*\\(");

    private ElementClassifier() {
    }

    /**
     * Scans all equations and classifies them by element type (stock, flow,
     * lookup, constant, etc.) without building model definitions yet.
     */
    static VensimImporter.PreClassificationResult preClassifyEquations(
            List<MdlEquation> equations, VensimImporter.SubscriptContext subscripts,
            VensimImporter.SimulationSettings sim, List<String> warnings) {
        Set<String> stockNames = new HashSet<>();
        Set<String> flowNames = new HashSet<>();
        Set<String> lookupNames = new HashSet<>();
        Set<String> cldVariableNames = new HashSet<>();
        Set<String> allNormalizedNames = new HashSet<>();

        Map<String, Double> constantValues = new HashMap<>();
        constantValues.put("TIME_STEP", sim.timeStep());
        constantValues.put("INITIAL_TIME", sim.initialTime());
        constantValues.put("FINAL_TIME", sim.finalTime());

        for (MdlEquation eq : equations) {
            String name = eq.name().strip();
            if (name.isEmpty() || isSystemVar(name)) {
                continue;
            }
            if (isDocumentationBlock(eq.expression())) {
                continue;
            }

            Matcher subMatcher = SUBSCRIPT_NAME_PATTERN.matcher(name);
            if (subMatcher.matches()) {
                if (preClassifySubscriptedEquation(subMatcher, eq,
                        subscripts.subscriptDimensions(), stockNames,
                        constantValues, allNormalizedNames)) {
                    continue;
                }
            }

            String eqName = VensimExprTranslator.normalizeName(name);

            if (eq.operator().equals(":") || eq.operator().equals("<->")) {
                continue;
            }

            if (eq.operator().equals("()")) {
                lookupNames.add(eqName);
                continue;
            }
            if (INTEG_PATTERN.matcher(eq.expression()).find()) {
                stockNames.add(eqName);
                continue;
            }
            if (eq.operator().equals(":=")) {
                continue;
            }
            if (AbstractModelImporter.isNumericLiteral(eq.expression())) {
                constantValues.put(eqName, Double.parseDouble(eq.expression().strip()));
                String altNormalized = name.replace(" ", "_");
                if (!altNormalized.equals(eqName)) {
                    constantValues.put(altNormalized, Double.parseDouble(eq.expression().strip()));
                }
            }
        }

        return new VensimImporter.PreClassificationResult(stockNames, flowNames, lookupNames,
                cldVariableNames, allNormalizedNames, constantValues);
    }

    private static boolean preClassifySubscriptedEquation(
            Matcher subMatcher, MdlEquation eq,
            Map<String, List<String>> subscriptDimensions,
            Set<String> stockNames,
            Map<String, Double> constantValues,
            Set<String> allNormalizedNames) {
        String baseName = subMatcher.group(1).strip();
        String dimNameRaw = subMatcher.group(2).strip();
        String dimKey = VensimExprTranslator.normalizeName(dimNameRaw);
        List<String> labels = subscriptDimensions.get(dimKey);
        if (labels != null) {
            String normalizedBase = VensimExprTranslator.normalizeName(baseName);
            boolean isInteg = INTEG_PATTERN.matcher(eq.expression()).find();
            List<String> perLabelValues = VensimSubscriptExpander.splitSubscriptValues(
                    eq.expression(), labels.size());
            for (int li = 0; li < labels.size(); li++) {
                String expandedName = normalizedBase + "_" + labels.get(li);
                allNormalizedNames.add(expandedName);
                if (isInteg) {
                    stockNames.add(expandedName);
                }
                if (perLabelValues != null) {
                    String val = perLabelValues.get(li).strip();
                    if (AbstractModelImporter.isNumericLiteral(val)) {
                        double numVal = Double.parseDouble(val);
                        constantValues.put(expandedName, numVal);
                        String altKey = VensimExprTranslator.normalizeName(
                                baseName + "[" + labels.get(li) + "]");
                        constantValues.put(altKey, numVal);
                    }
                }
            }
            return true;
        }

        if (dimNameRaw.contains(",")) {
            String normalizedBase = VensimExprTranslator.normalizeName(baseName);
            boolean isInteg = INTEG_PATTERN.matcher(eq.expression()).find();
            List<List<String>> combos = VensimSubscriptExpander.resolveMultiDimLabels(
                    dimNameRaw, subscriptDimensions);
            List<String> perLabelValues = VensimSubscriptExpander.splitSubscriptValues(
                    eq.expression(), combos.size());
            for (int ci = 0; ci < combos.size(); ci++) {
                String expandedName = normalizedBase + "_"
                        + String.join("_", combos.get(ci));
                allNormalizedNames.add(expandedName);
                if (isInteg) {
                    stockNames.add(expandedName);
                }
                if (perLabelValues != null) {
                    String val = perLabelValues.get(ci).strip();
                    if (AbstractModelImporter.isNumericLiteral(val)) {
                        constantValues.put(expandedName,
                                Double.parseDouble(val));
                    }
                }
            }
            return true;
        }

        String normalizedSub = VensimExprTranslator.normalizeName(dimNameRaw);
        boolean isLabel = subscriptDimensions.values().stream()
                .anyMatch(lbls -> lbls.contains(normalizedSub));
        if (isLabel) {
            String normalizedBase = VensimExprTranslator.normalizeName(baseName);
            String expandedName = normalizedBase + "_" + normalizedSub;
            allNormalizedNames.add(expandedName);
            boolean isInteg = INTEG_PATTERN.matcher(eq.expression()).find();
            if (isInteg) {
                stockNames.add(expandedName);
            }
            if (AbstractModelImporter.isNumericLiteral(eq.expression())) {
                constantValues.put(expandedName,
                        Double.parseDouble(eq.expression().strip()));
            }
            return true;
        }

        return false;
    }

    private static boolean isSystemVar(String name) {
        return Set.of("INITIAL TIME", "FINAL TIME", "TIME STEP", "SAVEPER")
                .contains(name.strip().toUpperCase(java.util.Locale.ROOT));
    }

    static boolean isDocumentationBlock(String expression) {
        if (expression == null || expression.isBlank()) {
            return false;
        }
        String trimmed = expression.strip();
        return A_FUNCTION_OF_PATTERN.matcher(trimmed).find();
    }
}
