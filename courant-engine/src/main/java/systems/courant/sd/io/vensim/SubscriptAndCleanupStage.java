package systems.courant.sd.io.vensim;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pipeline stage that expands subscripts, translates remaining operators,
 * rewrites lookup calls, and performs final cleanup.
 *
 * <p>Handles:
 * <ul>
 *   <li>{@code Time} → {@code TIME} (built-in variable)</li>
 *   <li>Vector function expansion ({@code SUM}, {@code VMIN})</li>
 *   <li>Subscript bracket notation ({@code name[label]} → {@code name_label})</li>
 *   <li>Equality operator ({@code =} → {@code ==})</li>
 *   <li>Lookup call rewriting ({@code tableName(x)} → {@code LOOKUP(tableName, x)})</li>
 * </ul>
 */
final class SubscriptAndCleanupStage implements ExprTransformationStage {

    private static final Pattern CARET_PATTERN = Pattern.compile("\\^");
    private static final Pattern TIME_VAR_PATTERN = Pattern.compile("(?i)\\bTime\\b");
    private static final Pattern SUM_FUNC_PATTERN = Pattern.compile("(?i)\\bSUM\\s*\\(");
    private static final Pattern VMIN_FUNC_PATTERN = Pattern.compile("(?i)\\bVMIN\\s*\\(");
    private static final Pattern BANG_DIM_PATTERN = Pattern.compile(
            "([a-zA-Z_][a-zA-Z0-9_]*)!");
    private static final Pattern SUBSCRIPT_BRACKET_PATTERN = Pattern.compile(
            "([a-zA-Z_][a-zA-Z0-9_]*)\\[([^\\]]+)\\]");
    private static final Pattern GET_XLS_DATA_PATTERN = Pattern.compile(
            "(?i)GET\\s+XLS\\s+DATA\\s*\\(");
    private static final Pattern GET_DIRECT_DATA_PATTERN = Pattern.compile(
            "(?i)GET\\s+DIRECT\\s+DATA\\s*\\(");
    private static final Pattern GET_XLS_CONSTANTS_PATTERN = Pattern.compile(
            "(?i)GET\\s+XLS\\s+CONSTANTS\\s*\\(");
    private static final Pattern GET_DIRECT_CONSTANTS_PATTERN = Pattern.compile(
            "(?i)GET\\s+DIRECT\\s+CONSTANTS\\s*\\(");
    private static final Pattern GET_XLS_LOOKUPS_PATTERN = Pattern.compile(
            "(?i)GET\\s+XLS\\s+LOOKUPS\\s*\\(");
    private static final Pattern GET_DIRECT_LOOKUPS_PATTERN = Pattern.compile(
            "(?i)GET\\s+DIRECT\\s+LOOKUPS\\s*\\(");
    private static final Set<String> UNSUPPORTED_FUNCTIONS = Set.of(
            "DELAY N", "TABBED ARRAY",
            "VECTOR SELECT", "VECTOR ELM MAP", "VECTOR SORT ORDER",
            "ALLOCATE AVAILABLE");
    private static final List<Pattern> UNSUPPORTED_FUNCTION_PATTERNS;
    static {
        java.util.ArrayList<Pattern> patterns = new java.util.ArrayList<>();
        for (String func : UNSUPPORTED_FUNCTIONS) {
            patterns.add(Pattern.compile(
                    "(?i)\\b" + Pattern.quote(func) + "\\s*\\("));
        }
        UNSUPPORTED_FUNCTION_PATTERNS = List.copyOf(patterns);
    }

    @Override
    public void apply(TranslationContext ctx) {
        String expr = ctx.expression();
        List<String> warnings = ctx.warnings();

        // ^ → ** (power operator; must run after function translations)
        expr = CARET_PATTERN.matcher(expr).replaceAll("**");

        // Time → TIME (unless "Time" is a user-defined name)
        if (!ctx.knownNamesLower().contains("time")) {
            expr = TIME_VAR_PATTERN.matcher(expr).replaceAll("TIME");
        }

        // Expand SUM(expr[dim!]) and VMIN(expr[dim!]) using subscript dimensions
        if (!ctx.subscriptDimensions().isEmpty()) {
            expr = expandVectorFunctions(expr, ctx.subscriptDimensions(), warnings);
        }

        // Translate subscript bracket notation: name[label] → name_label
        expr = translateSubscriptBrackets(expr);

        // Single = (equality) → == (deferred until after bracket translation so
        // that any = inside subscript brackets is not incorrectly doubled)
        expr = expr.replaceAll("(?<![<>=!])=(?!=)", "==");

        // Rewrite lookupName(arg) → LOOKUP(lookupName, arg)
        expr = rewriteLookupCalls(expr, ctx.lookupNames());

        // GET XLS/DIRECT functions → 0 placeholder with warning
        expr = FunctionTranslationStage.translateGetFunction(
                expr, GET_XLS_DATA_PATTERN, "GET XLS DATA", warnings);
        expr = FunctionTranslationStage.translateGetFunction(
                expr, GET_DIRECT_DATA_PATTERN, "GET DIRECT DATA", warnings);
        expr = FunctionTranslationStage.translateGetFunction(
                expr, GET_XLS_CONSTANTS_PATTERN, "GET XLS CONSTANTS", warnings);
        expr = FunctionTranslationStage.translateGetFunction(
                expr, GET_DIRECT_CONSTANTS_PATTERN, "GET DIRECT CONSTANTS", warnings);
        expr = FunctionTranslationStage.translateGetFunction(
                expr, GET_XLS_LOOKUPS_PATTERN, "GET XLS LOOKUPS", warnings);
        expr = FunctionTranslationStage.translateGetFunction(
                expr, GET_DIRECT_LOOKUPS_PATTERN, "GET DIRECT LOOKUPS", warnings);

        // Check for unsupported functions
        checkUnsupportedFunctions(expr, warnings);

        ctx.setExpression(expr);
    }

