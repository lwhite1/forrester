package systems.courant.sd.io.vensim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

        Map<String, MacroDef> macroMap = buildMacroMap(macroDefs);
        Map<String, Pattern> macroPatterns = buildMacroPatterns(macroDefs);

        List<String> warnings = new ArrayList<>();
        List<MdlEquation> result = new ArrayList<>(equations);
        Map<String, Integer> instantiationCounters = new HashMap<>();

        int maxPasses = 10;
        boolean anyExpanded = false;
        for (int pass = 0; pass < maxPasses; pass++) {
            List<MdlEquation> nextResult = new ArrayList<>();
            anyExpanded = false;

            for (MdlEquation eq : result) {
                if (eq.expression().isEmpty()) {
                    nextResult.add(eq);
                    continue;
                }

                MacroMatch match = findMacroCall(eq, macroMap, macroPatterns);
                if (match == null) {
                    nextResult.add(eq);
                    continue;
                }

                String skipWarning = validateMacroCall(match, eq, macroPatterns);
                if (skipWarning != null) {
                    warnings.add(skipWarning);
                    nextResult.add(eq);
                    continue;
                }

                String prefix = generatePrefix(match.macro, instantiationCounters);
                Map<String, String> substitutions = buildSubstitutionMap(
                        match.macro, match.actualArgs, eq.name(), prefix);

                anyExpanded = true;
                expandMacroBody(match, eq, substitutions, nextResult);
            }

            result = nextResult;
            if (!anyExpanded) {
                break;
            }
        }

        if (anyExpanded) {
            warnings.add("Macro expansion incomplete after " + maxPasses
                    + " passes — some macro calls may remain unexpanded");
        }

        return new ExpansionResult(result, warnings);
    }

    private record MacroMatch(MacroDef macro, Pattern pattern, Matcher callMatcher,
                               int openParen, int closeParen, List<String> actualArgs) {
    }

    private static Map<String, MacroDef> buildMacroMap(List<MacroDef> macroDefs) {
        Map<String, MacroDef> macroMap = new LinkedHashMap<>();
        for (MacroDef def : macroDefs) {
            macroMap.put(def.name().toLowerCase(java.util.Locale.ROOT).strip(), def);
        }
        return macroMap;
    }

    private static Map<String, Pattern> buildMacroPatterns(List<MacroDef> macroDefs) {
        Map<String, Pattern> macroPatterns = new LinkedHashMap<>();
        for (MacroDef def : macroDefs) {
            String escaped = Pattern.quote(def.name());
            Pattern p = Pattern.compile(
                    "(?i)(?<![a-zA-Z0-9_])" + escaped + "\\s*\\(",
                    Pattern.CASE_INSENSITIVE);
            macroPatterns.put(def.name().toLowerCase(java.util.Locale.ROOT).strip(), p);
        }
        return macroPatterns;
    }

    private static MacroMatch findMacroCall(MdlEquation eq,
                                             Map<String, MacroDef> macroMap,
                                             Map<String, Pattern> macroPatterns) {
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
            return null;
        }

        Matcher callMatcher = matchedPattern.matcher(eq.expression());
        if (!callMatcher.find()) {
            return null;
        }

        int openParen = callMatcher.end() - 1;
        int closeParen = VensimExprTranslator.findMatchingParen(eq.expression(), openParen);
        if (closeParen < 0) {
            return null;
        }

        String argsContent = eq.expression().substring(openParen + 1, closeParen);
        List<String> actualArgs = splitTopLevelArgs(argsContent);

        return new MacroMatch(matchedMacro, matchedPattern, callMatcher,
                openParen, closeParen, actualArgs);
    }

    private static String validateMacroCall(MacroMatch match, MdlEquation eq,
                                             Map<String, Pattern> macroPatterns) {
        if (match.macro.outputParams().size() != 1) {
            return "Macro '" + match.macro.name() + "' has "
                    + match.macro.outputParams().size()
                    + " output parameters; only single-output macros are supported. "
                    + "Skipping expansion.";
        }

        int expectedArgs = match.macro.inputParams().size();
        if (match.actualArgs.size() != expectedArgs) {
            return "Macro '" + match.macro.name() + "' expects " + expectedArgs
                    + " input arguments but got " + match.actualArgs.size()
                    + " in equation for '" + eq.name() + "'";
        }

        if (hasNestedMacroCalls(match.macro, macroPatterns)) {
            return "Macro '" + match.macro.name()
                    + "' contains nested macro calls, which are not supported. "
                    + "Skipping expansion.";
        }

        return null;
    }

    private static boolean hasNestedMacroCalls(MacroDef macro,
                                                Map<String, Pattern> macroPatterns) {
        for (MdlEquation bodyEq : macro.bodyEquations()) {
            for (var entry : macroPatterns.entrySet()) {
                if (entry.getValue().matcher(bodyEq.expression()).find()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String generatePrefix(MacroDef macro,
                                          Map<String, Integer> instantiationCounters) {
        String macroKey = VensimExprTranslator.normalizeName(macro.name()).toLowerCase();
        int count = instantiationCounters.merge(macroKey, 1, Integer::sum);
        return "__" + macroKey + "_" + count + "_";
    }

    private static Map<String, String> buildSubstitutionMap(MacroDef macro,
                                                             List<String> actualArgs,
                                                             String callerLhs,
                                                             String prefix) {
        Map<String, String> substitutions = new HashMap<>();

        for (int i = 0; i < macro.inputParams().size(); i++) {
            substitutions.put(macro.inputParams().get(i), "(" + actualArgs.get(i).strip() + ")");
        }

        String outputParam = macro.outputParams().get(0);
        substitutions.put(outputParam, callerLhs);

        List<String> allParams = new ArrayList<>(macro.inputParams());
        allParams.addAll(macro.outputParams());
        java.util.Set<String> paramNamesLower = new java.util.HashSet<>();
        for (String p : allParams) {
            paramNamesLower.add(p.strip().toLowerCase(java.util.Locale.ROOT));
        }

        for (MdlEquation bodyEq : macro.bodyEquations()) {
            String bodyName = bodyEq.name().strip();
            if (!paramNamesLower.contains(bodyName.toLowerCase(java.util.Locale.ROOT))) {
                substitutions.put(bodyName, prefix + VensimExprTranslator.normalizeName(bodyName));
            }
        }

        return substitutions;
    }

    private static void expandMacroBody(MacroMatch match, MdlEquation eq,
                                         Map<String, String> substitutions,
                                         List<MdlEquation> nextResult) {
        String outputParam = match.macro.outputParams().get(0);
        String callerLhs = eq.name();

        for (MdlEquation bodyEq : match.macro.bodyEquations()) {
            String bodyName = bodyEq.name().strip();
            String newName = applySubstitutions(bodyName, substitutions);
            String newExpr = applySubstitutions(bodyEq.expression(), substitutions);

            if (bodyName.equalsIgnoreCase(outputParam.strip())) {
                String exprPrefix = eq.expression().substring(0, match.callMatcher.start());
                String suffix = eq.expression().substring(match.closeParen + 1);
                String fullExpr;
                if (exprPrefix.isBlank() && suffix.isBlank()) {
                    fullExpr = newExpr;
                } else {
                    fullExpr = exprPrefix + "(" + newExpr + ")" + suffix;
                }
                nextResult.add(new MdlEquation(callerLhs, eq.operator(), fullExpr,
                        eq.units(), eq.comment(), eq.group()));
            } else {
                nextResult.add(new MdlEquation(newName, bodyEq.operator(), newExpr,
                        bodyEq.units(), bodyEq.comment(), eq.group()));
            }
        }
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
        if (content.isEmpty()) {
            return List.of();
        }
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
