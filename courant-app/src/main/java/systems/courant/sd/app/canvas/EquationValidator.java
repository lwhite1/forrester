package systems.courant.sd.app.canvas;

import systems.courant.sd.model.expr.Expr;
import systems.courant.sd.model.expr.ExprDependencies;
import systems.courant.sd.model.expr.ExprParser;
import systems.courant.sd.model.expr.ParseException;

import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.StockDef;
import systems.courant.sd.model.def.SubscriptDef;
import systems.courant.sd.model.def.VariableDef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates equation strings for syntax and reference errors.
 * Used for real-time feedback in equation fields.
 */
public final class EquationValidator {

    private static final Set<String> BUILTIN_NAMES = Set.of(
            "TIME", "DT", "Pi", "PI", "E");

    private EquationValidator() {
    }

    /**
     * Result of validating an equation string.
     *
     * @param valid   true if no errors were found
     * @param message error description, or null if valid
     * @param warning optional warning text (may be non-null even when valid is true)
     */
    public record Result(boolean valid, String message, String warning) {
        static final Result OK = new Result(true, null, null);
    }

    /**
     * Validates the given equation text against the model's known element names.
     * Checks for syntax errors first, then unresolved references.
     *
     * @param equation    the equation text to validate
     * @param editor      the model editor providing known element names
     * @param selfName    the name of the element being edited (excluded from self-reference check)
     * @return validation result
     */
    public static Result validate(String equation, ModelEditor editor, String selfName) {
        if (equation == null || equation.isBlank()) {
            return Result.OK; // blank handled by commit logic
        }

        // 1. Syntax check
        Expr expr;
        try {
            expr = ExprParser.parse(equation);
        } catch (ParseException e) {
            return new Result(false, formatParseError(e), null);
        }

        // 2. Reference check
        Set<String> refs = ExprDependencies.extract(expr);
        Set<String> knownNames = collectKnownNames(editor);
        List<String> unknowns = new ArrayList<>();
        for (String ref : refs) {
            String resolved = ref.replace('_', ' ');
            if (!knownNames.contains(ref) && !knownNames.contains(resolved)
                    && !BUILTIN_NAMES.contains(ref)) {
                unknowns.add(ref);
            }
        }

        if (!unknowns.isEmpty()) {
            if (unknowns.size() == 1) {
                String unknown = unknowns.getFirst();
                String suggestion = findClosestMatch(unknown, knownNames);
                String msg = "Unknown variable '" + unknown + "'";
                if (suggestion != null) {
                    msg += " \u2014 did you mean '" + suggestion + "'?";
                }
                return new Result(false, msg, null);
            }
            return new Result(false,
                    "Unknown variables: " + String.join(", ", unknowns), null);
        }

        // 3. Subscript completeness check
        String warning = checkSubscriptCompleteness(refs, editor, selfName);
        if (warning != null) {
            return new Result(true, null, warning);
        }

        return Result.OK;
    }

    private static String formatParseError(ParseException e) {
        String msg = e.getMessage();
        // Strip the "(at position N)" suffix added by ParseException constructor
        // for a cleaner display, since position is less useful in a text field
        int idx = msg.lastIndexOf(" (at position ");
        if (idx > 0) {
            return msg.substring(0, idx);
        }
        return msg;
    }

    private static Set<String> collectKnownNames(ModelEditor editor) {
        Set<String> names = new HashSet<>();
        editor.getStocks().forEach(s -> names.add(s.name()));
        editor.getFlows().forEach(f -> names.add(f.name()));
        editor.getVariables().forEach(a -> names.add(a.name()));
        editor.getLookupTables().forEach(t -> names.add(t.name()));
        editor.getModules().forEach(m -> {
            names.add(m.instanceName());
            names.addAll(m.outputBindings().values());
        });
        editor.getCldVariables().forEach(v -> names.add(v.name()));

        // Add expanded subscripted names (e.g., Population[North])
        Map<String, List<String>> dimLabels = buildDimensionLabelMap(editor);
        if (!dimLabels.isEmpty()) {
            addExpandedNames(names, editor.getStocks(), StockDef::name,
                    StockDef::subscripts, dimLabels);
            addExpandedNames(names, editor.getFlows(), FlowDef::name,
                    FlowDef::subscripts, dimLabels);
            addExpandedNames(names, editor.getVariables(), VariableDef::name,
                    VariableDef::subscripts, dimLabels);
        }

        return names;
    }

    private static Map<String, List<String>> buildDimensionLabelMap(ModelEditor editor) {
        Map<String, List<String>> map = new HashMap<>();
        for (SubscriptDef def : editor.getSubscripts()) {
            map.put(def.name(), def.labels());
        }
        return map;
    }

