package systems.courant.sd.app.canvas;

import systems.courant.sd.model.expr.Expr;
import systems.courant.sd.model.expr.ExprDependencies;
import systems.courant.sd.model.expr.ExprParser;
import systems.courant.sd.model.expr.ParseException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
     */
    public record Result(boolean valid, String message) {
        static final Result OK = new Result(true, null);
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
            return new Result(false, formatParseError(e));
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
                return new Result(false, msg);
            }
            return new Result(false,
                    "Unknown variables: " + String.join(", ", unknowns));
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
        return names;
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
