package systems.courant.sd.io.vensim;

import systems.courant.sd.model.def.ModelDefinitionBuilder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts subscript dimensions, equivalences, and mappings from parsed
 * Vensim equations. Separated from {@link VensimImporter} so that subscript
 * logic can be tested and modified independently of other import concerns.
 */
final class SubscriptProcessor {

    private static final Pattern SUBSCRIPT_NAME_PATTERN = Pattern.compile(
            "^(.+?)\\[(.+?)\\]$");
    private static final Set<String> CONTROL_GROUPS = Set.of(".Control");
    private static final Set<String> SYSTEM_VAR_KEYS = Set.of(
            "INITIAL TIME", "FINAL TIME", "TIME STEP", "SAVEPER");

    private SubscriptProcessor() {
    }

    /**
     * Scans all equations to collect Vensim names, control variables,
     * subscript dimensions (with labels), mappings, and equivalences.
     */
    static VensimImporter.SubscriptContext collectNamesAndSubscripts(
            List<MdlEquation> equations) {
        Set<String> vensimNames = new HashSet<>();
        Map<String, MdlEquation> controlVars = new LinkedHashMap<>();
        Map<String, List<String>> subscriptDimensions = new LinkedHashMap<>();
        Map<String, List<String>> subscriptDisplayLabels = new LinkedHashMap<>();
        Map<String, VensimImporter.SubscriptMapping> subscriptMappings = new LinkedHashMap<>();
        Map<String, String> equivalences = new LinkedHashMap<>();
        Map<String, String> equivalenceDisplayNames = new LinkedHashMap<>();

        for (MdlEquation eq : equations) {
            String name = eq.name().strip();
            if (name.isEmpty()) {
                continue;
            }
            vensimNames.add(name);
            if (name.startsWith("\"") && name.endsWith("\"") && name.length() > 2) {
                vensimNames.add(name.substring(1, name.length() - 1));
            }
            Matcher baseMatcher = SUBSCRIPT_NAME_PATTERN.matcher(name);
            if (baseMatcher.matches()) {
                vensimNames.add(baseMatcher.group(1).strip());
            }

            if (isSystemVar(name) || CONTROL_GROUPS.contains(eq.group())) {
                controlVars.put(normalizeSystemVarKey(name), eq);
            }

            if (eq.operator().equals(":")) {
                String dimName = VensimExprTranslator.normalizeName(name);
                String rawExpr = eq.expression().strip();

                int arrowPos = rawExpr.indexOf("->");
                String labelsStr = arrowPos >= 0
                        ? rawExpr.substring(0, arrowPos).strip() : rawExpr;

                List<String> normalizedLabels = Arrays.stream(labelsStr.split(","))
                        .map(String::strip)
                        .filter(s -> !s.isEmpty())
                        .map(VensimExprTranslator::normalizeName)
                        .toList();
                List<String> displayLabels = Arrays.stream(labelsStr.split(","))
                        .map(String::strip)
                        .filter(s -> !s.isEmpty())
                        .map(VensimExprTranslator::normalizeDisplayName)
                        .toList();
                if (!normalizedLabels.isEmpty()) {
                    subscriptDimensions.put(dimName, normalizedLabels);
                    subscriptDisplayLabels.put(dimName, displayLabels);
                }

                if (arrowPos >= 0) {
                    String targetDim = VensimExprTranslator.normalizeName(
                            rawExpr.substring(arrowPos + 2).strip());
                    List<String> rawLabels = Arrays.stream(labelsStr.split(","))
                            .map(String::strip)
                            .filter(s -> !s.isEmpty())
                            .toList();
                    subscriptMappings.put(dimName, new VensimImporter.SubscriptMapping(
                            targetDim, name.strip(), rawLabels));
                }
            }

            if (eq.operator().equals("<->")) {
                String dimName = VensimExprTranslator.normalizeName(name);
                String displayName = VensimExprTranslator.normalizeDisplayName(name);
                String targetDim = VensimExprTranslator.normalizeName(
                        eq.expression().strip());
                equivalences.put(dimName, targetDim);
                equivalenceDisplayNames.put(dimName, displayName);
            }
        }

        return new VensimImporter.SubscriptContext(vensimNames, controlVars,
                subscriptDimensions, subscriptDisplayLabels, subscriptMappings,
                equivalences, equivalenceDisplayNames);
    }

    /**
     * Resolves dimension equivalences by copying labels from target dimensions
     * to equivalent dimensions, iterating until stable.
     */
    static void resolveEquivalences(VensimImporter.SubscriptContext subscripts) {
        int maxIterations = subscripts.equivalences().size() + 1;
        boolean changed = true;
        while (changed) {
            if (--maxIterations < 0) {
                throw new IllegalArgumentException(
                        "Circular dimension equivalences detected — cannot resolve subscript mappings");
            }
            changed = false;
            for (var entry : subscripts.equivalences().entrySet()) {
                String dimName = entry.getKey();
                String targetDim = entry.getValue();
                List<String> targetLabels = subscripts.subscriptDimensions().get(targetDim);
                List<String> targetDisplayLabels = subscripts.subscriptDisplayLabels().get(targetDim);
                if (targetLabels != null
                        && !targetLabels.equals(subscripts.subscriptDimensions().get(dimName))) {
                    subscripts.subscriptDimensions().put(dimName, targetLabels);
                    subscripts.subscriptDisplayLabels().put(dimName, targetDisplayLabels);
                    changed = true;
                }
            }
        }
    }

    /**
     * Registers equivalence dimensions as subscripts in the model builder.
     */
    static void registerEquivalenceDimensions(VensimImporter.SubscriptContext subscripts,
                                               ModelDefinitionBuilder builder) {
        for (var entry : subscripts.equivalences().entrySet()) {
            String dimName = entry.getKey();
            List<String> labels = subscripts.subscriptDisplayLabels().get(dimName);
            String displayName = subscripts.equivalenceDisplayNames().get(dimName);
            if (labels != null && displayName != null) {
                builder.subscript(displayName, labels);
            }
        }
    }

    private static boolean isSystemVar(String name) {
        return SYSTEM_VAR_KEYS.contains(name.strip().toUpperCase(java.util.Locale.ROOT));
    }

    private static String normalizeSystemVarKey(String name) {
        return name.strip().toUpperCase(java.util.Locale.ROOT).replace(" ", "_");
    }
}