    private static <T> void addExpandedNames(
            Set<String> names, List<T> elements,
            java.util.function.Function<T, String> nameExtractor,
            java.util.function.Function<T, List<String>> subscriptExtractor,
            Map<String, List<String>> dimLabels) {
        for (T element : elements) {
            List<String> subs = subscriptExtractor.apply(element);
            if (subs == null || subs.isEmpty()) {
                continue;
            }
            String baseName = nameExtractor.apply(element).replace(' ', '_');
            List<List<String>> labelLists = new ArrayList<>();
            for (String dim : subs) {
                List<String> labels = dimLabels.get(dim);
                if (labels != null) {
                    labelLists.add(labels);
                }
            }
            if (!labelLists.isEmpty()) {
                for (List<String> combo : cartesianProduct(labelLists)) {
                    names.add(baseName + "[" + String.join(",", combo) + "]");
                }
            }
        }
    }

    /**
     * Checks whether any referenced element is subscripted but referenced
     * without bracket notation from a non-subscripted context.
     */
    private static String checkSubscriptCompleteness(
            Set<String> refs, ModelEditor editor, String selfName) {
        Map<String, List<String>> elementSubscripts = buildElementSubscriptMap(editor);
        List<String> selfSubs = selfName != null
                ? elementSubscripts.getOrDefault(selfName, List.of()) : List.of();
        // Also check underscore-resolved name
        if (selfSubs.isEmpty() && selfName != null) {
            selfSubs = elementSubscripts.getOrDefault(
                    selfName.replace('_', ' '), List.of());
        }

        List<String> bareRefs = new ArrayList<>();
        for (String ref : refs) {
            // Skip bracket-notation references — those are already explicit
            if (ref.contains("[")) {
                continue;
            }
            String resolved = ref.replace('_', ' ');
            List<String> refSubs = elementSubscripts.getOrDefault(ref, List.of());
            if (refSubs.isEmpty()) {
                refSubs = elementSubscripts.getOrDefault(resolved, List.of());
            }
            if (refSubs.isEmpty()) {
                continue; // not subscripted, nothing to warn about
            }
            // The referenced element is subscripted. If this element shares
            // all the same dimensions, the SubscriptExpander handles it — no warning.
            if (!selfSubs.containsAll(refSubs)) {
                bareRefs.add(ref);
            }
        }

        if (bareRefs.isEmpty()) {
            return null;
        }
        if (bareRefs.size() == 1) {
            return "'" + bareRefs.getFirst() + "' is subscripted \u2014 "
                    + "use bracket notation (e.g., " + bareRefs.getFirst() + "[label])";
        }
        return "Subscripted elements referenced without labels: "
                + String.join(", ", bareRefs);
    }

    private static Map<String, List<String>> buildElementSubscriptMap(ModelEditor editor) {
        Map<String, List<String>> map = new HashMap<>();
        for (StockDef s : editor.getStocks()) {
            if (!s.subscripts().isEmpty()) {
                map.put(s.name(), s.subscripts());
            }
        }
        for (FlowDef f : editor.getFlows()) {
            if (!f.subscripts().isEmpty()) {
                map.put(f.name(), f.subscripts());
            }
        }
        for (VariableDef v : editor.getVariables()) {
            if (!v.subscripts().isEmpty()) {
                map.put(v.name(), v.subscripts());
            }
        }
        return map;
    }

    private static List<List<String>> cartesianProduct(List<List<String>> lists) {
        List<List<String>> result = new ArrayList<>();
        result.add(new ArrayList<>());
        for (List<String> list : lists) {
            List<List<String>> next = new ArrayList<>();
            for (List<String> partial : result) {
                for (String item : list) {
                    List<String> combo = new ArrayList<>(partial);
                    combo.add(item);
                    next.add(combo);
                }
            }
            result = next;
        }
        return result;
    }

    /**
     * Finds the closest match for an unknown name using Levenshtein distance.
     * Returns null if no reasonably close match exists (distance > 3).
     */
    public static String findClosestMatch(String unknown, Set<String> knownNames) {
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        String lowerUnknown = unknown.toLowerCase();
        for (String name : knownNames) {
            int dist = levenshtein(lowerUnknown, name.toLowerCase());
            if (dist < bestDist) {
                bestDist = dist;
                best = name;
            }
        }
        // Only suggest if the edit distance is reasonable relative to name length
        if (best != null && bestDist <= Math.max(3, unknown.length() / 3)) {
            return best;
        }
        return null;
    }

    public static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }
}