    private static String expandVectorFunctions(String expr,
                                                 Map<String, List<String>> subscriptDimensions,
                                                 List<String> warnings) {
        expr = expandSingleVectorFunction(expr, SUM_FUNC_PATTERN, " + ",
                subscriptDimensions, warnings);
        expr = expandSingleVectorFunction(expr, VMIN_FUNC_PATTERN, null,
                subscriptDimensions, warnings);
        return expr;
    }

    private static String expandSingleVectorFunction(String expr, Pattern funcPattern,
                                                      String joinOp,
                                                      Map<String, List<String>> subscriptDimensions,
                                                      List<String> warnings) {
        Matcher m = funcPattern.matcher(expr);
        while (m.find()) {
            int openParen = m.end() - 1;
            int closeParen = ExprParsingUtils.findMatchingParen(expr, openParen);
            if (closeParen < 0) {
                break;
            }

            String innerExpr = expr.substring(openParen + 1, closeParen).strip();

            Matcher bangMatcher = BANG_DIM_PATTERN.matcher(innerExpr);
            String dimName = null;
            List<String> labels = null;
            while (bangMatcher.find()) {
                String candidate = bangMatcher.group(1);
                List<String> candidateLabels = subscriptDimensions.get(candidate);
                if (candidateLabels != null) {
                    dimName = candidate;
                    labels = candidateLabels;
                    break;
                }
            }

            if (dimName == null || labels.isEmpty()) {
                break;
            }

            List<String> expanded = new ArrayList<>();
            for (String label : labels) {
                expanded.add(innerExpr.replace(dimName + "!", label));
            }

            if (expanded.isEmpty()) {
                break;
            }

            String replacement;
            if (joinOp != null) {
                replacement = "(" + String.join(joinOp, expanded) + ")";
            } else {
                replacement = expanded.getLast();
                for (int ei = expanded.size() - 2; ei >= 0; ei--) {
                    replacement = "MIN(" + expanded.get(ei) + ", " + replacement + ")";
                }
            }

            expr = expr.substring(0, m.start()) + replacement
                    + expr.substring(closeParen + 1);
            m = funcPattern.matcher(expr);
        }
        return expr;
    }

    private static String translateSubscriptBrackets(String expr) {
        Matcher m = SUBSCRIPT_BRACKET_PATTERN.matcher(expr);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String varName = m.group(1);
            String rawSubscript = m.group(2);
            String subscript;
            if (rawSubscript.contains(",")) {
                StringBuilder subSb = new StringBuilder();
                String[] parts = rawSubscript.split(",");
                for (int j = 0; j < parts.length; j++) {
                    if (j > 0) {
                        subSb.append("_");
                    }
                    subSb.append(VensimExprTranslator.normalizeName(parts[j].strip()));
                }
                subscript = subSb.toString();
            } else {
                subscript = VensimExprTranslator.normalizeName(rawSubscript);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(varName + "_" + subscript));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String rewriteLookupCalls(String expr, Set<String> lookupNames) {
        if (lookupNames == null || lookupNames.isEmpty()) {
            return expr;
        }
        for (String name : lookupNames) {
            Pattern p = Pattern.compile("\\b(" + Pattern.quote(name) + ")\\s*\\(");
            Matcher m = p.matcher(expr);
            while (m.find()) {
                int funcStart = m.start();
                int argsStart = m.end();
                int closeParen = ExprParsingUtils.findMatchingParen(expr, argsStart - 1);
                if (closeParen > 0) {
                    String arg = expr.substring(argsStart, closeParen).strip();
                    String replacement = "LOOKUP(" + name + ", " + arg + ")";
                    expr = expr.substring(0, funcStart) + replacement
                            + expr.substring(closeParen + 1);
                    m = p.matcher(expr);
                } else {
                    break;
                }
            }
        }
        return expr;
    }

    private static void checkUnsupportedFunctions(String expr, List<String> warnings) {
        for (Pattern p : UNSUPPORTED_FUNCTION_PATTERNS) {
            Matcher m = p.matcher(expr);
            if (m.find()) {
                String matched = m.group().strip();
                int parenIdx = matched.indexOf('(');
                String funcName = (parenIdx > 0)
                        ? matched.substring(0, parenIdx).strip()
                        : matched;
                warnings.add("Unsupported Vensim function: " + funcName);
            }
        }
    }
}
