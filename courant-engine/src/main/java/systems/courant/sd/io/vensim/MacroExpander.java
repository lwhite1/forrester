package systems.courant.sd.io.vensim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands Vensim macro calls into ordinary equations by inline substitution.
 *
 * <p>This operates at the text level, before expression translation. Each macro
 * invocation is expanded by substituting formal parameters with actual arguments
 * and prefixing local variables to avoid collisions across multiple instantiations.
 */
public final class MacroExpander {

    /**
     * Result of macro expansion.
     *
     * @param expandedEquations the equations after expansion (original + synthetic)
     * @param warnings any warnings generated during expansion
     */
    public record ExpansionResult(List<MdlEquation> expandedEquations, List<String> warnings) {
        public ExpansionResult {
            expandedEquations = List.copyOf(expandedEquations);
            warnings = List.copyOf(warnings);
        }
    }

    private MacroExpander() {
    }

    /**
     * Expands all macro calls in the given equations.
     *
     * @param equations the parsed equations (may contain macro calls)
     * @param macroDefs the parsed macro definitions
     * @return the expanded equations with macro calls replaced
     */
    public static ExpansionResult expand(List<MdlEquation> equations, List<MacroDef> macroDefs) {
        if (macroDefs == null || macroDefs.isEmpty()) {
            return new ExpansionResult(equations, List.of());
        }

        // Build lookup map: normalized macro name → MacroDef
        Map<String, MacroDef> macroMap = new HashMap<>();
        for (MacroDef def : macroDefs) {
            macroMap.put(def.name().toLowerCase(java.util.Locale.ROOT).strip(), def);
        }

        // Build patterns for each macro name (case-insensitive, word-boundary-aware)
        // Must handle multi-word names: "SMOOTH CUSTOM" → pattern with spaces
        Map<String, Pattern> macroPatterns = new HashMap<>();
        for (MacroDef def : macroDefs) {
            String escaped = Pattern.quote(def.name());
            Pattern p = Pattern.compile(
                    "(?i)(?<![a-zA-Z0-9_])" + escaped + "\\s*\\(",
                    Pattern.CASE_INSENSITIVE);
            macroPatterns.put(def.name().toLowerCase(java.util.Locale.ROOT).strip(), p);
        }

        List<String> warnings = new ArrayList<>();
        List<MdlEquation> result = new ArrayList<>(equations);
        Map<String, Integer> instantiationCounters = new HashMap<>();

        // Re-expansion loop: after expanding one macro call per equation,
        // the result may still contain macro calls (e.g. SMOOTH(x,3) + SMOOTH(y,5)).
        // Re-process until no macros remain, with a safety limit.
        int maxPasses = 10;
        for (int pass = 0; pass < maxPasses; pass++) {
            List<MdlEquation> nextResult = new ArrayList<>();
            boolean anyExpanded = false;

            for (MdlEquation eq : result) {
                if (eq.expression().isEmpty()) {
                    nextResult.add(eq);
                    continue;
                }

                // Check if this equation's expression contains any macro call
                MacroDef matchedMacro = null;
                Pattern matchedPattern = null;
                for (var entry : macroPatterns.entrySet()) {
                    if (entry.getValue().matcher(eq.expression()).find()) {
                        matchedMacro = macroMap.get(entry.getKey());
                        matchedPattern = entry.getValue();
                        break;
                    }
                }

                if (matchedMacro == null) {
                    nextResult.add(eq);
                    continue;
                }

                // Validate: single-output macros only
                if (matchedMacro.outputParams().size() != 1) {
                    warnings.add("Macro '" + matchedMacro.name() + "' has "
                            + matchedMacro.outputParams().size()
                            + " output parameters; only single-output macros are supported. "
                            + "Skipping expansion.");
                    nextResult.add(eq);
                    continue;
                }

                // Find the macro call in the expression and extract arguments
                Matcher callMatcher = matchedPattern.matcher(eq.expression());
                if (!callMatcher.find()) {
                    nextResult.add(eq);
                    continue;
                }

                int openParen = callMatcher.end() - 1;
                int closeParen = VensimExprTranslator.findMatchingParen(eq.expression(), openParen);
                if (closeParen < 0) {
                    warnings.add("Malformed macro call to '" + matchedMacro.name()
                            + "' in equation for '" + eq.name() + "'");
                    nextResult.add(eq);
                    continue;
                }

                String argsContent = eq.expression().substring(openParen + 1, closeParen);
                List<String> actualArgs = splitTopLevelArgs(argsContent);

                // Vensim macro calls don't include the output param — it's bound to the caller's LHS
                int expectedArgs = matchedMacro.inputParams().size();
                if (actualArgs.size() != expectedArgs) {
                    warnings.add("Macro '" + matchedMacro.name() + "' expects " + expectedArgs
                            + " input arguments but got " + actualArgs.size()
                            + " in equation for '" + eq.name() + "'");
                    nextResult.add(eq);
                    continue;
                }

                // Check for nested macro calls in body
                boolean hasNestedCall = false;
                for (MdlEquation bodyEq : matchedMacro.bodyEquations()) {
                    for (var entry : macroPatterns.entrySet()) {
                        if (entry.getValue().matcher(bodyEq.expression()).find()) {
                            hasNestedCall = true;
                            break;
                        }
                    }
                    if (hasNestedCall) {
                        break;
                    }
                }
                if (hasNestedCall) {
                    warnings.add("Macro '" + matchedMacro.name()
                            + "' contains nested macro calls, which are not supported. "
                            + "Skipping expansion.");
                    nextResult.add(eq);
                    continue;
                }

                // Generate unique prefix
                String macroKey = VensimExprTranslator.normalizeName(matchedMacro.name()).toLowerCase();
                int count = instantiationCounters.merge(macroKey, 1, Integer::sum);
                String prefix = "__" + macroKey + "_" + count + "_";

                // Build substitution map
                // 1. Input params → actual argument expressions (parenthesized)
                // 2. Output param → caller's LHS variable name
                // 3. Local variables (body LHS names that aren't params) → prefixed names
                Map<String, String> substitutions = new HashMap<>();

                for (int i = 0; i < matchedMacro.inputParams().size(); i++) {
                    substitutions.put(matchedMacro.inputParams().get(i), "(" + actualArgs.get(i).strip() + ")");
                }

                String outputParam = matchedMacro.outputParams().get(0);
                substitutions.put(outputParam, eq.name());

                // Identify local variables (body equation LHS names that are not parameters)
                List<String> allParams = new ArrayList<>(matchedMacro.inputParams());
                allParams.addAll(matchedMacro.outputParams());
                java.util.Set<String> paramNamesLower = new java.util.HashSet<>();
                for (String p : allParams) {
                    paramNamesLower.add(p.strip().toLowerCase(java.util.Locale.ROOT));
                }

                for (MdlEquation bodyEq : matchedMacro.bodyEquations()) {
                    String bodyName = bodyEq.name().strip();
                    if (!paramNamesLower.contains(bodyName.toLowerCase(java.util.Locale.ROOT))) {
                        substitutions.put(bodyName, prefix + VensimExprTranslator.normalizeName(bodyName));
                    }
                }

                // Expand: substitute in each body equation
                String callerLhs = eq.name();
                boolean isOutputEquation;
                anyExpanded = true;
                for (MdlEquation bodyEq : matchedMacro.bodyEquations()) {
                    String bodyName = bodyEq.name().strip();
                    String newName = applySubstitutions(bodyName, substitutions);
                    String newExpr = applySubstitutions(bodyEq.expression(), substitutions);

                    isOutputEquation = bodyName.equalsIgnoreCase(outputParam.strip());

                    if (isOutputEquation) {
                        // The output equation replaces the caller's equation.
                        // If the macro call was the entire expression, replace it.
                        // If the macro call was embedded in a larger expression, splice in the output expression.
                        String prefix1 = eq.expression().substring(0, callMatcher.start());
                        String suffix = eq.expression().substring(closeParen + 1);
                        String fullExpr;
                        if (prefix1.isBlank() && suffix.isBlank()) {
                            fullExpr = newExpr;
                        } else {
                            fullExpr = prefix1 + "(" + newExpr + ")" + suffix;
                        }
                        nextResult.add(new MdlEquation(callerLhs, eq.operator(), fullExpr,
                                eq.units(), eq.comment(), eq.group()));
                    } else {
                        // Local/input body equation → synthetic equation with prefixed name
                        nextResult.add(new MdlEquation(newName, bodyEq.operator(), newExpr,
                                bodyEq.units(), bodyEq.comment(), eq.group()));
                    }
                }
            }

            result = nextResult;
            if (!anyExpanded) {
                break;
            }
        }

        return new ExpansionResult(result, warnings);
    }

    /**
     * Applies word-boundary-aware substitutions to an expression string.
     * Processes longest names first to prevent partial matches.
     */
    private static String applySubstitutions(String expr, Map<String, String> substitutions) {
        if (expr == null || expr.isEmpty() || substitutions.isEmpty()) {
            return expr;
        }

        // Sort by length descending to avoid partial matches
        List<Map.Entry<String, String>> sorted = new ArrayList<>(substitutions.entrySet());
        sorted.sort(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed());

        for (Map.Entry<String, String> entry : sorted) {
            String name = entry.getKey();
            String replacement = entry.getValue();
            String escaped = Pattern.quote(name);
            Pattern p = Pattern.compile(
                    "(?<![a-zA-Z0-9_])" + escaped + "(?![a-zA-Z0-9_])",
                    Pattern.CASE_INSENSITIVE);
            expr = p.matcher(expr).replaceAll(Matcher.quoteReplacement(replacement));
        }
        return expr;
    }

    /**
     * Splits a comma-separated argument string respecting nested parentheses.
     */
    private static List<String> splitTopLevelArgs(String content) {
        List<String> args = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                args.add(content.substring(start, i));
                start = i + 1;
            }
        }
        args.add(content.substring(start));
        return args;
    }
}
